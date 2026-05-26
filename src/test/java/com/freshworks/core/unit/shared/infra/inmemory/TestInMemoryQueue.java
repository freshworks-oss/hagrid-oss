package com.freshworks.core.shared.infra.inmemory;

import com.freshworks.core.shared.infra.persistent.MongoDbQueue;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.EnabledIf;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

@SpringBootTest
@EnabledIfSystemProperty(named = "spring.profiles.active", matches = ".*\\.unit\\.inmemory")
public class TestInMemoryQueue {

    @Test
    public void testAddMethod() throws Exception {

        InmemoryQueue inMemoryQueue = new InmemoryQueue();
        inMemoryQueue.setQueue(new ArrayList<>());
        inMemoryQueue.add("{\"name\": \"amit\"}");
        assertThat(inMemoryQueue.getQueueIndex().get(), is(1L));
        assertThat(inMemoryQueue.hasMoreData(), is(true));
    }

    @Test
    public void testAddListMethod() throws Exception {

        InmemoryQueue inMemoryQueue = new InmemoryQueue();
        inMemoryQueue.setQueue(new ArrayList<>());
        List<String> list = new ArrayList<>();
        list.add("{\"name\": \"amit\"}");
        list.add("{\"name\": \"rahul\"}");

        inMemoryQueue.add(list);
        assertThat(inMemoryQueue.getQueueIndex().get(), is(2L));
        assertThat(inMemoryQueue.hasMoreData(), is(true));
    }

    @Test
    public void testPollMethod() throws Exception {

        InmemoryQueue inMemoryQueue = new InmemoryQueue();
        inMemoryQueue.setQueue(new ArrayList<>());

        assertThat(inMemoryQueue.getPopIndex(), is(0L));
        assertThat(inMemoryQueue.getQueueIndex().get(), is(0L));

        List<String> list = new ArrayList<>();
        list.add("{\"name\": \"amit\"}");
        list.add("{\"name\": \"rahul\"}");
        inMemoryQueue.add(list);

        assertThat(inMemoryQueue.getPopIndex(), is(0L));
        assertThat(inMemoryQueue.getQueueIndex().get(), is(2L));

        inMemoryQueue.poll();

        assertThat(inMemoryQueue.getPopIndex(), is(1L));
        assertThat(inMemoryQueue.getQueueIndex().get(), is(2L));
        assertThat(inMemoryQueue.hasMoreData(), is(true));
    }

    @Test
    public void testPollNElementsMethod() throws Exception{

        InmemoryQueue inMemoryQueue = new InmemoryQueue();
        inMemoryQueue.setQueue(new ArrayList<>());

        assertThat(inMemoryQueue.getPopIndex(), is(0L));
        assertThat(inMemoryQueue.getQueueIndex().get(), is(0L));

        List<String> list = new ArrayList<>();
        list.add("{\"name\": \"amit\"}");
        list.add("{\"name\": \"rahul\"}");
        inMemoryQueue.add(list);

        assertThat(inMemoryQueue.getPopIndex(), is(0L));
        assertThat(inMemoryQueue.getQueueIndex().get(), is(2L));

        inMemoryQueue.poll(2);

        assertThat(inMemoryQueue.getPopIndex(), is(2L));
        assertThat(inMemoryQueue.getQueueIndex().get(), is(2L));
        assertThat(inMemoryQueue.size(), is(2L));
        assertThat(inMemoryQueue.isEmpty(), is(true));
    }

    public void testHashMoreDataWhenThereIsDataInQueue() throws Exception {

        InmemoryQueue inMemoryQueue = new InmemoryQueue();
        inMemoryQueue.setQueue(new ArrayList<>());

        inMemoryQueue.add("{\"name\": \"amit\"}");
        assertThat(inMemoryQueue.getQueueIndex().get(), is(1L));
        assertThat(inMemoryQueue.hasMoreData(), is(true));
    }

    @Test
    public void testHashMoreDataWhenQueueIsEmpty() throws Exception{

        InmemoryQueue inMemoryQueue = new InmemoryQueue();
        inMemoryQueue.setQueue(new ArrayList<>());
        Thread s = new Thread( () ->{
            try {
                assertThat(inMemoryQueue.hasMoreData(), is(true));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        s.start();
        Thread.sleep(2000);

        inMemoryQueue.hasMoreDataLock.lock();
        assertThat(inMemoryQueue.hasMoreDataLock.getWaitQueueLength(inMemoryQueue.getHasNotMoreDataQueue()), is(1));
        inMemoryQueue.hasMoreDataLock.unlock();
    }

    @Test
    public void testHashMoreDataWhenQueueIsHasData() throws Exception{

        InmemoryQueue inMemoryQueue = new InmemoryQueue();
        inMemoryQueue.setQueue(new ArrayList<>());
        inMemoryQueue.add("{\"name\": \"amit\"}");
        assertThat(inMemoryQueue.hasMoreData(), is(true));
    }

    @Test
    public void testAddMethodSignalsHasMoreData() throws Exception{

        ReentrantLock mockedLock = Mockito.mock(ReentrantLock.class);
        Condition mockedCondition = Mockito.mock(Condition.class);

        InmemoryQueue inMemoryQueue = new InmemoryQueue();
        inMemoryQueue.setQueue(new ArrayList<>());

        inMemoryQueue.setHasMoreDataLock(mockedLock);

        Field field = InmemoryQueue.class.getDeclaredField("hasNotMoreDataQueue");
        field.setAccessible(true);
        field.set(inMemoryQueue, mockedCondition);


        mockedLock.lock();
        assertThat(mockedLock.getWaitQueueLength(mockedCondition), is(0));
        inMemoryQueue.add("{\"name\": \"amit\"}");

        verify(mockedCondition, times(1)).signalAll();
        verify(mockedCondition, times(0)).signal();
        mockedLock.unlock();

    }

    @Test
    public void testAddListSignalsHasMoreData() throws Exception{

        MongoCollection<Document> mockedCollection = Mockito.mock(MongoCollection.class);
        ReentrantLock mockedLock = Mockito.mock(ReentrantLock.class);
        Condition mockedCondition = Mockito.mock(Condition.class);

        InmemoryQueue inMemoryQueue = new InmemoryQueue();
        inMemoryQueue.setQueue(new ArrayList<>());
        inMemoryQueue.setHasMoreDataLock(mockedLock);

        Field field = InmemoryQueue.class.getDeclaredField("hasNotMoreDataQueue");
        field.setAccessible(true);
        field.set(inMemoryQueue, mockedCondition);


        mockedLock.lock();
        assertThat(mockedLock.getWaitQueueLength(mockedCondition), is(0));
        List<String> list = new ArrayList<>();
        list.add("{\"name\": \"amit\"}");
        list.add("{\"name\": \"rahul\"}");
        inMemoryQueue.add(list);

        verify(mockedCondition, times(1)).signalAll();
        verify(mockedCondition, times(0)).signal();
        mockedLock.unlock();

    }

    @Test
    public void testRemovePublisherSignalsHasMoreData() throws Exception{

        MongoCollection<Document> mockedCollection = Mockito.mock(MongoCollection.class);
        ReentrantLock mockedLock = Mockito.mock(ReentrantLock.class);
        Condition mockedCondition = Mockito.mock(Condition.class);

        InmemoryQueue inMemoryQueue = new InmemoryQueue();
        inMemoryQueue.setQueue(new ArrayList<>());
        inMemoryQueue.setHasMoreDataLock(mockedLock);

        Field field = InmemoryQueue.class.getDeclaredField("hasNotMoreDataQueue");
        field.setAccessible(true);
        field.set(inMemoryQueue, mockedCondition);

        mockedLock.lock();
        assertThat(mockedLock.getWaitQueueLength(mockedCondition), is(0));
        inMemoryQueue.removePublisher();
        verify(mockedCondition, times(1)).signalAll();
        verify(mockedCondition, times(0)).signal();
        mockedLock.unlock();
    }
}
