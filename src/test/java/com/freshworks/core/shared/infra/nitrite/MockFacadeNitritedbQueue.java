package com.freshworks.core.shared.infra.nitrite;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;

import java.util.Arrays;
import java.util.List;

import org.dizitart.no2.Nitrite;
import org.mockito.Mockito;
import org.springframework.stereotype.Component;

import com.freshworks.core.MockFacadeInterface;
import com.freshworks.core.ReturnableMockTypeList;

@Component
public class MockFacadeNitritedbQueue implements MockFacadeInterface {

    Nitrite nitriteDb;

    ReturnableMockTypeList<String> poll = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<List<String>> pollNItems = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<Boolean> hasMoreData;


    @Override
    public MockFacadeNitritedbQueue configure(){

        reset();

        nitriteDb = Nitrite.builder()
            .openOrCreate();
        

        poll.add("{\"name\":\"amit\"}");
        pollNItems.add(Arrays.asList("{\"name\":\"amit\"}", "{\"name\":\"rahul\"}", "{\"name\":\"deepak\"}"));
        hasMoreData.add(false);
        return this;
    }


    public MockFacadeNitritedbQueue poll(String... pollMethodWithNoParamAndReturnData) {
        this.poll.clear();;
        this.poll.add(pollMethodWithNoParamAndReturnData);

        return this;
    }

    public MockFacadeNitritedbQueue pollNItems(List<String>... pollNItems) {
        this.pollNItems.clear();
        this.pollNItems.add(pollNItems);

        return this;
    }

    public MockFacadeNitritedbQueue hasMoreData(Boolean... hasMoreData) {
        this.hasMoreData.clear();
        this.hasMoreData.add(hasMoreData);
        return this;
    }

    public MockFacadeNitritedbQueue addHikariDataSource(Nitrite nitriteDb){
        this.nitriteDb = nitriteDb;
        return this;
    }

    @Override
    public NitriteDbQueue build() throws Exception {
        NitriteDbQueue nitriteDbQueue = new NitriteDbQueue(nitriteDb, "some_name_space","some_queue");
        nitriteDbQueue = Mockito.spy(nitriteDbQueue);

        doNothing().when(nitriteDbQueue).add(anyString());

        doNothing().when(nitriteDbQueue).add(anyList());

        doAnswer(poll.answer()).when(nitriteDbQueue).poll();

        doAnswer(hasMoreData.answer()).when(nitriteDbQueue).hasMoreData();

        doAnswer(pollNItems.answer()).when(nitriteDbQueue).poll(anyInt());


        return nitriteDbQueue;
    }
}
