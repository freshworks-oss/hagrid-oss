package com.freshworks.core.traverser;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;

import java.net.URISyntaxException;

import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.freshworks.core.MockFacadeInterface;
import com.freshworks.core.ReturnableMockTypeList;
import com.freshworks.core.traverser.exception.StepFailedException;
import com.freshworks.core.traverser.net.MockFacadeRequestResponseContainer;

@Component
public class MockFacadeNonHttpAbstractStep implements MockFacadeInterface {

    ReturnableMockTypeList<Class<? extends NonHttpAbstractStep>> abstractStepClass;
    ReturnableMockTypeList<Boolean> shouldProceedWithParentObjectNonHttp;

    @Autowired
    MockFacadeRequestResponseContainer mockFacadeRequestResponseContainer;
    ReturnableMockTypeList<RequestResponseContainer> startSyncNonHttp;
    ReturnableMockTypeList<RequestResponseContainer> executeNonHttp;
    ReturnableMockTypeList<RequestResponseContainer> getNextSyncRequestNonHttp;

    ReturnableMockTypeList<Boolean> isValidResponseNonHttp;

    ReturnableMockTypeList<DagTraversalService.TraverseAction> handleInValidResponseNonHttp;

    ReturnableMockTypeList<Boolean> isSyncCompleteNonHttp;

    ReturnableMockTypeList<StepDataBeanMapping> parseSyncResponseNonHttp;


    public MockFacadeNonHttpAbstractStep configure() throws Exception {
        reset();

        abstractStepClass.add(MockNonHttpAbstractStep.class);
        shouldProceedWithParentObjectNonHttp.add(true);
        startSyncNonHttp.add(mockFacadeRequestResponseContainer.configure().build());
        executeNonHttp.add(mockFacadeRequestResponseContainer.configure().build());
        getNextSyncRequestNonHttp.add(mockFacadeRequestResponseContainer.configure().build());

        isValidResponseNonHttp.add(true);

        handleInValidResponseNonHttp.add(new DagTraversalService.TraverseAction());

        isSyncCompleteNonHttp.add(true);

        StepDataBeanMapping stepDataBeanMapping = new StepDataBeanMapping();
        parseSyncResponseNonHttp.add(stepDataBeanMapping);

        return this;
    }

    public MockFacadeNonHttpAbstractStep abstractStepClass(Class<? extends NonHttpAbstractStep>... abstractStepClass) throws StepFailedException {

        this.abstractStepClass.clear();;
        this.abstractStepClass.add(abstractStepClass);
        return this;
    }


    public MockFacadeNonHttpAbstractStep shouldProceedWithParentObjectNonHttp(Boolean... shouldProceedWithParentObject) throws StepFailedException {

        this.shouldProceedWithParentObjectNonHttp.clear();
        this.shouldProceedWithParentObjectNonHttp.add(shouldProceedWithParentObject);
        return this;
    }


    public MockFacadeNonHttpAbstractStep startSyncNonHttp(RequestResponseContainer... requestResponseContainer) throws StepFailedException {

        this.startSyncNonHttp.clear();
        this.startSyncNonHttp.add(requestResponseContainer);
        return this;
    }

    public MockFacadeNonHttpAbstractStep executeNonHttp(RequestResponseContainer... requestResponseContainer) throws StepFailedException {
        this.executeNonHttp.clear();
        this.executeNonHttp.add(requestResponseContainer);
        return this;
    }

    public MockFacadeNonHttpAbstractStep getNextSyncRequestNonHttp(RequestResponseContainer... requestResponseContainer) throws StepFailedException {

        this.getNextSyncRequestNonHttp.clear();
        this.getNextSyncRequestNonHttp.add(requestResponseContainer);
        return this;
    }

    public MockFacadeNonHttpAbstractStep isValidResponseNonHttp(Boolean... validResponse) throws StepFailedException {

        this.isValidResponseNonHttp.clear();
        this.isValidResponseNonHttp.add(validResponse);
        return this;
    }


    public MockFacadeNonHttpAbstractStep handleInValidResponseNonHttp(DagTraversalService.TraverseAction... traverseAction) throws StepFailedException, URISyntaxException {

        this.handleInValidResponseNonHttp.clear();
        this.handleInValidResponseNonHttp.add(traverseAction);
        return this;
    }


    public MockFacadeNonHttpAbstractStep parseSyncResponseNonHttp(StepDataBeanMapping... stepDataBeanMapping){

        this.parseSyncResponseNonHttp.clear();
        this.parseSyncResponseNonHttp.add(stepDataBeanMapping);
        return this;
    }

    public MockFacadeNonHttpAbstractStep isSyncCompleteNonHttp(Boolean... syncComplete) throws Exception {

        this.isSyncCompleteNonHttp.clear();
        this.isSyncCompleteNonHttp.add(syncComplete);
        return this;
    }


    public AbstractStep build() throws Exception {

        NonHttpAbstractStep abstractStep = Mockito.spy(abstractStepClass.next());

        doAnswer(shouldProceedWithParentObjectNonHttp.answer()).when(abstractStep).shouldProceedWithParentObjectNonHttp(any(), any());
        doAnswer(startSyncNonHttp.answer()).when(abstractStep).startSyncNonHttp(any());
        doAnswer(executeNonHttp.answer()).when(abstractStep).executeNonHttp(any(), any());
        doAnswer(getNextSyncRequestNonHttp.answer()).when(abstractStep).getNextSyncRequestNonHttp(any(), any());
        doAnswer(isValidResponseNonHttp.answer()).when(abstractStep).isValidResponseNonHttp(any(), any());
        doAnswer(handleInValidResponseNonHttp.answer()).when(abstractStep).handleInValidResponseNonHttp(any(), any());
        doAnswer(isSyncCompleteNonHttp.answer()).when(abstractStep).isSyncCompleteNonHttp(any(), any());
        doAnswer(parseSyncResponseNonHttp.answer()).when(abstractStep).parseSyncResponseNonHttp(any(), any());
        doNothing().when(abstractStep).filterResponseNonHttp(any(), any());
        doNothing().when(abstractStep).setupNonHttp(any(), any());

        return abstractStep;
    }
}
