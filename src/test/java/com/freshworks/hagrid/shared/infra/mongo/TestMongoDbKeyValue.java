package com.freshworks.hagrid.shared.infra.mongo;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.freshworks.hagrid.shared.MockFacadeSyncServiceContainer;
import com.freshworks.hagrid.shared.SyncServiceContainer;
import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Indexes;

/**
 * Mongo twin of the Nitrite key-value behaviour. There is NO Nitrite key-value
 * test class in this repo (only MockFacadeNitritedbKeyValue, a mock helper), so
 * the "contract" is InfraDbKeyValue plus NitriteDbKeyValue's code. Tests in the
 * first block check behaviour that is the same in both stores; the second block
 * checks places where Mongo differs; the third covers FINDINGS.
 *
 * Several get/getList tests are FINDINGS: MongoDbKeyValue stores the value as a
 * BSON sub-document but reads it back casting to String, so reads fail.
 */
@SpringBootTest
@EnabledIfSystemProperty(named = "spring.profiles.active", matches = "unit")
public class TestMongoDbKeyValue {

    private static final String AMIT = "{\"name\": \"amit\"}";
    private static final String RAHUL = "{\"name\": \"rahul\"}";

    private static MongoClient mongoClient;

    @Autowired
    MockFacadeSyncServiceContainer mockFacadeSyncServiceContainer;

    @Autowired
    MockFacadeMongoDbKeyValue mockFacadeMongoDbKeyValue;

    private MongoDatabase db;
    private MongoDbKeyValue kv;
    private SyncServiceContainer syncServiceContainer;

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
    void setup() throws Exception {
        db = mongoClient.getDatabase("test_kv_" + UUID.randomUUID().toString().replace("-", ""));
        syncServiceContainer = mockFacadeSyncServiceContainer.configure().build();
        kv = new MongoDbKeyValue();
        kv.configure(syncServiceContainer);
        // same wiring as MongoService.getMongoDbKeyValue
        kv.setKeyValue(db.getCollection("key_value"));
        kv.getKeyValue().createIndex(Indexes.ascending("key"));
    }

    @AfterEach
    void tearDown() {
        db.drop();
    }

    /** Raw documents stored for an (already encoded) key, bypassing MongoDbKeyValue. */
    private List<Document> raw(String encodedKey) {
        return kv.getKeyValue().find(new Document("key", encodedKey)).into(new ArrayList<>());
    }

    // ------------------------------------------------ same behaviour as Nitrite (derived from contract)

    @Test
    public void testSetStoresOneDocumentPerKey() throws Exception {
        kv.set("user1", AMIT);

        List<Document> docs = raw("user1");
        assertThat(docs.size(), is(1));
        assertThat(((Document) docs.get(0).get("value")).getString("name"), is("amit"));
    }

    @Test
    public void testSetOverwritesExistingKey() throws Exception {
        kv.set("user1", AMIT);
        kv.set("user1", RAHUL);

        List<Document> docs = raw("user1");
        assertThat(docs.size(), is(1));
        assertThat(((Document) docs.get(0).get("value")).getString("name"), is("rahul"));
    }

    @Test
    public void testPutListStringAppends() throws Exception {
        kv.putList("users", AMIT);
        kv.putList("users", RAHUL);

        List<Document> docs = raw("users");
        assertThat(docs.size(), is(1));
        List<?> values = (List<?>) docs.get(0).get("value");
        assertThat(values.size(), is(2));
        assertThat(((Document) values.get(0)).getString("name"), is("amit"));
        assertThat(((Document) values.get(1)).getString("name"), is("rahul"));
    }

    @Test
    public void testPutListOfStringsAppendsInOrder() throws Exception {
        kv.putList("users", List.of(AMIT, RAHUL));
        kv.putList("users", List.of(AMIT));

        List<?> values = (List<?>) raw("users").get(0).get("value");
        assertThat(values.size(), is(3));
        assertThat(((Document) values.get(1)).getString("name"), is("rahul"));
    }

    @Test
    public void testGetListMissingKeyReturnsEmptyList() throws Exception {
        assertThat(kv.getList("nope"), is(empty()));
    }

    @Test
    public void testPutListEmptyListGivesEmptyResult() throws Exception {
        kv.putList("users", new ArrayList<String>());
        assertThat(kv.getList("users"), is(empty()));
    }

    @Test
    public void testDeleteRemovesEverything() throws Exception {
        kv.set("a", AMIT);
        kv.putList("b", RAHUL);
        kv.delete();

        assertThat(kv.getKeyValue().countDocuments(), is(0L));
        assertThat(kv.getList("b"), is(empty()));
    }

    @Test
    public void testConfigureDoesNotNeedAnyBean() throws Exception {
        // Nitrite configure() fetches NamespaceService + AnalyticsFactory; Mongo's is an empty method
        new MongoDbKeyValue().configure(null);
    }

    @Test
    public void testDotInKeyIsEncodedInStorage() throws Exception {
        kv.set("a.b.c", AMIT);

        assertThat(raw("aENCODE_DOTbENCODE_DOTc").size(), is(1));
        assertThat(raw("a.b.c").size(), is(0));
    }

    @Test
    public void testDotInValueIsEncodedInStorage() throws Exception {
        kv.set("k", "{\"host\": \"x.y.com\"}");

        Document value = (Document) raw("k").get(0).get("value");
        assertThat(value.getString("host"), is("xENCODE_DOTyENCODE_DOTcom"));
    }

    @Test
    public void testPutListDotInKeyAndValueEncoded() throws Exception {
        kv.putList("my.list", "{\"host\": \"x.y\"}");

        List<?> values = (List<?>) raw("myENCODE_DOTlist").get(0).get("value");
        assertThat(((Document) values.get(0)).getString("host"), is("xENCODE_DOTy"));
    }

    @Test
    public void testInvalidJsonValueIsRejected() {
        assertThrows(Exception.class, () -> kv.set("k", "not json"));
        assertThrows(Exception.class, () -> kv.putList("k", "not json"));
        assertThat(kv.getKeyValue().countDocuments(), is(0L));
    }

    @Test
    public void testLockIsReleasedAfterFailure() throws Exception {
        assertThrows(Exception.class, () -> kv.set("k", "not json"));
        assertThat(kv.getKeyAddLock().isHeldByCurrentThread(), is(false));
    }

    @Test
    public void testSpecialCharactersInKeyArePreserved() throws Exception {
        String key = "$weird key/é#1";
        kv.set(key, AMIT);
        assertThat(raw(key).size(), is(1));
    }

    @Test
    public void testConcurrentPutListLosesNothing() throws Exception {
        int threads = 4, perThread = 25;
        CountDownLatch start = new CountDownLatch(1);
        List<Thread> ts = new ArrayList<>();
        for (int t = 0; t < threads; t++) {
            Thread th = new Thread(() -> {
                try {
                    start.await();
                    for (int i = 0; i < perThread; i++) {
                        kv.putList("users", AMIT);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            ts.add(th);
            th.start();
        }
        start.countDown();
        for (Thread th : ts) {
            th.join();
        }
        assertThat(((List<?>) raw("users").get(0).get("value")).size(), is(threads * perThread));
    }

    // ------------------------------------------------ places where Mongo differs from Nitrite

    @Test
    public void testGetMissingKeyReturnsNull() throws Exception {
        // CONTRADICTS NITRITE: Nitrite get() does find(key).get(0) and throws IndexOutOfBoundsException
        assertThat(kv.get("nope"), is(nullValue()));
    }

    @Test
    public void testSizeCountsKeysNotEntries() throws Exception {
        // CONTRADICTS NITRITE: Nitrite size() is a counter incremented per inserted entry
        // (set on same key twice => 2, putList of 3 => 3). Mongo counts documents = distinct keys.
        assertThat(kv.size(), is(0L));
        kv.set("a", AMIT);
        kv.set("a", RAHUL);
        assertThat(kv.size(), is(1L));
        kv.putList("b", List.of(AMIT, RAHUL, AMIT));
        assertThat(kv.size(), is(2L));
    }

    @Test
    public void testSizeAfterDeleteIsZero() throws Exception {
        kv.set("a", AMIT);
        kv.delete();
        assertThat(kv.size(), is(0L));
    }

    @Test
    public void testPutListOnKeyCreatedBySetFails() throws Exception {
        // CONTRADICTS NITRITE: Nitrite just inserts another entry for the key.
        // Mongo $push on a sub-document (not an array) is rejected by the server.
        kv.set("k", AMIT);
        assertThrows(MongoException.class, () -> kv.putList("k", RAHUL));
    }

    @Test
    public void testSetOnKeyCreatedByPutListReplacesArray() throws Exception {
        // Same end result as Nitrite (set wipes earlier entries) but done by $set replacing the array
        kv.putList("k", List.of(AMIT, RAHUL));
        kv.set("k", AMIT);

        assertThat(raw("k").size(), is(1));
        assertThat(raw("k").get(0).get("value") instanceof Document, is(true));
    }

    @Test
    public void testDeleteDropsKeyIndexToo() throws Exception {
        // FINDING: delete() drops the whole collection, including the "key" index created at wiring time.
        // After delete() the same MongoDbKeyValue object keeps working but every find is a full scan.
        kv.delete();
        kv.set("a", AMIT);

        List<String> indexNames = new ArrayList<>();
        kv.getKeyValue().listIndexes().forEach(i -> indexNames.add(i.getString("name")));
        assertThat(indexNames.contains("key_1"), is(false));
    }

    @Test
    public void testKeyIndexExistsAfterWiring() {
        List<String> indexNames = new ArrayList<>();
        kv.getKeyValue().listIndexes().forEach(i -> indexNames.add(i.getString("name")));
        assertThat(indexNames, hasItem("key_1"));
    }

    @Test
    public void testKeyIndexIsNotUnique() throws Exception {
        // Nothing stops two documents with the same key if someone inserts directly
        kv.getKeyValue().insertOne(new Document("key", "dup").append("value", new Document()));
        kv.getKeyValue().insertOne(new Document("key", "dup").append("value", new Document()));
        assertThat(raw("dup").size(), is(2));
    }

    // ------------------------------------------------ FINDINGS: reads are broken

    @Test
    public void testGetAfterSetReturnsJson() throws Exception {
        // set() stores a BSON Document; get() serialises it back to JSON text with the ObjectMapper.
        kv.set("user1", AMIT);
        assertThat(kv.get("user1").replace(" ", ""), is("{\"name\":\"amit\"}"));
    }

    @Test
    public void testGetListAfterPutListReturnsJsonStrings() throws Exception {
        // putList stores an array of Documents; getList serialises each one back to a JSON string (the join parses them).
        kv.putList("users", List.of(AMIT, RAHUL));
        List<String> users = kv.getList("users");
        assertThat(users.size(), is(2));
        assertThat(users.get(0).replace(" ", ""), is("{\"name\":\"amit\"}"));
        assertThat(users.get(1).replace(" ", ""), is("{\"name\":\"rahul\"}"));
    }

    @Test
    public void testGetListOnKeyCreatedBySetThrowsToday() throws Exception {
        // FINDING: value is a Document, getList casts it to ArrayList.
        kv.set("k", AMIT);
        assertThrows(ClassCastException.class, () -> kv.getList("k"));
    }

    @Test
    public void testGetOnKeyCreatedByPutListReturnsJsonArray() throws Exception {
        // value is an array, so get() returns it as a JSON array text.
        kv.putList("k", AMIT);
        assertThat(kv.get("k").replace(" ", ""), is("[{\"name\":\"amit\"}]"));
    }

    @Test
    public void testSetGetRoundTrip() throws Exception {
        kv.set("user1", AMIT);
        assertThat(kv.get("user1").replace(" ", ""), is("{\"name\":\"amit\"}"));
    }

    @Test
    public void testPutListGetListRoundTrip() throws Exception {
        kv.putList("users", List.of(AMIT, RAHUL));
        kv.putList("users", "{\"name\": \"x.y\"}");
        assertThat(kv.getList("users").size(), is(3));
        assertThat(kv.getList("users").get(2).replace(" ", ""), is("{\"name\":\"x.y\"}"));
    }

    @Test
    public void testDotRoundTrip() throws Exception {
        kv.set("a.b", "{\"host\": \"x.y\"}");
        assertThat(kv.get("a.b").contains("x.y"), is(true));
    }

    // ------------------------------------------------ FINDINGS: encoding limits (same limit exists in Nitrite)

    @Test
    public void testDecimalNumberInValueIsRejectedToday() {
        // FINDING: every '.' in the JSON text is replaced with ENCODE_DOT, including the one inside 1.5,
        // so the text becomes {"price": 1ENCODE_DOT5} which is not valid JSON. Nitrite has the same limit
        // (Jackson fails on the encoded text), so this is not a Mongo-only regression.
        assertThrows(Exception.class, () -> kv.set("k", "{\"price\": 1.5}"));
    }

    @Test
    public void testLiteralEncodeDotInKeyCollidesWithDot() throws Exception {
        // FINDING: "a.b" and "aENCODE_DOTb" map to the same stored key; the second set overwrites the first.
        kv.set("a.b", AMIT);
        kv.set("aENCODE_DOTb", RAHUL);
        assertThat(raw("aENCODE_DOTb").size(), is(1));
        assertThat(((Document) raw("aENCODE_DOTb").get(0).get("value")).getString("name"), is("rahul"));
    }

    // ---------------------------------------------------------------- MockFacadeMongoDbKeyValue (mirror of MockFacadeNitritedbKeyValue)

    @Test
    public void testMockFacadeMongoDbKeyValueServesConfiguredAnswers() throws Exception {
        MongoDbKeyValue facadeKeyValue = mockFacadeMongoDbKeyValue.configure().build();

        facadeKeyValue.set("k", "{\"v\":1}");
        facadeKeyValue.putList("k", new java.util.ArrayList<String>());
        facadeKeyValue.putList("k", "{\"v\":1}");
        assertThat(facadeKeyValue.get("k"), is("{\"name\":\"amit\"}"));
        assertThat(facadeKeyValue.getList("k"), org.hamcrest.Matchers.contains("{\"name\":\"amit\"}", "{\"name\":\"rahul\"}", "{\"name\":\"deepak\"}"));
    }
}
