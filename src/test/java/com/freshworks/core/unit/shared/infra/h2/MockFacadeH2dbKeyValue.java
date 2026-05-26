package com.freshworks.core.shared.infra.h2;

import com.freshworks.core.MockFacadeInterface;
import com.freshworks.core.ReturnableMockTypeList;
import com.google.common.collect.Lists;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.mockito.Mockito;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;

@Component
public class MockFacadeH2dbKeyValue implements MockFacadeInterface {

    ReturnableMockTypeList<String> get = new ReturnableMockTypeList<>();
    ReturnableMockTypeList<List<String>> getList = new ReturnableMockTypeList<>();
    HikariDataSource hikariDataSource;

    @Override
    public MockFacadeH2dbKeyValue configure(){
        reset();

        HikariConfig config = new HikariConfig();
        String dbString =  "jdbc:h2:mem:testdb";
        config.setJdbcUrl(dbString);
        config.setUsername("");
        config.setPassword("");
        config.setIdleTimeout(60000); // 60 seconds
        hikariDataSource = new HikariDataSource(config);

        get.add("{\"name\":\"amit\"}");
        getList.add(Lists.newArrayList("{\"name\":\"amit\"}", "{\"name\":\"rahul\"}", "{\"name\":\"deepak\"}"));
        return this;
    }

    public MockFacadeH2dbKeyValue get(String... get) {
        this.get.clear();
        this.get.add(get);
        return this;
    }


    public MockFacadeH2dbKeyValue getList(List<String>... getList){
        this.getList.clear();
        this.getList.add(getList);
        return this;
    }

    public MockFacadeH2dbKeyValue addHikariDataSource(HikariDataSource hikariDataSource){
        this.hikariDataSource = hikariDataSource;
        return this;
    }

    @Override
    public H2DbKeyValue build() throws Exception {

        H2DbKeyValue h2DbKeyValue = new H2DbKeyValue(hikariDataSource, "some_namespace","key_value");
        h2DbKeyValue = Mockito.spy(h2DbKeyValue);

        doNothing().when(h2DbKeyValue).put(anyString(), anyString());

        doAnswer(get.answer()).when(h2DbKeyValue).get(anyString());

        doNothing().when(h2DbKeyValue).putList(anyString(), anyList());

        doNothing().when(h2DbKeyValue).putList(anyString(), anyString());

        doAnswer(getList.answer()).when(h2DbKeyValue).getList(anyString());

        return h2DbKeyValue;
    }
}
