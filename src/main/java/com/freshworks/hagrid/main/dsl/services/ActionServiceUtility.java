package com.freshworks.hagrid.main.dsl.services;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshworks.hagrid.main.dsl.runnable.ApiModel;

@Component
public class ActionServiceUtility {

    public List<ApiModel> parseResponseToModel(JsonNode jsonNode){

        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.convertValue(jsonNode, new TypeReference<List<ApiModel>>() {});
    }
       
}
