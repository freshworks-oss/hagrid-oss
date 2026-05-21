package com.freshworks.core.data.four_zero_zero.performance.fb.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshworks.core.shared.Namespace;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.analytics.AnalyticsFactory;
import com.freshworks.core.shared.analytics.AnalyticsService;
import com.freshworks.core.shared.sync.SyncStatusService;
import com.freshworks.core.traverser.*;
import com.freshworks.core.traverser.Annotations.FreshHierarchy;
import com.freshworks.core.traverser.exception.StepFailedException;
import com.freshworks.core.traverser.net.http.HttpRequest;
import com.freshworks.core.traverser.net.http.HttpRequestResponse;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import com.freshworks.core.data.four_zero_zero.performance.fb.beans.*;

import java.net.URISyntaxException;
import java.util.Objects;

@Slf4j
@FreshHierarchy(parentClass = ParentStep.class, rateLimit = 800, duration = 1)
@Component
@Scope("prototype")
public class FbUser extends HttpAbstractStep {

    int numberOfUsersEachPage = Integer.parseInt(Objects.requireNonNullElse(System.getenv("numberOfUsersEachPage"), "1"));
    int numberOfPagination  = Integer.parseInt(Objects.requireNonNullElse(System.getenv("numberOfUsersPagination"),"1"));
    long waitBetweenPaginationInMs = Long.parseLong(Objects.requireNonNullElse(System.getenv("userWaitBetweenPaginationInMs"), "0"));

    int count = 0;
    AnalyticsService analyticsService;
    AnalyticsFactory analyticsFactory;


    @Override
    public void configure(SyncServiceContainer syncServiceContainer){
        Namespace namespace = syncServiceContainer.getBean(Namespace.class);
        AnalyticsFactory analyticsFactory = syncServiceContainer.getBean(AnalyticsFactory.class);
        analyticsService = analyticsFactory.getAnalyticsService(namespace.getNamespace());

    }

    @Override
    public void setup(ImmutableMap<String, String> baggageMap, JsonNode... parentJsonObject) throws StepFailedException {
        analyticsService.infoEvent("METHOD_CALLED", "name", "setup");
    }

    @Override
    public boolean shouldProceedWithParentObject(ImmutableMap<String, String> baggageMap, JsonNode... parentJsonObject) throws StepFailedException {
        analyticsService.infoEvent("METHOD_CALLED", "name", "shouldProceedWithParentObject");
        return true;
    }

    @Override
    public HttpRequestResponse startSync(JsonNode... parentJsonObject) throws StepFailedException {
        analyticsService.infoEvent("METHOD_CALLED", "name", "startSync");
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


    }

    @Override
    public HttpRequestResponse getNextSyncRequest(HttpRequestResponse currentRequest, JsonNode... parentJsonObject) throws StepFailedException {
        try{
            analyticsService.infoEvent("METHOD_CALLED", "name", "getNextSyncRequest");
            analyticsService.infoEvent("THIRD_PARTY_API_CALLED");

            HttpRequestResponse httpRequestResponse = new HttpRequestResponse();
            HttpRequest httpRequest = new HttpRequest();
            httpRequest.initGet("http://django:3000/users?has_next=true&how_many=100");
            httpRequestResponse.setRequest(httpRequest);

            count = count + 1;
//            Thread.sleep(waitBetweenPaginationInMs);
            return httpRequestResponse;
        }
        catch (Exception e){
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public boolean isValidResponse(HttpRequestResponse currentRequest, JsonNode... parentJsonObject) throws StepFailedException {

        analyticsService.infoEvent("METHOD_CALLED", "name", "isValidResponse");
        if(currentRequest.getResponse().getCode() == 200){
            return true;
        }
        else{
            return false;
        }
    }

    @Override
    public DagTraversalService.TraverseAction handleInvalidResponse(HttpRequestResponse currentRequest, JsonNode... parentJsonObject) throws URISyntaxException, StepFailedException {
        analyticsService.infoEvent("METHOD_CALLED", "name", "handleInvalidResponse");
        analyticsService.infoEvent("THIRD_PARTY_API_INVALID_RESPONSE");
        DagTraversalService.TraverseAction traverseAction = new DagTraversalService.TraverseAction();
        return null;
    }

    @Override
    public boolean isSyncComplete(HttpRequestResponse currentRequest, JsonNode... parentJsonObject) throws StepFailedException {
        analyticsService.infoEvent("METHOD_CALLED", "name", "isSyncComplete");

        if(count < numberOfPagination){
            return false;
        }
        else{
            return true;
        }
    }

    @Override
    public StepDataBeanMapping parseSyncResponse(HttpRequestResponse httpRequestResponse, JsonNode... parentJsonObject) {
        analyticsService.infoEvent("METHOD_CALLED", "name", "parseSyncResponse");
        analyticsService.infoEvent("THIRD_PARTY_API_RESPONSE");
        try{
            ObjectMapper objectMapper = new ObjectMapper();

            StepDataBeanMapping stepDataBeanMapping = new StepDataBeanMapping();
            String response = httpRequestResponse.getResponse().getBody();

            JsonNode jsonNode = objectMapper.readTree(response);
            stepDataBeanMapping.setParseSyncedResponseData(jsonNode.get("body").get("data").get("users"));
            stepDataBeanMapping.setBeanClass(com.freshworks.core.data.four_zero_zero.performance.fb.beans.FbUser.class);
            return stepDataBeanMapping;
        }
        catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void closeSync() {
        analyticsService.infoEvent("METHOD_CALLED", "name", "closeSync");
    }
}