package com.freshworks.core.shared.sync;

import com.fasterxml.jackson.databind.JsonNode;
import com.freshworks.core.data.four_five_zero.unit.dag.steps.TestUser;
import com.freshworks.core.shared.MockFacadeSyncServiceContainer;
import com.freshworks.core.shared.SyncServiceContainer;
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
@EnabledIfSystemProperty(named = "spring.profiles.active", matches = ".*\\.unit\\..*")
public class TestSyncService {

    @Autowired
    MockFacadeSyncService mockFacadeSyncService;

    @Autowired
    MockFacadeSyncServiceContainer mockFacadeSyncServiceContainer;

    @BeforeEach
    public void beforeEach() throws Exception {
        mockFacadeSyncService.configure().build();
        mockFacadeSyncServiceContainer.configure().build();
    }

    @Test
    public void testInitSyncServiceContainerMethodReturnsSyncContainerWithConfiguredServices() throws Exception {

        SyncService syncService = mockFacadeSyncService.build();
        doCallRealMethod().when(syncService).initSyncServiceContainer(anyString(), any(), any());

        SyncServiceContainer syncServiceContainer = syncService.initSyncServiceContainer("my_name_space", ParentStep.class, ImmutableMap.<String, String>builder().build());
        TraverseConfigService traverseConfigService = syncServiceContainer.getBean(TraverseConfigService.class);
        JsonNode jsonNode = traverseConfigService.getConfigurationNode();
        assertThat(jsonNode.has("rateLimit"), Matchers.is(true));

    }

    @Test
    public void testWhenTraverseConfigIsModifiedThenItModifiedInSyncContainerAsWell() throws Exception{

        SyncService syncService = mockFacadeSyncService.build();
        doCallRealMethod().when(syncService).initSyncServiceContainer(anyString(), any(), any());

        SyncServiceContainer syncServiceContainer = syncService.initSyncServiceContainer("my_name_space", ParentStep.class, ImmutableMap.<String, String>builder().build());
        TraverseConfigService traverseConfigService = syncServiceContainer.getBean(TraverseConfigService.class);
        JsonNode jsonNode = traverseConfigService.getConfigurationNode();
        assertThat(jsonNode.has("rateLimit"), Matchers.is(true));

        traverseConfigService.setRateLimitForStep(TestUser.class, 200, 30);

        TraverseConfigService newTraverseConfigService = syncServiceContainer.getBean(TraverseConfigService.class);
        jsonNode = newTraverseConfigService.getConfigurationNode();

        assertThat(jsonNode.has("rateLimit"), Matchers.is(true));
        assertThat(jsonNode.get("rateLimit").has(TestUser.class.getName()), Matchers.is(true));
        assertThat(jsonNode.get("rateLimit").get(TestUser.class.getName()).get("api_count").asInt(), Matchers.is(200));
        assertThat(jsonNode.get("rateLimit").get(TestUser.class.getName()).get("seconds").asInt(), Matchers.is(30));

    }
}
