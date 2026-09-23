package com.freshworks.hagrid.main.assets;

import org.apache.commons.compress.utils.Lists;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshworks.hagrid.main.dsl.services.ActionService;
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
public class GenericAsset extends AbstractAsset{

    JsonNode output;
    String namespace;
    String actionName;
    String subActionName;
    String outputModelName;
    GenericBean genericBean;

    @JsonIgnore
    ActionService actionService;
    static ObjectMapper objectMapper = new ObjectMapper();



    public void setFromBean(GenericBean genericBean){
        
        this.genericBean = genericBean;
    }

    @Override
    public void transform() {
        
        NamespaceService namespaceService = getSyncServiceContainer().getBean(NamespaceService.class);
        String namespace = namespaceService.getNamespace();
        this.actionName = genericBean.getActionName();
        this.subActionName = genericBean.getSubActionName();
        ActionService actionService = SharedActionServiceMap.get(namespace, actionName, subActionName);
        output = actionService.populateActionOutput(genericBean.getData());
        this.outputModelName = actionService.getOutputModelName();
    }
    
}
