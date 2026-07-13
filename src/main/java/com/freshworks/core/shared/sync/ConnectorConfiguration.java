package com.freshworks.core.shared.sync;

import java.util.HashMap;
import java.util.Map;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.freshworks.core.traverser.AbstractStep;
import com.freshworks.core.traverser.Annotations.FreshHierarchy;

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

    int processorPollCount = 1000;
    int numberOfParallelProcessor = 20;

    String infraDbType = "file";
    String infraDbLocation = "./";

    String analyticsShouldPassTagsToMeterRegistry;

    int DEFAULT_RATE_LIMIT_API_CALLS = 100;
    int DEFAULT_RATE_LIMIT_DURATION_IN_SECONDS = 1;

    public void setStepRateLimit(Class<? extends AbstractStep> stepClass, StepRateLimitObject stepRateLimitObject){

        stepRateLimitMap.put(stepClass.getName(), stepRateLimitObject);
    }

    public StepRateLimitObject getStepRateLimit(Class<? extends AbstractStep> stepClass){

        if(stepRateLimitMap.containsKey(stepClass.getName())){
            return stepRateLimitMap.get(stepClass.getName());
        }
        else{

            // it means that no runtime rate limits are provided
            // Now check if @FreshHierarchy annotations are provided    

            StepRateLimitObject stepRateLimitObject = new StepRateLimitObject();

            FreshHierarchy freshHierarchy = stepClass.getAnnotation(FreshHierarchy.class);

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
