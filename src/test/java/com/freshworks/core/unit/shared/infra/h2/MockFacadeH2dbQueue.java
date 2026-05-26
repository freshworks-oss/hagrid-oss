package com.freshworks.core.shared.infra.h2;

import com.freshworks.core.MockFacadeInterface;
import com.freshworks.core.ReturnableMockTypeList;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.mockito.Mockito;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Component
public class MockFacadeH2dbQueue implements MockFacadeInterface {

    HikariDataSource hikariDataSource;

    ReturnableMockTypeList<String> poll = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<List<String>> pollNItems = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<Boolean> hasMoreData;


    @Override
    public MockFacadeH2dbQueue configure(){

        reset();

        HikariConfig config = new HikariConfig();
        String dbString =  "jdbc:h2:mem:testdb";
        config.setJdbcUrl(dbString);
        config.setUsername("");
        config.setPassword("");
        config.setIdleTimeout(60000); // 60 seconds
        hikariDataSource = new HikariDataSource(config);

        poll.add("{\"name\":\"amit\"}");
        pollNItems.add(Arrays.asList("{\"name\":\"amit\"}", "{\"name\":\"rahul\"}", "{\"name\":\"deepak\"}"));
        hasMoreData.add(false);
        return this;
    }


    public MockFacadeH2dbQueue poll(String... pollMethodWithNoParamAndReturnData) {
        this.poll.clear();;
        this.poll.add(pollMethodWithNoParamAndReturnData);

        return this;
    }

    public MockFacadeH2dbQueue pollNItems(List<String>... pollNItems) {
        this.pollNItems.clear();
        this.pollNItems.add(pollNItems);

        return this;
    }

    public MockFacadeH2dbQueue hasMoreData(Boolean... hasMoreData) {
        this.hasMoreData.clear();
        this.hasMoreData.add(hasMoreData);
        return this;
    }

    public MockFacadeH2dbQueue addHikariDataSource(HikariDataSource hikariDataSource){
        this.hikariDataSource = hikariDataSource;
        return this;
    }

    @Override
    public H2DbQueue build() throws Exception {
        H2DbQueue h2DbQueue = new H2DbQueue(hikariDataSource, "some_name_space","some_queue");
        h2DbQueue = Mockito.spy(h2DbQueue);

        doNothing().when(h2DbQueue).add(anyString());

        doNothing().when(h2DbQueue).add(anyList());

        doAnswer(poll.answer()).when(h2DbQueue).poll();

        doAnswer(hasMoreData.answer()).when(h2DbQueue).hasMoreData();

        doAnswer(pollNItems.answer()).when(h2DbQueue).poll(anyInt());


        return h2DbQueue;
    }
}
