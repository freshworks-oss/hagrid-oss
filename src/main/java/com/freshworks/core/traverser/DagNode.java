package com.freshworks.core.traverser;


import com.fasterxml.jackson.databind.JsonNode;
import com.freshworks.core.shared.infra.InfraDbKeyValue;
import com.freshworks.core.shared.infra.InfraDbList;
import com.freshworks.core.traverser.exception.StepFailedException;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

@Getter
@Setter
public class DagNode implements AutoCloseable {

    private Boolean isCloned = false;
    private String nodeId;
    private String name;
    private String shortName;
    private int nodeOverallTraverserStatus = -100;
    private JsonNode data;
    private LinkedHashMap<DagNode, Relationship> childrenRelationshipMap;
    private LinkedHashMap<DagNode, Relationship> parentRelationshipMap;
    private InfraDbList infraDbList;
    private InfraDbKeyValue infraDbKeyValue;

    private ReentrantLock dagManipulationLock = new ReentrantLock();

    private ReentrantLock nodeLock = new ReentrantLock();
    Stack<DagNode> nodesWaitingStack = new Stack<>();
    final Condition nodeSyncDataChangedCondition = nodeLock.newCondition();
    final Condition nodeTraverserStatusChangeCondition = nodeLock.newCondition();


    public DagNode(String name){
        this.name = name;
        this.shortName = name;
        this.nodeId = UUID.randomUUID().toString();
        childrenRelationshipMap = new LinkedHashMap<>();
        parentRelationshipMap = new LinkedHashMap<>();
    }

    public static DagNode shallowCopyOfDagNode(DagNode dagNode){
        DagNode newDagNode = new DagNode(dagNode.name);
        newDagNode.isCloned = true;
        newDagNode.nodeId = UUID.randomUUID().toString();
        newDagNode.name = dagNode.name;
        newDagNode.shortName = dagNode.shortName;
        newDagNode.data = dagNode.data;
        newDagNode.childrenRelationshipMap = new LinkedHashMap<>();
        newDagNode.parentRelationshipMap = new LinkedHashMap<>();
        newDagNode.nodeOverallTraverserStatus = -100;
        return newDagNode;
    }

    public void configInfra(InfraDbList infraDbList, InfraDbKeyValue infraDbKeyValue){
        this.infraDbList = infraDbList;
        this.infraDbKeyValue = infraDbKeyValue;
    }

    public DagNode find(String name){

        List<DagNode> preOrder = this.preOrder();
        Iterator<DagNode> iterator = preOrder.iterator();
        while(iterator.hasNext()){
            DagNode node = iterator.next();
            if(node.getName().toLowerCase().equals(name.toLowerCase())){
                return node;
            }
        }

        return null;
    }

    public void setParent(DagNode parent){

        try{
            dagManipulationLock.lock();

            // Before making parentNode as parent node of this node
            // Check if it is already a parent, if so do not add it
            // otherwise it will lead to duplication
            if(!parentRelationshipMap.containsKey(parent)){
                Relationship relationship = new Relationship();
                parentRelationshipMap.put(parent, relationship);
            }

            // Make sure to provide reverse relationship as well.
            if(!parent.getChildrenRelationshipMap().containsKey(this)){
                Relationship relationship = new Relationship();
                parent.getChildrenRelationshipMap().put(this, relationship);
            }
        }
        finally {
            dagManipulationLock.unlock();
        }
    }

    private boolean isInParentList(DagNode parentNode){
        return parentRelationshipMap.containsKey(parentNode);
    }

    public void relationshipIncrementTotalItemsCount(DagNode parentNode) throws IllegalArgumentException{

        if(isInParentList(parentNode)){
            Relationship relationship = parentRelationshipMap.computeIfAbsent(parentNode, k -> new Relationship());
            try{
                relationship.getRelationshipDataChangeLock().lock();
                relationship.setTotalItemsSynced(relationship.getTotalItemsSynced() + 1);
            }

            finally {
                relationship.getRelationshipDataChangeLock().unlock();
            }
        }
        else{
            throw new IllegalArgumentException("Parent Node " + parentNode.getName() + " is not parent of this child node " + getName());
        }
    }

    public void relationshipIncrementSuccessItemsCount(DagNode parentNode) throws IllegalArgumentException{

        if(isInParentList(parentNode)){
            Relationship relationship = parentRelationshipMap.computeIfAbsent(parentNode, k -> new Relationship());
            try{
                relationship.getRelationshipDataChangeLock().lock();
                relationship.setSuccessfulItemsSynced(relationship.getSuccessfulItemsSynced() + 1);
            }

            finally {
                relationship.getRelationshipDataChangeLock().unlock();
            }
        }

        else{
            throw new IllegalArgumentException("Parent Node " + parentNode.getName() + " is not parent of this child node " + getName());
        }
    }

    public void relationshipIncrementFailedItemsCount(DagNode parentNode) throws IllegalArgumentException{

        if(isInParentList(parentNode)){
            Relationship relationship = parentRelationshipMap.computeIfAbsent(parentNode, k -> new Relationship());
            try{
                relationship.getRelationshipDataChangeLock().lock();
                relationship.setFailedItemsSynced(relationship.getFailedItemsSynced() + 1);
            }

            finally {
                relationship.getRelationshipDataChangeLock().unlock();
            }
        }
        else{
            throw new IllegalArgumentException("Parent Node " + parentNode.getName() + " is not parent of this child node " + getName());
        }
    }

    public long getRelationshipFailedItemsCount(DagNode parentNode){

        if(isInParentList(parentNode)){
            Relationship relationship = parentRelationshipMap.computeIfAbsent(parentNode, k -> new Relationship());
            return relationship.getFailedItemsSynced();
        }
        else{
            throw new IllegalArgumentException("Parent Node " + parentNode.getName() + " is not parent of this child node " + getName());
        }
    }

    public long getRelationshipTotalItemsCount(DagNode parentNode){

        if(isInParentList(parentNode)){
            Relationship relationship = parentRelationshipMap.computeIfAbsent(parentNode, k -> new Relationship());
            return relationship.getTotalItemsSynced();
        }
        else{
            throw new IllegalArgumentException("Parent Node " + parentNode.getName() + " is not parent of this child node " + getName());
        }
    }

    public long getRelationshipSuccessfulItemsCount(DagNode parentNode){

        if(isInParentList(parentNode)){
            Relationship relationship = parentRelationshipMap.computeIfAbsent(parentNode, k -> new Relationship());
            return relationship.getSuccessfulItemsSynced();
        }
        else{
            throw new IllegalArgumentException("Parent Node " + parentNode.getName() + " is not parent of this child node " + getName());
        }
    }

    public void setRelationshipFailed(DagNode parentNode){

        if(isInParentList(parentNode)){
            Relationship relationship = parentRelationshipMap.computeIfAbsent(parentNode, k -> new Relationship());
            try{
                relationship.getRelationshipStatusChangeLock().lock();
                if(relationship.getFailedItemsSynced() > 0 || (relationship.getTotalItemsSynced() > (relationship.getSuccessfulItemsSynced() + relationship.getFailedItemsSynced()))){
                    relationship.setStatus(-1);
                    relationship.getRelationshipStatusChangedCondition().signalAll();
                }
                else if (relationship.getFailedItemsSynced() == 0 && relationship.getTotalItemsSynced() == relationship.getSuccessfulItemsSynced()){
                    throw new IllegalStateException("Cannot mark the node " + name + " failed for parent " + parentNode.getName() + " because" + " total dagNodePerItems " + relationship.getTotalItemsSynced() + " initiated are  " +
                            "equal to the sum of successful items "  + relationship.getSuccessfulItemsSynced() + " and failed items " + relationship.getFailedItemsSynced() + ". It is the case of successful node traversal");
                }

            }
            finally {
                relationship.getRelationshipStatusChangeLock().unlock();
            }
        }
        else{
            throw new IllegalArgumentException("Parent Node " + parentNode.getName() + " is not parent of this child node " + getName());
        }

    }

    public void setRelationshipInProgress(DagNode parentNode){

        if(isInParentList(parentNode)){
            Relationship relationship = parentRelationshipMap.computeIfAbsent(parentNode, k -> new Relationship());
            try{
                relationship.getRelationshipStatusChangeLock().lock();
                relationship.setStatus(0);
            }
            finally {
                relationship.getRelationshipStatusChangeLock().unlock();
            }
        }
        else{
            throw new IllegalArgumentException("Parent Node " + parentNode.getName() + " is not parent of this child node " + getName());
        }
    }

    public void setRelationshipSuccessful(DagNode parentNode){
        if(isInParentList(parentNode)){
            Relationship relationship = parentRelationshipMap.computeIfAbsent(parentNode, k -> new Relationship());
            try{
                relationship.getRelationshipStatusChangeLock().lock();
                if(relationship.getFailedItemsSynced() == 0 && relationship.getTotalItemsSynced() == relationship.getSuccessfulItemsSynced()){
                    relationship.setStatus(1);
                    relationship.getRelationshipStatusChangedCondition().signalAll();
                }
                else{
                    throw new IllegalStateException("Cannot mark the node " + name + " successful for parent " +  parentNode.getName() + " because" + " total dagNodePerItems " + relationship.getTotalItemsSynced() + " initiated are not " +
                            "equal to the sum of successful items "  + relationship.getSuccessfulItemsSynced() + " and failed items " + relationship.getFailedItemsSynced());
                }

            }
            finally {
                relationship.getRelationshipStatusChangeLock().unlock();
            }
        }
        else{
            throw new IllegalArgumentException("Parent Node " + parentNode.getName() + " is not parent of this child node " + getName());
        }

    }

    public void waitUntilRelationshipIsInProgress(DagNode parentNode) throws Exception{

        if(isInParentList(parentNode)){
            Relationship relationship = parentRelationshipMap.get(parentNode);
            try{
                relationship.getRelationshipStatusChangeLock().lock();
                if(relationship.getStatus() == 0 || relationship.getStatus() == -100){
                    relationship.getRelationshipStatusChangedCondition().await();
                }
            }
            finally {
                relationship.getRelationshipStatusChangeLock().unlock();
            }
        }
    }
    public int getRelationshipStatus(DagNode parentNode){

        if(isInParentList(parentNode)){
            Relationship relationship = parentRelationshipMap.computeIfAbsent(parentNode, k -> new Relationship());
            return relationship.getStatus();
        }
        else{
            throw new IllegalArgumentException("Parent Node " + parentNode.getName() + " is not parent of this child node " + getName());
        }
    }

    public void setNodeFailed(){

        try{
            nodeLock.lock();
            if(getTotalFailedItems() > 0 || (getTotalItemsSynced() > (getTotalSuccessfulItems() + getTotalFailedItems()))){
                this.setNodeOverallTraverserStatus(-1);
                nodeSyncDataChangedCondition.signalAll();
                nodeTraverserStatusChangeCondition.signalAll();
            }
            else if (getTotalFailedItems() == 0 && getTotalItemsSynced() == getTotalSuccessfulItems()){
                throw new IllegalStateException("Cannot mark the node " + name + " failed because" + " total dagNodePerItems " + getTotalItemsSynced() + " initiated are  " +
                        "equal to the sum of successful items "  + getTotalSuccessfulItems() + " and failed items " + getTotalFailedItems() + ". It is the case of successful node traversal");
            }
        }
        finally {
            nodeLock.unlock();
        }
    }

    public void setNodeInProgress(){
        this.setNodeOverallTraverserStatus(0);
    }

    public void setNodeSuccessful() throws IllegalStateException{

        try{
            nodeLock.lock();
            if(getTotalFailedItems() == 0 && getTotalItemsSynced() == getTotalSuccessfulItems()){
                this.setNodeOverallTraverserStatus(1);
                nodeSyncDataChangedCondition.signalAll();
                nodeTraverserStatusChangeCondition.signalAll();
            }
            else{
                throw new IllegalStateException("Cannot mark the node " + name + " successful because" + " total dagNodePerItems " + getTotalItemsSynced() + " initiated are not " +
                        "equal to the sum of successful items "  + getTotalSuccessfulItems() + " and failed items " + getTotalFailedItems());
            }
        }
        finally {
            nodeLock.unlock();
        }
    }

    public void setNodeStatusSuccessfulOrFailed() throws InterruptedException{

        try{
            nodeLock.lock();
            if(this.nodeOverallTraverserStatus == 0 && getTotalFailedItems() == 0 && getTotalItemsSynced() == getTotalSuccessfulItems()){

                this.nodeOverallTraverserStatus = 1;
                nodeSyncDataChangedCondition.signalAll();
                nodeTraverserStatusChangeCondition.signalAll();
            }

            else if (this.nodeOverallTraverserStatus == 0 && (getTotalFailedItems() > 0 || (getTotalItemsSynced() > (getTotalSuccessfulItems() + getTotalFailedItems())))){
                this.nodeOverallTraverserStatus = -1;
                nodeSyncDataChangedCondition.signalAll();
                nodeTraverserStatusChangeCondition.signalAll();
            }
            else{
                throw new IllegalStateException("Can not mark status as successful or failed. Current node status is " + nodeOverallTraverserStatus);
            }
        }
        finally {
            nodeLock.unlock();
        }
    }

    public void waitUntilNodeSyncIsInProgress() throws Exception{

        try{
            nodeLock.lock();

            if(nodeOverallTraverserStatus == 0){
                nodeTraverserStatusChangeCondition.await();
            }
        }

        finally {
            nodeLock.unlock();
        }
    }

    public long getTotalFailedItems(){

        long totalFailedItems = 0;
        for(Relationship relationship : parentRelationshipMap.values()){

            try{
                relationship.getRelationshipDataChangeLock().lock();
                totalFailedItems += relationship.getFailedItemsSynced();
            }
            finally {
                relationship.getRelationshipDataChangeLock().unlock();
            }
        }

        return totalFailedItems;
    }

    public long getTotalItemsSynced(){

        long totalItemsSynced = 0;
        for(Relationship relationship : parentRelationshipMap.values()){

            try{
                relationship.getRelationshipDataChangeLock().lock();
                totalItemsSynced += relationship.getTotalItemsSynced();
            }
            finally {
                relationship.getRelationshipDataChangeLock().unlock();
            }

        }
        return totalItemsSynced;
    }

    public long getTotalSuccessfulItems(){

        long successfulItemsSynced = 0;
        for(Relationship relationship : parentRelationshipMap.values()){
            try{
                relationship.getRelationshipDataChangeLock().lock();
                successfulItemsSynced += relationship.getSuccessfulItemsSynced();
            }
            finally {

                relationship.getRelationshipDataChangeLock().unlock();
            }

        }
        return successfulItemsSynced;
    }

    public void addChild(DagNode child){

        // Add this child node as child of this node only when it is not child already
        //
        if(!childrenRelationshipMap.containsKey(child)){
            this.childrenRelationshipMap.put(child, new Relationship());
        }

        // Add this node as Parent of child node as well
        if(!child.getParentRelationshipMap().containsKey(this)){
            child.getParentRelationshipMap().put(this, new Relationship());
        }
    }

    public boolean isNodeTraversingInProgress(){

        if(this.getNodeOverallTraverserStatus() == 0){
            return true;
        }
        else {
            return false;
        }
    }

    public boolean isNodeTraversingSuccessful(){

        if(this.getNodeOverallTraverserStatus() == 1){
            return true;
        }
        else {
            return false;
        }
    }

    public boolean isNodeTraversingFailed(){

        if(this.getNodeOverallTraverserStatus() == -1){
            return true;
        }
        else {
            return false;
        }
    }

    public void dropSubtree(DagNode node){

        // Find the parent of the node which matches the above node name
        List<DagNode> parentList = new ArrayList<>(this.find(node.getName()).getParentRelationshipMap().keySet());
        // Loop through each parent so that we can remove this node from their children list
        for (DagNode parent : parentList){
            parent.getChildrenRelationshipMap().remove(node);
        }
    }

    public List<DagNode> getSubtree(){
        return new ArrayList<>(this.getChildrenRelationshipMap().keySet());
    }

    public List<DagNode> preOrder(){

        List<DagNode> preOrderList = new ArrayList<>();
        Stack<DagNode> readyToScrapStack = new Stack<>();

        readyToScrapStack.push(this);

        int count = 0;
        while (!readyToScrapStack.isEmpty())
        {
            DagNode curr = readyToScrapStack.pop();
            preOrderList.add(curr);

            if (curr != null)
            {
                List<DagNode> nodeList = new ArrayList<>(curr.getChildrenRelationshipMap().keySet());
                for(int i = curr.getChildrenRelationshipMap().keySet().size() - 1; i >= 0; i--)
                {
                    DagNode childNode =  nodeList.get(i);
                    // This condition is important to make sure right scan happens
                    /**
                     *  Suppose below DAG 
                     *                  
                     *              A
                     *           /     <
                     *          /       \
                     *         v         \
                     *         B <------C
                     * 
                     *    A has B and C as child and , C has B and A as child. 
                     *   in this case, result should be just [A,B,C] ( order does not matter) 
                     *   preOrderList check is needed to make sure that we are not adding them again 
                     *   readyToScrapStack check is needed to make sure we have not scanned ( or in the stack already) already
                     */
                    
                    if(!preOrderList.contains(childNode) && !readyToScrapStack.contains(childNode)){
                        readyToScrapStack.add(childNode);
                    }
                }
            }
        }

        return preOrderList;
    }

    private boolean isNodeAlreadyPresentInCollection(DagNode nodeToCheck, List<DagNode> nodeList){

        for (DagNode node : nodeList) {

            if(node.getName().equalsIgnoreCase(nodeToCheck.getName())){
                return true;
            }
        }
        return false;
    }

    private static boolean isNodeAlreadyPresentInCollection(DagNode nodeToCheck, Stack<DagNode> nodeStack){

        for (DagNode node : nodeStack) {

            if(node.getName().equalsIgnoreCase(nodeToCheck.getName())){
                return true;
            }
        }
        return false;
    }

    public static DagNode cloneDag(DagNode dagNode) throws Exception{

        HashMap<String, DagNode> alreadyTraversedNode = new HashMap<>();
        Stack<DagNode> stackForNextSetOfOriginalChildrenNode = new Stack<>();
        DagNode rootOfClonedDag = shallowCopyOfDagNode(dagNode);

        stackForNextSetOfOriginalChildrenNode.push(dagNode);


        while(!stackForNextSetOfOriginalChildrenNode.isEmpty()){

            DagNode parentNode = stackForNextSetOfOriginalChildrenNode.pop();
            DagNode clonedParentNode = rootOfClonedDag.find(parentNode.getName());

            // Now get all children of this parent Node
            List<DagNode> children = new ArrayList<>(parentNode.getChildrenRelationshipMap().keySet());
            for (DagNode child : children) {
                if(!alreadyTraversedNode.containsKey(child.getName())){
                    // In this line, I will check if this child has already present in cloned Dag
                    // This cases are possible when this child was the child of some other parent node as well.
                    DagNode clonedChildNode = rootOfClonedDag.find(child.getName()) != null ? rootOfClonedDag.find(child.getName()): DagNode.shallowCopyOfDagNode(child);
                    clonedChildNode.setParent(clonedParentNode);

                    // Push this to stack for next traversal
                    if(!isNodeAlreadyPresentInCollection(child, stackForNextSetOfOriginalChildrenNode)){
                        stackForNextSetOfOriginalChildrenNode.push(child);
                    }

                }
                else{
                    // If it is already traversed then get its cloned node
                    DagNode clonedChildNode = alreadyTraversedNode.get(child.getName());

                    // Make this cloned child node as child of cloned Parent node
                    clonedChildNode.setParent(clonedParentNode);
                }
            }
            alreadyTraversedNode.put(parentNode.getName(), clonedParentNode);
        }

        return rootOfClonedDag;
    }

    public List<DagNode> inOrder(){

        return null;
    }

    public List<DagNode> postOrder(){

        return null;
    }

    public void saveSyncResult(String s) throws Exception{

        try{
            nodeLock.lock();
            this.infraDbList.add(s);
            nodeSyncDataChangedCondition.signalAll();
        }

        finally {
            nodeLock.unlock();
        }


    }

    public void saveSyncResult(List<String> s) throws Exception{

        try{
            nodeLock.lock();
            this.infraDbList.add(s);
            nodeSyncDataChangedCondition.signalAll();
        }

        finally {
            nodeLock.unlock();
        }

    }

    public String getSyncResult(int n) throws Exception, StepFailedException {
        return this.infraDbList.get(n);
    }


    // TODO: This method could be a point of contention when a parent has multiple child.
    // Suppose there are N children of this parent P. When N children will say hasMoreData then only one
    // would be able to take the lock and due to which other children would have to wait.
    // And the child who has taken the lock may go into await() condition further until more data is present for this child.
    public boolean waitUntilHasMoreData(int index, DagNode node) throws Exception {

        try{
            nodeLock.lock();

            // It means that child so far has consumed less data than parent has fetched already
            if(index < this.infraDbList.size()){
                return true;
            }

            // It means child has already consumed all data but parent is not yet done
            if(nodeOverallTraverserStatus == -100 || nodeOverallTraverserStatus == 0){
                nodesWaitingStack.push(node);
                nodeSyncDataChangedCondition.await();
                nodesWaitingStack.remove(node);
                return true;
            }

            // It means, child has consumed all data that parent has and parent is also completed with success or error
            else{
                return false;
            }
        }

        finally {
            nodeLock.unlock();
        }
    }

    public List<String> getSyncResult(int start, int n) throws Exception {
        return this.infraDbList.get(start, n);
    }

    public long getSyncResultSize() throws Exception {
        return this.infraDbList.size();
    }


    public static List<NodesCycle> getNodesCycleInDag(DagNode node){

        List<NodesCycle> nodeCycles = new ArrayList<>();
        Stack<List<DagNode>> differentPaths = new Stack<>();
        differentPaths.add(Arrays.asList(node));
        getNodesCycleInDagPrivate(differentPaths, nodeCycles);
        Set<NodesCycle> nodesCycleSet = new LinkedHashSet<>(nodeCycles);

        // Once we have list of cycles, we need to find whether cycles can be joined to form bigger cycles
//        nodeCycles = joinCyclesIfPossible(nodeCycles);
        return new ArrayList<>(nodesCycleSet);
    }


    private static List<NodesCycle> joinCyclesIfPossible(List<NodesCycle> nodeCycles){

        List<NodesCycle> cyclesAfterJoin = new ArrayList<>();

        for(NodesCycle nodeCycle : nodeCycles){
            DagNode lastNode = nodeCycle.getNodesInCycle().getLast();
            String cycleId = nodeCycle.getCycleId();

            for(NodesCycle anotherNodeCycle : nodeCycles){
                String anotherCycleId = anotherNodeCycle.getCycleId();
                if(!cycleId.equals(anotherCycleId)){

                }
            }
        }
        return cyclesAfterJoin;
    }


    private static void getNodesCycleInDagPrivate(Stack<List<DagNode>> differentPaths, List<NodesCycle> nodesCycles){

        List<DagNode> path = differentPaths.pop();
        DagNode lastNodeInThisPath = path.getLast();

        if(!lastNodeInThisPath.getChildrenRelationshipMap().isEmpty()){
            for(DagNode child : lastNodeInThisPath.getChildrenRelationshipMap().keySet()){

                if(path.contains(child)){
                    // Cycle detected
                    nodesCycles.add(createCycle(path, child));
                }
                else{
                    List<DagNode> extendedPath = new ArrayList<>(path);
                    extendedPath.add(child);
                    differentPaths.push(extendedPath);
                }
            }
        }

        if(!differentPaths.isEmpty()){
            getNodesCycleInDagPrivate(differentPaths, nodesCycles);
        }

    }

    private static NodesCycle createCycle(List<DagNode> nodeList, DagNode identifiedNode){

        int index = nodeList.indexOf(identifiedNode);

        NodesCycle nodesCycle = new NodesCycle();
        for(int i = index ;  i < nodeList.size(); i++){
            DagNode child = nodeList.get(i);
            nodesCycle.addNodeInCycle(child);
        }

        return nodesCycle;
    }

    @Override
    public int hashCode() {
        String s = this.getName() + this.getNodeId();
        return s.hashCode();
    }

    @Override
    public boolean equals(Object otherObject){

        DagNode otherDagNode = (DagNode)otherObject;

        if(otherDagNode.getName().equals(this.getName())){
            return true;
        }
        return false;
    }

    @Override
    public void close() throws Exception {

        try{
            nodeLock.lock();

            int numberOfWaitingThreads = nodeLock.getWaitQueueLength(nodeSyncDataChangedCondition);
            // As a safe side, wake up all threads
            nodeSyncDataChangedCondition.signalAll();
        }

        finally {
            nodeLock.unlock();
        }

    }
}
