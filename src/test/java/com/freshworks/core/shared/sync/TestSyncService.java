package com.freshworks.core.shared.sync;

import com.fasterxml.jackson.databind.JsonNode;
import com.freshworks.core.data.unit.dag.steps.TestUser;
import com.freshworks.core.data.unit.fb.steps.FbComment;
import com.freshworks.core.shared.MockFacadeSyncServiceContainer;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.sync.ConnectorConfiguration.StepRateLimitObject;
import com.freshworks.core.traverser.MockFacadeDagScannerService;
import com.freshworks.core.traverser.ParentStep;
import com.freshworks.core.traverser.TraverseConfigService;
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
    MockFacadeDagScannerService mockFacadeDagScannerService;

    


    @BeforeEach
    public void beforeEach() throws Exception {
        mockFacadeSyncService.configure().build();
        mockFacadeSyncServiceContainer.configure().build();
    }

    @Test
    public void testTraverserConfiguration() throws Exception{

        ConnectorConfiguration configuration = new ConnectorConfiguration();

        configuration.setTraverserThreadCount(100);
        StepRateLimitObject stepRateLimitObject = new StepRateLimitObject();
        stepRateLimitObject.setDurationInSeconds(1);
        stepRateLimitObject.setNumberOfApiCalls(100);
        configuration.setStepRateLimit(FbComment.class, stepRateLimitObject);


        SyncService syncService = mockFacadeSyncService.build();
        doCallRealMethod().when(syncService).configureSync(anyString(), any(), any(), any());

        SyncServiceContainer syncServiceContainer = syncService.configureSync("my_name_space", ParentStep.class, ImmutableMap.<String, String>builder().build(), configuration);
        TraverseConfigService traverseConfigService = syncServiceContainer.getBean(TraverseConfigService.class);
        stepRateLimitObject = traverseConfigService.getRateLimitForStep(FbComment.class);
        assertThat(stepRateLimitObject.getDurationInSeconds(), Matchers.is(1));
        assertThat(stepRateLimitObject.getNumberOfApiCalls(), Matchers.is(100));
    }
}
