package com.freshworks.core.processor;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.constants.Constants;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;


@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
//@JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
//        include = JsonTypeInfo.As.EXISTING_PROPERTY, property = Constants.JsonTypeInfo_As_PROPERTY, visible = true)

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = Constants.JsonTypeInfo_As_PROPERTY, visible = true)

public abstract class AbstractBean {

    public AbstractBean parentBean;

    public String clazz;

    @JsonIgnore
    SyncServiceContainer syncServiceContainer;

    public void configure(SyncServiceContainer syncServiceContainer){
        this.syncServiceContainer = syncServiceContainer;
    }

    public Boolean filter(){
        return true;
    }
    public abstract void transform();
    public void setParentBean(JsonNode parentJSONNode) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();

        if(parentJSONNode.has("clazz")){
            this.parentBean = objectMapper.readValue(parentJSONNode.toString(), AbstractBean.class);
        }
        else{
//            throw new JsonProcessingException("parent json node does not contains clazz field");
        }
    }
    public AbstractBean getParentBean(){
        return this.parentBean;
    }
    public Boolean hasParentBean(){
        if(getParentBean() == null){
            return false;
        }
        else{
            return true;
        }
    }

    @Override
    public boolean equals(Object abstractBean) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String s  = null;

            s = objectMapper.writeValueAsString(this);
            JsonNode jsonNode = objectMapper.readTree(s);
            ObjectNode o = (ObjectNode) jsonNode;
            o.remove("parentBean");

            s = objectMapper.writeValueAsString(abstractBean);
            JsonNode jsonNode1 = objectMapper.readTree(s);
            ObjectNode o1 = (ObjectNode) jsonNode1;
            o1.remove("parentBean");

            return jsonNode.equals(jsonNode1);

        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int hashCode(){
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String s  = null;
            s = objectMapper.writeValueAsString(this);
            JsonNode jsonNode = objectMapper.readTree(s);
            return jsonNode.hashCode();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public List<AbstractBean> map() {

        List<AbstractBean> abstractBeanList = new ArrayList<>();
        abstractBeanList.add(this);
        return abstractBeanList;
    }
}
