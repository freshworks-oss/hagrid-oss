package com.freshworks.core.traverser;

import com.freshworks.core.shared.infra.InfraDbKeyValue;
import com.freshworks.core.shared.infra.InfraDbList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@SpringBootTest()
@EnabledIfSystemProperty(named = "spring.profiles.active", matches = ".*\\.unit\\..*")
public class TestDagNode {


    @Test
    public void testDagNodeIsAutocloseable(){

        assertThat(AutoCloseable.class.isAssignableFrom(DagNode.class), is(true));
    }


    @Test
    public void testDagNodeConstructorReturnsClonedDagNode(){

        DagNode dagNode = new DagNode("1");
        dagNode.addChild(new DagNode("2"));
        dagNode.addChild(new DagNode("3"));

        DagNode clonedNode = DagNode.shallowCopyOfDagNode(dagNode);

        assertThat(clonedNode.hashCode(), is(not(dagNode.hashCode())));
        assertThat(clonedNode.getIsCloned(), is(true));
        assertThat(clonedNode.getTotalSuccessfulItems(), is(0L));
        assertThat(clonedNode.getTotalFailedItems(), is(0L));
        assertThat(clonedNode.getTotalItemsSynced(), is(0L));
    }

    @Test
    public void testDagPreorder(){

        DagNode dagNode1 = new DagNode("1");
        DagNode dagNode2 = new DagNode("2");
        DagNode dagNode3 = new DagNode("3");
        DagNode dagNode4 = new DagNode("4");
        DagNode dagNode5 = new DagNode("5");

        dagNode1.addChild(dagNode2);
        dagNode1.addChild(dagNode3);
        dagNode3.addChild(dagNode4);
        dagNode3.addChild(dagNode5);

        List<DagNode> dagNodeList = dagNode1.preOrder();

        assertThat(dagNodeList.size(), is(5));
        assertThat(dagNodeList.get(0), is(dagNode1));

        List<DagNode> child = Arrays.asList(dagNode2, dagNode3);
        assertThat(dagNodeList.get(1), in(child));
        assertThat(dagNodeList.get(2), in(child));
        assertThat(dagNodeList.get(1), not(dagNodeList.get(2)));


        child = Arrays.asList(dagNode4, dagNode5);
        assertThat(dagNodeList.get(3), in(child));
        assertThat(dagNodeList.get(4), in(child));
        assertThat(dagNodeList.get(3), not(dagNodeList.get(4)));
    }

    @Test
    public void testDagPreOrderInMultiParentSetup(){

        // In this case, I will make node 5 with two parents node 3 and node 6
        DagNode dagNode1 = new DagNode("1");
        DagNode dagNode2 = new DagNode("2");
        DagNode dagNode3 = new DagNode("3");
        DagNode dagNode4 = new DagNode("4");
        DagNode dagNode5 = new DagNode("5");
        DagNode dagNode6 = new DagNode("6");

        dagNode1.addChild(dagNode2);
        dagNode1.addChild(dagNode3);
        dagNode3.addChild(dagNode4);
        dagNode3.addChild(dagNode5);
        dagNode3.addChild(dagNode6);
        dagNode6.addChild(dagNode5);

        List<DagNode> dagNodeList = dagNode1.preOrder();

        assertThat(dagNodeList.size(), is(6));
        assertThat(dagNodeList.get(0), is(dagNode1));

        List<DagNode> child = Arrays.asList(dagNode2, dagNode3);
        assertThat(dagNodeList.get(1), in(child));
        assertThat(dagNodeList.get(2), in(child));
        assertThat(dagNodeList.get(1), not(dagNodeList.get(2)));


        child = Arrays.asList(dagNode4, dagNode5, dagNode6);
        assertThat(dagNodeList.get(3), in(child));
        assertThat(dagNodeList.get(4), in(child));
        assertThat(dagNodeList.get(5), in(child));
        assertThat(dagNodeList.get(3).getName(), not(dagNodeList.get(4).getName()));
        assertThat(dagNodeList.get(4).getName(), not(dagNodeList.get(5).getName()));
    }

    @Test
    public void testDagPreOrderInRecursiveSetup(){

        // In this case, I will make node 5 and 6 children of 3 and 5 and 6 child and parent of each other
        DagNode dagNode1 = new DagNode("1");
        DagNode dagNode2 = new DagNode("2");
        DagNode dagNode3 = new DagNode("3");
        DagNode dagNode4 = new DagNode("4");
        DagNode dagNode5 = new DagNode("5");
        DagNode dagNode6 = new DagNode("6");

        dagNode1.addChild(dagNode2);
        dagNode1.addChild(dagNode3);
        dagNode3.addChild(dagNode4);
        dagNode3.addChild(dagNode5);
        dagNode3.addChild(dagNode6);
        dagNode6.addChild(dagNode5);
        dagNode5.addChild(dagNode6);

        List<DagNode> dagNodeList = dagNode1.preOrder();

        assertThat(dagNodeList.size(), is(6));
        assertThat(dagNodeList.get(0), is(dagNode1));

        List<DagNode> child = Arrays.asList(dagNode2, dagNode3);
        assertThat(dagNodeList.get(1), in(child));
        assertThat(dagNodeList.get(2), in(child));
        assertThat(dagNodeList.get(1), not(dagNodeList.get(2)));


        child = Arrays.asList(dagNode4, dagNode5, dagNode6);
        assertThat(dagNodeList.get(3), in(child));
        assertThat(dagNodeList.get(4), in(child));
        assertThat(dagNodeList.get(5), in(child));
        assertThat(dagNodeList.get(3).getName(), not(dagNodeList.get(4).getName()));
        assertThat(dagNodeList.get(4).getName(), not(dagNodeList.get(5).getName()));
    }

    @Test
    public void testDagPreOrderInSelfRecursiveSetup(){

        // In this case, I will make child 6 and a self recursive and as well parent of node 5
        DagNode dagNode1 = new DagNode("1");
        DagNode dagNode2 = new DagNode("2");
        DagNode dagNode3 = new DagNode("3");
        DagNode dagNode4 = new DagNode("4");
        DagNode dagNode5 = new DagNode("5");
        DagNode dagNode6 = new DagNode("6");

        dagNode1.addChild(dagNode2);
        dagNode1.addChild(dagNode3);
        dagNode3.addChild(dagNode4);
        dagNode3.addChild(dagNode5);
        dagNode3.addChild(dagNode6);
        dagNode6.addChild(dagNode5);
        dagNode6.addChild(dagNode6);

        List<DagNode> dagNodeList = dagNode1.preOrder();

        assertThat(dagNodeList.size(), is(6));
        assertThat(dagNodeList.get(0), is(dagNode1));

        List<DagNode> child = Arrays.asList(dagNode2, dagNode3);
        assertThat(dagNodeList.get(1), in(child));
        assertThat(dagNodeList.get(2), in(child));
        assertThat(dagNodeList.get(1), not(dagNodeList.get(2)));


        child = Arrays.asList(dagNode4, dagNode5, dagNode6);
        assertThat(dagNodeList.get(3), in(child));
        assertThat(dagNodeList.get(4), in(child));
        assertThat(dagNodeList.get(5), in(child));
        assertThat(dagNodeList.get(3).getName(), not(dagNodeList.get(4).getName()));
        assertThat(dagNodeList.get(4).getName(), not(dagNodeList.get(5).getName()));
    }


    @Test
    public void testDagNodeSendSignalWhenThereIsDataInAvailableViaSaveSyncResult() throws Exception {

        InfraDbList mockedInfraDbList = Mockito.mock(InfraDbList.class);
        InfraDbKeyValue mockedInfraDbKeyValue = Mockito.mock(InfraDbKeyValue.class);


        DagNode dagNode = new DagNode("1");
        dagNode.configInfra(mockedInfraDbList, mockedInfraDbKeyValue);
        AtomicReference<Boolean> isSignalRight = new AtomicReference<>(false);
        Thread t1 = new Thread(() -> {

            try{
                dagNode.getNodeLock().lock();
                dagNode.nodeSyncDataChangedCondition.await();
                assertThat(true, is(true));
                isSignalRight.set(true);
            }
            catch (Exception e){
                e.printStackTrace();
            }
            finally {

                dagNode.getNodeLock().unlock();
            }
        });

        t1.start();
        Thread.sleep(2000);

        dagNode.saveSyncResult("{}");
        t1.join();

        assertThat(isSignalRight.get(), is(true));

    }

    @Test
    public void testDagNodeSendSignalWhenThereIsDataInAvailableViaSaveSyncResultList() throws Exception {

        InfraDbList mockedInfraDbList = Mockito.mock(InfraDbList.class);
        InfraDbKeyValue mockedInfraDbKeyValue = Mockito.mock(InfraDbKeyValue.class);


        DagNode dagNode = new DagNode("1");
        dagNode.configInfra(mockedInfraDbList, mockedInfraDbKeyValue);
        AtomicReference<Boolean> isSignalRight = new AtomicReference<>(false);
        Thread t1 = new Thread(() -> {

            try{
                dagNode.getNodeLock().lock();
                dagNode.nodeSyncDataChangedCondition.await();
                assertThat(true, is(true));
                isSignalRight.set(true);
            }
            catch (Exception e){
                e.printStackTrace();
            }
            finally {

                dagNode.getNodeLock().unlock();
            }
        });

        t1.start();
        Thread.sleep(2000);
        ArrayList<String> as = new ArrayList<>();
        as.add("{}");

        dagNode.saveSyncResult(as);
        t1.join();

        assertThat(isSignalRight.get(), is(true));

    }


    @Test
    public void testDagNodeSendSignalWhenThereIsDataInAvailableWhenNodeTraversingIsSuccessful() throws InterruptedException {

        InfraDbList mockedInfraDbList = Mockito.mock(InfraDbList.class);
        InfraDbKeyValue mockedInfraDbKeyValue = Mockito.mock(InfraDbKeyValue.class);


        DagNode dagNode = new DagNode("1");
        dagNode.configInfra(mockedInfraDbList, mockedInfraDbKeyValue);
        AtomicReference<Boolean> isSignalRight = new AtomicReference<>(false);
        Thread t1 = new Thread(() -> {

            try{
                dagNode.getNodeLock().lock();
                dagNode.nodeSyncDataChangedCondition.await();
                assertThat(true, is(true));
                isSignalRight.set(true);
            }
            catch (Exception e){
                e.printStackTrace();
            }
            finally {

                dagNode.getNodeLock().unlock();
            }
        });

        t1.start();
        Thread.sleep(2000);

        dagNode.setNodeSuccessful();
        t1.join();

        assertThat(isSignalRight.get(), is(true));

    }

    @Test
    public void testDagNodeSendSignalWhenThereIsDataInAvailableWhenNodeTraversingIsFailed() throws InterruptedException {

        InfraDbList mockedInfraDbList = Mockito.mock(InfraDbList.class);
        InfraDbKeyValue mockedInfraDbKeyValue = Mockito.mock(InfraDbKeyValue.class);

        DagNode parentNode = new DagNode("A");
        DagNode dagNode = new DagNode("1");
        dagNode.setParent(parentNode);
        dagNode.configInfra(mockedInfraDbList, mockedInfraDbKeyValue);
        AtomicReference<Boolean> isSignalRight = new AtomicReference<>(false);
        Thread t1 = new Thread(() -> {

            try{
                dagNode.getNodeLock().lock();
                dagNode.nodeSyncDataChangedCondition.await();
                assertThat(true, is(true));
                isSignalRight.set(true);
            }
            catch (Exception e){
                e.printStackTrace();
            }
            finally {

                dagNode.getNodeLock().unlock();
            }
        });

        t1.start();
        Thread.sleep(2000);

        dagNode.relationshipIncrementTotalItemsCount(parentNode);
        dagNode.relationshipIncrementFailedItemsCount(parentNode);
        dagNode.setRelationshipFailed(parentNode);
        dagNode.setNodeFailed();
        t1.join();

        assertThat(isSignalRight.get(), is(true));

    }

    @Test
    public void testDagNodeSendSignalWhenThereIsDataInAvailableWhenNodeIsAutoClosing() throws InterruptedException {

        InfraDbList mockedInfraDbList = Mockito.mock(InfraDbList.class);
        InfraDbKeyValue mockedInfraDbKeyValue = Mockito.mock(InfraDbKeyValue.class);


        DagNode dagNode = new DagNode("1");
        dagNode.configInfra(mockedInfraDbList, mockedInfraDbKeyValue);
        AtomicReference<Boolean> isSignalRight = new AtomicReference<>(false);
        Thread t1 = new Thread(() -> {

            try{
                dagNode.getNodeLock().lock();
                dagNode.nodeSyncDataChangedCondition.await();
                assertThat(true, is(true));
                isSignalRight.set(true);
            }
            catch (Exception e){
                e.printStackTrace();
            }
            finally {

                dagNode.getNodeLock().unlock();
            }
        });

        t1.start();
        Thread.sleep(2000);

        try(dagNode){

        }
        catch (Exception e){
            e.printStackTrace();
        }
        finally {


        }
        t1.join();

        assertThat(isSignalRight.get(), is(true));

    }

    @Test
    public void testCloneDagWithOneNode() throws Exception {

        DagNode dagNode = new DagNode("1");
        DagNode clonedDag = DagNode.cloneDag(dagNode);
        assertThat(clonedDag.getName(), is(dagNode.getName()));
        assertThat(clonedDag.getIsCloned(), is(true));
        assertThat(dagNode.getIsCloned(), is(false));
        assertThat(clonedDag.hashCode(), is(not(dagNode.hashCode())));
    }

    @Test
    public void testCloneDagWithOneChild() throws Exception {

        DagNode dagNodeOne = new DagNode("1");
        DagNode dagNodeTwo = new DagNode("2");
        dagNodeTwo.setParent(dagNodeOne);

        DagNode clonedDag = DagNode.cloneDag(dagNodeOne);
        assertThat(clonedDag.getName(), is(dagNodeOne.getName()));
        assertThat(clonedDag.getIsCloned(), is(true));
        assertThat(dagNodeOne.getIsCloned(), is(false));
        assertThat(clonedDag.hashCode(), is(not(dagNodeOne.hashCode())));

        assertThat(clonedDag.getChildrenRelationshipMap().keySet().size(), is(1));
        List<DagNode> x = new ArrayList<>(clonedDag.getChildrenRelationshipMap().keySet());
        DagNode secondClonedChild = x.get(0);

        assertThat(secondClonedChild.getName(), is(dagNodeTwo.getName()));
        assertThat(secondClonedChild.getIsCloned(), is(true));
        assertThat(secondClonedChild.hashCode(), is(not(dagNodeTwo.hashCode())));
    }

    @Test
    public void testCloneDagWithMultipleChild() throws Exception {

        DagNode dagNodeOne = new DagNode("1");
        DagNode dagNodeTwo = new DagNode("2");
        DagNode dagNodeThree = new DagNode("3");
        dagNodeTwo.setParent(dagNodeOne);
        dagNodeThree.setParent(dagNodeOne);

        DagNode clonedDag = DagNode.cloneDag(dagNodeOne);

        assertThat(clonedDag.getName(), is(dagNodeOne.getName()));
        assertThat(clonedDag.getIsCloned(), is(true));
        assertThat(dagNodeOne.getIsCloned(), is(false));
        assertThat(clonedDag.hashCode(), is(not(dagNodeOne.hashCode())));

        assertThat(clonedDag.getChildrenRelationshipMap().keySet().size(), is(2));

        DagNode secondClonedChild = clonedDag.getChildrenRelationshipMap().keySet().stream().findFirst().get();
        assertThat(secondClonedChild.getName(), is(dagNodeTwo.getName()));
        assertThat(secondClonedChild.getIsCloned(), is(true));
        assertThat(secondClonedChild.hashCode(), is(not(dagNodeTwo.hashCode())));

        DagNode thirdClonedChild = clonedDag.getChildrenRelationshipMap().keySet().stream().skip(1).findFirst().get();
        assertThat(thirdClonedChild.getName(), is(dagNodeThree.getName()));
        assertThat(thirdClonedChild.getIsCloned(), is(true));
        assertThat(thirdClonedChild.hashCode(), is(not(dagNodeThree.hashCode())));
    }

    @Test
    public void testCloneDagWithMultipleParentSetup() throws Exception {

        DagNode dagNodeOne = new DagNode("1");
        DagNode dagNodeTwo = new DagNode("2");
        DagNode dagNodeThree = new DagNode("3");
        dagNodeTwo.setParent(dagNodeOne);
        dagNodeThree.setParent(dagNodeOne);
        dagNodeThree.setParent(dagNodeTwo);

        DagNode clonedDag = DagNode.cloneDag(dagNodeOne);

        assertThat(clonedDag.getName(), is(dagNodeOne.getName()));
        assertThat(clonedDag.getIsCloned(), is(true));
        assertThat(dagNodeOne.getIsCloned(), is(false));
        assertThat(clonedDag.hashCode(), is(not(dagNodeOne.hashCode())));

        assertThat(clonedDag.getChildrenRelationshipMap().keySet().size(), is(2));
        DagNode secondClonedChild = clonedDag.getChildrenRelationshipMap().keySet().stream().findFirst().get();
        assertThat(secondClonedChild.getChildrenRelationshipMap().keySet().size(), is(1));
        DagNode thirdClonedChild = clonedDag.getChildrenRelationshipMap().keySet().stream().skip(1).findFirst().get();
        assertThat(thirdClonedChild.getChildrenRelationshipMap().keySet().size(), is(0));

        assertThat(secondClonedChild.getName(), is(dagNodeTwo.getName()));
        assertThat(secondClonedChild.getIsCloned(), is(true));
        assertThat(secondClonedChild.hashCode(), is(not(dagNodeTwo.hashCode())));


        assertThat(thirdClonedChild.getName(), is(dagNodeThree.getName()));
        assertThat(thirdClonedChild.getIsCloned(), is(true));
        assertThat(thirdClonedChild.hashCode(), is(not(dagNodeThree.hashCode())));
    }

    @Test
    public void testCloneDagWithRecursiveSetup() throws Exception {

        DagNode dagNodeOne = new DagNode("1");
        DagNode dagNodeTwo = new DagNode("2");
        DagNode dagNodeThree = new DagNode("3");
        dagNodeOne.addChild(dagNodeTwo);
        dagNodeOne.addChild(dagNodeThree);
        dagNodeTwo.setParent(dagNodeThree);
        dagNodeThree.setParent(dagNodeTwo);

        DagNode clonedDag = DagNode.cloneDag(dagNodeOne);

        assertThat(clonedDag.getName(), is(dagNodeOne.getName()));
        assertThat(clonedDag.getIsCloned(), is(true));
        assertThat(dagNodeOne.getIsCloned(), is(false));
        assertThat(clonedDag.hashCode(), is(not(dagNodeOne.hashCode())));

        assertThat(clonedDag.getChildrenRelationshipMap().size(), is(2));
        DagNode secondClonedChild = clonedDag.getChildrenRelationshipMap().keySet().stream().findFirst().get();
        assertThat(secondClonedChild.getChildrenRelationshipMap().keySet().size(), is(1));
        assertThat(secondClonedChild.getParentRelationshipMap().size(), is(2));
        DagNode thirdClonedChild = clonedDag.getChildrenRelationshipMap().keySet().stream().skip(1).findFirst().get();
        assertThat(thirdClonedChild.getChildrenRelationshipMap().size(), is(1));
        assertThat(thirdClonedChild.getParentRelationshipMap().size(), is(2));

        assertThat(secondClonedChild.getName(), is(dagNodeTwo.getName()));
        assertThat(secondClonedChild.getIsCloned(), is(true));
        assertThat(secondClonedChild.hashCode(), is(not(dagNodeTwo.hashCode())));


        assertThat(thirdClonedChild.getName(), is(dagNodeThree.getName()));
        assertThat(thirdClonedChild.getIsCloned(), is(true));
        assertThat(thirdClonedChild.hashCode(), is(not(dagNodeThree.hashCode())));
    }

    @Test
    public void testCloneDagWithSelfRecursiveSetup() throws Exception {

        DagNode dagNodeOne = new DagNode("1");
        DagNode dagNodeTwo = new DagNode("2");
        DagNode dagNodeThree = new DagNode("3");
        DagNode dagNodeFour = new DagNode("4");
        dagNodeOne.addChild(dagNodeTwo);
        dagNodeOne.addChild(dagNodeThree);
        dagNodeTwo.setParent(dagNodeThree);
        dagNodeThree.setParent(dagNodeTwo);
        dagNodeThree.addChild(dagNodeFour);
        dagNodeFour.setParent(dagNodeFour);

        DagNode clonedDag = DagNode.cloneDag(dagNodeOne);

        assertThat(clonedDag.getName(), is(dagNodeOne.getName()));
        assertThat(clonedDag.getIsCloned(), is(true));
        assertThat(dagNodeOne.getIsCloned(), is(false));
        assertThat(clonedDag.hashCode(), is(not(dagNodeOne.hashCode())));

        assertThat(clonedDag.getChildrenRelationshipMap().keySet().size(), is(2));
        DagNode secondClonedChild = clonedDag.getChildrenRelationshipMap().keySet().stream().findFirst().get();
        assertThat(secondClonedChild.getChildrenRelationshipMap().keySet().size(), is(1));
        assertThat(secondClonedChild.getParentRelationshipMap().keySet().size(), is(2));
        DagNode thirdClonedChild = clonedDag.getChildrenRelationshipMap().keySet().stream().skip(1).findFirst().get();
        assertThat(thirdClonedChild.getChildrenRelationshipMap().keySet().size(), is(2));
        assertThat(thirdClonedChild.getParentRelationshipMap().keySet().size(), is(2));

        DagNode FourthClonedChild = thirdClonedChild.getChildrenRelationshipMap().keySet().stream().findFirst().get();
        assertThat(FourthClonedChild.getChildrenRelationshipMap().keySet().size(), is(1));
        assertThat(FourthClonedChild.getParentRelationshipMap().keySet().size(), is(2));

        assertThat(secondClonedChild.getName(), is(dagNodeTwo.getName()));
        assertThat(secondClonedChild.getIsCloned(), is(true));
        assertThat(secondClonedChild.hashCode(), is(not(dagNodeTwo.hashCode())));
        assertThat(secondClonedChild.getIsCloned(), is(true));


        assertThat(thirdClonedChild.getName(), is(dagNodeThree.getName()));
        assertThat(thirdClonedChild.getIsCloned(), is(true));
        assertThat(thirdClonedChild.hashCode(), is(not(dagNodeThree.hashCode())));
        assertThat(thirdClonedChild.getIsCloned(), is(true));
    }

    @Test
    public void testBiDirectionalRelationshipObjectsAreCreated() throws Exception{

        DagNode dagNode1 = new DagNode("1");
        DagNode dagNode2 = new DagNode("2");
        DagNode dagNode3 = new DagNode("3");
        DagNode dagNode4 = new DagNode("4");
        DagNode dagNode5 = new DagNode("5");

        dagNode1.addChild(dagNode2);
        dagNode1.addChild(dagNode3);
        dagNode3.addChild(dagNode4);
        dagNode3.addChild(dagNode5);

        assertThat(dagNode1.getParentRelationshipMap().keySet().size(), is(0));
        assertThat(dagNode1.getChildrenRelationshipMap().keySet().size(), is(2));
        assertThat(dagNode2.getParentRelationshipMap().keySet().size(), is(1));
        assertThat(dagNode2.getChildrenRelationshipMap().keySet().size(), is(0));
        assertThat(dagNode3.getParentRelationshipMap().keySet().size(), is(1));
        assertThat(dagNode3.getChildrenRelationshipMap().keySet().size(), is(2));

        LinkedHashMap<DagNode, Relationship> dagNode1ChildrenRelationship = dagNode1.getChildrenRelationshipMap();
        for(Map.Entry<DagNode, Relationship> rel : dagNode1ChildrenRelationship.entrySet()){
            assertThat(rel.getKey().getName(), in(Arrays.asList(dagNode2.getName(), dagNode3.getName())));
            assertThat(rel.getValue().getStatus(), is(-100));
            assertThat(rel.getValue().getFailedItemsSynced(), is(0L));
            assertThat(rel.getValue().getSuccessfulItemsSynced(), is(0L));
            assertThat(rel.getValue().getTotalItemsSynced(), is(0L));
        }

        LinkedHashMap<DagNode, Relationship> dagNode3ChildrenRelationship = dagNode3.getChildrenRelationshipMap();
        for(Map.Entry<DagNode, Relationship> rel : dagNode3ChildrenRelationship.entrySet()){
            assertThat(rel.getKey().getName(), in(Arrays.asList(dagNode4.getName(), dagNode5.getName())));
            assertThat(rel.getValue().getStatus(), is(-100));
            assertThat(rel.getValue().getFailedItemsSynced(), is(0L));
            assertThat(rel.getValue().getSuccessfulItemsSynced(), is(0L));
            assertThat(rel.getValue().getTotalItemsSynced(), is(0L));
        }
    }

    @Test
    public void testRelationshipDataIsThreadSafe() throws Exception{

        DagNode dagNode1 = new DagNode("1");
        DagNode dagNode2 = new DagNode("2");
        DagNode dagNode3 = new DagNode("3");
        DagNode dagNode4 = new DagNode("4");
        DagNode dagNode5 = new DagNode("5");

        dagNode1.addChild(dagNode2);
        dagNode1.addChild(dagNode3);
        dagNode3.addChild(dagNode4);
        dagNode3.addChild(dagNode5);

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for(int i=0; i<100; i++){
            CompletableFuture<Void> f = CompletableFuture.runAsync(()->{
                dagNode3.relationshipIncrementTotalItemsCount(dagNode1);
            });

            futures.add(f);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()])).join();
        assertThat(dagNode3.getRelationshipTotalItemsCount(dagNode1), is(100L));
        futures.clear();

        for(int i=0; i<101; i++){
            CompletableFuture<Void> f = CompletableFuture.runAsync(()->{
                dagNode3.relationshipIncrementSuccessItemsCount(dagNode1);
            });

            futures.add(f);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()])).join();
        assertThat(dagNode3.getRelationshipSuccessfulItemsCount(dagNode1), is(101L));
        futures.clear();

        for(int i=0; i<102; i++){
            CompletableFuture<Void> f = CompletableFuture.runAsync(()->{
                dagNode3.relationshipIncrementFailedItemsCount(dagNode1);
            });

            futures.add(f);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()])).join();
        assertThat(dagNode3.getRelationshipFailedItemsCount(dagNode1), is(102L));
        futures.clear();
    }

    @Test
    public void testDagWhenItDoesNotContainsAnyCycle() throws Exception {

        DagNode dagNode1 = new DagNode("1");
        DagNode dagNode2 = new DagNode("2");
        DagNode dagNode3 = new DagNode("3");
        DagNode dagNode4 = new DagNode("4");
        DagNode dagNode5 = new DagNode("5");

        dagNode1.addChild(dagNode2);
        dagNode1.addChild(dagNode3);
        dagNode3.addChild(dagNode4);
        dagNode3.addChild(dagNode5);

        List<NodesCycle> nodesCycles = DagNode.getNodesCycleInDag(dagNode1);
        assertThat(nodesCycles.size(), is(0));
    }

    @Test
    public void testDagWhenItDoesContainsSelfCycle() throws Exception {

        DagNode dagNode1 = new DagNode("1");
        DagNode dagNode2 = new DagNode("2");
        DagNode dagNode3 = new DagNode("3");

        dagNode1.addChild(dagNode2);
        dagNode1.addChild(dagNode3);
        dagNode3.addChild(dagNode3);

        List<NodesCycle> nodesCycles = DagNode.getNodesCycleInDag(dagNode1);
        assertThat(nodesCycles.size(), is(1));
        assertThat(nodesCycles.get(0).getNodesInCycle().get(0), is(dagNode3));
        assertThat(nodesCycles.get(0).getCycleEntryPoints().get(0).getParentNode(), is(dagNode1));
        assertThat(nodesCycles.get(0).getCycleEntryPoints().get(0).getCycleNode(), is(dagNode3));
    }

    @Test
    public void testDagWhenItDoesContainsOneCycle() throws Exception {

        DagNode dagNode1 = new DagNode("1");
        DagNode dagNode2 = new DagNode("2");
        DagNode dagNode3 = new DagNode("3");
        DagNode dagNode4 = new DagNode("4");
        DagNode dagNode5 = new DagNode("5");

        dagNode1.addChild(dagNode2);
        dagNode1.addChild(dagNode3);
        dagNode3.addChild(dagNode4);
        dagNode4.addChild(dagNode5);
        dagNode5.addChild(dagNode3);

        List<NodesCycle> nodesCycles = DagNode.getNodesCycleInDag(dagNode1);
        assertThat(nodesCycles.size(), is(1));
        assertThat(nodesCycles.get(0).getNodesInCycle().size(), is(3));
        assertThat(nodesCycles.get(0).getCycleEntryPoints().get(0).getParentNode(), is(dagNode1));
        assertThat(nodesCycles.get(0).getCycleEntryPoints().get(0).getCycleNode(), is(dagNode3));
    }

    @Test
    public void testDagWhenItDoesContainsTwoSeparatedDisjointCycle() throws Exception {

        DagNode dagNode1 = new DagNode("1");
        DagNode dagNode2 = new DagNode("2");
        DagNode dagNode3 = new DagNode("3");
        DagNode dagNode4 = new DagNode("4");
        DagNode dagNode5 = new DagNode("5");
        DagNode dagNode6 = new DagNode("6");
        DagNode dagNode7 = new DagNode("7");

        dagNode1.addChild(dagNode2);
        dagNode1.addChild(dagNode3);
        dagNode3.addChild(dagNode4);
        dagNode4.addChild(dagNode5);
        dagNode5.addChild(dagNode3);

        dagNode2.addChild(dagNode6);
        dagNode6.addChild(dagNode7);
        dagNode7.addChild(dagNode2);

        List<NodesCycle> nodesCycles = DagNode.getNodesCycleInDag(dagNode1);
        assertThat(nodesCycles.size(), is(2));

        assertThat(nodesCycles.get(0).getNodesInCycle().size(), is(3));
        assertThat(nodesCycles.get(0).getCycleEntryPoints().get(0).getParentNode(), is(dagNode1));
        assertThat(nodesCycles.get(0).getCycleEntryPoints().get(0).getCycleNode(), is(dagNode3));

        assertThat(nodesCycles.get(1).getNodesInCycle().size(), is(3));
        assertThat(nodesCycles.get(1).getCycleEntryPoints().get(0).getParentNode(), is(dagNode1));
        assertThat(nodesCycles.get(1).getCycleEntryPoints().get(0).getCycleNode(), is(dagNode2));
    }

    @Test
    public void testDagWhenItDoesContainsTwoConnectedDisjointCycle() throws Exception {

        DagNode dagNode1 = new DagNode("1");
        DagNode dagNode2 = new DagNode("2");
        DagNode dagNode3 = new DagNode("3");
        DagNode dagNode4 = new DagNode("4");
        DagNode dagNode5 = new DagNode("5");
        DagNode dagNode6 = new DagNode("6");
        DagNode dagNode7 = new DagNode("7");
        DagNode dagNode8 = new DagNode("8");

        dagNode1.addChild(dagNode2);
        dagNode1.addChild(dagNode3);
        dagNode3.addChild(dagNode4);
        dagNode4.addChild(dagNode5);
        dagNode5.addChild(dagNode3);

        dagNode2.addChild(dagNode6);
        dagNode6.addChild(dagNode7);
        dagNode7.addChild(dagNode2);

        dagNode8.addChild(dagNode7);
        dagNode8.addChild(dagNode4);

        List<NodesCycle> nodesCycles = DagNode.getNodesCycleInDag(dagNode1);
        assertThat(nodesCycles.size(), is(2));

        assertThat(nodesCycles.get(0).getNodesInCycle().size(), is(3));
        assertThat(nodesCycles.get(0).getCycleEntryPoints().size(), is(2));

        assertThat(nodesCycles.get(0).getCycleEntryPoints().get(0).getParentNode(), is(dagNode1));
        assertThat(nodesCycles.get(0).getCycleEntryPoints().get(0).getCycleNode(), is(dagNode3));

        assertThat(nodesCycles.get(0).getCycleEntryPoints().get(1).getParentNode(), is(dagNode8));
        assertThat(nodesCycles.get(0).getCycleEntryPoints().get(1).getCycleNode(), is(dagNode4));


        assertThat(nodesCycles.get(1).getNodesInCycle().size(), is(3));
        assertThat(nodesCycles.get(1).getCycleEntryPoints().size(), is(2));
        assertThat(nodesCycles.get(1).getCycleEntryPoints().get(0).getParentNode(), is(dagNode1));
        assertThat(nodesCycles.get(1).getCycleEntryPoints().get(0).getCycleNode(), is(dagNode2));

        assertThat(nodesCycles.get(1).getCycleEntryPoints().get(1).getParentNode(), is(dagNode8));
        assertThat(nodesCycles.get(1).getCycleEntryPoints().get(1).getCycleNode(), is(dagNode7));

    }

    @Test
    public void testDagWhenItDoesContainsOneConcentricCycle() throws Exception {

        DagNode dagNode1 = new DagNode("1");
        DagNode dagNode2 = new DagNode("2");
        DagNode dagNode3 = new DagNode("3");
        DagNode dagNode4 = new DagNode("4");

        dagNode1.addChild(dagNode2);
        dagNode2.addChild(dagNode3);
        dagNode3.addChild(dagNode1);
        dagNode4.addChild(dagNode2);
        dagNode3.addChild(dagNode4);

        List<NodesCycle> nodesCycles = DagNode.getNodesCycleInDag(dagNode1);
        assertThat(nodesCycles.size(), is(2));

        assertThat(nodesCycles.get(0).getNodesInCycle().size(), is(3));
        assertThat(nodesCycles.get(0).getCycleEntryPoints().size(), is(1));
        assertThat(nodesCycles.get(0).getCycleEntryPoints().get(0).getParentNode(), is(dagNode4));
        assertThat(nodesCycles.get(0).getCycleEntryPoints().get(0).getCycleNode(), is(dagNode2));

        assertThat(nodesCycles.get(1).getNodesInCycle().size(), is(3));
        assertThat(nodesCycles.get(1).getCycleEntryPoints().size(), is(1));
        assertThat(nodesCycles.get(1).getCycleEntryPoints().get(0).getParentNode(), is(dagNode1));
        assertThat(nodesCycles.get(1).getCycleEntryPoints().get(0).getCycleNode(), is(dagNode2));
    }

    @Test
    public void testDagWhenItDoesContainsTwoDisjointAndOneConcentricCycleIsPresent() throws Exception {

        DagNode dagNode1 = new DagNode("1");
        DagNode dagNode2 = new DagNode("2");
        DagNode dagNode3 = new DagNode("3");
        DagNode dagNode4 = new DagNode("4");
        DagNode dagNode5 = new DagNode("5");
        DagNode dagNode6 = new DagNode("6");
        DagNode dagNode7 = new DagNode("7");

        // This will give one cycle
        dagNode1.addChild(dagNode2);
        dagNode1.addChild(dagNode3);
        dagNode3.addChild(dagNode4);
        dagNode4.addChild(dagNode5);
        dagNode5.addChild(dagNode3);

        // This will give concentric cycles
        dagNode5.addChild(dagNode1);

        // This will give another cycles
        dagNode2.addChild(dagNode6);
        dagNode6.addChild(dagNode7);
        dagNode7.addChild(dagNode2);

        List<NodesCycle> nodesCycles = DagNode.getNodesCycleInDag(dagNode1);
        assertThat(nodesCycles.size(), is(3));

        assertThat(nodesCycles.get(0).getNodesInCycle().size(), is(3));
        assertThat(nodesCycles.get(0).getCycleEntryPoints().get(0).getParentNode(), is(dagNode1));
        assertThat(nodesCycles.get(0).getCycleEntryPoints().get(0).getCycleNode(), is(dagNode3));

        assertThat(nodesCycles.get(1).getNodesInCycle().size(), is(4));
        assertThat(nodesCycles.get(1).getCycleEntryPoints().isEmpty(), is(true));

        assertThat(nodesCycles.get(2).getNodesInCycle().size(), is(3));
        assertThat(nodesCycles.get(2).getCycleEntryPoints().get(0).getParentNode(), is(dagNode1));
        assertThat(nodesCycles.get(2).getCycleEntryPoints().get(0).getCycleNode(), is(dagNode2));
    }

    @Test
    public void testWhetherBiggestPossibleCyclesIsDetectedInsteadOfSmallerOnes(){

        DagNode dagNode0 = new DagNode("0");
        DagNode dagNode1 = new DagNode("1");
        DagNode dagNode2 = new DagNode("2");
        DagNode dagNode3 = new DagNode("3");
        DagNode dagNode4 = new DagNode("4");
        DagNode dagNode5 = new DagNode("5");
        DagNode dagNode6 = new DagNode("6");

        dagNode0.addChild(dagNode1);
        dagNode1.addChild(dagNode2);
        dagNode2.addChild(dagNode3);
        dagNode3.addChild(dagNode4);
        dagNode3.addChild(dagNode1);
        dagNode4.addChild(dagNode5);
        dagNode5.addChild(dagNode6);
        dagNode6.addChild(dagNode3);

        List<NodesCycle> nodesCycles = DagNode.getNodesCycleInDag(dagNode0);
        assertThat(nodesCycles.size(), is(2));
    }

    @Test
    public void testWhetherBiggestPossibleCyclesIsDetectedInsteadOfSmallerOnesCase2(){

        DagNode dagNode0 = new DagNode("0");
        DagNode dagNode1 = new DagNode("1");
        DagNode dagNode2 = new DagNode("2");
        DagNode dagNode3 = new DagNode("3");
        DagNode dagNode4 = new DagNode("4");
        DagNode dagNode5 = new DagNode("5");
        DagNode dagNode6 = new DagNode("6");

        dagNode0.addChild(dagNode1);
        dagNode0.addChild(dagNode2);
        dagNode1.addChild(dagNode3);
        dagNode3.addChild(dagNode4);
        dagNode4.addChild(dagNode5);
        dagNode5.addChild(dagNode6);
        dagNode6.addChild(dagNode2);
        dagNode6.addChild(dagNode3);
        dagNode2.addChild(dagNode1);

        List<NodesCycle> nodesCycles = DagNode.getNodesCycleInDag(dagNode0);
        assertThat(nodesCycles.size(), is(2));
    }

    @Test
    public void testDuplicateCyclesAreNotCreated(){

        DagNode dagNode1 = new DagNode("1");
        DagNode dagNode2 = new DagNode("2");
        DagNode dagNode3 = new DagNode("3");
        DagNode dagNode4 = new DagNode("4");
        DagNode dagNode5 = new DagNode("5");

        dagNode1.addChild(dagNode2);
        dagNode1.addChild(dagNode3);
        dagNode2.addChild(dagNode4);
        dagNode4.addChild(dagNode5);
        dagNode5.addChild(dagNode3);
        dagNode3.addChild(dagNode2);

        List<NodesCycle> nodesCycles = DagNode.getNodesCycleInDag(dagNode1);
        assertThat(nodesCycles.size(), is(1));


    }

}
