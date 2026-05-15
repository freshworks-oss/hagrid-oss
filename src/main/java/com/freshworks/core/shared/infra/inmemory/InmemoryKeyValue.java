package com.freshworks.core.shared.infra.inmemory;

import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.infra.InfraDbKeyValue;
import com.google.common.collect.Lists;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;


@Slf4j
@Getter
@Setter
public class InmemoryKeyValue implements InfraDbKeyValue {

    ConcurrentHashMap<String, List<String>> concurrentHashMap;

    String keyValue;


    protected InmemoryKeyValue(){

    }

    @Override
    public void configure(SyncServiceContainer syncServiceContainer) throws Exception{

    }

    @Override
    public void put(String key, String value) throws Exception{

        this.concurrentHashMap.put(key, Lists.newArrayList(value));
    }

    @Override
    public String get(String key) throws Exception {
        return this.concurrentHashMap.get(key).get(0);
    }

    @Override
    public void putList(String key, List<String> value) throws Exception{

        this.concurrentHashMap.put(key, value);
    }

    @Override
    public void putList(String key, String value) throws Exception{
        if (concurrentHashMap.containsKey(key)) {
            this.concurrentHashMap.get(key).add(value);
            this.concurrentHashMap.put(key, this.concurrentHashMap.get(key));
        }
        else {
            List<String> newList = List.of(value);
            this.concurrentHashMap.put(key, newList);
        }
    }

    @Override
    public List<String> getList(String key) throws Exception {
        return this.concurrentHashMap.get(key);
    }

    @Override
    public void delete() throws Exception {
        concurrentHashMap.clear();
    }
}
