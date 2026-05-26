package com.freshworks.core.processor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Phaser;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshworks.core.shared.MockFacadeSyncServiceContainer;
import com.freshworks.core.shared.Namespace;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.executor.SharedExecutorService;
import com.freshworks.core.shared.infra.InfraService;
import com.freshworks.core.shared.infra.persistent.MockFacadeMongoDbService;
import com.freshworks.core.shared.infra.persistent.MockFacadeMongodbQueue;
import com.freshworks.core.shared.infra.persistent.MongoDbQueue;
import com.freshworks.core.shared.sync.SyncStatusService;

@SpringBootTest
@EnabledIfSystemProperty(named = "spring.profiles.active", matches = ".*\\.unit\\..*")
public class TestProcessorService {

    @Autowired
    MockFacadeProcessorService mockFacadeProcessorService;

    @Autowired
    MockFacadeMongoDbService mockFacadeMongoDbService;

    @Autowired
    MockFacadeMongodbQueue mockFacadeMongodbQueue;

    @Autowired
    MockFacadeSyncServiceContainer mockFacadeSyncServiceContainer;

    @Autowired
    MockFacadeProcessorConfigService mockFacadeProcessorConfigService;

    @Autowired
    MockFacadeAssetBeanDependencyService mockFacadeAssetBeanDependencyService;


    @Autowired
    SyncStatusService syncStatusService;

    ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void beforeEach() throws Exception {

        mockFacadeProcessorService.configure().build();
        mockFacadeMongoDbService.configure().build();
        mockFacadeMongodbQueue.configure().build();
        mockFacadeSyncServiceContainer.configure().build();
        mockFacadeProcessorConfigService.configure().build();
        mockFacadeAssetBeanDependencyService.configure().build();
    }

    @Test
    public void testWhenAllDataIsProcessedThenMarkStatusAsSuccessful() throws Exception {

        Namespace namespace = new Namespace();
        namespace.setNamespace("integrated_test");

        List<String> returnedPolledItems = new ArrayList<>();
        MongoDbQueue mongoDbQueue =  mockFacadeMongodbQueue
                .pollNItems(returnedPolledItems)
                .hasMoreData(true, false)
                .build();
        InfraService mongoInfraService = mockFacadeMongoDbService.getProcessorQueue(mongoDbQueue).build();

        SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer
                .add(mongoInfraService, InfraService.class)
                .add(syncStatusService, SyncStatusService.class)
                .add(namespace, Namespace.class)
                .build();

        AssetBeanDependencyService assetBeanDependencyService = mockFacadeAssetBeanDependencyService
                .build();

        ProcessorConfigService processorConfigService = mockFacadeProcessorConfigService.build();
        processorConfigService.configure(syncServiceContainer);

        ProcessorService processorService = mockFacadeProcessorService.build();
        doCallRealMethod().when(processorService).configure(anyString(), any(), any(), any(), any(), any(),any());
        doCallRealMethod().when(processorService).run();

        processorService.configure( "traverser", new Phaser(), syncServiceContainer, assetBeanDependencyService, mongoInfraService, syncStatusService, processorConfigService );

        SharedExecutorService sharedExecutorService = syncServiceContainer.getBean(SharedExecutorService.class);
        assertThat(syncStatusService.getProcessor_status(), Matchers.is(-100));
        sharedExecutorService.submit(namespace.getNamespace(), processorService);
        syncStatusService.waitUntilProcessorIsInProgress();
        assertThat(syncStatusService.getProcessor_status(), Matchers.is(1));
    }

}
