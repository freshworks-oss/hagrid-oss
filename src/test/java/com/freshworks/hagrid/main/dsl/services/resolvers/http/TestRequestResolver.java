package com.freshworks.hagrid.main.dsl.services.resolvers.http;

import static org.hamcrest.MatcherAssert.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import groovy.lang.Closure;
import groovy.lang.GroovyShell;
import com.freshworks.hagrid.main.dsl.config.requests.RequestConfig;
import com.freshworks.hagrid.main.dsl.config.requests.RequestConfig.REQUEST_TYPE;
import com.freshworks.hagrid.main.dsl.runnable.request.http.ActionHttpRestRequest;
import com.freshworks.hagrid.main.dsl.services.resolvers.request.http.RestHttpResolver;

import groovy.lang.Closure;

@SpringBootTest
public class TestRequestResolver {
    
    @Autowired
    ApplicationContext applicationContext;

    GroovyShell groovyShell = new GroovyShell();
    
    @Test 
    public void testRequestResolutionForHardcodedString() throws Exception{
        
        
        RestHttpResolver restHttpResolver = new RestHttpResolver();
        
        String configHost = "http://localhost:8080";
        String configPath = "/api/v1/users";
        String configMethod = "{{method}}";
        Map <String, String> configQuery = new HashMap<>();
        String configType = "HTTP";
        
        RequestConfig requestConfig = new RequestConfig();
        requestConfig.setHost(configHost);
        requestConfig.setPath(configPath);
        requestConfig.setMethod(configMethod);
        requestConfig.setQuery(configQuery);
        requestConfig.setType(REQUEST_TYPE.valueOf(configType));
    
        // Here I am creating requestMapping which will resolve closure to get the actual value 

        String unresolvedMethod = "'GET'";
        String closureString = "{ -> " + unresolvedMethod + " }";
        Closure c = (Closure)groovyShell.evaluate(closureString);

        Map<String, Object> resolvedMapping = new HashMap<>();
        resolvedMapping.put("method", c.call());

        ActionHttpRestRequest httpRequest = restHttpResolver.resolve(requestConfig, resolvedMapping);
        assertThat (httpRequest.getMethod(), Matchers.is(resolvedMapping.get("method")));  
    }

    @Test 
    // After request is resolved then even integer converted to string value 
    public void testRequestResolutionForNumber() throws Exception{
        
        
        RestHttpResolver restHttpResolver = new RestHttpResolver();
        
        String configHost = "http://localhost:8080";
        String configPath = "/api/v1/users";
        String configMethod = "{{method}}";
        Map <String, String> configQuery = new HashMap<>();
        String configType = "HTTP";
        
        RequestConfig requestConfig = new RequestConfig();
        requestConfig.setHost(configHost);
        requestConfig.setPath(configPath);
        requestConfig.setMethod(configMethod);
        requestConfig.setQuery(configQuery);
        requestConfig.setType(REQUEST_TYPE.valueOf(configType));
    
        // Here I am creating requestMapping which will resolve closure to get the actual value 

        String unresolvedMethod = "5";
        String closureString = "{ -> " + unresolvedMethod + " }";
        Closure c = (Closure)groovyShell.evaluate(closureString);

        Map<String, Object> resolvedMapping = new HashMap<>();
        resolvedMapping.put("method", c.call());

        ActionHttpRestRequest httpRequest = restHttpResolver.resolve(requestConfig, resolvedMapping);
        assertThat (httpRequest.getMethod(), Matchers.is(resolvedMapping.get("method").toString()));  
    }

    @Test 
    
    public void testRequestResolutionForJsonString() throws Exception{
        
        ObjectMapper obj = new ObjectMapper();

        RestHttpResolver restHttpResolver = new RestHttpResolver();
        
        String configHost = "http://localhost:8080";
        String configPath = "/api/v1/users";
        String configMethod = "{{method}}";
        Map <String, String> configQuery = new HashMap<>();
        String configType = "HTTP";
        
        RequestConfig requestConfig = new RequestConfig();
        requestConfig.setHost(configHost);
        requestConfig.setPath(configPath);
        requestConfig.setMethod(configMethod);
        requestConfig.setQuery(configQuery);
        requestConfig.setType(REQUEST_TYPE.valueOf(configType));
    
        // Here I am creating requestMapping which will resolve closure to get the actual value 

        String unresolvedMethod = "'{\"method\": \"GET\"}'";
        String closureString = "{ -> " + unresolvedMethod + " }";
        Closure c = (Closure)groovyShell.evaluate(closureString);

        Map<String, Object> resolvedMapping = new HashMap<>();
        resolvedMapping.put("method", c.call());

        ActionHttpRestRequest httpRequest = restHttpResolver.resolve(requestConfig, resolvedMapping);

        if (httpRequest.getMethod() instanceof String){
            // System.out.println(httpRequest.getMethod());
            Map map = obj.readValue(httpRequest.getMethod(), Map.class);
            String method = (String)map.get("method");
            System.out.println(method);

        }
        else{
            System.out.println("Not a string");
        }
        
        assertThat (httpRequest.getMethod(), Matchers.is(resolvedMapping.get("method")));  
    }
}   

