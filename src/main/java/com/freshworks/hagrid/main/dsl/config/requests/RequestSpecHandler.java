package com.freshworks.hagrid.main.dsl.config.requests;

import com.freshworks.hagrid.main.dsl.config.model.ModelSpec;

import groovy.lang.Closure;

public class RequestSpecHandler {
    
    RequestSpec requestSpec;

    public RequestSpecHandler(RequestSpec requestSpec) {
        this.requestSpec = requestSpec;
    }

    public void requests(Closure closure) {
        closure.setDelegate(this.requestSpec);
        closure.setResolveStrategy(Closure.DELEGATE_FIRST);
        closure.call();
    }

}
