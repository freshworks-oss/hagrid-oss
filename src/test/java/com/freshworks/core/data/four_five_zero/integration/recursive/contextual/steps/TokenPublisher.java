package com.freshworks.core.data.four_five_zero.integration.recursive.contextual.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.freshworks.core.CustomRegExConditionComparator;
import com.freshworks.core.data.four_five_zero.integration.recursive.contextual.beans.PublishedBean;
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
@FreshHierarchy(parentClass = {TokenRoute.class}, rateLimit = 800, duration = 1)
@Component
@Scope("prototype")
@Conditional(CustomRegExConditionComparator.class)
public class TokenPublisher extends NonHttpAbstractStep {

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

        JsonNode jsonNode = parentJsonObject[0];

        if(!jsonNode.get("context").isNull()){
            return true;
        }
        else{
            return false;
        }
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

        StepDataBeanMapping stepDataBeanMapping = new StepDataBeanMapping();
        stepDataBeanMapping.setParseSyncedResponseData(j);
        stepDataBeanMapping.setBeanClass(PublishedBean.class);
        return stepDataBeanMapping;
    }

    @Override
    public void closeSync() {

    }
}
