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
import java.util.Objects;
import java.net.URISyntaxException;


@Slf4j

// Using FreshHierarchy, you can create complex DAGs like below .. 
// @FreshHierarchy(parentClass = {FbUser.class, FbCommunity.class}, rateLimit = 800, duration = 1, ignore = false)

// Creating simple DAG
@FreshHierarchy(parentClass = {FbUser.class, FbCommunity.class}, rateLimit = 800, duration = 1, ignore = false)
@Component
@Scope("prototype")
public class FbPost extends HttpAbstractStep {

    // Below is the custom logic written to fetch data from our hypothetical fb database 
    
    int numberOfPostsEachPage = 5;
    int numberOfPostPagination = 1;
    long waitBetweenPostPaginationInMs = 0;

    int count = 0;
    AnalyticsService analyticsService;


    @Override
    public void configure(SyncServiceContainer syncServiceContainer){
        Namespace namespace = syncServiceContainer.getBean(Namespace.class);
        AnalyticsFactory analyticsFactory = syncServiceContainer.getBean(AnalyticsFactory.class);
        analyticsService = analyticsFactory.getAnalyticsService(namespace.getNamespace());
    }

    @Override
    public void setup(ImmutableMap<String, String> baggageMap, JsonNode... parentJsonObject) throws StepFailedException {
         if(baggageMap.containsKey("numberOfPostsEachPage")){
            numberOfPostsEachPage = Integer.parseInt(baggageMap.get("numberOfPostsEachPage"));
            numberOfPostPagination = Integer.parseInt(baggageMap.get("numberOfPostPagination"));
            waitBetweenPostPaginationInMs = Long.parseLong(baggageMap.get("waitBetweenPostPaginationInMs"));
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

            HttpRequestResponse httpRequestResponse = new HttpRequestResponse();
            HttpRequest httpRequest = new HttpRequest();

            if(parentJsonObject[0].has("user_id")){
                JsonNode userData = parentJsonObject[0];
                String userId = userData.get("user_id").asText();
                httpRequest.initGet("http://django:3000/posts?how_many=" + numberOfPostsEachPage + "&user_id=" + userId);
            }
            else{
                JsonNode communityData = parentJsonObject[0];
                String communityId = communityData.get("community_id").asText();
                httpRequest.initGet("http://django:3000/posts?how_many=" + numberOfPostsEachPage + "&community_id=" + communityId);
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
            analyticsService.infoLogEvent("METHOD_CALLED", "name", "getNextSyncRequest");
            analyticsService.infoLogEvent("THIRD_PARTY_API_CALLED");
            HttpRequestResponse httpRequestResponse = new HttpRequestResponse();
            HttpRequest httpRequest = new HttpRequest();

            if(parentJsonObject[0].has("user_id")){
                JsonNode userData = parentJsonObject[0];
                String userId = userData.get("user_id").asText();
                httpRequest.initGet("http://django:3000/posts?how_many=" + numberOfPostsEachPage + "&user_id=" + userId);
            }
            else{
                JsonNode communityData = parentJsonObject[0];
                String communityId = communityData.get("community_id").asText();
                httpRequest.initGet("http://django:3000/posts?how_many=" + numberOfPostsEachPage + "&community_id=" + communityId);
            }

            httpRequestResponse.setRequest(httpRequest);

            count = count + 1;
            Thread.sleep(waitBetweenPostPaginationInMs);
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
        return null;
    }

    @Override
    public boolean isSyncComplete(HttpRequestResponse currentRequest, JsonNode... parentJsonObject) throws StepFailedException {
        analyticsService.infoLogEvent("METHOD_CALLED", "name", "isSyncComplete");

        if(count < numberOfPostPagination){
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
            JsonNode postData = jsonNode.get("body").get("data").get("posts");

            String userId = jsonNode.get("body").get("data").get("user_id").asText();
            ArrayNode arrayNode = objectMapper.createArrayNode();

            for(JsonNode d : postData){
                ObjectNode o = objectMapper.createObjectNode();
                o.put("user_id", userId);
                o.put("post_id", d.get("post_id").asText());
                o.put("post_title", d.get("post_title").asText());
                o.put("post_text", d.get("post_text").asText());

                arrayNode.add(o);
            }

            stepDataBeanMapping.setParseSyncedResponseData(arrayNode);
            stepDataBeanMapping.setBeanClass(com.freshworks.hagrid.beans.FbPost.class);
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