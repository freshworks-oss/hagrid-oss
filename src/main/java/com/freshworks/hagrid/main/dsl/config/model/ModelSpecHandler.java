package com.freshworks.hagrid.main.dsl.config.model;

import groovy.lang.Closure;

public class ModelSpecHandler {
    
    private final ModelSpec modelSpec;

    public ModelSpecHandler(ModelSpec modelSpec) {
        this.modelSpec = modelSpec;
    }

    public void models(Closure closure) {
        closure.setDelegate(this.modelSpec);
        closure.setResolveStrategy(Closure.DELEGATE_FIRST);
        closure.call();
    }
}
