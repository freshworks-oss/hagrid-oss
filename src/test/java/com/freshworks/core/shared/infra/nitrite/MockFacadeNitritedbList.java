package com.freshworks.core.shared.infra.nitrite;

import com.freshworks.core.MockFacadeInterface;
import com.freshworks.core.ReturnableMockTypeList;
import com.freshworks.core.shared.infra.InfraDbCursor;
import com.freshworks.core.shared.infra.nitrite.NitriteDbList;
import com.google.common.collect.Lists;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import org.dizitart.no2.Nitrite;
import org.dizitart.no2.rocksdb.RocksDBModule;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@Component
public class MockFacadeNitritedbList implements MockFacadeInterface {

    Nitrite nitriteDb;

    @Autowired
    MockFacadeNitriteDbCursor mockFacadeNitriteDbCursor;

    ReturnableMockTypeList<String> listName;
    
    ReturnableMockTypeList<String> namespace;

    ReturnableMockTypeList<Long> addAndGetIndex = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<Long> addBulk = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<String> get = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<List<String>> getNFromStartIndex = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<InfraDbCursor> filter = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<List<String>> getGivenDocList = new ReturnableMockTypeList<>();


    ReturnableMockTypeList<Boolean> isEndOfListReached = new ReturnableMockTypeList<>();


    @Override
    public MockFacadeNitritedbList configure() throws Exception{

        reset();

        nitriteDb = Nitrite.builder()
            .openOrCreate();
        
        listName.add("dummy_list");
        namespace.add("dummy_namespace");
        addAndGetIndex.add(1000L);
        addBulk.add(100L);
        get.add("{\"name\":\"amit\"}");
        getNFromStartIndex.add(Lists.newArrayList("{\"name\":\"amit\"}"));
        getGivenDocList.add(Lists.newArrayList("1","2","3"));

        NitriteDbCursor nitriteDbCursor = mockFacadeNitriteDbCursor.configure().build();
        filter.add(nitriteDbCursor);
        return this;
    }


    public MockFacadeNitritedbList addAndGetIndex(Long... addAndGetIndex) {
        this.addAndGetIndex.clear();
        this.addAndGetIndex.add(addAndGetIndex);
        return this;
    }

    public MockFacadeNitritedbList addAndGetIndexBulk(Long... addBulk ){
        this.addBulk.clear();
        this.addBulk.add(addBulk);
        return this;
    }

    public MockFacadeNitritedbList get(String... get){
        this.get.clear();
        this.get.add(get);
        return this;
    }

    public MockFacadeNitritedbList getNFromStartIndex(List<String>... getNFromStartIndex){
        this.getNFromStartIndex.clear();
        this.getNFromStartIndex.add(getNFromStartIndex);
        return this;
    }

    public MockFacadeNitritedbList getGivenDocList(List<String>... getGivenDocList){
        this.getGivenDocList.clear();
        this.getGivenDocList.add(getGivenDocList);
        return this;
    }

    public MockFacadeNitritedbList isEndOfListReached(Boolean... isEndOfListReached){
        this.isEndOfListReached.clear();
        this.isEndOfListReached.add(isEndOfListReached);
        return this;
    }


    public MockFacadeNitritedbList addNitriteDataSource(Nitrite nitriteDb){
        this.nitriteDb = nitriteDb;
        return this;
    }

    public MockFacadeNitritedbList listName(String... listName){
        this.listName.clear();;
        this.listName.add(listName);
        return this;
    }

    public MockFacadeNitritedbList namespace(String... namespace){
        this.namespace.clear();;
        this.namespace.add(namespace);
        return this;
    }

    public MockFacadeNitritedbList filter(NitriteDbCursor... nitriteDbCursor){
        this.filter.clear();;
        this.filter.add(nitriteDbCursor);
        return this;
    }

    @Override
    public NitriteDbList build() throws Exception {

        NitriteDbList nitriteDbList = new NitriteDbList(nitriteDb, namespace.next(),listName.next());
        nitriteDbList = Mockito.spy(nitriteDbList);

        doNothing().when(nitriteDbList).add(anyString());

        doAnswer(addAndGetIndex.answer()).when(nitriteDbList).addAndGetIndex(anyString());

        doAnswer(addBulk.answer()).when(nitriteDbList).addBulk(anyList());

        doNothing().when(nitriteDbList).add(any(ArrayList.class));

        doAnswer(get.answer()).when(nitriteDbList).get(anyInt());

        doAnswer(getNFromStartIndex.answer()).when(nitriteDbList).get(anyInt(), anyInt());

        doAnswer(getGivenDocList.answer()).when(nitriteDbList).get(anyList());

        doAnswer(isEndOfListReached.answer()).when(nitriteDbList).isEndOfListReached(anyInt());

        doAnswer(filter.answer()).when(nitriteDbList).filter(any());
        
        return nitriteDbList;
    }

}
