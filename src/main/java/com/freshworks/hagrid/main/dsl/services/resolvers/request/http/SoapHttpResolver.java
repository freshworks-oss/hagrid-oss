package com.freshworks.hagrid.main.dsl.services.resolvers.request.http;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.freshworks.hagrid.main.dsl.config.requests.RequestConfig;
import com.freshworks.hagrid.main.dsl.runnable.request.http.ActionHttpRestRequest;

@Component
public class SoapHttpResolver {
    

    public ActionHttpRestRequest resolve(RequestConfig requestConfig, Map<String, Object> context) throws Exception{

        throw new IllegalAccessError("Resolver for SOAP Request is not yet written");
    }
}
