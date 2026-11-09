package com.freshworks.hagrid.main.dsl.runnable.context;

import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import groovy.lang.MissingPropertyException;
import groovy.util.Expando;

public class FluentExpando extends Expando {

    Object leafNode = null;
    ObjectMapper objectMapper = new ObjectMapper();

    public FluentExpando() {
        super();
    }

    public FluentExpando(Map<String, Object> sourceMap) {

        for(Map.Entry<String, Object> entry : sourceMap.entrySet()){
            if (entry.getValue() instanceof Map) {
                this.setProperty(entry.getKey(), new FluentExpando((Map<String, Object>) entry.getValue()));
            } else {
                FluentExpando leafFluentExpando = new FluentExpando();
                leafFluentExpando.leafNode = entry.getValue();
                this.setProperty(entry.getKey(), leafFluentExpando);
            }
        }
    }

     // Capture arbitrary nested missing properties to avoid NullPointerExceptions
    @Override
    public Object getProperty(String property) {
        try {
            Object result = super.getProperty(property);
            // If the user attempts to dig deeper into a raw Map, auto-wrap it on the fly
            if (result instanceof Map && !(result instanceof FluentExpando)) {
                return new FluentExpando((Map<String, Object>) result);
            }

            if(result == null){
                return new FluentExpando();
            }
            
            return result;
        } catch (MissingPropertyException e) {
            // Return an empty context holder instead of failing, allowing safe chaining
            return new FluentExpando();
        }
    }

    // 2. Type-conversion property logic
    public String getAsString() { 
        // If this context represents an empty fallback node, return a blank string
        if (this.leafNode == null) {
            return this.toString();
        }
        else{
            return this.leafNode.toString();
        }   
    }

    public Integer getAsInt() { 
        
        if (this.leafNode == null) {
            return null;
        }
        else{
            return Integer.valueOf(this.leafNode.toString());
        }   
    }
    
    public Map getAsMap() throws Exception{ 

        if (this.leafNode == null) {
            return this.getProperties();
        }
        else{
            return objectMapper.readValue(this.leafNode.toString(), Map.class);
        }   
    }
    
}
