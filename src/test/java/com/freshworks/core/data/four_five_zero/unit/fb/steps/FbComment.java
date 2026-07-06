package com.freshworks.core.data.four_five_zero.unit.fb.steps;

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

        analyticsService.infoLogEvent("STEP_METHOD_CALLED", "name", "setup");
        if(baggageMap.containsKey("numberOfCommentsEachPage")){
            numberOfCommentsEachPage = Integer.parseInt(baggageMap.get("numberOfCommentsEachPage"));
            numberOfCommentPagination = Integer.parseInt(baggageMap.get("numberOfCommentPagination"));
            waitBetweenCommentPaginationInMs = Long.parseLong(baggageMap.get("waitBetweenCommentPaginationInMs"));
        }
    }

    @Override
    public boolean shouldProceedWithParentObject(ImmutableMap<String, String> baggageMap, JsonNode... parentJsonObject) throws Exception {
        analyticsService.infoLogEvent("STEP_METHOD_CALLED", "name", "shouldProceedWithParentObject");
        return true;
    }

    @Override
    public HttpRequestResponse startSync(JsonNode... parentJsonObject) throws Exception {
        analyticsService.infoLogEvent("STEP_METHOD_CALLED", "name", "startSync");
        analyticsService.infoLogEvent("THIRD_PARTY_API_CALLED","api-name", "fbcomment");
        try{

            JsonNode data = parentJsonObject[0];
            String postId = data.get("post_id").asText();

            JsonNode userData = data.get("parentBean");
            String userId = userData.get("user_id").asText();

            HttpRequestResponse httpRequestResponse = new HttpRequestResponse();
            HttpRequest httpRequest = new HttpRequest();
            httpRequest.initGet("http://django:3000/post_comments?how_many=" + numberOfCommentsEachPage + "&user_id=" + userId + "&post_id=" + postId);
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
    public void filterResponse(StepDataBeanMapping stepDataBeanMapping, JsonNode... parentJsonObject) throws Exception {

        JsonNode node = stepDataBeanMapping.getParseSyncedResponseData();
    }

    @Override
    public HttpRequestResponse getNextSyncRequest(HttpRequestResponse currentRequest, JsonNode... parentJsonObject) throws Exception {
        try{

            analyticsService.infoLogEvent("STEP_METHOD_CALLED", "name", "getNextSyncRequest");
            analyticsService.infoLogEvent("THIRD_PARTY_API_CALLED","api-name", "fbcomment");

            JsonNode postData = parentJsonObject[0];
            String postId = postData.get("post_id").asText();

            JsonNode userData = postData.get("parentBean");
            String userId = userData.get("user_id").asText();

            HttpRequestResponse httpRequestResponse = new HttpRequestResponse();
            HttpRequest httpRequest = new HttpRequest();
            httpRequest.initGet("http://django:3000/post_comments?how_many=" + numberOfCommentsEachPage + "&user_id=" + userId + "&post_id=" + postId);
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

        if(count < numberOfCommentPagination){
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
            stepDataBeanMapping.setParseSyncedResponseData(jsonNode.get("data").get("comments"));
            stepDataBeanMapping.setBeanClass(com.freshworks.core.data.four_five_zero.unit.fb.beans.FbComment.class);
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
