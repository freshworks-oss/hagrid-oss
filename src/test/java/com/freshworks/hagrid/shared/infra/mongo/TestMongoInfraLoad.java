package com.freshworks.hagrid.shared.infra.mongo;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshworks.hagrid.main.assets.GenericAsset;
import com.freshworks.hagrid.shared.MockFacadeSyncServiceContainer;
import com.freshworks.hagrid.shared.SyncServiceContainer;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;

/**
 * Load test of the Mongo infra classes (MongoDbQueue, MongoDbList, MongoDbKeyValue) with N documents.
 * Needs the same profile gate as TestNitritePerformance (the profile name must contain ".performance."):
 *
 *   mvn test -Dspring.profiles.active=mongo.performance.run -Dtest=TestMongoInfraLoad -Dmongo.perf.size=500000
 *
 * Prints "[MONGO-PERF] ..." timing lines. Correctness is asserted; timings are only reported.
 * Properties: mongo.perf.size (default 500000), mongo.perf.batch (1000).
 */
@SpringBootTest
@EnabledIfSystemProperty(named = "spring.profiles.active", matches = ".*\\.performance\\..*")
public class TestMongoInfraLoad {

    private static final int N = Integer.getInteger("mongo.perf.size", 500_000);
    private static final int BATCH = Integer.getInteger("mongo.perf.batch", 1_000);
    private static final String GENERIC_CLASS = GenericAsset.class.getName();
    private static final String FILLER = "x".repeat(100);

    private static MongoClient mongoClient;

    @Autowired
    MockFacadeSyncServiceContainer mockFacadeSyncServiceContainer;

    private MongoDatabase db;
    private SyncServiceContainer syncServiceContainer;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SpelExpressionParser spelParser = new SpelExpressionParser();

    @BeforeAll
    static void connect() {
        mongoClient = MongoClients.create(MongoTestSupport.connectionString());
    }

    @AfterAll
    static void disconnect() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }

    @BeforeEach
    void setup() {
        db = mongoClient.getDatabase("test_perf_" + UUID.randomUUID().toString().replace("-", ""));
        syncServiceContainer = mockFacadeSyncServiceContainer.configure().build();
    }

    @AfterEach
    void tearDown() {
        db.drop();
    }

    private static void report(String label, long startNanos, long count) {
        long ms = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
        System.out.printf("[MONGO-PERF] %-45s %,9d docs %,9d ms %,9d docs/s%n", label, count, ms,
                ms == 0 ? count : count * 1000 / ms);
    }

    private MongoDbQueue newQueue() throws Exception {
        MongoDbQueue q = new MongoDbQueue();
        q.setQueue(db.getCollection("queue"));
        q.getQueue().createIndex(Indexes.ascending("queue_index"), new IndexOptions().unique(true));
        q.configure(syncServiceContainer);
        return q;
    }

    private MongoDbList newList() throws Exception {
        MongoDbList l = new MongoDbList();
        l.setList(db.getCollection("list"));
        l.getList().createIndex(Indexes.ascending("list_index"), new IndexOptions().unique(true));
        l.getList().createIndex(Indexes.ascending("value.created_at_ms"));
        l.configure(syncServiceContainer);
        return l;
    }

    private String queueItem(int seq) {
        return "{\"seq\": " + seq + ", \"id\": \"item_" + seq + "\", \"text\": \"" + FILLER + "\"}";
    }

    private String asset(String clazz, long createdAtMs, String name) {
        return "{\"clazz\":\"" + clazz + "\",\"created_at_ms\":" + createdAtMs + ",\"name\":\"" + name
                + "\",\"outputModel\":{\"name\":\"om\",\"data\":{\"text\":\"" + FILLER + "\"}}}";
    }

    private SpelExpression spel(String expr) {
        return (SpelExpression) spelParser.parseExpression(expr);
    }


    // ---------------------------------------------------------------- section 2: Mongo infra classes with N documents

    // ---------------------------------------------------------------- queue

    @Test
    @Timeout(value = 30, unit = TimeUnit.MINUTES)
    public void queueAddBatchesThenDrainKeepsFifoWithoutLossOrDuplicates() throws Exception {
        MongoDbQueue queue = newQueue();

        long t = System.nanoTime();
        for (int start = 0; start < N; start += BATCH) {
            List<String> batch = new ArrayList<>(BATCH);
            for (int i = start; i < Math.min(start + BATCH, N); i++) {
                batch.add(queueItem(i));
            }
            queue.add(batch);
        }
        report("queue add(List) batches of " + BATCH, t, N);

        assertThat(queue.size(), is((long) N));
        assertThat(queue.getQueue().countDocuments(), is((long) N));

        t = System.nanoTime();
        int expectedSeq = 0;
        while (!queue.isEmpty()) {
            List<String> polled = queue.poll(BATCH);
            assertTrue(!polled.isEmpty(), "poll(n) returned nothing while queue is not empty, at seq " + expectedSeq);
            for (String s : polled) {
                assertThat("FIFO order broken", objectMapper.readTree(s).get("seq").asInt(), is(expectedSeq));
                expectedSeq++;
            }
        }
        report("queue poll(" + BATCH + ") drain + order check", t, N);

        assertThat(expectedSeq, is(N));
        assertThat(queue.getPopIndex(), is((long) N));
        assertThat(queue.poll(), is((String) null));
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.MINUTES)
    public void queueConcurrentProducerAndConsumerLoseNothing() throws Exception {
        MongoDbQueue queue = newQueue();
        AtomicReference<Throwable> producerError = new AtomicReference<>();

        Thread producer = new Thread(() -> {
            try {
                for (int start = 0; start < N; start += BATCH) {
                    List<String> batch = new ArrayList<>(BATCH);
                    for (int i = start; i < Math.min(start + BATCH, N); i++) {
                        batch.add(queueItem(i));
                    }
                    queue.add(batch);
                }
            } catch (Throwable e) {
                producerError.set(e);
            } finally {
                try {
                    queue.removePublisher();
                } catch (Exception ignored) {
                }
            }
        });

        long t = System.nanoTime();
        producer.start();

        int expectedSeq = 0;
        while (queue.hasMoreData()) {
            for (String s : queue.poll(BATCH)) {
                assertThat("FIFO order broken", objectMapper.readTree(s).get("seq").asInt(), is(expectedSeq));
                expectedSeq++;
            }
        }
        producer.join();
        report("queue producer thread + consumer thread", t, N);

        assertThat("producer failed: " + producerError.get(), producerError.get() == null, is(true));
        assertThat(expectedSeq, is(N));
        assertThat(queue.getQueue().countDocuments(), is((long) N));
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.MINUTES)
    public void queueSingleAddsAndSinglePollsForFirstTenThousand() throws Exception {
        // add(String)/poll() do one round trip each, so only the first 10,000 are measured
        int n = Math.min(N, 10_000);
        MongoDbQueue queue = newQueue();

        long t = System.nanoTime();
        for (int i = 0; i < n; i++) {
            queue.add(queueItem(i));
        }
        report("queue add(String) one by one", t, n);

        t = System.nanoTime();
        for (int i = 0; i < n; i++) {
            assertThat(objectMapper.readTree(queue.poll()).get("seq").asInt(), is(i));
        }
        report("queue poll() one by one", t, n);
    }

    // ---------------------------------------------------------------- list

    @Test
    @Timeout(value = 30, unit = TimeUnit.MINUTES)
    public void listBulkAddThenFilterAssetAndPagination() throws Exception {
        MongoDbList list = newList();
        String rareClass = "com.freshworks.load.RareAsset";
        int rareEvery = 100;
        int expectedRare = 0;

        long t = System.nanoTime();
        for (int start = 0; start < N; start += BATCH) {
            List<String> batch = new ArrayList<>(BATCH);
            for (int i = start; i < Math.min(start + BATCH, N); i++) {
                if (i % rareEvery == 0) {
                    batch.add(asset(rareClass, i, "rare_" + i));
                    expectedRare++;
                } else {
                    batch.add(asset(GENERIC_CLASS, i, "user_" + i));
                }
            }
            list.addBulk(batch);
        }
        report("list addBulk batches of " + BATCH, t, N);

        long expectedGeneric = N - expectedRare;
        assertThat(list.size(), is((long) N));
        assertThat(list.getList().countDocuments(), is((long) N));

        // full scan of one class, sorted by created_at_ms; check order and count
        t = System.nanoTime();
        MongoDbCursor cursor = list.filterAsset(GenericAsset.class, null);
        assertThat(cursor.docSize(), is(expectedGeneric));
        long count = 0;
        long previous = -1;
        while (cursor.getDocumentCursor().hasNext()) {
            Document d = cursor.getDocumentCursor().next();
            long created = ((Number) ((Document) d.get("value")).get("created_at_ms")).longValue();
            assertTrue(created > previous, "created_at_ms order broken at " + created + " after " + previous);
            previous = created;
            count++;
        }
        cursor.getDocumentCursor().close();
        report("list filterAsset(GenericAsset) full scan+order", t, count);
        assertThat(count, is(expectedGeneric));

        // filter on a non-indexed field: Mongo has to scan the class's documents
        t = System.nanoTime();
        cursor = list.filterAsset(GenericAsset.class, spel("name == 'user_1'"));
        long hits = 0;
        while (cursor.getDocumentCursor().hasNext()) {
            cursor.getDocumentCursor().next();
            hits++;
        }
        cursor.getDocumentCursor().close();
        report("list filterAsset name == 'user_1' (no index)", t, N);
        assertThat(hits, is(1L));

        // paging over the whole list by list index
        t = System.nanoTime();
        long paged = 0;
        for (int start = 0; start < N; start += BATCH) {
            paged += list.get(start, BATCH).size();
        }
        report("list get(start, n) page through everything", t, paged);
        assertThat(paged, is((long) N));
    }

    // ---------------------------------------------------------------- key-value

    @Test
    @Timeout(value = 30, unit = TimeUnit.MINUTES)
    public void keyValueSetManyKeys() throws Exception {
        MongoDbKeyValue keyValue = new MongoDbKeyValue();
        keyValue.setKeyValue(db.getCollection("key_value"));
        keyValue.getKeyValue().createIndex(Indexes.ascending("key"));
        keyValue.configure(syncServiceContainer);

        long t = System.nanoTime();
        for (int i = 0; i < N; i++) {
            keyValue.set("key_" + i, "{\"v\": " + i + "}");
        }
        report("key-value set(key, value) one by one", t, N);

        assertThat(keyValue.getKeyValue().countDocuments(), is((long) N));
        assertThat(keyValue.size(), is((long) N));
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.MINUTES)
    public void keyValuePutListIntoOneKeyUntilDocumentLimit() throws Exception {
        // putList does $push into ONE document, and a Mongo document is capped at 16 MB.
        // This probes how far one key can grow and records what happens at the limit.
        MongoDbKeyValue keyValue = new MongoDbKeyValue();
        keyValue.setKeyValue(db.getCollection("key_value"));
        keyValue.getKeyValue().createIndex(Indexes.ascending("key"));
        keyValue.configure(syncServiceContainer);

        long t = System.nanoTime();
        int pushed = 0;
        String failure = null;
        try {
            for (int start = 0; start < N; start += BATCH) {
                List<String> batch = new ArrayList<>(BATCH);
                for (int i = start; i < Math.min(start + BATCH, N); i++) {
                    batch.add(queueItem(i));
                }
                keyValue.putList("one_big_key", batch);
                pushed += batch.size();
            }
        } catch (Exception e) {
            failure = e.getClass().getSimpleName() + ": " + e.getMessage();
        }
        report("key-value putList into one key", t, pushed);
        System.out.println("[MONGO-PERF] putList into one key stored " + pushed + " of " + N
                + (failure == null ? " (no limit hit)" : ", stopped by: " + failure));

        // whatever was reported as pushed must really be in the document
        Document doc = keyValue.getKeyValue().find().first();
        int stored = doc == null ? 0 : ((List<?>) doc.get("value")).size();
        assertThat(stored, is(pushed));
    }
}
