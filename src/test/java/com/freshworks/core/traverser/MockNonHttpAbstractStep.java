package com.freshworks.core.traverser;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.freshworks.core.traverser.Annotations.FreshHierarchy;
import com.google.common.collect.ImmutableMap;

@Component
@Scope("prototype")
@FreshHierarchy(parentClass = ParentStep.class, ignore = true)
public class MockNonHttpAbstractStep extends NonHttpAbstractStep{

    public MockNonHttpAbstractStep configure(){

        return this;

    }


    @Override
    public void closeSync() {

    }

    public void reset(){

    }

    @Override
    public void setupNonHttp(ImmutableMap<String, String> baggageMap, JsonNode... parentJsonObject) throws Exception {

    }

    @Override
    public boolean shouldProceedWithParentObjectNonHttp(ImmutableMap<String, String> baggageMap, JsonNode... parentJsonObject) throws Exception {
        return false;
    }

    @Override
    public RequestResponseContainer startSyncNonHttp(JsonNode... parentJsonObject) throws Exception {
        return null;
    }

    @Override
    public RequestResponseContainer executeNonHttp(RequestResponseContainer currentRequestResponse, JsonNode... parentJsonObject) throws Exception {
        return null;
    }


    @Override
    public RequestResponseContainer getNextSyncRequestNonHttp(RequestResponseContainer currentRequest, JsonNode... parentJsonObject) throws Exception {
        return null;
    }

    @Override
    public boolean isValidResponseNonHttp(RequestResponseContainer currentRequest, JsonNode... parentJsonObject) throws Exception {
        return false;
    }

    @Override
    public DagTraversalService.TraverseAction handleInValidResponseNonHttp(RequestResponseContainer currentRequest, JsonNode... parentJsonObject) throws Exception {
        return null;
    }

    @Override
    public boolean isSyncCompleteNonHttp(RequestResponseContainer currentRequest, JsonNode... parentJsonObject) throws Exception {
        return false;
    }

    @Override
    public StepDataBeanMapping parseSyncResponseNonHttp(RequestResponseContainer currentRequestResponse, JsonNode... parentJsonObject) throws Exception {
        return null;
    }
}
