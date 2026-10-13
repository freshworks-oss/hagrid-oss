package com.freshworks.hagrid.main.dsl.config.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.logging.log4j.util.Strings;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.freshworks.hagrid.main.dsl.runnable.ApiModel;

import groovy.lang.Binding;
import groovy.lang.Closure;
import groovy.lang.GroovyObjectSupport;
import groovy.lang.GroovyShell;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class OutputModelConfig extends GroovyObjectSupport{
    
    static ObjectMapper objectMapper = new ObjectMapper();
    String name = UUID.randomUUID().toString();
    ObjectNode data;    
    LinkedHashMap<String, ModelBinding> outputFieldMapping = new LinkedHashMap<>();

    public OutputModelConfig(){
        this.data = objectMapper.createObjectNode();
    }


    @JsonIgnore
    Closure<Boolean> filterClosure = new Closure<Boolean>(null) {
        
        public Boolean doCall(JsonNode data){

            return true;
        }
    };

    @JsonIgnore
    Closure<Void> transformClosure = new Closure<Void>(null) {
        
        public void doCall(JsonNode data){
            
            
        }
    };

    public void name(String modelName){
        this.name = modelName;
    }

    public void attr(LinkedHashMap<String, ModelBinding> attributeList){
        
        this.outputFieldMapping.putAll(attributeList);
    }


    public void filter(Closure<Boolean> filterClosure){
        this.filterClosure = filterClosure;
    }

    public void transform(Closure<Void> transform){
        this.transformClosure = transform;
    }

    public ModelBinding model(String modelName, String modelField){

        ModelBinding binding = new ModelBinding();
        binding.setModelName(modelName);
        binding.setModelField(modelField);
        return binding;
    }

     // This catches unquoted words used as properties/variables
    public Object propertyMissing(String name) {
        System.out.println("property missing catch " + name);
        return name; 
    }


    @Getter 
    @Setter 
    public class ModelBinding{

        String modelName;
        String modelField;
    }
}
