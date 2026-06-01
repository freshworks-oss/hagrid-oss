package com.freshworks.core.integration.sync.test;

import com.freshworks.core.data.four_five_zero.unit.processor.joins.assets.FbUserUsageAsset;
import com.freshworks.core.data.four_zero_zero.integration.fb.assets.FbUser;
import com.freshworks.core.data.four_zero_zero.integration.recursive.contextual.assets.PublishedAsset;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.consumer.ConsumerService;
import com.freshworks.core.shared.sync.SyncService;
import com.freshworks.core.shared.sync.SyncStatusService;
import com.freshworks.core.traverser.ParentStep;
import com.google.common.collect.ImmutableMap;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;

@SpringBootTest
@EnabledIfSystemProperty(named = "spring.profiles.active", matches = ".*\\.integration\\..*")

public class TestSyncService {

    static String infraType;

    @Autowired
    SyncService syncService;

    @BeforeAll
    public static void beforeAll() throws IOException {

        infraType = System.getProperty("spring.profiles.active").split("\\.")[2];

        if(infraType.equalsIgnoreCase("h2")){
            Files.deleteIfExists(Paths.get("/Users/aaggarwal/Documents/hagrid-releases/data/hagrid-3.7.0/database.mv.db"));
        }
    }

    @Test
    public void testWhenNoExplicitShutdownAndSyncIsInitAndStartThenSyncIsSuccessful() throws Exception {

        ImmutableMap<String, String> x = ImmutableMap.<String, String>builder()
                .put("numberOfUsersEachPage", "1")
                .put("numberOfUserPagination", "1")
                .put("waitBetweenUserPaginationInMs", "0")
                .put("numberOfPostsEachPage", "1")
                .put("numberOfPostPagination", "1")
                .put("waitBetweenPostPaginationInMs", "0")
                .put("numberOfCommentsEachPage", "10")
                .put("numberOfCommentPagination", "1")
                .put("waitBetweenCommentPaginationInMs", "0")
                .put("numberOfCommunitiesEachPage", "1")
                .put("numberOfCommunityPagination", "1")
                .put("waitBetweenCommunityPaginationInMs", "0").build();

        SyncServiceContainer syncServiceContainer = syncService.initSyncServiceContainer(UUID.randomUUID().toString(), ParentStep.class, x);
        SyncStatusService syncStatusService = syncServiceContainer.getBean(SyncStatusService.class);
        ConsumerService consumerService = syncServiceContainer.getBean(ConsumerService.class);
        syncService.startSync(syncServiceContainer);
        syncStatusService.waitUntilSyncIsInProgress();
        assertThat(syncStatusService.getSyncStatus(), Matchers.is(1));
        List<FbUser> fbUserList = consumerService.getAssetByAssetType(FbUser.class);
        assertThat(fbUserList.size(), Matchers.is(1));
        
        List<FbUserUsageAsset> fbUserUsageAssetList = consumerService.getAssetByAssetType(FbUserUsageAsset.class);
        assertThat(fbUserUsageAssetList.size(), Matchers.is(fbUserList.size()));

        assertThat(syncStatusService.getSyncStatus() , Matchers.is(1));
        assertThat(syncStatusService.getTraverser_status() , Matchers.is(1));
        assertThat(syncStatusService.getProcessor_status() , Matchers.is(1));
    }

    @Test
    public void testWhenNoExplicitShutdownAndSyncIsStartedDirectlyThenSyncIsSuccessful() throws Exception {

        ImmutableMap<String, String> x = ImmutableMap.<String, String>builder()
                .put("numberOfUsersEachPage", "1")
                .put("numberOfUserPagination", "1")
                .put("waitBetweenUserPaginationInMs", "0")
                .put("numberOfPostsEachPage", "1")
                .put("numberOfPostPagination", "1")
                .put("waitBetweenPostPaginationInMs", "0")
                .put("numberOfCommentsEachPage", "10")
                .put("numberOfCommentPagination", "1")
                .put("waitBetweenCommentPaginationInMs", "0")
                .put("numberOfCommunitiesEachPage", "1")
                .put("numberOfCommunityPagination", "1")
                .put("waitBetweenCommunityPaginationInMs", "0").build();

        SyncServiceContainer syncServiceContainer = syncService.startSync(ParentStep.class, UUID.randomUUID().toString(), x);
        SyncStatusService syncStatusService = syncServiceContainer.getBean(SyncStatusService.class);
        ConsumerService consumerService = syncServiceContainer.getBean(ConsumerService.class);
        syncStatusService.waitUntilSyncIsInProgress();

        List<FbUser> fbUserList = consumerService.getAssetByAssetType(FbUser.class);

        assertThat(fbUserList.size(), Matchers.is(1));
        assertThat(syncStatusService.getSyncStatus() , Matchers.is(1));
        assertThat(syncStatusService.getTraverser_status() , Matchers.is(1));
        assertThat(syncStatusService.getProcessor_status() , Matchers.is(1));
    }

    @Test
    public void testWhenSyncIsSuccessfulThenExplicitShutdownHasNoAffect() throws Exception {

        ImmutableMap<String, String> x = ImmutableMap.<String, String>builder()
                .put("numberOfUsersEachPage", "1")
                .put("numberOfUserPagination", "1")
                .put("waitBetweenUserPaginationInMs", "0")
                .put("numberOfPostsEachPage", "1")
                .put("numberOfPostPagination", "1")
                .put("waitBetweenPostPaginationInMs", "0")
                .put("numberOfCommentsEachPage", "10")
                .put("numberOfCommentPagination", "1")
                .put("waitBetweenCommentPaginationInMs", "0")
                .put("numberOfCommunitiesEachPage", "1")
                .put("numberOfCommunityPagination", "1")
                .put("waitBetweenCommunityPaginationInMs", "0").build();

        SyncServiceContainer syncServiceContainer = syncService.startSync(ParentStep.class, UUID.randomUUID().toString(), x);
        SyncStatusService syncStatusService = syncServiceContainer.getBean(SyncStatusService.class);
        syncStatusService.waitUntilSyncIsInProgress();
        assertThat(syncStatusService.getSyncStatus() , Matchers.is(1));
        assertThat(syncStatusService.getTraverser_status() , Matchers.is(1));
        assertThat(syncStatusService.getProcessor_status() , Matchers.is(1));
        syncService.shutdown();
        assertThat(syncStatusService.getSyncStatus() , Matchers.is(1));
        assertThat(syncStatusService.getTraverser_status() , Matchers.is(1));
        assertThat(syncStatusService.getProcessor_status() , Matchers.is(1));

    }


    @Test
    public void testExplicitShutdownWhenSyncIsRunning() throws Exception {

        ImmutableMap<String, String> x = ImmutableMap.<String, String>builder()
                .put("numberOfUsersEachPage", "100")
                .put("numberOfUserPagination", "1000")
                .put("waitBetweenUserPaginationInMs", "0")
                .put("numberOfPostsEachPage", "1")
                .put("numberOfPostPagination", "1")
                .put("waitBetweenPostPaginationInMs", "0")
                .put("numberOfCommentsEachPage", "1")
                .put("numberOfCommentPagination", "10")
                .put("waitBetweenCommentPaginationInMs", "0")
                .put("numberOfCommunitiesEachPage", "1")
                .put("numberOfCommunityPagination", "1")
                .put("waitBetweenCommunityPaginationInMs", "0").build();

        SyncServiceContainer syncServiceContainer = syncService.startSync(ParentStep.class, UUID.randomUUID().toString(), x);
        SyncStatusService syncStatusService = syncServiceContainer.getBean(SyncStatusService.class);

        TimeUnit.SECONDS.sleep(2);

        syncService.shutdown();
        assertThat(syncStatusService.getSyncStatus() , Matchers.is(-1));
        assertThat(syncStatusService.getTraverser_status() , Matchers.is(-1));
        assertThat(syncStatusService.getProcessor_status() , Matchers.anyOf(Matchers.is(-1), Matchers.is(1)));
    }


    @Test
    public void testSyncServiceWhenSyncIsConfiguredAndThenStart() throws Exception {

        ImmutableMap<String, String> x = ImmutableMap.<String, String>builder()
                .put("numberOfUsersEachPage", "1")
                .put("numberOfUserPagination", "1")
                .put("waitBetweenUserPaginationInMs", "0")
                .put("numberOfPostsEachPage", "10")
                .put("numberOfPostPagination", "1")
                .put("waitBetweenPostPaginationInMs", "0")
                .put("numberOfCommentsEachPage", "1")
                .put("numberOfCommentPagination", "10")
                .put("waitBetweenCommentPaginationInMs", "0")
                .put("numberOfCommunitiesEachPage", "1")
                .put("numberOfCommunityPagination", "1")
                .put("waitBetweenCommunityPaginationInMs", "0").build();

        SyncServiceContainer syncServiceContainer = syncService.initSyncServiceContainer( UUID.randomUUID().toString(), ParentStep.class, x);
        syncService.startSync(syncServiceContainer);
        SyncStatusService syncStatusService = syncServiceContainer.getBean(SyncStatusService.class);
        syncStatusService.waitUntilSyncIsInProgress();

        assertThat(syncStatusService.getSyncStatus() , Matchers.is(1));
        assertThat(syncStatusService.getTraverser_status() , Matchers.is(1));
        assertThat(syncStatusService.getProcessor_status() , Matchers.is(1));

    }

    @Test
    public void testSyncServiceWhenDagIsCyclic() throws Exception {

        ImmutableMap<String, String> x = ImmutableMap.<String, String>builder()
                .build();

        SyncServiceContainer syncServiceContainer = syncService.initSyncServiceContainer( UUID.randomUUID().toString(), ParentStep.class, x);
        syncService.startSync(syncServiceContainer);
        SyncStatusService syncStatusService = syncServiceContainer.getBean(SyncStatusService.class);
        syncStatusService.waitUntilSyncIsInProgress();

        ConsumerService consumerService = syncServiceContainer.getBean(ConsumerService.class);
        List<PublishedAsset> list = consumerService.getAssetByAssetType(PublishedAsset.class);
        for (PublishedAsset asset : list) {
            System.out.println(asset.getToken());
            System.out.println(asset.getContext());
        }
        assertThat(syncStatusService.getSyncStatus() , Matchers.is(1));
        assertThat(syncStatusService.getTraverser_status() , Matchers.is(1));
        assertThat(syncStatusService.getProcessor_status() , Matchers.is(1));

    }
}
