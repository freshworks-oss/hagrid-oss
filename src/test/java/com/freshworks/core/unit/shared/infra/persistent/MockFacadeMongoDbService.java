package com.freshworks.core.shared.infra.persistent;

import com.freshworks.core.MockFacadeInterface;
import com.freshworks.core.ReturnableMockTypeList;
import com.freshworks.core.shared.MockFacadeSyncServiceContainer;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.infra.InfraService;
import com.freshworks.freshindex.NamespaceService;
import com.freshworks.freshindex.index.JsonIndexService;
import com.freshworks.freshindex.index.query.JsonQueryService;
import com.google.common.base.Preconditions;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;


@Component
public class MockFacadeMongoDbService implements MockFacadeInterface {

    @Autowired
    ApplicationContext applicationContext;


    @Autowired
    MockFacadeSyncServiceContainer mockFacadeSyncServiceContainer;


    @Autowired
    MockFacadeMongodbList mockFacadeMongodbList;


    @Autowired
    MockFacadeMongodbKeyValue mockFacadeMongodbKeyValue;

    @Autowired
    MockFacadeMongodbQueue mockFacadeMongodbQueue;

    ReturnableMockTypeList<SyncServiceContainer> syncServiceContainer = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<MongoDbQueue> getProcessorQueue = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<JsonIndexService> getJsonIndexService = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<JsonQueryService> getJsonQueryService = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<NamespaceService> getNamespaceService = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<MongoDbList> getPublisherList = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<MongoDbKeyValue> getKeyValue = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<MongoDbList> getInfraDbListGivenName = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<String> getNamespace = new ReturnableMockTypeList<>();


    @Override
    public MockFacadeMongoDbService configure() throws  Exception{

        reset();

        syncServiceContainer.add(mockFacadeSyncServiceContainer.configure().build());
        getProcessorQueue.add(mockFacadeMongodbQueue.configure().build());
        getJsonIndexService.add(Mockito.mock(JsonIndexService.class));
        getJsonQueryService.add(Mockito.mock(JsonQueryService.class));
        getNamespaceService.add(Mockito.mock(NamespaceService.class));
        getPublisherList.add(mockFacadeMongodbList.configure().build());
        getKeyValue.add(mockFacadeMongodbKeyValue.configure().build());
        getInfraDbListGivenName.add(mockFacadeMongodbList.configure().build());
        getNamespace.add("some_dummy_namespace");
        return this;

    }


    public MockFacadeMongoDbService getProcessorQueue(MongoDbQueue... getProcessorQueue) {
        this.getProcessorQueue.clear();
        this.getProcessorQueue.add(getProcessorQueue);
        return this;
    }

    public MockFacadeMongoDbService getJsonIndexService(JsonIndexService... getJsonIndexService) {
        this.getJsonIndexService.clear();
        this.getJsonIndexService.add(getJsonIndexService);
        return this;
    }


    public MockFacadeMongoDbService getJsonQueryService(JsonQueryService... getJsonQueryService) {
        this.getJsonQueryService.clear();
        this.getJsonQueryService.add(getJsonQueryService);
        return this;
    }

    public MockFacadeMongoDbService getNamespaceService(NamespaceService... getNamespaceService) {
        this.getNamespaceService.clear();
        this.getNamespaceService.add(getNamespaceService);
        return this;
    }


    public MockFacadeMongoDbService getPublisherList( MongoDbList... getPublisherList) {
        this.getPublisherList.clear();
        this.getPublisherList.add(getPublisherList);
        return this;
    }


    public MockFacadeMongoDbService getKeyValue(MongoDbKeyValue... getKeyValue) {
        this.getKeyValue.clear();
        this.getKeyValue.add(getKeyValue);
        return this;
    }

    public MockFacadeMongoDbService getInfraDbListGivenName(MongoDbList... getInfraDbListGivenName) {
        this.getInfraDbListGivenName.clear();
        this.getInfraDbListGivenName.add((getInfraDbListGivenName));
        return this;
    }

    public MockFacadeMongoDbService getNamespace(String... getNamespace) {
        this.getNamespace.clear();
        this.getNamespace.add(getNamespace);
        return this;
    }

    public MockFacadeMongoDbService syncServiceContainer(SyncServiceContainer... syncServiceContainer) {
        this.syncServiceContainer.clear();
        this.syncServiceContainer.add(syncServiceContainer);
        return this;
    }

    @Override
    public InfraService build() throws Exception {

        Preconditions.checkNotNull(this.getNamespace, "Namespace must be set before hand as it is pre-requisite for configuring mongoService.");
        MongoService mongoService = new MongoService();
        mongoService = Mockito.spy(mongoService);

        doNothing().when(mongoService).configure(any(), any());

        doAnswer(getProcessorQueue.answer()).when(mongoService).getProcessorQueue();

        doAnswer(getJsonIndexService.answer()).when(mongoService).getJsonIndexService();

        doAnswer(getJsonQueryService.answer()).when(mongoService).getJsonQueryService();

        doAnswer(getNamespaceService.answer()).when(mongoService).getNamespaceService();

        doAnswer(getPublisherList.answer()).when(mongoService).getPublisherList();

        doAnswer(getKeyValue.answer()).when(mongoService).getKeyValue();

        doAnswer(getInfraDbListGivenName.answer()).when(mongoService).getInfraDbList(anyString());

        doAnswer(getNamespace.answer()).when(mongoService).getNamespace();

        return mongoService;
    }
}
