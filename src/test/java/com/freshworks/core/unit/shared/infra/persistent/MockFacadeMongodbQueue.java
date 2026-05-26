package com.freshworks.core.shared.infra.persistent;

import com.freshworks.core.MockFacadeInterface;
import com.freshworks.core.ReturnableMockTypeList;
import org.mockito.Mockito;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Component
public class MockFacadeMongodbQueue implements MockFacadeInterface {


    ReturnableMockTypeList<String> poll = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<List<String>> pollNItems = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<Boolean> hasMoreData;


    @Override
    public MockFacadeMongodbQueue configure(){

        reset();

        poll.add("{\"name\":\"amit\"}");
        pollNItems.add(Arrays.asList("{\"name\":\"amit\"}", "{\"name\":\"rahul\"}", "{\"name\":\"deepak\"}"));
        hasMoreData.add(false);
        return this;
    }


    public MockFacadeMongodbQueue poll(String... pollMethodWithNoParamAndReturnData) {
        this.poll.clear();;
        this.poll.add(pollMethodWithNoParamAndReturnData);

        return this;
    }

    public MockFacadeMongodbQueue pollNItems(List<String>... pollNItems) {
        this.pollNItems.clear();
        this.pollNItems.add(pollNItems);

        return this;
    }

    public MockFacadeMongodbQueue hasMoreData(Boolean... hasMoreData) {
        this.hasMoreData.clear();
        this.hasMoreData.add(hasMoreData);
        return this;
    }

    @Override
    public MongoDbQueue build() throws Exception {

        MongoDbQueue mongoDbQueue = new MongoDbQueue();
        mongoDbQueue = Mockito.spy(mongoDbQueue);


        doNothing().when(mongoDbQueue).add(anyString());

        doNothing().when(mongoDbQueue).add(anyList());

        doAnswer(poll.answer()).when(mongoDbQueue).poll();

        doAnswer(hasMoreData.answer()).when(mongoDbQueue).hasMoreData();

        doAnswer(pollNItems.answer()).when(mongoDbQueue).poll(anyInt());


        return mongoDbQueue;
    }
}
