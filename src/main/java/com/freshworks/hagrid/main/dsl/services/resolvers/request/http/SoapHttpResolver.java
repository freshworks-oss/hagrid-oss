package com.freshworks.uip.worker.hagrid.dsl.services.resolvers.request.http;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.freshworks.uip.worker.hagrid.dsl.config.requests.RequestConfig;
import com.freshworks.uip.worker.hagrid.dsl.runnable.request.http.ActionHttpRestRequest;

@Component
public class SoapHttpResolver {
    

    public ActionHttpRestRequest resolve(RequestConfig requestConfig, Map<String, Object> context) throws Exception{

        throw new IllegalAccessError("Resolver for SOAP Request is not yet written");
    }
}
