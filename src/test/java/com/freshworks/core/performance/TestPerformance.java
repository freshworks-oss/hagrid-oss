package com.freshworks.core.performance;

import com.freshworks.core.processor.ProcessorConfigService;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.consumer.ConsumerService;
import com.freshworks.core.shared.infra.InfraService;
import com.freshworks.core.shared.sync.SyncService;
import com.freshworks.core.shared.sync.SyncStatusService;
import com.freshworks.core.shared.synchronizers.ServiceTree;
import com.freshworks.core.traverser.ParentStep;
import com.freshworks.core.traverser.TraverseConfigService;
import com.freshworks.core.traverser.net.http.HttpClientService;
import com.freshworks.freshindex.index.query.Expression;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.ClassPath;
import io.github.classgraph.ClassInfo;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.EnabledIf;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@AutoConfigureObservability
@EnabledIfSystemProperty(named = "spring.profiles.active", matches = ".*\\.performance\\..*")
public class TestPerformance {

    @Autowired
    SyncService syncService;
    @Autowired
    private ServiceTree serviceTree;


    @Test
    public void testTenMillionPayloadWhenChildNodeHasMoreDataThanParent() throws Exception {

LocalDateTime localDataTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm");
        String formattedDateTime = localDataTime.format(formatter);
        Random random = new Random();
        int number = random.nextInt();

        ImmutableMap<String, String> x = ImmutableMap.<String, String>builder()
                .put("numberOfUsersEachPage", "1")
                .put("numberOfUserPagination", "1")
                .put("waitBetweenUserPaginationInMs", "0")
                .put("numberOfPostsEachPage", "10")
                .put("numberOfPostPagination", "10")
                .put("waitBetweenPostPaginationInMs", "0")
                .put("numberOfCommentsEachPage", "100")
                .put("numberOfCommentPagination", "100")
                .put("waitBetweenCommentPaginationInMs", "0")
                .put("numberOfCommunitiesEachPage", "1")
                .put("numberOfCommunityPagination", "1")
                .put("waitBetweenCommunityPaginationInMs", "0").build();
        MDC.put("mdc_key", "mdc_value");
        SyncServiceContainer syncServiceContainer = syncService.initSyncServiceContainer("ten_million_performance_test" + "_" + formattedDateTime + "_" + number, ParentStep.class, x);
        syncService.startSync(syncServiceContainer);

        SyncStatusService syncStatusService = syncServiceContainer.getBean(SyncStatusService.class);
        syncStatusService.waitUntilSyncIsInProgress();
        syncService.shutdown();
        assertThat(syncStatusService.getSyncStatus(), Matchers.is(1));
        assertThat(syncStatusService.getTraverser_status(), Matchers.is(1));
        assertThat(syncStatusService.getProcessor_status(), Matchers.is(1));
        Thread.sleep(10000);
    }

    @Test
    public void testTenMillionPayloadWhenParentNodeHasMoreDataThanChildNode() throws Exception {

        LocalDateTime localDataTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm");
        String formattedDateTime = localDataTime.format(formatter);

        ImmutableMap<String, String> x = ImmutableMap.<String, String>builder()
                .put("numberOfUsersEachPage", "100")
                .put("numberOfUserPagination", "100")
                .put("waitBetweenUserPaginationInMs", "0")
                .put("numberOfPostsEachPage", "10")
                .put("numberOfPostPagination", "10")
                .put("waitBetweenPostPaginationInMs", "0")
                .put("numberOfCommentsEachPage", "10")
                .put("numberOfCommentPagination", "1")
                .put("waitBetweenCommentPaginationInMs", "0")
                .put("numberOfCommunitiesEachPage", "1")
                .put("numberOfCommunityPagination", "1")
                .put("waitBetweenCommunityPaginationInMs", "0").build();

        SyncServiceContainer syncServiceContainer = syncService.initSyncServiceContainer("ten_million_performance_test" + "_" + formattedDateTime,  ParentStep.class, x);
        syncService.startSync(syncServiceContainer);

        SyncStatusService syncStatusService = syncServiceContainer.getBean(SyncStatusService.class);
        syncStatusService.waitUntilSyncIsInProgress();
        syncService.shutdown();
        assertThat(syncStatusService.getSyncStatus(), Matchers.is(1));
        assertThat(syncStatusService.getTraverser_status(), Matchers.is(1));
        assertThat(syncStatusService.getProcessor_status(), Matchers.is(1));

        Thread.sleep(10000);
    }
}
