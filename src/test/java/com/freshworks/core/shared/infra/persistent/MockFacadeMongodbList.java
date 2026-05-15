package com.freshworks.core.shared.infra.persistent;

import com.freshworks.core.MockFacadeInterface;
import com.freshworks.core.ReturnableMockTypeList;
import com.google.common.collect.Lists;
import org.mockito.Mockito;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Component
public class MockFacadeMongodbList implements MockFacadeInterface {


    ReturnableMockTypeList<Long> addAndGetIndex = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<List<Long>> addAndGetIndexBulk = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<String> get = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<List<String>> getNFromStartIndex = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<List<String>> getGivenDocList = new ReturnableMockTypeList<>();


    ReturnableMockTypeList<Boolean> isEndOfListReached = new ReturnableMockTypeList<>();


    @Override
    public MockFacadeMongodbList configure(){

        reset();

        addAndGetIndex.add(1000L);
        addAndGetIndexBulk.add(Lists.newArrayList(100L,20L,3L,4000L));
        get.add("{\"name\":\"amit\"}");
        getNFromStartIndex.add(Lists.newArrayList("{\"name\":\"amit\"}"));
        getGivenDocList.add(Lists.newArrayList("1","2","3"));
        return this;
    }


    public MockFacadeMongodbList addAndGetIndex(Long... addAndGetIndex) {
        this.addAndGetIndex.clear();
        this.addAndGetIndex.add(addAndGetIndex);
        return this;
    }

    public MockFacadeMongodbList addAndGetIndexBulk(List<Long>... addAndGetIndexBulk ){
        this.addAndGetIndexBulk.clear();
        this.addAndGetIndexBulk.add(addAndGetIndexBulk);
        return this;
    }

    public MockFacadeMongodbList get(String... get){
        this.get.clear();
        this.get.add(get);
        return this;
    }

    public MockFacadeMongodbList getNFromStartIndex( List<String>... getNFromStartIndex){
        this.getNFromStartIndex.clear();
        this.getNFromStartIndex.add(getNFromStartIndex);
        return this;
    }

    public MockFacadeMongodbList getGivenDocList(List<String>... getGivenDocList){
        this.getGivenDocList.clear();
        this.getGivenDocList.add(getGivenDocList);
        return this;
    }

    public MockFacadeMongodbList isEndOfListReached(Boolean... isEndOfListReached){
        this.isEndOfListReached.clear();
        this.isEndOfListReached.add(isEndOfListReached);
        return this;
    }

    @Override
    public MongoDbList build() throws Exception {

        MongoDbList mongoDbList = new MongoDbList();
        mongoDbList = Mockito.spy(mongoDbList);

        doNothing().when(mongoDbList).add(anyString());

        doAnswer(addAndGetIndex.answer()).when(mongoDbList).addAndGetIndex(anyString());

        doAnswer(addAndGetIndexBulk.answer()).when(mongoDbList).addAndGetIndexBulk(anyList());

        doNothing().when(mongoDbList).add(any(ArrayList.class));

        doAnswer(get.answer()).when(mongoDbList).get(anyInt());

        doAnswer(getNFromStartIndex.answer()).when(mongoDbList).get(anyInt(), anyInt());

        doAnswer(getGivenDocList.answer()).when(mongoDbList).get(anyList());

        doAnswer(isEndOfListReached.answer()).when(mongoDbList).isEndOfListReached(anyInt());

        return mongoDbList;
    }

}
