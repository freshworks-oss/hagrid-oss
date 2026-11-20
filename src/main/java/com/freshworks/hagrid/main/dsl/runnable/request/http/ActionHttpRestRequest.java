package com.freshworks.hagrid.main.dsl.runnable.request.http;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.util.Strings;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.freshworks.hagrid.main.dsl.config.requests.RequestConfig.REQUEST_SUB_TYPE;
import com.freshworks.hagrid.main.dsl.config.requests.RequestConfig.REQUEST_TYPE;
import com.freshworks.hagrid.main.dsl.runnable.request.ActionResponse;
import com.freshworks.hagrid.main.dsl.runnable.request.ActionResponse.ACTION_HTTP_CODE;

import groovy.lang.Closure;
import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter 
@JsonIgnoreProperties(ignoreUnknown = true)
public class ActionHttpRestRequest extends ActionHttpRequest{
    
    String name;
    REQUEST_TYPE type = REQUEST_TYPE.HTTP;
    String method;
    String host;
    String path;
    String contentType;
    
    Map<String, String> query;
    Map<String, String> headers;
    JsonNode body;
    Closure<ActionResponse> executeClosure;
    Map<String, String> customProperties = new HashMap<>();

    static ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public REQUEST_TYPE getRequestType() {
        
        return REQUEST_TYPE.HTTP;
    }

    @Override
    public REQUEST_SUB_TYPE getRequesSubType() {
        
        return REQUEST_SUB_TYPE.REST;
    }

    public ActionResponse executeRequest() throws Exception{

        if(this.getExecuteClosure() != null){
            return this.executeClosure.call();
        }

         /**
         *  Else create a request here and execute it 
         */
        RestTemplate restTemplate = new RestTemplate();
        
        // Set up headers container
        HttpHeaders headers = new HttpHeaders();
        for(Map.Entry<String, String> entry: this.headers.entrySet()){
            headers.add(entry.getKey(), entry.getValue());
        }

        // Now get body 
        JsonNode body = this.body;

        // Get type
        HttpMethod httpMethod = HttpMethod.valueOf(method.toString());

        // Wrap headers into an entity object
        HttpEntity entity;
        if(Boolean.FALSE.equals(httpMethod.equals(HttpMethod.GET)) && Strings.isNotBlank(objectMapper.writeValueAsString(body))){
            entity = new HttpEntity<>(body, headers);
        }
        else{
            entity = new HttpEntity<>(headers);
        }
         
        this.host = this.host.charAt(this.host.length() - 1) == '/' ? this.host : this.host + "/";
        this.path = this.path.charAt(0) == '/' ? this.path.substring(1) : this.path;

        String queryUrl = "";

        if (this.query != null && !this.query.isEmpty()) {
            queryUrl += "?" + this.query.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));
        }

        String url = this.host + this.path + queryUrl;
        ActionResponse actionResponse = new ActionResponse();

        try{
            ResponseEntity<JsonNode> responseEntity = restTemplate.exchange(url, httpMethod, entity, JsonNode.class);

            actionResponse.setHttpStatus(responseEntity.getStatusCode().is2xxSuccessful() ? ACTION_HTTP_CODE.OK : ACTION_HTTP_CODE.BAD);
            actionResponse.setStatusCode(responseEntity.getStatusCode().value());
            actionResponse.setBody(responseEntity.getBody());
            actionResponse.setHeaders(responseEntity.getHeaders());
        }
        catch(HttpStatusCodeException e){

            // RestTemplate throws for 4xx/5xx. Hand them over as a normal invalid response so the DSL can handle them.
            actionResponse.setHttpStatus(ACTION_HTTP_CODE.BAD);
            actionResponse.setStatusCode(e.getStatusCode().value());
            actionResponse.setBody(errorBody(e.getResponseBodyAsString()));
            actionResponse.setHeaders(e.getResponseHeaders());
        }

        return actionResponse;
        
    }

    private static JsonNode errorBody(String text){

        if(Strings.isBlank(text)){
            return objectMapper.nullNode();
        }
        try{
            return objectMapper.readTree(text);
        }
        catch(Exception e){
            return objectMapper.getNodeFactory().textNode(text);
        }
    }
}
