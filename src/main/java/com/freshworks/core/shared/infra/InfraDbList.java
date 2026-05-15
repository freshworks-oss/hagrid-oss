package com.freshworks.core.shared.infra;

import com.freshworks.core.shared.SyncServiceContainer;

import java.util.ArrayList;
import java.util.List;

public interface InfraDbList {

    public void configure(SyncServiceContainer syncServiceContainer) throws Exception;

    public void add(String s) throws Exception;

    public void add(List<String> s) throws Exception;

    public  Long addAndGetIndex(String s) throws Exception;

    public  List<Long> addAndGetIndexBulk(List<String> sList) throws Exception;

    public String get(int index) throws Exception;

    public List<String> get(int start, int n) throws Exception;

    public List<String> get(List<Long> documentIdList) throws Exception;

    //TODO : Deregister publisher will help to notify the consumer that data is done
    public void deRegisterPublisher() throws Exception;

    public long size() throws Exception;

    public Boolean isEndOfListReached(int index) throws Exception;

    public void delete() throws Exception;
}
