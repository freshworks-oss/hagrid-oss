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
import com.freshworks.hagrid.main.beans.GenericBean;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Component
@Scope ("prototype")
public class GenericAsset extends AbstractAsset{

    JsonNode output;

    GenericBean genericBean;

    @JsonIgnore
    ActionService actionService;

    static ObjectMapper objectMapper = new ObjectMapper();
    public void setFromBean(GenericBean genericBean){
        
        this.genericBean = genericBean;
    }

    @Override
    public void transform() {
        
        actionService = getSyncServiceContainer().getBean(ActionService.class);
        output = actionService.populateActionOutput(genericBean.getData());
    }
    
}
