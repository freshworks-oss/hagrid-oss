package com.freshworks.core.data.four_zero_zero.integration.recursive.json.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.freshworks.core.CustomRegExConditionComparator;
import com.freshworks.core.data.four_zero_zero.integration.recursive.json.beans.GeneratedJson;
import com.freshworks.core.data.four_zero_zero.integration.recursive.json.beans.NonPrimitiveKeyValue;
import com.freshworks.core.shared.Namespace;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.analytics.AnalyticsFactory;
import com.freshworks.core.shared.analytics.AnalyticsService;
import com.freshworks.core.shared.infra.InfraService;
import com.freshworks.core.traverser.*;
import com.freshworks.core.traverser.Annotations.CustomDagNode;
import com.freshworks.core.traverser.Annotations.FreshHierarchy;
import com.freshworks.core.traverser.exception.StepFailedException;
import com.freshworks.core.traverser.net.http.HttpRequestResponse;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.cert.ocsp.Req;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.net.URISyntaxException;
import java.util.*;

@Slf4j
@FreshHierarchy(parentClass = {JsonGenerator.class, JsonTraverser.class}, rateLimit = 800, duration = 1)
@Component
@Scope("prototype")
@Conditional(CustomRegExConditionComparator.class)
public class JsonTraverser extends NonHttpAbstractStep {

    AnalyticsService analyticsService;
    InfraService infraService;

    @Override
    public void configure(SyncServiceContainer syncServiceContainer) {
        AnalyticsFactory analyticsFactory = syncServiceContainer.getBean(AnalyticsFactory.class);
        Namespace namespace = syncServiceContainer.getBean(Namespace.class);
        this.analyticsService = analyticsFactory.getAnalyticsService(namespace.getNamespace());
        this.infraService = syncServiceContainer.getBean(InfraService.class);
    }


    @Override
    public void setupNonHttp(ImmutableMap<String, String> baggageMap, JsonNode... parentJsonObject) throws Exception {

    }

    @Override
    public boolean shouldProceedWithParentObjectNonHttp(ImmutableMap<String, String> baggageMap, JsonNode... parentJsonObject) throws StepFailedException {

        if(parentJsonObject[0].get("jsonNode").isEmpty()){
            return false;
        }

        return true;
    }

    @Override
    public RequestResponseContainer startSyncNonHttp(JsonNode... parentJsonObject) throws StepFailedException {

        JsonNode jsonNode = parentJsonObject[0];

        RequestResponseContainer requestResponseContainer = new RequestResponseContainer();
        requestResponseContainer.setRequest(jsonNode);
        return requestResponseContainer;
    }

    @Override
    public RequestResponseContainer executeNonHttp(RequestResponseContainer requestResponseContainer, JsonNode... parentJsonObject){

        JsonNode jsonNode = (JsonNode) requestResponseContainer.getRequest();
        requestResponseContainer.setResponse(jsonNode);
        return requestResponseContainer;
    }

    @Override
    public void filterResponseNonHttp(StepDataBeanMapping stepDataBeanMapping, JsonNode... parentJsonObject) throws Exception {

    }

    @Override
    public RequestResponseContainer getNextSyncRequestNonHttp(RequestResponseContainer currentRequest, JsonNode... parentJsonObject) throws Exception {
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
    public boolean isSyncCompleteNonHttp(RequestResponseContainer currentRequest, JsonNode... parentJsonObject) throws StepFailedException {
        return true;
    }

    @Override
    public StepDataBeanMapping parseSyncResponseNonHttp(RequestResponseContainer requestResponseContainer, JsonNode... parentJsonObject) {

        JsonNode j = (JsonNode) requestResponseContainer.getResponse();
        Iterator<Map.Entry<String, JsonNode>> s = j.get("jsonNode").fields();
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode rootNode = objectMapper.createObjectNode();

        while(s.hasNext()){
            Map.Entry<String, JsonNode> mainEntry = s.next();
            String mainKey = mainEntry.getKey();
            if (mainEntry.getValue().isObject()) {
                Iterator<Map.Entry<String, JsonNode>> objectEntry = mainEntry.getValue().fields();
                while(objectEntry.hasNext()){
                    Map.Entry<String, JsonNode> entry = objectEntry.next();
                    if(!entry.getValue().isObject()){
                        System.out.println(mainKey + ": " + entry.getKey() + "---> " + entry.getValue());
                    }
                    else{
                        rootNode.put(mainKey + ":" + entry.getKey(), entry.getValue());
                    }
                }
            }
            else {
                System.out.println(mainKey + ": " + mainEntry.getValue());
            }
        }

        ObjectNode x = objectMapper.createObjectNode();
        x.put("jsonNode", rootNode);

        StepDataBeanMapping stepDataBeanMapping = new StepDataBeanMapping();
        stepDataBeanMapping.setParseSyncedResponseData(x);
        stepDataBeanMapping.setBeanClass(NonPrimitiveKeyValue.class);

        return stepDataBeanMapping;
    }

    @Override
    public void closeSync() {

    }
}
