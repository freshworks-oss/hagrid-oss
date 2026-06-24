package com.freshworks.core.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshworks.core.data.four_five_zero.unit.fb.beans.FbUser;
import com.freshworks.core.shared.MockFacadeSyncServiceContainer;
import com.freshworks.core.shared.Namespace;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.consumer.MockFacadeConsumerService;
import com.freshworks.core.shared.executor.SharedExecutorService;
import com.freshworks.core.shared.infra.InfraConfigService;
import com.freshworks.core.shared.infra.InfraDbList;
import com.freshworks.core.shared.infra.InfraService;
import com.freshworks.core.shared.infra.MockFacadeInfraConfigService;
import com.freshworks.core.shared.infra.nitrite.MockFacadeNitriteDbService;
import com.freshworks.core.shared.infra.nitrite.MockFacadeNitritedbList;
import com.freshworks.core.shared.infra.nitrite.MockFacadeNitritedbQueue;
import com.freshworks.core.shared.infra.nitrite.NitriteDbList;
import com.freshworks.core.shared.infra.nitrite.NitriteDbQueue;
import com.freshworks.core.shared.infra.persistent.MockFacadeMongoDbService;
import com.freshworks.core.shared.infra.persistent.MockFacadeMongodbQueue;
import com.freshworks.core.shared.infra.persistent.MongoDbQueue;
import com.freshworks.core.shared.sync.SyncStatusService;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimap;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Phaser;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doCallRealMethod;

@SpringBootTest
@EnabledIfSystemProperty(named = "spring.profiles.active", matches = ".*\\.unit\\..*")
public class TestProcessorService {

    @Autowired
    MockFacadeProcessorService mockFacadeProcessorService;

    @Autowired
    MockFacadeMongoDbService mockFacadeMongoDbService;

    @Autowired
    MockFacadeNitriteDbService mockFacadeNitriteDbService;

    @Autowired
    MockFacadeMongodbQueue mockFacadeMongodbQueue;

    @Autowired
    MockFacadeNitritedbQueue mockFacadeNitritedbQueue;

    @Autowired
    MockFacadeNitritedbList mockFacadeNitritedbList;

    MockFacadeConsumerService mockFacadeConsumerService;

    @Autowired
    MockFacadeSyncServiceContainer mockFacadeSyncServiceContainer;

    @Autowired
    MockFacadeInfraConfigService mockFacadeInfraConfigService;

    @Autowired
    MockFacadeProcessorConfigService mockFacadeProcessorConfigService;

    @Autowired
    MockFacadeAssetBeanDependencyService mockFacadeAssetBeanDependencyService;

    @Autowired
    MockFacadeAssetAssetDependencyService mockFacadeAssetAssetDependencyService;

    @Autowired
    SyncStatusService syncStatusService;

    ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    public void beforeEach() throws Exception {

        mockFacadeProcessorService.configure().build();
        mockFacadeMongoDbService.configure().build();
        mockFacadeNitriteDbService.configure().build();
        mockFacadeMongodbQueue.configure().build();
        mockFacadeNitritedbQueue.configure().build();
        mockFacadeNitritedbList.configure().build();
        mockFacadeSyncServiceContainer.configure().build();
        mockFacadeInfraConfigService.configure().build();
        mockFacadeProcessorConfigService.configure().build();
        mockFacadeAssetBeanDependencyService.configure().build();
        mockFacadeAssetAssetDependencyService.configure().build();
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

        AssetAssetDependencyService assetAssetDependencyService = mockFacadeAssetAssetDependencyService.build();

        ProcessorConfigService processorConfigService = mockFacadeProcessorConfigService.build();
        processorConfigService.configure(syncServiceContainer);

        ProcessorService processorService = mockFacadeProcessorService.build();
        doCallRealMethod().when(processorService).configure(anyString(), any(), any(), any(), any(), any(), any(),any());
        doCallRealMethod().when(processorService).run();

        processorService.configure( "traverser", new Phaser(), syncServiceContainer, assetBeanDependencyService, assetAssetDependencyService, mongoInfraService, syncStatusService, processorConfigService );

        SharedExecutorService sharedExecutorService = syncServiceContainer.getBean(SharedExecutorService.class);
        assertThat(syncStatusService.getProcessor_status(), Matchers.is(-100));
        sharedExecutorService.submit(namespace.getNamespace(), processorService);
        syncStatusService.waitUntilProcessorIsInProgress();
        assertThat(syncStatusService.getProcessor_status(), Matchers.is(1));
    }

    @Test
    public void testProcessorServiceReturnsWhenProcessorTaskServiceInstancesAreReturned() throws Exception {

        Namespace namespace = new Namespace();
        namespace.setNamespace("unit_processor_test");

        List<String> returnedPolledItems = new ArrayList<>();

        FbUser fbUser = new FbUser();
        fbUser.setUser_id("first_user_id");
        fbUser.setUser_name("first_user_name");

        String s = objectMapper.writeValueAsString(fbUser);
        returnedPolledItems.add(s);

        fbUser.setUser_id("second_user_id");
        fbUser.setUser_name("second_user_name");

        s = objectMapper.writeValueAsString(fbUser);
        returnedPolledItems.add(s);


        NitriteDbQueue nitriteDbQueue =  mockFacadeNitritedbQueue
                .pollNItems(returnedPolledItems)
                .hasMoreData(true, true, false)
                .build();

        InfraConfigService infraConfigService = mockFacadeInfraConfigService
        .getInfraType("null")
        .build();

        InfraService nitriteInfraService = mockFacadeNitriteDbService
            .getProcessorQueue(nitriteDbQueue)
            .infraConfigService(infraConfigService)
            .build();
        doCallRealMethod().when(nitriteInfraService).getPublisherList();

        InfraDbList nitriteDbList = nitriteInfraService.getPublisherList();

        SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer
                .add(nitriteInfraService, InfraService.class)
                .add(syncStatusService, SyncStatusService.class)
                .add(namespace, Namespace.class)
                .build();

        
        Multimap<String, String> connectorConfigItemTable = ArrayListMultimap.create();
        connectorConfigItemTable.put("com.freshworks.core.data.four_five_zero.unit.fb.assets.FbUser", "com.freshworks.core.four_five_zero.unit.fb.beans.FbUser");
        
        AssetBeanDependencyService assetBeanDependencyService = mockFacadeAssetBeanDependencyService
        .scanner(ImmutableListMultimap.copyOf(connectorConfigItemTable))
                .build();

        AssetAssetDependencyService assetAssetDependencyService = mockFacadeAssetAssetDependencyService.build();

        ProcessorConfigService processorConfigService = mockFacadeProcessorConfigService
        .getNumberOfParallelProcessor(1)
        .build();

        processorConfigService.configure(syncServiceContainer);

        ProcessorService processorService = mockFacadeProcessorService.build();
        doCallRealMethod().when(processorService).configure(anyString(), any(), any(), any(), any(), any(), any(),any());
        doCallRealMethod().when(processorService).run();

        Phaser processorPhaser = new Phaser();
        processorService.configure( "processor", processorPhaser, syncServiceContainer, assetBeanDependencyService, assetAssetDependencyService, nitriteInfraService, syncStatusService, processorConfigService);

        SharedExecutorService sharedExecutorService = syncServiceContainer.getBean(SharedExecutorService.class);
        assertThat(syncStatusService.getProcessor_status(), Matchers.is(-100));
        sharedExecutorService.submit(namespace.getNamespace(), processorService);
        syncStatusService.waitUntilProcessorIsInProgress();
        List<String> publishedDocuments = nitriteDbList.get(0, 100);
        String publishedList = objectMapper.writeValueAsString(publishedDocuments);

        System.out.print(publishedList);

        assertThat(publishedDocuments.size(), Matchers.is(2));
        assertThat(syncStatusService.getProcessor_status(), Matchers.is(1));
    }

}
