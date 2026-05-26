package com.freshworks.core.traverser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.freshworks.core.MockFacadeInterface;
import com.freshworks.core.ReturnableMockTypeList;
import com.freshworks.core.data.four_zero_zero.unit.dag.steps.TestApplication;
import com.freshworks.core.shared.MockFacadeSyncServiceContainer;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.data.four_zero_zero.unit.dag.steps.TestUser;
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
    ReturnableMockTypeList<SyncServiceContainer> syncServiceContainer;

    ReturnableMockTypeList<Integer> getTraverserThreadCount;
    ReturnableMockTypeList<String> getStepLocation;
    ReturnableMockTypeList<String> getBeanLocation;
    ReturnableMockTypeList<JsonNode> getRateLimitForStep;

    @Override
    public MockFacadeTraverseConfigService configure(){
        reset();

        ObjectMapper mapper = new ObjectMapper();
        getTraverserThreadCount.add(1);
        getStepLocation.add("com.freshworks.core.data.dag.steps");
        getBeanLocation.add("com.freshworks.core.data.dag.beans");

        ObjectNode rateLimitObjectNode = mapper.createObjectNode();
        ObjectNode stepObjectNode = mapper.createObjectNode();
        ObjectNode rateLimitNode = mapper.createObjectNode();
        rateLimitNode.put("api_count", 100);
        rateLimitNode.put("seconds", 20);
        stepObjectNode.put(TestUser.class.getName(), rateLimitNode);
        rateLimitObjectNode.put("rateLimit", stepObjectNode);
        getRateLimitForStep.add(rateLimitObjectNode);

        DagNode dagNode = new DagNode(TestApplication.class.getName());
        HashMap<String, Object> hagridManagedBeans = new HashMap<>();
        hagridManagedBeans.put(DagNode.class.getName(), dagNode);
        syncServiceContainer.add(mockFacadeSyncServiceContainer.configure().hagridManagedBeans(hagridManagedBeans).build());

        return this;
    }


    public MockFacadeTraverseConfigService getTraverserThreadCount(Integer... getTraverserThreadCount){
        this.getTraverserThreadCount.clear();
        this.getTraverserThreadCount.add(getTraverserThreadCount);
        return this;
    }

    public MockFacadeTraverseConfigService getStepLocation(String... getStepLocation){
        this.getStepLocation.clear();
        this.getStepLocation.add(getStepLocation);
        return this;
    }

    public MockFacadeTraverseConfigService getBeanLocation(String... getBeanLocation){
        this.getBeanLocation.clear();
        this.getBeanLocation.add(getBeanLocation);
        return this;
    }

    public MockFacadeTraverseConfigService getRateLimitForStep(JsonNode... jsonNodes){
        this.getRateLimitForStep.clear();
        this.getRateLimitForStep.add(jsonNodes);
        return this;
    }


    public MockFacadeTraverseConfigService syncServiceContainer(SyncServiceContainer... syncServiceContainer){
        this.syncServiceContainer.clear();
        this.syncServiceContainer.add(syncServiceContainer);
        return this;
    }

    @Override
    public TraverseConfigService build() throws Exception {

        TraverseConfigService traverseConfigService = applicationContext.getBean(TraverseConfigService.class);
        TraverseConfigService traverseConfigServiceSpy = Mockito.spy(traverseConfigService);
        traverseConfigServiceSpy.configure(syncServiceContainer.next());
        doNothing().when(traverseConfigServiceSpy).configure(any());
        doAnswer(getTraverserThreadCount.answer()).when(traverseConfigServiceSpy).getTraverserThreadCount();
        doNothing().when(traverseConfigServiceSpy).setTraverserThreadCount(anyInt());
        doAnswer(getStepLocation.answer()).when(traverseConfigServiceSpy).getStepLocation();
        doNothing().when(traverseConfigServiceSpy).setStepLocation(anyString());
        doAnswer(getBeanLocation.answer()).when(traverseConfigServiceSpy).getBeanLocation();
        doNothing().when(traverseConfigServiceSpy).setBeanLocation(anyString());
        doNothing().when(traverseConfigServiceSpy).setRateLimitForStep(any(), anyInt(), anyInt());
        doAnswer(getRateLimitForStep.answer()).when(traverseConfigServiceSpy).getRateLimitForStep(any());

        return traverseConfigServiceSpy;
    }

}
