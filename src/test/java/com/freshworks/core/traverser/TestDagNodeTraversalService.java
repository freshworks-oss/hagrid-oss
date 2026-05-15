package com.freshworks.core.traverser;

import com.freshworks.core.shared.MockFacadeSyncServiceContainer;
import com.freshworks.core.shared.SimpleMockUtility;
import com.freshworks.core.shared.infra.persistent.MockFacadeMongoDbService;
import com.freshworks.core.shared.infra.persistent.MockFacadeMongodbList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.mockito.ArgumentMatchers.any;
import static org.hamcrest.MatcherAssert.assertThat;

@SpringBootTest
@EnabledIfSystemProperty(named = "spring.profiles.active", matches = ".*\\.unit\\..*")
public class TestDagNodeTraversalService {

    @Autowired
    SimpleMockUtility lowLevelMockUtility;

    @Autowired
    MockFacadeDagNodeTraversal mockFacadeDagNodeTraversal;

    @Autowired
    MockFacadeMongoDbService mockFacadeMongoDbService;

    @Autowired
    MockFacadeMongodbList mockFacadeMongodbList;

    @Autowired
    MockFacadeTraverseConfigService mockFacadeTraverseConfigService;

    @Autowired
    MockFacadeDagNode dagNodeMockFacade;

    @Autowired
    MockFacadeSyncServiceContainer mockFacadeSyncServiceContainer;

    private DagNodeTraversalService dagNodeTraversalService;


    @BeforeEach
    public void Mock() throws Exception {

        mockFacadeDagNodeTraversal.configure().build();
        mockFacadeMongoDbService.configure().build();
        mockFacadeMongodbList.configure().build();
        mockFacadeTraverseConfigService.configure().build();
        dagNodeMockFacade.configure().build();
        mockFacadeSyncServiceContainer.configure().build();
    }



//    @Test
//    public void testDagNodeTraverserCreatesNewStepObjectWhenStepIsSingletonForEveryParentObject() throws Exception {
//
//        Phaser mockedPhaser = Mockito.mock(Phaser.class);
//
//        List<AbstractStep> list = new ArrayList<>();
//        doAnswer(new Answer<Void>() {
//
//            @Override
//            public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
//
//                AbstractStep abstractStep  = (AbstractStep) invocationOnMock.callRealMethod();
//                list.add(abstractStep);
//                return null;
//            }
//        }).when(syncServiceContainer).getBean(TestSingleApplicationSingletonStep.class.getName());
//
//
//        // Run 1st time
//        // So that we run whole test cases on in memory
//        InmemoryService inmemoryService = new InmemoryService(syncServiceContainer);
//        nodeToTraverse = new DagNode(TestSingleApplicationSingletonStep.class.getName());
//        ImmutableMap<String, String> baggageMap = ImmutableMap.<String, String>builder().put("","").build();
//        CountDownLatch countDownLatch = new CountDownLatch(1);
//        dagNodeTraversalService.configure(syncServiceContainer, nodeToTraverse, nodeToTraverse, inmemoryService, traverseConfigService, baggageMap, countDownLatch);
//
//        Thread s = new Thread(()->{
//            try{
//                dagNodeTraversalService.traverse(nodeToTraverse, baggageMap, mockedPhaser);
//            }
//            catch (Exception e){
//
//            }
//
//        });
//
//        s.start();
//        s.join();
//
//
//        // Run 2nd time
//        // So that we run whole test cases on in memory
//        InmemoryService inMemoryService1 = new InmemoryService(syncServiceContainer);
//        nodeToTraverse = new DagNode(TestSingleApplicationSingletonStep.class.getName());
//        ImmutableMap<String, String> baggageMap1 = ImmutableMap.<String, String>builder().put("","").build();
//        countDownLatch = new CountDownLatch(1);
//        Phaser mockedPhaser1 = Mockito.mock(Phaser.class);
//
//        dagNodeTraversalService.configure(syncServiceContainer, nodeToTraverse, nodeToTraverse, inMemoryService1, traverseConfigService, baggageMap, countDownLatch);
//
//        Thread s1 = new Thread(()->{
//
//            try {
//                dagNodeTraversalService.traverse(nodeToTraverse, baggageMap1, mockedPhaser1);
//            } catch (Exception e) {
//                throw new RuntimeException(e);
//            }
//        });
//
//
//        s1.start();
//        s1.join();
//
//        assertThat(list.size(), is(2));
//        assertThat(list.get(0), instanceOf(TestSingleApplicationSingletonStep.class));
//        assertThat(list.get(1), instanceOf(TestSingleApplicationSingletonStep.class));
//        assertThat(list.get(0).hashCode(), is(list.get(1).hashCode()));
//    }
//
//    @Test
//    public void testDagNodeTraverserDoesNotProceedUntilAllParentItemsAreTraversed() throws Exception {
//
//        // Run 1st time
//        // So that we run whole test cases on in memory
//        InmemoryService inmemoryService = new InmemoryService(syncServiceContainer);
//        DagNode parentNode = new DagNode(TestApplication.class.getName());
//        DagNode parentNodeSpy = Mockito.spy(parentNode);
//
//        DagNode childNode = new DagNode(TestServicePrinciple.class.getName());
//
//        List<DagNode> list = new ArrayList<>();
//        list.add(childNode);
//        parentNodeSpy.setChildren(list);
//        childNode.setParent(parentNodeSpy);
//
//        List<String> parentSyncResults = new ArrayList<>();
//        parentSyncResults.add("{}");
//        parentSyncResults.add("{}");
//        doReturn(parentSyncResults).when(parentNodeSpy).getSyncResult(anyInt(), anyInt());
//        doReturn(true, false).when(parentNodeSpy).hasMoreData(anyInt(), any());
//
//        ImmutableMap<String, String> baggageMap = ImmutableMap.<String, String>builder().put("","").build();
//        CountDownLatch countDownLatch = new CountDownLatch(2);
//        Phaser phaser = new Phaser();
//        dagNodeTraversalService.configure(syncServiceContainer, childNode, parentNodeSpy, inmemoryService, traverseConfigService, baggageMap, countDownLatch);
//
//        Thread s = new Thread(()->{
//            try {
//                dagNodeTraversalService.traverse(childNode, baggageMap, phaser);
//            } catch (Exception e) {
//                throw new RuntimeException(e);
//            }
//        });
//
//        s.start();
//
//        // I am waiting for DagNodeTraversal to traverse
//        Thread.sleep(3000);
//
//
//        // Here I am mocking that per item traverser has completed this task.
//        phaser.arriveAndDeregister();
//
//        // As we have two parent items, even if 1 parent item is done, dag traversal should not bring count down latch down.
//        assertThat(countDownLatch.getCount(), is(2L));
//
//        // Here I am mocking another per item traverser has completed this task.
//        phaser.arriveAndDeregister();
//
//        // Giving time to DagNodeTraversal to bring down countdown latch
//        Thread.sleep(2000);
//
//        // As we have two parent items, even if 1 parent item is done, dag traversal should not bring count down latch down.
//        assertThat(countDownLatch.getCount(), is(1L));
//
//        s.join();
//    }

}
