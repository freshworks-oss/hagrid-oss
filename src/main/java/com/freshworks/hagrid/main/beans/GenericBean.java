package com.freshworks.hagrid.main.beans;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.freshworks.hagrid.processor.AbstractBean;
import com.freshworks.hagrid.shared.SyncServiceContainer;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Component
@Scope ("prototype")
public class GenericBean extends AbstractBean{
    
    JsonNode data;
    String actionName;
    String subActionName;
    String apiModelName;
    String outputModelName;
    
    SyncServiceContainer syncServiceContainer;
    @Override
    public void transform() {
        
        System.out.println("Bean is bean");
    }
}
