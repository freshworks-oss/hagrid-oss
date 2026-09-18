package com.freshworks.core.traverser;

import lombok.Getter;
import lombok.Setter;

import java.util.*;

@Getter
@Setter
public class NodesCycle {

    String cycleId = UUID.randomUUID().toString();
    String readableCycleName;
    private List<DagNode> nodesInCycle = new ArrayList<>();
    private List<CycleEntryPoint> cycleEntryPoints = new ArrayList<>();

    public void addNodeInCycle(DagNode node) {
        nodesInCycle.add(node);
        recalculateCycleEntryPoints();
        readableCycleName = readableCycleName();
    }

    private void recalculateCycleEntryPoints() {
        cycleEntryPoints.clear();
        for (DagNode node : nodesInCycle) {

            for(Map.Entry<DagNode, NodeRelationship> entry : node.getParentRelationshipMap().entrySet()) {

                DagNode parent = entry.getKey();
                NodeRelationship relationship = entry.getValue();

                if(!nodesInCycle.contains(parent)){
                    CycleEntryPoint cycleEntryPoint = new CycleEntryPoint();
                    cycleEntryPoint.setCycleNode(node);
                    cycleEntryPoint.setRelationship(relationship);
                    cycleEntryPoint.setParentNode(parent);
                    cycleEntryPoints.add(cycleEntryPoint);
                }
            }
        }
    }

    public String readableCycleName() {

        String readableCycleName = "";
        List<String> ascendingOrderNodeName = new ArrayList<>();
        for(DagNode node : nodesInCycle) {
            ascendingOrderNodeName.add(node.getShortName());
        }

        Collections.sort(ascendingOrderNodeName);
        readableCycleName = String.join(" , ", ascendingOrderNodeName);
        return readableCycleName;
    }

    @Override
    public int hashCode() {
        String hashCode;
        List<String> ascendingOrderNodeName = new ArrayList<>();
        for(DagNode node : nodesInCycle) {
            ascendingOrderNodeName.add(node.getName());
        }

        Collections.sort(ascendingOrderNodeName);
        hashCode = String.join("_", ascendingOrderNodeName);
        return hashCode.hashCode();
    }

    @Override
    public boolean equals(Object NodesCycle){
        NodesCycle other = (NodesCycle) NodesCycle;

        if(this.nodesInCycle.size() != other.nodesInCycle.size()){
            return false;
        }

        if(this.nodesInCycle.containsAll(other.nodesInCycle)){
            return true;
        }

        return false;



//
//        boolean areAllNodesSame = false;
//        for(DagNode node : nodesInCycle) {
//            for(DagNode otherNode : other.nodesInCycle) {
//                if(!node.equals(otherNode)) {
//                    areAllNodesSame = false;
//                    break;
//                }
//                else{
//                    areAllNodesSame = true;
//                }
//            }
//        }
//
//        if(areAllNodesSame) {
//            return true;
//        }
//
//        return false;
    }
}
