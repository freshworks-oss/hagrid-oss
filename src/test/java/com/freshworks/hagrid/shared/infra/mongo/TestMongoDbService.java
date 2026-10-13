package com.freshworks.hagrid.shared.infra.mongo;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.freshworks.hagrid.shared.MockFacadeSyncServiceContainer;
import com.freshworks.hagrid.shared.NamespaceService;
import com.freshworks.hagrid.shared.SyncServiceContainer;
import com.freshworks.hagrid.shared.infra.InfraConfigService;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * Mongo twin of TestNitriteDbService. Section 1 = Nitrite tests with the same
 * assertions, section 2 = gaps / findings the Nitrite tests never checked.
 *
 * Every test uses its own namespace (= its own Mongo database).
 * MongoService is built by hand (no Mockito spy) with a per-class MongoClientFactory,
 * so the Spring singleton MongoClientFactory is never touched.
 */
@SpringBootTest
@EnabledIfSystemProperty(named = "spring.profiles.active", matches = "unit")
public class TestMongoDbService {

    private static String connectionString;
    private static MongoClient inspector;               // our own client to look into the database
    private static final MongoClientFactory factory = new MongoClientFactory();

    @Autowired
    MockFacadeSyncServiceContainer mockFacadeSyncServiceContainer;

    @Autowired
    MockFacadeMongoDbService mockFacadeMongoDbService;

    @Autowired
    MeterRegistry meterRegistry;

    private final List<String> namespaces = new ArrayList<>();
    private final List<MongoService> services = new ArrayList<>();

    @BeforeAll
    static void connect() {
        connectionString = MongoTestSupport.connectionString();
        inspector = MongoClients.create(connectionString);
    }

    @AfterAll
    static void disconnect() {
        if (factory.getMongoClient() != null) {
            factory.getMongoClient().close();
        }
        if (inspector != null) {
            inspector.close();
        }
    }

    @BeforeEach
    void setup() {
        namespaces.clear();
        services.clear();
    }

    @AfterEach
    void tearDown() {
        for (String ns : namespaces) {
            inspector.getDatabase(ns).drop();
        }
    }

    // ---------------------------------------------------------------- helpers

    private String uniqueNamespace() {
        String ns = "test_svc_" + UUID.randomUUID().toString().replace("-", "");
        namespaces.add(ns);
        return ns;
    }

    private MongoService service(String namespace) throws Exception {
        NamespaceService namespaceService = new NamespaceService();
        namespaceService.setNamespace(namespace);

        SyncServiceContainer container = mockFacadeSyncServiceContainer.configure().build();
        container.add(namespaceService, NamespaceService.class);
        container.add(factory, MongoClientFactory.class);

        InfraConfigService infraConfigService = mock(InfraConfigService.class);
        when(infraConfigService.getMongoConnectionString()).thenReturn(connectionString);

        MongoService service = new MongoService();
        service.configure(container, infraConfigService);
        services.add(service);
        return service;
    }

    private MongoService service() throws Exception {
        return service(uniqueNamespace());
    }

    private String unique(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }

    /** index name -> index document, e.g. "queue_index_1" -> {key:{queue_index:1}, unique:true} */
    private List<Document> indexes(String namespace, String collection) {
        return inspector.getDatabase(namespace).getCollection(collection).listIndexes().into(new ArrayList<>());
    }

    private List<String> indexNames(String namespace, String collection) {
        List<String> names = new ArrayList<>();
        for (Document d : indexes(namespace, collection)) {
            names.add(d.getString("name"));
        }
        return names;
    }

    private Document index(String namespace, String collection, String name) {
        for (Document d : indexes(namespace, collection)) {
            if (name.equals(d.getString("name"))) {
                return d;
            }
        }
        return null;
    }

    private boolean databaseExists(String namespace) {
        return inspector.listDatabaseNames().into(new ArrayList<>()).contains(namespace);
    }

    // ================================================================ 1. COPIED from TestNitriteDbService

    @Test
    public void testInfraServiceReturnSameProcessorQueueEverytime() throws Exception {
        MongoService infraService = service();

        assertThat(infraService.getProcessorQueue(), is(sameInstance(infraService.getProcessorQueue())));
    }

    @Test
    public void testInfraServiceReturnSamePublisherListEverytime() throws Exception {
        MongoService infraService = service();

        assertThat(infraService.getPublisherList(), is(sameInstance(infraService.getPublisherList())));
    }

    @Test
    public void testInfraServiceReturnSameKeyValueListEverytime() throws Exception {
        MongoService infraService = service();

        assertThat(infraService.getKeyValue(), is(sameInstance(infraService.getKeyValue())));
    }

    @Test
    public void testInfraServiceReturnsSameNamespaceServiceEverytime() throws Exception {
        MongoService infraService = service();

        assertThat(infraService.getNamespace(), is(infraService.getNamespace()));
    }

    @Test
    public void testInfraServiceReturnsSameListEverytime() throws Exception {
        MongoService infraService = service();

        assertThat(infraService.getInfraDbList("some_list_name"),
                is(sameInstance(infraService.getInfraDbList("some_list_name"))));
    }

    @Test
    public void testInfraServiceReturnsDifferentListForDifferentNameEverytime() throws Exception {
        MongoService infraService = service();

        assertThat(infraService.getInfraDbList("some_list_name"),
                is(not(sameInstance(infraService.getInfraDbList("some_another_list_name")))));
    }

    @RepeatedTest(20)
    public void testConcurrentDeletionOfSameNamespaceWorksWithoutErrors() throws Exception {
        MongoService infraService = service();
        infraService.getProcessorQueue();
        infraService.getPublisherList();

        List<String> errors = Collections.synchronizedList(new ArrayList<>());
        Runnable destroy = () -> {
            try {
                infraService.destroy();
            } catch (Exception e) {
                errors.add(e.getMessage());
            }
        };
        Thread th1 = new Thread(destroy);
        Thread th2 = new Thread(destroy);
        th1.start();
        th2.start();
        th1.join();
        th2.join();

        assertThat(errors, is(empty()));
    }

    @Test
    public void testConcurrentDeletionOfDifferentNamespaceWorksWithoutErrors() throws Exception {
        MongoService service1 = service();
        MongoService service2 = service();
        service1.getProcessorQueue();
        service2.getProcessorQueue();

        List<String> errors = Collections.synchronizedList(new ArrayList<>());
        Thread th1 = new Thread(() -> {
            try {
                service1.destroy();
            } catch (Exception e) {
                errors.add(e.getMessage());
            }
        });
        Thread th2 = new Thread(() -> {
            try {
                service2.destroy();
            } catch (Exception e) {
                errors.add(e.getMessage());
            }
        });
        th1.start();
        th2.start();
        th1.join();
        th2.join();

        assertThat(errors, is(empty()));
    }

    @Test
    public void testInfraServiceReturnDifferentProcessorQueueEverytime() throws Exception {
        MongoService service1 = service();
        MongoService service2 = service();

        assertThat(service1.getProcessorQueue(), is(not(sameInstance(service2.getProcessorQueue()))));
    }

    @Test
    public void testInfraServiceReturnDifferentPublisherListEverytime() throws Exception {
        MongoService service1 = service();
        MongoService service2 = service();

        assertThat(service1.getPublisherList(), is(not(sameInstance(service2.getPublisherList()))));
    }

    @Test
    public void testInfraServiceReturnDifferentKeyValueListEverytime() throws Exception {
        MongoService service1 = service();
        MongoService service2 = service();

        assertThat(service1.getKeyValue(), is(not(sameInstance(service2.getKeyValue()))));
    }

    @Test
    public void testDifferentNamespacesAreIsolated() throws Exception {
        String ns1 = uniqueNamespace();
        String ns2 = uniqueNamespace();
        MongoService service1 = service(ns1);
        MongoService service2 = service(ns2);

        service1.getPublisherList().add("{\"name\": \"amit\"}");
        service1.getKeyValue().set("k", "{\"v\": 1}");

        assertThat(service2.getPublisherList().size(), is(0L));
        assertThat(service2.getKeyValue().get("k"), is(nullValue()));
        assertThat(service1.getPublisherList().size(), is(1L));

        // destroying one namespace leaves the other alone
        service1.destroy();
        service2.getPublisherList().add("{\"name\": \"rahul\"}");
        assertThat(service2.getPublisherList().size(), is(1L));
        assertThat(databaseExists(ns1), is(false));
        assertThat(databaseExists(ns2), is(true));
    }

    @Test
    public void testDestroyRemovesData() throws Exception {
        String ns = uniqueNamespace();
        MongoService infraService = service(ns);
        infraService.getPublisherList().add("{\"name\": \"amit\"}");
        infraService.getProcessorQueue().add("{\"name\": \"amit\"}");
        infraService.getKeyValue().set("k", "{\"v\": 1}");
        assertThat(databaseExists(ns), is(true));

        infraService.destroy();

        assertThat(databaseExists(ns), is(false));
        assertThat(infraService.persistentQueueSingletonMap.containsKey(ns), is(false));
        assertThat(infraService.persistentListSingletonMap.containsKey(ns), is(false));
        assertThat(infraService.persistentKeyValueSingletonMap.containsKey(ns), is(false));
    }

    @Test
    public void testConcurrentGetCallsReturnSingleton() throws Exception {
        MongoService infraService = service();
        int threads = 16;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            CountDownLatch start = new CountDownLatch(1);
            List<Future<Object[]>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                futures.add(pool.submit((Callable<Object[]>) () -> {
                    start.await();
                    return new Object[] {
                            infraService.getProcessorQueue(),
                            infraService.getPublisherList(),
                            infraService.getKeyValue(),
                            infraService.getInfraDbList("shared_list")
                    };
                }));
            }
            start.countDown();
            Object[] first = futures.get(0).get();
            for (Future<Object[]> f : futures) {
                Object[] r = f.get();
                for (int i = 0; i < first.length; i++) {
                    assertThat(r[i], is(sameInstance(first[i])));
                }
            }
        } finally {
            pool.shutdownNow();
        }
    }

    // ================================================================ 2. NEW gap tests

    // ---- configure()

    @Test
    public void testConfigureWiresNamespaceClientAndAnalytics() throws Exception {
        String ns = uniqueNamespace();
        MongoService infraService = service(ns);

        assertThat(infraService.getNamespace(), is(ns));
        assertThat(infraService.mongoClient, is(sameInstance(factory.getMongoClient())));
        assertThat(infraService.analyticsService, is(notNullValue()));
        assertThat(infraService.syncServiceContainer, is(notNullValue()));
        assertThat(infraService.infraConfigService, is(notNullValue()));
    }

    @Test
    public void testConfigureUsesMongoClientFactoryFromContainer() throws Exception {
        MongoService a = service();
        MongoService b = service();

        // same factory -> same client, not one client per MongoService
        assertThat(a.mongoClient, is(sameInstance(b.mongoClient)));
    }

    // ---- getters return correct types and collections

    @Test
    public void testGetProcessorQueueReturnsMongoDbQueueOnProcessorCollection() throws Exception {
        MongoDbQueue queue = service().getProcessorQueue();

        assertThat(queue, is(instanceOf(MongoDbQueue.class)));
        assertThat(queue.getQueue().getNamespace().getCollectionName(), is("processor"));
    }

    @Test
    public void testGetPublisherListReturnsMongoDbListOnPublisherListCollection() throws Exception {
        MongoDbList list = service().getPublisherList();

        assertThat(list, is(instanceOf(MongoDbList.class)));
        assertThat(list.getList().getNamespace().getCollectionName(), is("publisher_list"));
    }

    @Test
    public void testGetKeyValueReturnsMongoDbKeyValueOnKeyValueCollection() throws Exception {
        MongoDbKeyValue kv = service().getKeyValue();

        assertThat(kv, is(instanceOf(MongoDbKeyValue.class)));
        assertThat(kv.getKeyValue().getNamespace().getCollectionName(), is("key_value"));
    }

    @Test
    public void testGetInfraDbListReturnsMongoDbListOnGivenName() throws Exception {
        MongoDbList list = service().getInfraDbList("my_list");

        assertThat(list, is(instanceOf(MongoDbList.class)));
        assertThat(list.getList().getNamespace().getCollectionName(), is("my_list"));
    }

    @Test
    public void testCollectionsLiveInTheNamespaceDatabase() throws Exception {
        String ns = uniqueNamespace();
        MongoService infraService = service(ns);

        assertThat(infraService.getProcessorQueue().getQueue().getNamespace().getDatabaseName(), is(ns));
        assertThat(infraService.getKeyValue().getKeyValue().getNamespace().getDatabaseName(), is(ns));
        assertThat(infraService.getPublisherList().getList().getNamespace().getDatabaseName(), is(ns));
    }

    // ---- indexes

    @Test
    public void testQueueGetsUniqueQueueIndex() throws Exception {
        String ns = uniqueNamespace();
        service(ns).getProcessorQueue();

        Document idx = index(ns, "processor", "queue_index_1");
        assertThat(idx, is(notNullValue()));
        assertThat(idx.get("unique"), is(true));
    }

    @Test
    public void testListGetsUniqueListIndex() throws Exception {
        String ns = uniqueNamespace();
        service(ns).getInfraDbList("a_list");

        Document idx = index(ns, "a_list", "list_index_1");
        assertThat(idx, is(notNullValue()));
        assertThat(idx.get("unique"), is(true));
    }

    @Test
    public void testKeyValueGetsNonUniqueKeyIndex() throws Exception {
        String ns = uniqueNamespace();
        service(ns).getKeyValue();

        Document idx = index(ns, "key_value", "key_1");
        assertThat(idx, is(notNullValue()));
        // FINDING: key index is NOT unique, set() relies on upsert only, nothing stops duplicate keys
        assertThat(idx.get("unique"), is(nullValue()));
    }

    @Test
    public void testSecondListInNamespaceGetsCreatedAtMsIndex() throws Exception {
        String ns = uniqueNamespace();
        MongoService infraService = service(ns);
        infraService.getInfraDbList("first_list");
        infraService.getInfraDbList("second_list");

        assertThat(indexNames(ns, "second_list"), containsInAnyOrder("_id_", "list_index_1", "value.created_at_ms_1"));
    }

    @Test
    public void testFirstListInNamespaceMissesCreatedAtMsIndex() throws Exception {
        String ns = uniqueNamespace();
        MongoService infraService = service(ns);
        infraService.getPublisherList();   // normally the first list of a namespace

        // FINDING: first list in a namespace goes through the "else" branch of getMongoDbList,
        // which only creates list_index. The value.created_at_ms index is created only
        // when the namespace already had a list (MongoService.java, middle branch).
        assertThat(indexNames(ns, "publisher_list"), containsInAnyOrder("_id_", "list_index_1"));
    }

    @Test
    @Disabled("FINDING: first list in a namespace does not get the value.created_at_ms index; enable once the else-branch in getMongoDbList creates it")
    public void testEveryListGetsCreatedAtMsIndex() throws Exception {
        String ns = uniqueNamespace();
        MongoService infraService = service(ns);
        infraService.getPublisherList();

        assertThat(indexNames(ns, "publisher_list"), hasItem("value.created_at_ms_1"));
    }

    @Test
    public void testIndexesSurviveRepeatedGetCalls() throws Exception {
        String ns = uniqueNamespace();
        MongoService infraService = service(ns);
        infraService.getProcessorQueue();
        infraService.getProcessorQueue();

        assertThat(indexNames(ns, "processor"), containsInAnyOrder("_id_", "queue_index_1"));
    }

    // ---- gauges

    private Double gauge(String name) {
        io.micrometer.core.instrument.Gauge g = meterRegistry.find(name).gauge();
        return g == null ? null : g.value();
    }

    @Test
    public void testQueueAndListRegisterSizeGauge() throws Exception {
        MongoService infraService = service();
        String listName = unique("gauge_list");
        MongoDbList list = infraService.getInfraDbList(listName);          // first list: "else" branch
        String list2Name = unique("gauge_list");
        MongoDbList list2 = infraService.getInfraDbList(list2Name);       // second list: middle branch

        list.add("{\"a\": 1}");
        list2.add("{\"a\": 1}");
        list2.add("{\"a\": 2}");

        assertThat(gauge(listName + ".size"), is(1.0));
        assertThat(gauge(list2Name + ".size"), is(2.0));
    }

    @Test
    public void testKeyValueRegistersNoSizeGauge() throws Exception {
        MongoService infraService = service();
        infraService.getKeyValue();

        // FINDING: NitriteService registers a size gauge for key_value too; MongoService never does
        // (neither in the first nor the second branch). Queues and lists do.
        assertThat(gauge("key_value.size"), is(nullValue()));
    }

    @Test
    public void testGaugeIsNamedWithoutNamespaceSoNamespacesShareIt() throws Exception {
        MongoService service1 = service();
        MongoService service2 = service();
        String listName = unique("shared_gauge");
        MongoDbList list1 = service1.getInfraDbList(listName);
        MongoDbList list2 = service2.getInfraDbList(listName);
        list2.add("{\"a\": 1}");
        list2.add("{\"a\": 2}");

        // FINDING: gauge name is "<collection>.size" with no namespace tag (Nitrite used
        // "infra.collection.size" + name tag). Micrometer keeps the FIRST registration, so the
        // second namespace's list size is invisible: gauge reports namespace 1 (size 0), not 2.
        assertThat(list1.size(), is(0L));
        assertThat(gauge(listName + ".size"), is(0.0));
    }

    @Test
    public void testQueueTimerNameIsNullExecutionTime() throws Exception {
        MongoDbQueue queue = service().getProcessorQueue();

        // FINDING: see the @Disabled testExecutionTimerNameContainsQueueName in TestMongoDbQueue.
        // MongoService calls configure() before setQueue(), so queue is still null when the name is built.
        assertThat(queue.getTimer().getId().getName(), is("null.execution.time"));
    }

    // ---- lifecycle: destroy / deleteDb / get-before-configure

    @Test
    public void testDestroyTwiceDoesNotFail() throws Exception {
        MongoService infraService = service();
        infraService.getProcessorQueue();

        infraService.destroy();
        infraService.destroy();
    }

    @Test
    public void testDestroyWithoutAnyGetDoesNotFail() throws Exception {
        service().destroy();
    }

    @Test
    public void testGetAfterDestroyRecreatesFreshObjects() throws Exception {
        String ns = uniqueNamespace();
        MongoService infraService = service(ns);
        String listName = unique("recreate_list");
        MongoDbList before = infraService.getInfraDbList(listName);
        before.add("{\"a\": 1}");
        MongoDbQueue queueBefore = infraService.getProcessorQueue();
        queueBefore.add("{\"a\": 1}");

        infraService.destroy();

        MongoDbList after = infraService.getInfraDbList(listName);
        MongoDbQueue queueAfter = infraService.getProcessorQueue();
        assertThat(after, is(not(sameInstance(before))));
        assertThat(queueAfter, is(not(sameInstance(queueBefore))));
        assertThat(after.size(), is(0L));
        assertThat(queueAfter.size(), is(0L));
        // unique indexes are created again on the fresh database
        assertThat(index(ns, "processor", "queue_index_1"), is(notNullValue()));
        // and the new objects work from index 0
        after.add("{\"a\": 2}");
        assertThat(after.size(), is(1L));
    }

    @Test
    public void testGaugeStillPointsToOldObjectAfterDestroyAndRecreate() throws Exception {
        MongoService infraService = service();
        String listName = unique("regauge_list");
        MongoDbList before = infraService.getInfraDbList(listName);
        before.add("{\"a\": 1}");
        infraService.destroy();                                           // drops data, old object still referenced by us
        MongoDbList after = infraService.getInfraDbList(listName);
        after.add("{\"a\": 1}");
        after.add("{\"a\": 2}");

        // FINDING: no error on re-registration (AnalyticsService.registeredGauge is never filled), but
        // Micrometer returns the original gauge, which still reads the OLD list object.
        // Once the old object is garbage collected the gauge reports NaN forever.
        assertThat(after.size(), is(2L));
        assertThat(gauge(listName + ".size"), is((double) before.size()));
    }

    @Test
    public void testDeleteDbDropsDatabaseButKeepsCachedObjects() throws Exception {
        String ns = uniqueNamespace();
        MongoService infraService = service(ns);
        MongoDbQueue queue = infraService.getProcessorQueue();
        queue.add("{\"a\": 1}");
        assertThat(databaseExists(ns), is(true));

        infraService.deleteDb();

        assertThat(databaseExists(ns), is(false));
        // FINDING: unlike destroy(), deleteDb() leaves the singleton maps alone, so the stale queue
        // (with its in-memory queueIndex counter) is handed out again and its unique index is gone.
        assertThat(infraService.getProcessorQueue(), is(sameInstance(queue)));
        queue.add("{\"a\": 2}");
        assertThat(indexNames(ns, "processor"), contains("_id_"));
    }

    @Test
    public void testGetBeforeConfigureThrowsNullPointerException() throws Exception {
        MongoService notConfigured = new MongoService();

        assertThat(notConfigured.namespace, is(nullValue()));
        assertThrows(NullPointerException.class, notConfigured::getProcessorQueue);
        assertThrows(NullPointerException.class, notConfigured::getPublisherList);
        assertThrows(NullPointerException.class, notConfigured::getKeyValue);
        assertThrows(NullPointerException.class, () -> notConfigured.getInfraDbList("x"));
    }

    @Test
    public void testDestroyAndDeleteDbBeforeConfigureThrowNullPointerException() throws Exception {
        MongoService notConfigured = new MongoService();

        assertThat(notConfigured.getNamespace(), is(nullValue()));
        assertThrows(NullPointerException.class, notConfigured::destroy);
        assertThrows(NullPointerException.class, notConfigured::deleteDb);
        // the lock is released in finally, so a second call fails the same way instead of hanging
        assertThrows(NullPointerException.class, notConfigured::destroy);
    }

    @Test
    public void testSingletonMapsArePerInstanceNotStatic() throws Exception {
        String ns = uniqueNamespace();
        MongoService first = service(ns);
        MongoService second = service(ns);

        // FINDING: NitriteService keeps its singleton maps in static fields, so two NitriteService
        // objects on the same namespace share queues. MongoService keeps them per instance, so
        // two instances on the same namespace get two different Java objects over one collection
        // (and two separate in-memory index counters).
        assertThat(first.getProcessorQueue(), is(not(sameInstance(second.getProcessorQueue()))));
    }

    // ---------------------------------------------------------------- MockFacadeMongoDbService (mirror of MockFacadeNitriteDbService)

    @Test
    public void testMockFacadeMongoDbServiceServesConfiguredAnswers() throws Exception {
        com.freshworks.hagrid.shared.infra.InfraService facadeService = mockFacadeMongoDbService.configure().build();

        facadeService.configure(null, null);          // stubbed: does nothing, no Mongo needed
        assertThat(facadeService.getNamespace(), is("some_dummy_namespace"));
        assertThat(facadeService.getProcessorQueue(), org.hamcrest.Matchers.notNullValue());
        assertThat(facadeService.getPublisherList(), org.hamcrest.Matchers.notNullValue());
        assertThat(facadeService.getKeyValue(), org.hamcrest.Matchers.notNullValue());
        assertThat(facadeService.getInfraDbList("any_list"), org.hamcrest.Matchers.notNullValue());
    }
}
