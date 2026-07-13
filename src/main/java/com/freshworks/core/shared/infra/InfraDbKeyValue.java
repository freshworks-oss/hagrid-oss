package com.freshworks.core.shared.infra;

import com.freshworks.core.shared.SyncServiceContainer;

import java.util.List;

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
