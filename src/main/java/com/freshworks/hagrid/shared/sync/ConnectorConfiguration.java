package com.freshworks.hagrid.shared.sync;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import com.freshworks.hagrid.traverser.DagNode.NodeRateLimitObject;

import lombok.Getter;
import lombok.Setter;


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

    MongoDbConfiguration mongoDbConfiguration;

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


    @Getter 
    @Setter 
    public static class MongoDbConfiguration{

        String mongoConnectionString;
        @Value("${spring.connector.infra.mongo.database.user:admin}")
        String databaseUserName = "admin";
        @Value("${spring.connector.infra.mongo.database.password:password12345}")
        String databasePassword = "password12345";
        @Value("${spring.connector.infra.mongo.database.authDb:admin}")
        String databaseAuthDb = "admin";
        @Value("${spring.connector.infra.mongo.database.host:localhost}")
        String databaseHost = "localhost";
        @Value("${spring.connector.infra.mongo.database.port:27017}")
        int databasePort = 27017;
        @Value("${spring.connector.infra.mongo.database.additionalParams:}")
        String additionalParams;

    }
}
