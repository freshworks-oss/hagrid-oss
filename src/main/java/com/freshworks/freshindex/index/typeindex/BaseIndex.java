package com.freshworks.freshindex.index.typeindex;


import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public abstract class BaseIndex {

    public abstract void insert(String value, String documentId) throws Exception;

    public abstract void remove(String value, String documentId) throws  Exception;

    public abstract List<String> get(String value, String operator) throws  Exception;

    public abstract List<Object> getValues() throws Exception;
}
