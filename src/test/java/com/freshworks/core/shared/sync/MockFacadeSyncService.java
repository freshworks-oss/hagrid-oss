package com.freshworks.core.shared.sync;

import com.freshworks.core.MockFacadeInterface;
import com.freshworks.core.ReturnableMockTypeList;
import com.freshworks.core.shared.MockFacadeSyncServiceContainer;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.traverser.ParentStep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;

@Profile("!(four_five_zero.performance.inmemory | four_five_zero.performance.persistent)") 
@Component
public class MockFacadeSyncService implements MockFacadeInterface {

    @Autowired
    ApplicationContext applicationContext;

    @SpyBean
    SyncService syncServiceSpy;

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

        doAnswer(initSyncServiceContainer.answer()).when(syncServiceSpy).initSyncServiceContainer(anyString(), any(), any());
        doNothing().when(syncServiceSpy).startSync(any(SyncServiceContainer.class));
        doAnswer(startSync.answer()).when(syncServiceSpy).startSync(any(), anyString(), any());
        doNothing().when(syncServiceSpy).shutdown();
        return syncServiceSpy;
    }

}
