package com.freshworks.hagrid.shared.sync;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.freshworks.hagrid.main.steps.GenericNonHttpStep;
import com.freshworks.hagrid.traverser.AbstractStep;
import com.freshworks.hagrid.traverser.DagNode;
import com.freshworks.hagrid.traverser.Annotations.FreshHierarchy;
import com.freshworks.hagrid.traverser.DagNode.NodeRateLimitObject;

import lombok.Getter;
import lombok.Setter;
import lombok.AccessLevel;


@Getter
@Setter
@Component
@Scope("prototype")
public class ConnectorConfiguration {

    int traverserThreadCount = 1;

    List<List<String>> enabledDagPath = new ArrayList<>();

    int processorPollCount = 1000;
    int numberOfParallelProcessor = 20;

    @Value("${spring.connector.infra.type:file}")
    String infraDbType;

    @Value("${spring.connector.infra.nitrite.location:./database}")
    String infraDbLocation;

    String analyticsShouldPassTagsToMeterRegistry;

    HashMap<String, NodeRateLimitObject> nodeRateLimitHashMap = new HashMap<>();

    public ConnectorConfiguration(){
        this.infraDbType = "file";
        this.infraDbLocation = "./database";   
    }

    public void setStepRateLimit(String stepName, NodeRateLimitObject nodeRateLimitObject){

        nodeRateLimitHashMap.put(stepName, nodeRateLimitObject);
    }

    public void addPathToEnable(List<String> enabledPath){

        this.enabledDagPath.add(enabledPath);
    }

    public List<List<String>> getEnabledDagPathList(){

        return this.enabledDagPath;
    }    

    public NodeRateLimitObject getNodeRateLimitObject(String name){

        return nodeRateLimitHashMap.get(name);
    }
}
