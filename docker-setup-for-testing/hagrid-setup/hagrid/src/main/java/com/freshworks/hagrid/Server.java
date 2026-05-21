package com.freshworks.hagrid;

import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.traverser.ParentStep;
import com.freshworks.core.shared.consumer.ConsumerService;
import com.freshworks.core.shared.sync.SyncService;
import com.freshworks.core.shared.sync.SyncStatusService;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class Server {

    ApplicationContext applicationContext;
    public Server(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }


    @GetMapping("/hello")
    public String hello(){
        return "Hello World";
    }

    @GetMapping("/run_facebook_usecase")
    public String runFacebookUsecase() throws Exception{

        SyncService syncService = this.applicationContext.getBean(SyncService.class);
        String namespace = UUID.randomUUID().toString();
        ImmutableMap<String, String> map = ImmutableMap.<String,String>builder()
                        .build();
        SyncServiceContainer syncServiceContainer = syncService.initSyncServiceContainer(namespace, ParentStep.class, map);
        SyncStatusService syncStatusService = syncServiceContainer.getBean(SyncStatusService.class);
        ConsumerService consumerService = syncServiceContainer.getBean(ConsumerService.class);
        syncService.startSync(syncServiceContainer);
        syncStatusService.waitUntilSyncIsInProgress();
        syncService.shutdown();
        return "Sync is completed";
    }
}
