package com.freshworks.core.traverser;

import com.freshworks.core.shared.Namespace;
import com.freshworks.core.shared.analytics.AnalyticsFactory;
import com.freshworks.core.shared.analytics.AnalyticsService;
import com.google.common.base.Throwables;
import org.slf4j.MDC;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.Callable;

@Component
@Scope("prototype")
public class NodeCycleService implements Callable<Void> {

    Stack<NodesCycle> nodesCycles = new Stack<>();
    long delayInMs;
    AnalyticsService analyticsService;
    Namespace namespace;
    String uuid;
    DagNode startingNode;

    Map<String, String> mainThreadMdcCopy;


    public void configure(String parentUUId, int delayInMs, Namespace namespace, DagNode startingNode, AnalyticsFactory analyticsFactory) throws Exception {
        uuid =  parentUUId + "/" + UUID.randomUUID();
        this.startingNode = startingNode;
        this.analyticsService = analyticsFactory.getAnalyticsService(namespace.getNamespace());
        this.namespace = namespace;
        this.delayInMs = delayInMs;
        mainThreadMdcCopy = MDC.getCopyOfContextMap();
        nodesCycles.addAll(DagNode.getNodesCycleInDag(startingNode));
    }

    @Override
    public Void call() throws Exception {

        try{
            MDC.setContextMap(mainThreadMdcCopy);
            detectAndTerminateCyclesWithDelayOf(this.delayInMs);

            if(Thread.interrupted()){
                throw new InterruptedException("leaving process without completing as thread is interrupted");
            }

            analyticsService.infoLogEvent("HAGRID_NODE_CYCLE_SERVICE", "number_of_cycles", nodesCycles.size() , "_message", "returning because node cycle service is completed", "uuid", uuid, "namespace" ,namespace.getNamespace());
        }
        catch(InterruptedException e){
            analyticsService.errorLogEvent("HAGRID_NODE_CYCLE_SERVICE", "_message", e.getClass().getName() + ": " + e.getMessage(),  "stacktrace" , Throwables.getStackTraceAsString(e), "namespace" ,namespace.getNamespace(), "uuid", uuid);
        }
        finally {

            MDC.clear();
            // Clearing the thread interrupt flag if it is set so that when executor service lend this thread to some other task then it should have this flag cleared.
            Thread.interrupted();
            return null;
        }
    }

    public void detectAndTerminateCyclesWithDelayOf(long delayInMs) throws Exception{

        analyticsService.infoLogEvent("HAGRID_NODE_CYCLE_SERVICE", "number_of_pending_cycles", nodesCycles.size() , "_message", "Starting node cycle service", "uuid", uuid, "namespace" ,namespace.getNamespace());
        while(!nodesCycles.isEmpty()){
            NodesCycle nodesCycle = nodesCycles.pop();

                if(isNodeCycleSaturated(nodesCycle)){
                    analyticsService.infoLogEvent("HAGRID_NODE_CYCLE_SERVICE", "number_of_pending_cycles", nodesCycles.size(), "_message" , "node cycle is saturated and terminated the entry point node" ,  "node_cycle_id", nodesCycle.getCycleId(), "node_cycle_name" , nodesCycle.getReadableCycleName() , "uuid", uuid, "namespace" ,namespace.getNamespace());
                    // TODO: Here I am taking just any ( first in this case) entry point and terminating its node which should eventually terminate all nodes in the cycles
                    // If there are multiple entry point, which entry point should we choose to terminate ?
                    // As of now I do not find any differentiation between entrypoints hence taking the first one.
                    DagNode entryPointCycleNode = nodesCycle.getCycleEntryPoints().getFirst().cycleNode;
                    entryPointCycleNode.setNodeStatusSuccessfulOrFailed();
                }

                else if (isNodeCycleAlreadyTerminated(nodesCycle)){
                    analyticsService.errorLogEvent("HAGRID_NODE_CYCLE_SERVICE", "number_of_pending_cycles", nodesCycles.size(), "_message" , "Cycles has been terminated automatically. This is usually when termination of some other cycles triggers termination of this cycle." , "node_cycle_id", nodesCycle.getCycleId(), "node_cycle_name" , nodesCycle.getReadableCycleName(), "uuid", uuid, "namespace" ,namespace.getNamespace());

                }
                else{
                    // Then do not push this into stack again.
                    nodesCycles.push(nodesCycle);
                    analyticsService.infoLogEvent("HAGRID_NODE_CYCLE_SERVICE", "number_of_pending_cycles`", nodesCycles.size(), "_message" , "node cycle is not yet saturated or automatically terminated. Pushing it back to stack for checking it after sometime" , "node_cycle_id", nodesCycle.getCycleId(), "node_cycle_name" , nodesCycle.getReadableCycleName(), "uuid", uuid, "namespace" ,namespace.getNamespace());
                }

            Thread.sleep(delayInMs);
        }

    }

    public boolean isNodeCycleAlreadyTerminated(NodesCycle nodesCycle) throws Exception {

        boolean isNodeCycleTerminated = false;

        // All entry points are closed, now check if all cycles nodes are in parent stuck
        for(int i = 0; i < nodesCycle.getNodesInCycle().size(); i++){

            DagNode parentCycleNode;
            DagNode childCycleNode;

            // i.e it is last node
            if( i == nodesCycle.getNodesInCycle().size() - 1){

                parentCycleNode = nodesCycle.getNodesInCycle().get(i);
                childCycleNode = nodesCycle.getNodesInCycle().get(0);
            }
            else{
                parentCycleNode = nodesCycle.getNodesInCycle().get(i);
                childCycleNode = nodesCycle.getNodesInCycle().get(i + 1);
            }

            // Checking if all nodes in the cycles are terminated for their parent or not.
            // If yes then this cycle is already terminate .. This can be the case when Bigger cycle nested the smaller cycles
            // When bigger cycle terminate then it terminates the nested cycles also
            if(childCycleNode.getParentRelationshipMap().get(parentCycleNode).getStatus() == 1 || childCycleNode.getParentRelationshipMap().get(parentCycleNode).getStatus() == -1){
                isNodeCycleTerminated = true;
            }
            else{
                isNodeCycleTerminated = false;
                break;
            }
        }

        return isNodeCycleTerminated;
    }

    public boolean isNodeCycleSaturated(NodesCycle nodesCycle) throws Exception {

        boolean isCycleStale = false;

        // Check if all entry points are closed
        for(CycleEntryPoint cycleEntryPoint : nodesCycle.getCycleEntryPoints()){
            Relationship relationship = cycleEntryPoint.getRelationship();
            if(relationship.getStatus() == 0 || relationship.getStatus() == -100){
                return false;
            }
        }

        // All entry points are closed, now check if all cycles nodes are in parent stuck
        for(int i = 0; i < nodesCycle.getNodesInCycle().size(); i++){

            DagNode parentCycleNode;
            DagNode childCycleNode;

            // i.e it is last node
            if( i == nodesCycle.getNodesInCycle().size() - 1){

                parentCycleNode = nodesCycle.getNodesInCycle().get(i);
                childCycleNode = nodesCycle.getNodesInCycle().get(0);
            }
            else{
                parentCycleNode = nodesCycle.getNodesInCycle().get(i);
                childCycleNode = nodesCycle.getNodesInCycle().get(i + 1);
            }

            // Checking if child node is waiting on parentNode
            // Parent node waiting list must contain this child node
            // child node do not have any pending step running. It will make sure child node waiting on parent is actually blocked.
            // Child node has processed all data of its parent node
            // If all above conditions are true then it is 100% stale cycle
            if(!parentCycleNode.getNodesWaitingStack().isEmpty()
                    && parentCycleNode.getNodesWaitingStack().contains(childCycleNode)
                    && (childCycleNode.getParentRelationshipMap().get(parentCycleNode).getTotalItemsSynced() == (childCycleNode.getParentRelationshipMap().get(parentCycleNode).getSuccessfulItemsSynced() + childCycleNode.getParentRelationshipMap().get(parentCycleNode).getFailedItemsSynced())
                    && (childCycleNode.getParentRelationshipMap().get(parentCycleNode).getTotalItemsSynced() == parentCycleNode.getInfraDbList().size())))
            {
                isCycleStale = true;
            }
            else{
                isCycleStale = false;
                break;
            }
        }

        return isCycleStale;

    }
}
