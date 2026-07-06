package com.freshworks.core.traverser;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;

import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.freshworks.core.MockFacadeInterface;
import com.freshworks.core.ReturnableMockTypeList;
import com.freshworks.core.shared.SimpleMockUtility;

@Component
public class MockFacadeDagNodeTraversal implements MockFacadeInterface {

    @Autowired
    ApplicationContext applicationContext;

    DagNodeTraversalService dagNodeTraversalService;

    @Autowired
    SimpleMockUtility simpleMockUtility;

    ReturnableMockTypeList<DagNode> traverse;


    @Override
    public MockFacadeDagNodeTraversal configure() throws Exception {

        reset();
        traverse.addNull();
        return this;
    }

    public MockFacadeDagNodeTraversal traverse(DagNode... traverse){
        this.traverse.clear();
        this.traverse.add(traverse);
        return this;
    }

    public DagNodeTraversalService build() throws Exception {

        dagNodeTraversalService = applicationContext.getBean(DagNodeTraversalService.class);
        DagNodeTraversalService dagNodeTraversalServiceSpy = Mockito.spy(dagNodeTraversalService);
        doNothing().when(dagNodeTraversalServiceSpy).configure(anyString(), any(), any(), any(), any(), any(), any(), any());
        doNothing().when(dagNodeTraversalServiceSpy).traverse();
        return dagNodeTraversalServiceSpy;
    }
}

