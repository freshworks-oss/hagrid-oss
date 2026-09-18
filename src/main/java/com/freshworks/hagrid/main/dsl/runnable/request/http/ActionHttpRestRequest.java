package com.freshworks.uip.worker.hagrid.dsl.runnable.request.http;

import java.util.Map;

import org.apache.logging.log4j.util.Strings;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.freshworks.uip.worker.hagrid.dsl.config.requests.RequestConfig.REQUEST_SUB_TYPE;
import com.freshworks.uip.worker.hagrid.dsl.config.requests.RequestConfig.REQUEST_TYPE;
import com.freshworks.uip.worker.hagrid.dsl.runnable.request.ActionResponse;
import com.freshworks.uip.worker.hagrid.dsl.runnable.request.ActionResponse.ACTION_HTTP_CODE;

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
         

        String url = this.host + this.path;
        ResponseEntity<JsonNode> responseEntity = restTemplate.exchange(url, httpMethod, entity, JsonNode.class);

        ActionResponse actionResponse = new ActionResponse();

        if(responseEntity.getStatusCode().is2xxSuccessful()){
            actionResponse.setHttpStatus(ACTION_HTTP_CODE.OK);
        }
        else{
            actionResponse.setHttpStatus(ACTION_HTTP_CODE.BAD);
        }

        actionResponse.setBody(responseEntity.getBody());
        actionResponse.setHeaders(responseEntity.getHeaders());

        return actionResponse;
        
    }
}
