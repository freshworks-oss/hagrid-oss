package com.freshworks.core.durability;


import com.freshworks.core.data.durability.steps.handle_invalid.null_check.TestHandleInvalidNullCheck;
import com.freshworks.core.data.durability.steps.handle_invalid.shutdown.TestHandleInvalidShutdown;
import com.freshworks.core.data.durability.steps.is_complete.null_check.TestIsSyncCompleteNullCheck;
import com.freshworks.core.data.durability.steps.is_complete.shutdown.TestIsSyncCompleteShutdown;
import com.freshworks.core.data.durability.steps.is_valid.null_check.TestIsValidNullCheck;
import com.freshworks.core.data.durability.steps.is_valid.shutdown.TestIsValidShutdown;
import com.freshworks.core.data.durability.steps.next_request.shutdown.TestGetNextRequestShutdown;
import com.freshworks.core.data.durability.steps.parse_sync.null_check.TestParseSyncNullCheck;
import com.freshworks.core.data.durability.steps.parse_sync.shutdown.TestParseSyncShutdown;
import com.freshworks.core.data.durability.steps.setup.shutdown.TestSetupShutdown;
import com.freshworks.core.data.durability.steps.should_proceed.shutdown.TestShouldProceedShutdown;
import com.freshworks.core.data.durability.steps.start_sync.null_check.TestStartSyncNullCheck;
import com.freshworks.core.data.durability.steps.start_sync.shutdown.TestStartSyncShutdown;
import com.freshworks.core.processor.MockFacadeProcessorService;
import com.freshworks.hagrid.shared.NamespaceService;
import com.freshworks.hagrid.shared.SyncServiceContainer;
import com.freshworks.hagrid.shared.analytics.AnalyticsFactory;
import com.freshworks.hagrid.shared.analytics.AnalyticsService;
import com.freshworks.hagrid.shared.infra.InfraBeanService;
import com.freshworks.hagrid.shared.infra.InfraConfigService;
import com.freshworks.hagrid.shared.infra.InfraService;
import com.freshworks.hagrid.shared.sync.ConnectorConfiguration;
import com.freshworks.hagrid.shared.sync.SyncStatusService;
import com.freshworks.hagrid.shared.synchronizers.GlobalNamespaceService;
import com.freshworks.hagrid.shared.synchronizers.ServiceTree;
import com.freshworks.hagrid.traverser.*;
import com.freshworks.hagrid.traverser.net.http.HttpClientService;
import com.google.common.collect.ImmutableMap;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.mockserver.integration.ClientAndServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@EnabledIfSystemProperty(named = "spring.profiles.active", matches = "durability")
public class TestTraverser {


    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    MockFacadeProcessorService mockFacadeProcessorService;

    private static ClientAndServer server;

    @BeforeAll
    public static void beforeAll(){
//        server = ClientAndServer.startClientAndServer(1080);
    }

    @BeforeEach
    public void beforeEach() throws Exception {
        mockFacadeProcessorService.configure().build();
    }

    @Test
    public void testWhenTraverserIsShutdownByStepMethodSetupThenStepDoesNotExecuteFurtherStepMethods() throws Exception {

        ConnectorConfiguration connectorConfiguration = new ConnectorConfiguration();
        SyncServiceContainer syncServiceContainer = applicationContext.getBean(SyncServiceContainer.class);

        String namespaceStr = UUID.randomUUID().toString();
        NamespaceService namespace = new NamespaceService();
        namespace.setNamespace(namespaceStr);
        syncServiceContainer.add(namespace);

        GlobalNamespaceService singletonUniqueIdentifier = applicationContext.getBean(GlobalNamespaceService.class);
        syncServiceContainer.add(singletonUniqueIdentifier);

        HttpClientService httpClientService = applicationContext.getBean(HttpClientService.class);
        syncServiceContainer.add(httpClientService);

        TraverseConfigService traverseConfigService = applicationContext.getBean(TraverseConfigService.class);
        traverseConfigService.configure(syncServiceContainer);
        syncServiceContainer.add(traverseConfigService);

        InfraBeanService infraBeanConfiguration = applicationContext.getBean(InfraBeanService.class);
        InfraConfigService infraConfigService = applicationContext.getBean(InfraConfigService.class);
        infraConfigService.configure(syncServiceContainer);
        InfraService infraService = infraBeanConfiguration.getInfraService(infraConfigService);
        infraService.configure(syncServiceContainer, infraConfigService);
        syncServiceContainer.add(infraService, InfraService.class);

        AnalyticsFactory analyticsFactory = syncServiceContainer.getBean(AnalyticsFactory.class);
        AnalyticsService analyticsService = analyticsFactory.getAnalyticsService(namespace.getNamespace());
        syncServiceContainer.add(analyticsFactory);

        DagService dagService = applicationContext.getBean(DagService.class);
        dagService.configure(syncServiceContainer);
        DagNode rootNode = dagService.dagStaticStepScanner(namespaceStr, traverseConfigService, infraService);

        List<Class<? extends AbstractStep>> addEnabledPath = new ArrayList<>();
        addEnabledPath.add(TestSetupShutdown.class);
        
        List<List<Class<? extends AbstractStep>>> enabledPathList = new ArrayList<>();
        enabledPathList.add(addEnabledPath);

        // enable only the path added and disable rest of the path
        dagService.enableDisableDagPath(rootNode, enabledPathList);

        // Now set this as main root node
        dagService.setRootNode(rootNode);

        SyncStatusService syncStatusService = syncServiceContainer.getBean(SyncStatusService.class);
        syncServiceContainer.add(syncStatusService);

        ServiceTree serviceTree = applicationContext.getBean(ServiceTree.class);
        serviceTree.configure(syncServiceContainer);
        syncServiceContainer.add(serviceTree);

        assertThat(syncStatusService.getTraverser_status(), Matchers.is(-100));
        DagTraversalService dagTraversalService = applicationContext.getBean(DagTraversalService.class);

        ImmutableMap<String, String> x = ImmutableMap.<String, String>builder()
                .put("numberOfUsersEachPage", "10")
                .put("numberOfUserPagination", "1")
                .put("waitBetweenUserPaginationInMs", "0")
                .put("numberOfPostsEachPage", "10")
                .put("numberOfPostPagination", "1")
                .put("waitBetweenPostPaginationInMs", "0")
                .put("numberOfCommentsEachPage", "100")
                .put("numberOfCommentPagination", "1")
                .put("waitBetweenCommentPaginationInMs", "0")
                .put("numberOfCommunitiesEachPage", "1")
                .put("numberOfCommunityPagination", "1")
                .put("waitBetweenCommunityPaginationInMs", "0").build();

        CountDownLatch latch = new CountDownLatch(rootNode.getNodesInDag().size());
        dagTraversalService.configure("/traverser", rootNode, x, new Phaser(), syncServiceContainer);
        syncServiceContainer.add(dagTraversalService);
        TraverserExecutorService traverserExecutorService = syncServiceContainer.getBean(TraverserExecutorService.class);

        List<Boolean> list = new ArrayList<>();
        AtomicReference<List<Boolean>> methodCalls = new AtomicReference<>();
        methodCalls.set(list);

        analyticsService.registerEventCallback("HAGRID_DURABILITY_EVENT", (Map<String, Object> z)->{
                String step = (String)z.get("step");
            if(step.equals("com.freshworks.core.data.durability.steps.setup.shutdown.TestSetupShutdown")){

                String method = (String)z.get("method");
                if(method.equalsIgnoreCase("setup")){
                    methodCalls.get().add(true);
                }
                else{
                    // fail here
                    methodCalls.get().add(false);
                }
            }

        });


        traverserExecutorService.submit(namespaceStr, dagTraversalService);
        syncStatusService.waitUntilTraverserIsInProgress();
        infraService.destroy();
        assertThat(methodCalls.get().size(), Matchers.is(2));
        assertThat(methodCalls.get().get(0), Matchers.is(false));
        assertThat(methodCalls.get().get(1), Matchers.is(true));
        assertThat(syncStatusService.getTraverser_status(), Matchers.is(-1));
        assertThat(analyticsService.anyErrorEvent(), Matchers.is(true));
        Thread.sleep(10000);
    }

    @Test
    public void testWhenTraverserIsShutdownByStepMethodShouldProceedWithParentThenStepDoesNotExecuteFurtherStepMethods() throws Exception {

        ConnectorConfiguration connectorConfiguration = new ConnectorConfiguration();

        SyncServiceContainer syncServiceContainer = applicationContext.getBean(SyncServiceContainer.class);

        String namespaceStr = UUID.randomUUID().toString();
        NamespaceService namespace = new NamespaceService();
        namespace.setNamespace(namespaceStr);
        syncServiceContainer.add(namespace);

        GlobalNamespaceService singletonUniqueIdentifier = applicationContext.getBean(GlobalNamespaceService.class);
        syncServiceContainer.add(singletonUniqueIdentifier);

        HttpClientService httpClientService = applicationContext.getBean(HttpClientService.class);
        syncServiceContainer.add(httpClientService);

        TraverseConfigService traverseConfigService = applicationContext.getBean(TraverseConfigService.class);
        traverseConfigService.configure(syncServiceContainer);
        syncServiceContainer.add(traverseConfigService);

        InfraBeanService infraBeanConfiguration = applicationContext.getBean(InfraBeanService.class);
        InfraConfigService infraConfigService = applicationContext.getBean(InfraConfigService.class);
        infraConfigService.configure(syncServiceContainer);
        InfraService infraService = infraBeanConfiguration.getInfraService(infraConfigService);
        infraService.configure(syncServiceContainer, infraConfigService);
        syncServiceContainer.add(infraService, InfraService.class);

        AnalyticsFactory analyticsFactory = syncServiceContainer.getBean(AnalyticsFactory.class);
        AnalyticsService analyticsService = analyticsFactory.getAnalyticsService(namespace.getNamespace());
        syncServiceContainer.add(analyticsService);
        syncServiceContainer.add(analyticsFactory);

        DagService dagService = applicationContext.getBean(DagService.class);
        dagService.configure(syncServiceContainer);
        DagNode rootNode = dagService.dagStaticStepScanner(namespaceStr, traverseConfigService, infraService);

        List<Class<? extends AbstractStep>> addEnabledPath = new ArrayList<>();
        addEnabledPath.add(TestShouldProceedShutdown.class);
        
        List<List<Class<? extends AbstractStep>>> enabledPathList = new ArrayList<>();
        enabledPathList.add(addEnabledPath);

        // enable only the path added and disable rest of the path
        dagService.enableDisableDagPath(rootNode, enabledPathList);

        // Now set this as main root node
        dagService.setRootNode(rootNode);

        SyncStatusService syncStatusService = syncServiceContainer.getBean(SyncStatusService.class);
        syncServiceContainer.add(syncStatusService);

        ServiceTree serviceTree = applicationContext.getBean(ServiceTree.class);
        serviceTree.configure(syncServiceContainer);
        syncServiceContainer.add(serviceTree);

        assertThat(syncStatusService.getTraverser_status(), Matchers.is(-100));
        DagTraversalService dagTraversalService = applicationContext.getBean(DagTraversalService.class);

        ImmutableMap<String, String> x = ImmutableMap.<String, String>builder()
                .put("numberOfUsersEachPage", "10")
                .put("numberOfUserPagination", "1")
                .put("waitBetweenUserPaginationInMs", "0")
                .put("numberOfPostsEachPage", "10")
                .put("numberOfPostPagination", "1")
                .put("waitBetweenPostPaginationInMs", "0")
                .put("numberOfCommentsEachPage", "100")
                .put("numberOfCommentPagination", "1")
                .put("waitBetweenCommentPaginationInMs", "0")
                .put("numberOfCommunitiesEachPage", "1")
                .put("numberOfCommunityPagination", "1")
                .put("waitBetweenCommunityPaginationInMs", "0").build();

        CountDownLatch latch = new CountDownLatch(rootNode.getNodesInDag().size());
        dagTraversalService.configure("/traverser", rootNode, x, new Phaser(), syncServiceContainer);
        syncServiceContainer.add(dagTraversalService);
        TraverserExecutorService traverserExecutorService = syncServiceContainer.getBean(TraverserExecutorService.class);

        List<Boolean> list = new ArrayList<>();
        AtomicReference<List<Boolean>> methodCalls = new AtomicReference<>();
        methodCalls.set(list);

        analyticsService.registerEventCallback("HAGRID_DURABILITY_EVENT", (Map<String, Object> z)->{
            String step = (String)z.get("step");
            if(step.equals("com.freshworks.core.data.durability.steps.should_proceed.shutdown.TestShouldProceedShutdown")){

                String method = (String)z.get("method");
                if(method.equalsIgnoreCase("shouldProceedWithParentObject")){
                    methodCalls.get().add(true);
                }
                else{
                    // fail here
                    methodCalls.get().add(false);
                }
            }

        });


        traverserExecutorService.submit(namespaceStr, dagTraversalService);
        syncStatusService.waitUntilTraverserIsInProgress();
        infraService.destroy();
        assertThat(methodCalls.get().size(), Matchers.is(1));
        assertThat(methodCalls.get().get(0), Matchers.is(true));
        assertThat(syncStatusService.getTraverser_status(), Matchers.is(-1));
        assertThat(analyticsService.anyErrorEvent(), Matchers.is(true));
        Thread.sleep(10000);
    }

    /*
     * Commenting this test cases as it is not possible to return null from shouldProceedWithParent as return 
     * type is of boolean 
     */
    // @Test
    // public void testWhenTraverserStepMethodShouldProceedWithParentReturnNullThenStepDoesNotExecuteFurtherStepMethods() throws Exception {

    //     ConnectorConfiguration connectorConfiguration = new ConnectorConfiguration();

    //     SyncServiceContainer syncServiceContainer = applicationContext.getBean(SyncServiceContainer.class);
    //     String namespaceStr = UUID.randomUUID().toString();
    //     NamespaceService namespace = new NamespaceService();
    //     namespace.setNamespace(namespaceStr);
    //     syncServiceContainer.add(namespace);

    //     GlobalNamespaceService singletonUniqueIdentifier = applicationContext.getBean(GlobalNamespaceService.class);
    //     syncServiceContainer.add(singletonUniqueIdentifier);

    //     HttpClientService httpClientService = applicationContext.getBean(HttpClientService.class);
    //     syncServiceContainer.add(httpClientService);

    //     TraverseConfigService traverseConfigService = applicationContext.getBean(TraverseConfigService.class);
    //     traverseConfigService.configure(syncServiceContainer);
    //     syncServiceContainer.add(traverseConfigService);

    //     InfraBeanService infraBeanConfiguration = applicationContext.getBean(InfraBeanService.class);
    //     InfraConfigService infraConfigService = applicationContext.getBean(InfraConfigService.class);
    //     infraConfigService.configure(syncServiceContainer);
    //     InfraService infraService = infraBeanConfiguration.getInfraService(infraConfigService, connectorConfiguration);
    //     infraService.configure(syncServiceContainer, infraConfigService);
    //     syncServiceContainer.add(infraService, InfraService.class);


    //     AnalyticsFactory analyticsFactory = syncServiceContainer.getBean(AnalyticsFactory.class);
    //     AnalyticsService analyticsService = analyticsFactory.getAnalyticsService(namespace.getNamespace());
    //     syncServiceContainer.add(analyticsService);
    //     syncServiceContainer.add(analyticsFactory);

    //     DagService dagService = applicationContext.getBean(DagService.class);
    //     dagService.configure(syncServiceContainer);
    //     DagNode rootNode = dagService.dagScanner(namespaceStr, traverseConfigService, infraService);

    //     SyncStatusService syncStatusService = syncServiceContainer.getBean(SyncStatusService.class);
    //     syncServiceContainer.add(syncStatusService);

    //     ServiceTree serviceTree = applicationContext.getBean(ServiceTree.class);
    //     serviceTree.configure(syncServiceContainer);
    //     syncServiceContainer.add(serviceTree);

    //     assertThat(syncStatusService.getTraverser_status(), Matchers.is(-100));
    //     DagTraversalService dagTraversalService = applicationContext.getBean(DagTraversalService.class);

    //     ImmutableMap<String, String> x = ImmutableMap.<String, String>builder()
    //             .put("numberOfUsersEachPage", "10")
    //             .put("numberOfUserPagination", "1")
    //             .put("waitBetweenUserPaginationInMs", "0")
    //             .put("numberOfPostsEachPage", "10")
    //             .put("numberOfPostPagination", "1")
    //             .put("waitBetweenPostPaginationInMs", "0")
    //             .put("numberOfCommentsEachPage", "100")
    //             .put("numberOfCommentPagination", "1")
    //             .put("waitBetweenCommentPaginationInMs", "0")
    //             .put("numberOfCommunitiesEachPage", "1")
    //             .put("numberOfCommunityPagination", "1")
    //             .put("waitBetweenCommunityPaginationInMs", "0").build();

    //     CountDownLatch latch = new CountDownLatch(rootNode.getNodesInDag().size());
    //     dagTraversalService.configure("/traverser", rootNode, x, new Phaser(), syncServiceContainer);
    //     syncServiceContainer.add(dagTraversalService);
    //     TraverserExecutorService traverserExecutorService = syncServiceContainer.getBean(TraverserExecutorService.class);

    //     List<Boolean> list = new ArrayList<>();
    //     AtomicReference<List<Boolean>> methodCalls = new AtomicReference<>();
    //     methodCalls.set(list);

    //     analyticsService.registerEventCallback("HAGRID_DURABILITY_EVENT", (Map<String, Object> z)->{
    //         String step = (String)z.get("step");
    //         if(step.equals("com.freshworks.core.data.durability.steps.should_proceed.null_check.TestShouldProceedNullCheck")){

    //             String method = (String)z.get("method");
    //             System.out.println("Method called is " + method);
    //             if(method.equalsIgnoreCase("shouldProceedWithParentObject")){
    //                 methodCalls.get().add(true);
    //             }
    //             else{
    //                 // fail here
    //                 methodCalls.get().add(false);
    //             }
    //         }

    //     });


    //     traverserExecutorService.submit(namespaceStr, dagTraversalService);
    //     syncStatusService.waitUntilTraverserIsInProgress();
    //     infraService.destroy();
    //     assertThat(methodCalls.get().size(), Matchers.is(1));
    //     assertThat(methodCalls.get().get(0), Matchers.is(true));
    //     assertThat(syncStatusService.getTraverser_status(), Matchers.is(-1));
    //     assertThat(analyticsService.anyErrorEvent(), Matchers.is(true));
    //     Thread.sleep(10000);
    // }

    @Test
    public void testWhenTraverserIsShutdownByStepMethodStartSyncThenStepDoesNotExecuteFurtherStepMethods() throws Exception {

        ConnectorConfiguration connectorConfiguration = new ConnectorConfiguration();

        SyncServiceContainer syncServiceContainer = applicationContext.getBean(SyncServiceContainer.class);

        String namespaceStr = UUID.randomUUID().toString();
        NamespaceService namespace = new NamespaceService();
        namespace.setNamespace(namespaceStr);
        syncServiceContainer.add(namespace);

        GlobalNamespaceService singletonUniqueIdentifier = applicationContext.getBean(GlobalNamespaceService.class);
        syncServiceContainer.add(singletonUniqueIdentifier);

        HttpClientService httpClientService = applicationContext.getBean(HttpClientService.class);
        syncServiceContainer.add(httpClientService);

        TraverseConfigService traverseConfigService = applicationContext.getBean(TraverseConfigService.class);
        traverseConfigService.configure(syncServiceContainer);
        syncServiceContainer.add(traverseConfigService);

        InfraBeanService infraBeanConfiguration = applicationContext.getBean(InfraBeanService.class);
        InfraConfigService infraConfigService = applicationContext.getBean(InfraConfigService.class);
        infraConfigService.configure(syncServiceContainer);
        InfraService infraService = infraBeanConfiguration.getInfraService(infraConfigService);
        infraService.configure(syncServiceContainer, infraConfigService);
        syncServiceContainer.add(infraService, InfraService.class);

        AnalyticsFactory analyticsFactory = syncServiceContainer.getBean(AnalyticsFactory.class);
        AnalyticsService analyticsService = analyticsFactory.getAnalyticsService(namespace.getNamespace());
        syncServiceContainer.add(analyticsService);
        syncServiceContainer.add(analyticsFactory);

        DagService dagService = applicationContext.getBean(DagService.class);
        dagService.configure(syncServiceContainer);
        DagNode rootNode = dagService.dagStaticStepScanner(namespaceStr, traverseConfigService, infraService);

        List<Class<? extends AbstractStep>> addEnabledPath = new ArrayList<>();
        addEnabledPath.add(TestStartSyncShutdown.class);
        
        List<List<Class<? extends AbstractStep>>> enabledPathList = new ArrayList<>();
        enabledPathList.add(addEnabledPath);

        // enable only the path added and disable rest of the path
        dagService.enableDisableDagPath(rootNode, enabledPathList);

        // Now set this as main root node
        dagService.setRootNode(rootNode);

        SyncStatusService syncStatusService = syncServiceContainer.getBean(SyncStatusService.class);
        syncServiceContainer.add(syncStatusService);

        ServiceTree serviceTree = applicationContext.getBean(ServiceTree.class);
        serviceTree.configure(syncServiceContainer);
        syncServiceContainer.add(serviceTree);

        assertThat(syncStatusService.getTraverser_status(), Matchers.is(-100));
        DagTraversalService dagTraversalService = applicationContext.getBean(DagTraversalService.class);

        ImmutableMap<String, String> x = ImmutableMap.<String, String>builder()
                .put("numberOfUsersEachPage", "10")
                .put("numberOfUserPagination", "1")
                .put("waitBetweenUserPaginationInMs", "0")
                .put("numberOfPostsEachPage", "10")
                .put("numberOfPostPagination", "1")
                .put("waitBetweenPostPaginationInMs", "0")
                .put("numberOfCommentsEachPage", "100")
                .put("numberOfCommentPagination", "1")
                .put("waitBetweenCommentPaginationInMs", "0")
                .put("numberOfCommunitiesEachPage", "1")
                .put("numberOfCommunityPagination", "1")
                .put("waitBetweenCommunityPaginationInMs", "0").build();

        CountDownLatch latch = new CountDownLatch(rootNode.getNodesInDag().size());
        dagTraversalService.configure("/traverser", rootNode, x, new Phaser(), syncServiceContainer);
        syncServiceContainer.add(dagTraversalService);
        TraverserExecutorService traverserExecutorService = syncServiceContainer.getBean(TraverserExecutorService.class);

        List<Boolean> list = new ArrayList<>();
        AtomicReference<List<Boolean>> methodCalls = new AtomicReference<>();
        methodCalls.set(list);

        analyticsService.registerEventCallback("HAGRID_DURABILITY_EVENT", (Map<String, Object> z)->{
            String step = (String)z.get("step");
            if(step.equals("com.freshworks.core.data.durability.steps.start_sync.shutdown.TestStartSyncShutdown")){

                String method = (String)z.get("method");
                System.out.println("Method called is " + method);
                if(method.equalsIgnoreCase("startSync")){
                    methodCalls.get().add(true);
                }
                else{
                    // fail here
                    methodCalls.get().add(false);
                }
            }

        });


        traverserExecutorService.submit(namespaceStr, dagTraversalService);
        syncStatusService.waitUntilTraverserIsInProgress();
        infraService.destroy();
        assertThat(methodCalls.get().size(), Matchers.is(3));
        assertThat(methodCalls.get().get(0), Matchers.is(false));
        assertThat(methodCalls.get().get(1), Matchers.is(false));
        assertThat(methodCalls.get().get(2), Matchers.is(true));
        assertThat(syncStatusService.getTraverser_status(), Matchers.is(-1));
        assertThat(analyticsService.anyErrorEvent(), Matchers.is(true));
        Thread.sleep(10000);
    }

    @Test
    public void testWhenTraverserStepMethodStartSyncReturnNullThenStepDoesNotExecuteFurtherStepMethods() throws Exception {

        ConnectorConfiguration connectorConfiguration = new ConnectorConfiguration();

        SyncServiceContainer syncServiceContainer = applicationContext.getBean(SyncServiceContainer.class);

        String namespaceStr = UUID.randomUUID().toString();
        NamespaceService namespace = new NamespaceService();
        namespace.setNamespace(namespaceStr);
        syncServiceContainer.add(namespace);

        GlobalNamespaceService singletonUniqueIdentifier = applicationContext.getBean(GlobalNamespaceService.class);
        syncServiceContainer.add(singletonUniqueIdentifier);

        HttpClientService httpClientService = applicationContext.getBean(HttpClientService.class);
        syncServiceContainer.add(httpClientService);

        TraverseConfigService traverseConfigService = applicationContext.getBean(TraverseConfigService.class);
        traverseConfigService.configure(syncServiceContainer);
        syncServiceContainer.add(traverseConfigService);

        InfraBeanService infraBeanConfiguration = applicationContext.getBean(InfraBeanService.class);
        InfraConfigService infraConfigService = applicationContext.getBean(InfraConfigService.class);
        infraConfigService.configure(syncServiceContainer);
        InfraService infraService = infraBeanConfiguration.getInfraService(infraConfigService);
        infraService.configure(syncServiceContainer, infraConfigService);
        syncServiceContainer.add(infraService, InfraService.class);

        AnalyticsFactory analyticsFactory = syncServiceContainer.getBean(AnalyticsFactory.class);
        AnalyticsService analyticsService = analyticsFactory.getAnalyticsService(namespace.getNamespace());
        syncServiceContainer.add(analyticsService);
        syncServiceContainer.add(analyticsFactory);

        DagService dagService = applicationContext.getBean(DagService.class);
        dagService.configure(syncServiceContainer);
        DagNode rootNode = dagService.dagStaticStepScanner(namespaceStr, traverseConfigService, infraService);
        
        List<Class<? extends AbstractStep>> addEnabledPath = new ArrayList<>();
        addEnabledPath.add(TestStartSyncNullCheck.class);

        List<List<Class<? extends AbstractStep>>> enabledPathList = new ArrayList<>();
        enabledPathList.add(addEnabledPath);

        // enable only the path added and disable rest of the path
        dagService.enableDisableDagPath(rootNode, enabledPathList);

        // Now set this as main root node
        dagService.setRootNode(rootNode);

        SyncStatusService syncStatusService = syncServiceContainer.getBean(SyncStatusService.class);
        syncServiceContainer.add(syncStatusService);

        ServiceTree serviceTree = applicationContext.getBean(ServiceTree.class);
        serviceTree.configure(syncServiceContainer);
        syncServiceContainer.add(serviceTree);

        assertThat(syncStatusService.getTraverser_status(), Matchers.is(-100));
        DagTraversalService dagTraversalService = applicationContext.getBean(DagTraversalService.class);

        ImmutableMap<String, String> x = ImmutableMap.<String, String>builder()
                .put("numberOfUsersEachPage", "10")
                .put("numberOfUserPagination", "1")
                .put("waitBetweenUserPaginationInMs", "0")
                .put("numberOfPostsEachPage", "10")
                .put("numberOfPostPagination", "1")
                .put("waitBetweenPostPaginationInMs", "0")
                .put("numberOfCommentsEachPage", "100")
                .put("numberOfCommentPagination", "1")
                .put("waitBetweenCommentPaginationInMs", "0")
                .put("numberOfCommunitiesEachPage", "1")
                .put("numberOfCommunityPagination", "1")
                .put("waitBetweenCommunityPaginationInMs", "0").build();

        CountDownLatch latch = new CountDownLatch(rootNode.getNodesInDag().size());
        dagTraversalService.configure("/traverser", rootNode, x, new Phaser(), syncServiceContainer);
        syncServiceContainer.add(dagTraversalService);
        TraverserExecutorService traverserExecutorService = syncServiceContainer.getBean(TraverserExecutorService.class);

        List<Boolean> list = new ArrayList<>();
        AtomicReference<List<Boolean>> methodCalls = new AtomicReference<>();
        methodCalls.set(list);

        analyticsService.registerEventCallback("HAGRID_DURABILITY_EVENT", (Map<String, Object> z)->{
            String step = (String)z.get("step");
            if(step.equals("com.freshworks.core.data.durability.steps.start_sync.null_check.TestStartSyncNullCheck")){

                String method = (String)z.get("method");
                if(method.equalsIgnoreCase("startSync")){
                    methodCalls.get().add(true);
                }
                else{
                    // fail here
                    methodCalls.get().add(false);
                }
            }

        });


        traverserExecutorService.submit(namespaceStr, dagTraversalService);
        syncStatusService.waitUntilTraverserIsInProgress();
        infraService.destroy();
        assertThat(methodCalls.get().size(), Matchers.is(3));
        assertThat(methodCalls.get().get(0), Matchers.is(false));
        assertThat(methodCalls.get().get(1), Matchers.is(false));
        assertThat(methodCalls.get().get(2), Matchers.is(true));
        assertThat(syncStatusService.getTraverser_status(), Matchers.is(-1));
        assertThat(analyticsService.anyErrorEvent(), Matchers.is(true));
        Thread.sleep(10000);
    }

    @Test
    public void testWhenTraverserIsShutdownByStepMethodIsValidThenStepDoesNotExecuteFurtherStepMethods() throws Exception {

        ConnectorConfiguration connectorConfiguration = new ConnectorConfiguration();

        List<Class<? extends  AbstractStep>> addEnabledPath = new ArrayList();
        addEnabledPath.add(TestIsValidShutdown.class);
        connectorConfiguration.addPathToEnable(addEnabledPath);

        SyncServiceContainer syncServiceContainer = applicationContext.getBean(SyncServiceContainer.class);

        String namespaceStr = UUID.randomUUID().toString();
        NamespaceService namespace = new NamespaceService();
        namespace.setNamespace(namespaceStr);
        syncServiceContainer.add(namespace);

        GlobalNamespaceService singletonUniqueIdentifier = applicationContext.getBean(GlobalNamespaceService.class);
        syncServiceContainer.add(singletonUniqueIdentifier);

        HttpClientService httpClientService = applicationContext.getBean(HttpClientService.class);
        syncServiceContainer.add(httpClientService);

        TraverseConfigService traverseConfigService = applicationContext.getBean(TraverseConfigService.class);
        traverseConfigService.configure(syncServiceContainer);
        syncServiceContainer.add(traverseConfigService);

        InfraBeanService infraBeanConfiguration = applicationContext.getBean(InfraBeanService.class);
        InfraConfigService infraConfigService = applicationContext.getBean(InfraConfigService.class);
        infraConfigService.configure(syncServiceContainer);
        InfraService infraService = infraBeanConfiguration.getInfraService(infraConfigService);
        infraService.configure(syncServiceContainer, infraConfigService);
        syncServiceContainer.add(infraService, InfraService.class);

        AnalyticsFactory analyticsFactory = syncServiceContainer.getBean(AnalyticsFactory.class);
        AnalyticsService analyticsService = analyticsFactory.getAnalyticsService(namespace.getNamespace());
        syncServiceContainer.add(analyticsService);
        syncServiceContainer.add(analyticsFactory);

        DagService dagService = applicationContext.getBean(DagService.class);
        dagService.configure(syncServiceContainer);
        DagNode rootNode = dagService.dagStaticStepScanner(namespaceStr, traverseConfigService, infraService);

        List<List<Class<? extends AbstractStep>>> enabledPathList = new ArrayList<>();
        enabledPathList.add(addEnabledPath);

        // enable only the path added and disable rest of the path
        dagService.enableDisableDagPath(rootNode, enabledPathList);

        // Now set this as main root node
        dagService.setRootNode(rootNode);

        SyncStatusService syncStatusService = syncServiceContainer.getBean(SyncStatusService.class);
        syncServiceContainer.add(syncStatusService);

        ServiceTree serviceTree = applicationContext.getBean(ServiceTree.class);
        serviceTree.configure(syncServiceContainer);
        syncServiceContainer.add(serviceTree);

        assertThat(syncStatusService.getTraverser_status(), Matchers.is(-100));
        DagTraversalService dagTraversalService = applicationContext.getBean(DagTraversalService.class);

        ImmutableMap<String, String> x = ImmutableMap.<String, String>builder()
                .put("numberOfUsersEachPage", "10")
                .put("numberOfUserPagination", "1")
                .put("waitBetweenUserPaginationInMs", "0")
                .put("numberOfPostsEachPage", "10")
                .put("numberOfPostPagination", "1")
                .put("waitBetweenPostPaginationInMs", "0")
                .put("numberOfCommentsEachPage", "100")
                .put("numberOfCommentPagination", "1")
                .put("waitBetweenCommentPaginationInMs", "0")
                .put("numberOfCommunitiesEachPage", "1")
                .put("numberOfCommunityPagination", "1")
                .put("waitBetweenCommunityPaginationInMs", "0").build();


        CountDownLatch latch = new CountDownLatch(rootNode.getNodesInDag().size());
        dagTraversalService.configure("/traverser", rootNode, x,  new Phaser(), syncServiceContainer);
        syncServiceContainer.add(dagTraversalService);
        TraverserExecutorService traverserExecutorService = syncServiceContainer.getBean(TraverserExecutorService.class);

        List<Boolean> list = new ArrayList<>();
        AtomicReference<List<Boolean>> methodCalls = new AtomicReference<>();
        methodCalls.set(list);

        analyticsService.registerEventCallback("HAGRID_DURABILITY_EVENT", (Map<String, Object> z)->{
            String step = (String)z.get("step");
            if(step.equals("com.freshworks.core.data.durability.steps.is_valid.shutdown.TestIsValidShutdown")){

                String method = (String)z.get("method");
                System.out.println("method name is " + method);
                if(method.equalsIgnoreCase("isValidResponse")){
                    methodCalls.get().add(true);
                }
                else{
                    // fail here
                    methodCalls.get().add(false);
                }
            }

        });


        traverserExecutorService.submit(namespaceStr, dagTraversalService);
        syncStatusService.waitUntilTraverserIsInProgress();
        infraService.destroy();
        
        assertThat(methodCalls.get().size(), Matchers.is(4));
        assertThat(methodCalls.get().get(0), Matchers.is(false));
        assertThat(methodCalls.get().get(1), Matchers.is(false));
        assertThat(methodCalls.get().get(2), Matchers.is(false));
        assertThat(methodCalls.get().get(3), Matchers.is(true));
        assertThat(syncStatusService.getTraverser_status(), Matchers.is(-1));
        assertThat(analyticsService.anyErrorEvent(), Matchers.is(true));
        Thread.sleep(10000);
    }

    /**
     * Commenting this test case as isValud can not return null because it is of type boolean not Boolean
     * @throws Exception
     */
    // @Test
    // public void testWhenTraverserStepMethodIsValidReturnNullThenStepDoesNotExecuteFurtherStepMethods() throws Exception {

    //     ConnectorConfiguration connectorConfiguration = new ConnectorConfiguration();

    //     SyncServiceContainer syncServiceContainer = applicationContext.getBean(SyncServiceContainer.class);

    //     String namespaceStr = UUID.randomUUID().toString();
    //     NamespaceService namespace = new NamespaceService();
    //     namespace.setNamespace(namespaceStr);
    //     syncServiceContainer.add(namespace);

    //     GlobalNamespaceService singletonUniqueIdentifier = applicationContext.getBean(GlobalNamespaceService.class);
    //     syncServiceContainer.add(singletonUniqueIdentifier);

    //     HttpClientService httpClientService = applicationContext.getBean(HttpClientService.class);
    //     syncServiceContainer.add(httpClientService);

    //     TraverseConfigService traverseConfigService = applicationContext.getBean(TraverseConfigService.class);
    //     traverseConfigService.configure(syncServiceContainer);
    //     syncServiceContainer.add(traverseConfigService);

    //     InfraBeanService infraBeanConfiguration = applicationContext.getBean(InfraBeanService.class);
    //     InfraConfigService infraConfigService = applicationContext.getBean(InfraConfigService.class);
    //     infraConfigService.configure(syncServiceContainer);
    //     InfraService infraService = infraBeanConfiguration.getInfraService(infraConfigService, connectorConfiguration);
    //     infraService.configure(syncServiceContainer, infraConfigService);
    //     syncServiceContainer.add(infraService, InfraService.class);

    //     AnalyticsFactory analyticsFactory = syncServiceContainer.getBean(AnalyticsFactory.class);
    //     AnalyticsService analyticsService = analyticsFactory.getAnalyticsService(namespace.getNamespace());
    //     syncServiceContainer.add(analyticsService);
    //     syncServiceContainer.add(analyticsFactory);

    //     DagService dagService = applicationContext.getBean(DagService.class);
    //     dagService.configure(syncServiceContainer);
    //     DagNode rootNode = dagService.dagScanner(namespaceStr, traverseConfigService, infraService);

    //     List<Class<? extends AbstractStep>> addEnabledPath = new ArrayList<>();
    //     addEnabledPath.add(TestIsValidNullCheck.class);

    //     List<List<Class<? extends AbstractStep>>> enabledPathList = new ArrayList<>();
    //     enabledPathList.add(addEnabledPath);

    //     // enable only the path added and disable rest of the path
    //     dagService.enableDisableDagPath(rootNode, enabledPathList);

    //     // Now set this as main root node
    //     dagService.setRootNode(rootNode);
        
    //     SyncStatusService syncStatusService = syncServiceContainer.getBean(SyncStatusService.class);
    //     syncServiceContainer.add(syncStatusService);

    //     ServiceTree serviceTree = applicationContext.getBean(ServiceTree.class);
    //     serviceTree.configure(syncServiceContainer);
    //     syncServiceContainer.add(serviceTree);

    //     assertThat(syncStatusService.getTraverser_status(), Matchers.is(-100));
    //     DagTraversalService dagTraversalService = applicationContext.getBean(DagTraversalService.class);

    //     ImmutableMap<String, String> x = ImmutableMap.<String, String>builder()
    //             .put("numberOfUsersEachPage", "10")
    //             .put("numberOfUserPagination", "1")
    //             .put("waitBetweenUserPaginationInMs", "0")
    //             .put("numberOfPostsEachPage", "10")
    //             .put("numberOfPostPagination", "1")
    //             .put("waitBetweenPostPaginationInMs", "0")
    //             .put("numberOfCommentsEachPage", "100")
    //             .put("numberOfCommentPagination", "1")
    //             .put("waitBetweenCommentPaginationInMs", "0")
    //             .put("numberOfCommunitiesEachPage", "1")
    //             .put("numberOfCommunityPagination", "1")
    //             .put("waitBetweenCommunityPaginationInMs", "0").build();

    //     CountDownLatch latch = new CountDownLatch(rootNode.getNodesInDag().size());
    //     dagTraversalService.configure("/traverser", rootNode, x,  new Phaser(), syncServiceContainer);
    //     syncServiceContainer.add(dagTraversalService);
    //     TraverserExecutorService traverserExecutorService = syncServiceContainer.getBean(TraverserExecutorService.class);

    //     List<Boolean> list = new ArrayList<>();
    //     AtomicReference<List<Boolean>> methodCalls = new AtomicReference<>();
    //     methodCalls.set(list);

    //     analyticsService.registerEventCallback("HAGRID_DURABILITY_EVENT", (Map<String, Object> z)->{
    //         String step = (String)z.get("step");
    //         if(step.equals("com.freshworks.core.data.durability.steps.is_valid.null_check.TestIsValidNullCheck")){

    //             String method = (String)z.get("method");
    //             if(method.equalsIgnoreCase("isValidResponse")){
    //                 methodCalls.get().add(true);
    //             }
    //             else{
    //                 // fail here
    //                 methodCalls.get().add(false);
    //             }
    //         }

    //     });


    //     traverserExecutorService.submit(namespaceStr, dagTraversalService);
    //     syncStatusService.waitUntilTraverserIsInProgress();
    //     infraService.destroy();
    //     assertThat(methodCalls.get().size(), Matchers.is(4));
    //     assertThat(methodCalls.get().get(0), Matchers.is(false));
    //     assertThat(methodCalls.get().get(1), Matchers.is(false));
    //     assertThat(methodCalls.get().get(2), Matchers.is(false));
    //     assertThat(methodCalls.get().get(3), Matchers.is(true));
    //     assertThat(syncStatusService.getTraverser_status(), Matchers.is(-1));
    //     assertThat(analyticsService.anyErrorEvent(), Matchers.is(true));
    //     Thread.sleep(10000);
    // }

    @Test
    public void testWhenTraverserIsShutdownByStepMethodHandleInValidThenStepDoesNotExecuteFurtherStepMethods() throws Exception {

        ConnectorConfiguration connectorConfiguration = new ConnectorConfiguration();

        SyncServiceContainer syncServiceContainer = applicationContext.getBean(SyncServiceContainer.class);

        String namespaceStr = UUID.randomUUID().toString();
        NamespaceService namespace = new NamespaceService();
        namespace.setNamespace(namespaceStr);
        syncServiceContainer.add(namespace);

        GlobalNamespaceService singletonUniqueIdentifier = applicationContext.getBean(GlobalNamespaceService.class);
        syncServiceContainer.add(singletonUniqueIdentifier);

        HttpClientService httpClientService = applicationContext.getBean(HttpClientService.class);
        syncServiceContainer.add(httpClientService);

        TraverseConfigService traverseConfigService = applicationContext.getBean(TraverseConfigService.class);
        traverseConfigService.configure(syncServiceContainer);
        syncServiceContainer.add(traverseConfigService);

        InfraBeanService infraBeanConfiguration = applicationContext.getBean(InfraBeanService.class);
        InfraConfigService infraConfigService = applicationContext.getBean(InfraConfigService.class);
        infraConfigService.configure(syncServiceContainer);
        InfraService infraService = infraBeanConfiguration.getInfraService(infraConfigService);
        infraService.configure(syncServiceContainer, infraConfigService);
        syncServiceContainer.add(infraService, InfraService.class);

        DagService dagService = applicationContext.getBean(DagService.class);
        dagService.configure(syncServiceContainer);
        DagNode rootNode = dagService.dagStaticStepScanner(namespaceStr, traverseConfigService, infraService);

        List<Class<? extends  AbstractStep>> addEnabledPath = new ArrayList();
        addEnabledPath.add(TestHandleInvalidShutdown.class);

        List<List<Class<? extends AbstractStep>>> enabledPathList = new ArrayList<>();
        enabledPathList.add(addEnabledPath);

        // enable only the path added and disable rest of the path
        dagService.enableDisableDagPath(rootNode, enabledPathList);

        // Now set this as main root node
        dagService.setRootNode(rootNode);


        SyncStatusService syncStatusService = syncServiceContainer.getBean(SyncStatusService.class);
        syncServiceContainer.add(syncStatusService);

        AnalyticsFactory analyticsFactory = syncServiceContainer.getBean(AnalyticsFactory.class);
        AnalyticsService analyticsService = analyticsFactory.getAnalyticsService(namespace.getNamespace());
        syncServiceContainer.add(analyticsService);
        syncServiceContainer.add(analyticsFactory);

        ServiceTree serviceTree = applicationContext.getBean(ServiceTree.class);
        serviceTree.configure(syncServiceContainer);
        syncServiceContainer.add(serviceTree);

        assertThat(syncStatusService.getTraverser_status(), Matchers.is(-100));
        DagTraversalService dagTraversalService = applicationContext.getBean(DagTraversalService.class);

        ImmutableMap<String, String> x = ImmutableMap.<String, String>builder()
                .put("numberOfUsersEachPage", "10")
                .put("numberOfUserPagination", "1")
                .put("waitBetweenUserPaginationInMs", "0")
                .put("numberOfPostsEachPage", "10")
                .put("numberOfPostPagination", "1")
                .put("waitBetweenPostPaginationInMs", "0")
                .put("numberOfCommentsEachPage", "100")
                .put("numberOfCommentPagination", "1")
                .put("waitBetweenCommentPaginationInMs", "0")
                .put("numberOfCommunitiesEachPage", "1")
                .put("numberOfCommunityPagination", "1")
                .put("waitBetweenCommunityPaginationInMs", "0").build();

        CountDownLatch latch = new CountDownLatch(rootNode.getNodesInDag().size());
        dagTraversalService.configure("/traverser", rootNode, x,  new Phaser(), syncServiceContainer);
        syncServiceContainer.add(dagTraversalService);
        TraverserExecutorService traverserExecutorService = syncServiceContainer.getBean(TraverserExecutorService.class);

        List<Boolean> list = new ArrayList<>();
        AtomicReference<List<Boolean>> methodCalls = new AtomicReference<>();
        methodCalls.set(list);

        analyticsService.registerEventCallback("HAGRID_DURABILITY_EVENT", (Map<String, Object> z)->{
            String step = (String)z.get("step");
            if(step.equals("com.freshworks.core.data.durability.steps.handle_invalid.shutdown.TestHandleInvalidShutdown")){

                String method = (String)z.get("method");
                if(method.equalsIgnoreCase("handleInvalidResponse")){
                    methodCalls.get().add(true);
                }
                else{
                    // fail here
                    methodCalls.get().add(false);
                }
            }

        });


        traverserExecutorService.submit(namespaceStr, dagTraversalService);
        syncStatusService.waitUntilTraverserIsInProgress();
        infraService.destroy();
        assertThat(methodCalls.get().size(), Matchers.is(5));
        assertThat(methodCalls.get().get(0), Matchers.is(false));
        assertThat(methodCalls.get().get(1), Matchers.is(false));
        assertThat(methodCalls.get().get(2), Matchers.is(false));
        assertThat(methodCalls.get().get(3), Matchers.is(false));
        assertThat(methodCalls.get().get(4), Matchers.is(true));
        assertThat(syncStatusService.getTraverser_status(), Matchers.is(-1));
        assertThat(analyticsService.anyErrorEvent(), Matchers.is(true));
        Thread.sleep(10000);
    }

    @Test
    public void testWhenTraverserStepMethodHandleInValidReturnNullThenStepDoesNotExecuteFurtherStepMethods() throws Exception {

        ConnectorConfiguration connectorConfiguration = new ConnectorConfiguration();

        SyncServiceContainer syncServiceContainer = applicationContext.getBean(SyncServiceContainer.class);

        String namespaceStr = UUID.randomUUID().toString();
        NamespaceService namespace = new NamespaceService();
        namespace.setNamespace(namespaceStr);
        syncServiceContainer.add(namespace);

        GlobalNamespaceService singletonUniqueIdentifier = applicationContext.getBean(GlobalNamespaceService.class);
        syncServiceContainer.add(singletonUniqueIdentifier);

        HttpClientService httpClientService = applicationContext.getBean(HttpClientService.class);
        syncServiceContainer.add(httpClientService);

        TraverseConfigService traverseConfigService = applicationContext.getBean(TraverseConfigService.class);
        traverseConfigService.configure(syncServiceContainer);
        syncServiceContainer.add(traverseConfigService);

        InfraBeanService infraBeanConfiguration = applicationContext.getBean(InfraBeanService.class);
        InfraConfigService infraConfigService = applicationContext.getBean(InfraConfigService.class);
        infraConfigService.configure(syncServiceContainer);
        InfraService infraService = infraBeanConfiguration.getInfraService(infraConfigService);
        infraService.configure(syncServiceContainer, infraConfigService);
        syncServiceContainer.add(infraService, InfraService.class);

        DagService dagService = applicationContext.getBean(DagService.class);
        dagService.configure(syncServiceContainer);
        DagNode rootNode = dagService.dagStaticStepScanner(namespaceStr, traverseConfigService, infraService);

        List<Class<? extends  AbstractStep>> addEnabledPath = new ArrayList();
        addEnabledPath.add(TestHandleInvalidNullCheck.class);

        List<List<Class<? extends AbstractStep>>> enabledPathList = new ArrayList<>();
        enabledPathList.add(addEnabledPath);

        // enable only the path added and disable rest of the path
        dagService.enableDisableDagPath(rootNode, enabledPathList);

        // Now set this as main root node
        dagService.setRootNode(rootNode);

        SyncStatusService syncStatusService = syncServiceContainer.getBean(SyncStatusService.class);
        syncServiceContainer.add(syncStatusService);

        AnalyticsFactory analyticsFactory = syncServiceContainer.getBean(AnalyticsFactory.class);
        AnalyticsService analyticsService = analyticsFactory.getAnalyticsService(namespace.getNamespace());
        syncServiceContainer.add(analyticsService);
        syncServiceContainer.add(analyticsFactory);

        ServiceTree serviceTree = applicationContext.getBean(ServiceTree.class);
        serviceTree.configure(syncServiceContainer);
        syncServiceContainer.add(serviceTree);

        assertThat(syncStatusService.getTraverser_status(), Matchers.is(-100));
        DagTraversalService dagTraversalService = applicationContext.getBean(DagTraversalService.class);

        ImmutableMap<String, String> x = ImmutableMap.<String, String>builder()
                .put("numberOfUsersEachPage", "10")
                .put("numberOfUserPagination", "1")
                .put("waitBetweenUserPaginationInMs", "0")
                .put("numberOfPostsEachPage", "10")
                .put("numberOfPostPagination", "1")
                .put("waitBetweenPostPaginationInMs", "0")
                .put("numberOfCommentsEachPage", "100")
                .put("numberOfCommentPagination", "1")
                .put("waitBetweenCommentPaginationInMs", "0")
                .put("numberOfCommunitiesEachPage", "1")
                .put("numberOfCommunityPagination", "1")
                .put("waitBetweenCommunityPaginationInMs", "0").build();

        CountDownLatch latch = new CountDownLatch(rootNode.getNodesInDag().size());
        dagTraversalService.configure("/traverser", rootNode, x,  new Phaser(), syncServiceContainer);
        syncServiceContainer.add(dagTraversalService);
        TraverserExecutorService traverserExecutorService = syncServiceContainer.getBean(TraverserExecutorService.class);

        List<Boolean> list = new ArrayList<>();
        AtomicReference<List<Boolean>> methodCalls = new AtomicReference<>();
        methodCalls.set(list);

        analyticsService.registerEventCallback("HAGRID_DURABILITY_EVENT", (Map<String, Object> z)->{
            String step = (String)z.get("step");
            if(step.equals("com.freshworks.core.data.durability.steps.handle_invalid.null_check.TestHandleInvalidNullCheck")){

                String method = (String)z.get("method");
                if(method.equalsIgnoreCase("handleInvalidResponse")){
                    methodCalls.get().add(true);
                }
                else{
                    // fail here
                    methodCalls.get().add(false);
                }
            }

        });


        traverserExecutorService.submit(namespaceStr, dagTraversalService);
        syncStatusService.waitUntilTraverserIsInProgress();
        infraService.destroy();
        assertThat(methodCalls.get().size(), Matchers.is(5));
        assertThat(methodCalls.get().get(0), Matchers.is(false));
        assertThat(methodCalls.get().get(1), Matchers.is(false));
        assertThat(methodCalls.get().get(2), Matchers.is(false));
        assertThat(methodCalls.get().get(3), Matchers.is(false));
        assertThat(methodCalls.get().get(4), Matchers.is(true));
        assertThat(syncStatusService.getTraverser_status(), Matchers.is(-1));
        assertThat(analyticsService.anyErrorEvent(), Matchers.is(true));
        Thread.sleep(10000);
    }

    @Test
    public void testWhenTraverserIsShutdownByStepMethodParseSyncThenStepDoesNotExecuteFurtherStepMethods() throws Exception {

        ConnectorConfiguration connectorConfiguration = new ConnectorConfiguration();

        SyncServiceContainer syncServiceContainer = applicationContext.getBean(SyncServiceContainer.class);

        String namespaceStr = UUID.randomUUID().toString();
        NamespaceService namespace = new NamespaceService();
        namespace.setNamespace(namespaceStr);
        syncServiceContainer.add(namespace);

        GlobalNamespaceService singletonUniqueIdentifier = applicationContext.getBean(GlobalNamespaceService.class);
        syncServiceContainer.add(singletonUniqueIdentifier);

        HttpClientService httpClientService = applicationContext.getBean(HttpClientService.class);
        syncServiceContainer.add(httpClientService);

        TraverseConfigService traverseConfigService = applicationContext.getBean(TraverseConfigService.class);
        traverseConfigService.configure(syncServiceContainer);
        syncServiceContainer.add(traverseConfigService);

        InfraBeanService infraBeanConfiguration = applicationContext.getBean(InfraBeanService.class);
        InfraConfigService infraConfigService = applicationContext.getBean(InfraConfigService.class);
        infraConfigService.configure(syncServiceContainer);
        InfraService infraService = infraBeanConfiguration.getInfraService(infraConfigService);
        infraService.configure(syncServiceContainer, infraConfigService);
        syncServiceContainer.add(infraService, InfraService.class);

        DagService dagService = applicationContext.getBean(DagService.class);
        dagService.configure(syncServiceContainer);
        DagNode rootNode = dagService.dagStaticStepScanner(namespaceStr, traverseConfigService, infraService);

        List<Class<? extends  AbstractStep>> addEnabledPath = new ArrayList();
        addEnabledPath.add(TestParseSyncShutdown.class);

        List<List<Class<? extends AbstractStep>>> enabledPathList = new ArrayList<>();
        enabledPathList.add(addEnabledPath);

        // enable only the path added and disable rest of the path
        dagService.enableDisableDagPath(rootNode, enabledPathList);

        // Now set this as main root node
        dagService.setRootNode(rootNode);

        SyncStatusService syncStatusService = syncServiceContainer.getBean(SyncStatusService.class);
        syncServiceContainer.add(syncStatusService);

        AnalyticsFactory analyticsFactory = syncServiceContainer.getBean(AnalyticsFactory.class);
        AnalyticsService analyticsService = analyticsFactory.getAnalyticsService(namespace.getNamespace());
        syncServiceContainer.add(analyticsService);
        syncServiceContainer.add(analyticsFactory);

        ServiceTree serviceTree = applicationContext.getBean(ServiceTree.class);
        serviceTree.configure(syncServiceContainer);
        syncServiceContainer.add(serviceTree);

        assertThat(syncStatusService.getTraverser_status(), Matchers.is(-100));
        DagTraversalService dagTraversalService = applicationContext.getBean(DagTraversalService.class);

        ImmutableMap<String, String> x = ImmutableMap.<String, String>builder()
                .put("numberOfUsersEachPage", "10")
                .put("numberOfUserPagination", "1")
                .put("waitBetweenUserPaginationInMs", "0")
                .put("numberOfPostsEachPage", "10")
                .put("numberOfPostPagination", "1")
                .put("waitBetweenPostPaginationInMs", "0")
                .put("numberOfCommentsEachPage", "100")
                .put("numberOfCommentPagination", "1")
                .put("waitBetweenCommentPaginationInMs", "0")
                .put("numberOfCommunitiesEachPage", "1")
                .put("numberOfCommunityPagination", "1")
                .put("waitBetweenCommunityPaginationInMs", "0").build();

        CountDownLatch latch = new CountDownLatch(rootNode.getNodesInDag().size());
        dagTraversalService.configure("/traverser", rootNode, x, new Phaser(), syncServiceContainer);
        syncServiceContainer.add(dagTraversalService);
        TraverserExecutorService traverserExecutorService = syncServiceContainer.getBean(TraverserExecutorService.class);

        List<Boolean> list = new ArrayList<>();
        AtomicReference<List<Boolean>> methodCalls = new AtomicReference<>();
        methodCalls.set(list);

        analyticsService.registerEventCallback("HAGRID_DURABILITY_EVENT", (Map<String, Object> z)->{
            String step = (String)z.get("step");
            if(step.equals("com.freshworks.core.data.durability.steps.parse_sync.shutdown.TestParseSyncShutdown")){

                String method = (String)z.get("method");
                if(method.equalsIgnoreCase("parseSyncResponse")){
                    methodCalls.get().add(true);
                }
                else{
                    // fail here
                    methodCalls.get().add(false);
                }
            }

        });


        traverserExecutorService.submit(namespaceStr, dagTraversalService);
        syncStatusService.waitUntilTraverserIsInProgress();
        infraService.destroy();
        assertThat(methodCalls.get().size(), Matchers.is(5));
        assertThat(methodCalls.get().get(0), Matchers.is(false));
        assertThat(methodCalls.get().get(1), Matchers.is(false));
        assertThat(methodCalls.get().get(2), Matchers.is(false));
        assertThat(methodCalls.get().get(3), Matchers.is(false));
        assertThat(methodCalls.get().get(4), Matchers.is(true));
        assertThat(syncStatusService.getTraverser_status(), Matchers.is(-1));
        assertThat(analyticsService.anyErrorEvent(), Matchers.is(true));
        Thread.sleep(10000);
    }

    @Test
    public void testWhenTraverserStepMethodParseSyncReturnNullThenStepDoesNotExecuteFurtherStepMethods() throws Exception {

        ConnectorConfiguration connectorConfiguration = new ConnectorConfiguration();

        SyncServiceContainer syncServiceContainer = applicationContext.getBean(SyncServiceContainer.class);

        String namespaceStr = UUID.randomUUID().toString();
        NamespaceService namespace = new NamespaceService();
        namespace.setNamespace(namespaceStr);
        syncServiceContainer.add(namespace);

        GlobalNamespaceService singletonUniqueIdentifier = applicationContext.getBean(GlobalNamespaceService.class);
        syncServiceContainer.add(singletonUniqueIdentifier);

        HttpClientService httpClientService = applicationContext.getBean(HttpClientService.class);
        syncServiceContainer.add(httpClientService);

        TraverseConfigService traverseConfigService = applicationContext.getBean(TraverseConfigService.class);
        traverseConfigService.configure(syncServiceContainer);
        syncServiceContainer.add(traverseConfigService);

        InfraBeanService infraBeanConfiguration = applicationContext.getBean(InfraBeanService.class);
        InfraConfigService infraConfigService = applicationContext.getBean(InfraConfigService.class);
        infraConfigService.configure(syncServiceContainer);
        InfraService infraService = infraBeanConfiguration.getInfraService(infraConfigService);
        infraService.configure(syncServiceContainer, infraConfigService);
        syncServiceContainer.add(infraService, InfraService.class);

        DagService dagService = applicationContext.getBean(DagService.class);
        dagService.configure(syncServiceContainer);
        DagNode rootNode = dagService.dagStaticStepScanner(namespaceStr, traverseConfigService, infraService);

        List<Class<? extends  AbstractStep>> addEnabledPath = new ArrayList();
        addEnabledPath.add(TestParseSyncNullCheck.class);

        List<List<Class<? extends AbstractStep>>> enabledPathList = new ArrayList<>();
        enabledPathList.add(addEnabledPath);

        // enable only the path added and disable rest of the path
        dagService.enableDisableDagPath(rootNode, enabledPathList);

        // Now set this as main root node
        dagService.setRootNode(rootNode);


        SyncStatusService syncStatusService = syncServiceContainer.getBean(SyncStatusService.class);
        syncServiceContainer.add(syncStatusService);

        AnalyticsFactory analyticsFactory = syncServiceContainer.getBean(AnalyticsFactory.class);
        AnalyticsService analyticsService = analyticsFactory.getAnalyticsService(namespace.getNamespace());
        syncServiceContainer.add(analyticsService);
        syncServiceContainer.add(analyticsFactory);

        ServiceTree serviceTree = applicationContext.getBean(ServiceTree.class);
        serviceTree.configure(syncServiceContainer);
        syncServiceContainer.add(serviceTree);

        assertThat(syncStatusService.getTraverser_status(), Matchers.is(-100));
        DagTraversalService dagTraversalService = applicationContext.getBean(DagTraversalService.class);

        ImmutableMap<String, String> x = ImmutableMap.<String, String>builder()
                .put("numberOfUsersEachPage", "10")
                .put("numberOfUserPagination", "1")
                .put("waitBetweenUserPaginationInMs", "0")
                .put("numberOfPostsEachPage", "10")
                .put("numberOfPostPagination", "1")
                .put("waitBetweenPostPaginationInMs", "0")
                .put("numberOfCommentsEachPage", "100")
                .put("numberOfCommentPagination", "1")
                .put("waitBetweenCommentPaginationInMs", "0")
                .put("numberOfCommunitiesEachPage", "1")
                .put("numberOfCommunityPagination", "1")
                .put("waitBetweenCommunityPaginationInMs", "0").build();

        CountDownLatch latch = new CountDownLatch(rootNode.getNodesInDag().size());
        dagTraversalService.configure("/traverser", rootNode, x, new Phaser(), syncServiceContainer);
        syncServiceContainer.add(dagTraversalService);
        TraverserExecutorService traverserExecutorService = syncServiceContainer.getBean(TraverserExecutorService.class);

        List<Boolean> list = new ArrayList<>();
        AtomicReference<List<Boolean>> methodCalls = new AtomicReference<>();
        methodCalls.set(list);

        analyticsService.registerEventCallback("HAGRID_DURABILITY_EVENT", (Map<String, Object> z)->{
            String step = (String)z.get("step");
            if(step.equals("com.freshworks.core.data.durability.steps.parse_sync.null_check.TestParseSyncNullCheck")){

                String method = (String)z.get("method");
                if(method.equalsIgnoreCase("parseSyncResponse")){
                    methodCalls.get().add(true);
                }
                else{
                    // fail here
                    methodCalls.get().add(false);
                }
            }

        });


        traverserExecutorService.submit(namespaceStr, dagTraversalService);
        syncStatusService.waitUntilTraverserIsInProgress();
        infraService.destroy();
        assertThat(methodCalls.get().size(), Matchers.is(5));
        assertThat(methodCalls.get().get(0), Matchers.is(false));
        assertThat(methodCalls.get().get(1), Matchers.is(false));
        assertThat(methodCalls.get().get(2), Matchers.is(false));
        assertThat(methodCalls.get().get(3), Matchers.is(false));
        assertThat(methodCalls.get().get(4), Matchers.is(true));
        assertThat(syncStatusService.getTraverser_status(), Matchers.is(-1));
        assertThat(analyticsService.anyErrorEvent(), Matchers.is(true));
        Thread.sleep(10000);
    }

    @Test
    public void testWhenTraverserIsShutdownByStepMethodIsSyncCompleteThenStepDoesNotExecuteFurtherStepMethods() throws Exception {

        ConnectorConfiguration connectorConfiguration = new ConnectorConfiguration();

        SyncServiceContainer syncServiceContainer = applicationContext.getBean(SyncServiceContainer.class);

        String namespaceStr = UUID.randomUUID().toString();
        NamespaceService namespace = new NamespaceService();
        namespace.setNamespace(namespaceStr);
        syncServiceContainer.add(namespace);

        GlobalNamespaceService singletonUniqueIdentifier = applicationContext.getBean(GlobalNamespaceService.class);
        syncServiceContainer.add(singletonUniqueIdentifier);

        HttpClientService httpClientService = applicationContext.getBean(HttpClientService.class);
        syncServiceContainer.add(httpClientService);

        TraverseConfigService traverseConfigService = applicationContext.getBean(TraverseConfigService.class);
        traverseConfigService.configure(syncServiceContainer);
        syncServiceContainer.add(traverseConfigService);

        InfraBeanService infraBeanConfiguration = applicationContext.getBean(InfraBeanService.class);
        InfraConfigService infraConfigService = applicationContext.getBean(InfraConfigService.class);
        infraConfigService.configure(syncServiceContainer);
        InfraService infraService = infraBeanConfiguration.getInfraService(infraConfigService);
        infraService.configure(syncServiceContainer, infraConfigService);
        syncServiceContainer.add(infraService, InfraService.class);

        DagService dagService = applicationContext.getBean(DagService.class);
        dagService.configure(syncServiceContainer);
        DagNode rootNode = dagService.dagStaticStepScanner(namespaceStr, traverseConfigService, infraService);


        List<Class<? extends  AbstractStep>> addEnabledPath = new ArrayList();
        addEnabledPath.add(TestIsSyncCompleteShutdown.class);

        List<List<Class<? extends AbstractStep>>> enabledPathList = new ArrayList<>();
        enabledPathList.add(addEnabledPath);

        // enable only the path added and disable rest of the path
        dagService.enableDisableDagPath(rootNode, enabledPathList);

        // Now set this as main root node
        dagService.setRootNode(rootNode);


        SyncStatusService syncStatusService = syncServiceContainer.getBean(SyncStatusService.class);
        syncServiceContainer.add(syncStatusService);

        AnalyticsFactory analyticsFactory = syncServiceContainer.getBean(AnalyticsFactory.class);
        AnalyticsService analyticsService = analyticsFactory.getAnalyticsService(namespace.getNamespace());
        syncServiceContainer.add(analyticsService);
        syncServiceContainer.add(analyticsFactory);

        ServiceTree serviceTree = applicationContext.getBean(ServiceTree.class);
        serviceTree.configure(syncServiceContainer);
        syncServiceContainer.add(serviceTree);

        assertThat(syncStatusService.getTraverser_status(), Matchers.is(-100));
        DagTraversalService dagTraversalService = applicationContext.getBean(DagTraversalService.class);

        ImmutableMap<String, String> x = ImmutableMap.<String, String>builder()
                .put("numberOfUsersEachPage", "10")
                .put("numberOfUserPagination", "1")
                .put("waitBetweenUserPaginationInMs", "0")
                .put("numberOfPostsEachPage", "10")
                .put("numberOfPostPagination", "1")
                .put("waitBetweenPostPaginationInMs", "0")
                .put("numberOfCommentsEachPage", "100")
                .put("numberOfCommentPagination", "1")
                .put("waitBetweenCommentPaginationInMs", "0")
                .put("numberOfCommunitiesEachPage", "1")
                .put("numberOfCommunityPagination", "1")
                .put("waitBetweenCommunityPaginationInMs", "0").build();

        CountDownLatch latch = new CountDownLatch(rootNode.getNodesInDag().size());
        dagTraversalService.configure("/traverser", rootNode, x, new Phaser(),  syncServiceContainer);
        syncServiceContainer.add(dagTraversalService);
        TraverserExecutorService traverserExecutorService = syncServiceContainer.getBean(TraverserExecutorService.class);

        List<Boolean> list = new ArrayList<>();
        AtomicReference<List<Boolean>> methodCalls = new AtomicReference<>();
        methodCalls.set(list);

        analyticsService.registerEventCallback("HAGRID_DURABILITY_EVENT", (Map<String, Object> z)->{
            String step = (String)z.get("step");
            if(step.equals("com.freshworks.core.data.durability.steps.is_complete.shutdown.TestIsSyncCompleteShutdown")){

                String method = (String)z.get("method");
                if(method.equalsIgnoreCase("isSyncComplete")){
                    methodCalls.get().add(true);
                }
                else{
                    // fail here
                    methodCalls.get().add(false);
                }
            }

        });


        traverserExecutorService.submit(namespaceStr, dagTraversalService);
        syncStatusService.waitUntilTraverserIsInProgress();
        infraService.destroy();
        assertThat(methodCalls.get().size(), Matchers.is(6));
        assertThat(methodCalls.get().get(0), Matchers.is(false));
        assertThat(methodCalls.get().get(1), Matchers.is(false));
        assertThat(methodCalls.get().get(2), Matchers.is(false));
        assertThat(methodCalls.get().get(3), Matchers.is(false));
        assertThat(methodCalls.get().get(4), Matchers.is(false));
        assertThat(methodCalls.get().get(5), Matchers.is(true));
        assertThat(syncStatusService.getTraverser_status(), Matchers.is(-1));
        assertThat(analyticsService.anyErrorEvent(), Matchers.is(true));
        Thread.sleep(10000);
    }


    // IsSyncComplete can not return null as its return type is boolean not Boolean
    // @Test
    // public void testWhenTraverserStepMethodIsSyncCompleteReturnNullThenStepDoesNotExecuteFurtherStepMethods() throws Exception {

    //     ConnectorConfiguration connectorConfiguration = new ConnectorConfiguration();

    //     SyncServiceContainer syncServiceContainer = applicationContext.getBean(SyncServiceContainer.class);

    //     String namespaceStr = UUID.randomUUID().toString();
    //     NamespaceService namespace = new NamespaceService();
    //     namespace.setNamespace(namespaceStr);
    //     syncServiceContainer.add(namespace);

    //     GlobalNamespaceService singletonUniqueIdentifier = applicationContext.getBean(GlobalNamespaceService.class);
    //     syncServiceContainer.add(singletonUniqueIdentifier);

    //     HttpClientService httpClientService = applicationContext.getBean(HttpClientService.class);
    //     syncServiceContainer.add(httpClientService);

    //     TraverseConfigService traverseConfigService = applicationContext.getBean(TraverseConfigService.class);
    //     traverseConfigService.configure(syncServiceContainer);
    //     syncServiceContainer.add(traverseConfigService);

    //     InfraBeanService infraBeanConfiguration = applicationContext.getBean(InfraBeanService.class);
    //     InfraConfigService infraConfigService = applicationContext.getBean(InfraConfigService.class);
    //     infraConfigService.configure(syncServiceContainer);
    //     InfraService infraService = infraBeanConfiguration.getInfraService(infraConfigService, connectorConfiguration);
    //     infraService.configure(syncServiceContainer, infraConfigService);
    //     syncServiceContainer.add(infraService, InfraService.class);

    //     DagService dagService = applicationContext.getBean(DagService.class);
    //     dagService.configure(syncServiceContainer);
    //     DagNode rootNode = dagService.dagScanner(namespaceStr, traverseConfigService, infraService);

    //     List<Class<? extends  AbstractStep>> addEnabledPath = new ArrayList();
    //     addEnabledPath.add(TestIsSyncCompleteNullCheck.class);

    //     List<List<Class<? extends AbstractStep>>> enabledPathList = new ArrayList<>();
    //     enabledPathList.add(addEnabledPath);

    //     // enable only the path added and disable rest of the path
    //     dagService.enableDisableDagPath(rootNode, enabledPathList);

    //     // Now set this as main root node
    //     dagService.setRootNode(rootNode);

    //     SyncStatusService syncStatusService = syncServiceContainer.getBean(SyncStatusService.class);
    //     syncServiceContainer.add(syncStatusService);

    //     AnalyticsFactory analyticsFactory = syncServiceContainer.getBean(AnalyticsFactory.class);
    //     AnalyticsService analyticsService = analyticsFactory.getAnalyticsService(namespace.getNamespace());
    //     syncServiceContainer.add(analyticsService);
    //     syncServiceContainer.add(analyticsFactory);

    //     ServiceTree serviceTree = applicationContext.getBean(ServiceTree.class);
    //     serviceTree.configure(syncServiceContainer);
    //     syncServiceContainer.add(serviceTree);

    //     assertThat(syncStatusService.getTraverser_status(), Matchers.is(-100));
    //     DagTraversalService dagTraversalService = applicationContext.getBean(DagTraversalService.class);

    //     ImmutableMap<String, String> x = ImmutableMap.<String, String>builder()
    //             .put("numberOfUsersEachPage", "10")
    //             .put("numberOfUserPagination", "1")
    //             .put("waitBetweenUserPaginationInMs", "0")
    //             .put("numberOfPostsEachPage", "10")
    //             .put("numberOfPostPagination", "1")
    //             .put("waitBetweenPostPaginationInMs", "0")
    //             .put("numberOfCommentsEachPage", "100")
    //             .put("numberOfCommentPagination", "1")
    //             .put("waitBetweenCommentPaginationInMs", "0")
    //             .put("numberOfCommunitiesEachPage", "1")
    //             .put("numberOfCommunityPagination", "1")
    //             .put("waitBetweenCommunityPaginationInMs", "0").build();

    //     CountDownLatch latch = new CountDownLatch(rootNode.getNodesInDag().size());
    //     dagTraversalService.configure("/traverser", rootNode, x, new Phaser(), syncServiceContainer);
    //     syncServiceContainer.add(dagTraversalService);
    //     TraverserExecutorService traverserExecutorService = syncServiceContainer.getBean(TraverserExecutorService.class);

    //     List<Boolean> list = new ArrayList<>();
    //     AtomicReference<List<Boolean>> methodCalls = new AtomicReference<>();
    //     methodCalls.set(list);

    //     analyticsService.registerEventCallback("HAGRID_DURABILITY_EVENT", (Map<String, Object> z)->{
    //         String step = (String)z.get("step");
    //         if(step.equals("com.freshworks.core.data.durability.steps.is_complete.null_check.TestIsSyncCompleteNullCheck")){

    //             String method = (String)z.get("method");
    //             System.out.print(method);
    //             if(method.equalsIgnoreCase("isSyncComplete")){
    //                 methodCalls.get().add(true);
    //             }
    //             else{
    //                 // fail here
    //                 methodCalls.get().add(false);
    //             }
    //         }

    //     });


    //     traverserExecutorService.submit(namespaceStr, dagTraversalService);
    //     syncStatusService.waitUntilTraverserIsInProgress();
    //     infraService.destroy();
    //     assertThat(methodCalls.get().size(), Matchers.is(6));
    //     assertThat(methodCalls.get().get(0), Matchers.is(false));
    //     assertThat(methodCalls.get().get(1), Matchers.is(false));
    //     assertThat(methodCalls.get().get(2), Matchers.is(false));
    //     assertThat(methodCalls.get().get(3), Matchers.is(false));
    //     assertThat(methodCalls.get().get(4), Matchers.is(false));
    //     assertThat(methodCalls.get().get(5), Matchers.is(false));
    //     assertThat(methodCalls.get().get(6), Matchers.is(true));
    //     assertThat(syncStatusService.getTraverser_status(), Matchers.is(-1));
    //     assertThat(analyticsService.anyErrorEvent(), Matchers.is(true));
    //     Thread.sleep(10000);
    // }

    @Test
    public void testWhenTraverserIsShutdownByStepMethodGetNextRequestThenStepDoesNotExecuteFurtherStepMethods() throws Exception {

        ConnectorConfiguration connectorConfiguration = new ConnectorConfiguration();

        SyncServiceContainer syncServiceContainer = applicationContext.getBean(SyncServiceContainer.class);

        String namespaceStr = UUID.randomUUID().toString();
        NamespaceService namespace = new NamespaceService();
        namespace.setNamespace(namespaceStr);
        syncServiceContainer.add(namespace);

        GlobalNamespaceService singletonUniqueIdentifier = applicationContext.getBean(GlobalNamespaceService.class);
        syncServiceContainer.add(singletonUniqueIdentifier);

        HttpClientService httpClientService = applicationContext.getBean(HttpClientService.class);
        syncServiceContainer.add(httpClientService);

        TraverseConfigService traverseConfigService = applicationContext.getBean(TraverseConfigService.class);
        traverseConfigService.configure(syncServiceContainer);
        syncServiceContainer.add(traverseConfigService);

        InfraBeanService infraBeanConfiguration = applicationContext.getBean(InfraBeanService.class);
        InfraConfigService infraConfigService = applicationContext.getBean(InfraConfigService.class);
        infraConfigService.configure(syncServiceContainer);
        InfraService infraService = infraBeanConfiguration.getInfraService(infraConfigService);
        infraService.configure(syncServiceContainer, infraConfigService);
        syncServiceContainer.add(infraService, InfraService.class);

        DagService dagService = applicationContext.getBean(DagService.class);
        dagService.configure(syncServiceContainer);
        DagNode rootNode = dagService.dagStaticStepScanner(namespaceStr, traverseConfigService, infraService);


        List<Class<? extends  AbstractStep>> addEnabledPath = new ArrayList();
        addEnabledPath.add(TestGetNextRequestShutdown.class);

        List<List<Class<? extends AbstractStep>>> enabledPathList = new ArrayList<>();
        enabledPathList.add(addEnabledPath);

        // enable only the path added and disable rest of the path
        dagService.enableDisableDagPath(rootNode, enabledPathList);

        // Now set this as main root node
        dagService.setRootNode(rootNode);

        SyncStatusService syncStatusService = syncServiceContainer.getBean(SyncStatusService.class);
        syncServiceContainer.add(syncStatusService);

        AnalyticsFactory analyticsFactory = syncServiceContainer.getBean(AnalyticsFactory.class);
        AnalyticsService analyticsService = analyticsFactory.getAnalyticsService(namespace.getNamespace());
        syncServiceContainer.add(analyticsService);
        syncServiceContainer.add(analyticsFactory);

        ServiceTree serviceTree = applicationContext.getBean(ServiceTree.class);
        serviceTree.configure(syncServiceContainer);
        syncServiceContainer.add(serviceTree);

        assertThat(syncStatusService.getTraverser_status(), Matchers.is(-100));
        DagTraversalService dagTraversalService = applicationContext.getBean(DagTraversalService.class);

        ImmutableMap<String, String> x = ImmutableMap.<String, String>builder()
                .put("numberOfUsersEachPage", "10")
                .put("numberOfUserPagination", "2")
                .put("waitBetweenUserPaginationInMs", "0")
                .put("numberOfPostsEachPage", "10")
                .put("numberOfPostPagination", "2")
                .put("waitBetweenPostPaginationInMs", "0")
                .put("numberOfCommentsEachPage", "100")
                .put("numberOfCommentPagination", "2")
                .put("waitBetweenCommentPaginationInMs", "0")
                .put("numberOfCommunitiesEachPage", "1")
                .put("numberOfCommunityPagination", "2")
                .put("waitBetweenCommunityPaginationInMs", "0").build();

        CountDownLatch latch = new CountDownLatch(rootNode.getNodesInDag().size());
        dagTraversalService.configure("/traverser", rootNode, x,  new Phaser(), syncServiceContainer);
        syncServiceContainer.add(dagTraversalService);
        TraverserExecutorService traverserExecutorService = syncServiceContainer.getBean(TraverserExecutorService.class);

        List<Boolean> list = new ArrayList<>();
        AtomicReference<List<Boolean>> methodCalls = new AtomicReference<>();
        methodCalls.set(list);

        analyticsService.registerEventCallback("HAGRID_DURABILITY_EVENT", (Map<String, Object> z)->{
            String step = (String)z.get("step");
            if(step.equals("com.freshworks.core.data.durability.steps.next_request.shutdown.TestGetNextRequestShutdown")){

                String method = (String)z.get("method");
                if(method.equalsIgnoreCase("getNextSyncRequest")){
                    methodCalls.get().add(true);
                }
                else{
                    // fail here
                    methodCalls.get().add(false);
                }
            }

        });


        traverserExecutorService.submit(namespaceStr, dagTraversalService);
        syncStatusService.waitUntilTraverserIsInProgress();
        infraService.destroy();
        assertThat(methodCalls.get().size(), Matchers.is(7));
        assertThat(methodCalls.get().get(0), Matchers.is(false));
        assertThat(methodCalls.get().get(1), Matchers.is(false));
        assertThat(methodCalls.get().get(2), Matchers.is(false));
        assertThat(methodCalls.get().get(3), Matchers.is(false));
        assertThat(methodCalls.get().get(4), Matchers.is(false));
        assertThat(methodCalls.get().get(5), Matchers.is(false));
        assertThat(methodCalls.get().get(6), Matchers.is(true));
        assertThat(syncStatusService.getTraverser_status(), Matchers.is(-1));
        assertThat(analyticsService.anyErrorEvent(), Matchers.is(true));
        Thread.sleep(10000);
    }

    @Test
    public void testWhenTraverserStepMethodGetNextRequestReturnNullThenStepDoesNotExecuteFurtherStepMethods() throws Exception {

        ConnectorConfiguration connectorConfiguration = new ConnectorConfiguration();

        SyncServiceContainer syncServiceContainer = applicationContext.getBean(SyncServiceContainer.class);

        String namespaceStr = UUID.randomUUID().toString();
        NamespaceService namespace = new NamespaceService();
        namespace.setNamespace(namespaceStr);
        syncServiceContainer.add(namespace);

        GlobalNamespaceService singletonUniqueIdentifier = applicationContext.getBean(GlobalNamespaceService.class);
        syncServiceContainer.add(singletonUniqueIdentifier);

        HttpClientService httpClientService = applicationContext.getBean(HttpClientService.class);
        syncServiceContainer.add(httpClientService);

        TraverseConfigService traverseConfigService = applicationContext.getBean(TraverseConfigService.class);
        traverseConfigService.configure(syncServiceContainer);
        syncServiceContainer.add(traverseConfigService);

        InfraBeanService infraBeanConfiguration = applicationContext.getBean(InfraBeanService.class);
        InfraConfigService infraConfigService = applicationContext.getBean(InfraConfigService.class);
        infraConfigService.configure(syncServiceContainer);
        InfraService infraService = infraBeanConfiguration.getInfraService(infraConfigService);
        infraService.configure(syncServiceContainer, infraConfigService);
        syncServiceContainer.add(infraService, InfraService.class);

        DagService dagService = applicationContext.getBean(DagService.class);
        DagNode rootNode = dagService.dagStaticStepScanner(namespaceStr, traverseConfigService, infraService);

        SyncStatusService syncStatusService = syncServiceContainer.getBean(SyncStatusService.class);
        syncServiceContainer.add(syncStatusService);

        AnalyticsFactory analyticsFactory = syncServiceContainer.getBean(AnalyticsFactory.class);
        AnalyticsService analyticsService = analyticsFactory.getAnalyticsService(namespace.getNamespace());
        syncServiceContainer.add(analyticsService);
        syncServiceContainer.add(analyticsFactory);

        ServiceTree serviceTree = applicationContext.getBean(ServiceTree.class);
        serviceTree.configure(syncServiceContainer);
        syncServiceContainer.add(serviceTree);

        assertThat(syncStatusService.getTraverser_status(), Matchers.is(-100));
        DagTraversalService dagTraversalService = applicationContext.getBean(DagTraversalService.class);

        ImmutableMap<String, String> x = ImmutableMap.<String, String>builder()
                .put("numberOfUsersEachPage", "10")
                .put("numberOfUserPagination", "2")
                .put("waitBetweenUserPaginationInMs", "0")
                .put("numberOfPostsEachPage", "10")
                .put("numberOfPostPagination", "2")
                .put("waitBetweenPostPaginationInMs", "0")
                .put("numberOfCommentsEachPage", "100")
                .put("numberOfCommentPagination", "2")
                .put("waitBetweenCommentPaginationInMs", "0")
                .put("numberOfCommunitiesEachPage", "2")
                .put("numberOfCommunityPagination", "2")
                .put("waitBetweenCommunityPaginationInMs", "0").build();

        CountDownLatch latch = new CountDownLatch(rootNode.getNodesInDag().size());
        dagTraversalService.configure("/traverser", rootNode, x, new Phaser(), syncServiceContainer);
        syncServiceContainer.add(dagTraversalService);
        TraverserExecutorService traverserExecutorService = syncServiceContainer.getBean(TraverserExecutorService.class);

        List<Boolean> list = new ArrayList<>();
        AtomicReference<List<Boolean>> methodCalls = new AtomicReference<>();
        methodCalls.set(list);

        analyticsService.registerEventCallback("HAGRID_DURABILITY_EVENT", (Map<String, Object> z)->{
            String step = (String)z.get("step");
            if(step.equals("com.freshworks.core.data.durability.steps.next_request.null_check.TestGetNextRequestNullCheck")){

                String method = (String)z.get("method");
                if(method.equalsIgnoreCase("getNextSyncRequest")){
                    methodCalls.get().add(true);
                }
                else{
                    // fail here
                    methodCalls.get().add(false);
                }
            }

        });


        traverserExecutorService.submit(namespaceStr, dagTraversalService);
        syncStatusService.waitUntilTraverserIsInProgress();
        infraService.destroy();
        assertThat(methodCalls.get().size(), Matchers.is(8));
        assertThat(methodCalls.get().get(0), Matchers.is(false));
        assertThat(methodCalls.get().get(1), Matchers.is(false));
        assertThat(methodCalls.get().get(2), Matchers.is(false));
        assertThat(methodCalls.get().get(3), Matchers.is(false));
        assertThat(methodCalls.get().get(4), Matchers.is(false));
        assertThat(methodCalls.get().get(5), Matchers.is(false));
        assertThat(methodCalls.get().get(6), Matchers.is(false));
        assertThat(methodCalls.get().get(7), Matchers.is(true));
        assertThat(syncStatusService.getTraverser_status(), Matchers.is(-1));
        assertThat(analyticsService.anyErrorEvent(), Matchers.is(true));
        Thread.sleep(10000);
    }
}
