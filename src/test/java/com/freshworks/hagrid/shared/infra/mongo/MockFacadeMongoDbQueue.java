package com.freshworks.hagrid.shared.infra.mongo;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;

import java.util.Arrays;
import java.util.List;

import org.bson.Document;
import org.mockito.Mockito;
import org.springframework.stereotype.Component;

import com.freshworks.hagrid.MockFacadeInterface;
import com.freshworks.hagrid.ReturnableMockTypeList;
import com.mongodb.client.MongoCollection;

/**
 * Mongo twin of MockFacadeNitritedbQueue. Nitrite's facade opens an in-memory Nitrite; Mongo has no in-memory mode,
 * so the collection is a Mockito mock and no database is needed. Every public method used by callers is stubbed.
 */
@Component
public class MockFacadeMongoDbQueue implements MockFacadeInterface {

    MongoCollection<Document> mongoCollection;

    ReturnableMockTypeList<String> poll = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<List<String>> pollNItems = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<Boolean> hasMoreData;


    @Override
    @SuppressWarnings("unchecked")
    public MockFacadeMongoDbQueue configure(){

        reset();

        mongoCollection = Mockito.mock(MongoCollection.class);

        poll.add("{\"name\":\"amit\"}");
        pollNItems.add(Arrays.asList("{\"name\":\"amit\"}", "{\"name\":\"rahul\"}", "{\"name\":\"deepak\"}"));
        hasMoreData.add(false);
        return this;
    }


    public MockFacadeMongoDbQueue poll(String... pollMethodWithNoParamAndReturnData) {
        this.poll.clear();
        this.poll.add(pollMethodWithNoParamAndReturnData);

        return this;
    }

    public MockFacadeMongoDbQueue pollNItems(List<String>... pollNItems) {
        this.pollNItems.clear();
        this.pollNItems.add(pollNItems);

        return this;
    }

    public MockFacadeMongoDbQueue hasMoreData(Boolean... hasMoreData) {
        this.hasMoreData.clear();
        this.hasMoreData.add(hasMoreData);
        return this;
    }

    public MockFacadeMongoDbQueue addMongoCollection(MongoCollection<Document> mongoCollection){
        this.mongoCollection = mongoCollection;
        return this;
    }

    @Override
    public MongoDbQueue build() throws Exception {
        MongoDbQueue mongoDbQueue = new MongoDbQueue();
        mongoDbQueue.setQueue(mongoCollection);
        mongoDbQueue = Mockito.spy(mongoDbQueue);

        doNothing().when(mongoDbQueue).add(anyString());

        doNothing().when(mongoDbQueue).add(anyList());

        doAnswer(poll.answer()).when(mongoDbQueue).poll();

        doAnswer(hasMoreData.answer()).when(mongoDbQueue).hasMoreData();

        doAnswer(pollNItems.answer()).when(mongoDbQueue).poll(anyInt());


        return mongoDbQueue;
    }
}
