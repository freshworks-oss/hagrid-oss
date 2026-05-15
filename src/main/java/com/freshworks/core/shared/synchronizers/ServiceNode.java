package com.freshworks.core.shared.synchronizers;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.util.HashMap;
import java.util.Set;

@Data
public class ServiceNode {

    String selfId;
//    ServiceNode parentNode;
    Boolean shouldTerminate;
    JsonNode data;
    HashMap<String, ServiceNode> children;

    protected ServiceNode(String selfId){
        this.selfId = selfId;
        shouldTerminate = false;
        children = new HashMap<>();
    }

    protected void setShouldTerminate(boolean shouldTerminate){
        this.shouldTerminate = shouldTerminate;
    }

    protected ServiceNode addChild(String uniqueId){

        if(this.children.containsKey(uniqueId)){
            throw new IllegalStateException("Can not add child as child with uniqueId " + uniqueId + " already exists");
        }
        ServiceNode childServiceNode = new ServiceNode(uniqueId);
        this.children.put(uniqueId, childServiceNode);
//        childServiceNode.parentNode = this;
        return childServiceNode;
    }

    protected Set<String> getChildrenKeySet(){

        return children.keySet();
    }
}
