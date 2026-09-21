package com.freshworks.hagrid.main.dsl.services.resolvers.request.http;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshworks.hagrid.main.dsl.config.requests.RequestConfig;
import com.freshworks.hagrid.main.dsl.config.requests.RequestConfig.REQUEST_TYPE;
import com.freshworks.hagrid.main.dsl.runnable.request.http.ActionHttpRestRequest;

@Component 
public class RestHttpResolver{

    ObjectMapper objectMapper = new ObjectMapper();

    public ActionHttpRestRequest resolve(RequestConfig requestConfig, Map<String, Object> context) throws Exception{
        
        ActionHttpRestRequest httpRequest = new ActionHttpRestRequest();

        // resolve host key here
        String host = requestConfig.getHost();
        httpRequest.setHost(resolveStr(host, context));


        // resolve path key here
        String path = requestConfig.getPath();
        httpRequest.setPath(resolveStr(path, context));



        // resolve Method key here
        String method = requestConfig.getMethod();
        httpRequest.setMethod(resolveStr(method, context));


        // resolve type key here
        String type = requestConfig.getType().toString();
        httpRequest.setType(REQUEST_TYPE.valueOf(resolveStr(type, context)));
        


        // resolve query here
        Object unresolvedQueryObject = requestConfig.getQuery();
        Map<String, String> unresolvedQuery = new HashMap<>();

        if(unresolvedQueryObject instanceof Map){

            unresolvedQuery = (Map<String, String>) unresolvedQueryObject;
        }

        else if (unresolvedQueryObject instanceof String){

            try {

                unresolvedQuery = objectMapper.convertValue(unresolvedQueryObject, Map.class);
            }
            catch (Exception e){
                throw new IllegalArgumentException("Error while parsing query part of the rest client. It seems like query object provide is in the String format. If so then it must be valid JSON string");
            }
        }

        Map<String, String> resolvedQuery = new HashMap<>();

        for(Map.Entry<String, String> param : unresolvedQuery.entrySet()){

            String unresolvedKey = param.getKey();
            String unresolvedValue = param.getValue();
            
            String resolvedKey = resolveStr(unresolvedKey, context);
            String resolvedValue = resolveStr(unresolvedValue, context);
            resolvedQuery.put(resolvedKey, resolvedValue);
        }

        httpRequest.setQuery(resolvedQuery);
        

        // resolve headers here
        Object unresolvedHeadersObject = requestConfig.getHeaders();
        Map<String, String> unresolvedHeaders = new HashMap<>();

        if(unresolvedHeadersObject instanceof Map){
            unresolvedHeaders = (Map<String, String>)unresolvedHeadersObject;
        }
        else if(unresolvedHeadersObject instanceof String){

            try {

                unresolvedHeaders = objectMapper.convertValue(unresolvedHeadersObject, Map.class);
            }
            catch (Exception e){
                throw new IllegalArgumentException("Error while parsing header part of the rest client. It seems like query object provide is in the String format. If so then it must be valid JSON string");
            }
        }


        Map<String, String> resolvedHeaders = new HashMap<>();

        for(Map.Entry<String, String> header : unresolvedHeaders.entrySet()){

            String unresolvedHeaderKey = header.getKey();
            String unresolvedHeaderValue = header.getValue();
            
            String resolvedKey = resolveStr(unresolvedHeaderKey, context);
            String resolvedValue = resolveStr(unresolvedHeaderValue, context);
            resolvedHeaders.put(resolvedKey, resolvedValue);
        }

        httpRequest.setHeaders(resolvedHeaders);

        // resolve body here
        // Here body could be nested, hence instead of going through key value pair, I am converting it to string and doing replace

        Object unresolvedBodyObject = requestConfig.getBody();
        String unresolvedBodyStr = "";

        if(unresolvedBodyObject instanceof Map){
            unresolvedBodyStr = objectMapper.writeValueAsString(unresolvedBodyObject);
        }
        else if(unresolvedBodyObject instanceof String){

            try {

                unresolvedBodyStr = String.valueOf(unresolvedBodyObject);
            }
            catch (Exception e){
                throw new IllegalArgumentException("Error while parsing body part of the rest client. It seems like query object provide is in the String format. If so then it must be valid JSON string");
            }
        }


        String resolvedBodyStr = resolveStr(unresolvedBodyStr, context);
        JsonNode resolvedBody = objectMapper.convertValue(resolvedBodyStr, JsonNode.class);
        httpRequest.setBody(resolvedBody);

        httpRequest.setExecuteClosure(requestConfig.getExecuteClosure());
        
        return httpRequest;
    }


    public String resolveStr(String str, Map<String, Object> context){

        for(Map.Entry<String, Object> map : context.entrySet()){

            if(str.contains( "{{" + map.getKey() + "}}")){

               str = str.replace("{{" + map.getKey() + "}}", String.valueOf(map.getValue()));
            }
        }

        return str;
    }
}
