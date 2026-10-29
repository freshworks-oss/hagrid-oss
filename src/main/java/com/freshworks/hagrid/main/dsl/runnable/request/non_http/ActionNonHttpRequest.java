package com.freshworks.hagrid.main.dsl.runnable.request.non_http;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.freshworks.hagrid.main.dsl.config.requests.RequestConfig.REQUEST_SUB_TYPE;
import com.freshworks.hagrid.main.dsl.config.requests.RequestConfig.REQUEST_TYPE;
import com.freshworks.hagrid.main.dsl.runnable.request.ActionRequest;
import com.freshworks.hagrid.main.dsl.runnable.request.ActionResponse;

import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter 
@JsonIgnoreProperties(ignoreUnknown = true)
public class ActionNonHttpRequest implements ActionRequest{

    String name;
    REQUEST_TYPE type = REQUEST_TYPE.NON_HTTP;
    REQUEST_SUB_TYPE subType = REQUEST_SUB_TYPE.NON_HTTP;

    Map<String, String> customProperties = new HashMap<>();

    @Override
    public ActionResponse executeRequest() {
        
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'executeRequest'");
    }

    @Override
    public REQUEST_TYPE getRequestType() {
        
        return REQUEST_TYPE.NON_HTTP;
    }

    @Override
    public REQUEST_SUB_TYPE getRequesSubType() {
        
        return REQUEST_SUB_TYPE.NON_HTTP;
    }
    
}
