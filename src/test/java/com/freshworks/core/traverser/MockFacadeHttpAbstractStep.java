package com.freshworks.core.traverser;

import com.freshworks.core.MockFacadeInterface;
import com.freshworks.core.ReturnableMockTypeList;
import com.freshworks.core.shared.MockFacadeSyncServiceContainer;
import com.freshworks.core.traverser.exception.StepFailedException;
import com.freshworks.core.traverser.net.http.*;
import org.apache.hc.core5.http.ParseException;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Component
public class MockFacadeHttpAbstractStep implements MockFacadeInterface {


    @Autowired
    MockFacadeHttpRequestResponse mockFacadeHttpRequestResponse;

    @Autowired
    MockFacadeHttpRequest mockFacadeHttpRequest;

    @Autowired
    MockFacadeHttpResponse mockFacadeHttpResponse;

    @Autowired
    MockFacadeSyncServiceContainer mockFacadeSyncServiceContainer;

    ReturnableMockTypeList<HttpRequestResponse> httpRequestResponse = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<Class<? extends HttpAbstractStep>> abstractStepClass = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<Boolean> shouldProceedWithParentObject = new ReturnableMockTypeList<>();


    ReturnableMockTypeList<HttpRequestResponse> startSync = new ReturnableMockTypeList<>();


    ReturnableMockTypeList<HttpRequestResponse> getNextSyncRequestWithHttpRequestResponse = new ReturnableMockTypeList<>();


    ReturnableMockTypeList<Boolean> isValidResponseWithHttp = new ReturnableMockTypeList<>();


    ReturnableMockTypeList<DagTraversalService.TraverseAction> handleInvalidResponseWithHttp = new ReturnableMockTypeList<>();


    ReturnableMockTypeList<Boolean> isSyncCompleteWithHttp = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<StepDataBeanMapping> parseSyncResponseWithHttp = new ReturnableMockTypeList<>();



    public MockFacadeHttpAbstractStep configure() throws IOException, NoSuchAlgorithmException, KeyStoreException, ParseException, URISyntaxException, KeyManagementException, InterruptedException {

        reset();
        abstractStepClass.add(MockHttpAbstractStep.class);

        shouldProceedWithParentObject.add(true);

        httpRequestResponse.add(mockFacadeHttpRequestResponse.configure().build());
        startSync.add(httpRequestResponse.next());

        httpRequestResponse.add(mockFacadeHttpRequestResponse.configure().build());
        getNextSyncRequestWithHttpRequestResponse.add(httpRequestResponse.next());

        isValidResponseWithHttp.add(true);

        handleInvalidResponseWithHttp.add(new DagTraversalService.TraverseAction());
        handleInvalidResponseWithHttp.next().holdAndReTry(1, TimeUnit.MILLISECONDS);

        isSyncCompleteWithHttp.add(true);

        StepDataBeanMapping stepDataBeanMapping = new StepDataBeanMapping();
        parseSyncResponseWithHttp.add(stepDataBeanMapping);

        return this;
    }

    public MockFacadeHttpAbstractStep abstractStep(Class<? extends HttpAbstractStep>... stepClass){
        this.abstractStepClass.clear();
        this.abstractStepClass.add(stepClass);
        return this;
    }

    public MockFacadeHttpAbstractStep httpRequestResponse(HttpRequestResponse... httpRequestResponse){
        this.httpRequestResponse.clear();
        this.httpRequestResponse.add(httpRequestResponse);
        return this;
    }

    public MockFacadeHttpAbstractStep shouldProceedWithParentObject(Boolean... shouldProceedWithParentObject ) throws StepFailedException {
        this.shouldProceedWithParentObject.clear();
        this.shouldProceedWithParentObject.add(shouldProceedWithParentObject);
        return this;
    }

    public MockFacadeHttpAbstractStep startSync(HttpRequestResponse... startSync) throws StepFailedException {
        this.startSync.clear();
        this.startSync.add(startSync);
        return this;
    }


    public MockFacadeHttpAbstractStep getNextSyncRequestWithHttpRequestResponse(HttpRequestResponse... getNextSyncRequestWithHttpRequestResponse) throws StepFailedException {
        this.getNextSyncRequestWithHttpRequestResponse.clear();
        this.getNextSyncRequestWithHttpRequestResponse.add(getNextSyncRequestWithHttpRequestResponse);
        return this;
    }

    public MockFacadeHttpAbstractStep isValidResponseWithHttp(Boolean... isValidResponseWithHttp) throws StepFailedException {
        this.isValidResponseWithHttp.clear();
        this.isValidResponseWithHttp.add(isValidResponseWithHttp);
        return this;
    }

    public MockFacadeHttpAbstractStep handleInvalidResponseWithHttp(DagTraversalService.TraverseAction... handleInvalidResponseWithHttp) throws StepFailedException {
        this.handleInvalidResponseWithHttp.clear();
        this.handleInvalidResponseWithHttp.add(handleInvalidResponseWithHttp);
        return this;
    }


    public MockFacadeHttpAbstractStep isSyncCompleteWithHttp(Boolean... isSyncCompleteWithHttp) throws StepFailedException {
        this.isSyncCompleteWithHttp.clear();
        this.isSyncCompleteWithHttp.add(isSyncCompleteWithHttp);
        return this;
    }

    public MockFacadeHttpAbstractStep parseSyncResponseWithHttp( StepDataBeanMapping... parseSyncResponseWithHttp) throws StepFailedException {
        this.parseSyncResponseWithHttp.clear();
        this.parseSyncResponseWithHttp.add(parseSyncResponseWithHttp);
        return this;
    }


    public AbstractStep build() throws Exception {

        HttpAbstractStep abstractStep = Mockito.spy(abstractStepClass.next());

        doNothing().when(abstractStep).configure(any());

        doNothing().when(abstractStep).setup(any(), any());

        doAnswer(shouldProceedWithParentObject.answer()).when(abstractStep).shouldProceedWithParentObject(any(), any());

        doAnswer(startSync.answer()).when(abstractStep).startSync(any());

        doAnswer(getNextSyncRequestWithHttpRequestResponse.answer()).when(abstractStep).getNextSyncRequest(any(), any(), any());

        doAnswer(isValidResponseWithHttp.answer()).when(abstractStep).isValidResponse(any(), any());

        doAnswer(handleInvalidResponseWithHttp.answer()).when(abstractStep).handleInvalidResponse(any(), any());

        doAnswer(isSyncCompleteWithHttp.answer()).when(abstractStep).isSyncComplete(any(), any());

        doAnswer(parseSyncResponseWithHttp.answer()).when(abstractStep).parseSyncResponse(any(), any());

        doNothing().when(abstractStep).closeSync();

        return abstractStep;
    }
}
