package com.freshworks.core.shared.infra.inmemory;

import com.freshworks.core.shared.MockFacadeSyncServiceContainer;
import com.freshworks.core.shared.Namespace;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.analytics.AnalyticsJPA;
import com.freshworks.core.shared.analytics.AnalyticsService;
import com.freshworks.core.shared.executor.SharedExecutorService;
import com.freshworks.core.shared.infra.*;
import com.freshworks.freshindex.NamespaceService;
import com.freshworks.freshindex.index.JsonIndexService;
import com.freshworks.freshindex.index.SummaryIndex;
import com.freshworks.freshindex.index.query.JsonQueryService;

import org.checkerframework.checker.units.qual.A;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.EnabledIf;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@SpringBootTest
@EnabledIfSystemProperty(named = "spring.profiles.active", matches = ".*\\.unit\\.inmemory")
public class TestInMemoryService {

    @Autowired
    ApplicationContext applicationContext;

    @MockBean
    InfraConfigService infraConfigService;

    @Autowired
    MockFacadeSyncServiceContainer mockFacadeSyncServiceContainer;

    @BeforeEach
    public void beforeEach(){
        mockFacadeSyncServiceContainer.configure().build();
    }

    @Nested
    public class SameNameSpace{

        @Test
        public void testInfraServiceReturnSameProcessorQueueEverytime() throws Exception {

            InfraService infraService = new InmemoryService();
            String namespace = "some_random_namespace";
            Namespace namespaceService = new Namespace();
            namespaceService.setNamespace(namespace);
            SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer
                    .add(namespaceService, Namespace.class)
                            .build();
            infraService.configure(syncServiceContainer, infraConfigService);

            InfraDbQueue processorQueue1 = infraService.getProcessorQueue();
            InfraDbQueue processorQueue2 = infraService.getProcessorQueue();

            assertThat(processorQueue1, is(Matchers.equalToObject(processorQueue2)));
        }

        @Test
        public void testInfraServiceReturnSamePublisherListEverytime() throws Exception {

            InfraService infraService = new InmemoryService();
            String namespace = "some_random_namespace";
            Namespace namespaceService = new Namespace();
            namespaceService.setNamespace(namespace);
            SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer
                    .add(namespaceService, Namespace.class)
                    .build();
            infraService.configure(syncServiceContainer, infraConfigService);

            InfraDbList publisherList1 = infraService.getPublisherList();
            InfraDbList publisherList2 = infraService.getPublisherList();

            assertThat(publisherList1, is(Matchers.equalToObject(publisherList2)));
        }

        @Test
        public void testInfraServiceReturnSameKeyValueListEverytime() throws Exception {

            InfraService infraService = new InmemoryService();
            String namespace = "some_random_namespace";

            Namespace namespaceService = new Namespace();
            namespaceService.setNamespace(namespace);
            SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer
                    .add(namespaceService, Namespace.class)
                    .build();
            infraService.configure(syncServiceContainer, infraConfigService);

            InfraDbKeyValue keyValue1 = infraService.getKeyValue();
            InfraDbKeyValue keyValue2 = infraService.getKeyValue();

            assertThat(keyValue1, is(Matchers.equalToObject(keyValue2)));

        }

        @Test
        public void testInfraServiceReturnsSameJsonQueryServiceEverytime() throws Exception {

            InfraService infraService = new InmemoryService();
            String namespace = "some_random_namespace";

            Namespace namespaceService = new Namespace();
            namespaceService.setNamespace(namespace);
            SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer
                    .add(namespaceService, Namespace.class)
                    .build();
            infraService.configure(syncServiceContainer, infraConfigService);

            // Instead of this model, we can modify the freshIndex to have freshIndexService. Via this service we can extract,
            // JsonIndexService, JsonQueryService and Namespace Service. All this needs to be model in FreshIndex
            JsonQueryService jsonQueryService1 = infraService.getJsonQueryService();
            JsonQueryService jsonQueryService2 = infraService.getJsonQueryService();

//            assertThat(jsonQueryService1, Matchers.is(Matchers.equalToObject(jsonQueryService2.getSummaryIndex())));
        }

        @Test
        public void testInfraServiceReturnsSameJsonIndexServiceEverytime() throws Exception {

            InfraService infraService = new InmemoryService();
            String namespace = "some_random_namespace";
            Namespace namespaceService = new Namespace();
            namespaceService.setNamespace(namespace);
            SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer
                    .add(namespaceService, Namespace.class)
                    .build();
            infraService.configure(syncServiceContainer, infraConfigService);

            // Instead of this model, we can modify the freshIndex to have freshIndexService. Via this service we can extract,
            // JsonIndexService, JsonQueryService and Namespace Service. All this needs to be model in FreshIndex
            JsonIndexService jsonIndexService1 = infraService.getJsonIndexService();
            JsonIndexService jsonIndexService2 = infraService.getJsonIndexService();

//            assertThat(jsonIndexService1.getSummaryIndex(), Matchers.is(Matchers.equalToObject(jsonIndexService2.getSummaryIndex())));
        }

        @Test
        public void testInfraServiceReturnsSameNamespaceServiceEverytime() throws Exception {

            InfraService infraService = new InmemoryService();
            String namespace = "some_random_namespace";
            Namespace namespaceService = new Namespace();
            namespaceService.setNamespace(namespace);
            SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer
                    .add(namespaceService, Namespace.class)
                    .build();
            infraService.configure(syncServiceContainer, infraConfigService);

            NamespaceService namespaceService1 = infraService.getNamespaceService();
            NamespaceService namespaceService2 = infraService.getNamespaceService();

            assertThat(namespaceService1, is(Matchers.equalToObject(namespaceService2)));
        }

        @Test
        public void testInfraServiceReturnsSameListEverytime() throws Exception {

            InfraService infraService = new InmemoryService();
            String namespace = "some_random_namespace";

            Namespace namespaceService = new Namespace();
            namespaceService.setNamespace(namespace);
            SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer
                    .add(namespaceService, Namespace.class)
                    .build();
            infraService.configure(syncServiceContainer, infraConfigService);

            InfraDbList infraDbList1 = infraService.getInfraDbList("some_list_name");
            InfraDbList infraDbList2 = infraService.getInfraDbList("some_list_name");

            assertThat(infraDbList1, is(Matchers.equalToObject(infraDbList2)));

        }

        @Test
        public void testInfraServiceReturnsDifferentListForDifferentNameEverytime() throws Exception {

            InfraService infraService = new InmemoryService();
            String namespace = "some_random_namespace";

            Namespace namespaceService = new Namespace();
            namespaceService.setNamespace(namespace);
            SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer
                    .add(namespaceService, Namespace.class)
                    .build();
            infraService.configure(syncServiceContainer, infraConfigService);

            InfraDbList infraDbList1 = infraService.getInfraDbList("some_list_name");
            InfraDbList infraDbList2 = infraService.getInfraDbList("some_another_list_name");

            assertThat(infraDbList1, is(Matchers.not(Matchers.equalToObject(infraDbList2))));

        }

        @Test
        public void testInfraServiceDestroyAllObject() throws Exception {

            InfraService infraService = new InmemoryService();
            String namespace = "some_random_namespace";

            Namespace namespaceService = new Namespace();
            namespaceService.setNamespace(namespace);
            SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer
                    .add(namespaceService, Namespace.class)
                    .build();
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
        }
    }

    @Nested
    public class DifferentNameSpace{

        @Test
        public void testInfraServiceReturnDifferentProcessorQueueEverytime() throws Exception {

            InfraService infraService = new InmemoryService();
            String namespace = "some_random_namespace";

            Namespace namespaceService = new Namespace();
            namespaceService.setNamespace(namespace);
            SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer
                    .add(namespaceService, Namespace.class)
                    .build();
            infraService.configure( syncServiceContainer, infraConfigService);
            InfraDbQueue processorQueue1 = infraService.getProcessorQueue();


            InfraService infraService2 = new InmemoryService();
            namespace = "some_another_random_namespace";
            namespaceService = new Namespace();
            namespaceService.setNamespace(namespace);
            syncServiceContainer = mockFacadeSyncServiceContainer
                    .add(namespaceService, Namespace.class)
                    .build();
            infraService2.configure(syncServiceContainer, infraConfigService);
            InfraDbQueue processorQueue2 = infraService2.getProcessorQueue();

            assertThat(processorQueue1, is(Matchers.not(Matchers.equalToObject(processorQueue2))));
        }

        @Test
        public void testInfraServiceReturnDifferentPublisherListEverytime() throws Exception {

            InfraService infraService = new InmemoryService();
            String namespace = "some_random_namespace";
            Namespace namespaceService = new Namespace();
            namespaceService.setNamespace(namespace);
            SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer
                    .add(namespaceService, Namespace.class)
                    .build();
            infraService.configure( syncServiceContainer, infraConfigService);
            InfraDbList publisherList1 = infraService.getPublisherList();


            InfraService infraService2 = new InmemoryService();
            namespace = "some_another_random_namespace";
            namespaceService = new Namespace();
            namespaceService.setNamespace(namespace);
            syncServiceContainer = mockFacadeSyncServiceContainer
                    .add(namespaceService, Namespace.class)
                    .build();
            infraService2.configure(syncServiceContainer, infraConfigService);
            InfraDbList publisherList2 = infraService2.getPublisherList();

            assertThat(publisherList1, is(Matchers.not(Matchers.equalToObject(publisherList2))));
        }

        @Test
        public void testInfraServiceReturnDifferentKeyValueListEverytime() throws Exception {

            InfraService infraService = new InmemoryService();
            String namespace = "some_random_namespace";
            Namespace namespaceService = new Namespace();
            namespaceService.setNamespace(namespace);
            SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer
                    .add(namespaceService, Namespace.class)
                    .build();
            infraService.configure(syncServiceContainer, infraConfigService);
            InfraDbKeyValue keyValue1 = infraService.getKeyValue();


            InfraService infraService2 = new InmemoryService();
            namespace = "some_another_random_namespace";
            namespaceService = new Namespace();
            namespaceService.setNamespace(namespace);
            syncServiceContainer = mockFacadeSyncServiceContainer
                    .add(namespaceService, Namespace.class)
                    .build();
            infraService2.configure(syncServiceContainer, infraConfigService);
            InfraDbKeyValue keyValue2 = infraService2.getKeyValue();

            assertThat(keyValue1, is(Matchers.not(Matchers.equalToObject(keyValue2))));

        }

        @Test
        public void testInfraServiceReturnsDifferentJsonQueryServiceEverytime(){}

        @Test
        public void testInfraServiceReturnsDifferentNamespaceServiceEverytime(){}

        @Test
        public void testInfraServiceReturnsDifferentJsonIndexServiceEverytime(){}

        @Test
        public void testInfraServiceReturnsDifferentListForSameNameEverytime() throws Exception {

            InfraService infraService = new InmemoryService();
            String namespace = "some_random_namespace";
            Namespace namespaceService = new Namespace();
            namespaceService.setNamespace(namespace);
            SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer
                    .add(namespaceService, Namespace.class)
                    .build();
            infraService.configure( syncServiceContainer, infraConfigService);
            InfraDbList list1 = infraService.getInfraDbList("same-list-name");


            InfraService infraService2 = new InmemoryService();
            namespace = "some_another_random_namespace";
            namespaceService = new Namespace();
            namespaceService.setNamespace(namespace);
            syncServiceContainer = mockFacadeSyncServiceContainer
                    .add(namespaceService, Namespace.class)
                    .build();
            infraService2.configure(syncServiceContainer, infraConfigService);
            InfraDbList list2 = infraService2.getInfraDbList("same-list-name");

            assertThat(list1, is(Matchers.not(Matchers.equalToObject(list2))));

        }

        @Test
        public void testInfraServiceDestroyAllObjectFromSameNamespace() throws Exception {

            InfraService infraService = new InmemoryService();
            String namespace = "some_random_namespace";
            Namespace namespaceService = new Namespace();
            namespaceService.setNamespace(namespace);
            SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer
                    .add(namespaceService, Namespace.class)
                    .build();
            infraService.configure(syncServiceContainer, infraConfigService);
            InfraDbQueue processorQueue = infraService.getProcessorQueue();
            InfraDbList publisherList = infraService.getPublisherList();


            // Create infra from another service
            InfraService anotherInfraService2 = new InmemoryService();
            namespace = "some_another_random_namespace";
            namespaceService = new Namespace();
            namespaceService.setNamespace(namespace);
            syncServiceContainer = mockFacadeSyncServiceContainer
                    .add(namespaceService, Namespace.class)
                    .build();
            anotherInfraService2.configure(syncServiceContainer, infraConfigService);
            InfraDbQueue anotherProcessorQueue = anotherInfraService2.getProcessorQueue();
            InfraDbList anotherPublisherList = anotherInfraService2.getPublisherList();


            // Now destroy the first infra
            infraService.destroy();

            // Fetch the object again
            InfraDbQueue processorQueue1 = infraService.getProcessorQueue();
            InfraDbList publisherList1 = infraService.getPublisherList();

            // First infra should have completely new objects
            assertThat(processorQueue, is(Matchers.not(processorQueue1)));
            assertThat(publisherList, is(Matchers.not(publisherList1)));


            // Now fetch from second infra which was not destroyed
            InfraDbQueue anotherProcessorQueue1 = anotherInfraService2.getProcessorQueue();
            InfraDbList anotherPublisherList1 = anotherInfraService2.getPublisherList();

            // There should NOT be any change in it.
            assertThat(anotherProcessorQueue, is(Matchers.equalToObject(anotherProcessorQueue1)));
            assertThat(anotherPublisherList, is(Matchers.equalToObject(anotherPublisherList1)));

            //TODO We need to asset FreshIndex objects also but should be done only once we refactor the code of freshIndex.
        }
    }
}
