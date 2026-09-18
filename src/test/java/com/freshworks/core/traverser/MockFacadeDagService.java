package com.freshworks.core.traverser;

import com.freshworks.core.MockFacadeInterface;
import com.freshworks.core.ReturnableMockTypeList;
import com.freshworks.core.data.unit.dag.steps.TestApplication;
import com.freshworks.core.data.unit.dag.steps.TestServicePrinciple;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.mockito.Mockito;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

@Component
public class MockFacadeDagService implements MockFacadeInterface {

    @Autowired
    ApplicationContext applicationContext;

    DagService dagService;

    @Autowired
    MockFacadeDagNode mockFacadeDagNode;

    ReturnableMockTypeList<DagNode> scanner = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<Set<Class<?>>> getSteps = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<DagNode> createDAG = new ReturnableMockTypeList<>();




    public MockFacadeDagService configure() throws Exception {
        reset();

        DagNode dagNode = mockFacadeDagNode.configure().build();
        scanner.add(dagNode);
        createDAG.add(dagNode);
        Set<Class<?>> a = new HashSet<>();
        a.add(TestApplication.class);
        a.add(TestServicePrinciple.class);
        getSteps.add(a);
        return this;
    }


    public MockFacadeDagService scanner(DagNode... scanner){
        this.scanner.clear();
        this.scanner.add(scanner);
        return this;
    }

    public MockFacadeDagService getSteps(Set<Class<?>>... getSteps){
        this.getSteps.clear();
        this.getSteps.add(getSteps);
        return this;
    }

    public MockFacadeDagService createDAG(DagNode... nodes){
        this.createDAG.clear();
        this.createDAG.add(nodes);
        return this;
    }


    @Override
    public DagService build() throws Exception {

        dagService = applicationContext.getBean(DagService.class);
        DagService dagServiceSpy = Mockito.spy(dagService);
        doAnswer(scanner.answer()).when(dagServiceSpy).scanner(any(), any());
        doAnswer(createDAG.answer()).when(dagServiceSpy).createDAG(any(), any());
        return dagServiceSpy;
    }
}
