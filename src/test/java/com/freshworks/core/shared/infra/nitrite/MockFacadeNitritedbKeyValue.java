package com.freshworks.core.shared.infra.nitrite;

import com.freshworks.core.MockFacadeInterface;
import com.freshworks.core.ReturnableMockTypeList;
import com.freshworks.core.shared.infra.nitrite.NitriteDbKeyValue;
import com.google.common.collect.Lists;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import org.dizitart.no2.Nitrite;
import org.dizitart.no2.rocksdb.RocksDBModule;
import org.mockito.Mockito;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;

@Component
public class MockFacadeNitritedbKeyValue implements MockFacadeInterface {

    ReturnableMockTypeList<String> get = new ReturnableMockTypeList<>();
    ReturnableMockTypeList<List<String>> getList = new ReturnableMockTypeList<>();
    Nitrite nitriteDb;

    @Override
    public MockFacadeNitritedbKeyValue configure(){
        reset();
        nitriteDb = Nitrite.builder()
            .openOrCreate();

        get.add("{\"name\":\"amit\"}");
        getList.add(Lists.newArrayList("{\"name\":\"amit\"}", "{\"name\":\"rahul\"}", "{\"name\":\"deepak\"}"));
        return this;
    }

    public MockFacadeNitritedbKeyValue get(String... get) {
        this.get.clear();
        this.get.add(get);
        return this;
    }


    public MockFacadeNitritedbKeyValue getList(List<String>... getList){
        this.getList.clear();
        this.getList.add(getList);
        return this;
    }

    public MockFacadeNitritedbKeyValue addHikariDataSource(Nitrite nitriteDb){
        this.nitriteDb = nitriteDb;
        return this;
    }

    @Override
    public NitriteDbKeyValue build() throws Exception {

        NitriteDbKeyValue h2DbKeyValue = new NitriteDbKeyValue(nitriteDb, "some_namespace","key_value");
        h2DbKeyValue = Mockito.spy(h2DbKeyValue);

        doNothing().when(h2DbKeyValue).put(anyString(), anyString());

        doAnswer(get.answer()).when(h2DbKeyValue).get(anyString());

        doNothing().when(h2DbKeyValue).putList(anyString(), anyList());

        doNothing().when(h2DbKeyValue).putList(anyString(), anyString());

        doAnswer(getList.answer()).when(h2DbKeyValue).getList(anyString());

        return h2DbKeyValue;
    }
}
