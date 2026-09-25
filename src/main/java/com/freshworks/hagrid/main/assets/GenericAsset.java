package com.freshworks.hagrid.main.assets;

import org.apache.commons.compress.utils.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshworks.hagrid.main.dsl.runnable.OutputModel;
import com.freshworks.hagrid.main.dsl.runnable.context.ActionContext;
import com.freshworks.hagrid.main.dsl.services.ActionService;
import com.freshworks.hagrid.main.dsl.services.ApiModelService;
import com.freshworks.hagrid.main.dsl.services.OutputModelService;
import com.freshworks.hagrid.processor.AbstractAsset;
import com.freshworks.hagrid.shared.NamespaceService;
import com.freshworks.hagrid.main.SharedActionServiceMap;
import com.freshworks.hagrid.main.beans.GenericBean;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Component
@Scope ("prototype")
@JsonIgnoreProperties (ignoreUnknown = true, value = {"outputModelService", "syncServiceContainer"})
public class GenericAsset extends AbstractAsset{

    boolean isDslAsset = true;
    OutputModel outputModel;
    GenericBean genericBean;
    ActionContext actionContext;
    OutputModelService outputModelService;
    static ObjectMapper objectMapper = new ObjectMapper();

    public void setOutputModelService(OutputModelService outputModelService, ActionContext actionContext){
        this.outputModelService = outputModelService;
        this.outputModelService.configure(actionContext);
        this.actionContext = actionContext;
    }

    public void setFromBean(GenericBean genericBean){
        
        this.genericBean = genericBean;
    }

    @Override 
    public boolean filter(){

        return this.outputModelService.filterOutputModel(outputModel);
    }

    @Override
    public void transform() {
        
        this.outputModel = this.outputModelService.transformOutputModel(outputModel);
    }
    
}
