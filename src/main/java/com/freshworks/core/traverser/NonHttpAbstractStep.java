package com.freshworks.core.traverser;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.freshworks.core.traverser.exception.StepFailedException;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract  class NonHttpAbstractStep extends AbstractStep{

    public abstract void setupNonHttp(ImmutableMap<String, String> baggageMap, JsonNode... parentJsonObject) throws Exception;

    public abstract boolean shouldProceedWithParentObjectNonHttp(ImmutableMap<String, String> baggageMap, JsonNode... parentJsonObject) throws Exception;

    public abstract RequestResponseContainer startSyncNonHttp(JsonNode... parentJsonObject) throws Exception;

    public abstract RequestResponseContainer executeNonHttp(RequestResponseContainer currentRequestResponse, JsonNode... parentJsonObject) throws Exception;

    public abstract RequestResponseContainer getNextSyncRequestNonHttp(RequestResponseContainer currentRequest, JsonNode... parentJsonObject) throws Exception;

    public abstract  boolean isValidResponseNonHttp(RequestResponseContainer currentRequest, JsonNode... parentJsonObject) throws Exception;

    public abstract  DagTraversalService.TraverseAction handleInValidResponseNonHttp(RequestResponseContainer currentRequest, JsonNode... parentJsonObject) throws Exception;

    public abstract boolean isSyncCompleteNonHttp(RequestResponseContainer currentRequest, JsonNode... parentJsonObject) throws Exception;

    public abstract StepDataBeanMapping parseSyncResponseNonHttp(RequestResponseContainer currentRequestResponse, JsonNode... parentJsonObject) throws  Exception;

}
