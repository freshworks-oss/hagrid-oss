// package com.freshworks.hagrid.steps;

// import com.fasterxml.jackson.databind.JsonNode;
// import com.fasterxml.jackson.databind.ObjectMapper;
// import com.freshworks.core.shared.Namespace;
// import com.freshworks.core.shared.SyncServiceContainer;
// import com.freshworks.core.shared.analytics.AnalyticsFactory;
// import com.freshworks.core.shared.analytics.AnalyticsService;
// import com.freshworks.core.traverser.*;
// import com.freshworks.core.traverser.Annotations.FreshHierarchy;
// import com.freshworks.core.traverser.exception.StepFailedException;
// import com.freshworks.core.traverser.net.http.HttpRequest;
// import com.freshworks.core.traverser.net.http.HttpRequestResponse;
// import com.google.common.base.Optional;
// import com.google.common.collect.ImmutableMap;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.context.annotation.Scope;
// import org.springframework.stereotype.Component;

// import java.net.URISyntaxException;

// // @Slf4j
// // @FreshHierarchy(parentClass = ParentStep.class, rateLimit = 800, duration = 1)
// // @Component
// // @Scope("prototype")
// public class FbUserSingleton extends HttpAbstractStep {

//     AnalyticsService analyticsService;
//     AnalyticsFactory analyticsFactory;
//     int size;


//     @Override
//     public void configure(SyncServiceContainer syncServiceContainer){
//         Namespace namespace = syncServiceContainer.getBean(Namespace.class);
//         AnalyticsFactory analyticsFactory = syncServiceContainer.getBean(AnalyticsFactory.class);
//         analyticsService = analyticsFactory.getAnalyticsService(namespace.getNamespace());

//     }

//     @Override
//     public void setup(ImmutableMap<String, String> baggageMap, JsonNode... parentJsonObject) throws StepFailedException {
//         analyticsService.infoEvent("METHOD_CALLED", "name", "setup");
//         size = Integer.parseInt(baggageMap.get("size"));
//     }

//     @Override
//     public boolean shouldProceedWithParentObject(ImmutableMap<String, String> baggageMap, JsonNode... parentJsonObject) throws StepFailedException {
//         analyticsService.infoEvent("METHOD_CALLED", "name", "shouldProceedWithParentObject");
//         return true;
//     }

//     @Override
//     public HttpRequestResponse startSync(JsonNode... parentJsonObject) throws StepFailedException {
//         analyticsService.infoEvent("METHOD_CALLED", "name", "startSync");
//         try{
//             HttpRequestResponse httpRequestResponse = new HttpRequestResponse();
//             HttpRequest httpRequest = new HttpRequest();
//             httpRequest.initGet("https://l3rtckyana.execute-api.us-east-1.amazonaws.com/performance-testing/user?how_many=" + size);
//             httpRequestResponse.setRequest(httpRequest);

//             analyticsService.infoEvent("THIRD_PARTY_API_CALLED");
//             return httpRequestResponse;
//         }
//         catch (Exception e){
//             e.printStackTrace();
//             return null;
//         }
//     }

//     @Override
//     public void filterResponse(StepDataBeanMapping stepDataBeanMapping, JsonNode... parentJsonObject) throws Exception {

//     }

//     @Override
//     public HttpRequestResponse getNextSyncRequest(HttpRequestResponse currentRequest, JsonNode... parentJsonObject) throws Exception {
//         return null;
//     }


//     @Override
//     public boolean isValidResponse(HttpRequestResponse currentRequest, JsonNode... parentJsonObject) throws StepFailedException {

//         if(currentRequest.getResponse().getCode() == 200){
//             analyticsService.infoEvent("METHOD_CALLED", "name", "isValidResponse", "response_code", "positive");
//             return true;
//         }
//         else{
//             analyticsService.infoEvent("METHOD_CALLED", "name", "isValidResponse", "response_code", "negative");
//             return false;
//         }
//     }

//     @Override
//     public DagTraversalService.TraverseAction handleInvalidResponse(HttpRequestResponse currentRequest, JsonNode... parentJsonObject) throws URISyntaxException, StepFailedException {
//         analyticsService.infoEvent("METHOD_CALLED", "name", "handleInvalidResponse");
//         analyticsService.infoEvent("THIRD_PARTY_API_INVALID_RESPONSE");
//         DagTraversalService.TraverseAction traverseAction = new DagTraversalService.TraverseAction();
//         return null;
//     }

//     @Override
//     public boolean isSyncComplete(HttpRequestResponse currentRequest, JsonNode... parentJsonObject) throws StepFailedException {
//         analyticsService.infoEvent("METHOD_CALLED", "name", "isSyncComplete");
//         return true;

//     }

//     @Override
//     public StepDataBeanMapping parseSyncResponse(HttpRequestResponse httpRequestResponse, JsonNode... parentJsonObject) {
//         analyticsService.infoEvent("METHOD_CALLED", "name", "parseSyncResponse");
//         analyticsService.infoEvent("THIRD_PARTY_API_RESPONSE");
//         try{
//             ObjectMapper objectMapper = new ObjectMapper();

//             StepDataBeanMapping stepDataBeanMapping = new StepDataBeanMapping();
//             String response = httpRequestResponse.getResponse().getBody();

//             JsonNode jsonNode = objectMapper.readTree(response);
//             stepDataBeanMapping.setParseSyncedResponseData(jsonNode.get("body").get("data"));
//             stepDataBeanMapping.setBeanClass(com.freshworks.hagrid.beans.UserSingleton.class);
//             return stepDataBeanMapping;
//         }
//         catch (Exception e){
//             e.printStackTrace();
//             return null;
//         }
//     }

//     @Override
//     public void closeSync() {
//         analyticsService.infoEvent("METHOD_CALLED", "name", "closeSync");
//     }
// }
