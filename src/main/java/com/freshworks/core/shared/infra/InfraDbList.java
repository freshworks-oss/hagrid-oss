package com.freshworks.core.shared.infra;

import com.freshworks.core.shared.SyncServiceContainer;

import java.util.ArrayList;
import java.util.List;

public interface InfraDbList {

    public void configure(SyncServiceContainer syncServiceContainer) throws Exception;

    // Add a single string to the list 
    public void add(String s) throws Exception;

    // Add a array of strings to the list 
    public void add(List<String> s) throws Exception;

    // Add a single string and get its index 
    public  Long addAndGetIndex(String s) throws Exception;

    // Add a list of strings and get their indexes
    public  List<Long> addAndGetIndexBulk(List<String> sList) throws Exception;

    // Get a string added at index index
    public String get(int index) throws Exception;

    // Get list of n strings added at index starting from start 
    public List<String> get(int start, int n) throws Exception;

    // Get list of strings for given ids
    public List<String> get(List<Long> documentIdList) throws Exception;

    //TODO : Deregister publisher will help to notify the consumer that data is done
    public void deRegisterPublisher() throws Exception;

    public long size() throws Exception;

    public Boolean isEndOfListReached(int index) throws Exception;

    public void delete() throws Exception;
}
