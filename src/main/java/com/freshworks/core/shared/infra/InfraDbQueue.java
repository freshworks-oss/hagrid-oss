package com.freshworks.core.shared.infra;

import com.freshworks.core.shared.SyncServiceContainer;

import java.util.List;

public interface InfraDbQueue {

    public void configure(SyncServiceContainer syncServiceContainer) throws Exception;

    public void add(String s) throws Exception;

    public void add(List<String> s) throws Exception;

    public String poll() throws Exception;

    public List<String> poll(int n) throws Exception;

    public long size() throws Exception;

    public boolean hasMoreData() throws Exception;

    public void attachPublisher() throws Exception;

    //TODO : Deregister publisher will help to notify the consumer that data is done
    public void removePublisher() throws Exception;

    public Boolean isEmpty() throws Exception;

    public void delete() throws Exception;
}
