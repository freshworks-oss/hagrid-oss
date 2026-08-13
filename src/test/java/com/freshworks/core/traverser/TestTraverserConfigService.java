package com.freshworks.core.traverser;



import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.freshworks.core.shared.MockFacadeSyncServiceContainer;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.sync.ConnectorConfiguration;
import com.freshworks.core.shared.sync.ConnectorConfiguration.StepRateLimitObject;


@SpringBootTest
@ActiveProfiles(value = "unit")
public class TestTraverserConfigService{

    @Autowired
    MockFacadeTraverseConfigService mockTraverseConfigService;

    @Autowired
    MockFacadeSyncServiceContainer mockFacadeSyncServiceContainer;

    @Autowired
    MockFacadeDagNode mockFacadeDagNode;


    @BeforeEach
    public void beforeEach() throws Exception {
        mockTraverseConfigService.configure().build();
        mockFacadeDagNode.configure().build();
        mockFacadeSyncServiceContainer.configure().build();
    }


    @Test
    public void testCorrectTraverserThreadCountPickedUp() throws Exception {

        ConnectorConfiguration connectorConfiguration = new ConnectorConfiguration();
        connectorConfiguration.setTraverserThreadCount(100);

        TraverseConfigService traverseConfigService = mockTraverseConfigService.build();

        SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer
                .build();
        syncServiceContainer.add(connectorConfiguration, ConnectorConfiguration.class);

        traverseConfigService.configure(syncServiceContainer);

        doCallRealMethod().when(traverseConfigService).getTraverserThreadCount();
        assertThat(traverseConfigService.getTraverserThreadCount(), is(100));

    }


    @Test
    public void testTraverseWhenLoadedConfigureRateLimitViaFreshHierarchyAnnotation() throws Exception {

        Class<? extends AbstractStep> sc = (Class<? extends AbstractStep>) Class.forName("com.freshworks.core.data.unit.dag.steps.TestServicePrinciple");

        DagNode servicePrincipal = mockFacadeDagNode
                .name(sc)
                .build();

        Class<? extends AbstractStep> c = (Class<? extends AbstractStep>) Class.forName("com.freshworks.core.data.unit.dag.steps.TestApplication");
        DagNode applicationNode = mockFacadeDagNode
                .name(c)
                .children(new LinkedHashMap<>(Map.of(servicePrincipal, new NodeRelationship())))
                .build();

        HashMap<String, Object> hagridManagedSyncServiceContainer = new HashMap<>();
        hagridManagedSyncServiceContainer.put(DagNode.class.getName(), applicationNode);

        SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer
                .hagridManagedBeans(hagridManagedSyncServiceContainer)
                .build();

        ConnectorConfiguration connectorConfiguration = new ConnectorConfiguration();
        syncServiceContainer.add(connectorConfiguration, ConnectorConfiguration.class);


        TraverseConfigService traverseConfigService = mockTraverseConfigService
                .build();

        traverseConfigService.configure(syncServiceContainer);

        doCallRealMethod().when(traverseConfigService).getRateLimitForStep(any());

        StepRateLimitObject stepRateLimitNode = traverseConfigService.getRateLimitForStep(c);

        assertThat(stepRateLimitNode.getNumberOfApiCalls(), is(20));
        assertThat(stepRateLimitNode.getDurationInSeconds(), is(100));


    
        stepRateLimitNode = traverseConfigService.getRateLimitForStep(sc);

        assertThat(stepRateLimitNode.getNumberOfApiCalls(), is(800));
        assertThat(stepRateLimitNode.getDurationInSeconds(), is(1));


    }
}