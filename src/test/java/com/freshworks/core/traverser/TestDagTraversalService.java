package com.freshworks.core.traverser;

import com.freshworks.core.data.four_zero_zero.unit.dag.steps.TestAppRoleAssignment;
import com.freshworks.core.data.four_zero_zero.unit.dag.steps.TestApplication;
import com.freshworks.core.data.four_zero_zero.unit.dag.steps.TestServicePrinciple;
import com.freshworks.core.shared.MockFacadeSyncServiceContainer;
import com.freshworks.core.shared.SimpleMockUtility;
import com.freshworks.core.shared.executor.SharedExecutorService;
import com.freshworks.core.shared.infra.InfraConfigService;
import com.freshworks.core.shared.infra.MockFacadeInfraConfigService;
import com.freshworks.core.shared.infra.persistent.MockFacadeMongoDbService;
import com.freshworks.core.shared.Namespace;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.infra.InfraService;
import com.freshworks.core.shared.sync.MockFacadeSyncStatusService;
import com.freshworks.core.shared.sync.SyncStatusService;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Phaser;

@SpringBootTest
@EnabledIfSystemProperty(named = "spring.profiles.active", matches = ".*\\.unit\\..*")
public class TestDagTraversalService {

    @Autowired
    SimpleMockUtility traverserMockUtility;

    @Autowired
    MockFacadeDagNode mockFacadeDagNode;

    @Autowired
    MockFacadeMongoDbService mongoDbServiceFacade;

    @Autowired
    MockFacadeSyncServiceContainer mockFacadeSyncServiceContainer;

    @Autowired
    MockFacadeSyncStatusService mockFacadeSyncStatusService;

    @Autowired
    MockFacadeInfraConfigService mockFacadeInfraConfigService;

    @Autowired
    DagTraversalService dagTraversalService;

    TraverserExecutorService traverserExecutorService;
    @Autowired
    private MockFacadeMongoDbService mockFacadeMongoDbService;


    @BeforeEach
    public void beforeEach() throws Exception {

        mockFacadeDagNode.configure().build();
        mongoDbServiceFacade.configure().build();
        mockFacadeSyncServiceContainer.configure().build();
        mockFacadeSyncStatusService.configure().build();
        mockFacadeInfraConfigService.configure().build();

    }


    @Test
    public void testDagTraverseAllNodesOfDag() throws Exception {

        Phaser mockedPhaser = Mockito.mock(Phaser.class);

        DagNode parentNode = mockFacadeDagNode
                .name(TestApplication.class)
                .build();

        mockFacadeDagNode.configure();

        DagNode childNode = mockFacadeDagNode
                .name(TestServicePrinciple.class)
                .build();
        parentNode.addChild(childNode);

        mockFacadeDagNode.configure();

        DagNode childNode2 = mockFacadeDagNode
                .name(TestAppRoleAssignment.class)
                        .build();
        parentNode.addChild(childNode2);


        Namespace namespace = new Namespace();
        namespace.setNamespace("dummy_name_space");

        SyncStatusService syncStatusService = mockFacadeSyncStatusService.build();
        InfraConfigService infraConfigService = mockFacadeInfraConfigService.build();
        InfraService mongoService = mockFacadeMongoDbService.build();
        traverserExecutorService = traverserMockUtility.mockTraverserExecutorService();

        SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer
                .add(syncStatusService, SyncStatusService.class)
                .add(infraConfigService, InfraConfigService.class)
                .add(mongoService, InfraService.class)
                .add(namespace, Namespace.class)
                .add(traverserExecutorService, TraverserExecutorService.class)
                .build();



        ImmutableMap<String, String> map = ImmutableMap.<String, String>builder().put("", "").build();

        dagTraversalService.configure("/traverser", parentNode, map,mockedPhaser, syncServiceContainer);
        SharedExecutorService sharedExecutorService = syncServiceContainer.getBean(SharedExecutorService.class);

        sharedExecutorService.submit(namespace.getNamespace(), dagTraversalService).get();

        verify(traverserExecutorService, times(3)).submit(anyString(), any());
        verify(syncStatusService, times(1)).setTraverserInSuccessful();

    }


//    @Test
//    public void testDagTraversalWaitUntilAllNodesAreTraversed() throws Exception{
//
//        DagNode parentNode = new DagNode("1");
//        DagNode childNode = new DagNode("2");
//        parentNode.addChild(childNode);
//
//        DagNode childNode2 = new DagNode("3");
//        parentNode.addChild(childNode2);
//
//        CountDownLatch countDownLatch = new CountDownLatch(parentNode.preOrder().size());
//
//        ImmutableMap<String, String> map = ImmutableMap.<String, String>builder().put("", "").build();
//
//        dagTraversalService.configure(syncServiceContainer);
//
//        Thread s1 = new Thread(()->{
//            try{
//                dagTraversalService.traverser(parentNode, map, countDownLatch);
//            }
//            catch(Exception e){
//
//            }
//
//        });
//
//        s1.start();
//
//        Thread.sleep(2000);
//
//        countDownLatch.countDown();
//        countDownLatch.countDown();
//        countDownLatch.countDown();
//
//
//        verify(traverserExecutorService, times(3)).submit(any());
//        verify(syncStatusService, times(1)).setTraverserInSuccessful();
//
//    }
}
