package com.freshworks.hagrid.shared.infra.mongo;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;

import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.freshworks.hagrid.MockFacadeInterface;
import com.freshworks.hagrid.ReturnableMockTypeList;
import com.freshworks.hagrid.shared.MockFacadeSyncServiceContainer;
import com.freshworks.hagrid.shared.SyncServiceContainer;
import com.freshworks.hagrid.shared.infra.InfraConfigService;
import com.freshworks.hagrid.shared.infra.InfraService;
import com.freshworks.hagrid.shared.infra.MockFacadeInfraConfigService;
import com.google.common.base.Preconditions;

/**
 * Mongo twin of MockFacadeNitriteDbService.
 * Difference: the Nitrite facade runs the real configure() before spying, because an in-memory Nitrite needs
 * nothing external. MongoService.configure() builds a real MongoClient from the infra config's Mongo settings
 * (null by default) and so cannot run against these mocks; here configure() is only stubbed.
 */
@Component
public class MockFacadeMongoDbService implements MockFacadeInterface {

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    MockFacadeSyncServiceContainer mockFacadeSyncServiceContainer;

    @Autowired
    MockFacadeInfraConfigService mockFacadeInfraConfigService;

    @Autowired
    MockFacadeMongoDbList mockFacadeMongoDbList;

    @Autowired
    MockFacadeMongoDbKeyValue mockFacadeMongoKeyValue;

    @Autowired
    MockFacadeMongoDbQueue mockFacadeMongoDbQueue;

    ReturnableMockTypeList<SyncServiceContainer> syncServiceContainer = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<InfraConfigService> infraConfigService = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<MongoDbQueue> getProcessorQueue = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<MongoDbList> getPublisherList = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<MongoDbKeyValue> getKeyValue = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<MongoDbList> getInfraDbListGivenName = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<String> getNamespace = new ReturnableMockTypeList<>();


    @Override
    public MockFacadeMongoDbService configure() throws  Exception{

        reset();
        syncServiceContainer.add(mockFacadeSyncServiceContainer.configure().build());
        infraConfigService.add(mockFacadeInfraConfigService.configure().build());
        getProcessorQueue.add(mockFacadeMongoDbQueue.configure().build());
        getPublisherList.add(mockFacadeMongoDbList.configure().build());
        getKeyValue.add(mockFacadeMongoKeyValue.configure().build());
        getInfraDbListGivenName.add(mockFacadeMongoDbList.configure().build());
        getNamespace.add("some_dummy_namespace");

        return this;
    }

    public MockFacadeMongoDbService getProcessorQueue(MongoDbQueue... getProcessorQueue) {
        this.getProcessorQueue.clear();
        this.getProcessorQueue.add(getProcessorQueue);
        return this;
    }

    public MockFacadeMongoDbService getPublisherList(MongoDbList... getPublisherList) {
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

    public MockFacadeMongoDbService infraConfigService(InfraConfigService... infraConfigService) {
        this.infraConfigService.clear();
        this.infraConfigService.add(infraConfigService);
        return this;
    }

    @Override
    public InfraService build() throws Exception {

        Preconditions.checkNotNull(this.getNamespace, "Namespace must be set before hand as it is pre-requisite for configuring mongoService.");

        MongoService mongoService = new MongoService();
        mongoService = Mockito.spy(mongoService);

        doNothing().when(mongoService).configure(any(), any());
        doAnswer(getProcessorQueue.answer()).when(mongoService).getProcessorQueue();
        doAnswer(getPublisherList.answer()).when(mongoService).getPublisherList();
        doAnswer(getKeyValue.answer()).when(mongoService).getKeyValue();
        doAnswer(getInfraDbListGivenName.answer()).when(mongoService).getInfraDbList(anyString());
        doAnswer(getNamespace.answer()).when(mongoService).getNamespace();

        return mongoService;
    }
}
