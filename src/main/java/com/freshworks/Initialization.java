package com.freshworks;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.freshworks.core.processor.AbstractAsset;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.consumer.ConsumerService;
import com.freshworks.core.shared.infra.InfraDbCursor;
import com.freshworks.core.shared.infra.nitrite.NitriteDbCursor;
import com.freshworks.core.shared.sync.ConnectorConfiguration;
import com.freshworks.core.shared.sync.SyncService;
import com.freshworks.core.shared.sync.SyncStatusService;
import com.freshworks.core.traverser.ParentStep;
import com.freshworks.hagrid.assets.FbCommentAsset;
import com.freshworks.hagrid.assets.FbUserAsset;
import com.google.common.collect.ImmutableMap;

@Component
public class Initialization {

    ApplicationContext applicationContext;

    public Initialization(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public void run(){

        try{
            // namespace must be unique for every run of hagrid sync
            String namespace = UUID.randomUUID().toString();

            // Take the main service SyncService ( prototype bean ) every time you want to run Hagrid DAG 
            SyncService syncService = this.applicationContext.getBean(SyncService.class);
            ImmutableMap<String, String> map = ImmutableMap.<String, String>builder().put("namespace", namespace).build();

            // Sync container is like spring container, but it will contain all services which Hagrid using to run this sync
            // You can use syncContainer to fetch or modify the behaviour of the hagrid
            // There are many services like `consumerService`, `traverserConfigService`, `processorService` 

            final SyncServiceContainer syncServiceContainer;

            // Run DAG from parentstep.class .. You can run Hagrid DAG from any step.
            ConnectorConfiguration connectorConfiguration = new ConnectorConfiguration();
            syncServiceContainer = syncService.configureSync(namespace, ParentStep.class, map , connectorConfiguration);
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