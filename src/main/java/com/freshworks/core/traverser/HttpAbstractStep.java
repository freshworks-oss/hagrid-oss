package com.freshworks.core.traverser;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.freshworks.core.traverser.exception.StepFailedException;
import com.freshworks.core.traverser.net.http.HttpRequestResponse;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;

import java.net.URISyntaxException;

@Slf4j
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract  class HttpAbstractStep extends AbstractStep{

    public abstract void setup(ImmutableMap<String, String> baggageMap, JsonNode... parentJsonObject) throws Exception;

    public abstract boolean shouldProceedWithParentObject(ImmutableMap<String, String> baggageMap, JsonNode... parentJsonObject) throws Exception;

    public abstract HttpRequestResponse startSync(JsonNode... parentJsonObject) throws Exception;

    public abstract void filterResponse(StepDataBeanMapping stepDataBeanMapping, JsonNode... parentJsonObject) throws Exception;

    public abstract HttpRequestResponse getNextSyncRequest(HttpRequestResponse currentRequest, JsonNode... parentJsonObject) throws Exception;

    public abstract boolean isValidResponse(HttpRequestResponse currentRequest, JsonNode... parentJsonObject) throws Exception;

    public abstract DagTraversalService.TraverseAction handleInvalidResponse(HttpRequestResponse currentRequest, JsonNode... parentJsonObject) throws Exception;

    public abstract boolean isSyncComplete(HttpRequestResponse currentRequest, JsonNode... parentJsonObject) throws Exception;

    public abstract StepDataBeanMapping parseSyncResponse(HttpRequestResponse httpRequestResponse, JsonNode... parentJsonObject) throws Exception;

}
