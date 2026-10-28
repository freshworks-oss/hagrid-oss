package com.freshworks.hagrid.main.dsl.config.requests;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshworks.hagrid.main.dsl.runnable.request.ActionResponse;

import groovy.lang.Closure;
import groovy.lang.GroovyObject;
import groovy.lang.MetaClass;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RequestConfig {

    @JsonIgnore
    ObjectMapper objectMapper = new ObjectMapper();

    public enum REQUEST_TYPE{

        HTTP, 
        NON_HTTP
    }

    public enum REQUEST_SUB_TYPE{

        REST, 
        SOAP,
        NON_HTTP
    }

    String name;
    REQUEST_TYPE type = REQUEST_TYPE.HTTP;
    REQUEST_SUB_TYPE subType = REQUEST_SUB_TYPE.REST;
    String method;
    String host;
    String path;
    String contentType;
    
    Object query;
    Object headers;
    Object body;
    Map<String, String> missingProperties = new HashMap<>();
    
    Closure<ActionResponse> executeClosure;

    public void name(String name){
        this.name = name;
    }

    public void type(String type){
        this.type = REQUEST_TYPE.valueOf(type);
    }

    public void sub_type(String type){
        this.subType = REQUEST_SUB_TYPE.valueOf(type);
    }

    public void method(String method){
        this.method = method;
    }

    public void host(String host){
        this.host = host;
    }

    public void content_type(String content_type){
        this.contentType = content_type;
    }

    public void path(String path){
        this.path = path;
    }

    public void query(String query){

        this.query = query;
    }

    public void query(Map<String, String> query){
        this.query = query;
    }

    public void headers(Map<String, String> headers){
        this.headers = headers;
    }

    public void headers(String headers){
        this.headers = headers;
    }

    public void body(Map<String, String> body){
        this.body = body;
    }

    public void body(String body){
        this.body = body;
    }

    public void propertyMissing(String name, Object value) throws Exception{
        missingProperties.put(name, objectMapper.writeValueAsString(value));
    }

    public Object propertyMissing(String name) {
        return missingProperties.get(name);
    }

    public Object methodMissing(String name, Object args) throws Exception{

        Object[] argsArray = (Object[]) args;

        missingProperties.put(name, objectMapper.writeValueAsString(argsArray[0]));

        return null;
    }

    public void execute(Closure<ActionResponse> closure){

        this.executeClosure = closure;
    }
}
