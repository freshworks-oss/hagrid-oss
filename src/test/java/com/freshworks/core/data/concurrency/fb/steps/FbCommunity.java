package com.freshworks.core.data.concurrency.fb.steps;

import java.net.URISyntaxException;

import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshworks.core.shared.NamespaceService;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.analytics.AnalyticsFactory;
import com.freshworks.core.shared.analytics.AnalyticsService;
import com.freshworks.core.shared.infra.InfraService;
import com.freshworks.core.traverser.DagTraversalService;
import com.freshworks.core.traverser.HttpAbstractStep;
import com.freshworks.core.traverser.StepDataBeanMapping;
import com.freshworks.core.traverser.Annotations.FreshHierarchy;
import com.freshworks.core.traverser.exception.StepFailedException;
import com.freshworks.core.traverser.net.http.HttpRequest;
import com.freshworks.core.traverser.net.http.HttpRequestResponse;
import com.google.common.collect.ImmutableMap;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@FreshHierarchy(parentClass = FbUser.class, rateLimit = 800, duration = 1, ignore = false)
@Component
@Scope("prototype")
@Profile("concurrency")
public class FbCommunity extends HttpAbstractStep {

    int numberOfCommunitiesEachPage = 1;
    int numberOfCommunityPagination = 1;
    long waitBetweenCommunityPaginationInMs = 0;
    boolean shouldFail = false;

    int count = 0;
    AnalyticsService analyticsService;
    InfraService infraService;

    public FbCommunity() {

    }

    @Override
    public void configure(SyncServiceContainer syncServiceContainer) {
        AnalyticsFactory analyticsFactory = syncServiceContainer.getBean(AnalyticsFactory.class);
        NamespaceService namespace = syncServiceContainer.getBean(NamespaceService.class);
        this.analyticsService = analyticsFactory.getAnalyticsService(namespace.getNamespace());
        this.infraService = syncServiceContainer.getBean(InfraService.class);
    }

    @Override
    public void setup(ImmutableMap<String, String> baggageMap, JsonNode... parentJsonObject) throws StepFailedException {

        analyticsService.infoLogEvent("STEP_METHOD_CALLED", "name", "setup");
        if(baggageMap.containsKey("numberOfCommunitiesEachPage")){
            numberOfCommunitiesEachPage = Integer.parseInt(baggageMap.get("numberOfCommunitiesEachPage"));
            numberOfCommunityPagination = Integer.parseInt(baggageMap.get("numberOfCommunityPagination"));
            waitBetweenCommunityPaginationInMs = Long.parseLong(baggageMap.get("waitBetweenCommunityPaginationInMs"));
            shouldFail = Boolean.parseBoolean(baggageMap.get("shouldFail"));
        }
    }

    @Override
    public boolean shouldProceedWithParentObject(ImmutableMap<String, String> baggageMap, JsonNode... parentJsonObject) throws StepFailedException {
        analyticsService.infoLogEvent("STEP_METHOD_CALLED", "name", "shouldProceedWithParentObject");
        return true;
    }

    @Override
    public HttpRequestResponse startSync(JsonNode... parentJsonObject) throws StepFailedException {
        analyticsService.infoLogEvent("STEP_METHOD_CALLED", "name", "startSync");
        analyticsService.infoLogEvent("THIRD_PARTY_API_CALLED","api-name", "fbcommunity");
        try{

            JsonNode userData = parentJsonObject[0];
            String userId = userData.get("user_id").asText();
            HttpRequestResponse httpRequestResponse = new HttpRequestResponse();
            HttpRequest httpRequest = new HttpRequest();

            if(shouldFail){
                httpRequest.initGet("http://django:3000/user_communities-failed-end-point?how_many=" + numberOfCommunitiesEachPage + "&user_id=" + userId);
            }
            else {
                httpRequest.initGet("http://django:3000/user_communities?how_many=" + numberOfCommunitiesEachPage + "&user_id=" + userId);
            }

            httpRequestResponse.setRequest(httpRequest);

            analyticsService.infoLogEvent("THIRD_PARTY_API_CALLED");

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
            analyticsService.infoLogEvent("STEP_METHOD_CALLED", "name", "getNextSyncRequest");
            analyticsService.infoLogEvent("THIRD_PARTY_API_CALLED","api-name", "fbcommunity");

            JsonNode userData = parentJsonObject[0];
            String userId = userData.get("user_id").asText();
            HttpRequestResponse httpRequestResponse = new HttpRequestResponse();
            HttpRequest httpRequest = new HttpRequest();
            httpRequest.initGet("http://django:3000/user_communities?how_many=" + numberOfCommunitiesEachPage + "&user_id=" + userId);
            httpRequestResponse.setRequest(httpRequest);

            count = count + 1;
            Thread.sleep(waitBetweenCommunityPaginationInMs);
            return httpRequestResponse;
        }
        catch (Exception e){
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public boolean isValidResponse(HttpRequestResponse currentRequest, JsonNode... parentJsonObject) throws StepFailedException {

        analyticsService.infoLogEvent("STEP_METHOD_CALLED", "name", "isValidResponse");
        if(currentRequest.getResponse().getCode() == 200){
            return true;
        }
        else{
            return false;
        }
    }

    @Override
    public DagTraversalService.TraverseAction handleInvalidResponse(HttpRequestResponse currentRequest, JsonNode... parentJsonObject) throws URISyntaxException, StepFailedException {
        analyticsService.infoLogEvent("STEP_METHOD_CALLED", "name", "handleInvalidResponse");
        analyticsService.infoLogEvent("THIRD_PARTY_API_INVALID_RESPONSE");
        return null;
    }

    @Override
    public boolean isSyncComplete(HttpRequestResponse currentRequest, JsonNode... parentJsonObject) throws StepFailedException {
        analyticsService.infoLogEvent("STEP_METHOD_CALLED", "name", "isSyncComplete");

        if(count < numberOfCommunityPagination){
            return false;
        }
        else{
            return true;
        }
    }

    @Override
    public StepDataBeanMapping parseSyncResponse(HttpRequestResponse httpRequestResponse, JsonNode... parentJsonObject) {
        analyticsService.infoLogEvent("STEP_METHOD_CALLED", "name", "parseSyncResponse");
        analyticsService.infoLogEvent("THIRD_PARTY_API_RESPONSE");
        try{
            ObjectMapper objectMapper = new ObjectMapper();

            StepDataBeanMapping stepDataBeanMapping = new StepDataBeanMapping();
            String response = httpRequestResponse.getResponse().getBody();

            JsonNode jsonNode = objectMapper.readTree(response);
            stepDataBeanMapping.setParseSyncedResponseData(jsonNode.get("data").get("communities"));
            stepDataBeanMapping.setBeanClass(com.freshworks.core.data.five_zero_zero.concurrency.fb.beans.FbCommunity.class);
            return stepDataBeanMapping;
        }
        catch (Exception e){
            e.printStackTrace();
            return null;
        }

    }

    @Override
    public void closeSync() {
        analyticsService.infoLogEvent("STEP_METHOD_CALLED", "name", "closeSync");
    }
}
