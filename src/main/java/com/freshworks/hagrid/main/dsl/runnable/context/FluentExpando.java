package com.freshworks.hagrid.main.dsl.runnable.context;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import groovy.lang.MissingPropertyException;
import groovy.util.Expando;
import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter 
@JsonSerialize (using = FluentExpandoSerializer.class)
@JsonDeserialize (using = FluentExpandoDeserializer.class)
public class FluentExpando extends Expando {

    Object actualValue = null; 
    boolean isLeafNode = false;
    ObjectMapper objectMapper = new ObjectMapper();

    public FluentExpando() {
        super();
    }


    public FluentExpando(Object value) {

        super();
        
        // If the value is neither a Map nor an Iterable, create a new FluentExpando for the leaf node
            
        if( value instanceof String){
            this.isLeafNode = true;
            this.actualValue = value.toString();
            this.setProperty("leaf", value.toString());
        }
        if (value instanceof Integer){
            this.isLeafNode = true;
            this.actualValue = Integer.valueOf(value.toString());
            this.setProperty("leaf", Integer.valueOf(value.toString()));
        }

        if ( value == null){
            this.isLeafNode = true;
            this.actualValue = null;
            this.setProperty("leaf", null);
        }
    }



     // Capture arbitrary nested missing properties to avoid NullPointerExceptions
    @Override
    public Object getProperty(String property) {
        try {
            Object result = super.getProperty(property);
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
    public String asString() { 
        // If this context represents an empty fallback node, return a blank string
        if (this.isLeafNode == false) {
            return this.toString();
        }
        else{
            return this.actualValue.toString();
        }   
    }

    public Integer asInt() { 
        
        if (this.isLeafNode == false) {
            // It means that as int called on a non-leaf node, so return null
            return null;
        }
        else{

            // If the leaf node is not null, attempt to convert it to an Integer
            return Integer.valueOf(this.actualValue.toString());
        }   
    }
    
    public Map asMap() throws Exception{ 

        if (this.isLeafNode == false) {
            // It means that as map called on a non-leaf node, so return the properties of this expando
            return this.getProperties();
        }
        else{

            // It means that as map called on a leaf node, so return the leaf node as map
            return objectMapper.readValue(this.actualValue.toString(), Map.class);
        }   
    }
    
}
