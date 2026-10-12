package com.freshworks.hagrid.shared.infra.mongo;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.freshworks.hagrid.main.assets.GenericAsset;
import com.freshworks.hagrid.shared.MockFacadeSyncServiceContainer;
import com.freshworks.hagrid.shared.SyncServiceContainer;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;

/**
 * Mongo twin of TestNitriteDbList. The first block is the Nitrite test set with the
 * same assertions; the rest covers gaps the Nitrite tests never checked.
 * Findings about the Mongo code are marked "FINDING:".
 */
@SpringBootTest
@EnabledIfSystemProperty(named = "spring.profiles.active", matches = "unit")
public class TestMongoDbList {

    private static final String AMIT = "{\"name\": \"amit\"}";
    private static final String RAHUL = "{\"name\": \"rahul\"}";
    private static final String DEEPAK = "{\"name\": \"deepak\"}";
    private static final String PRAVEEN = "{\"name\": \"praveen\"}";
    private static final String GENERIC_CLASS = GenericAsset.class.getName();

    private static MongoClient mongoClient;

    @Autowired
    MockFacadeSyncServiceContainer mockFacadeSyncServiceContainer;

    @Autowired
    MockFacadeMongoDbList mockFacadeMongoDbList;

    private MongoDatabase db;
    private MongoDbList list;
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
    void setup() throws Exception {
        db = mongoClient.getDatabase("test_list_" + UUID.randomUUID().toString().replace("-", ""));
        syncServiceContainer = mockFacadeSyncServiceContainer.configure().build();
        list = newList("some_name");
    }

    @AfterEach
    void tearDown() {
        db.drop();
    }

    /** Same wiring MongoService.getMongoDbList does: collection + unique list_index + created_at_ms index. */
    private MongoDbList newList(String name) throws Exception {
        MongoDbList l = new MongoDbList();
        l.setList(db.getCollection(name));
        l.getList().createIndex(Indexes.ascending("list_index"), new IndexOptions().unique(true));
        l.getList().createIndex(Indexes.ascending("value.created_at_ms"));
        l.configure(syncServiceContainer);
        return l;
    }

    private SpelExpression spel(String expr) {
        return (SpelExpression) spelParser.parseExpression(expr);
    }

    /** A stored "asset": clazz + created_at_ms + outputModel, like GenericAsset JSON. */
    private String asset(String clazz, long createdAtMs, String name, String modelName, String dataJson) {
        return "{\"clazz\":\"" + clazz + "\",\"created_at_ms\":" + createdAtMs + ",\"name\":\"" + name
                + "\",\"outputModel\":{\"name\":\"" + modelName + "\",\"data\":" + dataJson + "}}";
    }

    private String generic(long createdAtMs, String name, String modelName, String dataJson) {
        return asset(GENERIC_CLASS, createdAtMs, name, modelName, dataJson);
    }

    /** Drains the raw cursor and returns the value.name of every document, in cursor order. */
    private List<String> names(MongoDbCursor cursor) {
        List<String> out = new ArrayList<>();
        while (cursor.getDocumentCursor().hasNext()) {
            Document d = cursor.getDocumentCursor().next();
            out.add(((Document) d.get("value")).getString("name"));
        }
        return out;
    }

    // ---------------------------------------------------------------- copied from TestNitriteDbList

    @Test
    public void testAddMethod() throws Exception {
        list.add(AMIT);
        assertThat(list.getListIndex().get(), is(1L));
        list.delete();
    }

    @Test
    public void testAddListMethod() throws Exception {
        list.add(new ArrayList<>(List.of(AMIT, RAHUL)));
        assertThat(list.getListIndex().get(), is(2L));
        list.delete();
    }

    @Test
    public void testAddBulkMethod() throws Exception {
        ArrayList<String> items = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            items.add("{\"name\": \"" + i + " \"}");
        }
        Long count = list.addBulk(items);
        assertThat(count, is(100L));
        list.delete();
    }

    @Test
    public void testAddAndGetIndexMethod() throws Exception {
        Long index = list.addAndGetIndex(AMIT);
        assertThat(index, is(0L));
        list.delete();
    }

    // Copy of Nitrite's testGetByIndexMethod. MongoDbList.get(int) now serialises the stored sub-document with the ObjectMapper.
    @Test
    public void testGetByIndexMethod() throws Exception {
        list.add(AMIT);
        list.add(RAHUL);
        String s = list.get(0);
        assertThat(s.contains("amit"), is(true));
        list.delete();
    }

    @Test
    public void testGetByIndexNElementsMethod() throws Exception {
        list.add(AMIT);
        list.add(RAHUL);
        list.add(DEEPAK);
        list.add(PRAVEEN);

        List<String> sList = list.get(0, 2);
        assertThat(sList.size(), is(2));
        assertThat(sList.get(0).contains("amit"), is(true));
        assertThat(sList.get(1).contains("rahul"), is(true));
        list.delete();
    }

    @Test
    public void testGetByDocumentIdNElementsMethod() throws Exception {
        list.add(AMIT);
        list.add(RAHUL);
        list.add(DEEPAK);
        list.add(PRAVEEN);

        List<Long> documentIdList = new ArrayList<>();
        documentIdList.add(1L);
        documentIdList.add(2L);

        List<String> sList = list.get(documentIdList);
        assertThat(sList.size(), is(2));
        assertThat(sList.get(0).contains("rahul"), is(true));
        assertThat(sList.get(1).contains("deepak"), is(true));
        list.delete();
    }

    @Test
    public void testIsEndOfListReachedMethod() throws Exception {
        list.add(AMIT);
        list.add(RAHUL);
        assertThat(list.isEndOfListReached(0), is(false));
        assertThat(list.isEndOfListReached(1), is(false));
        assertThat(list.isEndOfListReached(2), is(true));
        list.delete();
    }

    // ---------------------------------------------------------------- gap tests: configure / adds

    @Test
    public void testConfigureSetsAnalyticsService() throws Exception {
        assertThat(list.getAnalyticsService(), is(notNullValue()));
    }

    @Test
    public void testAddStoresValueAndListIndexInCollection() throws Exception {
        list.add(AMIT);
        list.add(RAHUL);
        List<Document> docs = db.getCollection("some_name").find().sort(new Document("list_index", 1)).into(new ArrayList<>());
        assertThat(docs.size(), is(2));
        assertThat(docs.get(0).get("list_index"), is((Object) 0L));
        assertThat(docs.get(1).get("list_index"), is((Object) 1L));
        assertThat(((Document) docs.get(0).get("value")).getString("name"), is("amit"));
    }

    @Test
    public void testAddAndGetIndexReturnsIncreasingIndexes() throws Exception {
        assertThat(list.addAndGetIndex(AMIT), is(0L));
        assertThat(list.addAndGetIndex(RAHUL), is(1L));
        list.add(DEEPAK);
        assertThat(list.addAndGetIndex(PRAVEEN), is(3L));
        assertThat(list.size(), is(4L));
    }

    @Test
    public void testAddListAssignsConsecutiveIndexesAndKeepsOrder() throws Exception {
        list.add(AMIT);
        list.add(new ArrayList<>(List.of(RAHUL, DEEPAK, PRAVEEN)));
        assertThat(list.size(), is(4L));
        List<String> all = list.get(0, 4);
        assertThat(all.size(), is(4));
        assertThat(all.get(0), containsString("amit"));
        assertThat(all.get(1), containsString("rahul"));
        assertThat(all.get(2), containsString("deepak"));
        assertThat(all.get(3), containsString("praveen"));
    }

    @Test
    public void testAddEmptyListIsNoop() throws Exception {
        list.add(new ArrayList<>());
        assertThat(list.size(), is(0L));
        assertThat(db.getCollection("some_name").countDocuments(), is(0L));
    }

    // FINDING: Nitrite's addBulk returns 0L for an empty list. Mongo calls insertMany with an
    // empty list, which the driver rejects with IllegalArgumentException (MongoDbList.java:139).
    @Test
    public void testAddBulkEmptyListThrowsInMongo() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> list.addBulk(new ArrayList<>()));
        assertThat(list.size(), is(0L));
        // the lock must have been released by the finally block, otherwise this add would hang
        list.add(AMIT);
        assertThat(list.size(), is(1L));
    }

    @Test
    public void testAddBulkAssignsIndexesAndKeepsOrder() throws Exception {
        list.add(AMIT);
        assertThat(list.addBulk(new ArrayList<>(List.of(RAHUL, DEEPAK))), is(2L));
        assertThat(list.size(), is(3L));
        List<String> all = list.get(0, 3);
        assertThat(all.get(1), containsString("rahul"));
        assertThat(all.get(2), containsString("deepak"));
        assertThat(list.addAndGetIndex(PRAVEEN), is(3L));
    }

    @Test
    public void testAddInvalidJsonThrowsAndDoesNotAdvanceIndex() throws Exception {
        assertThrows(Exception.class, () -> list.add("this is not json"));
        assertThat(list.size(), is(0L));
        // lock released and counter intact: next add still gets index 0
        assertThat(list.addAndGetIndex(AMIT), is(0L));
    }

    @Test
    public void testAddListWithOneInvalidItemInsertsNothing() throws Exception {
        assertThrows(Exception.class, () -> list.add(new ArrayList<>(List.of(AMIT, "not json"))));
        assertThat(list.size(), is(0L));
        assertThat(db.getCollection("some_name").countDocuments(), is(0L));
    }

    @Test
    public void testConcurrentAddsGetUniqueConsecutiveIndexes() throws Exception {
        int n = 50;
        ExecutorService pool = Executors.newFixedThreadPool(8);
        List<Future<Long>> futures = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            final int k = i;
            futures.add(pool.submit(() -> list.addAndGetIndex("{\"name\": \"n" + k + "\"}")));
        }
        List<Long> indexes = new ArrayList<>();
        for (Future<Long> f : futures) {
            indexes.add(f.get());
        }
        pool.shutdown();
        assertThat(indexes.stream().distinct().count(), is((long) n));
        assertThat(indexes.stream().mapToLong(Long::longValue).max().getAsLong(), is((long) n - 1));
        assertThat(list.size(), is((long) n));
        assertThat(db.getCollection("some_name").countDocuments(), is((long) n));
    }

    // ---------------------------------------------------------------- gap tests: get

    @Test
    public void testGetByIndexReturnsJsonOfStoredItem() throws Exception {
        list.add(AMIT);
        assertThat(list.get(0).replace(" ", ""), is("{\"name\":\"amit\"}"));
    }

    @Test
    public void testGetByIndexOutOfRangeReturnsNull() throws Exception {
        list.add(AMIT);
        assertThat(list.get(5), is(nullValue()));
        assertThat(list.get(-1), is(nullValue()));
    }

    @Test
    public void testGetReturnsExactJsonValues() throws Exception {
        list.add("{\"name\": \"amit\", \"age\": 30, \"tags\": [\"a\", \"b\"], \"addr\": {\"city\": \"blr\"}, \"ok\": true}");
        List<String> got = list.get(0, 1);
        JsonNode node = objectMapper.readTree(got.get(0));
        assertThat(node.get("name").asText(), is("amit"));
        assertThat(node.get("age").asInt(), is(30));
        assertThat(node.get("tags").size(), is(2));
        assertThat(node.get("addr").get("city").asText(), is("blr"));
        assertThat(node.get("ok").asBoolean(), is(true));
        // list_index must not leak into the returned value
        assertThat(node.has("list_index"), is(false));
    }

    @Test
    public void testGetRangeOnEmptyListReturnsEmpty() throws Exception {
        assertThat(list.get(0, 5), is(empty()));
        assertThat(list.get(new ArrayList<Long>()), is(empty()));
    }

    @Test
    public void testGetRangePastEndReturnsOnlyExistingItems() throws Exception {
        list.add(AMIT);
        list.add(RAHUL);
        list.add(DEEPAK);
        List<String> got = list.get(2, 10);
        assertThat(got.size(), is(1));
        assertThat(got.get(0), containsString("deepak"));
        assertThat(list.get(10, 3), is(empty()));
    }

    @Test
    public void testGetRangeWithZeroCountReturnsEmpty() throws Exception {
        list.add(AMIT);
        assertThat(list.get(0, 0), is(empty()));
    }

    @Test
    public void testGetRangeInTheMiddle() throws Exception {
        list.add(new ArrayList<>(List.of(AMIT, RAHUL, DEEPAK, PRAVEEN)));
        List<String> got = list.get(1, 2);
        assertThat(got.size(), is(2));
        assertThat(got.get(0), containsString("rahul"));
        assertThat(got.get(1), containsString("deepak"));
    }

    // FINDING / CONTRADICTS NITRITE: Nitrite get(List<Long>) returns items in the order requested.
    // Mongo uses a single $in query, so the order is whatever the DB returns (insertion order here).
    @Test
    public void testGetByIdListDoesNotFollowRequestedOrder() throws Exception {
        list.add(new ArrayList<>(List.of(AMIT, RAHUL, DEEPAK)));
        List<String> got = list.get(new ArrayList<>(List.of(2L, 1L)));
        assertThat(got.size(), is(2));
        assertThat(got.get(0), containsString("rahul"));
        assertThat(got.get(1), containsString("deepak"));
    }

    // CONTRADICTS NITRITE: Nitrite get(List<Long>) throws NullPointerException for a missing id;
    // Mongo silently skips it.
    @Test
    public void testGetByIdListSkipsMissingIds() throws Exception {
        list.add(new ArrayList<>(List.of(AMIT, RAHUL)));
        List<String> got = list.get(new ArrayList<>(List.of(1L, 99L)));
        assertThat(got.size(), is(1));
        assertThat(got.get(0), containsString("rahul"));
    }

    // ---------------------------------------------------------------- gap tests: dots / decimals

    @Test
    public void testDotsInValuesAreEncodedInStorageAndDecodedOnRead() throws Exception {
        list.add("{\"email\": \"a.b@c.com\"}");
        Document raw = db.getCollection("some_name").find().first();
        String stored = ((Document) raw.get("value")).getString("email");
        assertThat(stored, is("aENCODE_DOTb@cENCODE_DOTcom"));
        List<String> got = list.get(0, 1);
        assertThat(objectMapper.readTree(got.get(0)).get("email").asText(), is("a.b@c.com"));
    }

    @Test
    public void testDotsInKeysAreEncodedAndDecoded() throws Exception {
        list.add("{\"a.b\": 1}");
        Document raw = db.getCollection("some_name").find().first();
        assertThat(((Document) raw.get("value")).containsKey("aENCODE_DOTb"), is(true));
        JsonNode node = objectMapper.readTree(list.get(0, 1).get(0));
        assertThat(node.has("a.b"), is(true));
    }

    @Test
    public void testDotsRoundTripThroughAddListBulkAndIdList() throws Exception {
        list.add(new ArrayList<>(List.of("{\"v\": \"1.1\"}")));
        list.addBulk(new ArrayList<>(List.of("{\"v\": \"2.2\"}")));
        list.addAndGetIndex("{\"v\": \"3.3\"}");
        List<String> byIds = list.get(new ArrayList<>(List.of(0L, 1L, 2L)));
        assertThat(byIds.get(0), containsString("1.1"));
        assertThat(byIds.get(1), containsString("2.2"));
        assertThat(byIds.get(2), containsString("3.3"));
    }

    // Same as Nitrite: a bare decimal number 1.5 becomes 1ENCODE_DOT5 (invalid JSON) and the add fails.
    // FINDING: this means numeric decimals can never be stored; only strings like "1.5" work.
    @Test
    public void testDecimalNumberCannotBeStored() throws Exception {
        assertThrows(Exception.class, () -> list.add("{\"price\": 1.5}"));
        assertThat(list.size(), is(0L));
    }

    @Test
    public void testDecimalAsStringIsStoredAndReturned() throws Exception {
        list.add("{\"price\": \"1.5\"}");
        assertThat(objectMapper.readTree(list.get(0, 1).get(0)).get("price").asText(), is("1.5"));
    }

    // FINDING: the encoder is a plain string replace over the whole JSON, so a user value that
    // literally contains ENCODE_DOT comes back with a dot in it.
    @Test
    public void testLiteralEncodeDotTextInPayloadIsCorrupted() throws Exception {
        list.add("{\"note\": \"fooENCODE_DOTbar\"}");
        String note = objectMapper.readTree(list.get(0, 1).get(0)).get("note").asText();
        assertThat(note, is("foo.bar"));
    }

    // ---------------------------------------------------------------- gap tests: size / end / empty

    @Test
    public void testEmptyList() throws Exception {
        assertThat(list.size(), is(0L));
        assertThat(list.isEndOfListReached(0), is(true));
        assertThat(list.get(0), is(nullValue()));
        MongoDbCursor cursor = list.filterAsset(GenericAsset.class, null);
        assertThat(cursor.hasNext(), is(false));
        assertThat(cursor.docSize(), is(0L));
    }

    @Test
    public void testSizeCountsAllAddMethods() throws Exception {
        list.add(AMIT);
        list.addAndGetIndex(RAHUL);
        list.add(new ArrayList<>(List.of(DEEPAK, PRAVEEN)));
        list.addBulk(new ArrayList<>(List.of(AMIT, RAHUL, DEEPAK)));
        assertThat(list.size(), is(7L));
        assertThat(db.getCollection("some_name").countDocuments(), is(7L));
    }

    @Test
    public void testIsEndOfListReachedAfterEachAdd() throws Exception {
        assertThat(list.isEndOfListReached(0), is(true));
        list.add(AMIT);
        assertThat(list.isEndOfListReached(0), is(false));
        assertThat(list.isEndOfListReached(1), is(true));
        assertThat(list.isEndOfListReached(100), is(true));
    }

    // ---------------------------------------------------------------- gap tests: restart / unique index

    // FINDING: size() and the next index live only in memory (AtomicLong). A new MongoDbList on a
    // collection that already has data starts at 0: size() says 0 and the next add collides with the
    // unique list_index index. Nitrite has the same shape of problem; Mongo is persistent so it is real.
    @Test
    public void testCountersAfterRestartStartAtZeroAndNextAddCollides() throws Exception {
        list.add(AMIT);
        list.add(RAHUL);

        MongoDbList restarted = newList("some_name");
        assertThat(restarted.size(), is(0L));
        assertThat(restarted.isEndOfListReached(0), is(true));
        assertThrows(MongoWriteException.class, () -> restarted.add(DEEPAK));
        assertThat(restarted.size(), is(0L));
        assertThat(db.getCollection("some_name").countDocuments(), is(2L));
    }

    @Test
    public void testUniqueListIndexRejectsDuplicates() throws Exception {
        list.add(AMIT);
        assertThrows(MongoWriteException.class,
                () -> db.getCollection("some_name").insertOne(new Document("list_index", 0L).append("value", new Document("name", "dup"))));
    }

    @Test
    public void testListIndexIndexesExist() throws Exception {
        List<Document> indexes = db.getCollection("some_name").listIndexes().into(new ArrayList<>());
        Document listIdx = indexes.stream().filter(d -> "list_index_1".equals(d.getString("name"))).findFirst().orElse(null);
        assertThat(listIdx, is(notNullValue()));
        assertThat(listIdx.getBoolean("unique"), is(true));
        assertThat(indexes.stream().map(d -> d.getString("name")).toList(), hasItem("value.created_at_ms_1"));
    }

    // ---------------------------------------------------------------- gap tests: createIndex

    @Test
    public void testCreateIndexOnAssetField() throws Exception {
        list.createIndexOnAssetField("name");
        List<String> names = db.getCollection("some_name").listIndexes().into(new ArrayList<>())
                .stream().map(d -> d.getString("name")).toList();
        assertThat(names, hasItem("value.name_1"));
    }

    @Test
    public void testCreateIndexOnOutputModelField() throws Exception {
        list.createIndexOnOutputModelField("city");
        List<String> names = db.getCollection("some_name").listIndexes().into(new ArrayList<>())
                .stream().map(d -> d.getString("name")).toList();
        assertThat(names, hasItem("value.outputModel.data.city_1"));
    }

    @Test
    public void testCreateIndexTwiceIsIdempotent() throws Exception {
        list.createIndexOnAssetField("name");
        list.createIndexOnAssetField("name");
        list.createIndexOnOutputModelField("city");
        list.createIndexOnOutputModelField("city");
        long count = db.getCollection("some_name").listIndexes().into(new ArrayList<>())
                .stream().filter(d -> "value.name_1".equals(d.getString("name"))).count();
        assertThat(count, is(1L));
    }

    // ---------------------------------------------------------------- gap tests: filterAsset

    @Test
    public void testFilterAssetNullClassThrows() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> list.filterAsset(null, null));
    }

    @Test
    public void testFilterAssetWithoutFilterReturnsOnlyThatClass() throws Exception {
        list.add(generic(10, "a", "om1", "{}"));
        list.add(asset("com.other.SomethingElse", 20, "b", "om1", "{}"));
        list.add(generic(30, "c", "om1", "{}"));
        MongoDbCursor cursor = list.filterAsset(GenericAsset.class, null);
        assertThat(cursor.docSize(), is(2L));
        assertThat(names(cursor), is(List.of("a", "c")));
    }

    // Class names contain dots, which are stored as ENCODE_DOT; the filter has to encode the same way.
    @Test
    public void testFilterAssetOnClassNameWithDots() throws Exception {
        list.add(generic(10, "a", "om1", "{}"));
        Document raw = db.getCollection("some_name").find().first();
        assertThat(((Document) raw.get("value")).getString("clazz"),
                is(GENERIC_CLASS.replace(".", "ENCODE_DOT")));
        assertThat(list.filterAsset(GenericAsset.class, null).hasNext(), is(true));
    }

    @Test
    public void testFilterAssetSortsByCreatedAtMs() throws Exception {
        list.add(generic(30, "third", "om1", "{}"));
        list.add(generic(10, "first", "om1", "{}"));
        list.add(generic(20, "second", "om1", "{}"));
        assertThat(names(list.filterAsset(GenericAsset.class, null)), is(List.of("first", "second", "third")));
        assertThat(names(list.filterAsset(GenericAsset.class, spel("name == 'first' or name == 'third'"))),
                is(List.of("first", "third")));
    }

    @Test
    public void testFilterAssetEqualsString() throws Exception {
        list.add(generic(10, "a", "om1", "{}"));
        list.add(generic(20, "b", "om1", "{}"));
        MongoDbCursor cursor = list.filterAsset(GenericAsset.class, spel("name == 'b'"));
        assertThat(names(cursor), is(List.of("b")));
        assertThat(cursor.docSize(), is(1L));
    }

    @Test
    public void testFilterAssetEqualsInt() throws Exception {
        list.add(generic(10, "a", "om1", "{}"));
        list.add(generic(20, "b", "om1", "{}"));
        assertThat(names(list.filterAsset(GenericAsset.class, spel("created_at_ms == 20"))), is(List.of("b")));
    }

    @Test
    public void testFilterAssetAnd() throws Exception {
        list.add(generic(10, "a", "om1", "{}"));
        list.add(generic(20, "a", "om1", "{}"));
        list.add(generic(30, "b", "om1", "{}"));
        assertThat(names(list.filterAsset(GenericAsset.class, spel("name == 'a' and created_at_ms == 20"))), is(List.of("a")));
        MongoDbCursor none = list.filterAsset(GenericAsset.class, spel("name == 'b' and created_at_ms == 10"));
        assertThat(none.hasNext(), is(false));
    }

    @Test
    public void testFilterAssetOr() throws Exception {
        list.add(generic(10, "a", "om1", "{}"));
        list.add(generic(20, "b", "om1", "{}"));
        list.add(generic(30, "c", "om1", "{}"));
        assertThat(names(list.filterAsset(GenericAsset.class, spel("name == 'a' or name == 'c'"))), is(List.of("a", "c")));
    }

    @Test
    public void testFilterAssetNestedAndOr() throws Exception {
        list.add(generic(10, "a", "om1", "{}"));
        list.add(generic(20, "b", "om1", "{}"));
        list.add(generic(30, "c", "om1", "{}"));
        MongoDbCursor c = list.filterAsset(GenericAsset.class,
                spel("created_at_ms == 10 or (name == 'c' and created_at_ms == 30)"));
        assertThat(names(c), is(List.of("a", "c")));
    }

    // CONTRADICTS NITRITE: Nitrite handles != (OpNE); Mongo throws IllegalArgumentException.
    @Test
    public void testFilterAssetNotEqualsIsUnsupported() throws Exception {
        list.add(generic(10, "a", "om1", "{}"));
        assertThrows(IllegalArgumentException.class, () -> list.filterAsset(GenericAsset.class, spel("name != 'a'")));
    }

    @Test
    public void testFilterAssetOtherOperatorsAreUnsupported() throws Exception {
        for (String expr : new String[] { "created_at_ms > 5", "created_at_ms < 5", "created_at_ms >= 5",
                "created_at_ms <= 5", "name matches 'a.*'", "!(name == 'a')" }) {
            assertThrows(IllegalArgumentException.class, () -> list.filterAsset(GenericAsset.class, spel(expr)), expr);
        }
    }

    @Test
    public void testFilterAssetEqualsNonStringNonIntLiteralThrowsIllegalState() throws Exception {
        assertThrows(IllegalStateException.class, () -> list.filterAsset(GenericAsset.class, spel("price == 1.5")));
        assertThrows(IllegalStateException.class, () -> list.filterAsset(GenericAsset.class, spel("ok == true")));
    }

    // ---------------------------------------------------------------- gap tests: docSize

    @Test
    public void testDocSizeWithoutAndWithFilterOnFilterAsset() throws Exception {
        list.add(generic(10, "a", "om1", "{}"));
        list.add(generic(20, "a", "om1", "{}"));
        list.add(generic(30, "b", "om1", "{}"));
        list.add(asset("com.other.X", 40, "a", "om1", "{}"));
        assertThat(list.filterAsset(GenericAsset.class, null).docSize(), is(3L));
        assertThat(list.filterAsset(GenericAsset.class, spel("name == 'a'")).docSize(), is(2L));
        assertThat(list.filterAsset(GenericAsset.class, spel("name == 'zzz'")).docSize(), is(0L));
    }

    @Test
    public void testDocSizeWithoutFilterOnFilterOutputModel() throws Exception {
        list.add(generic(10, "a", "om1", "{}"));
        list.add(generic(20, "b", "om1", "{}"));
        list.add(generic(30, "c", "om2", "{}"));
        assertThat(list.filterOutputModel("om1", null).docSize(), is(2L));
        assertThat(list.filterOutputModel("om2", null).docSize(), is(1L));
    }

    // FINDING: filterOutputModel with a filter never sets docSize (MongoDbList.java:331-350), so
    // docSize() is 0 even though documents match. Current behaviour pinned here; the correct
    // behaviour is in the disabled test below.
    @Test
    public void testDocSizeWithFilterOnFilterOutputModelIsZeroBug() throws Exception {
        list.add(generic(10, "a", "om1", "{\"city\":\"blr\"}"));
        list.add(generic(20, "b", "om1", "{\"city\":\"blr\"}"));
        list.add("{\"clazz\":\"" + GENERIC_CLASS + "\",\"created_at_ms\":30,\"city\":\"blr\",\"outputModel\":{\"name\":\"om1\",\"data\":{}}}");
        // use a filter on a field that exists at value.<field>, since that is what Mongo queries (see next test)
        MongoDbCursor cursor = list.filterOutputModel("om1", spel("city == 'blr'"));
        assertThat(cursor.hasNext(), is(true));
        assertThat(cursor.docSize(), is(0L));
    }

    @Test
    @Disabled("FINDING: filterOutputModel(name, filter) never computes docSize, it stays 0")
    public void testDocSizeWithFilterOnFilterOutputModelShouldBeMatchCount() throws Exception {
        list.add(generic(10, "a", "om1", "{\"city\":\"blr\"}"));
        list.add(generic(20, "b", "om1", "{\"city\":\"blr\"}"));
        list.add(generic(30, "c", "om1", "{\"city\":\"del\"}"));
        assertThat(list.filterOutputModel("om1", spel("city == 'blr'")).docSize(), is(2L));
    }

    // ---------------------------------------------------------------- gap tests: filterOutputModel

    @Test
    public void testFilterOutputModelWithoutFilter() throws Exception {
        list.add(generic(30, "c", "om1", "{}"));
        list.add(generic(10, "a", "om1", "{}"));
        list.add(generic(20, "b", "om2", "{}"));
        list.add(asset("com.other.X", 5, "x", "om1", "{}"));
        MongoDbCursor cursor = list.filterOutputModel("om1", null);
        assertThat(names(cursor), is(List.of("a", "c")));
    }

    @Test
    public void testFilterOutputModelUnknownNameReturnsNothing() throws Exception {
        list.add(generic(10, "a", "om1", "{}"));
        MongoDbCursor cursor = list.filterOutputModel("nope", null);
        assertThat(cursor.hasNext(), is(false));
        assertThat(cursor.docSize(), is(0L));
    }

    @Test
    public void testFilterOutputModelNextOutputModelReturnsData() throws Exception {
        list.add(generic(10, "a", "om1", "{\"city\":\"blr\",\"age\":5}"));
        MongoDbCursor cursor = list.filterOutputModel("om1", null);
        assertThat(cursor.hasNext(), is(true));
        ObjectNode data = cursor.getNextOutputModel();
        assertThat(data.get("city").asText(), is("blr"));
        assertThat(data.get("age").asInt(), is(5));
    }

    @Test
    public void testFilterOutputModelDotsInDataAreDecoded() throws Exception {
        list.add(generic(10, "a", "om1", "{\"email\":\"a.b@c.com\"}"));
        ObjectNode data = list.filterOutputModel("om1", null).getNextOutputModel();
        assertThat(data.get("email").asText(), is("a.b@c.com"));
    }

    // FINDING / CONTRADICTS NITRITE: for output-model filters Nitrite queries
    // value.outputModel.data.<field>; Mongo ignores isDslBased and queries value.<field>
    // (MongoDbList.java:401,406). So a developer filter on an output model field matches nothing,
    // and createIndexOnOutputModelField indexes a path the filter never uses.
    @Test
    public void testFilterOutputModelDeveloperFilterQueriesWrongPath() throws Exception {
        list.add(generic(10, "a", "om1", "{\"city\":\"blr\"}"));
        MongoDbCursor cursor = list.filterOutputModel("om1", spel("city == 'blr'"));
        assertThat(cursor.hasNext(), is(false));
    }

    @Test
    @Disabled("FINDING: filterOutputModel developer filter should query value.outputModel.data.<field> (like Nitrite) but queries value.<field>")
    public void testFilterOutputModelDeveloperFilterShouldMatchOutputModelData() throws Exception {
        list.add(generic(10, "a", "om1", "{\"city\":\"blr\"}"));
        list.add(generic(20, "b", "om1", "{\"city\":\"del\"}"));
        MongoDbCursor cursor = list.filterOutputModel("om1", spel("city == 'blr'"));
        assertThat(cursor.hasNext(), is(true));
        assertThat(names(cursor), is(List.of("a")));
    }

    @Test
    public void testFilterOutputModelAndOrOnTopLevelFieldsAndNeOperatorThrows() throws Exception {
        list.add(generic(10, "a", "om1", "{}"));
        list.add(generic(20, "b", "om1", "{}"));
        list.add(generic(30, "c", "om1", "{}"));
        assertThat(names(list.filterOutputModel("om1", spel("name == 'a' or name == 'c'"))), is(List.of("a", "c")));
        assertThat(names(list.filterOutputModel("om1", spel("name == 'a' and created_at_ms == 10"))), is(List.of("a")));
        assertThrows(IllegalArgumentException.class, () -> list.filterOutputModel("om1", spel("name != 'a'")));
    }

    // ---------------------------------------------------------------- gap tests: delete

    @Test
    public void testDeleteDropsCollection() throws Exception {
        list.add(AMIT);
        assertThat(db.listCollectionNames().into(new ArrayList<>()), hasItem("some_name"));
        list.delete();
        assertThat(db.listCollectionNames().into(new ArrayList<>()), not(hasItem("some_name")));
        assertThat(list.get(0, 5), is(empty()));
        assertThat(list.filterAsset(GenericAsset.class, null).hasNext(), is(false));
    }

    @Test
    public void testDeleteOnEmptyOrAlreadyDeletedListDoesNotThrow() throws Exception {
        list.delete();
        list.delete();
    }

    // FINDING: delete() does not reset the in-memory counter, and later adds silently recreate the
    // collection WITHOUT the unique list_index index (indexes die with the drop).
    @Test
    public void testAddAfterDeleteKeepsCounterAndLosesUniqueIndex() throws Exception {
        list.add(AMIT);
        list.add(RAHUL);
        list.delete();
        assertThat(list.size(), is(2L));
        assertThat(list.addAndGetIndex(DEEPAK), is(2L));
        List<String> idxNames = db.getCollection("some_name").listIndexes().into(new ArrayList<>())
                .stream().map(d -> d.getString("name")).toList();
        assertThat(idxNames, not(hasItem("list_index_1")));
    }

    @Test
    public void testListsAreIsolatedByCollectionName() throws Exception {
        MongoDbList other = newList("other_list");
        list.add(AMIT);
        other.add(RAHUL);
        other.add(DEEPAK);
        assertThat(list.size(), is(1L));
        assertThat(other.size(), is(2L));
        assertThat(list.get(0, 5).size(), is(1));
        assertThat(other.get(0, 5).size(), is(2));
        other.delete();
        assertThat(list.get(0, 5).size(), is(1));
    }

    // ---------------------------------------------------------------- MockFacadeMongoDbList (mirror of MockFacadeNitritedbList)

    @Test
    public void testMockFacadeMongoDbListServesConfiguredAnswers() throws Exception {
        MongoDbList facadeList = mockFacadeMongoDbList.configure().build();

        facadeList.add("{\"x\":1}");
        assertThat(facadeList.addAndGetIndex("{\"x\":1}"), is(1000L));
        assertThat(facadeList.addBulk(new java.util.ArrayList<>()), is(100L));
        assertThat(facadeList.get(0), is("{\"name\":\"amit\"}"));
        assertThat(facadeList.get(0, 5), org.hamcrest.Matchers.contains("{\"name\":\"amit\"}"));
        assertThat(facadeList.get(new java.util.ArrayList<Long>()), org.hamcrest.Matchers.contains("1", "2", "3"));
        assertThat(facadeList.isEndOfListReached(0), is(false));
        assertThat(facadeList.filterAsset(GenericAsset.class, null), org.hamcrest.Matchers.notNullValue());
    }
}
