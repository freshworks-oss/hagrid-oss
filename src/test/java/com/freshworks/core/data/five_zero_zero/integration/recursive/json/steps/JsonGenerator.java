package com.freshworks.core.data.five_zero_zero.integration.recursive.json.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.freshworks.core.CustomRegExConditionComparator;
import com.freshworks.core.data.five_zero_zero.integration.recursive.json.beans.GeneratedJson;
import com.freshworks.core.data.five_zero_zero.integration.recursive.json.beans.PrimitiveKeyValue;
import com.freshworks.core.shared.NamespaceService;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.analytics.AnalyticsFactory;
import com.freshworks.core.shared.analytics.AnalyticsService;
import com.freshworks.core.shared.infra.InfraService;
import com.freshworks.core.traverser.*;
import com.freshworks.core.traverser.Annotations.FreshHierarchy;
import com.freshworks.core.traverser.exception.StepFailedException;
import com.freshworks.core.traverser.net.RequestResponse;
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
public class JsonGenerator extends NonHttpAbstractStep {


    AnalyticsService analyticsService;
    InfraService infraService;
    JsonNode jsonNode;

    public JsonGenerator() {
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


        String jsonString = "{\n" +
                "  \"id\": 12345,\n" +
                "  \"type\": \"Object\",\n" +
                "  \"name\": \"Dummy Nested JSON Example\",\n" +
                "  \"isActive\": true,\n" +
                "  \"metadata\": {\n" +
                "    \"createdAt\": \"2025-11-28T13:21:00Z\",\n" +
                "    \"tags\": [\"example\", \"nested\", \"data\", \"structure\"],\n" +
                "    \"sourceSystem\": \"API Generator\"\n" +
                "  },\n" +
                "  \"data\": {\n" +
                "    \"level1\": {\n" +
                "      \"keyL1a\": \"Value A\",\n" +
                "      \"keyL1b\": 42,\n" +
                "      \"nestedArray\": [\n" +
                "        {\n" +
                "          \"itemName\": \"Item One\",\n" +
                "          \"itemId\": \"A1\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"itemName\": \"Item Two\",\n" +
                "          \"itemId\": \"B2\",\n" +
                "          \"details\": {\n" +
                "            \"size\": \"Large\",\n" +
                "            \"color\": \"Red\"\n" +
                "          }\n" +
                "        },\n" +
                "        {\n" +
                "          \"itemName\": \"Item Three\",\n" +
                "          \"itemId\": \"C3\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"level2\": {\n" +
                "        \"keyL2a\": \"Another Value\",\n" +
                "        \"status\": \"active\",\n" +
                "        \"level3\": {\n" +
                "          \"deepKey\": \"Deeply nested value\",\n" +
                "          \"timestamp\": 1678886400\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  },\n" +
                "  \"configuration\": null,\n" +
                "  \"notes\": \"This is a simple example demonstrating nested objects and arrays within a single JSON structure.\"\n" +
                "}\n";
        ObjectMapper objectMapper = new ObjectMapper();
        try{
            jsonNode = objectMapper.readValue(jsonString, JsonNode.class);
            requestResponseContainer.setRequest(jsonNode);
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
        JsonNode value = (JsonNode) requestResponseContainer.getRequest();
        ObjectMapper  objectMapper = new ObjectMapper();
        ObjectNode objectNode = objectMapper.createObjectNode();
        objectNode.put("jsonNode", value);
        stepDataBeanMapping.setParseSyncedResponseData(objectNode);
        stepDataBeanMapping.setBeanClass(GeneratedJson.class);

        return stepDataBeanMapping;
    }

    @Override
    public void closeSync() {
        analyticsService.infoLogEvent("STEP_METHOD_CALLED", "name", "closeSync");
    }
}
