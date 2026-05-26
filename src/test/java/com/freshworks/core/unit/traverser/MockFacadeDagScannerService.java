package com.freshworks.core.traverser;

import com.freshworks.core.MockFacadeInterface;
import com.freshworks.core.ReturnableMockTypeList;
import com.freshworks.core.data.four_zero_zero.unit.dag.steps.TestApplication;
import com.freshworks.core.data.four_zero_zero.unit.dag.steps.TestServicePrinciple;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;

@Component
public class MockFacadeDagScannerService implements MockFacadeInterface {

    @SpyBean
    DagScannerService dagScannerServiceSpy;

    @Autowired
    MockFacadeDagNode mockFacadeDagNode;

    ReturnableMockTypeList<DagNode> scanner;

    ReturnableMockTypeList<Set<Class<?>>> getSteps;

    ReturnableMockTypeList<DagNode> createDAG;




    public MockFacadeDagScannerService configure() throws Exception {
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


    public MockFacadeDagScannerService scanner(DagNode... scanner){
        this.scanner.clear();
        this.scanner.add(scanner);
        return this;
    }

    public MockFacadeDagScannerService getSteps(Set<Class<?>>... getSteps){
        this.getSteps.clear();
        this.getSteps.add(getSteps);
        return this;
    }

    public MockFacadeDagScannerService createDAG(DagNode... nodes){
        this.createDAG.clear();
        this.createDAG.add(nodes);
        return this;
    }


    @Override
    public DagScannerService build() throws Exception {

        doAnswer(scanner.answer()).when(dagScannerServiceSpy).scanner(any(), any());
        doAnswer(getSteps.answer()).when(dagScannerServiceSpy).getSteps(any(), anyString());
        doAnswer(createDAG.answer()).when(dagScannerServiceSpy).createDAG(any(), any(), any());
        return dagScannerServiceSpy;
    }
}
