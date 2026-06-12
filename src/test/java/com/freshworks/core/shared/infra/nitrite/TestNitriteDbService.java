package com.freshworks.core.shared.infra.nitrite;

import com.freshworks.core.shared.MockFacadeSyncServiceContainer;
import com.freshworks.core.shared.Namespace;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.infra.*;
import com.freshworks.freshindex.NamespaceService;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

@SpringBootTest
@DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
@EnabledIfSystemProperty(named = "spring.profiles.active", matches = ".*\\.unit\\.nitrite")
public class TestNitriteDbService {

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    MockFacadeNitriteClientFactory mockFacadeH2ClientFactory;

    @Autowired
    MockFacadeInfraConfigService mockFacadeInfraConfigService;

    @Autowired
    MockFacadeSyncServiceContainer mockFacadeSyncServiceContainer;

    @Autowired
    MockFacadeNitriteDbService mockFacadeH2DbService;


    @BeforeEach
    public void setup() throws Exception {


        mockFacadeInfraConfigService.configure().build();
        mockFacadeSyncServiceContainer.configure().build();
        mockFacadeH2DbService.configure().build();
        mockFacadeH2ClientFactory.configure().build();
    }


    @Nested
    public class SameNameSpace{

        @Test
        public void testInfraServiceReturnSameProcessorQueueEverytime() throws Exception {

            String namespace = UUID.randomUUID().toString();
            Namespace namespaceService = applicationContext.getBean(Namespace.class);
            namespaceService.setNamespace(namespace);
            SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer.build();
            syncServiceContainer.add(namespaceService, Namespace.class);


            InfraConfigService infraConfigService = mockFacadeInfraConfigService.build();
            doCallRealMethod().when(infraConfigService).configure(any());
            infraConfigService.configure(syncServiceContainer);
            syncServiceContainer.add(infraConfigService);


            InfraService infraService = mockFacadeH2DbService
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
            SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer.build();
            Namespace namespaceService = applicationContext.getBean(Namespace.class);
            namespaceService.setNamespace(namespace);
            syncServiceContainer.add(namespaceService, Namespace.class);

            InfraConfigService infraConfigService = mockFacadeInfraConfigService.build();
            doCallRealMethod().when(infraConfigService).configure(any());
            infraConfigService.configure(syncServiceContainer);

            InfraService infraService = mockFacadeH2DbService
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
            SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer.build();
            Namespace namespaceService = applicationContext.getBean(Namespace.class);
            namespaceService.setNamespace(namespace);
            syncServiceContainer.add(namespaceService, Namespace.class);
            InfraConfigService infraConfigService = mockFacadeInfraConfigService.build();
            doCallRealMethod().when(infraConfigService).configure(any());
            infraConfigService.configure(syncServiceContainer);

            InfraService infraService = mockFacadeH2DbService
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
            SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer.build();
            Namespace namespaceService = applicationContext.getBean(Namespace.class);
            namespaceService.setNamespace(namespace);
            syncServiceContainer.add(namespaceService, Namespace.class);

            InfraConfigService infraConfigService = mockFacadeInfraConfigService.build();
            doCallRealMethod().when(infraConfigService).configure(any());
            infraConfigService.configure(syncServiceContainer);

            InfraService infraService = mockFacadeH2DbService
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
            SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer.build();
            Namespace namespaceService = applicationContext.getBean(Namespace.class);
            namespaceService.setNamespace(namespace);
            syncServiceContainer.add(namespaceService, Namespace.class);

            InfraConfigService infraConfigService = mockFacadeInfraConfigService.build();
            doCallRealMethod().when(infraConfigService).configure(any());
            infraConfigService.configure(syncServiceContainer);

            InfraService infraService = mockFacadeH2DbService
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
            SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer.build();
            Namespace namespaceService = applicationContext.getBean(Namespace.class);
            namespaceService.setNamespace(namespace);
            syncServiceContainer.add(namespaceService, Namespace.class);

            InfraConfigService infraConfigService = mockFacadeInfraConfigService.build();
            doCallRealMethod().when(infraConfigService).configure(any());
            infraConfigService.configure(syncServiceContainer);

            InfraService infraService = mockFacadeH2DbService
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
            SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer.build();
            Namespace namespaceService = applicationContext.getBean(Namespace.class);
            namespaceService.setNamespace(namespace);
            syncServiceContainer.add(namespaceService, Namespace.class);

            InfraConfigService infraConfigService = mockFacadeInfraConfigService
                    .getNitriteDataPath("/Users/aaggarwal/Documents/hagrid-releases/data/hagrid-3.7.0/some_database_file_here")
                    .getNitriteDatabaseType("file")
                    .build();
            doCallRealMethod().when(infraConfigService).configure(any());
            infraConfigService.configure(syncServiceContainer);

            InfraService infraService = mockFacadeH2DbService
                    .syncServiceContainer(syncServiceContainer)
                    .build();

            doCallRealMethod().when(infraService).configure(any(), any());
            doCallRealMethod().when(infraService).getProcessorQueue();
            doCallRealMethod().when(infraService).getPublisherList();
            doCallRealMethod().when(infraService).destroy();

            infraService.configure(syncServiceContainer, infraConfigService);


            InfraDbQueue processorQueue = infraService.getProcessorQueue();
            InfraDbList publisherList = infraService.getPublisherList();


            // Assert that non of them is null
            assertThat(processorQueue, is(Matchers.notNullValue()));
            assertThat(publisherList, is(Matchers.notNullValue()));

            // Now destroy the infra
            infraService.destroy();
            Path path = Paths.get("/Users/aaggarwal/Documents/hagrid-releases/data/hagrid-3.7.0/some_database_file_here"  + ".mv.db");
            assertThat(Files.exists(path), is(true));
        }


        @RepeatedTest(50)
        public void testConcurrentDeletionOfSameNamespaceWorksWithoutErrors() throws Exception {

            String namespace = UUID.randomUUID().toString();
            SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer.build();
            Namespace namespaceService = applicationContext.getBean(Namespace.class);
            namespaceService.setNamespace(namespace);
            syncServiceContainer.add(namespaceService, Namespace.class);

            InfraConfigService infraConfigService = mockFacadeInfraConfigService
                    .getNitriteDataPath("/Users/aaggarwal/Documents/hagrid-releases/data/hagrid-3.7.0/some_database_file_here")
                    .getNitriteDatabaseType("file")
                    .build();
            doCallRealMethod().when(infraConfigService).configure(any());
            infraConfigService.configure(syncServiceContainer);

            InfraService infraService = mockFacadeH2DbService
                    .syncServiceContainer(syncServiceContainer)
                    .build();

            doCallRealMethod().when(infraService).configure(any(), any());
            doCallRealMethod().when(infraService).getProcessorQueue();
            doCallRealMethod().when(infraService).getPublisherList();
            doCallRealMethod().when(infraService).destroy();

            infraService.configure(syncServiceContainer, infraConfigService);

            infraService.getProcessorQueue();
            infraService.getPublisherList();

            List<String> errors = new ArrayList<>();
            Thread th1 = new Thread(() -> {
                try {
                    infraService.destroy();
                } catch (Exception e) {
                    errors.add(e.getMessage());
                    throw new RuntimeException(e);
                }
            });
            Thread th2 = new Thread(() -> {
                try {
                    infraService.destroy();
                } catch (Exception e) {
                    errors.add(e.getMessage());
                    throw new RuntimeException(e);
                }
            });

            th1.start();
            th2.start();
            th1.join();
            th2.join();

            assertThat(errors, is(Matchers.empty()));
        }
    }
//
    @Nested
    public class DifferentNameSpace{
//
        @Test
        public void testInfraServiceReturnDifferentProcessorQueueEverytime() throws Exception {

            String namespace = UUID.randomUUID().toString();
            SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer.build();
            Namespace namespaceService = applicationContext.getBean(Namespace.class);
            namespaceService.setNamespace(namespace);
            syncServiceContainer.add(namespaceService, Namespace.class);

            InfraConfigService infraConfigService = mockFacadeInfraConfigService.build();
            doCallRealMethod().when(infraConfigService).configure(any());
            infraConfigService.configure(syncServiceContainer);

            InfraService infraService = mockFacadeH2DbService
                    .syncServiceContainer(syncServiceContainer)
                    .build();

            doCallRealMethod().when(infraService).configure(any(), any());
            doCallRealMethod().when(infraService).getProcessorQueue();
            infraService.configure(syncServiceContainer, infraConfigService);

            InfraDbQueue processorQueue1 = infraService.getProcessorQueue();

            String namespace2 = UUID.randomUUID().toString();
            SyncServiceContainer syncServiceContainer2 = mockFacadeSyncServiceContainer.build();
            namespaceService = applicationContext.getBean(Namespace.class);
            namespaceService.setNamespace(namespace2);
            syncServiceContainer.add(namespaceService, Namespace.class);

            InfraConfigService infraConfigService2 = mockFacadeInfraConfigService.build();
            doCallRealMethod().when(infraConfigService2).configure(any());
            infraConfigService2.configure(syncServiceContainer2);
            InfraService infraService2 = mockFacadeH2DbService
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
            SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer.build();
            Namespace namespaceService = applicationContext.getBean(Namespace.class);
            namespaceService.setNamespace(namespace);
            syncServiceContainer.add(namespaceService, Namespace.class);

            InfraConfigService infraConfigService = mockFacadeInfraConfigService.build();
            doCallRealMethod().when(infraConfigService).configure(any());
            infraConfigService.configure(syncServiceContainer);

            InfraService infraService = mockFacadeH2DbService
                    .syncServiceContainer(syncServiceContainer)
                    .build();

            doCallRealMethod().when(infraService).configure(any(), any());
            doCallRealMethod().when(infraService).getPublisherList();
            infraService.configure(syncServiceContainer, infraConfigService);

            InfraDbList publisherList1 = infraService.getPublisherList();


            String namespace2 = UUID.randomUUID().toString();
            SyncServiceContainer syncServiceContainer2 = mockFacadeSyncServiceContainer.build();
            namespaceService = applicationContext.getBean(Namespace.class);
            namespaceService.setNamespace(namespace2);
            syncServiceContainer.add(namespaceService, Namespace.class);

            InfraConfigService infraConfigService2 = mockFacadeInfraConfigService.build();
            doCallRealMethod().when(infraConfigService2).configure(any());
            infraConfigService2.configure(syncServiceContainer2);

            InfraService infraService2 = mockFacadeH2DbService
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
            SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer.build();
            Namespace namespaceService = applicationContext.getBean(Namespace.class);
            namespaceService.setNamespace(namespace);
            syncServiceContainer.add(namespaceService, Namespace.class);
            InfraConfigService infraConfigService = mockFacadeInfraConfigService.build();
            doCallRealMethod().when(infraConfigService).configure(any());
            infraConfigService.configure(syncServiceContainer);

            InfraService infraService = mockFacadeH2DbService
                    .syncServiceContainer(syncServiceContainer)
                    .build();

            doCallRealMethod().when(infraService).configure(any(), any());
            doCallRealMethod().when(infraService).getKeyValue();
            infraService.configure(syncServiceContainer, infraConfigService);

            InfraDbKeyValue keyValue1 = infraService.getKeyValue();


            String namespace2 = UUID.randomUUID().toString();
            SyncServiceContainer syncServiceContainer2 = mockFacadeSyncServiceContainer.build();
            namespaceService = applicationContext.getBean(Namespace.class);
            namespaceService.setNamespace(namespace2);
            syncServiceContainer.add(namespaceService, Namespace.class);

            InfraConfigService infraConfigService2 = mockFacadeInfraConfigService.build();
            doCallRealMethod().when(infraConfigService2).configure(any());
            infraConfigService2.configure(syncServiceContainer2);

            InfraService infraService2 = mockFacadeH2DbService
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

        @Test
        public void testConcurrentDeletionOfDifferentNamespaceWorksWithoutErrors() throws Exception {

        }
    }
}
