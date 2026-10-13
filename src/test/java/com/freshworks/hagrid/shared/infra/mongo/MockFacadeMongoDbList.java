package com.freshworks.hagrid.shared.infra.mongo;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.freshworks.hagrid.MockFacadeInterface;
import com.freshworks.hagrid.ReturnableMockTypeList;
import com.google.common.collect.Lists;
import com.mongodb.client.MongoCollection;

/**
 * Mongo twin of MockFacadeNitritedbList. Nitrite's list takes (db, namespace, listName) in its constructor; the Mongo
 * list has no such constructor (MongoService calls setList on it), so listName/namespace are kept only for parity
 * with the Nitrite facade's API.
 */
@Component
public class MockFacadeMongoDbList implements MockFacadeInterface {

    MongoCollection<Document> mongoCollection;

    @Autowired
    MockFacadeMongoDbCursor mockFacadeMongoDbCursor;

    ReturnableMockTypeList<String> listName;

    ReturnableMockTypeList<String> namespace;

    ReturnableMockTypeList<Long> addAndGetIndex = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<Long> addBulk = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<String> get = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<List<String>> getNFromStartIndex = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<MongoDbCursor> filter = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<List<String>> getGivenDocList = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<Boolean> isEndOfListReached = new ReturnableMockTypeList<>();


    @Override
    @SuppressWarnings("unchecked")
    public MockFacadeMongoDbList configure() throws Exception{

        reset();

        mongoCollection = Mockito.mock(MongoCollection.class);

        listName.add("dummy_list");
        namespace.add("dummy_namespace");
        addAndGetIndex.add(1000L);
        addBulk.add(100L);
        get.add("{\"name\":\"amit\"}");
        getNFromStartIndex.add(Lists.newArrayList("{\"name\":\"amit\"}"));
        getGivenDocList.add(Lists.newArrayList("1","2","3"));
        isEndOfListReached.add(false);
        MongoDbCursor mongoDbCursor = mockFacadeMongoDbCursor.configure().build();
        filter.add(mongoDbCursor);
        return this;
    }

    public MockFacadeMongoDbList addAndGetIndex(Long... addAndGetIndex) {
        this.addAndGetIndex.clear();
        this.addAndGetIndex.add(addAndGetIndex);
        return this;
    }

    public MockFacadeMongoDbList addAndGetIndexBulk(Long... addBulk ){
        this.addBulk.clear();
        this.addBulk.add(addBulk);
        return this;
    }

    public MockFacadeMongoDbList get(String... get){
        this.get.clear();
        this.get.add(get);
        return this;
    }

    public MockFacadeMongoDbList getNFromStartIndex(List<String>... getNFromStartIndex){
        this.getNFromStartIndex.clear();
        this.getNFromStartIndex.add(getNFromStartIndex);
        return this;
    }

    public MockFacadeMongoDbList getGivenDocList(List<String>... getGivenDocList){
        this.getGivenDocList.clear();
        this.getGivenDocList.add(getGivenDocList);
        return this;
    }

    public MockFacadeMongoDbList isEndOfListReached(Boolean... isEndOfListReached){
        this.isEndOfListReached.clear();
        this.isEndOfListReached.add(isEndOfListReached);
        return this;
    }

    public MockFacadeMongoDbList addMongoCollection(MongoCollection<Document> mongoCollection){
        this.mongoCollection = mongoCollection;
        return this;
    }

    public MockFacadeMongoDbList listName(String... listName){
        this.listName.clear();
        this.listName.add(listName);
        return this;
    }

    public MockFacadeMongoDbList namespace(String... namespace){
        this.namespace.clear();
        this.namespace.add(namespace);
        return this;
    }

    public MockFacadeMongoDbList filter(MongoDbCursor... mongoDbCursor){
        this.filter.clear();
        this.filter.add(mongoDbCursor);
        return this;
    }

    @Override
    public MongoDbList build() throws Exception {

        MongoDbList mongoDbList = new MongoDbList();
        mongoDbList.setList(mongoCollection);
        mongoDbList = Mockito.spy(mongoDbList);

        doNothing().when(mongoDbList).add(anyString());
        doAnswer(addAndGetIndex.answer()).when(mongoDbList).addAndGetIndex(anyString());
        doAnswer(addBulk.answer()).when(mongoDbList).addBulk(anyList());
        doNothing().when(mongoDbList).add(any(ArrayList.class));
        doAnswer(get.answer()).when(mongoDbList).get(anyInt());
        doAnswer(getNFromStartIndex.answer()).when(mongoDbList).get(anyInt(), anyInt());
        doAnswer(getGivenDocList.answer()).when(mongoDbList).get(anyList());
        doAnswer(isEndOfListReached.answer()).when(mongoDbList).isEndOfListReached(anyInt());
        doAnswer(filter.answer()).when(mongoDbList).filterAsset(any(), any());

        return mongoDbList;
    }
}
