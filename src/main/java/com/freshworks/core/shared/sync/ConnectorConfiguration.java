package com.freshworks.core.shared.sync;

import java.util.HashMap;
import java.util.Map;

import com.freshworks.core.traverser.AbstractStep;

import lombok.Getter;
import lombok.Setter;
import lombok.AccessLevel;


@Getter
@Setter
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


    public void setStepRateLimit(Class<? extends AbstractStep> stepClass, StepRateLimitObject stepRateLimitObject){

        stepRateLimitMap.put(stepClass.getName(), stepRateLimitObject);
    }

    public StepRateLimitObject getStepRateLimit(Class<? extends AbstractStep> stepClass){

        return stepRateLimitMap.get(stepClass.getName());
    }


    @Getter
    @Setter
    public static class StepRateLimitObject{

        int numberOfApiCalls = 100;
        int durationInSeconds = 1;
    }

    
}
