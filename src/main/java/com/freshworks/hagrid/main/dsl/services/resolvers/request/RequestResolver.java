package com.freshworks.uip.worker.hagrid.dsl.services.resolvers.request;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshworks.uip.worker.hagrid.dsl.config.model.ApiModelConfig;
import com.freshworks.uip.worker.hagrid.dsl.config.requests.RequestConfig;
import com.freshworks.uip.worker.hagrid.dsl.config.requests.RequestConfig.REQUEST_TYPE;
import com.freshworks.uip.worker.hagrid.dsl.runnable.context.ActionContext;
import com.freshworks.uip.worker.hagrid.dsl.runnable.request.ActionRequest;
import com.freshworks.uip.worker.hagrid.dsl.runnable.request.http.ActionHttpRequest;
import com.freshworks.uip.worker.hagrid.dsl.services.resolvers.request.http.HttpRequestResolver;
import com.freshworks.uip.worker.hagrid.dsl.services.resolvers.request.nonhttp.NonHttpRequestResolver;

@Component 
public class RequestResolver {
    
    ObjectMapper objectMapper = new ObjectMapper();
    HttpRequestResolver httpRequestResolver;
    NonHttpRequestResolver nonHttpRequestResolver;

    public RequestResolver(HttpRequestResolver httpRequestResolver, NonHttpRequestResolver nonHttpRequestResolver){
        this.httpRequestResolver = httpRequestResolver;
        this.nonHttpRequestResolver = nonHttpRequestResolver;
    }

    public ActionRequest resolve(RequestConfig requestConfig, ActionContext actionContext) throws Exception{
        
        if(requestConfig.getType().toString().equalsIgnoreCase(REQUEST_TYPE.HTTP.toString())){
            return this.httpRequestResolver.resolve(requestConfig, (Map<String, Object>)actionContext.getActionSharedMap().get("_resolved_request_params"));
        }
        else{
            return this.nonHttpRequestResolver.resolve(requestConfig, (Map<String, Object>)actionContext.getActionSharedMap().get("_resolved_request_params"));
        }
    }

}
