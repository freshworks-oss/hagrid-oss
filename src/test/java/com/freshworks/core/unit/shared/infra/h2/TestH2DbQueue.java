package com.freshworks.core.shared.infra.h2;


import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.*;

@SpringBootTest
@EnabledIfSystemProperty(named = "spring.profiles.active", matches = ".*\\.unit\\.h2")
public class TestH2DbQueue {

    HikariDataSource hikariDataSource;
    @BeforeEach
    public void setup(){

        HikariConfig config = new HikariConfig();
        String dbString =  "jdbc:h2:mem:testdb";
        config.setJdbcUrl(dbString);
        config.setUsername("");
        config.setPassword("");
        config.setIdleTimeout(60000); // 60 seconds
        hikariDataSource = new HikariDataSource(config);
    }


    @Test
    public void testAddMethod() throws Exception {

        H2DbQueue h2DbQueue = new H2DbQueue(hikariDataSource, "some_name_space", "some_name");
        h2DbQueue.add("{\"name\": \"amit\"}");
        assertThat(h2DbQueue.getQueueIndex().get(), is(1L));
        assertThat(h2DbQueue.hasMoreData(), is(true));
        h2DbQueue.delete();
    }

    @Test
    public void testAddListMethod() throws Exception {

        H2DbQueue h2DbQueue = new H2DbQueue(hikariDataSource, "some_name_space", "some_name");
        List<String> list = new ArrayList<>();
        list.add("{\"name\": \"amit\"}");
        list.add("{\"name\": \"rahul\"}");

        h2DbQueue.add(list);
        assertThat(h2DbQueue.getQueueIndex().get(), is(2L));
        assertThat(h2DbQueue.hasMoreData(), is(true));
        h2DbQueue.delete();
    }

    @Test
    public void testPollMethod() throws Exception {

        H2DbQueue h2DbQueue = new H2DbQueue(hikariDataSource,  "some_name_space","some_name");

        assertThat(h2DbQueue.getPopIndex(), is(0L));
        assertThat(h2DbQueue.getQueueIndex().get(), is(0L));

        List<String> list = new ArrayList<>();
        list.add("{\"name\": \"amit\"}");
        list.add("{\"name\": \"rahul\"}");
        h2DbQueue.add(list);

        assertThat(h2DbQueue.getPopIndex(), is(0L));
        assertThat(h2DbQueue.getQueueIndex().get(), is(2L));

        h2DbQueue.poll();

        assertThat(h2DbQueue.getPopIndex(), is(1L));
        assertThat(h2DbQueue.getQueueIndex().get(), is(2L));
        assertThat(h2DbQueue.hasMoreData(), is(true));

        h2DbQueue.delete();
    }

    @Test
    public void testPollNElementsMethod() throws Exception{

        H2DbQueue h2DbQueue = new H2DbQueue(hikariDataSource,  "some_name_space","some_name");

        assertThat(h2DbQueue.getPopIndex(), is(0L));
        assertThat(h2DbQueue.getQueueIndex().get(), is(0L));

        List<String> list = new ArrayList<>();
        list.add("{\"name\": \"amit\"}");
        list.add("{\"name\": \"rahul\"}");
        h2DbQueue.add(list);

        assertThat(h2DbQueue.getPopIndex(), is(0L));
        assertThat(h2DbQueue.getQueueIndex().get(), is(2L));

        h2DbQueue.poll(2);

        assertThat(h2DbQueue.getPopIndex(), is(2L));
        assertThat(h2DbQueue.getQueueIndex().get(), is(2L));
        assertThat(h2DbQueue.size(), is(2L));
        assertThat(h2DbQueue.isEmpty(), is(true));

        h2DbQueue.delete();
    }

    public void testHashMoreDataWhenThereIsDataInQueue() throws Exception {

        H2DbQueue h2DbQueue = new H2DbQueue(hikariDataSource,  "some_name_space","some_name");
        h2DbQueue.add("{\"name\": \"amit\"}");
        assertThat(h2DbQueue.getQueueIndex().get(), is(1L));
        assertThat(h2DbQueue.hasMoreData(), is(true));

        h2DbQueue.delete();
    }

    @Test
    public void testHashMoreDataWhenQueueIsEmpty() throws Exception{

        H2DbQueue h2DbQueue = new H2DbQueue(hikariDataSource,  "some_name_space","some_name");

        Thread s = new Thread( () ->{
            try {
                assertThat(h2DbQueue.hasMoreData(), is(true));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        s.start();
        Thread.sleep(2000);

        h2DbQueue.hasMoreDataLock.lock();
        assertThat(h2DbQueue.hasMoreDataLock.getWaitQueueLength(h2DbQueue.getHasNotMoreDataQueue()), is(1));
        h2DbQueue.hasMoreDataLock.unlock();

        h2DbQueue.delete();
    }

    @Test
    public void testHashMoreDataWhenQueueIsHasData() throws Exception{

        H2DbQueue h2DbQueue = new H2DbQueue(hikariDataSource, "some_name_space", "some_name");
        h2DbQueue.add("{\"name\": \"amit\"}");
        assertThat(h2DbQueue.hasMoreData(), is(true));

        h2DbQueue.delete();
    }

    @Test
    public void testAddMethodSignalsHasMoreData() throws Exception{

        ReentrantLock mockedLock = Mockito.mock(ReentrantLock.class);
        Condition mockedCondition = Mockito.mock(Condition.class);

        H2DbQueue h2DbQueue = new H2DbQueue(hikariDataSource,  "some_name_space","some_name");
        h2DbQueue.setHasMoreDataLock(mockedLock);

        Field field = H2DbQueue.class.getDeclaredField("hasNotMoreDataQueue");
        field.setAccessible(true);
        field.set(h2DbQueue, mockedCondition);


        mockedLock.lock();
        assertThat(mockedLock.getWaitQueueLength(mockedCondition), is(0));
        h2DbQueue.add("{\"name\": \"amit\"}");

        verify(mockedCondition, times(1)).signalAll();
        verify(mockedCondition, times(0)).signal();
        mockedLock.unlock();

        h2DbQueue.delete();

    }

    @Test
    public void testAddListSignalsHasMoreData() throws Exception{

        ReentrantLock mockedLock = Mockito.mock(ReentrantLock.class);
        Condition mockedCondition = Mockito.mock(Condition.class);

        H2DbQueue h2DbQueue = new H2DbQueue(hikariDataSource,  "some_name_space","some_name");
        h2DbQueue.setHasMoreDataLock(mockedLock);

        Field field = H2DbQueue.class.getDeclaredField("hasNotMoreDataQueue");
        field.setAccessible(true);
        field.set(h2DbQueue, mockedCondition);


        mockedLock.lock();
        assertThat(mockedLock.getWaitQueueLength(mockedCondition), is(0));
        List<String> list = new ArrayList<>();
        list.add("{\"name\": \"amit\"}");
        list.add("{\"name\": \"rahul\"}");
        h2DbQueue.add(list);

        verify(mockedCondition, times(1)).signalAll();
        verify(mockedCondition, times(0)).signal();
        mockedLock.unlock();

        h2DbQueue.delete();

    }

    @Test
    public void testRemovePublisherSignalsHasMoreData() throws Exception{

        ReentrantLock mockedLock = Mockito.mock(ReentrantLock.class);
        Condition mockedCondition = Mockito.mock(Condition.class);

        H2DbQueue h2DbQueue = new H2DbQueue(hikariDataSource,  "some_name_space","some_name");
        h2DbQueue.setHasMoreDataLock(mockedLock);

        Field field = H2DbQueue.class.getDeclaredField("hasNotMoreDataQueue");
        field.setAccessible(true);
        field.set(h2DbQueue, mockedCondition);

        mockedLock.lock();
        assertThat(mockedLock.getWaitQueueLength(mockedCondition), is(0));
        h2DbQueue.removePublisher();
        verify(mockedCondition, times(1)).signalAll();
        verify(mockedCondition, times(0)).signal();
        mockedLock.unlock();

        h2DbQueue.delete();
    }
}
