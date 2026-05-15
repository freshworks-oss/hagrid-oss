package com.freshworks.core.data.four_zero_zero.concurrency.fb.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshworks.core.CustomRegExConditionComparator;
import com.freshworks.core.shared.Namespace;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.analytics.AnalyticsFactory;
import com.freshworks.core.shared.analytics.AnalyticsService;
import com.freshworks.core.shared.infra.InfraService;
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

@Slf4j
@FreshHierarchy(parentClass = FbPost.class, rateLimit = 800, duration = 1, ignore = false)
@Component
@Scope("prototype")
@Conditional(CustomRegExConditionComparator.class)
public class FbComment extends HttpAbstractStep {

    int numberOfCommentsEachPage = 1;
    int numberOfCommentPagination = 1;
    long waitBetweenCommentPaginationInMs = 0;
    boolean shouldFail = false;

    int count = 0;
    AnalyticsService analyticsService;
    InfraService infraService;

    public FbComment() {

    }

    @Override
    public void configure(SyncServiceContainer syncServiceContainer) {
        AnalyticsFactory analyticsFactory = syncServiceContainer.getBean(AnalyticsFactory.class);
        Namespace namespace = syncServiceContainer.getBean(Namespace.class);
        this.analyticsService = analyticsFactory.getAnalyticsService(namespace.getNamespace());
        this.infraService = syncServiceContainer.getBean(InfraService.class);
    }

    @Override
    public void setup(ImmutableMap<String, String> baggageMap, JsonNode... parentJsonObject) throws StepFailedException {

        analyticsService.infoEvent("STEP_METHOD_CALLED", "name", "setup");
        if(baggageMap.containsKey("numberOfCommentsEachPage")){
            numberOfCommentsEachPage = Integer.parseInt(baggageMap.get("numberOfCommentsEachPage"));
            numberOfCommentPagination = Integer.parseInt(baggageMap.get("numberOfCommentPagination"));
            waitBetweenCommentPaginationInMs = Long.parseLong(baggageMap.get("waitBetweenCommentPaginationInMs"));
            shouldFail = Boolean.parseBoolean(baggageMap.get("shouldFail"));
        }
    }

    @Override
    public boolean shouldProceedWithParentObject(ImmutableMap<String, String> baggageMap, JsonNode... parentJsonObject) throws StepFailedException {
        analyticsService.infoEvent("STEP_METHOD_CALLED", "name", "shouldProceedWithParentObject");
        return true;
    }

    @Override
    public HttpRequestResponse startSync(JsonNode... parentJsonObject) throws StepFailedException {
        analyticsService.infoEvent("STEP_METHOD_CALLED", "name", "startSync");
        analyticsService.infoEvent("THIRD_PARTY_API_CALLED","api-name", "fbcomment");
        try{

            JsonNode data = parentJsonObject[0];
            String postId = data.get("post_id").asText();

            JsonNode userData = data.get("parentBean");
            String userId = userData.get("user_id").asText();

            HttpRequestResponse httpRequestResponse = new HttpRequestResponse();
            HttpRequest httpRequest = new HttpRequest();

            if(shouldFail){
                httpRequest.initGet("https://l3rtckyana.execute-api.us-east-1.amazonaws.com/performance-testing/posts/comments-failed-endpoint?how_many=" + numberOfCommentsEachPage + "&user_id=" + userId + "&post_id=" + postId);
            }
            else{
                httpRequest.initGet("https://l3rtckyana.execute-api.us-east-1.amazonaws.com/performance-testing/posts/comments?how_many=" + numberOfCommentsEachPage + "&user_id=" + userId + "&post_id=" + postId);
            }

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

        JsonNode node = stepDataBeanMapping.getParseSyncedResponseData();
    }

    @Override
    public HttpRequestResponse getNextSyncRequest(HttpRequestResponse currentRequest, JsonNode... parentJsonObject) throws StepFailedException {
        try{

            analyticsService.infoEvent("STEP_METHOD_CALLED", "name", "getNextSyncRequest");
            analyticsService.infoEvent("THIRD_PARTY_API_CALLED","api-name", "fbcomment");

            JsonNode postData = parentJsonObject[0];
            String postId = postData.get("post_id").asText();

            JsonNode userData = postData.get("parentBean");
            String userId = userData.get("user_id").asText();

            HttpRequestResponse httpRequestResponse = new HttpRequestResponse();
            HttpRequest httpRequest = new HttpRequest();
            httpRequest.initGet("https://l3rtckyana.execute-api.us-east-1.amazonaws.com/performance-testing/posts/comments?how_many=" + numberOfCommentsEachPage + "&user_id=" + userId + "&post_id=" + postId);
            httpRequestResponse.setRequest(httpRequest);
            count = count + 1;
            Thread.sleep(waitBetweenCommentPaginationInMs);
            return httpRequestResponse;
        }
        catch (Exception e){
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public boolean isValidResponse(HttpRequestResponse currentRequest, JsonNode... parentJsonObject) throws StepFailedException {

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
        analyticsService.infoEvent("STEP_METHOD_CALLED", "name", "handleInvalidResponse");
        analyticsService.infoEvent("THIRD_PARTY_API_INVALID_RESPONSE");
        return null;
    }

    @Override
    public boolean isSyncComplete(HttpRequestResponse currentRequest, JsonNode... parentJsonObject) throws StepFailedException {
        analyticsService.infoEvent("STEP_METHOD_CALLED", "name", "isSyncComplete");

        if(count < numberOfCommentPagination){
            return false;
        }
        else{
            return true;
        }

    }

    @Override
    public StepDataBeanMapping parseSyncResponse(HttpRequestResponse httpRequestResponse, JsonNode... parentJsonObject) {
        analyticsService.infoEvent("STEP_METHOD_CALLED", "name", "parseSyncResponse");
        analyticsService.infoEvent("THIRD_PARTY_API_RESPONSE");
        try{
            ObjectMapper objectMapper = new ObjectMapper();

            StepDataBeanMapping stepDataBeanMapping = new StepDataBeanMapping();
            String response = httpRequestResponse.getResponse().getBody();

            JsonNode jsonNode = objectMapper.readTree(response);
            stepDataBeanMapping.setParseSyncedResponseData(jsonNode.get("data").get("comments"));
            stepDataBeanMapping.setBeanClass(com.freshworks.core.data.four_zero_zero.concurrency.fb.beans.FbComment.class);
            return stepDataBeanMapping;
        }
        catch (Exception e){
            e.printStackTrace();
            return null;
        }

    }

    @Override
    public void closeSync() {
        analyticsService.infoEvent("STEP_METHOD_CALLED", "name", "closeSync");
    }
}
