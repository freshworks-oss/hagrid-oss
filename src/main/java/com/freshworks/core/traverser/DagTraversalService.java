package com.freshworks.core.traverser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshworks.core.shared.Annotations.BetaRelease;
import com.freshworks.core.shared.Annotations.Retire;
import com.freshworks.core.shared.Namespace;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.analytics.AnalyticsFactory;
import com.freshworks.core.shared.analytics.AnalyticsService;
import com.freshworks.core.shared.infra.InfraConfigService;
import com.freshworks.core.shared.infra.InfraService;
import com.freshworks.core.shared.sync.SyncStatusService;
import com.freshworks.core.shared.synchronizers.ServiceTree;
import com.freshworks.core.traverser.net.http.HttpRequestResponse;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import lombok.Getter;
import org.slf4j.MDC;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Component
@Scope(value="prototype")
public class DagTraversalService implements Callable<Void> {

    @Getter
    String uuid;

    DagNode startingNode;

    ImmutableMap<String, String> baggageMap;

    Phaser dagPhaser;


    InfraService infra;

    Namespace namespace;

    SyncServiceContainer syncServiceContainer;

    TraverserExecutorService traverserExecutorService;

    TraverseConfigService traverserConfigService;

    SyncStatusService syncStatusService;

    InfraConfigService infraConfigService;

    ObjectMapper objectMapper = new ObjectMapper();

    AnalyticsService analyticsService;

    ServiceTree serviceTree;

    Map<String, String> mainThreadMdcCopy;

    ReentrantReadWriteLock shutdownLock = new ReentrantReadWriteLock();

    @Override
    public Void call() throws Exception {

        try{

            analyticsService.infoLogEvent("HAGRID_DAG",  "_message", "DagTraversal started", "uuid", uuid, "namespace" ,namespace.getNamespace());
            MDC.setContextMap(mainThreadMdcCopy);
            traverser();

            // Wait until all nodes until they are returned back
            dagPhaser.arriveAndAwaitAdvance();

            if(Thread.interrupted()){

                throw new InterruptedException("leaving process without completing as thread is interrupted");
            }

            else{

                // Here now check the status of each node, whether it is 1 or -1 .
                Iterator<DagNode> treeNodeIterator = startingNode.preOrder().iterator();
                boolean isTraverserSuccessful = true;

                while(treeNodeIterator.hasNext()){
                    DagNode treeNode = treeNodeIterator.next();

                    if(treeNode.getNodeOverallTraverserStatus() == 0 ){
                        String errorMessage = "One of the DagNode " + treeNode.getName() + " is still in progress with status " + treeNode.getNodeOverallTraverserStatus() + " while DagTraversing has returned. It should not be the case";
                        analyticsService.errorLogEvent("HAGRID_DAG", "_message", errorMessage,"uuid", uuid, "namespace", namespace.getNamespace());
                        throw new IllegalStateException(errorMessage);
                    }
                    if(treeNode.getNodeOverallTraverserStatus() == -1){
                        isTraverserSuccessful = false;
                        break;
                    }
                }

                if(isTraverserSuccessful){
                    syncStatusService.setTraverserInSuccessful();
                    analyticsService.infoLogEvent("HAGRID_DAG",  "_message", "returning because sync is completed", "uuid", uuid, "namespace", namespace.getNamespace());
                    analyticsService.infoLogEvent("HAGRID_DAG",  "_message", "Sync is successful", "uuid", uuid, "namespace" ,namespace.getNamespace());
                }
                else{
                    syncStatusService.setTraverserInFailed();
                    analyticsService.warnLogEvent("HAGRID_DAG",  "_message", "sync has failed because one or more node has failed status", "uuid", uuid, "namespace" ,namespace.getNamespace());
                }
            }
        }

        catch(Exception e){

            syncStatusService.setTraverserInFailed();
            analyticsService.errorLogEvent("HAGRID_DAG", "_message", e.getMessage(), "stacktrace" , Throwables.getStackTraceAsString(e),  "uuid" , uuid, "namespace" ,namespace.getNamespace());
        }

        finally {

            // Signify infra that traverser is done pushing all messages
            infra.getProcessorQueue().removePublisher();
            // clearing the items as this thread might be reuse
            MDC.clear();

            // Clearing the thread interrupt flag if it is set so that when executor service lend this thread to some other task then it should have this flag cleared.
            Thread.interrupted();
        }

        return null;
    }

    public void configure(String parentServiceUid, DagNode startingNode, ImmutableMap<String, String> baggageMap, Phaser dagPhaser, SyncServiceContainer syncServiceContainer){

        uuid =  parentServiceUid +  "/" + UUID.randomUUID();
        this.startingNode = startingNode;
        this.baggageMap = baggageMap;
        this.dagPhaser = dagPhaser;
        this.dagPhaser.register();
        this.syncServiceContainer = syncServiceContainer;
        this.infra = syncServiceContainer.getBean(InfraService.class);
        this.syncStatusService = syncServiceContainer.getBean(SyncStatusService.class);
        this.traverserConfigService = syncServiceContainer.getBean(TraverseConfigService.class);
        this.infraConfigService = syncServiceContainer.getBean(InfraConfigService.class);
        this.traverserExecutorService = syncServiceContainer.getBean(TraverserExecutorService.class);
        AnalyticsFactory analyticsFactory = syncServiceContainer.getBean(AnalyticsFactory.class);
        namespace = syncServiceContainer.getBean(Namespace.class);
        this.serviceTree = syncServiceContainer.getBean(ServiceTree.class);
        this.analyticsService = analyticsFactory.getAnalyticsService(namespace.getNamespace());

        mainThreadMdcCopy = MDC.getCopyOfContextMap();
    }

    public void traverser() throws Exception {

//        this.serviceTree.register(uuid + "/" + "dag_node_traversal");
        String threadName = "DagTraversalService" + "_" + Thread.currentThread().getName();
        Thread.currentThread().setName(threadName);

        syncStatusService.setTraverserInProgress();
        Iterator<DagNode> treeNodeIterator = startingNode.preOrder().iterator();

        while (treeNodeIterator.hasNext() && Boolean.FALSE.equals(Thread.interrupted())) {
            DagNode n = treeNodeIterator.next();
            DagNodeTraversalService dagNodeTraversal = syncServiceContainer.getBean(DagNodeTraversalService.class);
            dagPhaser.register();
            dagNodeTraversal.configure(uuid + "/dag_node_traversal" , dagPhaser, syncServiceContainer, n, startingNode, this.infra, this.traverserConfigService, baggageMap);
            traverserExecutorService.submit( namespace.getNamespace(), dagNodeTraversal);
        }

    }

    @BetaRelease(sourceVersion = "3.6.0", useCase = "shutdown sync gracefully", message = "this is in beta and under testing")
    public boolean interruptSync() throws InterruptedException {
        traverserExecutorService.interruptSync(namespace.getNamespace());
        traverserExecutorService.destroy(namespace.getNamespace());
        return true;
    }


    public static class TraverseAction{

        public enum TRAVERSE_EVENT{
            ABORT_TRANSACTION, // Rest all
            ON_HOLD_AND_RETRY, // 429 rate limit handling
            RETRY_WITH_NEW_REQUEST, // 401 authentication issue
//            RESUME_TRANSACTION,
            ABORT_CURRENT_PARENT_AND_CONTINUE_WITH_NEXT_PARENT // 403 permission issue handling
        }

        DagTraversalService.TraverseAction.TRAVERSE_EVENT traverse_event;

        long waitTimeInMilliseconds;

        HttpRequestResponse requestResponse;

        RequestResponseContainer requestResponseContainer;


        @Retire(targetVersion = "4.0.0", alternate = "use abortCurrentParentAndContinueWithNextParent", message = "This unit is redundant of what abortCurrentParentAndContinueWithNextParent. Earlier intention to use this action was to abort the whole transaction, which I think is not the right (couldn't find any use case as such) and aborting whole sync just because one item has not been fetch is highly unlikely use case")
        public void abortTransaction(){
            traverse_event = DagTraversalService.TraverseAction.TRAVERSE_EVENT.ABORT_TRANSACTION;
        }

        public void holdAndReTry(long waitTime, TimeUnit timeUnit){
            this.waitTimeInMilliseconds = timeUnit.toMillis(waitTime);
            traverse_event = DagTraversalService.TraverseAction.TRAVERSE_EVENT.ON_HOLD_AND_RETRY;
        }

        public void retryWithNewRequest(HttpRequestResponse newRequest){
            traverse_event = DagTraversalService.TraverseAction.TRAVERSE_EVENT.RETRY_WITH_NEW_REQUEST;
            requestResponse = newRequest;
        }

        public void retryWithNewRequestContainer(RequestResponseContainer newRequest){
            traverse_event = DagTraversalService.TraverseAction.TRAVERSE_EVENT.RETRY_WITH_NEW_REQUEST;
            requestResponseContainer = newRequest;
        }

        public void abortCurrentParentAndContinueWithNextParentInstance(){
            traverse_event = DagTraversalService.TraverseAction.TRAVERSE_EVENT.ABORT_CURRENT_PARENT_AND_CONTINUE_WITH_NEXT_PARENT;
        }
    }
}
