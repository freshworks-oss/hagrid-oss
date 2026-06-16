package com.freshworks.core.shared.sync;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.freshworks.core.MockFacadeInterface;
import com.freshworks.core.ReturnableMockTypeList;

@Component
public class MockFacadeSyncStatusService implements MockFacadeInterface {

    @Autowired
    ApplicationContext applicationContext;

    SyncStatusService syncStatusService;

    ReturnableMockTypeList<Integer> setTraverserStatus;
    ReturnableMockTypeList<Integer> setProcessorStatus;
    ReturnableMockTypeList<Integer> getSyncStatus;


    @Override
    public MockFacadeSyncStatusService configure(){
        reset();
        setTraverserStatus.add(-100);
        setProcessorStatus.add(-100);
        getSyncStatus.add(-100);
        return this;
    }


    public MockFacadeSyncStatusService setTraverserStatus(Integer... setTraverserStatus){
        this.setTraverserStatus.clear();
        this.setTraverserStatus.add(setTraverserStatus);
        return this;
    }

    public MockFacadeSyncStatusService setProcessorStatus(Integer... setProcessorStatus){
        this.setProcessorStatus.clear();
        this.setProcessorStatus.add(setProcessorStatus);
        return this;
    }

    public MockFacadeSyncStatusService getSyncStatus(Integer... getSyncStatus){
        this.getSyncStatus.clear();
        this.getSyncStatus.add(getSyncStatus);
        return this;
    }

    @Override
    public SyncStatusService build() throws Exception {

        syncStatusService = applicationContext.getBean(SyncStatusService.class);
        SyncStatusService syncStatusServiceSpy = Mockito.spy(syncStatusService); 
        doNothing().when(syncStatusServiceSpy).configure(any());
        doNothing().when(syncStatusServiceSpy).setTraverserInProgress();
        doNothing().when(syncStatusServiceSpy).setProcessorInProgress();
        doNothing().when(syncStatusServiceSpy).setTraverserInFailed();
        doNothing().when(syncStatusServiceSpy).setProcessorInFailed();
        doNothing().when(syncStatusServiceSpy).setTraverserInSuccessful();
        doNothing().when(syncStatusServiceSpy).setProcessorInSuccessful();
        doAnswer(getSyncStatus.answer()).when(syncStatusServiceSpy).getSyncStatus();
        return syncStatusServiceSpy;
    }
}
