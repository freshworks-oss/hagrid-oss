package com.freshworks.core.traverser;



import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.fasterxml.jackson.databind.JsonNode;
import com.freshworks.core.shared.MockFacadeSyncServiceContainer;
import com.freshworks.core.shared.SyncServiceContainer;


@SpringBootTest
@EnabledIfSystemProperty(named = "spring.profiles.active", matches = ".*\\.unit\\..*")
public class TestTraverserConfigService{

    @Autowired
    MockFacadeTraverseConfigService mockTraverseConfigService;

    @Autowired
    MockFacadeSyncServiceContainer mockFacadeSyncServiceContainer;

    @Autowired
    MockFacadeDagNode mockFacadeDagNode;

    String releaseVersion;


    @BeforeEach
    public void beforeEach() throws Exception {

        releaseVersion = System.getProperty("spring.profiles.active").split("\\.")[0];
        mockTraverseConfigService.configure().build();
        mockFacadeDagNode.configure().build();
        mockFacadeSyncServiceContainer.configure().build();
    }


    @Test
    public void testCorrectTraverserThreadCountPickedUp() throws Exception {

        TraverseConfigService traverseConfigService = mockTraverseConfigService.build();
        doCallRealMethod().when(traverseConfigService).getTraverserThreadCount();
        assertThat(traverseConfigService.getTraverserThreadCount(), is(1));

    }

    @Test
    public void testCorrectStepLocationPickedUp() throws Exception {
        TraverseConfigService traverseConfigService = mockTraverseConfigService.build();
        doCallRealMethod().when(traverseConfigService).getStepLocation();
        assertThat(traverseConfigService.getStepLocation(), is("com.freshworks.core.data."+ releaseVersion + ".unit.fb.steps"));
    }

    @Test
    public void testCorrectBeanLocationPickedUp() throws Exception {
        TraverseConfigService traverseConfigService = mockTraverseConfigService.build();
        doCallRealMethod().when(traverseConfigService).getBeanLocation();
        assertThat(traverseConfigService.getBeanLocation(), is("com.freshworks.core.data." + releaseVersion + ".unit.fb.beans"));
    }


    @Test
    public void testTraverseWhenLoadedConfigureRateLimitViaFreshHierarchyAnnotation() throws Exception {

        Class<? extends AbstractStep> sc = (Class<? extends AbstractStep>) Class.forName("com.freshworks.core.data." + releaseVersion + ".unit.dag.steps.TestServicePrinciple");

        DagNode servicePrincipal = mockFacadeDagNode
                .name(sc)
                .build();

        Class<? extends AbstractStep> c = (Class<? extends AbstractStep>) Class.forName("com.freshworks.core.data." + releaseVersion + ".unit.dag.steps.TestApplication");
        DagNode applicationNode = mockFacadeDagNode
                .name(c)
                .children(new LinkedHashMap<>(Map.of(servicePrincipal, new Relationship())))
                .build();

        HashMap<String, Object> hagridManagedSyncServiceContainer = new HashMap<>();
        hagridManagedSyncServiceContainer.put(DagNode.class.getName(), applicationNode);

        SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer
                .hagridManagedBeans(hagridManagedSyncServiceContainer)
                .build();

        TraverseConfigService traverseConfigService = mockTraverseConfigService
                .syncServiceContainer(syncServiceContainer)
                .build();

        doCallRealMethod().when(traverseConfigService).setRateLimitForStep(any(), anyInt(), anyInt());
        doCallRealMethod().when(traverseConfigService).getRateLimitForStep(any());
        doCallRealMethod().when(traverseConfigService).setStepLocation(anyString());

        traverseConfigService.setStepLocation("com.freshworks.core.data." + releaseVersion + ".unit.dag.steps");
        JsonNode stepRateLimitNode = traverseConfigService.getRateLimitForStep(c);

        assertThat(stepRateLimitNode.has("api_count"), is(true));
        assertThat(stepRateLimitNode.get("api_count").asInt(), is(20));
        assertThat(stepRateLimitNode.has("seconds"), is(true));
        assertThat(stepRateLimitNode.get("seconds").asInt(), is(100));


    
        stepRateLimitNode = traverseConfigService.getRateLimitForStep(sc);

        assertThat(stepRateLimitNode.has("api_count"), is(true));
        assertThat(stepRateLimitNode.get("api_count").asInt(), is(800));
        assertThat(stepRateLimitNode.has("seconds"), is(true));
        assertThat(stepRateLimitNode.get("seconds").asInt(), is(1));


    }
}