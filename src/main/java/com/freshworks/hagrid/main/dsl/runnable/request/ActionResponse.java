package com.freshworks.hagrid.main.dsl.runnable.request;

import java.util.Map;

import org.checkerframework.checker.units.qual.m;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter 
@JsonIgnoreProperties(ignoreUnknown = true)
public class ActionResponse {

    public ACTION_HTTP_CODE httpStatus;
    Object body;
    Map headers;
    Map model;


    public enum ACTION_HTTP_CODE {

        OK, 
        BAD, 

        // This code is useful when you want to show that action is partial successful. 
        // Mostly in the cases of long running sync actions
        PART_OK
    }

    public void setBody(Object body){
        this.body = body;
    }

    public void setHeaders(Map headers){
        this.headers = headers;
    }

    public void setModel(Map model){
        this.model = model;
    }

    public Object getBody(){
        return this.body;
    }

    public Map getHeaders(){
        return this.headers;
    }

    public Map getModel(){
        return this.model;
    }
}
