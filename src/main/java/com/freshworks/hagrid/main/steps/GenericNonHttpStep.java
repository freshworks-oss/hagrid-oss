package com.freshworks.hagrid.main.steps;

import java.util.HashMap;
import java.util.Map;

import org.checkerframework.checker.units.qual.s;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.freshworks.hagrid.main.SharedActionServiceMap;
import com.freshworks.hagrid.main.beans.*;
import com.freshworks.hagrid.shared.NamespaceService;
import com.freshworks.hagrid.shared.SyncServiceContainer;
import com.freshworks.hagrid.traverser.DagTraversalService;
import com.freshworks.hagrid.traverser.NonHttpAbstractStep;
import com.freshworks.hagrid.traverser.ParentStep;
import com.freshworks.hagrid.traverser.RequestResponseContainer;
import com.freshworks.hagrid.traverser.StepDataBeanMapping;
import com.freshworks.hagrid.traverser.Annotations.FreshHierarchy;
import com.freshworks.hagrid.traverser.exception.StepFailedException;
import com.freshworks.hagrid.main.dsl.services.ActionService;
import com.google.common.collect.ImmutableMap;

import groovy.xml.Namespace;

@Component
@Scope("prototype")
@FreshHierarchy(parentClass = ParentStep.class, rateLimit = 5, duration = 5)
public class GenericNonHttpStep extends NonHttpAbstractStep{

    ObjectMapper objectMapper = new ObjectMapper();
    private ActionService actionService;
    private String actionName;
    private String subActionName;
    String namespace;
    private ImmutableMap<String, String> actionInput;
    SyncServiceContainer syncServiceContainer;

    RequestResponseContainer lastContainer;
    JsonNode[] lastParentJsonNodes;

    @Autowired
    public GenericNonHttpStep(ActionService actionService) {
        this.actionService = actionService;
    }

    @Override
    public void setupNonHttp(ImmutableMap<String, String> actionInput, JsonNode... parentJsonObject) throws StepFailedException{

        // First configure the action service 
        this.actionService.setup(parentJsonObject);
    }

    public void setSubActionName(String subActionName){
        this.subActionName = subActionName;
    }

    public boolean shouldProceedWithParentObjectNonHttp(ImmutableMap<String, String> baggageMap, JsonNode... parentJsonObject) throws Exception{
        
        this.syncServiceContainer = getSyncServiceContainer();
        NamespaceService namespaceService = this.syncServiceContainer.getBean(NamespaceService.class);
        String namespace = namespaceService.getNamespace();
        this.actionInput = baggageMap;
        this.actionName = actionInput.get("actionName");

        Map<String, Object> newInputMap = new HashMap<>();

        for(Map.Entry<String, String>in : actionInput.entrySet()){

            newInputMap.put(in.getKey(), in.getValue());
        }
        getSyncServiceContainer().add(this.actionService, ActionService.class);

        // First configure the action service 
        this.actionService.configure(actionName, subActionName, newInputMap, parentJsonObject);

        SharedActionServiceMap.add(namespace, actionName, subActionName, actionService);
        
        // Now starts calling the the step methods 
        return this.actionService.shouldProceedWithParent(parentJsonObject);
    }

    public RequestResponseContainer startSyncNonHttp(JsonNode... parentJsonObject) throws StepFailedException{

        try{
            RequestResponseContainer container = this.actionService.startAction(parentJsonObject);
            lastContainer = container;
            lastParentJsonNodes = parentJsonObject;
            return container;
        }
        catch (Exception e){

            e.printStackTrace();
            return null;
        }
        
    }


    public RequestResponseContainer executeNonHttp(RequestResponseContainer currentRequestResponse, JsonNode... parentJsonObject){

        RequestResponseContainer requestResponseContainer = this.actionService.execute(currentRequestResponse, parentJsonObject);
        lastContainer = requestResponseContainer;
        lastParentJsonNodes = parentJsonObject;
        return requestResponseContainer;
    }
    

    public boolean isValidResponseNonHttp(RequestResponseContainer currentRequest, JsonNode... parentJsonObject) throws StepFailedException{

        return this.actionService.isValidResponse(currentRequest, parentJsonObject);
    }


    public DagTraversalService.TraverseAction handleInValidResponseNonHttp(RequestResponseContainer currentRequest, JsonNode... parentJsonObject){

        return this.actionService.handleInValidResponseNonHttp(currentRequest, parentJsonObject);
    }

    public RequestResponseContainer getNextSyncRequestNonHttp(RequestResponseContainer currentRequest, JsonNode... parentJsonObject) throws StepFailedException{

        try{

            currentRequest = this.actionService.getNextAction(currentRequest, parentJsonObject);
            lastContainer = currentRequest;
            lastParentJsonNodes = parentJsonObject;

            return currentRequest;
        }
        catch(Exception e){

            e.printStackTrace();
            return null;
        }

    }


    public boolean isSyncCompleteNonHttp(RequestResponseContainer currentRequest, JsonNode... parentJsonObject) throws StepFailedException{

        return !this.actionService.hasMore(currentRequest, parentJsonObject);
    }

    
    public StepDataBeanMapping parseSyncResponseNonHttp(RequestResponseContainer currentRequestResponse, JsonNode... parentJsonObject){

        JsonNode response = this.actionService.parseSyncResponseNonHttp(currentRequestResponse, parentJsonObject);
        ObjectNode node = objectMapper.createObjectNode();
        node.set("data", response);
        node.put("actionName", this.actionName);

        StepDataBeanMapping stepDataBeanMapping = new StepDataBeanMapping();
        stepDataBeanMapping.setBeanClass(GenericBean.class);
        stepDataBeanMapping.setParseSyncedResponseData(node);
        return stepDataBeanMapping;
    }

    @Override
    public void closeSync(){

        this.actionService.afterAction(lastContainer, lastParentJsonNodes);
    }
}
