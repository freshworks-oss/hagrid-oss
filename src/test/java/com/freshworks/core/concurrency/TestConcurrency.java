package com.freshworks.core.concurrency;

import com.freshworks.core.data.four_five_zero.concurrency.fb.assets.FbComment;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.consumer.ConsumerService;
import com.freshworks.core.shared.infra.InfraService;
import com.freshworks.core.shared.sync.SyncService;
import com.freshworks.core.shared.sync.SyncStatusService;
import com.freshworks.core.traverser.DagNode;
import com.freshworks.core.traverser.ParentStep;
import com.google.common.collect.ImmutableMap;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@AutoConfigureObservability
@EnabledIfSystemProperty(named = "spring.profiles.active", matches = ".*\\.concurrency\\..*")
public class TestConcurrency {

    @Autowired
    ApplicationContext applicationContext;

    private SyncServiceContainer longSyncRun(String namespace, boolean shouldFail) throws Exception {
        ImmutableMap<String, String> x;
        x = ImmutableMap.<String, String>builder()
                .put("numberOfUsersEachPage", "100")
                .put("shouldFail", String.valueOf(shouldFail))
                .put("numberOfUserPagination", "100")
                .put("waitBetweenUserPaginationInMs", "0")
                .put("numberOfPostsEachPage", "100")
                .put("numberOfPostPagination", "1000")
                .put("waitBetweenPostPaginationInMs", "0")
                .put("numberOfCommentsEachPage", "1000")
                .put("numberOfCommentPagination", "100")
                .put("waitBetweenCommentPaginationInMs", "0")
                .put("numberOfCommunitiesEachPage", "1")
                .put("numberOfCommunityPagination", "2")
                .put("waitBetweenCommunityPaginationInMs", "0").build();


        SyncService syncService = applicationContext.getBean(SyncService.class);
        SyncServiceContainer syncServiceContainer = syncService.initSyncServiceContainer(namespace, ParentStep.class, x);
        syncService.startSync(syncServiceContainer);

        return syncServiceContainer;
    }


    private SyncServiceContainer lightSyncRun(String namespace, Boolean shouldFail) throws Exception {

        ImmutableMap<String, String> x = ImmutableMap.<String, String>builder()
                .put("numberOfUsersEachPage", "1")
                .put("shouldFail", String.valueOf(shouldFail))
                .put("numberOfUserPagination", "1")
                .put("waitBetweenUserPaginationInMs", "0")
                .put("numberOfPostsEachPage", "1")
                .put("numberOfPostPagination", "10")
                .put("waitBetweenPostPaginationInMs", "0")
                .put("numberOfCommentsEachPage", "10")
                .put("numberOfCommentPagination", "100")
                .put("waitBetweenCommentPaginationInMs", "0")
                .put("numberOfCommunitiesEachPage", "1")
                .put("numberOfCommunityPagination", "1")
                .put("waitBetweenCommunityPaginationInMs", "0").build();
        SyncService syncService = applicationContext.getBean(SyncService.class);
        SyncServiceContainer syncServiceContainer = syncService.initSyncServiceContainer(namespace, ParentStep.class, x);
        syncService.startSync(syncServiceContainer);
        return syncServiceContainer;
    }


    @RepeatedTest(value = 10, failureThreshold = 1)
    public void testTwoSyncsRunningInParallelWhenBothOfThemCompletedWithSuccessfulStatus() throws Exception {

        CompletableFuture<SyncServiceContainer> sync1 = CompletableFuture.supplyAsync(() ->{
            try{

                String namespace =  "sync1_" + UUID.randomUUID().toString();
                return lightSyncRun(namespace, false);
            }
            catch (Exception e){
                return null;
            }
        });

        CompletableFuture<SyncServiceContainer> sync2 = CompletableFuture.supplyAsync(() ->{
            try{
                String namespace = "sync2_" + UUID.randomUUID().toString();
                return lightSyncRun(namespace, false);
            }
            catch (Exception e){
                return null;
            }
        });


        SyncServiceContainer s1  = sync1.get();
        SyncServiceContainer s2  = sync2.get();

        SyncStatusService syncStatusService1 = s1.getBean(SyncStatusService.class);
        syncStatusService1.waitUntilSyncIsInProgress();

        SyncStatusService syncStatusService2 = s2.getBean(SyncStatusService.class);
        syncStatusService2.waitUntilSyncIsInProgress();

        assertThat(syncStatusService1.getSyncStatus() , Matchers.is(1));
        assertThat(syncStatusService2.getSyncStatus() , Matchers.is(1));


        SyncService syncService1 = s1.getBean(SyncService.class);
        SyncService syncService2 = s2.getBean(SyncService.class);

        ConsumerService consumerService1 = s1.getBean(ConsumerService.class);
        ConsumerService consumerService2 = s2.getBean(ConsumerService.class);

        assertThat(consumerService1.getAssetByAssetType(FbComment.class).size(), Matchers.is(10000));
        assertThat(consumerService2.getAssetByAssetType(FbComment.class).size(), Matchers.is(10000));


        InfraService infraService1 = s1.getBean(InfraService.class);
        InfraService infraService2 = s2.getBean(InfraService.class);

        assertThat(infraService1.getProcessorQueue().size(), Matchers.is(10012L));
        assertThat(infraService2.getProcessorQueue().size(), Matchers.is(10012L));

        DagNode rootNode = s1.getBean(DagNode.class);
        List<DagNode> dagNodeList = rootNode.preOrder();
        for(DagNode dagNode : dagNodeList){
            assertThat(dagNode.getNodeOverallTraverserStatus(), Matchers.is(Matchers.not(0)));
            assertThat(dagNode.getTotalFailedItems(), Matchers.is(0L));
            assertThat(dagNode.getTotalItemsSynced(), Matchers.is(dagNode.getTotalSuccessfulItems()));
        }


        rootNode = s2.getBean(DagNode.class);
        dagNodeList = rootNode.preOrder();
        for(DagNode dagNode : dagNodeList){
            assertThat(dagNode.getNodeOverallTraverserStatus(), Matchers.is(Matchers.not(0)));
            assertThat(dagNode.getTotalFailedItems(), Matchers.is(0L));
            assertThat(dagNode.getTotalItemsSynced(), Matchers.is(dagNode.getTotalSuccessfulItems()));
        }
    }


    @RepeatedTest(value = 10, failureThreshold = 1)
    public void testWhenTwoSyncAreRunningAndBothOfThemCompletedWithFailedDueToIssuesWithStepsStatus() throws Exception {

        CompletableFuture<SyncServiceContainer> sync1 = CompletableFuture.supplyAsync(() ->{
            try{
                String namespace = "sync1_" + UUID.randomUUID().toString();
                return lightSyncRun(namespace, true);
            }
            catch (Exception e){
                return null;
            }
        });

        CompletableFuture<SyncServiceContainer> sync2 = CompletableFuture.supplyAsync(() ->{
            try{
                String namespace = "sync2_" + UUID.randomUUID().toString();
                return lightSyncRun(namespace, true);
            }
            catch (Exception e){
                return null;
            }
        });


        SyncServiceContainer s1  = sync1.get();
        SyncServiceContainer s2  = sync2.get();

        SyncStatusService syncStatusService1 = s1.getBean(SyncStatusService.class);
        SyncStatusService syncStatusService2 = s2.getBean(SyncStatusService.class);

        syncStatusService1.waitUntilSyncIsInProgress();
        syncStatusService2.waitUntilSyncIsInProgress();

        assertThat(syncStatusService1.getSyncStatus() , Matchers.is(-1));
        assertThat(syncStatusService2.getSyncStatus() , Matchers.is(-1));


        SyncService syncService1 = s1.getBean(SyncService.class);
        SyncService syncService2 = s2.getBean(SyncService.class);

        ConsumerService consumerService1 = s1.getBean(ConsumerService.class);
        ConsumerService consumerService2 = s2.getBean(ConsumerService.class);

        assertThat(consumerService1.getAssetByAssetType(FbComment.class).size(), Matchers.lessThan(10000));
        assertThat(consumerService2.getAssetByAssetType(FbComment.class).size(), Matchers.lessThan(10000));


        InfraService infraService1 = s1.getBean(InfraService.class);
        InfraService infraService2 = s2.getBean(InfraService.class);

        assertThat(infraService1.getProcessorQueue().size(), Matchers.lessThan(10012L));
        assertThat(infraService2.getProcessorQueue().size(), Matchers.lessThan(10012L));

        DagNode rootNode = s1.getBean(DagNode.class);
        List<DagNode> dagNodeList = rootNode.preOrder();
        for(DagNode dagNode : dagNodeList){
            assertThat(dagNode.getNodeOverallTraverserStatus(), Matchers.is(Matchers.not(0)));
            assertThat(dagNode.getTotalFailedItems(), Matchers.is(dagNode.getTotalItemsSynced()));
            assertThat(dagNode.getTotalSuccessfulItems(), Matchers.is(0L));
        }


        rootNode = s2.getBean(DagNode.class);
        dagNodeList = rootNode.preOrder();
        for(DagNode dagNode : dagNodeList){
            assertThat(dagNode.getNodeOverallTraverserStatus(), Matchers.is(Matchers.not(0)));
            assertThat(dagNode.getTotalFailedItems(), Matchers.is(dagNode.getTotalItemsSynced()));
            assertThat(dagNode.getTotalSuccessfulItems(), Matchers.is(0L));
        }
    }


    @RepeatedTest(value = 10, failureThreshold = 1)
    public void testTwoSyncsRunningInParallelWhenOneOfThemCompletedWithFailedStatusAndOtherCompletedWithSuccessfulStatus() throws Exception {


        CompletableFuture<SyncServiceContainer> sync1 = CompletableFuture.supplyAsync(() ->{
            try{
                String namespace = "sync1_" + UUID.randomUUID().toString();
                System.out.println("name space is success one " + namespace);
                return lightSyncRun(namespace, false);
            }
            catch (Exception e){
                return null;
            }
        });

        CompletableFuture<SyncServiceContainer> sync2 = CompletableFuture.supplyAsync(() ->{
            try{
                String namespace = "sync2_" + UUID.randomUUID().toString();
                System.out.println("name space is for failed one " + namespace);
                return lightSyncRun(namespace, true);
            }
            catch (Exception e){
                return null;
            }
        });


        SyncServiceContainer s1  = sync1.get();
        SyncServiceContainer s2  = sync2.get();

        SyncStatusService syncStatusService1 = s1.getBean(SyncStatusService.class);
        SyncStatusService syncStatusService2 = s2.getBean(SyncStatusService.class);

        syncStatusService1.waitUntilSyncIsInProgress();
        syncStatusService2.waitUntilSyncIsInProgress();

        assertThat(syncStatusService1.getSyncStatus() , Matchers.is(1));
        assertThat(syncStatusService2.getSyncStatus() , Matchers.is(-1));


        SyncService syncService1 = s1.getBean(SyncService.class);
        SyncService syncService2 = s2.getBean(SyncService.class);

        ConsumerService consumerService1 = s1.getBean(ConsumerService.class);
        ConsumerService consumerService2 = s2.getBean(ConsumerService.class);

        assertThat(consumerService1.getAssetByAssetType(FbComment.class).size(), Matchers.is(10000));
        assertThat(consumerService2.getAssetByAssetType(FbComment.class).size(), Matchers.lessThan(10000));


        InfraService infraService1 = s1.getBean(InfraService.class);
        InfraService infraService2 = s2.getBean(InfraService.class);

        assertThat(infraService1.getProcessorQueue().size(), Matchers.is(10012L));
        assertThat(infraService2.getProcessorQueue().size(), Matchers.lessThan(10012L));

        DagNode rootNode = s1.getBean(DagNode.class);
        List<DagNode> dagNodeList = rootNode.preOrder();
        for(DagNode dagNode : dagNodeList){
            assertThat(dagNode.getTotalFailedItems(), Matchers.is(0L));
            assertThat(dagNode.getTotalItemsSynced(), Matchers.is(dagNode.getTotalSuccessfulItems()));
        }


        rootNode = s2.getBean(DagNode.class);
        dagNodeList = rootNode.preOrder();
        for(DagNode dagNode : dagNodeList){
            assertThat(dagNode.getTotalSuccessfulItems(), Matchers.is(0L));
            assertThat(dagNode.getTotalFailedItems(), Matchers.is(dagNode.getTotalItemsSynced()));
        }
    }


    @RepeatedTest(value = 10, failureThreshold = 1)
    public void testTwoSyncsRunningInParallelWhenOneOfThemIsShutdownAbruptlyAndOtherCompletedWithSuccessfulStatus() throws Exception {

        CompletableFuture<SyncServiceContainer> sync1 = CompletableFuture.supplyAsync(() ->{
            try{
                String namespace = "sync1_" + UUID.randomUUID().toString();
                System.out.println("Name space is "  + namespace);
                return lightSyncRun(namespace, false);
            }
            catch (Exception e){
                return null;
            }
        });

        CompletableFuture<SyncServiceContainer> sync2 = CompletableFuture.supplyAsync(() ->{
            try{
                String namespace = "sync2_" + UUID.randomUUID().toString();
                System.out.println("Name space is "  + namespace);
                return longSyncRun(namespace, false);
            }
            catch (Exception e){
                return null;
            }
        });

        // First get the sync container of S2 immediately.
        SyncServiceContainer s2  = sync2.get();
        SyncStatusService syncStatusService2 = s2.getBean(SyncStatusService.class);
        // Extracting services before shutting down so that it can be validated.
        // Once shutdown then syncService Container got cleared.
        SyncService syncService2 = s2.getBean(SyncService.class);
        ConsumerService consumerService2 = s2.getBean(ConsumerService.class);
        InfraService infraService2 = s2.getBean(InfraService.class);
        DagNode rootNodes2 = s2.getBean(DagNode.class);

        TimeUnit.SECONDS.sleep(10);
        syncService2.shutdown();
        System.out.println("Getting sync2 syncStatys after shutdown");
        assertThat(syncStatusService2.getSyncStatus() , Matchers.is(-1));

        List<DagNode> dagNodeLists2 = rootNodes2.preOrder();
        for(DagNode dagNode : dagNodeLists2){
            assertThat( "node name is " + dagNode.getName(), dagNode.getNodeOverallTraverserStatus(), Matchers.is(Matchers.not(0)));
            assertThat(dagNode.getTotalFailedItems(), Matchers.greaterThanOrEqualTo(0L));
            assertThat(dagNode.getTotalSuccessfulItems(), Matchers.lessThanOrEqualTo(dagNode.getTotalItemsSynced()));
            assertThat(dagNode.getTotalItemsSynced(), Matchers.greaterThanOrEqualTo(dagNode.getTotalSuccessfulItems() + dagNode.getTotalFailedItems()));
        }

        SyncServiceContainer s1  = sync1.get();
        SyncStatusService syncStatusService1 = s1.getBean(SyncStatusService.class);
        syncStatusService1.waitUntilSyncIsInProgress();
        System.out.println("Getting sync1 syncStatys sync completion");
        assertThat(syncStatusService1.getSyncStatus() , Matchers.is(1));
        ConsumerService consumerService1 = s1.getBean(ConsumerService.class);
        assertThat(consumerService1.getAssetByAssetType(FbComment.class).size(), Matchers.is(10000));
        InfraService infraService1 = s1.getBean(InfraService.class);
        assertThat(infraService1.getProcessorQueue().size(), Matchers.is(10012L));

        DagNode rootNode = s1.getBean(DagNode.class);
        List<DagNode> dagNodeList = rootNode.preOrder();
        for(DagNode dagNode : dagNodeList){
            assertThat(dagNode.getTotalFailedItems(), Matchers.is(0L));
            assertThat(dagNode.getTotalSuccessfulItems(), Matchers.is(dagNode.getTotalItemsSynced()));
            assertThat(dagNode.getTotalItemsSynced(), Matchers.greaterThanOrEqualTo(dagNode.getTotalSuccessfulItems() + dagNode.getTotalFailedItems()));
        }

        SyncService syncService1 = s1.getBean(SyncService.class);
        syncService1.shutdown();
    }

    @Test
    public void testTwoSyncsRunningInParallelWhenBothOfThemShutdownAbruptly() throws Exception {

        CompletableFuture<SyncServiceContainer> sync1 = CompletableFuture.supplyAsync(() ->{
            try{
                String namespace = "sync1_" + UUID.randomUUID().toString();
                return longSyncRun(namespace, false);
            }
            catch (Exception e){
                return null;
            }
        });

        CompletableFuture<SyncServiceContainer> sync2 = CompletableFuture.supplyAsync(() ->{
            try{
                String namespace = "sync2_" +UUID.randomUUID().toString();
                return longSyncRun(namespace, false);
            }
            catch (Exception e){
                return null;
            }
        });

        // First get the sync container of S2 immediately.
        SyncServiceContainer s2  = sync2.get();
        SyncStatusService syncStatusService2 = s2.getBean(SyncStatusService.class);
        // Extracting services before shutting down so that it can be validated.
        // Once shutdown then syncService Container got cleared.
        SyncService syncService2 = s2.getBean(SyncService.class);
        ConsumerService consumerService2 = s2.getBean(ConsumerService.class);
        InfraService infraService2 = s2.getBean(InfraService.class);
        DagNode rootNodes2 = s2.getBean(DagNode.class);

        TimeUnit.SECONDS.sleep(5);
        syncService2.shutdown();
        assertThat(syncStatusService2.getSyncStatus() , Matchers.is(-1));
        assertThat(infraService2.getProcessorQueue().size(), Matchers.lessThan(10012L));

        List<DagNode> dagNodeLists2 = rootNodes2.preOrder();
        for(DagNode dagNode : dagNodeLists2){
            System.out.println("dag node name is " + dagNode.getName());
            assertThat(dagNode.getTotalFailedItems(), Matchers.greaterThanOrEqualTo(0L));
            assertThat(dagNode.getTotalSuccessfulItems(), Matchers.lessThanOrEqualTo(dagNode.getTotalItemsSynced()));
            assertThat(dagNode.getTotalItemsSynced(), Matchers.greaterThanOrEqualTo(dagNode.getTotalSuccessfulItems() + dagNode.getTotalFailedItems()));
        }


        SyncServiceContainer s1  = sync1.get();
        SyncStatusService syncStatusService1 = s1.getBean(SyncStatusService.class);
        ConsumerService consumerService1 = s1.getBean(ConsumerService.class);
        DagNode rootNode = s1.getBean(DagNode.class);
        SyncService syncService1 = s1.getBean(SyncService.class);
        InfraService infraService1 = s1.getBean(InfraService.class);


        TimeUnit.SECONDS.sleep(5);
        syncService1.shutdown();

        assertThat(syncStatusService1.getSyncStatus() , Matchers.is(-1));
        assertThat(infraService1.getProcessorQueue().size(), Matchers.lessThanOrEqualTo(10012L));

        List<DagNode> dagNodeList = rootNode.preOrder();
        for(DagNode dagNode : dagNodeList){
            assertThat(dagNode.getTotalFailedItems(), Matchers.greaterThanOrEqualTo(0L));
            assertThat(dagNode.getTotalSuccessfulItems(), Matchers.lessThanOrEqualTo(dagNode.getTotalItemsSynced()));
            assertThat(dagNode.getTotalItemsSynced(), Matchers.greaterThanOrEqualTo(dagNode.getTotalSuccessfulItems() + dagNode.getTotalFailedItems()));
        }
    }
}
