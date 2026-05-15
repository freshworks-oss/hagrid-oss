package com.freshworks.core.shared.infra.h2;

import com.freshworks.core.MockFacadeInterface;
import com.freshworks.core.ReturnableMockTypeList;
import com.freshworks.core.shared.MockFacadeSyncServiceContainer;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.infra.InfraService;
import com.freshworks.core.shared.infra.persistent.MongoService;
import com.freshworks.freshindex.NamespaceService;
import com.freshworks.freshindex.index.JsonIndexService;
import com.freshworks.freshindex.index.query.JsonQueryService;
import com.google.common.base.Preconditions;
import com.zaxxer.hikari.HikariDataSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;


@Component
public class MockFacadeH2DbService implements MockFacadeInterface {

    @Autowired
    ApplicationContext applicationContext;


    @Autowired
    MockFacadeSyncServiceContainer mockFacadeSyncServiceContainer;


    @Autowired
    MockFacadeH2dbList mockFacadeH2dbList;


    @Autowired
    MockFacadeH2dbKeyValue mockFacadeH2KeyValue;

    @Autowired
    MockFacadeH2dbQueue mockFacadeH2DbQueue;

    ReturnableMockTypeList<SyncServiceContainer> syncServiceContainer = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<H2DbQueue> getProcessorQueue = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<JsonIndexService> getJsonIndexService = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<JsonQueryService> getJsonQueryService = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<NamespaceService> getNamespaceService = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<H2DbList> getPublisherList = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<H2DbKeyValue> getKeyValue = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<H2DbList> getInfraDbListGivenName = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<String> getNamespace = new ReturnableMockTypeList<>();

    HikariDataSource hikariDataSource;

    @Override
    public MockFacadeH2DbService configure() throws  Exception{

        reset();
        syncServiceContainer.add(mockFacadeSyncServiceContainer.configure().build());
        getProcessorQueue.add(mockFacadeH2DbQueue.configure().build());
        getJsonIndexService.add(Mockito.mock(JsonIndexService.class));
        getJsonQueryService.add(Mockito.mock(JsonQueryService.class));
        getNamespaceService.add(Mockito.mock(NamespaceService.class));
        getPublisherList.add(mockFacadeH2dbList.configure().build());
        getKeyValue.add(mockFacadeH2KeyValue.configure().build());
        getInfraDbListGivenName.add(mockFacadeH2dbList.configure().build());
        getNamespace.add("some_dummy_namespace");
        return this;

    }


    public MockFacadeH2DbService getProcessorQueue(H2DbQueue... getProcessorQueue) {
        this.getProcessorQueue.clear();
        this.getProcessorQueue.add(getProcessorQueue);
        return this;
    }

    public MockFacadeH2DbService getJsonIndexService(JsonIndexService... getJsonIndexService) {
        this.getJsonIndexService.clear();
        this.getJsonIndexService.add(getJsonIndexService);
        return this;
    }


    public MockFacadeH2DbService getJsonQueryService(JsonQueryService... getJsonQueryService) {
        this.getJsonQueryService.clear();
        this.getJsonQueryService.add(getJsonQueryService);
        return this;
    }

    public MockFacadeH2DbService getNamespaceService(NamespaceService... getNamespaceService) {
        this.getNamespaceService.clear();
        this.getNamespaceService.add(getNamespaceService);
        return this;
    }


    public MockFacadeH2DbService getPublisherList(H2DbList... getPublisherList) {
        this.getPublisherList.clear();
        this.getPublisherList.add(getPublisherList);
        return this;
    }


    public MockFacadeH2DbService getKeyValue(H2DbKeyValue... getKeyValue) {
        this.getKeyValue.clear();
        this.getKeyValue.add(getKeyValue);
        return this;
    }

    public MockFacadeH2DbService getInfraDbListGivenName(H2DbList... getInfraDbListGivenName) {
        this.getInfraDbListGivenName.clear();
        this.getInfraDbListGivenName.add((getInfraDbListGivenName));
        return this;
    }

    public MockFacadeH2DbService getNamespace(String... getNamespace) {
        this.getNamespace.clear();
        this.getNamespace.add(getNamespace);
        return this;
    }

    public MockFacadeH2DbService syncServiceContainer(SyncServiceContainer... syncServiceContainer) {
        this.syncServiceContainer.clear();
        this.syncServiceContainer.add(syncServiceContainer);
        return this;
    }


    public MockFacadeH2DbService addHikariDataSource(HikariDataSource hikariDataSource){
        this.hikariDataSource = hikariDataSource;
        return this;
    }

    @Override
    public InfraService build() throws Exception {

        Preconditions.checkNotNull(this.getNamespace, "Namespace must be set before hand as it is pre-requisite for configuring mongoService.");
        H2DbService h2DbService = new H2DbService();
        h2DbService = Mockito.spy(h2DbService);

        doNothing().when(h2DbService).configure(any(), any());

        doAnswer(getProcessorQueue.answer()).when(h2DbService).getProcessorQueue();

        doAnswer(getJsonIndexService.answer()).when(h2DbService).getJsonIndexService();

        doAnswer(getJsonQueryService.answer()).when(h2DbService).getJsonQueryService();

        doAnswer(getNamespaceService.answer()).when(h2DbService).getNamespaceService();

        doAnswer(getPublisherList.answer()).when(h2DbService).getPublisherList();

        doAnswer(getKeyValue.answer()).when(h2DbService).getKeyValue();

        doAnswer(getInfraDbListGivenName.answer()).when(h2DbService).getInfraDbList(anyString());

        doAnswer(getNamespace.answer()).when(h2DbService).getNamespace();

        return h2DbService;
    }
}
