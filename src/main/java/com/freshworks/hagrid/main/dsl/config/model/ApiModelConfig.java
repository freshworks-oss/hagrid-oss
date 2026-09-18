package com.freshworks.hagrid.main.dsl.config.model;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import groovy.lang.Closure;
import groovy.lang.GroovyObjectSupport;
import groovy.lang.GroovyShell;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@JsonIgnoreProperties(value = { "groovyShell", "objectMapper", "desiredAttrListMap" }, ignoreUnknown = true)
public class ApiModelConfig extends GroovyObjectSupport {
    
    ObjectMapper objectMapper = new ObjectMapper();

    GroovyShell groovyShell = new GroovyShell();
    
    String name = UUID.randomUUID().toString();
    Map<String, Closure> desiredAttrListMap = new HashMap<>();
    ObjectNode data;

    public ApiModelConfig(){

        this.data = objectMapper.createObjectNode();
    }

    public Object propertyMissing(String name) {
        System.out.println("property missing catch " + name);
        return name; 
    }

    @JsonIgnore
    Closure<Boolean> filterClosure = new Closure<Boolean>(null) {public Boolean doCall(Map data){return true;}};

    @JsonIgnore
    Closure<Map> transformClosure = new Closure<Map>(null) {public Map doCall(Map data){return data;}};

    public void name(String modelName){
        this.name = modelName;
    }

    public void attr(LinkedHashMap<String, String> attributeMap){
    
        for(Map.Entry<String, String> attrMap : attributeMap.entrySet()){
            
            String attr = "{ -> " + attrMap.getValue() + " }";
            Closure c = (Closure)groovyShell.evaluate(attr);
            this.desiredAttrListMap.put(attrMap.getKey(), c);
        }
    }

    public void filter(Closure<Boolean> filterClosure){
        this.filterClosure = filterClosure;
    }

    public void transform(Closure<Map> transform){
        this.transformClosure = transform;
    }
}
