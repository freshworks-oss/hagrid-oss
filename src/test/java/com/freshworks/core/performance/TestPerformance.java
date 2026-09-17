package com.freshworks.core.performance;

import static org.hamcrest.MatcherAssert.assertThat;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import com.esotericsoftware.kryo.kryo5.util.ObjectMap;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshworks.core.data.performance.fb.assets.FbComment;
import com.freshworks.core.data.performance.fb.assets.FbUser;
import com.freshworks.core.data.performance.fb.assets.non_primitive_assets.FbUserComment;
import com.freshworks.core.data.performance.fb.steps.FbUserServer;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.consumer.ConsumerService;
import com.freshworks.core.shared.infra.InfraDbCursor;
import com.freshworks.core.shared.sync.ConnectorConfiguration;
import com.freshworks.core.shared.sync.SyncService;
import com.freshworks.core.shared.sync.SyncStatusService;
import com.freshworks.core.shared.synchronizers.ServiceTree;
import com.freshworks.core.traverser.ParentStep;
import com.google.common.collect.ImmutableMap;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@AutoConfigureObservability
@EnabledIfSystemProperty(named = "spring.profiles.active", matches = "performance")
public class TestPerformance {

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    SyncService syncService;


    @Autowired
    private ServiceTree serviceTree;


    @Test
    public void testTenMillionPayloadWhenChildNodeHasMoreDataThanParent() throws Exception {

        ConnectorConfiguration connectorConfiguration = new ConnectorConfiguration();

        LocalDateTime localDataTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH_mm");
        String formattedDateTime = localDataTime.format(formatter);
        Random random = new Random();
        int number = random.nextInt();

        ImmutableMap<String, String> x = ImmutableMap.<String, String>builder()
                .put("numberOfUsersEachPage", "1")
                .put("numberOfUserPagination", "1")
                .put("waitBetweenUserPaginationInMs", "0")
                .put("numberOfPostsEachPage", "5")
                .put("numberOfPostPagination", "5")
                .put("waitBetweenPostPaginationInMs", "0")
                .put("numberOfCommentsEachPage", "10")
                .put("numberOfCommentPagination", "10")
                .put("waitBetweenCommentPaginationInMs", "0")
                .put("numberOfCommunitiesEachPage", "10")
                .put("numberOfCommunityPagination", "10")
                .put("waitBetweenCommunityPaginationInMs", "0").build();

        MDC.put("mdc_key", "mdc_value");
        SyncServiceContainer syncServiceContainer = syncService.configureSync("ten_million_performance_test" + "_" + formattedDateTime + "_" + number, ParentStep.class, x, connectorConfiguration);
        syncService.startSync();

        SyncStatusService syncStatusService = syncServiceContainer.getBean(SyncStatusService.class);
        syncStatusService.waitUntilSyncIsInProgress();
        ConsumerService consumerService = syncServiceContainer.getBean(ConsumerService.class);
        
        InfraDbCursor<FbUserComment> infraDbCursor = consumerService.getAssetCursor(FbUserComment.class);
        List<FbUserComment> fbUserCommentList = new ArrayList<>();

        while(infraDbCursor.hasNext()){
            fbUserCommentList.add(infraDbCursor.getNext());
        }


        InfraDbCursor<FbUser> infraDbCursorFbUser = consumerService.getAssetCursor(FbUser.class);
        List<FbUser> fbUserList = new ArrayList<>();

        while(infraDbCursorFbUser.hasNext()){
            fbUserList.add(infraDbCursorFbUser.getNext());
        }


        InfraDbCursor<FbComment> infraDbCursorFbComment = consumerService.getAssetCursor(FbComment.class);
        List<FbComment> fbCommentList = new ArrayList<>();

        while(infraDbCursorFbComment.hasNext()){
            fbCommentList.add(infraDbCursorFbComment.getNext());
        }

        syncService.shutdown();
        assertThat(syncStatusService.getSyncStatus(), Matchers.is(1));
        assertThat(syncStatusService.getTraverser_status(), Matchers.is(1));
        assertThat(syncStatusService.getProcessor_status(), Matchers.is(1));
        assertThat(fbUserCommentList.size(), Matchers.is(fbCommentList.size()));
        Thread.sleep(10000);
    }

    @Test
    public void testTenMillionPayloadWhenParentNodeHasMoreDataThanChildNode() throws Exception {

        ConnectorConfiguration connectorConfiguration = new ConnectorConfiguration();

        LocalDateTime localDataTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH_mm");
        String formattedDateTime = localDataTime.format(formatter);

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

        SyncServiceContainer syncServiceContainer = syncService.configureSync("ten_million_performance_test" + "_" + formattedDateTime,  ParentStep.class, x, connectorConfiguration);
        syncService.startSync();

        SyncStatusService syncStatusService = syncServiceContainer.getBean(SyncStatusService.class);
        syncStatusService.waitUntilSyncIsInProgress();
        syncService.shutdown();
        assertThat(syncStatusService.getSyncStatus(), Matchers.is(1));
        assertThat(syncStatusService.getTraverser_status(), Matchers.is(1));
        assertThat(syncStatusService.getProcessor_status(), Matchers.is(1));
        Thread.sleep(10000);
    }


    @Test
    public void testWhenThousandAPIRequestComes() throws Exception {
        
        ConnectorConfiguration connectorConfiguration = new ConnectorConfiguration();

        CompletableFuture<Void> futureTasks[] = new CompletableFuture[10];
        ObjectMapper objectMapper = new ObjectMapper();
        
        for(int i=0; i<10; i++){

            CompletableFuture<Void> task = CompletableFuture.runAsync(() ->{

                try{

                    SyncService syncService = applicationContext.getBean(SyncService.class);
                    ImmutableMap<String, String> x = ImmutableMap.<String, String>builder()
                        .build();

                    SyncServiceContainer syncServiceContainer = syncService.configureSync(UUID.randomUUID().toString(),  FbUserServer.class, x, connectorConfiguration);
                    syncService.startSync();

                    SyncStatusService syncStatusService = syncServiceContainer.getBean(SyncStatusService.class);
                    syncStatusService.waitUntilSyncIsInProgress();
                    ConsumerService consumerService = syncServiceContainer.getBean(ConsumerService.class);

                    InfraDbCursor<FbUser> infraDbCursor = consumerService.getAssetCursor(FbUser.class);
                    List<FbUser> fbUserList = new ArrayList();

                    while(infraDbCursor.hasNext()){
                        fbUserList.add(infraDbCursor.getNext());
                    }
                    
                    System.out.println(objectMapper.writeValueAsString(fbUserList));
                    assertThat(fbUserList.size(), Matchers.is(100));
                    // syncService.shutdown();
                }
                catch(Exception e){
                    e.printStackTrace();
                }

            });

            futureTasks[i] = task;
        }

        CompletableFuture<Void> allCompleted = CompletableFuture.allOf(futureTasks);
        allCompleted.join();
        System.out.println("All Completed");

    }
}
