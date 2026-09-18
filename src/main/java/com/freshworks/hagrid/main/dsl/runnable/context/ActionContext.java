package com.freshworks.hagrid.main.dsl.runnable.context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshworks.hagrid.main.dsl.runnable.request.ActionRequest;
import com.freshworks.hagrid.main.dsl.runnable.request.ActionResponse;
import com.freshworks.hagrid.main.dsl.runnable.request.ActionResponse.ACTION_HTTP_CODE;

import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter 
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

    public Map getContext() throws Exception{

        Map contextMap = new HashMap<>();
        contextMap.put("_input", input);
        contextMap.put("_request", actionRequest);
        contextMap.put("_response", actionResponse);
        contextMap.put("_shared", actionSharedMap);
        contextMap.put("_parent_actions", parentApiModelData);

        return contextMap;
    }

    public ParseSyncResponse parse(String responsePath, String modelName, String outputModelName, String responseType){

        ParseSyncResponse parseSyncResponse =  new ParseSyncResponse();
        parseSyncResponse.setResponsePath(responsePath);
        parseSyncResponse.setModelName(modelName);
        parseSyncResponse.setOutputModelName(outputModelName);
        parseSyncResponse.setResponseType(ACTION_HTTP_CODE.valueOf(responseType));
        return parseSyncResponse;
    }
    
    @Getter
    @Setter
    public class ParseSyncResponse{

        String responsePath;
        String modelName;
        String outputModelName;
        ACTION_HTTP_CODE responseType;

        public void parse(String responsePath, String modelName, String outputModelName, ACTION_HTTP_CODE responseType){
            this.responsePath = responsePath;
            this.modelName = modelName;
            this.outputModelName = outputModelName;
            this.responseType = responseType;
        }

        public JsonNode getDiggedResponseBody() throws Exception{
            
            JsonNode bodyNode = objectMapper.convertValue(actionResponse.getBody(), JsonNode.class);
            
            String[] split = this.responsePath.split("\\.");

            for(int i = 0; i < split.length; i++){
                bodyNode = bodyNode.get(split[i]);
            }

            return bodyNode;
        }
    }
}
