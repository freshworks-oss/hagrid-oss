package com.freshworks.hagrid.shared.sync;

import com.fasterxml.jackson.databind.JsonNode;
import com.freshworks.hagrid.data.unit.dag.steps.TestUser;
import com.freshworks.hagrid.data.unit.fb.steps.FbComment;
import com.freshworks.hagrid.shared.MockFacadeSyncServiceContainer;
import com.freshworks.hagrid.traverser.MockFacadeDagService;
import com.freshworks.hagrid.shared.SyncServiceContainer;
import com.freshworks.hagrid.shared.sync.ConnectorConfiguration;
import com.freshworks.hagrid.shared.sync.SyncService;
import com.freshworks.hagrid.traverser.ParentStep;
import com.freshworks.hagrid.traverser.TraverseConfigService;
import com.freshworks.hagrid.traverser.DagNode.NodeRateLimitObject;
import com.google.common.collect.ImmutableMap;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;

@SpringBootTest
@EnabledIfSystemProperty(named = "spring.profiles.active", matches = "unit")
public class TestSyncService {

    @Autowired
    MockFacadeSyncService mockFacadeSyncService;

    @Autowired
    MockFacadeSyncServiceContainer mockFacadeSyncServiceContainer;

    @Autowired
    MockFacadeDagService mockFacadeDagScannerService;

    


    @BeforeEach
    public void beforeEach() throws Exception {
        mockFacadeSyncService.configure().build();
        mockFacadeSyncServiceContainer.configure().build();
    }

    @Test
    public void testTraverserConfiguration() throws Exception{

        ConnectorConfiguration configuration = new ConnectorConfiguration();

        configuration.setTraverserThreadCount(100);
        NodeRateLimitObject stepRateLimitObject = new NodeRateLimitObject();
        stepRateLimitObject.setDurationInSeconds(1);
        stepRateLimitObject.setNumberOfApiCalls(100);
        configuration.setStepRateLimit(FbComment.class.getName(), stepRateLimitObject);


        SyncService syncService = mockFacadeSyncService.build();
        doCallRealMethod().when(syncService).configureWithStaticSteps(anyString(), any(), any(), any());

        SyncServiceContainer syncServiceContainer = syncService.configureWithStaticSteps("my_name_space", ParentStep.class, ImmutableMap.<String, String>builder().build(), configuration);
        TraverseConfigService traverseConfigService = syncServiceContainer.getBean(TraverseConfigService.class);
        stepRateLimitObject = traverseConfigService.getRateLimitForStep(FbComment.class.getName());
        assertThat(stepRateLimitObject.getDurationInSeconds(), Matchers.is(1));
        assertThat(stepRateLimitObject.getNumberOfApiCalls(), Matchers.is(100));
    }
}
