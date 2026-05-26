package com.freshworks.core.shared.infra.h2;

import com.freshworks.core.MockFacadeInterface;
import com.freshworks.core.ReturnableMockTypeList;
import com.google.common.collect.Lists;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.mockito.Mockito;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;

@Component
public class MockFacadeH2dbList implements MockFacadeInterface {

    HikariDataSource hikariDataSource;

    ReturnableMockTypeList<String> listName;
    ReturnableMockTypeList<String> namespace;

    ReturnableMockTypeList<Long> addAndGetIndex = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<List<Long>> addAndGetIndexBulk = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<String> get = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<List<String>> getNFromStartIndex = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<List<String>> getGivenDocList = new ReturnableMockTypeList<>();


    ReturnableMockTypeList<Boolean> isEndOfListReached = new ReturnableMockTypeList<>();


    @Override
    public MockFacadeH2dbList configure(){

        reset();


        HikariConfig config = new HikariConfig();
        String dbString =  "jdbc:h2:mem:testdb";
        config.setJdbcUrl(dbString);
        config.setUsername("");
        config.setPassword("");
        config.setIdleTimeout(60000); // 60 seconds
        hikariDataSource = new HikariDataSource(config);

        listName.add("dummy_list");
        namespace.add("dummy_namespace");
        addAndGetIndex.add(1000L);
        addAndGetIndexBulk.add(Lists.newArrayList(100L,20L,3L,4000L));
        get.add("{\"name\":\"amit\"}");
        getNFromStartIndex.add(Lists.newArrayList("{\"name\":\"amit\"}"));
        getGivenDocList.add(Lists.newArrayList("1","2","3"));
        return this;
    }


    public MockFacadeH2dbList addAndGetIndex(Long... addAndGetIndex) {
        this.addAndGetIndex.clear();
        this.addAndGetIndex.add(addAndGetIndex);
        return this;
    }

    public MockFacadeH2dbList addAndGetIndexBulk(List<Long>... addAndGetIndexBulk ){
        this.addAndGetIndexBulk.clear();
        this.addAndGetIndexBulk.add(addAndGetIndexBulk);
        return this;
    }

    public MockFacadeH2dbList get(String... get){
        this.get.clear();
        this.get.add(get);
        return this;
    }

    public MockFacadeH2dbList getNFromStartIndex(List<String>... getNFromStartIndex){
        this.getNFromStartIndex.clear();
        this.getNFromStartIndex.add(getNFromStartIndex);
        return this;
    }

    public MockFacadeH2dbList getGivenDocList(List<String>... getGivenDocList){
        this.getGivenDocList.clear();
        this.getGivenDocList.add(getGivenDocList);
        return this;
    }

    public MockFacadeH2dbList isEndOfListReached(Boolean... isEndOfListReached){
        this.isEndOfListReached.clear();
        this.isEndOfListReached.add(isEndOfListReached);
        return this;
    }


    public MockFacadeH2dbList addHikariDataSource(HikariDataSource hikariDataSource){
        this.hikariDataSource = hikariDataSource;
        return this;
    }

    public MockFacadeH2dbList listName(String... listName){
        this.listName.clear();;
        this.listName.add(listName);
        return this;
    }

    public MockFacadeH2dbList namespace(String... namespace){
        this.namespace.clear();;
        this.namespace.add(namespace);
        return this;
    }

    @Override
    public H2DbList build() throws Exception {

        H2DbList h2DbList = new H2DbList(hikariDataSource, namespace.next(),listName.next());
        h2DbList = Mockito.spy(h2DbList);

        doNothing().when(h2DbList).add(anyString());

        doAnswer(addAndGetIndex.answer()).when(h2DbList).addAndGetIndex(anyString());

        doAnswer(addAndGetIndexBulk.answer()).when(h2DbList).addAndGetIndexBulk(anyList());

        doNothing().when(h2DbList).add(any(ArrayList.class));

        doAnswer(get.answer()).when(h2DbList).get(anyInt());

        doAnswer(getNFromStartIndex.answer()).when(h2DbList).get(anyInt(), anyInt());

        doAnswer(getGivenDocList.answer()).when(h2DbList).get(anyList());

        doAnswer(isEndOfListReached.answer()).when(h2DbList).isEndOfListReached(anyInt());

        return h2DbList;
    }

}
