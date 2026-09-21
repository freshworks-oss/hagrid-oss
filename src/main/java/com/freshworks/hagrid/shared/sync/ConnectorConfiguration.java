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
import com.freshworks.hagrid.traverser.Annotations.FreshHierarchy;

import lombok.Getter;
import lombok.Setter;
import lombok.AccessLevel;


@Getter
@Setter
@Component
@Scope("prototype")
public class ConnectorConfiguration {

    int traverserThreadCount = 1;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    Map<String, StepRateLimitObject> stepRateLimitMap = new HashMap<>();

    List<List<String>> enabledDagPath = new ArrayList<>();

    int processorPollCount = 1000;
    int numberOfParallelProcessor = 20;

    @Value("${spring.connector.infra.type:file}")
    String infraDbType;

    @Value("${spring.connector.infra.nitrite.location:./database}")
    String infraDbLocation;

    String analyticsShouldPassTagsToMeterRegistry;

    int DEFAULT_RATE_LIMIT_API_CALLS = 100;
    int DEFAULT_RATE_LIMIT_DURATION_IN_SECONDS = 1;

    // This method is used internally by the sync service to configure whether the current execution is based on static steps or dsl based dag
    boolean dslBasedExecution = false;

    public ConnectorConfiguration(){
        this.infraDbType = "file";
        this.infraDbLocation = "./database";
        
        // Setting the default for action based steps
        StepRateLimitObject rateLimitObject = new StepRateLimitObject();
        rateLimitObject.setDurationInSeconds(1);
        rateLimitObject.setNumberOfApiCalls(1000);
        stepRateLimitMap.put(GenericNonHttpStep.class.getName(), rateLimitObject);
    }

    public void setStepRateLimit(String stepName, StepRateLimitObject stepRateLimitObject){

        stepRateLimitMap.put(stepName, stepRateLimitObject);
    }

    public void addPathToEnable(List<String> enabledPath){

        this.enabledDagPath.add(enabledPath);
    }

    public List<List<String>> getEnabledDagPathList(){

        return this.enabledDagPath;
    }
    
    public StepRateLimitObject getStepRateLimit(String stepName) throws Exception{

        if(stepRateLimitMap.containsKey(stepName)){
            return stepRateLimitMap.get(stepName);
        }
        else{

            // it means that no runtime rate limits are provided
            // Now check if @FreshHierarchy annotations are provided    

            StepRateLimitObject stepRateLimitObject = new StepRateLimitObject();

            Class<?> x = Class.forName(stepName);
            FreshHierarchy freshHierarchy = x.getAnnotation(FreshHierarchy.class);

            if(freshHierarchy != null){

                int durationInSeconds = freshHierarchy.duration();

                if(durationInSeconds == 0 ){
                    durationInSeconds = DEFAULT_RATE_LIMIT_DURATION_IN_SECONDS;
                }

                int numberOfApiCalls = freshHierarchy.rateLimit();

                if(numberOfApiCalls == 0 ){
                    numberOfApiCalls = DEFAULT_RATE_LIMIT_API_CALLS;
                }

                stepRateLimitObject.setDurationInSeconds(durationInSeconds);
                stepRateLimitObject.setNumberOfApiCalls(numberOfApiCalls);
            }

            else{

                stepRateLimitObject.setDurationInSeconds(DEFAULT_RATE_LIMIT_DURATION_IN_SECONDS);
                stepRateLimitObject.setNumberOfApiCalls(DEFAULT_RATE_LIMIT_API_CALLS);
            }

            return stepRateLimitObject;
        }
        
    }


    @Getter
    @Setter
    public static class StepRateLimitObject{

        int numberOfApiCalls = 100;
        int durationInSeconds = 1;
    }

    
}
