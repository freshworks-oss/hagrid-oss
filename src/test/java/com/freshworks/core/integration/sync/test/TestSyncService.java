package com.freshworks.core.integration.sync.test;

import com.freshworks.core.data.integration.fb.assets.FbUser;
import com.freshworks.core.data.integration.fb.assets.complex_asset.FbUserComment;
import com.freshworks.core.data.integration.fb.assets.complex_asset.FbUserCommentUserJoinAsset;
import com.freshworks.core.data.integration.recursive.contextual.assets.PublishedAsset;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.consumer.ConsumerService;
import com.freshworks.core.shared.infra.InfraDbCursor;
import com.freshworks.core.shared.sync.ConnectorConfiguration;
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
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;

@SpringBootTest
@EnabledIfSystemProperty(named = "spring.profiles.active", matches = "integration")
public class TestSyncService {

    static String infraType;

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    SyncService syncService;

    @BeforeAll
    public static void beforeAll() throws IOException {

    }

    @Test
    public void testWhenNoExplicitShutdownAndSyncIsInitAndStartThenSyncIsSuccessful() throws Exception {

        ConnectorConfiguration connectorConfiguration = new ConnectorConfiguration();

        ImmutableMap<String, String> x = ImmutableMap.<String, String>builder()
                .put("numberOfUsersEachPage", "1")
                .put("numberOfUserPagination", "1")
                .put("waitBetweenUserPaginationInMs", "0")
                .put("numberOfPostsEachPage", "1")
                .put("numberOfPostPagination", "1")
                .put("waitBetweenPostPaginationInMs", "0")
                .put("numberOfCommentsEachPage", "1")
                .put("numberOfCommentPagination", "1")
                .put("waitBetweenCommentPaginationInMs", "0")
                .put("numberOfCommunitiesEachPage", "1")
                .put("numberOfCommunityPagination", "1")
                .put("waitBetweenCommunityPaginationInMs", "0").build();

        syncService = applicationContext.getBean(SyncService.class);
        SyncServiceContainer syncServiceContainer = syncService.configureSync(UUID.randomUUID().toString(), ParentStep.class, x, connectorConfiguration);
        SyncStatusService syncStatusService = syncServiceContainer.getBean(SyncStatusService.class);
        ConsumerService consumerService = syncServiceContainer.getBean(ConsumerService.class);
        syncService.startSync();
        syncStatusService.waitUntilSyncIsInProgress();
        assertThat(syncStatusService.getSyncStatus(), Matchers.is(1));

        InfraDbCursor<FbUser> infraDbCursor = consumerService.getAssetCursor(FbUser.class);
        List<FbUser> fbUserList = new ArrayList<>();
        
        while(infraDbCursor.hasNext()){
            fbUserList.add(infraDbCursor.getNext());
        }
        assertThat(fbUserList.size(), Matchers.is(1));
        

        InfraDbCursor<FbUserComment> infraDbCursor1 = consumerService.getAssetCursor(FbUserComment.class);

        List<FbUserComment> fbUserCommentAssetList = new ArrayList();

        while(infraDbCursor1.hasNext()){

            fbUserCommentAssetList.add(infraDbCursor1.getNext());
        }

        assertThat(fbUserCommentAssetList.size(), Matchers.is(1));



        InfraDbCursor<FbUserCommentUserJoinAsset> infraDbCursor2 = consumerService.getAssetCursor(FbUserCommentUserJoinAsset.class);
        List<FbUserCommentUserJoinAsset> fbUserCommentUserAssetList = new ArrayList();


        while(infraDbCursor2.hasNext()){
            fbUserCommentUserAssetList.add(infraDbCursor2.getNext());
        }

        assertThat(fbUserCommentUserAssetList.size(), Matchers.is(1));

        assertThat(syncStatusService.getSyncStatus() , Matchers.is(1));
        assertThat(syncStatusService.getTraverser_status() , Matchers.is(1));
        assertThat(syncStatusService.getProcessor_status() , Matchers.is(1));
    }

    @Test
    public void testWhenNoExplicitShutdownAndSyncIsStartedDirectlyThenSyncIsSuccessful() throws Exception {

        ConnectorConfiguration connectorConfiguration = new ConnectorConfiguration();

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

        SyncServiceContainer syncServiceContainer = syncService.configureSync(UUID.randomUUID().toString(), ParentStep.class, x, connectorConfiguration);
        syncService.startSync();
        SyncStatusService syncStatusService = syncServiceContainer.getBean(SyncStatusService.class);
        ConsumerService consumerService = syncServiceContainer.getBean(ConsumerService.class);
        syncStatusService.waitUntilSyncIsInProgress();

        InfraDbCursor<FbUser> infraDbCursor = consumerService.getAssetCursor(FbUser.class);
        List<FbUser> fbUserList = new ArrayList<>();

        while(infraDbCursor.hasNext()){
            fbUserList.add(infraDbCursor.getNext());
        }

        assertThat(fbUserList.size(), Matchers.is(1));
        assertThat(syncStatusService.getSyncStatus() , Matchers.is(1));
        assertThat(syncStatusService.getTraverser_status() , Matchers.is(1));
        assertThat(syncStatusService.getProcessor_status() , Matchers.is(1));
    }

    @Test
    public void testWhenSyncIsSuccessfulThenExplicitShutdownHasNoAffect() throws Exception {

        ConnectorConfiguration connectorConfiguration = new ConnectorConfiguration();
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

        SyncServiceContainer syncServiceContainer = syncService.configureSync(UUID.randomUUID().toString(), ParentStep.class, x, connectorConfiguration);
        syncService.startSync();
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

        ConnectorConfiguration connectorConfiguration = new ConnectorConfiguration();
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

        SyncServiceContainer syncServiceContainer = syncService.configureSync(UUID.randomUUID().toString(), ParentStep.class, x, connectorConfiguration);
        syncService.startSync();
        SyncStatusService syncStatusService = syncServiceContainer.getBean(SyncStatusService.class);

        TimeUnit.SECONDS.sleep(2);

        syncService.shutdown();
        assertThat(syncStatusService.getSyncStatus() , Matchers.is(-1));
        assertThat(syncStatusService.getTraverser_status() , Matchers.is(-1));
        assertThat(syncStatusService.getProcessor_status() , Matchers.anyOf(Matchers.is(-1), Matchers.is(1)));
    }


    @Test
    public void testSyncServiceWhenSyncIsConfiguredAndThenStart() throws Exception {

        ConnectorConfiguration connectorConfiguration = new ConnectorConfiguration();

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

        SyncServiceContainer syncServiceContainer = syncService.configureSync( UUID.randomUUID().toString(), ParentStep.class, x, connectorConfiguration);
        syncService.startSync();
        SyncStatusService syncStatusService = syncServiceContainer.getBean(SyncStatusService.class);
        syncStatusService.waitUntilSyncIsInProgress();

        assertThat(syncStatusService.getSyncStatus() , Matchers.is(1));
        assertThat(syncStatusService.getTraverser_status() , Matchers.is(1));
        assertThat(syncStatusService.getProcessor_status() , Matchers.is(1));

    }

    @Test
    public void testSyncServiceWhenDagIsCyclic() throws Exception {

        ConnectorConfiguration connectorConfiguration = new ConnectorConfiguration();

        
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

        SyncServiceContainer syncServiceContainer = syncService.configureSync( UUID.randomUUID().toString(), ParentStep.class, x, connectorConfiguration);
        syncService.startSync();
        SyncStatusService syncStatusService = syncServiceContainer.getBean(SyncStatusService.class);
        syncStatusService.waitUntilSyncIsInProgress();

        ConsumerService consumerService = syncServiceContainer.getBean(ConsumerService.class);
        InfraDbCursor<PublishedAsset> infraDbCursor = consumerService.getAssetCursor(PublishedAsset.class);
        List<PublishedAsset> list = new ArrayList();

        while(infraDbCursor.hasNext()){
            list.add(infraDbCursor.getNext());
        }
        
        for (PublishedAsset asset : list) {
            System.out.println(asset.getToken());
            System.out.println(asset.getContext());
        }
        assertThat(syncStatusService.getSyncStatus() , Matchers.is(1));
        assertThat(syncStatusService.getTraverser_status() , Matchers.is(1));
        assertThat(syncStatusService.getProcessor_status() , Matchers.is(1));

    }
}
