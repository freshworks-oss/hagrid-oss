package com.freshworks.core.traverser;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.concurrent.Phaser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.freshworks.core.shared.MockFacadeSyncServiceContainer;
import com.freshworks.core.shared.NamespaceService;
import com.freshworks.core.shared.SimpleMockUtility;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.executor.SharedExecutorService;
import com.freshworks.core.shared.infra.InfraConfigService;
import com.freshworks.core.shared.infra.InfraService;
import com.freshworks.core.shared.infra.MockFacadeInfraConfigService;
import com.freshworks.core.shared.infra.nitrite.MockFacadeNitriteDbService;
import com.freshworks.core.shared.sync.MockFacadeSyncStatusService;
import com.freshworks.core.shared.sync.SyncStatusService;
import com.google.common.collect.ImmutableMap;

@SpringBootTest
@EnabledIfSystemProperty(named = "spring.profiles.active", matches = ".*\\.unit\\..*")
public class TestDagTraversalService {

    @Autowired
    SimpleMockUtility traverserMockUtility;

    @Autowired
    MockFacadeDagNode mockFacadeDagNode;

    @Autowired
    MockFacadeNitriteDbService nitriteDbServiceFacade;

    @Autowired
    MockFacadeSyncServiceContainer mockFacadeSyncServiceContainer;

    @Autowired
    MockFacadeSyncStatusService mockFacadeSyncStatusService;

    @Autowired
    MockFacadeInfraConfigService mockFacadeInfraConfigService;

    @Autowired
    DagTraversalService dagTraversalService;

    TraverserExecutorService traverserExecutorService;

    String releaseVersion;

    Class<? extends AbstractStep> appRoleAssignmentStep;
    Class<? extends AbstractStep> application;
    Class<? extends AbstractStep> servicePrinciple;

    @BeforeEach
    public void beforeEach() throws Exception {
        releaseVersion = System.getProperty("spring.profiles.active").split("\\.")[0];

        mockFacadeDagNode.configure().build();
        nitriteDbServiceFacade.configure().build();
        mockFacadeSyncServiceContainer.configure().build();
        mockFacadeSyncStatusService.configure().build();
        mockFacadeInfraConfigService.configure().build();

        appRoleAssignmentStep = (Class<? extends AbstractStep>) Class.forName("com.freshworks.core.data." + releaseVersion + ".unit.dag.steps.TestAppRoleAssignment");
        application = (Class<? extends AbstractStep>) Class.forName("com.freshworks.core.data." + releaseVersion + ".unit.dag.steps.TestApplication");
        servicePrinciple = (Class<? extends AbstractStep>) Class.forName("com.freshworks.core.data." + releaseVersion + ".unit.dag.steps.TestServicePrinciple");
    }


    @Test
    public void testDagTraverseAllNodesOfDag() throws Exception {

        Phaser mockedPhaser = Mockito.mock(Phaser.class);

        DagNode parentNode = mockFacadeDagNode
                .name(application)
                .build();

        mockFacadeDagNode.configure();

        DagNode childNode = mockFacadeDagNode
                .name(servicePrinciple)
                .build();
        parentNode.addChild(childNode);

        mockFacadeDagNode.configure();

        DagNode childNode2 = mockFacadeDagNode
                .name(appRoleAssignmentStep)
                        .build();
        parentNode.addChild(childNode2);


        NamespaceService namespace = new NamespaceService();
        namespace.setNamespace("dummy_name_space");

        SyncStatusService syncStatusService = mockFacadeSyncStatusService.build();
        InfraConfigService infraConfigService = mockFacadeInfraConfigService.build();
        InfraService nitriteService = nitriteDbServiceFacade.build();
        traverserExecutorService = traverserMockUtility.mockTraverserExecutorService();

        SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer
                .add(syncStatusService, SyncStatusService.class)
                .add(infraConfigService, InfraConfigService.class)
                .add(nitriteService, InfraService.class)
                .add(namespace, NamespaceService.class)
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
