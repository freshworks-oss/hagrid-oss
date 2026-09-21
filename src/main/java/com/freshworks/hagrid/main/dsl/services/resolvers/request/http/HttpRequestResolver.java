package com.freshworks.hagrid.main.dsl.services.resolvers.request.http;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.freshworks.hagrid.main.dsl.config.requests.RequestConfig;
import com.freshworks.hagrid.main.dsl.config.requests.RequestConfig.REQUEST_SUB_TYPE;
import com.freshworks.hagrid.main.dsl.config.requests.RequestConfig.REQUEST_TYPE;
import com.freshworks.hagrid.main.dsl.runnable.request.ActionRequest;
import com.freshworks.hagrid.main.dsl.runnable.request.http.ActionHttpRequest;
import com.freshworks.hagrid.main.dsl.services.resolvers.request.nonhttp.NonHttpRequestResolver;

@Component
public class HttpRequestResolver {
 
    RestHttpResolver restHttpResolver;
    SoapHttpResolver soapHttpResolver;

    @Autowired 
    public HttpRequestResolver(RestHttpResolver restHttpResolver, SoapHttpResolver soapHttpResolver){
        this.restHttpResolver = restHttpResolver;
        this.soapHttpResolver = soapHttpResolver;
    }
    
    
    public ActionHttpRequest resolve(RequestConfig requestConfig, Map<String, Object> context) throws Exception{

        if(requestConfig.getSubType().equals(REQUEST_SUB_TYPE.REST)){
            return this.restHttpResolver.resolve(requestConfig, context);
        }

        else {

            return this.soapHttpResolver.resolve(requestConfig, context);
        }
    }

}
