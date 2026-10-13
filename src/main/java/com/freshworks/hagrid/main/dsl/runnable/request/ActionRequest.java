package com.freshworks.hagrid.main.dsl.runnable.request;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.freshworks.hagrid.main.dsl.config.requests.RequestConfig.REQUEST_SUB_TYPE;
import com.freshworks.hagrid.main.dsl.config.requests.RequestConfig.REQUEST_TYPE;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "clazz", visible = true)
public interface ActionRequest {
    
    public ActionResponse executeRequest() throws Exception;

    public REQUEST_TYPE getRequestType();

    public REQUEST_SUB_TYPE getRequesSubType();
}
