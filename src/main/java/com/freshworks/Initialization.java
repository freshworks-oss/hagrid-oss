package com.freshworks;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.freshworks.hagrid.processor.AbstractAsset;
import com.freshworks.hagrid.shared.SyncServiceContainer;
import com.freshworks.hagrid.shared.consumer.ConsumerService;
import com.freshworks.hagrid.shared.sync.ConnectorConfiguration;
import com.freshworks.hagrid.shared.sync.SyncService;
import com.freshworks.hagrid.shared.sync.SyncStatusService;
import com.freshworks.hagrid.traverser.ParentStep;
import com.freshworks.hagrid.assets.FbCommentAsset;
import com.freshworks.hagrid.assets.FbUserAsset;
import com.google.common.collect.ImmutableMap;

@Component
public class Initialization {

    ApplicationContext applicationContext;

    public Initialization(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    // 4.9.0 does not wire MongoDbConfiguration itself; creating it as a bean applies its
    // @Value(spring.connector.infra.mongo.database.*) fields from application.properties
    @Bean
    @Scope("prototype")
    ConnectorConfiguration.MongoDbConfiguration mongoDbConfiguration(){
        return new ConnectorConfiguration.MongoDbConfiguration();
    }

    public void run(){

        try{
            // namespace must be unique for every run of hagrid sync
            String namespace = UUID.randomUUID().toString();

            // Take the main service SyncService ( prototype bean ) every time you want to run Hagrid DAG 
            SyncService syncService = this.applicationContext.getBean(SyncService.class);
            ImmutableMap<String, String> map = ImmutableMap.<String, String>builder().put("namespace", namespace)
                // small sync: 1 page of 10 per parent for each step
                .put("numberOfUsersEachPage", "10").put("numberOfUserPagination", "1").put("waitBetweenUserPaginationInMs", "0")
                .put("numberOfCommunitiesEachPage", "10").put("numberOfCommunityPagination", "1").put("waitBetweenCommunityPaginationInMs", "0")
                .put("numberOfPostsEachPage", "10").put("numberOfPostPagination", "1").put("waitBetweenPostPaginationInMs", "0")
                .put("numberOfCommentsEachPage", "10").put("numberOfCommentPagination", "1").put("waitBetweenCommentPaginationInMs", "0")
                .build();

            // Sync container is like spring container, but it will contain all services which Hagrid using to run this sync
            // You can use syncContainer to fetch or modify the behaviour of the hagrid
            // There are many services like `consumerService`, `traverserConfigService`, `processorService` 

            final SyncServiceContainer syncServiceContainer;

            // Run DAG from parentstep.class .. You can run Hagrid DAG from any step.
            // prototype bean, so infra type and Mongo settings come from application.properties (spring.connector.infra.*)
            ConnectorConfiguration connectorConfiguration = this.applicationContext.getBean(ConnectorConfiguration.class);
            connectorConfiguration.setMongoDbConfiguration(this.applicationContext.getBean(ConnectorConfiguration.MongoDbConfiguration.class));
            syncServiceContainer = syncService.configureWithStaticSteps(namespace, ParentStep.class, map , connectorConfiguration);
            syncService.startSync();

            SyncStatusService syncStatusService = syncServiceContainer.getBean(SyncStatusService.class);
            ConsumerService consumerService = syncServiceContainer.getBean(ConsumerService.class);

            // Now consume assets as they are being generated            
            consumerService.streamAsset(FbCommentAsset.class, asset ->{
                FbCommentAsset commentAsset = (FbCommentAsset)asset;
                System.out.println("Comment id of the comment generated it " + commentAsset.getComment_id());
            });

            // Wait main thread until sync is done ( either successfull or failed)
            syncStatusService.waitUntilSyncIsInProgress();
            System.out.println("Sync is done");

            // Another way to consume all assets after sync is done. 
            // Mindful here, this method returns all assets at once. 


            // List<FbUser> listofFbUserAsset = consumerService.getAssetByAssetType(FbUser.class);

            // for( FbUser fbAsset : listofFbUserAsset){
            //         System.out.println("fetched asset is " + fbAsset);
            // }
            

            // Once sync is done, then must shutdown to release all resouces
            syncService.shutdown();
        }

        catch (Exception e){
            e.printStackTrace();
        }
    }

}