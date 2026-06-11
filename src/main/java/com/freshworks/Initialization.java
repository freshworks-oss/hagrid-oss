package com.freshworks;

import com.freshworks.core.shared.Namespace;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.analytics.AnalyticsFactory;
import com.freshworks.core.shared.analytics.AnalyticsService;
import com.freshworks.core.shared.consumer.ConsumerService;
import com.freshworks.core.shared.sync.SyncService;
import com.freshworks.core.shared.sync.SyncStatusService;
import com.freshworks.core.traverser.ParentStep;
import com.freshworks.freshindex.index.query.Expression;
import com.freshworks.hagrid.assets.FbComment;
import com.google.common.collect.ImmutableMap;

import org.apache.commons.lang3.function.Consumers;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class Initialization {

    ApplicationContext applicationContext;

    public Initialization(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public void run(){

        try{
            String namespace = UUID.randomUUID().toString();
            SyncService syncService = this.applicationContext.getBean(SyncService.class);
            ImmutableMap<String, String> map = ImmutableMap.<String, String>builder().put("namespace", namespace).build();
            final SyncServiceContainer syncServiceContainer;

            syncServiceContainer = syncService.startSync(ParentStep.class, namespace,  map);
            SyncStatusService syncStatusService = syncServiceContainer.getBean(SyncStatusService.class);
            syncStatusService.waitUntilSyncIsInProgress();
            ConsumerService consumerService = syncServiceContainer.getBean(ConsumerService.class);
            List<FbComment> listOfAllComments = consumerService.getAssetByAssetType(FbComment.class);

            // To check various example of consuming, please checkout this documentation - https://freshworks-oss.github.io/hagrid-oss/usecases/consumer/

            
            System.out.println("Sync is done");
        }

        catch (Exception e){
            e.printStackTrace();
        }
    }


    public void consumeFbComments(Map<String, Object> fbTags){


    }


}