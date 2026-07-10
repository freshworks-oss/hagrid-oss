package com.freshworks.core.data.five_zero_zero.integration.recursive.contextual.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.freshworks.core.CustomRegExConditionComparator;
import com.freshworks.core.data.five_zero_zero.integration.recursive.contextual.beans.GeneratedToken;
import com.freshworks.core.shared.NamespaceService;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.analytics.AnalyticsFactory;
import com.freshworks.core.shared.analytics.AnalyticsService;
import com.freshworks.core.shared.infra.InfraService;
import com.freshworks.core.traverser.Annotations.FreshHierarchy;
import com.freshworks.core.traverser.*;
import com.freshworks.core.traverser.exception.StepFailedException;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@FreshHierarchy(parentClass = ParentStep.class, rateLimit = 800, duration = 1)
@Component
@Scope("prototype")
@Conditional(CustomRegExConditionComparator.class)
public class TokenGenerator extends NonHttpAbstractStep {


    AnalyticsService analyticsService;
    InfraService infraService;
    JsonNode jsonNode;

    public TokenGenerator() {
    }


    @Override
    public void configure(SyncServiceContainer syncServiceContainer) {
        AnalyticsFactory analyticsFactory = syncServiceContainer.getBean(AnalyticsFactory.class);
        NamespaceService namespace = syncServiceContainer.getBean(NamespaceService.class);
        this.analyticsService = analyticsFactory.getAnalyticsService(namespace.getNamespace());
        this.infraService = syncServiceContainer.getBean(InfraService.class);
    }


    @Override
    public void setupNonHttp(ImmutableMap<String, String> baggageMap, JsonNode... parentJsonObject) throws StepFailedException {
        analyticsService.infoLogEvent("STEP_METHOD_CALLED", "name", "setup");

    }

    @Override
    public boolean shouldProceedWithParentObjectNonHttp(ImmutableMap<String, String> baggageMap, JsonNode... parentJsonObject) throws StepFailedException {
        analyticsService.infoLogEvent("STEP_METHOD_CALLED", "name", "shouldProceedWithParentObject");
        return true;
    }

    @Override
    public RequestResponseContainer startSyncNonHttp(JsonNode... parentJsonObject) throws StepFailedException {

        RequestResponseContainer requestResponseContainer = new RequestResponseContainer();
        Faker faker = new Faker();

        Map<String, String> tokenContextMapping = new HashMap<>();

        for(int i = 0; i< 100; i++) {
            if(i%2 == 0){
                tokenContextMapping.put(faker.letterify("user_???????????"), faker.letterify("context_???????"));
            }
            else{
                tokenContextMapping.put(faker.letterify("user_???????????"), null);
            }
        }

        try{
            requestResponseContainer.setRequest(tokenContextMapping);
        }
        catch (Exception e){
            e.printStackTrace();
        }

        return requestResponseContainer;
    }

    @Override
    public RequestResponseContainer executeNonHttp(RequestResponseContainer currentRequestResponse, JsonNode... parentJsonObject) throws Exception {
        return currentRequestResponse;
    }

    @Override
    public void filterResponseNonHttp(StepDataBeanMapping stepDataBeanMapping, JsonNode... parentJsonObject) throws StepFailedException {
        

    }

    @Override
    public RequestResponseContainer getNextSyncRequestNonHttp(RequestResponseContainer currentRequest, JsonNode... parentJsonObject) throws StepFailedException {

        return null;
    }

    @Override
    public boolean isValidResponseNonHttp(RequestResponseContainer currentRequest, JsonNode... parentJsonObject) throws StepFailedException {

        return true;
    }

    @Override
    public DagTraversalService.TraverseAction handleInValidResponseNonHttp(RequestResponseContainer currentRequest, JsonNode... parentJsonObject) throws Exception {
        return null;
    }


    @Override
    public boolean isSyncCompleteNonHttp(RequestResponseContainer requestResponseContainer, JsonNode... parentJsonObject) throws StepFailedException {
        return true;
    }

    @Override
    public StepDataBeanMapping parseSyncResponseNonHttp(RequestResponseContainer requestResponseContainer, JsonNode... parentJsonObject) {

        StepDataBeanMapping stepDataBeanMapping = new StepDataBeanMapping();
        HashMap<String, String> tokens = (HashMap<String, String>) requestResponseContainer.getRequest();
        ObjectMapper  objectMapper = new ObjectMapper();
        ArrayNode arrayNode = objectMapper.createArrayNode();

        for(Map.Entry<String, String> entry : tokens.entrySet()){
            ObjectNode objectNode = objectMapper.createObjectNode();
            objectNode.put("token", entry.getKey());
            objectNode.put("context", entry.getValue());
            arrayNode.add(objectNode);
        }

        stepDataBeanMapping.setParseSyncedResponseData(arrayNode);
        stepDataBeanMapping.setBeanClass(GeneratedToken.class);

        return stepDataBeanMapping;
    }

    @Override
    public void closeSync() {
        analyticsService.infoLogEvent("STEP_METHOD_CALLED", "name", "closeSync");
    }
}
