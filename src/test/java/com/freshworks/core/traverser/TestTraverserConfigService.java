package com.freshworks.core.traverser;



import com.fasterxml.jackson.databind.JsonNode;
import com.freshworks.core.data.four_zero_zero.unit.dag.steps.TestApplication;
import com.freshworks.core.data.four_zero_zero.unit.dag.steps.TestServicePrinciple;
import com.freshworks.core.shared.MockFacadeSyncServiceContainer;
import com.freshworks.core.shared.SyncServiceContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;


import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doCallRealMethod;


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


        DagNode servicePrincipal = mockFacadeDagNode
                .name(TestServicePrinciple.class)
                .build();

        DagNode applicationNode = mockFacadeDagNode
                .name(TestApplication.class)
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
        JsonNode stepRateLimitNode = traverseConfigService.getRateLimitForStep(TestApplication.class);

        assertThat(stepRateLimitNode.has("api_count"), is(true));
        assertThat(stepRateLimitNode.get("api_count").asInt(), is(20));
        assertThat(stepRateLimitNode.has("seconds"), is(true));
        assertThat(stepRateLimitNode.get("seconds").asInt(), is(100));


        stepRateLimitNode = traverseConfigService.getRateLimitForStep(TestServicePrinciple.class);

        assertThat(stepRateLimitNode.has("api_count"), is(true));
        assertThat(stepRateLimitNode.get("api_count").asInt(), is(800));
        assertThat(stepRateLimitNode.has("seconds"), is(true));
        assertThat(stepRateLimitNode.get("seconds").asInt(), is(1));


    }
}