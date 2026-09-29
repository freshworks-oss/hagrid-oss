package com.freshworks;

import java.util.Map;
import java.util.UUID;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.freshworks.hagrid.assets.FbUser;
import com.freshworks.hagrid.shared.SyncServiceContainer;
import com.freshworks.hagrid.shared.consumer.ConsumerService;
import com.freshworks.hagrid.shared.infra.InfraDbCursor;
import com.freshworks.hagrid.shared.sync.ConnectorConfiguration;
import com.freshworks.hagrid.shared.sync.SyncService;
import com.freshworks.hagrid.shared.sync.SyncStatusService;
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

            ConnectorConfiguration connectorConfiguration = new ConnectorConfiguration();

            syncService.configureWithStaticSteps(namespace, com.freshworks.hagrid.traverser.ParentStep.class, map, connectorConfiguration);

            // Run DAG from parentstep.class .. You can run Hagrid DAG from any step.
            syncServiceContainer = syncService.startSync();


            SyncStatusService syncStatusService = syncServiceContainer.getBean(SyncStatusService.class);
            ConsumerService consumerService = syncServiceContainer.getBean(ConsumerService.class);

            // Now consume assets as they are being generated
            // Create a token which say how many and from which index do you want to consume
            InfraDbCursor<FbUser> dbCursor = consumerService.getAssetCursor(FbUser.class);

            // Wait main thread until sync is done ( either successfull or failed)
            syncStatusService.waitUntilSyncIsInProgress();
            System.out.println("Sync is done");

            // Another way to consume all assets after sync is done. 
            // Mindful here, this method returns all assets at once. 

            while(dbCursor.hasNext()){

                FbUser fbUser = dbCursor.getNext();

                System.out.println(fbUser.getUser_id());
            }
            

            // Once sync is done, then must shutdown to release all resouces
            syncService.shutdown();
        }

        catch (Exception e){
            e.printStackTrace();
        }
    }


    public void consumeFbComments(Map<String, Object> fbTags){


    }


}