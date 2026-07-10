package com.freshworks.core.shared.sync;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;

import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.freshworks.core.MockFacadeInterface;
import com.freshworks.core.ReturnableMockTypeList;
import com.freshworks.core.shared.MockFacadeSyncServiceContainer;
import com.freshworks.core.shared.SyncServiceContainer;

@Component
public class MockFacadeSyncService implements MockFacadeInterface {

    @Autowired
    ApplicationContext applicationContext;

    SyncService syncService;

    @Autowired
    MockFacadeSyncServiceContainer mockFacadeSyncServiceContainer;
    ReturnableMockTypeList<SyncServiceContainer> mockSyncServiceContainer;

    ReturnableMockTypeList<SyncServiceContainer> initSyncServiceContainer;
    ReturnableMockTypeList<SyncServiceContainer> startSync;


    @Override
    public MockFacadeSyncService configure() throws Exception {
        reset();
        mockSyncServiceContainer.add(mockFacadeSyncServiceContainer.configure().build());
        initSyncServiceContainer.add(mockSyncServiceContainer.next());
        startSync.add(mockSyncServiceContainer.next());
        return this;
    }

    public MockFacadeSyncService mockSyncServiceContainer( SyncServiceContainer... syncServiceContainer ) {

        this.mockSyncServiceContainer.clear();
        this.mockSyncServiceContainer.add(syncServiceContainer);
        return this;
    }

    public MockFacadeSyncService initSyncServiceContainer( SyncServiceContainer... syncServiceContainer ) {
        this.initSyncServiceContainer.clear();
        this.initSyncServiceContainer.add(syncServiceContainer);
        return this;
    }

    public MockFacadeSyncService startSync( SyncServiceContainer... syncServiceContainer ) {
        this.startSync.clear();
        this.startSync.add(syncServiceContainer);
        return this;
    }

    @Override
    public SyncService build() throws Exception {
        syncService = applicationContext.getBean(SyncService.class);
        SyncService syncServiceSpy = Mockito.spy(syncService);
        doAnswer(startSync.answer()).when(syncServiceSpy).startSync(any(), anyString(), any(), any());
        doNothing().when(syncServiceSpy).shutdown();
        return syncServiceSpy;
    }

}
