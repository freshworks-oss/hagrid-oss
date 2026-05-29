package com.freshworks.core.data.four_zero_zero.integration.recursive.contextual.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.freshworks.core.CustomRegExConditionComparator;
import com.freshworks.core.data.four_zero_zero.integration.recursive.contextual.beans.FilteredBean;
import com.freshworks.core.data.four_zero_zero.integration.recursive.contextual.beans.TransformedBean;
import com.freshworks.core.shared.Namespace;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.analytics.AnalyticsFactory;
import com.freshworks.core.shared.analytics.AnalyticsService;
import com.freshworks.core.shared.infra.InfraService;
import com.freshworks.core.traverser.Annotations.FreshHierarchy;
import com.freshworks.core.traverser.DagTraversalService;
import com.freshworks.core.traverser.NonHttpAbstractStep;
import com.freshworks.core.traverser.RequestResponseContainer;
import com.freshworks.core.traverser.StepDataBeanMapping;
import com.freshworks.core.traverser.exception.StepFailedException;
import com.google.common.collect.ImmutableMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Slf4j
@FreshHierarchy(parentClass = {TokenTransformation.class}, rateLimit = 800, duration = 1)
@Component
@Scope("prototype")
@Conditional(CustomRegExConditionComparator.class)
public class TokenFilter extends NonHttpAbstractStep {

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

        String token = jsonNode.get("token").textValue() + "_filter";
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode objectNode = objectMapper.createObjectNode();
        objectNode.put("token", token);
        objectNode.put("context", jsonNode.get("context").asText());

        requestResponseContainer.setResponse(objectNode);
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

        StepDataBeanMapping stepDataBeanMapping = new StepDataBeanMapping();
        stepDataBeanMapping.setParseSyncedResponseData(j);
        stepDataBeanMapping.setBeanClass(FilteredBean.class);
        return stepDataBeanMapping;
    }

    @Override
    public void closeSync() {

    }
}
