package com.freshworks.hagrid.main.dsl.services.resolvers.request.nonhttp;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.freshworks.hagrid.main.dsl.config.requests.RequestConfig;
import com.freshworks.hagrid.main.dsl.runnable.request.non_http.ActionNonHttpRequest;

@Component 
public class NonHttpRequestResolver {
        
    public ActionNonHttpRequest resolve(RequestConfig requestConfig, Map<String, Object> context) throws Exception{

        throw new IllegalAccessError("Resolver for non http request is not yet implemented");
    }

}
