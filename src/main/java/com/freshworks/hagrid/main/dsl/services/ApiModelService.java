package com.freshworks.hagrid.main.dsl.services;

import java.util.HashMap;
import java.util.Map;

import org.checkerframework.checker.units.qual.m;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.freshworks.hagrid.main.dsl.config.model.ApiModelConfig;
import com.freshworks.hagrid.main.dsl.config.model.ModelSpec;
import com.freshworks.hagrid.main.dsl.runnable.ApiModel;
import com.freshworks.hagrid.main.dsl.runnable.context.ActionContext;
import com.freshworks.hagrid.main.dsl.runnable.request.ActionResponse;

import groovy.lang.Closure;

@Component
@Scope("prototype")
public class ApiModelService {
    
    ActionContext actionContext;
    ModelSpec modelSpec;
    ObjectMapper objectMapper = new ObjectMapper();

    @Autowired 
    public ApiModelService(ModelSpec modelSpec){
        this.modelSpec = modelSpec;
    }


    public void configure(ActionContext actionContext){
        this.actionContext = actionContext;
    }

    public ApiModel getApiModelForBean(ObjectNode beanData){

        ApiModelConfig apiModelConfig = null;
        String apiModelConfigName = (String)this.actionContext.getActionSharedMap().get("_api_config_model_name");
        apiModelConfig = this.modelSpec.getApiModelByName(apiModelConfigName);
    

        // Now resolve model attributes
        Map<String, Closure> desiredFieldListConfig = apiModelConfig.getDesiredAttrListMap();

        // Creating a context for each
        Map resolvedApiModelAttributeMap = new HashMap<>();

        // Create a context 
        Map eachModelMap = objectMapper.convertValue(beanData, Map.class);
        ActionResponse actionResponse = this.actionContext.getActionResponse();
        actionResponse.setModel(eachModelMap);                    

        for(Map.Entry<String, Closure> fieldConfig: desiredFieldListConfig.entrySet()){    

            Closure c = fieldConfig.getValue();

            // resolve field config closure on api response
            c.setDelegate(this.actionContext);
            c.setResolveStrategy(Closure.DELEGATE_ONLY);
            resolvedApiModelAttributeMap.put(fieldConfig.getKey(), c.call());
        }

        ApiModel apiModel = new ApiModel();
        apiModel.setName(apiModelConfig.getName());
        apiModel.setFilterClosure(apiModelConfig.getFilterClosure());
        apiModel.setTransformClosure(apiModelConfig.getTransformClosure());

        ObjectNode objectNode = objectMapper.convertValue(resolvedApiModelAttributeMap, ObjectNode.class);

        apiModel.setData(objectNode);

        return apiModel;
    }


    public ApiModel transformBean(ApiModel apiModel){

        apiModel.transformModel();
        return apiModel;
    }


    public boolean filterBean(ApiModel apiModel){

        return apiModel.filterModel();   
    }

}
