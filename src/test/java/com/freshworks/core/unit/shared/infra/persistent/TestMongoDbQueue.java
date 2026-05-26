package com.freshworks.core.shared.infra.persistent;


import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.EnabledIf;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@EnabledIfSystemProperty(named = "spring.profiles.active", matches = ".*\\.unit\\.persistent")
public class TestMongoDbQueue {

    @Test
    public void testAddMethod() throws Exception {

        MongoCollection<Document> mockedCollection = Mockito.mock(MongoCollection.class);

        MongoDbQueue mongoDbQueue = new MongoDbQueue();
        mongoDbQueue.setQueue(mockedCollection);
        mongoDbQueue.add("{\"name\": \"amit\"}");
        assertThat(mongoDbQueue.getQueueIndex().get(), is(1L));
        assertThat(mongoDbQueue.hasMoreData(), is(true));
    }

    @Test
    public void testAddListMethod() throws Exception {

        MongoCollection<Document> mockedCollection = Mockito.mock(MongoCollection.class);

        MongoDbQueue mongoDbQueue = new MongoDbQueue();
        mongoDbQueue.setQueue(mockedCollection);

        List<String> list = new ArrayList<>();
        list.add("{\"name\": \"amit\"}");
        list.add("{\"name\": \"rahul\"}");

        mongoDbQueue.add(list);
        assertThat(mongoDbQueue.getQueueIndex().get(), is(2L));
        assertThat(mongoDbQueue.hasMoreData(), is(true));
    }

    @Test
    public void testPollMethod() throws Exception {

        MongoCollection<Document> mockedCollection = Mockito.mock(MongoCollection.class);
        FindIterable<Document> mockedFindIterable = Mockito.mock(FindIterable.class);
        Document mockedDocument = Mockito.mock(Document.class);

        when(mockedCollection.find(ArgumentMatchers.any(Bson.class))).thenReturn(mockedFindIterable);
        when(mockedFindIterable.first()).thenReturn(mockedDocument);
        when(mockedDocument.get("value")).thenReturn(mockedDocument);

        when(mockedDocument.toJson()).thenReturn("{\"name\": \"amit\"}");


        MongoDbQueue mongoDbQueue = new MongoDbQueue();
        mongoDbQueue.setQueue(mockedCollection);

        assertThat(mongoDbQueue.getPopIndex(), is(0L));
        assertThat(mongoDbQueue.getQueueIndex().get(), is(0L));

        List<String> list = new ArrayList<>();
        list.add("{\"name\": \"amit\"}");
        list.add("{\"name\": \"rahul\"}");
        mongoDbQueue.add(list);

        assertThat(mongoDbQueue.getPopIndex(), is(0L));
        assertThat(mongoDbQueue.getQueueIndex().get(), is(2L));

        mongoDbQueue.poll();

        assertThat(mongoDbQueue.getPopIndex(), is(1L));
        assertThat(mongoDbQueue.getQueueIndex().get(), is(2L));
        assertThat(mongoDbQueue.hasMoreData(), is(true));
    }

    @Test
    public void testPollNElementsMethod() throws Exception{

        MongoCollection<Document> mockedCollection = Mockito.mock(MongoCollection.class);
        FindIterable<Document> mockedFindIterable = Mockito.mock(FindIterable.class);
        MongoCursor<Document> mockedCursor = Mockito.mock(MongoCursor.class);
        Document mockedDocument = Mockito.mock(Document.class);

        when(mockedCollection.find(ArgumentMatchers.any(Bson.class))).thenReturn(mockedFindIterable);
        when(mockedFindIterable.iterator()).thenReturn(mockedCursor);
        when(mockedCursor.hasNext()).thenReturn(true, true, false);
        when(mockedCursor.next()).thenReturn(mockedDocument, mockedDocument);
        when(mockedDocument.get("value")).thenReturn(mockedDocument);
        when(mockedDocument.toJson()).thenReturn("{\"name\": \"amit\"}");


        MongoDbQueue mongoDbQueue = new MongoDbQueue();
        mongoDbQueue.setQueue(mockedCollection);

        assertThat(mongoDbQueue.getPopIndex(), is(0L));
        assertThat(mongoDbQueue.getQueueIndex().get(), is(0L));

        List<String> list = new ArrayList<>();
        list.add("{\"name\": \"amit\"}");
        list.add("{\"name\": \"rahul\"}");
        mongoDbQueue.add(list);

        assertThat(mongoDbQueue.getPopIndex(), is(0L));
        assertThat(mongoDbQueue.getQueueIndex().get(), is(2L));

        mongoDbQueue.poll(2);

        assertThat(mongoDbQueue.getPopIndex(), is(2L));
        assertThat(mongoDbQueue.getQueueIndex().get(), is(2L));
        assertThat(mongoDbQueue.size(), is(2L));
        assertThat(mongoDbQueue.isEmpty(), is(true));
    }

    public void testHashMoreDataWhenThereIsDataInQueue() throws Exception {

        MongoCollection<Document> mockedCollection = Mockito.mock(MongoCollection.class);

        MongoDbQueue mongoDbQueue = new MongoDbQueue();
        mongoDbQueue.setQueue(mockedCollection);
        mongoDbQueue.add("{\"name\": \"amit\"}");
        assertThat(mongoDbQueue.getQueueIndex().get(), is(1L));
        assertThat(mongoDbQueue.hasMoreData(), is(true));
    }

    @Test
    public void testHashMoreDataWhenQueueIsEmpty() throws Exception{

        MongoCollection<Document> mockedCollection = Mockito.mock(MongoCollection.class);

        MongoDbQueue mongoDbQueue = new MongoDbQueue();
        mongoDbQueue.setQueue(mockedCollection);

        Thread s = new Thread( () ->{
            try {
                assertThat(mongoDbQueue.hasMoreData(), is(true));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        s.start();
        Thread.sleep(2000);

        mongoDbQueue.hasMoreDataLock.lock();
        assertThat(mongoDbQueue.hasMoreDataLock.getWaitQueueLength(mongoDbQueue.getHasNotMoreDataQueue()), is(1));
        mongoDbQueue.hasMoreDataLock.unlock();
    }

    @Test
    public void testHashMoreDataWhenQueueIsHasData() throws Exception{

        MongoCollection<Document> mockedCollection = Mockito.mock(MongoCollection.class);

        MongoDbQueue mongoDbQueue = new MongoDbQueue();
        mongoDbQueue.setQueue(mockedCollection);
        mongoDbQueue.add("{\"name\": \"amit\"}");
        assertThat(mongoDbQueue.hasMoreData(), is(true));
    }

    @Test
    public void testAddMethodSignalsHasMoreData() throws Exception{

        MongoCollection<Document> mockedCollection = Mockito.mock(MongoCollection.class);
        ReentrantLock mockedLock = Mockito.mock(ReentrantLock.class);
        Condition mockedCondition = Mockito.mock(Condition.class);

        MongoDbQueue mongoDbQueue = new MongoDbQueue();
        mongoDbQueue.setQueue(mockedCollection);
        mongoDbQueue.setHasMoreDataLock(mockedLock);

        Field field = MongoDbQueue.class.getDeclaredField("hasNotMoreDataQueue");
        field.setAccessible(true);
        field.set(mongoDbQueue, mockedCondition);


        mockedLock.lock();
        assertThat(mockedLock.getWaitQueueLength(mockedCondition), is(0));
        mongoDbQueue.add("{\"name\": \"amit\"}");

        verify(mockedCondition, times(1)).signalAll();
        verify(mockedCondition, times(0)).signal();
        mockedLock.unlock();

    }

    @Test
    public void testAddListSignalsHasMoreData() throws Exception{

        MongoCollection<Document> mockedCollection = Mockito.mock(MongoCollection.class);
        ReentrantLock mockedLock = Mockito.mock(ReentrantLock.class);
        Condition mockedCondition = Mockito.mock(Condition.class);

        MongoDbQueue mongoDbQueue = new MongoDbQueue();
        mongoDbQueue.setQueue(mockedCollection);
        mongoDbQueue.setHasMoreDataLock(mockedLock);

        Field field = MongoDbQueue.class.getDeclaredField("hasNotMoreDataQueue");
        field.setAccessible(true);
        field.set(mongoDbQueue, mockedCondition);


        mockedLock.lock();
        assertThat(mockedLock.getWaitQueueLength(mockedCondition), is(0));
        List<String> list = new ArrayList<>();
        list.add("{\"name\": \"amit\"}");
        list.add("{\"name\": \"rahul\"}");
        mongoDbQueue.add(list);

        verify(mockedCondition, times(1)).signalAll();
        verify(mockedCondition, times(0)).signal();
        mockedLock.unlock();

    }

    @Test
    public void testRemovePublisherSignalsHasMoreData() throws Exception{

        MongoCollection<Document> mockedCollection = Mockito.mock(MongoCollection.class);
        ReentrantLock mockedLock = Mockito.mock(ReentrantLock.class);
        Condition mockedCondition = Mockito.mock(Condition.class);

        MongoDbQueue mongoDbQueue = new MongoDbQueue();
        mongoDbQueue.setQueue(mockedCollection);
        mongoDbQueue.setHasMoreDataLock(mockedLock);

        Field field = MongoDbQueue.class.getDeclaredField("hasNotMoreDataQueue");
        field.setAccessible(true);
        field.set(mongoDbQueue, mockedCondition);

        mockedLock.lock();
        assertThat(mockedLock.getWaitQueueLength(mockedCondition), is(0));
        mongoDbQueue.removePublisher();
        verify(mockedCondition, times(1)).signalAll();
        verify(mockedCondition, times(0)).signal();
        mockedLock.unlock();
    }
}
