package com.freshworks.core.shared.sync;

import com.freshworks.core.MockFacadeInterface;
import com.freshworks.core.ReturnableMockTypeList;
import com.freshworks.core.shared.MockFacadeSyncServiceContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.stereotype.Component;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;

@Component
public class MockFacadeSyncStatusService implements MockFacadeInterface {

    @SpyBean
    SyncStatusService syncStatusServiceSpy;

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
