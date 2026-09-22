package com.freshworks.hagrid.traverser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.freshworks.hagrid.MockFacadeInterface;
import com.freshworks.hagrid.ReturnableMockTypeList;
import com.freshworks.hagrid.data.unit.dag.steps.TestApplication;
import com.freshworks.hagrid.data.unit.dag.steps.TestUser;
import com.freshworks.hagrid.shared.MockFacadeSyncServiceContainer;
import com.freshworks.hagrid.shared.SyncServiceContainer;
import com.freshworks.hagrid.traverser.TraverseConfigService;
import com.freshworks.hagrid.traverser.DagNode.NodeRateLimitObject;

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
    ReturnableMockTypeList<NodeRateLimitObject> getRateLimitForStep = new ReturnableMockTypeList<>();;

    @Override
    public MockFacadeTraverseConfigService configure(){
        reset();

        getTraverserThreadCount.add(1);
        NodeRateLimitObject stepRateLimitObject = new NodeRateLimitObject();
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

    public MockFacadeTraverseConfigService getRateLimitForStep(NodeRateLimitObject... stepRateLimitObject){
        this.getRateLimitForStep.clear();
        this.getRateLimitForStep.add(stepRateLimitObject);
        return this;
    }


    @Override
    public TraverseConfigService build() throws Exception {

        TraverseConfigService traverseConfigService = applicationContext.getBean(TraverseConfigService.class);
        TraverseConfigService traverseConfigServiceSpy = Mockito.spy(traverseConfigService);
        doAnswer(getTraverserThreadCount.answer()).when(traverseConfigServiceSpy).getTraverserThreadCount();
        doAnswer(getRateLimitForStep.answer()).when(traverseConfigServiceSpy).getRateLimitForStep(anyString());
        doAnswer(getRateLimitForStep.answer()).when(traverseConfigServiceSpy).getRateLimitForStep(any(DagNode.class));

        return traverseConfigServiceSpy;
    }

}
