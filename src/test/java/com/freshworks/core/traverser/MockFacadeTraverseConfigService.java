package com.freshworks.core.traverser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.freshworks.core.MockFacadeInterface;
import com.freshworks.core.ReturnableMockTypeList;
import com.freshworks.core.data.unit.dag.steps.TestApplication;
import com.freshworks.core.data.unit.dag.steps.TestUser;
import com.freshworks.core.shared.MockFacadeSyncServiceContainer;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.sync.ConnectorConfiguration.StepRateLimitObject;

import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.HashMap;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;

@Component
public class MockFacadeTraverseConfigService implements MockFacadeInterface {

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    MockFacadeSyncServiceContainer mockFacadeSyncServiceContainer;

    ReturnableMockTypeList<Integer> getTraverserThreadCount = new ReturnableMockTypeList<>();
    ReturnableMockTypeList<StepRateLimitObject> getRateLimitForStep = new ReturnableMockTypeList<>();;

    @Override
    public MockFacadeTraverseConfigService configure(){
        reset();

        getTraverserThreadCount.add(1);
        StepRateLimitObject stepRateLimitObject = new StepRateLimitObject();
        stepRateLimitObject.setDurationInSeconds(1);
        stepRateLimitObject.setNumberOfApiCalls(100);
        getRateLimitForStep.add(stepRateLimitObject);
        
        return this;
    }


    public MockFacadeTraverseConfigService getTraverserThreadCount(Integer... getTraverserThreadCount){
        this.getTraverserThreadCount.clear();
        this.getTraverserThreadCount.add(getTraverserThreadCount);
        return this;
    }

    public MockFacadeTraverseConfigService getRateLimitForStep(StepRateLimitObject... stepRateLimitObject){
        this.getRateLimitForStep.clear();
        this.getRateLimitForStep.add(stepRateLimitObject);
        return this;
    }


    @Override
    public TraverseConfigService build() throws Exception {

        TraverseConfigService traverseConfigService = applicationContext.getBean(TraverseConfigService.class);
        TraverseConfigService traverseConfigServiceSpy = Mockito.spy(traverseConfigService);
        doAnswer(getTraverserThreadCount.answer()).when(traverseConfigServiceSpy).getTraverserThreadCount();
        doAnswer(getRateLimitForStep.answer()).when(traverseConfigServiceSpy).getRateLimitForStep(any());

        return traverseConfigServiceSpy;
    }

}
