package com.freshworks.core.shared.infra.persistent;

import com.freshworks.core.shared.MockFacadeSyncServiceContainer;
import com.freshworks.core.shared.Namespace;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.analytics.AnalyticsFactory;
import com.freshworks.core.shared.analytics.AnalyticsService;
import com.freshworks.core.shared.executor.SharedExecutorService;
import com.freshworks.core.shared.infra.*;
import com.freshworks.core.shared.sync.MockFacadeSyncStatusService;
import com.freshworks.core.shared.sync.SyncStatusService;
import com.freshworks.freshindex.NamespaceService;
import com.freshworks.freshindex.index.JsonIndexService;
import com.freshworks.freshindex.index.SummaryIndex;
import com.freshworks.freshindex.index.query.JsonQueryService;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.EnabledIf;

import java.io.IOException;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

@SpringBootTest
@EnabledIfSystemProperty(named = "spring.profiles.active", matches = ".*\\.unit\\.persistent")
public class TestMongoService {

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    MockFacadeMongoClientFactory mockFacadeMongoClientFactory;

    @Autowired
    MockFacadeInfraConfigService mockFacadeInfraConfigService;
    
    @Autowired
    MockFacadeSyncServiceContainer mockFacadeSyncServiceContainer;

    @Autowired
    MockFacadeMongoDbService mockFacadeMongoDbService;

    @BeforeEach
    public void setup() throws Exception {

        mockFacadeInfraConfigService.configure().build();
        mockFacadeSyncServiceContainer.configure().build();
        mockFacadeMongoDbService.configure().build();
        mockFacadeMongoClientFactory.configure().build();
    }


    @Nested
    public class SameNameSpace{

        @Test
        public void testInfraServiceReturnSameProcessorQueueEverytime() throws Exception {

            String namespace = UUID.randomUUID().toString();
            InfraConfigService infraConfigService = mockFacadeInfraConfigService.build();
            MongoClientFactory mongoClientFactory = mockFacadeMongoClientFactory.build();
            Namespace namespaceService = applicationContext.getBean(Namespace.class);
            namespaceService.setNamespace(namespace);

            SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer
                    .add(namespaceService, Namespace.class)
                    .build();

            syncServiceContainer.add(mongoClientFactory, MongoClientFactory.class);
            syncServiceContainer.add(namespaceService, Namespace.class);

            InfraService infraService = mockFacadeMongoDbService
                    .syncServiceContainer(syncServiceContainer)
                            .build();

            doCallRealMethod().when(infraService).configure(any(), any());
            doCallRealMethod().when(infraService).getProcessorQueue();
            infraService.configure(syncServiceContainer, infraConfigService);

            InfraDbQueue processorQueue1 = infraService.getProcessorQueue();
            InfraDbQueue processorQueue2 = infraService.getProcessorQueue();

            assertThat(processorQueue1, is(Matchers.equalToObject(processorQueue2)));

            infraService.destroy();
        }

        @Test
        public void testInfraServiceReturnSamePublisherListEverytime() throws Exception {

            String namespace = UUID.randomUUID().toString();
            InfraConfigService infraConfigService = mockFacadeInfraConfigService.build();
            MongoClientFactory mongoClientFactory = mockFacadeMongoClientFactory.build();

            Namespace namespaceService = applicationContext.getBean(Namespace.class);
            namespaceService.setNamespace(namespace);

            SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer
                    .add(namespaceService, Namespace.class)
                    .build();

            syncServiceContainer.add(mongoClientFactory, MongoClientFactory.class);
            syncServiceContainer.add(namespaceService, Namespace.class);

            InfraService infraService = mockFacadeMongoDbService
                    .syncServiceContainer(syncServiceContainer)
                    .build();

            doCallRealMethod().when(infraService).configure(any(), any());
            doCallRealMethod().when(infraService).getPublisherList();
            infraService.configure(syncServiceContainer, infraConfigService);

            InfraDbList publisherList1 = infraService.getPublisherList();
            InfraDbList publisherList2 = infraService.getPublisherList();

            assertThat(publisherList1, is(Matchers.equalToObject(publisherList2)));

            infraService.destroy();
        }

        @Test
        public void testInfraServiceReturnSameKeyValueListEverytime() throws Exception {

            String namespace = UUID.randomUUID().toString();
            InfraConfigService infraConfigService = mockFacadeInfraConfigService.build();
            MongoClientFactory mongoClientFactory = mockFacadeMongoClientFactory.build();

            Namespace namespaceService = applicationContext.getBean(Namespace.class);
            namespaceService.setNamespace(namespace);

            SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer
                    .add(namespaceService, Namespace.class)
                    .build();

            syncServiceContainer.add(mongoClientFactory, MongoClientFactory.class);
            syncServiceContainer.add(namespaceService, Namespace.class);

            InfraService infraService = mockFacadeMongoDbService
                    .syncServiceContainer(syncServiceContainer)
                    .build();

            doCallRealMethod().when(infraService).configure(any(), any());
            doCallRealMethod().when(infraService).getKeyValue();

            infraService.configure(syncServiceContainer, infraConfigService);

            InfraDbKeyValue keyValue1 = infraService.getKeyValue();
            InfraDbKeyValue keyValue2 = infraService.getKeyValue();

            assertThat(keyValue1, is(Matchers.equalToObject(keyValue2)));

            infraService.destroy();

        }


        @Test
        public void testInfraServiceReturnsSameNamespaceServiceEverytime() throws Exception {

            String namespace = UUID.randomUUID().toString();
            InfraConfigService infraConfigService = mockFacadeInfraConfigService.build();
            MongoClientFactory mongoClientFactory = mockFacadeMongoClientFactory.build();

            Namespace namespaceService = applicationContext.getBean(Namespace.class);
            namespaceService.setNamespace(namespace);

            SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer
                    .add(namespaceService, Namespace.class)
                    .build();
            syncServiceContainer.add(mongoClientFactory, MongoClientFactory.class);
            syncServiceContainer.add(namespaceService, Namespace.class);

            InfraService infraService = mockFacadeMongoDbService
                    .syncServiceContainer(syncServiceContainer)
                    .build();

            doCallRealMethod().when(infraService).configure(any(), any());
            doCallRealMethod().when(infraService).getNamespaceService();

            infraService.configure(syncServiceContainer, infraConfigService);

            NamespaceService namespaceService1 = infraService.getNamespaceService();
            NamespaceService namespaceService2 = infraService.getNamespaceService();

            assertThat(namespaceService1, is(Matchers.equalToObject(namespaceService2)));

            infraService.destroy();
        }

        @Test
        public void testInfraServiceReturnsSameListEverytime() throws Exception {

            String namespace = UUID.randomUUID().toString();
            InfraConfigService infraConfigService = mockFacadeInfraConfigService.build();
            MongoClientFactory mongoClientFactory = mockFacadeMongoClientFactory.build();

            Namespace namespaceService = applicationContext.getBean(Namespace.class);
            namespaceService.setNamespace(namespace);

            SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer
                    .add(namespaceService, Namespace.class)
                    .build();

            syncServiceContainer.add(mongoClientFactory, MongoClientFactory.class);
            syncServiceContainer.add(namespaceService, Namespace.class);

            InfraService infraService = mockFacadeMongoDbService
                    .syncServiceContainer(syncServiceContainer)
                    .build();

            doCallRealMethod().when(infraService).configure(any(), any());
            doCallRealMethod().when(infraService).getInfraDbList(anyString());

            infraService.configure(syncServiceContainer, infraConfigService);

            InfraDbList infraDbList1 = infraService.getInfraDbList("some_list_name");
            InfraDbList infraDbList2 = infraService.getInfraDbList("some_list_name");

            assertThat(infraDbList1, is(Matchers.equalToObject(infraDbList2)));

            infraService.destroy();

        }
//
        @Test
        public void testInfraServiceReturnsDifferentListForDifferentNameEverytime() throws Exception {

            String namespace = UUID.randomUUID().toString();
            InfraConfigService infraConfigService = mockFacadeInfraConfigService.build();
            MongoClientFactory mongoClientFactory = mockFacadeMongoClientFactory.build();

            Namespace namespaceService = applicationContext.getBean(Namespace.class);
            namespaceService.setNamespace(namespace);

            SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer
                    .add(namespaceService, Namespace.class)
                    .build();

            syncServiceContainer.add(mongoClientFactory, MongoClientFactory.class);
            syncServiceContainer.add(namespaceService, Namespace.class);

            InfraService infraService = mockFacadeMongoDbService
                    .syncServiceContainer(syncServiceContainer)
                    .build();

            doCallRealMethod().when(infraService).configure(any(), any());
            doCallRealMethod().when(infraService).getInfraDbList(anyString());

            infraService.configure(syncServiceContainer, infraConfigService);

            InfraDbList infraDbList1 = infraService.getInfraDbList("some_list_name");
            InfraDbList infraDbList2 = infraService.getInfraDbList("some_another_list_name");

            assertThat(infraDbList1, is(Matchers.not(Matchers.equalToObject(infraDbList2))));

            infraService.destroy();

        }
//
        @Test
        public void testInfraServiceDestroyAllObject() throws Exception {


            String namespace = UUID.randomUUID().toString();
            InfraConfigService infraConfigService = mockFacadeInfraConfigService.build();
            MongoClientFactory mongoClientFactory = mockFacadeMongoClientFactory.build();

            Namespace namespaceService = applicationContext.getBean(Namespace.class);
            namespaceService.setNamespace(namespace);

            SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer
                    .add(namespaceService, Namespace.class)
                    .build();

            syncServiceContainer.add(mongoClientFactory, MongoClientFactory.class);
            syncServiceContainer.add(namespaceService, Namespace.class);

            InfraService infraService = mockFacadeMongoDbService
                    .syncServiceContainer(syncServiceContainer)
                    .build();

            doCallRealMethod().when(infraService).configure(any(), any());
            doCallRealMethod().when(infraService).getProcessorQueue();
            doCallRealMethod().when(infraService).getPublisherList();

            infraService.configure(syncServiceContainer, infraConfigService);


            InfraDbQueue processorQueue = infraService.getProcessorQueue();
            InfraDbList publisherList = infraService.getPublisherList();


            // Assert that non of them is null
            assertThat(processorQueue, is(Matchers.notNullValue()));
            assertThat(publisherList, is(Matchers.notNullValue()));


            // Now destroy the infra
            infraService.destroy();


            InfraDbQueue processorQueue1 = infraService.getProcessorQueue();
            InfraDbList publisherList1 = infraService.getPublisherList();


            assertThat(processorQueue, is(Matchers.not(processorQueue1)));
            assertThat(publisherList, is(Matchers.not(publisherList1)));

            //TODO We need to asset FreshIndex objects also but should be done only once we refactor the code of freshIndex.

            infraService.destroy();
        }
    }
//
    @Nested
    public class DifferentNameSpace{
//
        @Test
        public void testInfraServiceReturnDifferentProcessorQueueEverytime() throws Exception {

            String namespace = UUID.randomUUID().toString();
            InfraConfigService infraConfigService = mockFacadeInfraConfigService.build();
            MongoClientFactory mongoClientFactory = mockFacadeMongoClientFactory.build();

            Namespace namespaceService = applicationContext.getBean(Namespace.class);
            namespaceService.setNamespace(namespace);

            SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer
                    .add(namespaceService, Namespace.class)
                    .build();

            syncServiceContainer.add(mongoClientFactory, MongoClientFactory.class);
            syncServiceContainer.add(namespaceService, Namespace.class);

            InfraService infraService = mockFacadeMongoDbService
                    .syncServiceContainer(syncServiceContainer)
                    .build();

            doCallRealMethod().when(infraService).configure(any(), any());
            doCallRealMethod().when(infraService).getProcessorQueue();
            infraService.configure(syncServiceContainer, infraConfigService);

            InfraDbQueue processorQueue1 = infraService.getProcessorQueue();


            String namespace2 = UUID.randomUUID().toString();
            InfraConfigService infraConfigService2 = mockFacadeInfraConfigService.build();
            SyncServiceContainer syncServiceContainer2 = mockFacadeSyncServiceContainer.build();
            MongoClientFactory mongoClientFactory2 = mockFacadeMongoClientFactory.build();

            namespaceService = applicationContext.getBean(Namespace.class);
            namespaceService.setNamespace(namespace2);

            syncServiceContainer.add(namespaceService, Namespace.class);
            syncServiceContainer.add(mongoClientFactory2, MongoClientFactory.class);
            syncServiceContainer.add(namespaceService, Namespace.class);

            InfraService infraService2 = mockFacadeMongoDbService
                    .syncServiceContainer(syncServiceContainer2)
                    .build();

            doCallRealMethod().when(infraService2).configure(any(), any());
            doCallRealMethod().when(infraService2).getProcessorQueue();
            infraService2.configure(syncServiceContainer, infraConfigService2);
            InfraDbQueue processorQueue2 = infraService2.getProcessorQueue();

            assertThat(processorQueue1, is(Matchers.not(Matchers.equalToObject(processorQueue2))));

            infraService.destroy();
            infraService2.destroy();
        }
//
        @Test
        public void testInfraServiceReturnDifferentPublisherListEverytime() throws Exception {


            String namespace = UUID.randomUUID().toString();
            InfraConfigService infraConfigService = mockFacadeInfraConfigService.build();
            SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer.build();
            MongoClientFactory mongoClientFactory = mockFacadeMongoClientFactory.build();

            Namespace namespaceService = applicationContext.getBean(Namespace.class);
            namespaceService.setNamespace(namespace);

            syncServiceContainer.add(namespaceService, Namespace.class);
            syncServiceContainer.add(mongoClientFactory, MongoClientFactory.class);
            syncServiceContainer.add(namespaceService, Namespace.class);

            InfraService infraService = mockFacadeMongoDbService
                    .syncServiceContainer(syncServiceContainer)
                    .build();

            doCallRealMethod().when(infraService).configure(any(), any());
            doCallRealMethod().when(infraService).getPublisherList();
            infraService.configure(syncServiceContainer, infraConfigService);

            InfraDbList publisherList1 = infraService.getPublisherList();


            String namespace2 = UUID.randomUUID().toString();
            InfraConfigService infraConfigService2 = mockFacadeInfraConfigService.build();
            SyncServiceContainer syncServiceContainer2 = mockFacadeSyncServiceContainer.build();
            MongoClientFactory mongoClientFactory2 = mockFacadeMongoClientFactory.build();
            namespaceService = applicationContext.getBean(Namespace.class);
            namespaceService.setNamespace(namespace2);

            syncServiceContainer.add(namespaceService, Namespace.class);
            syncServiceContainer.add(mongoClientFactory2, MongoClientFactory.class);
            syncServiceContainer.add(namespaceService, Namespace.class);

            InfraService infraService2 = mockFacadeMongoDbService
                    .syncServiceContainer(syncServiceContainer2)
                    .build();

            doCallRealMethod().when(infraService2).configure(any(), any());
            doCallRealMethod().when(infraService2).getPublisherList();
            infraService2.configure(syncServiceContainer, infraConfigService2);

            InfraDbList publisherList2 = infraService2.getPublisherList();

            assertThat(publisherList1, is(Matchers.not(Matchers.equalToObject(publisherList2))));

            infraService.destroy();
            infraService2.destroy();
        }
//
        @Test
        public void testInfraServiceReturnDifferentKeyValueListEverytime() throws Exception {


            String namespace = UUID.randomUUID().toString();
            InfraConfigService infraConfigService = mockFacadeInfraConfigService.build();
            SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer.build();
            MongoClientFactory mongoClientFactory = mockFacadeMongoClientFactory.build();

            Namespace namespaceService = applicationContext.getBean(Namespace.class);
            namespaceService.setNamespace(namespace);

            syncServiceContainer.add(namespaceService, Namespace.class);
            syncServiceContainer.add(mongoClientFactory, MongoClientFactory.class);
            syncServiceContainer.add(namespaceService, Namespace.class);

            InfraService infraService = mockFacadeMongoDbService
                    .syncServiceContainer(syncServiceContainer)
                    .build();

            doCallRealMethod().when(infraService).configure(any(), any());
            doCallRealMethod().when(infraService).getKeyValue();
            infraService.configure(syncServiceContainer, infraConfigService);

            InfraDbKeyValue keyValue1 = infraService.getKeyValue();


            String namespace2 = UUID.randomUUID().toString();
            InfraConfigService infraConfigService2 = mockFacadeInfraConfigService.build();
            SyncServiceContainer syncServiceContainer2 = mockFacadeSyncServiceContainer.build();
            MongoClientFactory mongoClientFactory2 = mockFacadeMongoClientFactory.build();

            namespaceService = applicationContext.getBean(Namespace.class);
            namespaceService.setNamespace(namespace2);
            syncServiceContainer.add(namespaceService, Namespace.class);
            syncServiceContainer.add(mongoClientFactory2, MongoClientFactory.class);
            syncServiceContainer.add(namespaceService, Namespace.class);

            InfraService infraService2 = mockFacadeMongoDbService
                    .syncServiceContainer(syncServiceContainer2)
                    .build();

            doCallRealMethod().when(infraService2).configure(any(), any());
            doCallRealMethod().when(infraService2).getKeyValue();
            infraService2.configure(syncServiceContainer, infraConfigService2);

            InfraDbKeyValue keyValue2 = infraService2.getKeyValue();

            assertThat(keyValue1, is(Matchers.not(Matchers.equalToObject(keyValue2))));
            infraService.destroy();
            infraService2.destroy();


        }
//
//        @Test
//        public void testInfraServiceReturnsDifferentJsonQueryServiceEverytime(){}
//
//        @Test
//        public void testInfraServiceReturnsDifferentNamespaceServiceEverytime(){}
//
//        @Test
//        public void testInfraServiceReturnsDifferentJsonIndexServiceEverytime(){}
//
//        @Test
//        public void testInfraServiceReturnsDifferentListForSameNameEverytime() throws IOException {
//
//            InfraService infraService = new MongoService(syncServiceContainer);
//            String namespace = "some_random_namespace";
//            infraService.configure(namespace, infraConfigService);
//            InfraDbList list1 = infraService.getInfraDbList("same-list-name");
//
//
//            InfraService infraService2 = new MongoService(syncServiceContainer);
//            namespace = "some_another_random_namespace";
//            infraService2.configure(namespace, infraConfigService);
//            InfraDbList list2 = infraService2.getInfraDbList("same-list-name");
//
//            assertThat(list1, is(Matchers.not(Matchers.equalToObject(list2))));
//
//        }
//
//        @Test
//        public void testInfraServiceDestroyAllObjectFromSameNamespace() throws IOException {
//
//            InfraService infraService = new MongoService(syncServiceContainer);
//            String namespace = "some_random_namespace";
//            infraService.configure(namespace, infraConfigService);
//            InfraDbQueue processorQueue = infraService.getProcessorQueue();
//            InfraDbList publisherList = infraService.getPublisherList();
//
//            System.out.println("Destroy all objects");
//
//            // Create infra from another service
//            InfraService anotherInfraService2 = new MongoService(syncServiceContainer);
//            namespace = "some_another_random_namespace";
//            anotherInfraService2.configure(namespace, infraConfigService);
//            InfraDbQueue anotherProcessorQueue = anotherInfraService2.getProcessorQueue();
//
//            System.out.println("Destroy all objects");
//            InfraDbList anotherPublisherList = anotherInfraService2.getPublisherList();
//            System.out.println("Destroy all objects");
//
//
//            System.out.println("Destroy another all objects");
//
//            // Now destroy the first infra
//            infraService.destroy();
//
//            // Fetch the object again
//            InfraDbQueue processorQueue1 = infraService.getProcessorQueue();
//            InfraDbList publisherList1 = infraService.getPublisherList();
//
//            System.out.println("Destroy another another all objects");
//
//            // First infra should have completely new objects
//            assertThat(processorQueue, is(Matchers.not(processorQueue1)));
//            assertThat(publisherList, is(Matchers.not(publisherList1)));
//
//
//            // Now fetch from second infra which was not destroyed
//            InfraDbQueue anotherProcessorQueue1 = anotherInfraService2.getProcessorQueue();
//            InfraDbList anotherPublisherList1 = anotherInfraService2.getPublisherList();
//
//            // There should NOT be any change in it.
//            assertThat(anotherProcessorQueue, is(Matchers.equalToObject(anotherProcessorQueue1)));
//            assertThat(anotherPublisherList, is(Matchers.equalToObject(anotherPublisherList1)));
//
//            //TODO We need to asset FreshIndex objects also but should be done only once we refactor the code of freshIndex.
//        }
    }
}
