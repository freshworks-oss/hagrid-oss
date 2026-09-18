package com.freshworks.uip.worker.hagrid.dsl.services.resolvers.request.nonhttp;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.freshworks.uip.worker.hagrid.dsl.config.requests.RequestConfig;
import com.freshworks.uip.worker.hagrid.dsl.runnable.request.non_http.ActionNonHttpRequest;

@Component 
public class NonHttpRequestResolver {
        
    public ActionNonHttpRequest resolve(RequestConfig requestConfig, Map<String, Object> context) throws Exception{

        throw new IllegalAccessError("Resolver for non http request is not yet implemented");
    }

}
