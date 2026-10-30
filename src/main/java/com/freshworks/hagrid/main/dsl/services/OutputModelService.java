package com.freshworks.hagrid.main.dsl.services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshworks.hagrid.main.dsl.config.model.ModelSpec;
import com.freshworks.hagrid.main.dsl.runnable.ApiModel;
import com.freshworks.hagrid.main.dsl.runnable.OutputModel;
import com.freshworks.hagrid.main.dsl.runnable.OutputModel.Join;
import com.freshworks.hagrid.main.dsl.runnable.context.ActionContext;
import com.freshworks.hagrid.main.dsl.runnable.request.ActionResponse.ACTION_HTTP_CODE;

@Component
@Scope("prototype")
public class OutputModelService {
    
    ActionContext actionContext;
    ModelSpec modelSpec;
    ObjectMapper objectMapper = new ObjectMapper();

    @Autowired 
    public OutputModelService(ModelSpec modelSpec){
        this.modelSpec = modelSpec;
    }


    public void configure(ActionContext actionContext){
        this.actionContext = actionContext;
    }

    public OutputModel getOutputModelFromApiModel(JsonNode beanNode){

        ApiModel apiModel = parseResponseToModel(beanNode);
        OutputModel outputModel = new OutputModel();
        outputModel.setName(apiModel.getName());    
        String statusCode = (String)this.actionContext.getActionSharedMap().get("_response_status_code");
        outputModel.setHttpStatus(ACTION_HTTP_CODE.valueOf(statusCode));

        if(Boolean.TRUE.equals(outputModel.primitive())){
            outputModel.populate(apiModel);
        }

        else{

            // it is partial 
            // It means that it is partial 
            Join join = outputModel.getJoin();   

            if(join.getLeftModelKey().equalsIgnoreCase(apiModel.getName())){

                //  Check if right filled exists already ? 
                // if so then populate it with left model 
            }

            else{

                // it is right model
                // check if left filled already exists ? 
                // if so then populate it with right filled 
                }
            }

        return outputModel;
    }

    
    public ApiModel parseResponseToModel(JsonNode jsonNode){

        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.convertValue(jsonNode, new TypeReference<ApiModel>() {});
    }

    public OutputModel transformOutputModel(OutputModel outputModel){

        outputModel.transformModel();
        return outputModel;
    }


    public boolean filterOutputModel(OutputModel outputModel){

        return outputModel.filterModel();   
    }
}
