package com.freshworks.core.data.four_five_zero.durability.steps.next_request.exception;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshworks.core.CustomRegExConditionComparator;
import com.freshworks.core.data.four_zero_zero.durability.beans.FbUser;
import com.freshworks.core.shared.Namespace;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.analytics.AnalyticsFactory;
import com.freshworks.core.shared.analytics.AnalyticsService;
import com.freshworks.core.shared.infra.InfraService;
import com.freshworks.core.traverser.*;
import com.freshworks.core.traverser.Annotations.FreshHierarchy;
import com.freshworks.core.traverser.exception.StepFailedException;
import com.freshworks.core.traverser.net.http.HttpRequest;
import com.freshworks.core.traverser.net.http.HttpRequestResponse;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.net.URISyntaxException;

@Slf4j
@FreshHierarchy(parentClass = ParentStep.class, rateLimit = 800, duration = 1)
@Component
@Scope("prototype")
@Conditional(CustomRegExConditionComparator.class)
public class TestGetNextRequestException extends HttpAbstractStep {

    private SyncServiceContainer syncServiceContainer;
    int numberOfUsersEachPage = 100;
    int numberOfUserPagination = 1;
    long waitBetweenUserPaginationInMs = 0;

    int count = 0;
    AnalyticsService analyticsService;
    InfraService infraService;


    @Override
    public void configure(SyncServiceContainer syncServiceContainer) {
        this.syncServiceContainer = syncServiceContainer;
        AnalyticsFactory analyticsFactory = syncServiceContainer.getBean(AnalyticsFactory.class);
        Namespace namespace = syncServiceContainer.getBean(Namespace.class);
        this.analyticsService = analyticsFactory.getAnalyticsService(namespace.getNamespace());
        this.infraService = syncServiceContainer.getBean(InfraService.class);
    }

    @Override
    public void setup(ImmutableMap<String, String> baggageMap, JsonNode... parentJsonObject) throws StepFailedException {

        // As of now I can not test throwing exception from step because method signature allow only to throw StepFailedException
        // It is suppose to be replaced with Exception
    }

    @Override
    public boolean shouldProceedWithParentObject(ImmutableMap<String, String> baggageMap, JsonNode... parentJsonObject) throws StepFailedException {
        analyticsService.infoEvent("HAGRID_DURABILITY_EVENT", "step", this.getClass().getName(), "method", "shouldProceedWithParentObject");
        analyticsService.infoEvent("STEP_METHOD_CALLED", "name", "shouldProceedWithParentObject");
        return true;
    }

    @Override
    public HttpRequestResponse startSync(JsonNode... parentJsonObject) throws StepFailedException {
        analyticsService.infoEvent("HAGRID_DURABILITY_EVENT", "step", this.getClass().getName(), "method", "startSync");
        analyticsService.infoEvent("STEP_METHOD_CALLED", "name", "startSync");
        analyticsService.infoEvent("THIRD_PARTY_API_CALLED", "api-name", "fbuser");
        try{
            HttpRequestResponse httpRequestResponse = new HttpRequestResponse();
            HttpRequest httpRequest = new HttpRequest();
            httpRequest.initGet("http://django:3000/users?how_many=" + numberOfUsersEachPage);
            httpRequestResponse.setRequest(httpRequest);
            analyticsService.infoEvent("THIRD_PARTY_API_CALLED");
            count = count + 1;
            return httpRequestResponse;
        }
        catch (Exception e){
            e.printStackTrace();
            return null;
        }

    }

    @Override
    public void filterResponse(StepDataBeanMapping stepDataBeanMapping, JsonNode... parentJsonObject) throws StepFailedException {
        analyticsService.infoEvent("HAGRID_DURABILITY_EVENT", "step", this.getClass().getName(), "method", "filterResponse");

    }

    @Override
    public HttpRequestResponse getNextSyncRequest(HttpRequestResponse currentRequest, JsonNode... parentJsonObject) throws StepFailedException {
        try{
            analyticsService.infoEvent("HAGRID_DURABILITY_EVENT", "step", this.getClass().getName(), "method", "getNextSyncRequest");
            analyticsService.infoEvent("STEP_METHOD_CALLED", "name", "getNextSyncRequest");
            analyticsService.infoEvent("THIRD_PARTY_API_CALLED","api-name", "fbuser");

            HttpRequestResponse httpRequestResponse = new HttpRequestResponse();
            HttpRequest httpRequest = new HttpRequest();
            httpRequest.initGet("http://django:3000/users?has_next=true&how_many=" + numberOfUsersEachPage);
            httpRequestResponse.setRequest(httpRequest);

            count = count + 1;
            Thread.sleep(waitBetweenUserPaginationInMs);
            return httpRequestResponse;
        }
        catch (Exception e){
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public boolean isValidResponse(HttpRequestResponse currentRequest, JsonNode... parentJsonObject) throws StepFailedException {

        analyticsService.infoEvent("HAGRID_DURABILITY_EVENT", "step", this.getClass().getName(), "method", "isValidResponse");

        analyticsService.infoEvent("STEP_METHOD_CALLED", "name", "isValidResponse");
        if(currentRequest.getResponse().getCode() == 200){
            return true;
        }
        else{
            return false;
        }
    }

    @Override
    public DagTraversalService.TraverseAction handleInvalidResponse(HttpRequestResponse currentRequest, JsonNode... parentJsonObject) throws URISyntaxException, StepFailedException {
        analyticsService.infoEvent("HAGRID_DURABILITY_EVENT", "step", this.getClass().getName(), "method", "handleInvalidResponse");
        analyticsService.infoEvent("STEP_METHOD_CALLED", "name", "handleInvalidResponse");
        analyticsService.infoEvent("THIRD_PARTY_API_INVALID_RESPONSE");
        return null;
    }

    @Override
    public boolean isSyncComplete(HttpRequestResponse currentRequest, JsonNode... parentJsonObject) throws StepFailedException {

        analyticsService.infoEvent("HAGRID_DURABILITY_EVENT", "step", this.getClass().getName(), "method", "isSyncComplete");

        analyticsService.infoEvent("STEP_METHOD_CALLED", "name", "isSyncComplete");

        if(count < numberOfUserPagination){
            return false;
        }
        else{
            return true;
        }
    }

    @Override
    public StepDataBeanMapping parseSyncResponse(HttpRequestResponse httpRequestResponse, JsonNode... parentJsonObject) {
        analyticsService.infoEvent("HAGRID_DURABILITY_EVENT", "step", this.getClass().getName(), "method", "parseSyncResponse");
        analyticsService.infoEvent("STEP_METHOD_CALLED", "name", "parseSyncResponse");
        analyticsService.infoEvent("THIRD_PARTY_API_RESPONSE");
        try{
            ObjectMapper objectMapper = new ObjectMapper();

            StepDataBeanMapping stepDataBeanMapping = new StepDataBeanMapping();
            String response = httpRequestResponse.getResponse().getBody();

            JsonNode jsonNode = objectMapper.readTree(response);
            stepDataBeanMapping.setParseSyncedResponseData(jsonNode.get("data").get("users"));
            stepDataBeanMapping.setBeanClass(FbUser.class);
            return stepDataBeanMapping;

        }
        catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void closeSync() {
        analyticsService.infoEvent("HAGRID_DURABILITY_EVENT", "step", this.getClass().getName(), "method", "closeSync");
        analyticsService.infoEvent("STEP_METHOD_CALLED", "name", "closeSync");
    }
}
