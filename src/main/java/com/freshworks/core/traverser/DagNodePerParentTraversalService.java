package com.freshworks.core.traverser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshworks.core.shared.Namespace;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.analytics.AnalyticsFactory;
import com.freshworks.core.shared.analytics.AnalyticsService;
import com.freshworks.core.shared.infra.InfraService;
import com.freshworks.core.shared.synchronizers.ServiceTree;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import io.github.bucket4j.Bucket;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Phaser;
import java.util.concurrent.Semaphore;

@Component
@Scope("prototype")
public class DagNodePerParentTraversalService implements Callable<Void> {


    @Getter
    String uuid ;
    ServiceTree serviceTree;
    AnalyticsService analyticsService;
    SyncServiceContainer syncServiceContainer;
    ObjectMapper objectMapper = new ObjectMapper();
    Bucket rateLimitBucket = null;
    Namespace namespace;
    DagNode node;
    DagNode parentNode;
    TraverserExecutorService traverserExecutorService;
    InfraService infraService;
    TraverseConfigService traverseConfigService;
    Map<String, String> mainThreadMdcCopy;
    ImmutableMap<String, String> baggageMap;
    Semaphore limitNumberOfConcurrentPerItemTraversalSemaphore;

    @Setter
    @Getter
    Phaser dagNodePerParentPhaser;

    Phaser parentPhaser;

    @Override
    public Void call() throws Exception {

        analyticsService.infoEvent("HAGRID_DAG_NODE_PER_PARENT", "node", node.getName(), "parentNode" , parentNode.getName(),  "_message", "DagNodePerParentTraversal started", "uuid", uuid, "namespace" ,namespace.getNamespace());

        try{
            MDC.setContextMap(mainThreadMdcCopy);
            traverse();

            // In all cases, DagNodeTraversalPerParent Service will wait until all already spun dag node per item for this parent are returned
            dagNodePerParentPhaser.arriveAndAwaitAdvance();

            if(Thread.interrupted()){

                throw new InterruptedException("leaving process without completing as thread is interrupted");
            }

            else{

                // It is success case
                if((node.getRelationshipFailedItemsCount(parentNode) == 0)  && (node.getRelationshipTotalItemsCount(parentNode) == node.getRelationshipSuccessfulItemsCount(parentNode))){
                    node.setRelationshipSuccessful(parentNode);
                    analyticsService.infoEvent("HAGRID_DAG_NODE_PER_PARENT", "node", node.getName(),  "parent" , parentNode.getName() , "_message", "returning because node is completed", "uuid", uuid, "namespace" ,namespace.getNamespace());
                }

                // It is failure case
                else if((node.getRelationshipFailedItemsCount(parentNode) > 0) && (node.getRelationshipTotalItemsCount(parentNode) == node.getRelationshipSuccessfulItemsCount(parentNode) + node.getRelationshipFailedItemsCount(parentNode))){
                    node.setRelationshipFailed(parentNode);
                    analyticsService.warnEvent("HAGRID_DAG_NODE_PER_PARENT", "node", node.getName() ,  "parent" , parentNode.getName(), "_message", "returning because node is completed with errors", "uuid", uuid, "namespace" ,namespace.getNamespace());
                }

                // It is illegal state case
                else{
                    String errorMessage = "For node " + node.getName() + " and parent " + parentNode.getName() + " total perNodeItems are not equal to total failed and total successful. " +
                            "total per node items is " + node.getRelationshipTotalItemsCount(parentNode) + " total success items are " + node.getRelationshipSuccessfulItemsCount(parentNode) + " total failed items are " + node.getRelationshipFailedItemsCount(parentNode);
                    analyticsService.errorEvent("HAGRID_DAG_NODE_PER_PARENT", "node" , node.getName(), "parent" , parentNode.getName() ,"_message" , errorMessage, "uuid", uuid, "namespace" ,namespace.getNamespace());
                    throw new IllegalStateException(errorMessage);

                }
            }

        }

        catch(Exception e){
            node.setRelationshipFailed(parentNode);
            analyticsService.errorEvent("HAGRID_DAG_NODE_PER_PARENT", "_message", e.getClass().getName() + ": " + e.getMessage(), "stacktrace" , Throwables.getStackTraceAsString(e), "node", this.node.getName(), "parent" , this.parentNode.getName() , "namespace" ,namespace.getNamespace(), "uuid", uuid, "stacktrace", Throwables.getStackTraceAsString(e));
            dagNodePerParentPhaser.arriveAndDeregister();
        }


        finally {

            MDC.clear();
            // Clearing the thread interrupt flag if it is set so that when executor service lend this thread to some other task then it should have this flag cleared.
            Thread.interrupted();
            parentPhaser.arriveAndDeregister();
        }

        return null;
    }

    public void configure(String parentUUId, Phaser parentPhaser, SyncServiceContainer syncServiceContainer, DagNode node, DagNode parentNode, InfraService infraService, TraverseConfigService traverseConfigService, ImmutableMap<String, String> baggageMap, Semaphore limitNumberOfConcurrentPerItemTraversalSemaphore, Bucket rateLimitBucket) throws Exception {

        uuid =  parentUUId + "/" + UUID.randomUUID();
        this.syncServiceContainer = syncServiceContainer;
        this.node = node;
        this.baggageMap = baggageMap;
        this.infraService = infraService;
        this.traverseConfigService = traverseConfigService;
        AnalyticsFactory analyticsFactory = syncServiceContainer.getBean(AnalyticsFactory.class);
        namespace = syncServiceContainer.getBean(Namespace.class);
        this.analyticsService = analyticsFactory.getAnalyticsService(namespace.getNamespace());
        this.traverserExecutorService = syncServiceContainer.getBean(TraverserExecutorService.class);
        this.serviceTree = syncServiceContainer.getBean(ServiceTree.class);
        this.parentPhaser = parentPhaser;
        this.dagNodePerParentPhaser = new Phaser(1);
        this.parentNode = parentNode;
        this.rateLimitBucket = rateLimitBucket;
        this.limitNumberOfConcurrentPerItemTraversalSemaphore = limitNumberOfConcurrentPerItemTraversalSemaphore;
        mainThreadMdcCopy = MDC.getCopyOfContextMap();
    }


    protected void traverse() throws Exception {

        int index = 0;
        int rateLimitUsed = 0;

        node.setRelationshipInProgress(parentNode);
        node.setNodeInProgress();

        while (Boolean.TRUE.equals(parentNode.waitUntilHasMoreData(index, node))  && Boolean.FALSE.equals(Thread.interrupted())) {

            int numberOfPerItemTraverserCanBeLaunched = limitNumberOfConcurrentPerItemTraversalSemaphore.drainPermits();
            List<String> listOfParentItems;

            if (numberOfPerItemTraverserCanBeLaunched == 0) {
                limitNumberOfConcurrentPerItemTraversalSemaphore.acquire();
                listOfParentItems = parentNode.getSyncResult(index, 1);
            } else {
                listOfParentItems = parentNode.getSyncResult(index, numberOfPerItemTraverserCanBeLaunched);
            }

            int listOfParentItemsFetched = listOfParentItems.size();
            index = index + listOfParentItemsFetched;

            // This to handle the case when drained limit say 800 but items fetched are 700 then we should return 100 permits back
            if (numberOfPerItemTraverserCanBeLaunched > listOfParentItemsFetched) {
                int reverseLimit = numberOfPerItemTraverserCanBeLaunched - listOfParentItemsFetched;
                limitNumberOfConcurrentPerItemTraversalSemaphore.release(reverseLimit);
            }

            for (String s : listOfParentItems) {

                AbstractStep abstractStep = syncServiceContainer.getBean(node.getName());
                DagNodePerItemTraversalService dagNodePerItemTraversalService = getDagNodePerItemTraversalService();
                dagNodePerParentPhaser.register();
                dagNodePerItemTraversalService.configure(uuid + "/" + "dag_node_per_item_traversal", syncServiceContainer, abstractStep, objectMapper.readTree(s), node, parentNode, limitNumberOfConcurrentPerItemTraversalSemaphore, dagNodePerParentPhaser, rateLimitBucket, infraService.getProcessorQueue(), traverseConfigService, baggageMap);
                // Here I am registering per item to the waiter phaser
                // TODO: What if I have register it but when task was in the queue, it got cancelled
                // Then DagNodeTraversal could get stuck.
                // If I move this to register statement to DagNodePerItem then DagNodeTraversal will cross waitUntil if task
                // DagNodePerItem remain in scheduler queue for some time.
                traverserExecutorService.submit(namespace.getNamespace(), dagNodePerItemTraversalService);
                rateLimitUsed = rateLimitUsed + 1;
            }

        } // while close
    }

    @Lookup
    public DagNodePerItemTraversalService getDagNodePerItemTraversalService() {
        return null;
    }
}
