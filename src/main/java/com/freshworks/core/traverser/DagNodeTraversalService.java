package com.freshworks.core.traverser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshworks.core.shared.Namespace;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.analytics.AnalyticsFactory;
import com.freshworks.core.shared.analytics.AnalyticsService;
import com.freshworks.core.shared.infra.InfraService;
import com.freshworks.core.shared.synchronizers.ServiceTree;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

@Component
@Scope("prototype")
public class DagNodeTraversalService implements Callable<Void> {

    @Getter
    String uuid ;

    ServiceTree serviceTree;
    SyncServiceContainer syncServiceContainer;
    AnalyticsService analyticsService;
    ObjectMapper objectMapper = new ObjectMapper();
    Bucket rateLimitBucket = null;
    Namespace namespace;

    TraverserExecutorService traverserExecutorService;
    DagNode node;
    DagNode topNodeOfThisSubTree;
    ImmutableMap<String, String> baggageMap;
    InfraService  infraService;
    TraverseConfigService traverseConfigService;

    Map<String, String> mainThreadMdcCopy;

    @Setter
    @Getter
    Phaser dagNodePhaser;

    Phaser parentPhaser;

    @Override
    public Void call() throws Exception {

        analyticsService.infoLogEvent("HAGRID_DAG_NODE", "node", node.getName(), "_message", "DagNodeTraversal started", "uuid", uuid, "namespace" ,namespace.getNamespace());

        DagNode dagNode = node;
        try(dagNode){

            MDC.setContextMap(mainThreadMdcCopy);
            traverse();

            // In all cases, DagNodeTraversal Service will wait until all already spun dag node per parents are returned
            dagNodePhaser.arriveAndAwaitAdvance();

            if(Thread.interrupted()){

                throw new InterruptedException("leaving process without completing as thread is interrupted");
            }

            else{

                // It is success case
                if((node.getTotalFailedItems() == 0)  && (node.getTotalItemsSynced() == node.getTotalSuccessfulItems())){
                    node.setNodeSuccessful();
                    analyticsService.infoLogEvent("HAGRID_DAG_NODE", "node", node.getName(), "_message", "returning because node is completed", "uuid", uuid, "namespace" ,namespace.getNamespace());
                }

                // It is failure case
                else if((node.getTotalFailedItems() > 0) && (node.getTotalItemsSynced() == node.getTotalSuccessfulItems() + node.getTotalFailedItems())){
                    node.setNodeFailed();
                    analyticsService.warnLogEvent("HAGRID_DAG_NODE", "node", node.getName(), "_message", "returning because node is completed with errors", "uuid", uuid, "namespace" ,namespace.getNamespace());
                }

                // It is illegal state case
                else{
                    String errorMessage = "For node " + node.getName() + " total perNodeItems are not equal to total failed and total successful. " +
                            "total per node items is " + node.getTotalItemsSynced() + " total success items are " + node.getTotalSuccessfulItems() + " total failed items are " + node.getTotalFailedItems();
                    analyticsService.errorLogEvent("HAGRID_DAG_NODE", "node" , node.getName(), "_message" , errorMessage);
                    throw new IllegalStateException(errorMessage);

                }
            }
        }

        catch (Exception e){

            node.setNodeFailed();
            analyticsService.errorLogEvent("HAGRID_DAG_NODE", "_message", e.getClass().getName() + ": " + e.getMessage(), "stacktrace" , Throwables.getStackTraceAsString(e),"node", node.getName(), "uuid", uuid, "namespace" , namespace.getNamespace());
            dagNodePhaser.arriveAndDeregister();
        }

        finally {

            MDC.clear();
            // Clearing the thread interrupt flag if it is set so that when executor service lend this thread to some other task then it should have this flag cleared.
            Thread.interrupted();
            parentPhaser.arriveAndDeregister();
        }

        return null;
    }

    public void configure(String parentUUId, Phaser parentPhaser,  SyncServiceContainer syncServiceContainer, DagNode node, DagNode topNodeOfThisSubTree, InfraService infraService, TraverseConfigService traverseConfigService, ImmutableMap<String, String> baggageMap) throws Exception {

        uuid =  parentUUId + "/" + UUID.randomUUID();
        this.syncServiceContainer = syncServiceContainer;
        this.node = node;
        this.baggageMap = baggageMap;
        this.infraService = infraService;
        this.traverseConfigService = traverseConfigService;
        this.topNodeOfThisSubTree = topNodeOfThisSubTree;
        AnalyticsFactory analyticsFactory = syncServiceContainer.getBean(AnalyticsFactory.class);
        namespace = syncServiceContainer.getBean(Namespace.class);
        this.analyticsService = analyticsFactory.getAnalyticsService(namespace.getNamespace());
        this.traverserExecutorService = syncServiceContainer.getBean(TraverserExecutorService.class);
        this.serviceTree = syncServiceContainer.getBean(ServiceTree.class);
        this.parentPhaser = parentPhaser;
        this.dagNodePhaser = new Phaser(1);
        mainThreadMdcCopy = MDC.getCopyOfContextMap();
    }

    protected void traverse() throws Exception {

        String threadName = "DagNodeTraversal" + "_" + node.getName();
        Thread.currentThread().setName(threadName);

        node.setNodeInProgress();

        if (node.getParentRelationshipMap().keySet().isEmpty() && node.getName().equals(ParentStep.class.getName())) {
            node.setNodeInProgress();
            node.saveSyncResult("{}");
            node.setNodeSuccessful();
        } else {

            AbstractStep step = syncServiceContainer.getBean(node.getName());
            JsonNode jsonNode = this.traverseConfigService.getRateLimitForStep(step.getClass());
            List<DagNode> parentNodeList = new ArrayList<>();
            int rateLimit = jsonNode.get("api_count").asInt();
            int rateLimitDuration = jsonNode.get("seconds").asInt();
            rateLimitBucket = Bucket.builder().addLimit(Bandwidth.simple(rateLimit, Duration.ofSeconds(rateLimitDuration))).build();
            Semaphore limitNumberOfConcurrentPerItemTraversalSemaphore = new Semaphore(rateLimit);

            // if top node itself is being traversed then it
            if (node.getName().equals(topNodeOfThisSubTree.getName())) {
                DagNode parentNode = new DagNode(node.getName() + "_parent");
                parentNode.configInfra(infraService.getInfraDbList(parentNode.getShortName()), infraService.getKeyValue());
                parentNode.setNodeInProgress();
                parentNode.saveSyncResult("{}");
                parentNode.setNodeSuccessful();

                // Here we will do DAG manipulation to create dummy parent Node of this node.

                // remove all other parents of this node
                node.getParentRelationshipMap().clear();

                // Add this parentNode as the only parentNode of this node
                parentNode.addChild(node);
                parentNodeList.add(parentNode);
            }
            else{
                parentNodeList = new ArrayList<>(node.getParentRelationshipMap().keySet());
            }

            for(DagNode parentNode : parentNodeList) {

                DagNodePerParentTraversalService dagNodePerParentTraversalService = getNodePerParentTraversalService();
                dagNodePhaser.register();
                dagNodePerParentTraversalService.configure(uuid + "/" + "dag_node_per_parent_traversal", dagNodePhaser, syncServiceContainer,node, parentNode, infraService, traverseConfigService, baggageMap, limitNumberOfConcurrentPerItemTraversalSemaphore, rateLimitBucket);
                this.traverserExecutorService.submit(namespace.getNamespace(), dagNodePerParentTraversalService);
            }
        }
    }


    @Lookup
    public DagNodePerParentTraversalService getNodePerParentTraversalService() {
        return null;
    }
}
