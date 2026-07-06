package com.freshworks.hagrid.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.freshworks.core.shared.Namespace;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.analytics.AnalyticsFactory;
import com.freshworks.core.shared.analytics.AnalyticsService;
import com.freshworks.core.traverser.AbstractStep;
import com.freshworks.core.traverser.Annotations.FreshHierarchy;
import com.freshworks.core.traverser.DagTraversalService;
import com.freshworks.core.traverser.HttpAbstractStep;
import com.freshworks.core.traverser.StepDataBeanMapping;
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
import java.util.Objects;

import javax.management.ObjectName;

@Slf4j
@FreshHierarchy(parentClass = FbUser.class, rateLimit = 800, duration = 1)
@Component
@Scope("prototype")
public class FbCommunity extends HttpAbstractStep {

    // Below is the custom logic written to fetch data from our hypothetical fb database    
    int numberOfCommunitiesEachPage = 5;
    int numberOfCommunityPagination = 1;
    long waitBetweenCommunityPaginationInMs = 0;

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
        
        analyticsService.infoLogEvent("STEP_METHOD_CALLED", "name", "setup");
        if(baggageMap.containsKey("numberOfCommunitiesEachPage")){
            numberOfCommunitiesEachPage = Integer.parseInt(baggageMap.get("numberOfCommunitiesEachPage"));
            numberOfCommunityPagination = Integer.parseInt(baggageMap.get("numberOfCommunityPagination"));
            waitBetweenCommunityPaginationInMs = Long.parseLong(baggageMap.get("waitBetweenCommunityPaginationInMs"));
        }
    }

    @Override
    public boolean shouldProceedWithParentObject(ImmutableMap<String, String> baggageMap, JsonNode... parentJsonObject) throws StepFailedException {
        analyticsService.infoLogEvent("METHOD_CALLED", "name", "shouldProceedWithParentObject");
        return true;
    }

    @Override
    public HttpRequestResponse startSync(JsonNode... parentJsonObject) throws StepFailedException {
        analyticsService.infoLogEvent("METHOD_CALLED", "name", "startSync");
        try{
            JsonNode userData = parentJsonObject[0];
            String userId = userData.get("user_id").asText();
            HttpRequestResponse httpRequestResponse = new HttpRequestResponse();
            HttpRequest httpRequest = new HttpRequest();
            httpRequest.initGet("http://django:3000/user_communities?how_many=" + numberOfCommunitiesEachPage + "&user_id=" + userId);
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
            analyticsService.infoLogEvent("METHOD_CALLED", "name", "getNextSyncRequest");
            analyticsService.infoLogEvent("THIRD_PARTY_API_CALLED");

            JsonNode userData = parentJsonObject[0];
            String userId = userData.get("user_id").asText();
            HttpRequestResponse httpRequestResponse = new HttpRequestResponse();
            HttpRequest httpRequest = new HttpRequest();
            httpRequest.initGet("http://django:3000/user_communities?how_many=" + numberOfCommunitiesEachPage + "&user_id=" + userId);
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

        analyticsService.infoLogEvent("METHOD_CALLED", "name", "isValidResponse");
        if(currentRequest.getResponse().getCode() == 200){
            return true;
        }
        else{
            return false;
        }
    }

    @Override
    public DagTraversalService.TraverseAction handleInvalidResponse(HttpRequestResponse currentRequest, JsonNode... parentJsonObject) throws URISyntaxException, StepFailedException {
        analyticsService.infoLogEvent("METHOD_CALLED", "name", "handleInvalidResponse");
        analyticsService.infoLogEvent("THIRD_PARTY_API_INVALID_RESPONSE");
        DagTraversalService.TraverseAction traverseAction = new DagTraversalService.TraverseAction();
        return null;
    }

    @Override
    public boolean isSyncComplete(HttpRequestResponse currentRequest, JsonNode... parentJsonObject) throws StepFailedException {
        analyticsService.infoLogEvent("METHOD_CALLED", "name", "isSyncComplete");

        if(count < numberOfCommunityPagination){
            return false;
        }
        else{
            return true;
        }
    }

    @Override
    public StepDataBeanMapping parseSyncResponse(HttpRequestResponse httpRequestResponse, JsonNode... parentJsonObject) {
        analyticsService.infoLogEvent("METHOD_CALLED", "name", "parseSyncResponse");
        analyticsService.infoLogEvent("THIRD_PARTY_API_RESPONSE");
        try{
            ObjectMapper objectMapper = new ObjectMapper();

            StepDataBeanMapping stepDataBeanMapping = new StepDataBeanMapping();
            String response = httpRequestResponse.getResponse().getBody();

            JsonNode jsonNode = objectMapper.readTree(response);
            JsonNode communityData = jsonNode.get("body").get("data").get("communities");
            String userId = jsonNode.get("body").get("data").get("user_id").asText();
            ArrayNode arrayNode = objectMapper.createArrayNode();

            for(JsonNode d : communityData){
                ObjectNode o = objectMapper.createObjectNode();
                o.put("user_id", userId);
                o.put("community_id", d.get("community_id").asText());
                o.put("community_title", d.get("community_title").asText());
                o.put("community_description", d.get("community_description").asText());

                arrayNode.add(o);
            }
            
            stepDataBeanMapping.setParseSyncedResponseData(arrayNode);
            stepDataBeanMapping.setBeanClass(com.freshworks.hagrid.beans.FbCommunity.class);
            return stepDataBeanMapping;
        }
        catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void closeSync() {
        analyticsService.infoLogEvent("METHOD_CALLED", "name", "closeSync");
    }
}