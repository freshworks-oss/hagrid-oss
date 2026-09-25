package com.freshworks.hagrid.main.dsl.runnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.freshworks.hagrid.main.dsl.config.model.ApiModelConfig;

import groovy.lang.Closure;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApiModel {

    String name = UUID.randomUUID().toString();
    ObjectNode data;
    
    @JsonIgnore 
    ObjectMapper objectMapper = new ObjectMapper();

    @JsonIgnore
    Closure<Boolean> filterClosure = new Closure<Boolean>(null) {public Boolean doCall(Map data){return true;}};

    @JsonIgnore
    Closure<Map> transformClosure = new Closure<Map>(null) {public Map doCall(Map data){return data;}};

    public void transformModel(){
        Map dataMapForm = getModelDataAsMap();
        Map transformedDataMapForm = this.transformClosure.call(dataMapForm);
        this.data = getModelDataAsObjectNode(transformedDataMapForm);
    }

    public boolean filterModel(){

        Map dataMapForm = getModelDataAsMap();
        return this.filterClosure.call(dataMapForm);
    }

    public Map getModelDataAsMap(){

        return objectMapper.convertValue(data, Map.class);
    }

    public ObjectNode getModelDataAsObjectNode(Map modelData){

        return objectMapper.convertValue(modelData, ObjectNode.class);
    }
}
