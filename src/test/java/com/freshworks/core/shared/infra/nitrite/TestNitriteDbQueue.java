package com.freshworks.core.shared.infra.nitrite;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.dizitart.no2.Nitrite;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.freshworks.core.shared.MockFacadeSyncServiceContainer;
import com.freshworks.core.shared.NamespaceService;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.analytics.AnalyticsFactory;
import com.freshworks.core.shared.analytics.AnalyticsService;

@SpringBootTest
@EnabledIfSystemProperty(named = "spring.profiles.active", matches = "unit")
public class TestNitriteDbQueue {

    Nitrite nitriteDb;

    @Autowired
    MockFacadeSyncServiceContainer mockFacadeSyncServiceContainer;

    @Autowired
    AnalyticsFactory analyticsFactory;

    @BeforeEach
    public void setup(){
        nitriteDb = Nitrite.builder()
            .openOrCreate();

        mockFacadeSyncServiceContainer.configure().build();
    }


    @Test
    public void testAddMethod() throws Exception {

        NitriteDbQueue nitriteDbQueue = new NitriteDbQueue(nitriteDb, "some_name_space", "some_name");

        NamespaceService namespace = new NamespaceService();
        namespace.setNamespace("some_namespace");

        AnalyticsService analyticsService = analyticsFactory.getAnalyticsService("some_namespace");
        SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer.add(namespace, NamespaceService.class)
        .add(analyticsService, AnalyticsService.class)
        .build();

        nitriteDbQueue.configure(syncServiceContainer);

        nitriteDbQueue.add("{\"name\": \"amit\"}");
        assertThat(nitriteDbQueue.getQueueIndex().get(), is(1L));
        assertThat(nitriteDbQueue.hasMoreData(), is(true));
        nitriteDbQueue.delete();
    }

    @Test
    public void testAddListMethod() throws Exception {

        NitriteDbQueue nitriteDbQueue = new NitriteDbQueue(nitriteDb, "some_name_space", "some_name");

        NamespaceService namespace = new NamespaceService();
        namespace.setNamespace("some_namespace");

        AnalyticsService analyticsService = analyticsFactory.getAnalyticsService("some_namespace");
        SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer.add(namespace, NamespaceService.class)
        .add(analyticsService, AnalyticsService.class)
        .build();

        nitriteDbQueue.configure(syncServiceContainer);

        List<String> list = new ArrayList<>();
        list.add("{\"name\": \"amit\"}");
        list.add("{\"name\": \"rahul\"}");

        nitriteDbQueue.add(list);
        assertThat(nitriteDbQueue.getQueueIndex().get(), is(2L));
        assertThat(nitriteDbQueue.hasMoreData(), is(true));
        nitriteDbQueue.delete();
    }

    @Test
    public void testPollMethod() throws Exception {

        NitriteDbQueue nitriteDbQueue = new NitriteDbQueue(nitriteDb,  "some_name_space","some_name");

        NamespaceService namespace = new NamespaceService();
        namespace.setNamespace("some_namespace");

        AnalyticsService analyticsService = analyticsFactory.getAnalyticsService("some_namespace");
        SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer.add(namespace, NamespaceService.class)
        .add(analyticsService, AnalyticsService.class)
        .build();

        nitriteDbQueue.configure(syncServiceContainer);

        assertThat(nitriteDbQueue.getPopIndex(), is(0L));
        assertThat(nitriteDbQueue.getQueueIndex().get(), is(0L));

        List<String> list = new ArrayList<>();
        list.add("{\"name\": \"amit\"}");
        list.add("{\"name\": \"rahul\"}");
        nitriteDbQueue.add(list);

        assertThat(nitriteDbQueue.getPopIndex(), is(0L));
        assertThat(nitriteDbQueue.getQueueIndex().get(), is(2L));

        nitriteDbQueue.poll();

        assertThat(nitriteDbQueue.getPopIndex(), is(1L));
        assertThat(nitriteDbQueue.getQueueIndex().get(), is(2L));
        assertThat(nitriteDbQueue.hasMoreData(), is(true));

        nitriteDbQueue.delete();
    }

    @Test
    public void testPollNElementsMethod() throws Exception{

        NitriteDbQueue nitriteDbQueue = new NitriteDbQueue(nitriteDb,  "some_name_space","some_name");

        NamespaceService namespace = new NamespaceService();
        namespace.setNamespace("some_namespace");

        AnalyticsService analyticsService = analyticsFactory.getAnalyticsService("some_namespace");
        SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer.add(namespace, NamespaceService.class)
        .add(analyticsService, AnalyticsService.class)
        .build();

        nitriteDbQueue.configure(syncServiceContainer);


        assertThat(nitriteDbQueue.getPopIndex(), is(0L));
        assertThat(nitriteDbQueue.getQueueIndex().get(), is(0L));

        List<String> list = new ArrayList<>();
        list.add("{\"name\": \"amit\"}");
        list.add("{\"name\": \"rahul\"}");
        nitriteDbQueue.add(list);

        assertThat(nitriteDbQueue.getPopIndex(), is(0L));
        assertThat(nitriteDbQueue.getQueueIndex().get(), is(2L));

        nitriteDbQueue.poll(2);

        assertThat(nitriteDbQueue.getPopIndex(), is(2L));
        assertThat(nitriteDbQueue.getQueueIndex().get(), is(2L));
        assertThat(nitriteDbQueue.size(), is(2L));
        assertThat(nitriteDbQueue.isEmpty(), is(true));

        nitriteDbQueue.delete();
    }

    public void testHashMoreDataWhenThereIsDataInQueue() throws Exception {

        NitriteDbQueue nitriteDbQueue = new NitriteDbQueue(nitriteDb,  "some_name_space","some_name");

        NamespaceService namespace = new NamespaceService();
        namespace.setNamespace("some_namespace");

        AnalyticsService analyticsService = analyticsFactory.getAnalyticsService("some_namespace");
        SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer.add(namespace, NamespaceService.class)
        .add(analyticsService, AnalyticsService.class)
        .build();

        nitriteDbQueue.configure(syncServiceContainer);

        nitriteDbQueue.add("{\"name\": \"amit\"}");
        assertThat(nitriteDbQueue.getQueueIndex().get(), is(1L));
        assertThat(nitriteDbQueue.hasMoreData(), is(true));

        nitriteDbQueue.delete();
    }

    @Test
    public void testHashMoreDataWhenQueueIsEmpty() throws Exception{

        NitriteDbQueue nitriteDbQueue = new NitriteDbQueue(nitriteDb,  "some_name_space","some_name");

        NamespaceService namespace = new NamespaceService();
        namespace.setNamespace("some_namespace");

        AnalyticsService analyticsService = analyticsFactory.getAnalyticsService("some_namespace");
        SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer.add(namespace, NamespaceService.class)
        .add(analyticsService, AnalyticsService.class)
        .build();

        nitriteDbQueue.configure(syncServiceContainer);

        Thread s = new Thread( () ->{
            try {
                assertThat(nitriteDbQueue.hasMoreData(), is(true));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        s.start();
        Thread.sleep(2000);

        nitriteDbQueue.hasMoreDataLock.lock();
        assertThat(nitriteDbQueue.hasMoreDataLock.getWaitQueueLength(nitriteDbQueue.getHasNotMoreDataQueue()), is(1));
        nitriteDbQueue.hasMoreDataLock.unlock();

        nitriteDbQueue.delete();
    }

    @Test
    public void testHashMoreDataWhenQueueIsHasData() throws Exception{

        NitriteDbQueue nitriteDbQueue = new NitriteDbQueue(nitriteDb, "some_name_space", "some_name");

        NamespaceService namespace = new NamespaceService();
        namespace.setNamespace("some_namespace");

        AnalyticsService analyticsService = analyticsFactory.getAnalyticsService("some_namespace");
        SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer.add(namespace, NamespaceService.class)
        .add(analyticsService, AnalyticsService.class)
        .build();

        nitriteDbQueue.configure(syncServiceContainer);

        nitriteDbQueue.add("{\"name\": \"amit\"}");
        assertThat(nitriteDbQueue.hasMoreData(), is(true));

        nitriteDbQueue.delete();
    }

    @Test
    public void testAddMethodSignalsHasMoreData() throws Exception{

        ReentrantLock mockedLock = Mockito.mock(ReentrantLock.class);
        Condition mockedCondition = Mockito.mock(Condition.class);

        NitriteDbQueue nitriteDbQueue = new NitriteDbQueue(nitriteDb,  "some_name_space","some_name");

        NamespaceService namespace = new NamespaceService();
        namespace.setNamespace("some_namespace");

        AnalyticsService analyticsService = analyticsFactory.getAnalyticsService("some_namespace");
        SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer.add(namespace, NamespaceService.class)
        .add(analyticsService, AnalyticsService.class)
        .build();

        nitriteDbQueue.configure(syncServiceContainer);

        nitriteDbQueue.setHasMoreDataLock(mockedLock);

        Field field = NitriteDbQueue.class.getDeclaredField("hasNotMoreDataQueue");
        field.setAccessible(true);
        field.set(nitriteDbQueue, mockedCondition);


        mockedLock.lock();
        assertThat(mockedLock.getWaitQueueLength(mockedCondition), is(0));
        nitriteDbQueue.add("{\"name\": \"amit\"}");

        verify(mockedCondition, times(1)).signalAll();
        verify(mockedCondition, times(0)).signal();
        mockedLock.unlock();

        nitriteDbQueue.delete();

    }

    @Test
    public void testAddListSignalsHasMoreData() throws Exception{

        ReentrantLock mockedLock = Mockito.mock(ReentrantLock.class);
        Condition mockedCondition = Mockito.mock(Condition.class);

        NitriteDbQueue nitriteDbQueue = new NitriteDbQueue(nitriteDb,  "some_name_space","some_name");

        NamespaceService namespace = new NamespaceService();
        namespace.setNamespace("some_namespace");

        AnalyticsService analyticsService = analyticsFactory.getAnalyticsService("some_namespace");
        SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer.add(namespace, NamespaceService.class)
        .add(analyticsService, AnalyticsService.class)
        .build();

        nitriteDbQueue.configure(syncServiceContainer);

        nitriteDbQueue.setHasMoreDataLock(mockedLock);

        Field field = NitriteDbQueue.class.getDeclaredField("hasNotMoreDataQueue");
        field.setAccessible(true);
        field.set(nitriteDbQueue, mockedCondition);


        mockedLock.lock();
        assertThat(mockedLock.getWaitQueueLength(mockedCondition), is(0));
        List<String> list = new ArrayList<>();
        list.add("{\"name\": \"amit\"}");
        list.add("{\"name\": \"rahul\"}");
        nitriteDbQueue.add(list);

        verify(mockedCondition, times(1)).signalAll();
        verify(mockedCondition, times(0)).signal();
        mockedLock.unlock();

        nitriteDbQueue.delete();

    }

    @Test
    public void testRemovePublisherSignalsHasMoreData() throws Exception{

        ReentrantLock mockedLock = Mockito.mock(ReentrantLock.class);
        Condition mockedCondition = Mockito.mock(Condition.class);

        NitriteDbQueue nitriteDbQueue = new NitriteDbQueue(nitriteDb,  "some_name_space","some_name");

        NamespaceService namespace = new NamespaceService();
        namespace.setNamespace("some_namespace");

        AnalyticsService analyticsService = analyticsFactory.getAnalyticsService("some_namespace");
        SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer.add(namespace, NamespaceService.class)
        .add(analyticsService, AnalyticsService.class)
        .build();

        nitriteDbQueue.configure(syncServiceContainer);
        
        nitriteDbQueue.setHasMoreDataLock(mockedLock);

        Field field = NitriteDbQueue.class.getDeclaredField("hasNotMoreDataQueue");
        field.setAccessible(true);
        field.set(nitriteDbQueue, mockedCondition);

        mockedLock.lock();
        assertThat(mockedLock.getWaitQueueLength(mockedCondition), is(0));
        nitriteDbQueue.removePublisher();
        verify(mockedCondition, times(1)).signalAll();
        verify(mockedCondition, times(0)).signal();
        mockedLock.unlock();

        nitriteDbQueue.delete();
    }
}
