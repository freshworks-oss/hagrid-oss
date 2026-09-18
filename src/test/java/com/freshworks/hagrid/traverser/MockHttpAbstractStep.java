package com.freshworks.core.traverser;

import com.fasterxml.jackson.databind.JsonNode;
import com.freshworks.hagrid.traverser.DagTraversalService;
import com.freshworks.hagrid.traverser.HttpAbstractStep;
import com.freshworks.hagrid.traverser.ParentStep;
import com.freshworks.hagrid.traverser.StepDataBeanMapping;
import com.freshworks.hagrid.traverser.Annotations.FreshHierarchy;
import com.freshworks.hagrid.traverser.exception.StepFailedException;
import com.freshworks.hagrid.traverser.net.http.HttpRequestResponse;
import com.google.common.collect.ImmutableMap;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.net.URISyntaxException;

@Component
@Scope("prototype")
@FreshHierarchy(parentClass = ParentStep.class, ignore = true)
public class MockHttpAbstractStep extends HttpAbstractStep{

    public MockHttpAbstractStep configure(){

        return this;

    }

    @Override
    public void setup(ImmutableMap<String, String> baggageMap, JsonNode... parentJsonObject) throws StepFailedException {

    }

    @Override
    public boolean shouldProceedWithParentObject(ImmutableMap<String, String> baggageMap,JsonNode... parentJsonObject) throws StepFailedException {
        return true;
    }

    @Override
    public HttpRequestResponse startSync(JsonNode... parentJsonObject) throws StepFailedException {
        return null;
    }


    @Override
    public HttpRequestResponse getNextSyncRequest(HttpRequestResponse currentRequest, JsonNode... parentJsonObject) throws StepFailedException {
        return null;
    }

    @Override
    public boolean isValidResponse(HttpRequestResponse currentRequest, JsonNode... parentJsonObject) throws StepFailedException {
        return true;
    }

    @Override
    public DagTraversalService.TraverseAction handleInvalidResponse(HttpRequestResponse currentRequest, JsonNode... parentJsonObject) throws URISyntaxException, StepFailedException {
        return null;
    }

    @Override
    public boolean isSyncComplete(HttpRequestResponse currentRequest, JsonNode... parentJsonObject) throws StepFailedException {
        return true;
    }

    @Override
    public StepDataBeanMapping parseSyncResponse(HttpRequestResponse httpRequestResponse, JsonNode... parentJsonObject) {
        return null;
    }

    @Override
    public void closeSync() {

    }

    public void reset(){

    }
}
