package com.freshworks.hagrid.main.dsl.runnable.context;

import java.util.ArrayList;
import java.util.List;
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


    public FluentExpando(Object value) {

        super();
        
        if (value instanceof Map){
            
            for(Map.Entry<String, Object> entry : ((Map<String, Object>) value).entrySet()){

                if (entry.getValue() instanceof Map) {
                    // Recursively create a new FluentExpando for the nested map
                    this.setProperty(entry.getKey(), new FluentExpando(entry.getValue()));
                } 
                
                else if (entry.getValue() instanceof Iterable) {
                    
                    List<FluentExpando> list = new ArrayList<>();
                    for (Object item : (List<Object>) entry.getValue()) {
                        if (item instanceof Map) {
                            list.add(new FluentExpando(item));
                        } else {
                        list.add(new FluentExpando(item));
                    }
                }

                    // Add the list of FluentExpando objects to the current FluentExpando instance
                    this.setProperty(entry.getKey(), list);
                }

                else {

                    // If the value is neither a Map nor an Iterable, create a new FluentExpando for the leaf node
                    this.setProperty(entry.getKey(), new FluentExpando(entry.getValue()));
                }
            }
        }

        else{

            // If the value is neither a Map nor an Iterable, create a new FluentExpando for the leaf node
            this.leafNode = value;
            this.setProperty("leaf", this.leafNode);
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
            // It means that as int called on a non-leaf node, so return null
            return null;
        }
        else{

            // If the leaf node is not null, attempt to convert it to an Integer
            return Integer.valueOf(this.leafNode.toString());
        }   
    }
    
    public Map getAsMap() throws Exception{ 

        if (this.leafNode == null) {
            // It means that as map called on a non-leaf node, so return the properties of this expando
            return this.getProperties();
        }
        else{

            // It means that as map called on a leaf node, so return the leaf node as map
            return objectMapper.readValue(this.leafNode.toString(), Map.class);
        }   
    }
    
}
