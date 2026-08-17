package com.freshworks.core.integration.traverser.test;

import com.freshworks.core.data.integration.fb.steps.FbComment;
import com.freshworks.core.data.integration.fb.steps.FbCommunity;
import com.freshworks.core.data.integration.fb.steps.FbPost;
import com.freshworks.core.data.integration.fb.steps.FbUser;
import com.freshworks.core.data.integration.recursive.contextual.steps.TokenGenerator;
import com.freshworks.core.data.integration.recursive.contextual.steps.TokenPublisher;
import com.freshworks.core.data.integration.recursive.contextual.steps.TokenRoute;
import com.freshworks.core.data.integration.recursive.contextual.steps.TokenTransformation;
import com.freshworks.core.data.integration.recursive.json.steps.JsonGenerator;
import com.freshworks.core.data.integration.recursive.json.steps.JsonTraverser;
import com.freshworks.core.processor.MockFacadeProcessorService;
import com.freshworks.core.shared.NamespaceService;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.analytics.AnalyticsFactory;
import com.freshworks.core.shared.executor.SharedExecutorService;
import com.freshworks.core.shared.infra.InfraBeanService;
import com.freshworks.core.shared.infra.InfraConfigService;
import com.freshworks.core.shared.infra.InfraService;
import com.freshworks.core.shared.sync.ConnectorConfiguration;
import com.freshworks.core.shared.sync.SyncService;
import com.freshworks.core.shared.sync.SyncStatusService;
import com.freshworks.core.shared.synchronizers.ServiceTree;
import com.freshworks.core.shared.synchronizers.GlobalNamespaceService;
import com.freshworks.core.traverser.*;
import com.freshworks.core.traverser.net.http.HttpClientService;
import com.google.common.collect.ImmutableMap;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.mockserver.integration.ClientAndServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@AutoConfigureObservability
@EnabledIfSystemProperty(named = "spring.profiles.active", matches = "integration")
public class TestTraverser {

    static String infraType;

    @Autowired
    ConnectorConfiguration connectorConfiguration;

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    MockFacadeProcessorService mockFacadeProcessorService;

    private static ClientAndServer server;

    @BeforeAll
    public static void beforeAll() throws IOException {
    }


    @BeforeEach
    public void beforeEach() throws Exception {
        mockFacadeProcessorService.configure().build();
    }

    @Test
    public void testTraverserIsRunningSuccessfully() throws Exception {

        ConnectorConfiguration connectorConfiguration = new ConnectorConfiguration();

        // List<Class<? extends AbstractStep>> enabledPath = new ArrayList<>();
        // enabledPath.add(TokenGenerator.class);
        // enabledPath.add(TokenRoute.class);
        // enabledPath.add(TokenPublisher.class);
        // connectorConfiguration.addPathToEnable(enabledPath);

        // enabledPath = new ArrayList<>();
        // enabledPath.add(TokenGenerator.class);
        // enabledPath.add(TokenRoute.class);
        // enabledPath.add(TokenTransformation.class);
        // connectorConfiguration.addPathToEnable(enabledPath);

        SyncServiceContainer syncServiceContainer = applicationContext.getBean(SyncServiceContainer.class);

        AnalyticsFactory analyticsFactory = applicationContext.getBean(AnalyticsFactory.class);
        syncServiceContainer.add(analyticsFactory);
        
        NamespaceService namespace = new NamespaceService();
        namespace.setNamespace(UUID.randomUUID().toString());
        syncServiceContainer.add(namespace);

        TraverseConfigService traverseConfigService = applicationContext.getBean(TraverseConfigService.class);
        traverseConfigService.configure(syncServiceContainer);
        syncServiceContainer.add(traverseConfigService);
        syncServiceContainer.add(connectorConfiguration);


        InfraBeanService infraBeanConfiguration = applicationContext.getBean(InfraBeanService.class);
        InfraConfigService infraConfigService = applicationContext.getBean(InfraConfigService.class);
        infraConfigService.configure(syncServiceContainer);
        InfraService infraService = infraBeanConfiguration.getInfraService(infraConfigService, connectorConfiguration);
        infraService.configure(syncServiceContainer, infraConfigService);
        syncServiceContainer.add(infraService, InfraService.class);

//        SingletonUniqueIdentifier singletonUniqueIdentifier = applicationContext.getBean(SingletonUniqueIdentifier.class);
//        syncServiceContainer.add(singletonUniqueIdentifier);
//
//        HttpClientService httpClientService = applicationContext.getBean(HttpClientService.class);
//        httpClientService.configure(syncServiceContainer);
//        syncServiceContainer.add(httpClientService);


        DagService dagService = applicationContext.getBean(DagService.class);
        dagService.configure(syncServiceContainer);
        DagNode rootNode = dagService.dagScanner(namespace.getNamespace(), traverseConfigService, infraService);

        SyncStatusService syncStatusService = syncServiceContainer.getBean(SyncStatusService.class);
        syncServiceContainer.add(syncStatusService);

        ServiceTree serviceTree = applicationContext.getBean(ServiceTree.class);
        serviceTree.configure(syncServiceContainer);
        syncServiceContainer.add(serviceTree);

        assertThat(syncStatusService.getTraverser_status(), Matchers.is(-100));
        DagTraversalService dagTraversalService = applicationContext.getBean(DagTraversalService.class);

        ImmutableMap<String, String> x = ImmutableMap.<String, String>builder()
                .put("numberOfUsersEachPage", "1")
                .put("numberOfUserPagination", "1")
                .put("waitBetweenUserPaginationInMs", "0")
                .put("numberOfPostsEachPage", "10")
                .put("numberOfPostPagination", "10")
                .put("waitBetweenPostPaginationInMs", "0")
                .put("numberOfCommentsEachPage", "1")
                .put("numberOfCommentPagination", "1")
                .put("waitBetweenCommentPaginationInMs", "0")
                .put("numberOfCommunitiesEachPage", "1")
                .put("numberOfCommunityPagination", "1")
                .put("waitBetweenCommunityPaginationInMs", "0").build();

        CountDownLatch latch = new CountDownLatch(rootNode.getNodesInDag().size());

        NodeCycleService nodeCycleService = applicationContext.getBean(NodeCycleService.class);
        nodeCycleService.configure("/traverser", 1000 , namespace, rootNode, analyticsFactory);

        dagTraversalService.configure("/traverser", rootNode, x, new Phaser(), syncServiceContainer);
        SharedExecutorService sharedExecutorService = syncServiceContainer.getBean(SharedExecutorService.class);
        sharedExecutorService.submit(namespace.getNamespace(), dagTraversalService);
        sharedExecutorService.submit(namespace.getNamespace(), nodeCycleService);
        syncStatusService.waitUntilTraverserIsInProgress();

        // TODO: Here I am not handling the situation where what if traverser has failed for any of the parent Item
        // Kind of partial success case.
        assertThat(syncStatusService.getTraverser_status(), Matchers.is(1));

        infraService.destroy();
    }


    @Test
    public void testWhenTraverserExplicitlyShutdownThenTraverserFailed() throws Exception {

        SyncServiceContainer syncServiceContainer = applicationContext.getBean(SyncServiceContainer.class);

        AnalyticsFactory analyticsFactory = applicationContext.getBean(AnalyticsFactory.class);
        syncServiceContainer.add(analyticsFactory);

        NamespaceService namespace = new NamespaceService();
        namespace.setNamespace(UUID.randomUUID().toString());
        syncServiceContainer.add(namespace);

        TraverseConfigService traverseConfigService = applicationContext.getBean(TraverseConfigService.class);
        traverseConfigService.configure(syncServiceContainer);
        syncServiceContainer.add(traverseConfigService);

        TraverserExecutorService traverserExecutorService = applicationContext.getBean(TraverserExecutorService.class);
        syncServiceContainer.add(traverserExecutorService);

        InfraBeanService infraBeanConfiguration = applicationContext.getBean(InfraBeanService.class);
        InfraConfigService infraConfigService = applicationContext.getBean(InfraConfigService.class);
        infraConfigService.configure(syncServiceContainer);
        InfraService infraService = infraBeanConfiguration.getInfraService(infraConfigService, connectorConfiguration);
        infraService.configure(syncServiceContainer, infraConfigService);
        syncServiceContainer.add(infraService, InfraService.class);


        GlobalNamespaceService singletonUniqueIdentifier = applicationContext.getBean(GlobalNamespaceService.class);
        syncServiceContainer.add(singletonUniqueIdentifier);

        HttpClientService httpClientService = applicationContext.getBean(HttpClientService.class);
        syncServiceContainer.add(httpClientService);

        DagService dagService = applicationContext.getBean(DagService.class);
        dagService.configure(syncServiceContainer);
        DagNode rootNode = dagService.dagScanner(namespace.getNamespace(), traverseConfigService, infraService);

        SyncStatusService syncStatusService = syncServiceContainer.getBean(SyncStatusService.class);
        syncStatusService.configure(syncServiceContainer);
        syncServiceContainer.add(syncStatusService);

        ServiceTree serviceTree = applicationContext.getBean(ServiceTree.class);
        serviceTree.configure(syncServiceContainer);
        syncServiceContainer.add(serviceTree);

        assertThat(syncStatusService.getTraverser_status(), Matchers.is(-100));
        DagTraversalService dagTraversalService = applicationContext.getBean(DagTraversalService.class);
        ImmutableMap<String, String> x = ImmutableMap.<String, String>builder()
                .put("numberOfUsersEachPage", "100")
                .put("numberOfUserPagination", "1000")
                .put("waitBetweenUserPaginationInMs", "0")
                .put("numberOfPostsEachPage", "1")
                .put("numberOfPostPagination", "1")
                .put("waitBetweenPostPaginationInMs", "0")
                .put("numberOfCommentsEachPage", "100")
                .put("numberOfCommentPagination", "1000")
                .put("waitBetweenCommentPaginationInMs", "0")
                .put("numberOfCommunitiesEachPage", "1")
                .put("numberOfCommunityPagination", "1")
                .put("waitBetweenCommunityPaginationInMs", "0").build();

        CountDownLatch latch = new CountDownLatch(rootNode.getNodesInDag().size());
        dagTraversalService.configure("/traverser",rootNode, x, new Phaser(), syncServiceContainer);
        SharedExecutorService sharedExecutorService = syncServiceContainer.getBean(SharedExecutorService.class);
        sharedExecutorService.submit(namespace.getNamespace(), dagTraversalService);

        TimeUnit.SECONDS.sleep(10);

        // Here I am terminating all services running under /traverser
        dagTraversalService.interruptSync();

        syncStatusService.waitUntilSyncIsInProgress();
        assertThat(syncStatusService.getTraverser_status(), Matchers.is(-1));
        infraService.destroy();
    }

    @Test
    public void testWhenTraverserExplicitlyShutdownThenResubmitOfTraverseShouldWork() throws Exception {

        SyncStatusService syncStatusService = runTraverser();
        assertThat(syncStatusService.getTraverser_status(), Matchers.is(-1));

        syncStatusService = runTraverser();
        assertThat(syncStatusService.getTraverser_status(), Matchers.is(-1));

    }

    private SyncStatusService runTraverser() throws Exception {

        SyncServiceContainer syncServiceContainer = applicationContext.getBean(SyncServiceContainer.class);

        NamespaceService namespace = new NamespaceService();
        namespace.setNamespace(UUID.randomUUID().toString());
        syncServiceContainer.add(namespace);

        TraverseConfigService traverseConfigService = applicationContext.getBean(TraverseConfigService.class);
        traverseConfigService.configure(syncServiceContainer);
        syncServiceContainer.add(traverseConfigService);

        TraverserExecutorService traverserExecutorService = applicationContext.getBean(TraverserExecutorService.class);
        syncServiceContainer.add(traverserExecutorService);

        InfraBeanService infraBeanConfiguration = applicationContext.getBean(InfraBeanService.class);
        InfraConfigService infraConfigService = applicationContext.getBean(InfraConfigService.class);
        infraConfigService.configure(syncServiceContainer);
        InfraService infraService = infraBeanConfiguration.getInfraService(infraConfigService, connectorConfiguration);
        infraService.configure(syncServiceContainer, infraConfigService);
        syncServiceContainer.add(infraService, InfraService.class);


        GlobalNamespaceService singletonUniqueIdentifier = applicationContext.getBean(GlobalNamespaceService.class);
        syncServiceContainer.add(singletonUniqueIdentifier);

        HttpClientService httpClientService = applicationContext.getBean(HttpClientService.class);
        syncServiceContainer.add(httpClientService);

        DagService dagService = applicationContext.getBean(DagService.class);
        DagNode rootNode = dagService.dagScanner(namespace.getNamespace(), traverseConfigService, infraService);

        SyncStatusService syncStatusService = syncServiceContainer.getBean(SyncStatusService.class);
        syncServiceContainer.add(syncStatusService);

        ServiceTree serviceTree = applicationContext.getBean(ServiceTree.class);
        serviceTree.configure(syncServiceContainer);
        syncServiceContainer.add(serviceTree);

        assertThat(syncStatusService.getTraverser_status(), Matchers.is(-100));
        DagTraversalService dagTraversalService = applicationContext.getBean(DagTraversalService.class);
        ImmutableMap<String, String> x = ImmutableMap.<String, String>builder()
                .put("numberOfUsersEachPage", "100")
                .put("numberOfUserPagination", "1000")
                .put("waitBetweenUserPaginationInMs", "0")
                .put("numberOfPostsEachPage", "1")
                .put("numberOfPostPagination", "1")
                .put("waitBetweenPostPaginationInMs", "0")
                .put("numberOfCommentsEachPage", "100")
                .put("numberOfCommentPagination", "1000")
                .put("waitBetweenCommentPaginationInMs", "0")
                .put("numberOfCommunitiesEachPage", "1")
                .put("numberOfCommunityPagination", "1")
                .put("waitBetweenCommunityPaginationInMs", "0").build();

        CountDownLatch latch = new CountDownLatch(rootNode.getNodesInDag().size());
        dagTraversalService.configure("/traverser",rootNode, x, new Phaser(), syncServiceContainer);
        SharedExecutorService sharedExecutorService = syncServiceContainer.getBean(SharedExecutorService.class);
        sharedExecutorService.submit(namespace.getNamespace(), dagTraversalService);

        TimeUnit.SECONDS.sleep(10);

        // Here I am terminating all services running under /traverser
        dagTraversalService.interruptSync();
        infraService.destroy();

        return syncStatusService;
    }

    @Test
    public void testWhenConnectorHasSelfRecursiveStep() throws Exception{

        SyncService syncService = applicationContext.getBean(SyncService.class);
        String namespace = UUID.randomUUID().toString();

        SyncServiceContainer syncServiceContainer = syncService.startSync( ParentStep.class,namespace,null, connectorConfiguration);
        SyncStatusService syncStatusService = syncServiceContainer.getBean(SyncStatusService.class);
        syncStatusService.waitUntilSyncIsInProgress();
        System.out.println("Sync is done");

    }
}
