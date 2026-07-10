package com.freshworks.core.shared;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.freshworks.core.shared.infra.InfraDbQueue;
import com.freshworks.core.shared.infra.InfraService;
import com.freshworks.core.shared.infra.persistent.MongoDbKeyValue;
import com.freshworks.core.shared.infra.persistent.MongoDbList;
import com.freshworks.core.shared.infra.persistent.MongoDbQueue;
import com.freshworks.core.shared.sync.SyncStatusService;
import com.freshworks.core.traverser.*;
import com.freshworks.core.traverser.net.http.HttpRequest;
import com.freshworks.core.traverser.net.http.HttpRequestResponse;
import com.freshworks.core.traverser.net.http.HttpResponse;
import com.google.common.collect.ImmutableMap;
import io.github.bucket4j.BlockingBucket;
import io.github.bucket4j.Bucket;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Phaser;
import java.util.concurrent.Semaphore;

import static org.mockito.Mockito.when;

@Component
public class SimpleMockUtility {

    ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    ApplicationContext applicationContext;

    public HttpRequestResponse mockHttpRequestResponse(){
        return Mockito.mock(HttpRequestResponse.class);
    }

    public HttpRequest mockHttpRequest(){
        return Mockito.mock(HttpRequest.class);
    }

    public HttpResponse mockHttpResponse(){
        return Mockito.mock(HttpResponse.class);
    }

    public RequestResponseContainer mockNonHttpRequestResponse(){
        RequestResponseContainer requestResponseContainer = Mockito.mock(RequestResponseContainer.class);

        when(requestResponseContainer.getRequest()).thenReturn("mysql -uroot -ppassword");
        when(requestResponseContainer.getResponse()).thenReturn("some table response data");
        return requestResponseContainer;
    }

    public StepDataBeanMapping mockStepDataBeanMapping() {

        StepDataBeanMapping stepDataBeanMapping = Mockito.mock(StepDataBeanMapping.class);
        ObjectNode objectNode = objectMapper.createObjectNode();
        objectNode.put("name", "amit");
        when(stepDataBeanMapping.getParseSyncedResponseData()).thenReturn(objectNode);

        return stepDataBeanMapping;
    }

    public Phaser mockPhaser(){

        return  Mockito.mock(Phaser.class);
    }

    public Bucket mockBucket(){

        Bucket bucket = Mockito.mock(Bucket.class);
        BlockingBucket blockingBucket = Mockito.mock(BlockingBucket.class);
        when(bucket.asBlocking()).thenReturn(blockingBucket);
        return bucket;
    }

    public Semaphore mockSemaphore(){
        return Mockito.mock(Semaphore.class);
    }

    public InfraDbQueue mockProcessorQueue(){
        return Mockito.mock(InfraDbQueue.class);
    }

    public TraverseConfigService mockTraverseConfigService(){
        return Mockito.mock(TraverseConfigService.class);
    }

    public ImmutableMap<String, String> mockImmutableMap(){

        return ImmutableMap.<String, String>builder().put("namespace","namespace").build();
    }

    public NamespaceService mockNamespace(String namespace){
        NamespaceService namespaceObj = Mockito.mock(NamespaceService.class);
        when(namespaceObj.getNamespace()).thenReturn(namespace);
        return namespaceObj;
    }

    public SyncServiceContainer mockSyncServiceContainer(){
        SyncServiceContainer syncServiceContainer = applicationContext.getBean(SyncServiceContainer.class);
        return Mockito.mock(syncServiceContainer);
    }

    public SyncServiceContainer spySyncServiceContainer(){
        SyncServiceContainer syncServiceContainer = applicationContext.getBean(SyncServiceContainer.class);
        return Mockito.spy(syncServiceContainer);
    }

    public DagTraversalService.TraverseAction mockTraverseAction(){
        return Mockito.mock(DagTraversalService.TraverseAction.class);
    }

    public DagTraversalService.TraverseAction spyTraverseAction(){
        return Mockito.spy(DagTraversalService.TraverseAction.class);
    }

    public TraverserExecutorService mockTraverserExecutorService(){
        return Mockito.mock(TraverserExecutorService.class);
    }

    public TraverserExecutorService spyTraverserExecutorService(){
        return Mockito.spy(TraverserExecutorService.class);
    }

    public SyncStatusService mockSyncStatusService(){
        return Mockito.mock(SyncStatusService.class);
    }
    public SyncServiceContainer spySyncStatusService(){
        return Mockito.spy(SyncServiceContainer.class);
    }

    public MongoDbQueue mockMongoDbQueue(){
        return Mockito.mock(MongoDbQueue.class);
    }
    public MongoDbQueue spyMongoDbQueue(){
        return Mockito.spy(MongoDbQueue.class);
    }

    public MongoDbList mockMongoDbList(){
        return Mockito.mock(MongoDbList.class);
    }
    public MongoDbList spyMongoDbList(){
        return Mockito.spy(MongoDbList.class);
    }

    public MongoDbKeyValue mockMongoDbKeyValue(){
        return Mockito.mock(MongoDbKeyValue.class);
    }
    public MongoDbKeyValue spyMongoDbKeyValue(){
        return Mockito.spy(MongoDbKeyValue.class);
    }

    public InfraService mockInfraService(){
        return Mockito.mock(InfraService.class);
    }

    public InfraService spyInfraService(){
        return Mockito.spy(InfraService.class);
    }

    public CountDownLatch mockCountDownLatch(){
        return Mockito.mock(CountDownLatch.class);
    }

    public DagNode mockDagNode(){
        return Mockito.mock(DagNode.class);
    }

    public DagNode spyDagNode(){
        return Mockito.spy(DagNode.class);
    }

}
