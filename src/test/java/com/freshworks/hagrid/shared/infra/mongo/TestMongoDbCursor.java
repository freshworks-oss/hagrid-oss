package com.freshworks.hagrid.shared.infra.mongo;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.NoSuchElementException;
import java.util.UUID;

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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.freshworks.hagrid.main.assets.GenericAsset;
import com.freshworks.hagrid.processor.AbstractAsset;
import com.freshworks.hagrid.shared.MockFacadeSyncServiceContainer;
import com.freshworks.hagrid.shared.SyncServiceContainer;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;

/**
 * Mongo twin of NitriteDbCursor behaviour. There is no Nitrite cursor test class
 * (only MockFacadeNitriteDbCursor, a mock helper used by consumer tests), so the
 * contract is InfraDbCursor + NitriteDbCursor's code.
 *
 * Cursors are obtained through MongoDbList.filterAsset / filterOutputModel (used
 * only as a tool), or built directly with the public constructor.
 */
@SpringBootTest
@EnabledIfSystemProperty(named = "spring.profiles.active", matches = "unit")
public class TestMongoDbCursor {

    /** Minimal concrete asset, serialised with the "clazz" type id exactly like production assets. */
    public static class DemoAsset extends AbstractAsset {
        public String name;

        public DemoAsset() {
        }

        public DemoAsset(String name, long createdAtMs) {
            this.name = name;
            setCreated_at_ms(createdAtMs);
        }

        @Override
        public void transform() {
        }
    }

    /** A second asset type, to prove filterAsset only returns the requested class. */
    public static class OtherAsset extends AbstractAsset {
        public String name;

        @Override
        public void transform() {
        }
    }

    private static MongoClient mongoClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    MockFacadeSyncServiceContainer mockFacadeSyncServiceContainer;

    @Autowired
    MockFacadeMongoDbCursor mockFacadeMongoDbCursor;

    private MongoDatabase db;
    private MongoDbList list;
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
        db = mongoClient.getDatabase("test_cursor_" + UUID.randomUUID().toString().replace("-", ""));
        syncServiceContainer = mockFacadeSyncServiceContainer.configure().build();
        // same wiring as MongoService.getMongoDbList
        list = new MongoDbList();
        list.configure(syncServiceContainer);
        list.setList(db.getCollection("some_list"));
        list.getList().createIndex(Indexes.ascending("list_index"), new IndexOptions().unique(true));
        list.getList().createIndex(Indexes.ascending("value.created_at_ms"));
    }

    @AfterEach
    void tearDown() {
        db.drop();
    }

    private void addAsset(AbstractAsset asset) throws Exception {
        list.add(objectMapper.writeValueAsString(asset));
    }

    private String genericAssetJson(String modelName, String dataJson, long createdAt) {
        return "{\"clazz\":\"" + GenericAsset.class.getName() + "\",\"uuid\":\"" + UUID.randomUUID()
                + "\",\"created_at_ms\":" + createdAt + ",\"outputModel\":{\"name\":\"" + modelName
                + "\",\"data\":" + dataJson + "}}";
    }

    // ---------------------------------------------------------------- hasNext / docSize / exhaustion

    @Test
    public void testEmptyCursor() throws Exception {
        MongoDbCursor cursor = list.filterAsset(DemoAsset.class, null);

        assertThat(cursor.hasNext(), is(false));
        assertThat(cursor.docSize(), is(0L));
        assertThrows(NoSuchElementException.class, cursor::getNextAsset);
        assertThrows(NoSuchElementException.class, cursor::getNextOutputModel);
    }

    @Test
    public void testDocSizeAndHasNext() throws Exception {
        addAsset(new DemoAsset("a", 1));
        addAsset(new DemoAsset("b", 2));
        addAsset(new DemoAsset("c", 3));

        MongoDbCursor cursor = list.filterAsset(DemoAsset.class, null);

        assertThat(cursor.docSize(), is(3L));
        assertThat(cursor.hasNext(), is(true));
    }

    @Test
    public void testHasNextDoesNotAdvance() throws Exception {
        addAsset(new DemoAsset("only", 1));
        MongoDbCursor cursor = list.filterAsset(DemoAsset.class, null);

        assertThat(cursor.hasNext(), is(true));
        assertThat(cursor.hasNext(), is(true));
        assertThat(((DemoAsset) cursor.<AbstractAsset>getNextAsset()).name, is("only"));
        assertThat(cursor.hasNext(), is(false));
        assertThat(cursor.hasNext(), is(false));
    }

    @Test
    public void testDocSizeDoesNotShrinkWhileConsuming() throws Exception {
        addAsset(new DemoAsset("a", 1));
        addAsset(new DemoAsset("b", 2));
        MongoDbCursor cursor = list.filterAsset(DemoAsset.class, null);

        cursor.getNextAsset();
        cursor.getNextAsset();

        assertThat(cursor.docSize(), is(2L));
        assertThat(cursor.hasNext(), is(false));
    }

    @Test
    public void testExhaustedCursorThrowsNoSuchElement() throws Exception {
        addAsset(new DemoAsset("a", 1));
        MongoDbCursor cursor = list.filterAsset(DemoAsset.class, null);
        cursor.getNextAsset();

        assertThrows(NoSuchElementException.class, cursor::getNextAsset);
        assertThrows(NoSuchElementException.class, cursor::getNextOutputModel);
    }

    // ---------------------------------------------------------------- getNextAsset

    @Test
    public void testGetNextAssetReturnsConcreteTypeInCreatedAtOrder() throws Exception {
        addAsset(new DemoAsset("third", 30));
        addAsset(new DemoAsset("first", 10));
        addAsset(new DemoAsset("second", 20));

        MongoDbCursor cursor = list.filterAsset(DemoAsset.class, null);

        AbstractAsset a1 = cursor.getNextAsset();
        AbstractAsset a2 = cursor.getNextAsset();
        AbstractAsset a3 = cursor.getNextAsset();
        assertThat(a1 instanceof DemoAsset, is(true));
        assertThat(((DemoAsset) a1).name, is("first"));
        assertThat(((DemoAsset) a2).name, is("second"));
        assertThat(((DemoAsset) a3).name, is("third"));
        assertThat(a1.getCreated_at_ms(), is(10L));
        assertThat(cursor.hasNext(), is(false));
    }

    @Test
    public void testGetNextAssetDecodesEncodeDot() throws Exception {
        addAsset(new DemoAsset("john.doe@example.com", 1));

        // stored form has ENCODE_DOT, nothing with a raw dot in the value
        Document stored = list.getList().find().first();
        assertThat(((Document) stored.get("value")).getString("name"), is("john" + "ENCODE_DOT" + "doe@example" + "ENCODE_DOT" + "com"));

        MongoDbCursor cursor = list.filterAsset(DemoAsset.class, null);
        assertThat(((DemoAsset) cursor.<AbstractAsset>getNextAsset()).name, is("john.doe@example.com"));
    }

    @Test
    public void testFilterAssetOnlyReturnsRequestedClass() throws Exception {
        addAsset(new DemoAsset("mine", 1));
        OtherAsset other = new OtherAsset();
        other.name = "theirs";
        addAsset(other);

        MongoDbCursor cursor = list.filterAsset(DemoAsset.class, null);

        assertThat(cursor.docSize(), is(1L));
        assertThat(((DemoAsset) cursor.<AbstractAsset>getNextAsset()).name, is("mine"));
        assertThat(cursor.hasNext(), is(false));
    }

    @Test
    public void testFilterAssetWithSpelFilterGivesMatchingDocsAndDocSize() throws Exception {
        addAsset(new DemoAsset("a", 1));
        addAsset(new DemoAsset("b", 2));
        SpelExpression spel = (SpelExpression) new SpelExpressionParser().parseExpression("name == 'b'");

        MongoDbCursor cursor = list.filterAsset(DemoAsset.class, spel);

        assertThat(cursor.docSize(), is(1L));
        assertThat(((DemoAsset) cursor.<AbstractAsset>getNextAsset()).name, is("b"));
    }

    // ---------------------------------------------------------------- getNextOutputModel

    @Test
    public void testGetNextOutputModelReturnsDataNodeAndDecodesDots() throws Exception {
        list.add(genericAssetJson("om", "{\"email\":\"a.b@c.com\",\"n\":7}", 1));

        MongoDbCursor cursor = list.filterOutputModel("om", null);

        assertThat(cursor.docSize(), is(1L));
        ObjectNode data = cursor.getNextOutputModel();
        assertThat(data.get("email").asText(), is("a.b@c.com"));
        assertThat(data.get("n").asInt(), is(7));
        assertThat(cursor.hasNext(), is(false));
    }

    @Test
    public void testGetNextOutputModelInCreatedAtOrderAndFilteredByModelName() throws Exception {
        list.add(genericAssetJson("om", "{\"k\":\"second\"}", 20));
        list.add(genericAssetJson("other", "{\"k\":\"nope\"}", 5));
        list.add(genericAssetJson("om", "{\"k\":\"first\"}", 10));

        MongoDbCursor cursor = list.filterOutputModel("om", null);

        assertThat(cursor.docSize(), is(2L));
        assertThat(cursor.getNextOutputModel().get("k").asText(), is("first"));
        assertThat(cursor.getNextOutputModel().get("k").asText(), is("second"));
        assertThat(cursor.hasNext(), is(false));
    }

    @Test
    public void testFilterOutputModelUnknownNameIsEmpty() throws Exception {
        list.add(genericAssetJson("om", "{\"k\":1}", 1));

        MongoDbCursor cursor = list.filterOutputModel("missing", null);

        assertThat(cursor.hasNext(), is(false));
        assertThat(cursor.docSize(), is(0L));
    }

    @Test
    public void testFilterOutputModelWithSpelDocSizeIsZeroToday() throws Exception {
        // FINDING (in MongoDbList.filterOutputModel, visible through the cursor): when a SpEL filter is given,
        // docSize is never computed, so the cursor reports 0 even when it has rows. The no-filter branch counts.
        // The filter below is on a top-level field of "value" (uuid) so that it actually matches the row.
        String json = genericAssetJson("om", "{\"k\":1}", 1);
        list.add(json);
        String uuid = objectMapper.readTree(json).get("uuid").asText();
        SpelExpression spel = (SpelExpression) new SpelExpressionParser().parseExpression("uuid == '" + uuid + "'");

        MongoDbCursor cursor = list.filterOutputModel("om", spel);

        assertThat(cursor.hasNext(), is(true));
        assertThat(cursor.docSize(), is(0L));
    }

    @Test
    @Disabled("FINDING: filterOutputModel with a SpEL filter leaves docSize at 0; expected 1 (matching count)")
    public void testFilterOutputModelWithSpelDocSizeShouldBeCount() throws Exception {
        String json = genericAssetJson("om", "{\"k\":1}", 1);
        list.add(json);
        String uuid = objectMapper.readTree(json).get("uuid").asText();
        SpelExpression spel = (SpelExpression) new SpelExpressionParser().parseExpression("uuid == '" + uuid + "'");

        MongoDbCursor cursor = list.filterOutputModel("om", spel);

        assertThat(cursor.docSize(), is(1L));
    }

    // ---------------------------------------------------------------- built directly

    @Test
    public void testDirectConstructorDocSizeIsTakenOnTrust() throws Exception {
        // ⚠ CONTRADICTS NITRITE: Nitrite derives docSize from DocumentCursor.size(); Mongo cursor just echoes
        // whatever number the caller passes (no check against the real count).
        list.add(genericAssetJson("om", "{\"k\":1}", 1));

        MongoDbCursor cursor = new MongoDbCursor(list.getList().find().iterator(), 42L);

        assertThat(cursor.docSize(), is(42L));
        assertThat(cursor.hasNext(), is(true));
    }

    @Test
    public void testGetNextOutputModelWithoutOutputModelThrowsNpe() throws Exception {
        // A GenericAsset row with no outputModel: same NPE as Nitrite (getOutputModel() is null)
        list.getList().insertOne(new Document("list_index", 0L).append("value",
                Document.parse("{\"clazz\":\"" + GenericAsset.class.getName().replace(".", "ENCODE_DOT") + "\"}")));

        MongoDbCursor cursor = new MongoDbCursor(list.getList().find().iterator(), 1L);

        assertThrows(NullPointerException.class, cursor::getNextOutputModel);
    }

    @Test
    public void testDirectConstructorWithEmptyMongoCursor() throws Exception {
        MongoDbCursor cursor = new MongoDbCursor(list.getList().find().iterator(), 0L);

        assertThat(cursor.hasNext(), is(false));
        assertThat(cursor.docSize(), is(0L));
    }

    // ---------------------------------------------------------------- MockFacadeMongoDbCursor (mirror of MockFacadeNitriteDbCursor)

    @Test
    public void testMockFacadeMongoDbCursorServesConfiguredAnswers() throws Exception {
        MongoDbCursor facadeCursor = mockFacadeMongoDbCursor.configure().hasMore(true).docSize(42L).build();

        assertThat(facadeCursor.hasNext(), is(true));
        assertThat(facadeCursor.docSize(), is(42L));
        assertThat(facadeCursor.getNextAsset() instanceof com.freshworks.hagrid.main.assets.GenericAsset, is(true));
    }
}
