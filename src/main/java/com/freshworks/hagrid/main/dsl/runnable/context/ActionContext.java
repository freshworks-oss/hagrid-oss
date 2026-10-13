package com.freshworks.hagrid.main.dsl.runnable.context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshworks.hagrid.main.dsl.runnable.request.ActionRequest;
import com.freshworks.hagrid.main.dsl.runnable.request.ActionResponse;
import com.freshworks.hagrid.main.dsl.runnable.request.ActionResponse.ACTION_HTTP_CODE;

import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter 
@JsonIgnoreProperties ({"objectMapper", "input", "actionRequest" , "actionResponse", "actionSharedMap", "parentApiModelData"})
public class ActionContext {

    ObjectMapper objectMapper = new ObjectMapper();

    // This holds the graph input
    Map<String, Object> input = new HashMap();

    // This holds the last request 
    ActionRequest actionRequest;


    // This holds the last response
    ActionResponse actionResponse;


    // This holds the shared variables
    Map<String, Object> actionSharedMap = new HashMap();

    // This holds the parent data 
    List<Map<String, Object>> parentApiModelData = new ArrayList<>();

    Map context = new HashMap<>();


    public Map getContext() throws Exception{

        context.put("_input", input);
        context.put("_request", objectMapper.convertValue(actionRequest, Map.class));
        context.put("_response", objectMapper.convertValue(actionResponse, Map.class));
        context.put("_shared", actionSharedMap);
        context.put("_parent_actions", parentApiModelData);

        return context;
    }

    public void setInput(Map input){

        this.input = input;
        context.put("_input", input);
    }

    public void setActionRequest(ActionRequest actionRequest){
        this.actionRequest = actionRequest;
        context.put("_request", objectMapper.convertValue(actionRequest, Map.class));
    }

    public void setActionResponse(ActionResponse actionResponse){
        this.actionResponse = actionResponse;
        context.put("_response", objectMapper.convertValue(actionResponse, Map.class));
    }


    public void setActionSharedMap(Map actionSharedMap){
        this.actionSharedMap = actionSharedMap;
        context.put("_shared", actionSharedMap);
    }


    public void setParentApiModelData(List parentData){
        this.parentApiModelData = parentData;
        context.put("_parent_actions", parentData);
    }


    public void setContext(Map context){

        this.input = (Map)context.get("_input");
        this.actionRequest = objectMapper.convertValue(context.get("_request"), ActionRequest.class);
        this.actionResponse = objectMapper.convertValue(context.get("_response"), ActionResponse.class);
        this.actionSharedMap = (Map)context.get("_shared");
        this.parentApiModelData = objectMapper.convertValue(context.get("_parent_actions"), new TypeReference<List<Map<String, Object>>>(){});
    }

    public ParseSyncResponse parse(Map parsedResponse, String responsePath, String modelName, String outputModelName, String responseType){

        ParseSyncResponse parseSyncResponse =  new ParseSyncResponse();
        parseSyncResponse.setParsedResponse(parsedResponse);
        parseSyncResponse.setResponsePath(responsePath);
        parseSyncResponse.setModelName(modelName);
        parseSyncResponse.getOutputConfigModelNameList().add(outputModelName);
        parseSyncResponse.setResponseType(ACTION_HTTP_CODE.valueOf(responseType));
        return parseSyncResponse;
    }
    
    @Getter
    @Setter
    public class ParseSyncResponse{

        Map parsedResponse;
        String responsePath;
        String modelName;
        List<String> outputConfigModelNameList = new ArrayList<>();
        ACTION_HTTP_CODE responseType;

        public void parse(Map parsedResponse, String responsePath, String modelName, String outputModelName, ACTION_HTTP_CODE responseType){
            this.parsedResponse = parsedResponse;
            this.responsePath = responsePath;
            this.modelName = modelName;
            this.outputConfigModelNameList.add(outputModelName);
            this.responseType = responseType;
        }

        public JsonNode getDiggedResponseBody() throws Exception{
            
            JsonNode bodyNode = objectMapper.convertValue(parsedResponse, JsonNode.class);
            
            String[] split = this.responsePath.split("\\.");

            for(int i = 0; i < split.length; i++){
                bodyNode = bodyNode.get(split[i]);
            }

            return bodyNode;
        }
    }
}
