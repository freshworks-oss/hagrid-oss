package com.freshworks.hagrid.shared.infra.mongo;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;

import java.util.List;

import org.bson.Document;
import org.mockito.Mockito;
import org.springframework.stereotype.Component;

import com.freshworks.hagrid.MockFacadeInterface;
import com.freshworks.hagrid.ReturnableMockTypeList;
import com.google.common.collect.Lists;
import com.mongodb.client.MongoCollection;

/** Mongo twin of MockFacadeNitritedbKeyValue (collection is a Mockito mock, see MockFacadeMongoDbQueue). */
@Component
public class MockFacadeMongoDbKeyValue implements MockFacadeInterface {

    ReturnableMockTypeList<String> get = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<List<String>> getList = new ReturnableMockTypeList<>();

    MongoCollection<Document> mongoCollection;

    @Override
    @SuppressWarnings("unchecked")
    public MockFacadeMongoDbKeyValue configure(){

        reset();

        mongoCollection = Mockito.mock(MongoCollection.class);

        get.add("{\"name\":\"amit\"}");
        getList.add(Lists.newArrayList("{\"name\":\"amit\"}", "{\"name\":\"rahul\"}", "{\"name\":\"deepak\"}"));
        return this;
    }

    public MockFacadeMongoDbKeyValue get(String... get) {
        this.get.clear();
        this.get.add(get);
        return this;
    }

    public MockFacadeMongoDbKeyValue getList(List<String>... getList){
        this.getList.clear();
        this.getList.add(getList);
        return this;
    }

    public MockFacadeMongoDbKeyValue addMongoCollection(MongoCollection<Document> mongoCollection){
        this.mongoCollection = mongoCollection;
        return this;
    }

    @Override
    public MongoDbKeyValue build() throws Exception {
        MongoDbKeyValue mongoDbKeyValue = new MongoDbKeyValue();
        mongoDbKeyValue.setKeyValue(mongoCollection);
        mongoDbKeyValue = Mockito.spy(mongoDbKeyValue);

        doNothing().when(mongoDbKeyValue).set(anyString(), anyString());
        doAnswer(get.answer()).when(mongoDbKeyValue).get(anyString());
        doNothing().when(mongoDbKeyValue).putList(anyString(), anyList());
        doNothing().when(mongoDbKeyValue).putList(anyString(), anyString());
        doAnswer(getList.answer()).when(mongoDbKeyValue).getList(anyString());
        return mongoDbKeyValue;
    }
}
