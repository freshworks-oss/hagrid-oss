package com.freshworks.core.data.five_zero_zero.unit.dag.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshworks.core.CustomRegExConditionComparator;
import com.freshworks.core.data.five_zero_zero.unit.dag.beans.Application;
import com.freshworks.core.traverser.*;
import com.freshworks.core.traverser.Annotations.FreshHierarchy;
import com.freshworks.core.traverser.exception.StepFailedException;
import com.freshworks.core.traverser.net.http.HttpRequest;
import com.freshworks.core.traverser.net.http.HttpRequestResponse;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import io.micrometer.core.instrument.LongTaskTimer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@FreshHierarchy(parentClass = ParentStep.class, rateLimit = 800, duration = 1, ignore = true)
@Component
@Scope("prototype")
@Conditional(CustomRegExConditionComparator.class)
public class TestIgnored extends HttpAbstractStep {
    String token;
    LongTaskTimer.Sample taskId = null;



    @Override
    public void setup(ImmutableMap<String, String> baggageMap, JsonNode... parentJsonObject) throws StepFailedException {

    }

    @Override
    public boolean shouldProceedWithParentObject(ImmutableMap<String, String> baggageMap, JsonNode... parentJsonObject) {
        return true;
    }

    @Override
    public HttpRequestResponse startSync(JsonNode... parentJsonObject) {
        HttpRequestResponse requestResponse = new HttpRequestResponse();
        String url = "https://graph.microsoft.com/v1.0/applications?$count=true&$top=999&$select=appId,displayName";
        try{
            HttpRequest request = new HttpRequest(url);
            request.setHeader("Authorization",token);
            requestResponse.setRequest(request);
            return requestResponse;
        }
        catch(Exception e){
            log.error("Error is {}", e.getStackTrace());
            return null;
        }
    }

    @Override
    public void filterResponse(StepDataBeanMapping stepDataBeanMapping, JsonNode... parentJsonObject) {


//        Iterator<JsonNode> it = jsonNode.iterator();
//        while(it.hasNext()){
//            JsonNode j = it.next();
//            if(Boolean.FALSE.equals(j.get("displayName").asText().toLowerCase().contains("jamf "))){
//                it.remove();
//                metric.increment("total_object_dropped", 1, "step", "application");
//            }
//        }
    }

    @Override
    public HttpRequestResponse getNextSyncRequest(HttpRequestResponse currentRequest, JsonNode... parentJsonObject) {

        try{

            ObjectMapper objectMapper = new ObjectMapper();
            String responseContent = currentRequest.getResponse().getBody();
            JsonNode node = objectMapper.readTree(responseContent);
            JsonNode marker = node.get("@odata.nextLink");
            String url = marker.asText();

            HttpRequest request = new HttpRequest(url);
            request.setHeader("Authorization",token);
            currentRequest.setRequest(request);
            return currentRequest;
        }
        catch(Exception e){
            log.error("Error is {}", e.getStackTrace());
            return null;
        }
    }

    @Override
    public boolean isValidResponse(HttpRequestResponse currentRequest, JsonNode... parentJsonObject) throws StepFailedException {

        if(currentRequest.getResponse().getCode() == 200){
            return true;
        }
        else{
            return false;
        }
    }

    @Override
    public DagTraversalService.TraverseAction handleInvalidResponse(HttpRequestResponse currentRequest,  JsonNode... parentJsonObject) throws URISyntaxException {
        DagTraversalService.TraverseAction traverseAction = new DagTraversalService.TraverseAction();
        int statusCode = currentRequest.getResponse().getCode();


        if(statusCode == 429) {
            HashMap<String, Object> httpHeaders= currentRequest.getResponse().getHeaders("Retry-After");
            Long waitTime = Long.parseLong((String)httpHeaders.get("Retry-After"));
            traverseAction.holdAndReTry(waitTime, TimeUnit.SECONDS);
        }
        if(statusCode == 403){
            traverseAction.abortCurrentParentAndContinueWithNextParentInstance();
        }
        if (statusCode == 401){

            this.token = Authentication.getAuthtoken();
            String uri = currentRequest.getRequest().getRequestUri();
            HttpRequest request = new HttpRequest(uri);
            request.setHeader("Authorization",token);
            HttpRequestResponse requestResponse = new HttpRequestResponse();
            requestResponse.setRequest(request);
            traverseAction.retryWithNewRequest(requestResponse);
        }

        return traverseAction;
    }

    @Override
    public boolean isSyncComplete(HttpRequestResponse currentRequest, JsonNode... parentJsonObject) {
        try{
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode node = objectMapper.readTree(currentRequest.getResponse().getBody());
            JsonNode marker = node.get("@odata.nextLink");
            if(marker == null){
                return true;
            }
        }
        catch(Exception e){
            log.error("Error is {}", e.getStackTrace());
        }
        return false;
    }

    @Override
    public StepDataBeanMapping parseSyncResponse(HttpRequestResponse httpRequestResponse, JsonNode... parentJsonObject) {

        try{
            ObjectMapper objectMapper = new ObjectMapper();
            String response = httpRequestResponse.getResponse().getBody();
            StepDataBeanMapping stepDataBeanMapping = new StepDataBeanMapping();
            stepDataBeanMapping.setBeanClass(Application.class);
            stepDataBeanMapping.setParseSyncedResponseData(objectMapper.readTree(response));
            return stepDataBeanMapping;
        }
        catch (Exception e){

        }

        return null;
    }

    @Override
    public void closeSync() {
        taskId.stop();
    }
}
