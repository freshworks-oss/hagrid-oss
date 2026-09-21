package com.freshworks.hagrid.data.unit.dag.steps.loop;

import com.fasterxml.jackson.databind.JsonNode;
import com.freshworks.hagrid.traverser.AbstractStep;
import com.freshworks.hagrid.traverser.DagTraversalService;
import com.freshworks.hagrid.traverser.HttpAbstractStep;
import com.freshworks.hagrid.traverser.StepDataBeanMapping;
import com.freshworks.hagrid.traverser.Annotations.FreshHierarchy;
import com.freshworks.hagrid.traverser.exception.StepFailedException;
import com.freshworks.hagrid.traverser.net.http.HttpRequest;
import com.freshworks.hagrid.traverser.net.http.HttpRequestResponse;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.net.URISyntaxException;

@Slf4j
@FreshHierarchy(parentClass = {StepA.class, StepC.class}, rateLimit = 20, duration = 100)
@Component("unit_dag_step_StepB")
@Scope("prototype")
@Profile("unit")
public class StepB extends HttpAbstractStep {

    @Override
    public void setup(ImmutableMap<String, String> baggageMap, JsonNode... parentJsonObject) throws StepFailedException {

    }

    @Override
    public boolean shouldProceedWithParentObject(ImmutableMap<String, String> baggageMap, JsonNode... parentJsonObject) throws StepFailedException {
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
}
