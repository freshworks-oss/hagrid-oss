package com.freshworks.core.shared.infra.nitrite;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.ArrayList;
import java.util.List;

import org.dizitart.no2.Nitrite;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.freshworks.core.shared.MockFacadeSyncServiceContainer;
import com.freshworks.core.shared.NamespaceService;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.analytics.AnalyticsFactory;
import com.freshworks.core.shared.analytics.AnalyticsService;
import com.freshworks.core.shared.infra.InfraConfigService;
import com.freshworks.core.shared.sync.MockFacadeSyncService;

@SpringBootTest
@EnabledIfSystemProperty(named = "spring.profiles.active", matches = ".*\\.unit\\.nitrite")
public class TestNitriteDbList {

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
    public void testAddMethod() throws Exception{

        NitriteDbList nitriteDbList = new NitriteDbList(nitriteDb, "some_name_space","some_name");

        NamespaceService namespace = new NamespaceService();
        namespace.setNamespace("some_namespace");

        AnalyticsService analyticsService = analyticsFactory.getAnalyticsService("some_namespace");
        SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer.add(namespace, NamespaceService.class)
        .add(analyticsService, AnalyticsService.class)
        .build();

        nitriteDbList.configure(syncServiceContainer);
        
        nitriteDbList.add("{\"name\": \"amit\"}");
        assertThat(nitriteDbList.getListIndex().get(), is(1L));
        nitriteDbList.delete();
    }

    @Test
    public void testAddListMethod() throws Exception{
        NitriteDbList nitriteDbList = new NitriteDbList(nitriteDb, "some_name_space","some_name");

        NamespaceService namespace = new NamespaceService();
        namespace.setNamespace("some_namespace");

        AnalyticsService analyticsService = analyticsFactory.getAnalyticsService("some_namespace");
        SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer.add(namespace, NamespaceService.class)
        .add(analyticsService, AnalyticsService.class)
        .build();

        nitriteDbList.configure(syncServiceContainer);

        ArrayList<String> list = new ArrayList<>();
        list.add("{\"name\": \"amit\"}");
        list.add("{\"name\": \"rahul\"}");

        nitriteDbList.add(list);
        assertThat(nitriteDbList.getListIndex().get(), is(2L));
        nitriteDbList.delete();
    }

    @Test
    public void testAddBulkMethod() throws Exception{

        NitriteDbList nitriteDbList = new NitriteDbList(nitriteDb,  "some_name_space","some_name");

        NamespaceService namespace = new NamespaceService();
        namespace.setNamespace("some_namespace");

        AnalyticsService analyticsService = analyticsFactory.getAnalyticsService("some_namespace");
        SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer.add(namespace, NamespaceService.class)
        .add(analyticsService, AnalyticsService.class)
        .build();

        nitriteDbList.configure(syncServiceContainer);

        ArrayList<String> list = new ArrayList<>();

        for(int i=0; i<100; i++){
            String s = "{\"name\": \"" + i + " \"}";
            list.add(s);
        }

        Long longList = nitriteDbList.addBulk(list);
        assertThat(longList, is(100L));

        nitriteDbList.delete();
    }

    @Test
    public void testAddAndGetIndexMethod() throws Exception{
        NitriteDbList nitriteDbList = new NitriteDbList(nitriteDb,  "some_name_space","some_name");

        NamespaceService namespace = new NamespaceService();
        namespace.setNamespace("some_namespace");

        AnalyticsService analyticsService = analyticsFactory.getAnalyticsService("some_namespace");
        SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer.add(namespace, NamespaceService.class)
        .add(analyticsService, AnalyticsService.class)
        .build();

        nitriteDbList.configure(syncServiceContainer);

        Long index = nitriteDbList.addAndGetIndex("{\"name\": \"amit\"}");
        assertThat(index, is(0L));
        nitriteDbList.delete();
    }


    @Test
    public void testGetByIndexMethod() throws Exception {
        NitriteDbList nitriteDbList = new NitriteDbList(nitriteDb,  "some_name_space","some_name");

        NamespaceService namespace = new NamespaceService();
        namespace.setNamespace("some_namespace");

        AnalyticsService analyticsService = analyticsFactory.getAnalyticsService("some_namespace");
        SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer.add(namespace, NamespaceService.class)
        .add(analyticsService, AnalyticsService.class)
        .build();

        nitriteDbList.configure(syncServiceContainer);

        nitriteDbList.add("{\"name\": \"amit\"}");
        nitriteDbList.add("{\"name\": \"rahul\"}");
        String s = nitriteDbList.get(0);
        assertThat(s.contains("amit"), is(true));
        nitriteDbList.delete();
    }

    @Test
    public void testGetByIndexNElementsMethod() throws Exception {
        NitriteDbList nitriteDbList = new NitriteDbList(nitriteDb,  "some_name_space","some_name");

        NamespaceService namespace = new NamespaceService();
        namespace.setNamespace("some_namespace");

        AnalyticsService analyticsService = analyticsFactory.getAnalyticsService("some_namespace");
        SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer.add(namespace, NamespaceService.class)
        .add(analyticsService, AnalyticsService.class)
        .build();

        nitriteDbList.configure(syncServiceContainer);

        nitriteDbList.add("{\"name\": \"amit\"}");
        nitriteDbList.add("{\"name\": \"rahul\"}");
        nitriteDbList.add("{\"name\": \"deepak\"}");
        nitriteDbList.add("{\"name\": \"praveen\"}");


        List<String> sList = nitriteDbList.get(0, 2);
        assertThat(sList.size(), is(2));
        assertThat(sList.get(0).contains("amit"), is(true));
        assertThat(sList.get(1).contains("rahul"), is(true));
        nitriteDbList.delete();
    }

    @Test
    public void testGetByDocumentIdNElementsMethod() throws Exception {

        NitriteDbList nitriteDbList = new NitriteDbList(nitriteDb,  "some_name_space","some_name");

        NamespaceService namespace = new NamespaceService();
        namespace.setNamespace("some_namespace");

        AnalyticsService analyticsService = analyticsFactory.getAnalyticsService("some_namespace");
        SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer.add(namespace, NamespaceService.class)
        .add(analyticsService, AnalyticsService.class)
        .build();

        nitriteDbList.configure(syncServiceContainer);

        nitriteDbList.add("{\"name\": \"amit\"}");
        nitriteDbList.add("{\"name\": \"rahul\"}");
        nitriteDbList.add("{\"name\": \"deepak\"}");
        nitriteDbList.add("{\"name\": \"praveen\"}");

        List<Long> documentIdList = new ArrayList<>();
        documentIdList.add(1L);
        documentIdList.add(2L);

        List<String> sList = nitriteDbList.get(documentIdList);
        assertThat(sList.size(), is(2));
        assertThat(sList.get(0).contains("rahul"), is(true));
        assertThat(sList.get(1).contains("deepak"), is(true));
        nitriteDbList.delete();
    }

    @Test
    public void testIsEndOfListReachedMethod() throws Exception{
        NitriteDbList nitriteDbList = new NitriteDbList(nitriteDb,  "some_name_space","some_name");

        NamespaceService namespace = new NamespaceService();
        namespace.setNamespace("some_namespace");

        AnalyticsService analyticsService = analyticsFactory.getAnalyticsService("some_namespace");
        SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer.add(namespace, NamespaceService.class)
        .add(analyticsService, AnalyticsService.class)
        .build();

        nitriteDbList.configure(syncServiceContainer);

        nitriteDbList.add("{\"name\": \"amit\"}");
        nitriteDbList.add("{\"name\": \"rahul\"}");
        assertThat(nitriteDbList.isEndOfListReached(0), is(false));
        assertThat(nitriteDbList.isEndOfListReached(1), is(false));
        assertThat(nitriteDbList.isEndOfListReached(2), is(true));
        nitriteDbList.delete();

    }

}
