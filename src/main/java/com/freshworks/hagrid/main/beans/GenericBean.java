package com.freshworks.hagrid.main.beans;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.freshworks.hagrid.main.SharedActionServiceMap;
import com.freshworks.hagrid.main.dsl.runnable.ApiModel;
import com.freshworks.hagrid.main.dsl.runnable.context.ActionContext;
import com.freshworks.hagrid.main.dsl.services.ActionService;
import com.freshworks.hagrid.main.dsl.services.ApiModelService;
import com.freshworks.hagrid.processor.AbstractBean;
import com.freshworks.hagrid.shared.SyncServiceContainer;

import ch.qos.logback.core.joran.action.Action;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Component
@Scope ("prototype")
public class GenericBean extends AbstractBean{
    
    boolean isDslBasedBean = true;
    ApiModel apiModel;
    ActionContext actionContext;
    ApiModelService apiModelService;

    SyncServiceContainer syncServiceContainer;


    public void setApiModelService(ApiModelService apiModelService, ActionContext actionContext){
        this.apiModelService = apiModelService;
        this.apiModelService.configure(actionContext);
        this.actionContext = actionContext;
    }

    @Override 
    public Boolean filter(){

        return this.apiModelService.filterBean(apiModel);
    }

    @Override
    public void transform() {
    
        this.apiModel = this.apiModelService.transformBean(apiModel);
    }
}
