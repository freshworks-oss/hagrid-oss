package com.freshworks.core.shared.infra.persistent;

import com.freshworks.core.MockFacadeInterface;
import com.freshworks.core.ReturnableMockTypeList;
import com.google.common.collect.Lists;
import org.mockito.Mockito;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@Component
public class MockFacadeMongodbKeyValue implements MockFacadeInterface {


    ReturnableMockTypeList<String> get = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<List<String>> getList = new ReturnableMockTypeList<>();

    @Override

    public MockFacadeMongodbKeyValue configure(){
        reset();
        get.add("{\"name\":\"amit\"}");
        getList.add(Lists.newArrayList("{\"name\":\"amit\"}", "{\"name\":\"rahul\"}", "{\"name\":\"deepak\"}"));
        return this;
    }

    public MockFacadeMongodbKeyValue get(String... get) {
        this.get.clear();
        this.get.add(get);
        return this;
    }


    public MockFacadeMongodbKeyValue getList( List<String>... getList){
        this.getList.clear();
        this.getList.add(getList);
        return this;
    }


    @Override
    public MongoDbKeyValue build() throws Exception {

        MongoDbKeyValue mongoDbKeyValue = new MongoDbKeyValue();
        mongoDbKeyValue = Mockito.spy(mongoDbKeyValue);

        doNothing().when(mongoDbKeyValue).put(anyString(), anyString());

        doAnswer(get.answer()).when(mongoDbKeyValue).get(anyString());

        doNothing().when(mongoDbKeyValue).putList(anyString(), anyList());

        doNothing().when(mongoDbKeyValue).putList(anyString(), anyString());

        doAnswer(getList.answer()).when(mongoDbKeyValue).getList(anyString());

        return mongoDbKeyValue;
    }
}
