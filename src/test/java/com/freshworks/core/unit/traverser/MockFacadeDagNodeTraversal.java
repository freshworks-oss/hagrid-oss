package com.freshworks.core.traverser;


import com.freshworks.core.MockFacadeInterface;
import com.freshworks.core.ReturnableMockTypeList;
import com.freshworks.core.shared.SimpleMockUtility;
import com.freshworks.core.shared.MockFacadeSyncServiceContainer;
import com.freshworks.core.shared.Namespace;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.analytics.AnalyticsService;
import com.freshworks.core.shared.infra.InfraService;
import com.freshworks.core.shared.infra.persistent.MockFacadeMongoDbService;
import com.google.common.collect.ImmutableMap;
import io.github.bucket4j.Bucket;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Phaser;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;

@Component
public class MockFacadeDagNodeTraversal implements MockFacadeInterface {

    @SpyBean
    DagNodeTraversalService dagNodeTraversalServiceSpy;

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

        doNothing().when(dagNodeTraversalServiceSpy).configure(anyString(), any(), any(), any(), any(), any(), any(), any());
        doNothing().when(dagNodeTraversalServiceSpy).traverse();
        return dagNodeTraversalServiceSpy;
    }
}

