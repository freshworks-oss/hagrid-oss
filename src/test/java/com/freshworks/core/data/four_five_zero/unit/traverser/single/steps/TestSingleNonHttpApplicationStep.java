package com.freshworks.core.data.four_five_zero.unit.traverser.single.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshworks.core.CustomRegExConditionComparator;
import com.freshworks.core.data.four_five_zero.unit.dag.steps.Authentication;
import com.freshworks.core.data.four_five_zero.unit.traverser.single.beans.Application;
import com.freshworks.core.traverser.*;
import com.freshworks.core.traverser.Annotations.FreshHierarchy;
import com.freshworks.core.traverser.net.http.HttpRequest;
import com.freshworks.core.traverser.net.http.HttpRequestResponse;
import com.google.common.collect.ImmutableMap;
import io.micrometer.core.instrument.LongTaskTimer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@FreshHierarchy(parentClass = ParentStep.class, rateLimit = 800, duration = 1)
@Component
@Scope("prototype")
@Conditional(CustomRegExConditionComparator.class)
public class TestSingleNonHttpApplicationStep extends NonHttpAbstractStep {

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
    public void filterResponseNonHttp(StepDataBeanMapping stepDataBeanMapping, JsonNode... parentJsonObject) throws Exception {

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

    @Override
    public void closeSync() {

    }
}
