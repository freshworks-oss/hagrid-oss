package com.freshworks.hagrid.shared.infra;

import java.util.List;

import com.freshworks.hagrid.shared.SyncServiceContainer;

public interface InfraDbKeyValue {

    public void configure(SyncServiceContainer syncServiceContainer) throws Exception;

    public void set(String key, String value) throws Exception;
    public String get(String key) throws Exception;

    public void putList(String key, List<String> value) throws Exception;

    public void putList(String key, String value) throws Exception;

    public List<String> getList(String key) throws Exception;

    public void delete() throws Exception;

    default public long size() throws Exception{

        return 0;
    }
}
