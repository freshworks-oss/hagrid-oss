package com.freshworks.core.traverser;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Phaser;
import java.util.concurrent.Semaphore;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.freshworks.core.shared.MockFacadeSyncServiceContainer;
import com.freshworks.core.shared.Namespace;
import com.freshworks.core.shared.SimpleMockUtility;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.executor.SharedExecutorService;
import com.freshworks.core.shared.infra.InfraService;
import com.freshworks.core.shared.infra.persistent.MockFacadeMongoDbService;
import com.freshworks.core.shared.infra.persistent.MockFacadeMongodbList;
import com.freshworks.core.shared.infra.persistent.MongoDbList;
import com.google.common.collect.ImmutableMap;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;

@SpringBootTest
@EnabledIfSystemProperty(named = "spring.profiles.active", matches = ".*\\.unit\\..*")
public class TestDagNodePerParentTraversalService {


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

    @Autowired
    DagNodePerParentTraversalService dagNodePerParentTraversalService;

    Class< ? extends AbstractStep> application;

    String releaseVersion;

    @BeforeEach
    public void Mock() throws Exception {

        releaseVersion = System.getProperty("spring.profiles.active").split("\\.")[0];

        mockFacadeDagNodeTraversal.configure().build();
        mockFacadeMongoDbService.configure().build();
        mockFacadeMongodbList.configure().build();
        mockFacadeTraverseConfigService.configure().build();
        dagNodeMockFacade.configure().build();
        mockFacadeSyncServiceContainer.configure().build();

        application = (Class<? extends AbstractStep>) Class.forName("com.freshworks.core.data." + releaseVersion  + ".unit.dag.steps.TestApplication");
    }

    @Test
    public void testDagNodeTraverserCreatesNewStepObjectWhenStepIsPrototypeForEveryParentObject() throws Exception {

        Namespace namespace = new Namespace();
        namespace.setNamespace("random_namespace");

        SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer
                .add(namespace, Namespace.class)
                .build();

        List<String> data = new ArrayList<>();
        data.add("{\"name\":\"amit\"}");
        data.add("{\"name\":\"rahul\"}");

        MongoDbList m = mockFacadeMongodbList
                .getNFromStartIndex(data)
                .build();

        InfraService mongoService = mockFacadeMongoDbService
                .getInfraDbListGivenName(m)
                .build();

        ImmutableMap<String, String> immutableMap = lowLevelMockUtility.mockImmutableMap();

        DagNode parentNode = dagNodeMockFacade
                .name(ParentStep.class)
                .build();

        DagNode nodeToTraverse = dagNodeMockFacade
                .hasMoreData(true, false)
                .name(application)
                .parentList(new LinkedHashMap<>(Map.of(parentNode, new Relationship())))
                .build();

        parentNode.setChildrenRelationshipMap(new LinkedHashMap<>(Map.of(nodeToTraverse, new Relationship())));

        TraverseConfigService traverseConfigService = mockFacadeTraverseConfigService
                .build();
        doCallRealMethod().when(traverseConfigService).configure(any());
        doCallRealMethod().when(traverseConfigService).getRateLimitForStep(any());

        traverseConfigService.configure(syncServiceContainer);

        Phaser phaser = new Phaser(1);
        CountDownLatch countDownLatch = lowLevelMockUtility.mockCountDownLatch();


        List<AbstractStep> list = new ArrayList<>();
        doAnswer(new Answer<AbstractStep>() {

            @Override
            public AbstractStep answer(InvocationOnMock invocationOnMock) throws Throwable {

                AbstractStep abstractStep  = (AbstractStep) invocationOnMock.callRealMethod();
                list.add(abstractStep);
                return abstractStep;
            }
        }).when(syncServiceContainer).getBean(application.getName());


        int rateLimit = 10;
        int rateLimitDuration = 1;
        Bucket rateLimitBucket = Bucket.builder().addLimit(Bandwidth.simple(rateLimit, Duration.ofSeconds(rateLimitDuration))).build();

        SharedExecutorService sharedExecutorService = syncServiceContainer.getBean(SharedExecutorService.class);
        dagNodePerParentTraversalService.configure("/traverser", phaser, syncServiceContainer, nodeToTraverse, parentNode, mongoService, traverseConfigService, immutableMap, new Semaphore(100), rateLimitBucket );
        sharedExecutorService.submit(namespace.getNamespace(), dagNodePerParentTraversalService).get();

        assertThat(list.size(), greaterThan(1));
        assertThat(list.get(0), instanceOf(application));
        assertThat(list.get(1), instanceOf(application));
        assertThat(list.get(0).hashCode(), is(not(list.get(1).hashCode())));
    }
}
