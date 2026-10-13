package com.freshworks.hagrid.shared.infra.mongo;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshworks.hagrid.shared.MockFacadeSyncServiceContainer;
import com.freshworks.hagrid.shared.SyncServiceContainer;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;

/**
 * Mongo twin of TestNitriteDbQueue. The first block of tests is the Nitrite
 * test set with the same assertions; the second block covers gaps the Nitrite
 * tests never checked.
 */
@SpringBootTest
@EnabledIfSystemProperty(named = "spring.profiles.active", matches = "unit")
public class TestMongoDbQueue {

    private static final String AMIT = "{\"name\": \"amit\"}";
    private static final String RAHUL = "{\"name\": \"rahul\"}";

    private static MongoClient mongoClient;

    @Autowired
    MockFacadeSyncServiceContainer mockFacadeSyncServiceContainer;

    @Autowired
    MockFacadeMongoDbQueue mockFacadeMongoDbQueue;

    private MongoDatabase db;
    private MongoDbQueue queue;
    private SyncServiceContainer syncServiceContainer;
    private final ObjectMapper objectMapper = new ObjectMapper();

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
        // own database per test, so tests never see each other's data
        db = mongoClient.getDatabase("test_queue_" + UUID.randomUUID().toString().replace("-", ""));
        syncServiceContainer = mockFacadeSyncServiceContainer.configure().build();
        queue = newQueue("some_name");
    }

    @AfterEach
    void tearDown() {
        db.drop();
    }

    /** Same wiring MongoService.getMongoDbQueue does: collection + unique queue_index index. */
    private MongoDbQueue newQueue(String name) throws Exception {
        MongoDbQueue q = new MongoDbQueue();
        q.setQueue(db.getCollection(name));
        q.getQueue().createIndex(Indexes.ascending("queue_index"), new IndexOptions().unique(true));
        q.configure(syncServiceContainer);
        return q;
    }

    private void mockLockAndCondition(MongoDbQueue q, ReentrantLock lock, Condition condition) throws Exception {
        q.setHasMoreDataLock(lock);
        Field field = MongoDbQueue.class.getDeclaredField("hasNotMoreDataQueue");
        field.setAccessible(true);
        field.set(q, condition);
    }

    // ---------------------------------------------------------------- copied from TestNitriteDbQueue

    @Test
    public void testAddMethod() throws Exception {
        queue.add(AMIT);
        assertThat(queue.getQueueIndex().get(), is(1L));
        assertThat(queue.hasMoreData(), is(true));
        queue.delete();
    }

    @Test
    public void testAddListMethod() throws Exception {
        queue.add(List.of(AMIT, RAHUL));
        assertThat(queue.getQueueIndex().get(), is(2L));
        assertThat(queue.hasMoreData(), is(true));
        queue.delete();
    }

    @Test
    public void testPollMethod() throws Exception {
        assertThat(queue.getPopIndex(), is(0L));
        assertThat(queue.getQueueIndex().get(), is(0L));

        queue.add(List.of(AMIT, RAHUL));
        assertThat(queue.getPopIndex(), is(0L));
        assertThat(queue.getQueueIndex().get(), is(2L));

        queue.poll();

        assertThat(queue.getPopIndex(), is(1L));
        assertThat(queue.getQueueIndex().get(), is(2L));
        assertThat(queue.hasMoreData(), is(true));
        queue.delete();
    }

    @Test
    public void testPollNElementsMethod() throws Exception {
        assertThat(queue.getPopIndex(), is(0L));
        assertThat(queue.getQueueIndex().get(), is(0L));

        queue.add(List.of(AMIT, RAHUL));
        assertThat(queue.getPopIndex(), is(0L));
        assertThat(queue.getQueueIndex().get(), is(2L));

        queue.poll(2);

        assertThat(queue.getPopIndex(), is(2L));
        assertThat(queue.getQueueIndex().get(), is(2L));
        assertThat(queue.size(), is(2L));
        assertThat(queue.isEmpty(), is(true));
        queue.delete();
    }

    @Test
    public void testHashMoreDataWhenQueueIsEmpty() throws Exception {
        Thread s = new Thread(() -> {
            try {
                assertThat(queue.hasMoreData(), is(true));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        s.setDaemon(true);
        s.start();
        Thread.sleep(2000);

        queue.hasMoreDataLock.lock();
        assertThat(queue.hasMoreDataLock.getWaitQueueLength(queue.getHasNotMoreDataQueue()), is(1));
        queue.hasMoreDataLock.unlock();

        s.interrupt();
        queue.delete();
    }

    @Test
    public void testHashMoreDataWhenQueueIsHasData() throws Exception {
        queue.add(AMIT);
        assertThat(queue.hasMoreData(), is(true));
        queue.delete();
    }

    @Test
    public void testAddMethodSignalsHasMoreData() throws Exception {
        ReentrantLock mockedLock = Mockito.mock(ReentrantLock.class);
        Condition mockedCondition = Mockito.mock(Condition.class);
        mockLockAndCondition(queue, mockedLock, mockedCondition);

        mockedLock.lock();
        assertThat(mockedLock.getWaitQueueLength(mockedCondition), is(0));
        queue.add(AMIT);

        verify(mockedCondition, times(1)).signalAll();
        verify(mockedCondition, times(0)).signal();
        mockedLock.unlock();
        queue.delete();
    }

    @Test
    public void testAddListSignalsHasMoreData() throws Exception {
        ReentrantLock mockedLock = Mockito.mock(ReentrantLock.class);
        Condition mockedCondition = Mockito.mock(Condition.class);
        mockLockAndCondition(queue, mockedLock, mockedCondition);

        mockedLock.lock();
        assertThat(mockedLock.getWaitQueueLength(mockedCondition), is(0));
        queue.add(List.of(AMIT, RAHUL));

        verify(mockedCondition, times(1)).signalAll();
        verify(mockedCondition, times(0)).signal();
        mockedLock.unlock();
        queue.delete();
    }

    @Test
    public void testRemovePublisherSignalsHasMoreData() throws Exception {
        ReentrantLock mockedLock = Mockito.mock(ReentrantLock.class);
        Condition mockedCondition = Mockito.mock(Condition.class);
        mockLockAndCondition(queue, mockedLock, mockedCondition);

        mockedLock.lock();
        assertThat(mockedLock.getWaitQueueLength(mockedCondition), is(0));
        queue.removePublisher();
        verify(mockedCondition, times(1)).signalAll();
        verify(mockedCondition, times(0)).signal();
        mockedLock.unlock();
        queue.delete();
    }

    // ---------------------------------------------------------------- gaps the Nitrite tests never checked

    @Test
    public void testPollReturnsItemsInFifoOrder() throws Exception {
        queue.add(AMIT);
        queue.add(RAHUL);

        assertThat(objectMapper.readTree(queue.poll()), is(objectMapper.readTree(AMIT)));
        assertThat(objectMapper.readTree(queue.poll()), is(objectMapper.readTree(RAHUL)));
    }

    @Test
    public void testPollNReturnsItemsInFifoOrder() throws Exception {
        List<String> items = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            items.add("{\"n\": " + i + "}");
        }
        queue.add(items);

        List<String> polled = queue.poll(50);

        assertThat(polled.size(), is(50));
        for (int i = 0; i < 50; i++) {
            assertThat(objectMapper.readTree(polled.get(i)).get("n").asInt(), is(i));
        }
    }

    @Test
    public void testPollOnEmptyQueueReturnsNull() throws Exception {
        assertThat(queue.poll(), is(nullValue()));
        assertThat(queue.getPopIndex(), is(0L));
    }

    @Test
    public void testPollNOnEmptyQueueReturnsEmptyList() throws Exception {
        assertThat(queue.poll(5), is(empty()));
        assertThat(queue.getPopIndex(), is(0L));
    }

    @Test
    public void testPollNWithMoreThanAvailableOnlyAdvancesByWhatWasReturned() throws Exception {
        queue.add(List.of(AMIT, RAHUL));

        List<String> polled = queue.poll(10);

        assertThat(polled.size(), is(2));
        assertThat(queue.getPopIndex(), is(2L));
        assertThat(queue.isEmpty(), is(true));
    }

    @Test
    public void testPollNContinuesFromWhereLastPollStopped() throws Exception {
        queue.add(List.of("{\"n\": 0}", "{\"n\": 1}", "{\"n\": 2}", "{\"n\": 3}"));

        queue.poll();
        List<String> polled = queue.poll(2);

        assertThat(objectMapper.readTree(polled.get(0)).get("n").asInt(), is(1));
        assertThat(objectMapper.readTree(polled.get(1)).get("n").asInt(), is(2));
        assertThat(queue.getPopIndex(), is(3L));
    }

    @Test
    public void testHasMoreDataIsFalseAfterPublisherRemovedAndQueueDrained() throws Exception {
        queue.add(AMIT);
        queue.poll();
        queue.removePublisher();

        assertThat(queue.hasMoreData(), is(false));
    }

    @Test
    public void testHasMoreDataStaysTrueWhileItemsRemainEvenAfterPublisherRemoved() throws Exception {
        queue.add(List.of(AMIT, RAHUL));
        queue.removePublisher();
        queue.poll();

        assertThat(queue.hasMoreData(), is(true));
    }

    @Test
    public void testDotsInValuesRoundTrip() throws Exception {
        String json = "{\"email\": \"amit@freshworks.com\"}";
        queue.add(json);

        String polled = queue.poll();

        assertThat(objectMapper.readTree(polled), is(objectMapper.readTree(json)));
    }

    @Test
    public void testDecimalNumberInPayloadIsRejected() throws Exception {
        // FINDING: add() encodes every "." in the raw JSON text, so 1.5 becomes 1ENCODE_DOT5,
        // which is not a valid JSON number. Any payload with a decimal fails. Flip this to a
        // round-trip assertion once fixed (the same replace exists in NitriteDbQueue).
        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () -> queue.add("{\"price\": 1.5}"));
        assertThat(queue.getQueueIndex().get(), is(0L));
    }

    @Test
    public void testNestedJsonRoundTrip() throws Exception {
        String json = "{\"a\": {\"b\": [1, 2, {\"c\": \"d\"}]}, \"flag\": true, \"nothing\": null}";
        queue.add(json);

        assertThat(objectMapper.readTree(queue.poll()), is(objectMapper.readTree(json)));
    }

    @Test
    public void testItemsAreStoredInMongoWithQueueIndex() throws Exception {
        queue.add(List.of(AMIT, RAHUL));

        assertThat(queue.getQueue().countDocuments(), is(2L));
        Document second = queue.getQueue().find(new Document("queue_index", 1L)).first();
        assertThat(((Document) second.get("value")).getString("name"), is("rahul"));
    }

    @Test
    public void testPollDoesNotRemoveDocumentsFromMongo() throws Exception {
        queue.add(AMIT);
        queue.poll();

        assertThat(queue.getQueue().countDocuments(), is(1L));
    }

    @Test
    public void testDeleteDropsTheCollection() throws Exception {
        queue.add(AMIT);
        queue.delete();

        assertThat(db.listCollectionNames().into(new ArrayList<>()), not(contains("some_name")));
    }

    @Test
    public void testResetAfterRestartRestoresCounters() throws Exception {
        queue.add(List.of(AMIT, RAHUL));
        queue.poll();

        // simulate a process restart: a new queue object over the same collection
        MongoDbQueue restarted = newQueue("some_name");
        assertThat(restarted.getQueueIndex().get(), is(0L));
        restarted.reset();

        assertThat(restarted.getPopIndex(), is(0L));
        assertThat(restarted.getQueueIndex().get(), is(2L));
        // and adding no longer collides with the unique index
        restarted.add("{\"name\": \"third\"}");
        assertThat(restarted.size(), is(3L));
    }

    @Test
    public void testAddWithoutResetAfterRestartCollidesWithExistingRows() throws Exception {
        queue.add(AMIT);

        MongoDbQueue restarted = newQueue("some_name");

        // documents the current behaviour: counters are memory-only, so index 0 is reused
        org.junit.jupiter.api.Assertions.assertThrows(com.mongodb.MongoWriteException.class,
                () -> restarted.add(RAHUL));
    }

    @Test
    public void testAddInvalidJsonThrowsAndDoesNotAdvanceQueueIndex() throws Exception {
        org.junit.jupiter.api.Assertions.assertThrows(Exception.class, () -> queue.add("not json"));

        assertThat(queue.getQueueIndex().get(), is(0L));
        // the add lock must have been released
        queue.add(AMIT);
        assertThat(queue.size(), is(1L));
    }

    @Test
    @Disabled("FINDING #1: timer is named null.execution.time; enable once MongoService/MongoDbQueue is fixed")
    public void testExecutionTimerNameContainsQueueName() throws Exception {
        // FINDING: MongoService calls configure() before setQueue(), and configure() builds the
        // name from `queue + ...`, so in production the timer is "null.execution.time".
        MongoDbQueue q = new MongoDbQueue();
        q.configure(syncServiceContainer);
        assertThat(q.getTimer().getId().getName(), not(containsString("null")));
    }

    // ---------------------------------------------------------------- mirror of a Nitrite case that has no @Test there

    /** TestNitriteDbQueue.testHashMoreDataWhenThereIsDataInQueue: the method exists in Nitrite but is missing @Test, so it never runs there. */
    @Test
    public void testHashMoreDataWhenThereIsDataInQueue() throws Exception {
        queue.add(AMIT);
        assertThat(queue.getQueueIndex().get(), is(1L));
        assertThat(queue.hasMoreData(), is(true));
        queue.delete();
    }

    // ---------------------------------------------------------------- MockFacadeMongoDbQueue (mirror of MockFacadeNitritedbQueue)

    @Test
    public void testMockFacadeMongoDbQueueServesConfiguredAnswers() throws Exception {
        MongoDbQueue facadeQueue = mockFacadeMongoDbQueue.configure().poll("{\"a\":1}").hasMoreData(true).build();

        facadeQueue.add("{\"x\":1}");                 // stubbed: does nothing, no database needed
        facadeQueue.add(new java.util.ArrayList<>());

        assertThat(facadeQueue.poll(), is("{\"a\":1}"));
        assertThat(facadeQueue.hasMoreData(), is(true));
        assertThat(facadeQueue.poll(3), org.hamcrest.Matchers.contains("{\"name\":\"amit\"}", "{\"name\":\"rahul\"}", "{\"name\":\"deepak\"}"));
    }
}
