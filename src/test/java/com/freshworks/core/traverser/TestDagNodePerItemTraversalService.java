package com.freshworks.core.traverser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.freshworks.core.data.four_zero_zero.unit.dag.beans.ComplexBean;
import com.freshworks.core.data.four_zero_zero.unit.dag.beans.SimpleBean;
import com.freshworks.core.data.four_zero_zero.unit.traverser.single.steps.TestSingleApplicationStep;
import com.freshworks.core.data.four_zero_zero.unit.traverser.single.steps.TestSingleNonHttpApplicationStep;
import com.freshworks.core.shared.SimpleMockUtility;
import com.freshworks.core.shared.MockFacadeSyncServiceContainer;
import com.freshworks.core.traverser.net.http.MockFacadeHttpRequestResponse;
import com.freshworks.core.traverser.net.http.HttpRequestResponse;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.mockito.InOrder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
@EnabledIfSystemProperty(named = "spring.profiles.active", matches = ".*\\.unit\\..*")
public class TestDagNodePerItemTraversalService {

    ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    MockFacadeSyncServiceContainer mockFacadeSyncServiceContainer;

    @Autowired
    MockFacadeDagNode mockFacadeDagNode;

    @Autowired
    SimpleMockUtility traverserMockUtility;

    @Autowired
    MockFacadeDagNodePerItemTraversal dagNodePerItemTraversalFacade;

    @Autowired
    MockFacadeHttpAbstractStep httpAbstractStepFacade;

    @Autowired
    MockFacadeHttpRequestResponse mockFacadeHttpRequestResponse;

    @Autowired
    MockFacadeNonHttpAbstractStep nonHttpAbstractStepFacade;


    @BeforeEach
    public void beforeEach() throws Exception {

        mockFacadeSyncServiceContainer.configure();
        mockFacadeDagNode.configure();
        httpAbstractStepFacade.configure();
        dagNodePerItemTraversalFacade.configure();
        nonHttpAbstractStepFacade.configure();
        mockFacadeHttpRequestResponse.configure();

    }

    @Nested
    class WhenStepIsHttpAbstractStep {
        @Test
        public void testWhenHttpStepsExecutionInRightOrder() throws Exception {

            HttpRequestResponse httpRequestResponse = mockFacadeHttpRequestResponse.build();
            StepDataBeanMapping stepDataBeanMapping = traverserMockUtility.mockStepDataBeanMapping();
            DagNode dagNode = new DagNode("dummy dag node");
            ObjectNode parentNode = objectMapper.createObjectNode();


            AbstractStep mockedAbstractStep = httpAbstractStepFacade.abstractStep(TestSingleApplicationStep.class)
                    .shouldProceedWithParentObject(true)
                    .startSync(httpRequestResponse)
                    .isValidResponseWithHttp(true)
                    .parseSyncResponseWithHttp(stepDataBeanMapping)
                    .isSyncCompleteWithHttp(true)
                    .build();

            DagNodePerItemTraversalService dagNodePerItemTraversalService = dagNodePerItemTraversalFacade.abstractStep(mockedAbstractStep)
                    .parentNode(parentNode)
                    .build();

            doCallRealMethod().when(dagNodePerItemTraversalService).traverse();
            doCallRealMethod().when(dagNodePerItemTraversalService).ShouldProceedWithParentObjectHttp(any(), any());
            doCallRealMethod().when(dagNodePerItemTraversalService).setupHttp(any(), any(), any());
            doCallRealMethod().when(dagNodePerItemTraversalService).startSyncHttp(any(), any());
            doCallRealMethod().when(dagNodePerItemTraversalService).isValidResponseHttp(any(), any(), any());
            doCallRealMethod().when(dagNodePerItemTraversalService).parseSyncResponseHttp(any(), any(), any());
            doCallRealMethod().when(dagNodePerItemTraversalService).filterHttp(any(), any(), any());
            doCallRealMethod().when(dagNodePerItemTraversalService).isSyncCompleteHttp(any(), any(), any());

            dagNodePerItemTraversalService.traverse();

            InOrder order = inOrder(dagNodePerItemTraversalService);
            order.verify(dagNodePerItemTraversalService, times(1)).ShouldProceedWithParentObjectHttp(any(), any());
            order.verify(dagNodePerItemTraversalService, times(1)).setupHttp(any(), any(), any());
            order.verify(dagNodePerItemTraversalService, times(1)).startSyncHttp(any(), any());
            order.verify(dagNodePerItemTraversalService, times(1)).isValidResponseHttp(any(), any(), any());
            order.verify(dagNodePerItemTraversalService, times(0)).handleInValidResponseHttp(any(), any(), any());
            order.verify(dagNodePerItemTraversalService, times(1)).parseSyncResponseHttp(any(), any(), any());
            order.verify(dagNodePerItemTraversalService, times(1)).filterHttp(any(), any(), any());
            order.verify(dagNodePerItemTraversalService, times(1)).isSyncCompleteHttp(any(), any(), any());
        }


        @Test
        public void testWhenResponseIsInValidThenHandleInValidResponseIsCalled() throws Exception {

            HttpRequestResponse httpRequestResponse = mockFacadeHttpRequestResponse.build();
            StepDataBeanMapping stepDataBeanMapping = traverserMockUtility.mockStepDataBeanMapping();
            DagNode dagNode = new DagNode("dummy dag node");
            ObjectNode parentNode = objectMapper.createObjectNode();

            DagTraversalService.TraverseAction traverseAction = new DagTraversalService.TraverseAction();

            AbstractStep mockedAbstractStep = httpAbstractStepFacade.abstractStep(TestSingleApplicationStep.class)
                    .shouldProceedWithParentObject(true)
                    .startSync(httpRequestResponse)
                    .isValidResponseWithHttp(false)
                    .handleInvalidResponseWithHttp(traverseAction)
                    .parseSyncResponseWithHttp(stepDataBeanMapping)
                    .isSyncCompleteWithHttp(true)
                    .build();

            DagNodePerItemTraversalService dagNodePerItemTraversalService = dagNodePerItemTraversalFacade.abstractStep(mockedAbstractStep)
                    .parentNode(parentNode)
                    .build();

            doCallRealMethod().when(dagNodePerItemTraversalService).traverse();
            doCallRealMethod().when(dagNodePerItemTraversalService).ShouldProceedWithParentObjectHttp(any(), any());
            doCallRealMethod().when(dagNodePerItemTraversalService).setupHttp(any(), any(), any());
            doCallRealMethod().when(dagNodePerItemTraversalService).startSyncHttp(any(), any());
            doCallRealMethod().when(dagNodePerItemTraversalService).isValidResponseHttp(any(), any(), any());
            doCallRealMethod().when(dagNodePerItemTraversalService).handleInValidResponseHttp(any(), any(), any());
            doCallRealMethod().when(dagNodePerItemTraversalService).parseSyncResponseHttp(any(), any(), any());
            doCallRealMethod().when(dagNodePerItemTraversalService).filterHttp(any(), any(), any());
            doCallRealMethod().when(dagNodePerItemTraversalService).isSyncCompleteNonHttp(any(), any(), any());

            dagNodePerItemTraversalService.traverse();

            InOrder order = inOrder(dagNodePerItemTraversalService);
            order.verify(dagNodePerItemTraversalService, times(1)).ShouldProceedWithParentObjectHttp(any(), any());
            order.verify(dagNodePerItemTraversalService, times(1)).setupHttp(any(), any(), any());
            order.verify(dagNodePerItemTraversalService, times(1)).startSyncHttp(any(), any());
            order.verify(dagNodePerItemTraversalService, times(1)).isValidResponseHttp(any(), any(), any());
            order.verify(dagNodePerItemTraversalService, times(1)).handleInValidResponseHttp(any(), any(), any());
            order.verify(dagNodePerItemTraversalService, times(0)).parseSyncResponseHttp(any(), any(), any());
            order.verify(dagNodePerItemTraversalService, times(0)).filterHttp(any(), any(), any());
            order.verify(dagNodePerItemTraversalService, times(0)).isSyncCompleteHttp(any(), any(), any());
        }

        @Test
        public void testWhenResponseIsInValidAndTraverseActionIsAbortTransactionThenTransactionAborted() throws Exception {

            HttpRequestResponse httpRequestResponse = mockFacadeHttpRequestResponse.build();
            StepDataBeanMapping stepDataBeanMapping = traverserMockUtility.mockStepDataBeanMapping();
            DagNode dagNode = new DagNode("dummy dag node");
            ObjectNode parentNode = objectMapper.createObjectNode();

            DagTraversalService.TraverseAction traverseAction = new DagTraversalService.TraverseAction();
            traverseAction.abortTransaction();

            AbstractStep mockedAbstractStep = httpAbstractStepFacade.abstractStep(TestSingleApplicationStep.class)
                    .shouldProceedWithParentObject(true)
                    .startSync(httpRequestResponse)
                    .isValidResponseWithHttp(false, true)
                    .handleInvalidResponseWithHttp(traverseAction)
                    .parseSyncResponseWithHttp(stepDataBeanMapping)
                    .isSyncCompleteWithHttp(true)
                    .build();

            DagNodePerItemTraversalService dagNodePerItemTraversalService = dagNodePerItemTraversalFacade.abstractStep(mockedAbstractStep)
                    .parentNode(parentNode)
                    .build();

            doCallRealMethod().when(dagNodePerItemTraversalService).traverse();
            doCallRealMethod().when(dagNodePerItemTraversalService).ShouldProceedWithParentObjectHttp(any(), any());
            doCallRealMethod().when(dagNodePerItemTraversalService).setupHttp(any(), any(), any());
            doCallRealMethod().when(dagNodePerItemTraversalService).startSyncHttp(any(), any());
            doCallRealMethod().when(dagNodePerItemTraversalService).isValidResponseHttp(any(), any(), any());
            doCallRealMethod().when(dagNodePerItemTraversalService).handleInValidResponseHttp(any(), any(), any());
            doCallRealMethod().when(dagNodePerItemTraversalService).parseSyncResponseHttp(any(), any(), any());
            doCallRealMethod().when(dagNodePerItemTraversalService).filterHttp(any(), any(), any());
            doCallRealMethod().when(dagNodePerItemTraversalService).isSyncCompleteHttp(any(), any(), any());
            dagNodePerItemTraversalService.traverse();

            dagNodePerItemTraversalService.traverse();

            verify(dagNodePerItemTraversalService, times(1)).handleAbortTransactionActionHttp();
        }

        //
        @Test
        public void testWhenResponseIsInValidAndTraverseActionIsHoldAndReTryThenSyncIsHoldForGiveMsAndReTry() throws Exception {

            HttpRequestResponse httpRequestResponse = mockFacadeHttpRequestResponse.build();
            StepDataBeanMapping stepDataBeanMapping = traverserMockUtility.mockStepDataBeanMapping();
            DagNode dagNode = new DagNode("dummy dag node");
            ObjectNode parentNode = objectMapper.createObjectNode();

            DagTraversalService.TraverseAction traverseAction = new DagTraversalService.TraverseAction();
            traverseAction.holdAndReTry(1, TimeUnit.MILLISECONDS);

            AbstractStep mockedAbstractStep = httpAbstractStepFacade.abstractStep(TestSingleApplicationStep.class)
                    .shouldProceedWithParentObject(true)
                    .startSync(httpRequestResponse)
                    .isValidResponseWithHttp(false, true)
                    .handleInvalidResponseWithHttp(traverseAction)
                    .parseSyncResponseWithHttp(stepDataBeanMapping)
                    .isSyncCompleteWithHttp(true)
                    .build();

            DagNodePerItemTraversalService dagNodePerItemTraversalService = dagNodePerItemTraversalFacade.abstractStep(mockedAbstractStep)
                    .parentNode(parentNode)
                    .build();

            doCallRealMethod().when(dagNodePerItemTraversalService).traverse();
            doCallRealMethod().when(dagNodePerItemTraversalService).ShouldProceedWithParentObjectHttp(any(), any());
            doCallRealMethod().when(dagNodePerItemTraversalService).setupHttp(any(), any(), any());
            doCallRealMethod().when(dagNodePerItemTraversalService).startSyncHttp(any(), any());
            doCallRealMethod().when(dagNodePerItemTraversalService).isValidResponseHttp(any(), any(), any());
            doCallRealMethod().when(dagNodePerItemTraversalService).handleInValidResponseHttp(any(), any(), any());
            doCallRealMethod().when(dagNodePerItemTraversalService).parseSyncResponseHttp(any(), any(), any());
            doCallRealMethod().when(dagNodePerItemTraversalService).filterHttp(any(), any(), any());
            doCallRealMethod().when(dagNodePerItemTraversalService).isSyncCompleteHttp(any(), any(), any());

            dagNodePerItemTraversalService.traverse();

            verify(dagNodePerItemTraversalService, times(1)).handleHoldAndRetryActionHttp(traverseAction);
        }

        //
        @Test
        public void testWhenResponseIsInValidAndTraverseActionIsRetryWithNewRequestThenSyncIsReTryWithNewRequest() throws Exception {

            HttpRequestResponse httpRequestResponse = mockFacadeHttpRequestResponse.build();
            StepDataBeanMapping stepDataBeanMapping = traverserMockUtility.mockStepDataBeanMapping();
            DagNode dagNode = new DagNode("dummy dag node");
            ObjectNode parentNode = objectMapper.createObjectNode();

            DagTraversalService.TraverseAction traverseAction = new DagTraversalService.TraverseAction();
            traverseAction.retryWithNewRequest(mockFacadeHttpRequestResponse.configure().build());

            AbstractStep mockedAbstractStep = httpAbstractStepFacade.abstractStep(TestSingleApplicationStep.class)
                    .shouldProceedWithParentObject(true)
                    .startSync(httpRequestResponse)
                    .isValidResponseWithHttp(false, true)
                    .handleInvalidResponseWithHttp(traverseAction)
                    .parseSyncResponseWithHttp(stepDataBeanMapping)
                    .isSyncCompleteWithHttp(true)
                    .build();

            DagNodePerItemTraversalService dagNodePerItemTraversalService = dagNodePerItemTraversalFacade.abstractStep(mockedAbstractStep)
                    .parentNode(parentNode)
                    .build();

            doCallRealMethod().when(dagNodePerItemTraversalService).traverse();
            doCallRealMethod().when(dagNodePerItemTraversalService).ShouldProceedWithParentObjectHttp(any(), any());
            doCallRealMethod().when(dagNodePerItemTraversalService).setupHttp(any(), any(), any());
            doCallRealMethod().when(dagNodePerItemTraversalService).startSyncHttp(any(), any());
            doCallRealMethod().when(dagNodePerItemTraversalService).isValidResponseHttp(any(), any(), any());
            doCallRealMethod().when(dagNodePerItemTraversalService).handleInValidResponseHttp(any(), any(), any());
            doCallRealMethod().when(dagNodePerItemTraversalService).parseSyncResponseHttp(any(), any(), any());
            doCallRealMethod().when(dagNodePerItemTraversalService).filterHttp(any(), any(), any());
            doCallRealMethod().when(dagNodePerItemTraversalService).isSyncCompleteHttp(any(), any(), any());

            dagNodePerItemTraversalService.traverse();

            verify(dagNodePerItemTraversalService, times(1)).handleRetryWithNewRequest(traverseAction);
        }

        //
        @Test
        public void testWhenResponseIsInValidAndTraverseActionIsAbortCurrentParentThenSyncIsAbortCurrentParent() throws Exception {

            HttpRequestResponse httpRequestResponse = mockFacadeHttpRequestResponse.build();
            StepDataBeanMapping stepDataBeanMapping = traverserMockUtility.mockStepDataBeanMapping();
            DagNode dagNode = new DagNode("dummy dag node");
            ObjectNode parentNode = objectMapper.createObjectNode();

            DagTraversalService.TraverseAction traverseAction = new DagTraversalService.TraverseAction();
            traverseAction.abortCurrentParentAndContinueWithNextParentInstance();

            AbstractStep mockedAbstractStep = httpAbstractStepFacade.abstractStep(TestSingleApplicationStep.class)
                    .shouldProceedWithParentObject(true)
                    .startSync(httpRequestResponse)
                    .isValidResponseWithHttp(false, true)
                    .handleInvalidResponseWithHttp(traverseAction)
                    .parseSyncResponseWithHttp(stepDataBeanMapping)
                    .isSyncCompleteWithHttp(true)
                    .build();

            DagNodePerItemTraversalService dagNodePerItemTraversalService = dagNodePerItemTraversalFacade.abstractStep(mockedAbstractStep)
                    .parentNode(parentNode)
                    .build();

            doCallRealMethod().when(dagNodePerItemTraversalService).traverse();
            doCallRealMethod().when(dagNodePerItemTraversalService).ShouldProceedWithParentObjectHttp(any(), any());
            doCallRealMethod().when(dagNodePerItemTraversalService).setupHttp(any(), any(), any());
            doCallRealMethod().when(dagNodePerItemTraversalService).startSyncHttp(any(), any());
            doCallRealMethod().when(dagNodePerItemTraversalService).isValidResponseHttp(any(), any(), any());
            doCallRealMethod().when(dagNodePerItemTraversalService).handleInValidResponseHttp(any(), any(), any());
            doCallRealMethod().when(dagNodePerItemTraversalService).parseSyncResponseHttp(any(), any(), any());
            doCallRealMethod().when(dagNodePerItemTraversalService).filterHttp(any(), any(), any());
            doCallRealMethod().when(dagNodePerItemTraversalService).isSyncCompleteHttp(any(), any(), any());

            dagNodePerItemTraversalService.traverse();

            verify(dagNodePerItemTraversalService, times(1)).abortCurrentParentAndReTryWithNewParentHttp();
        }
    }


    @Nested
    class WhenStepIsNonHttpAbstractStep{
//
        @Test
        public void testWhenNonHttpStepsExecutionInRightOrder() throws Exception {

            RequestResponseContainer requestResponseContainer = traverserMockUtility.mockNonHttpRequestResponse();
            Object nonHttpRequest = "mkdir dir";
            Object nonHttpResponse = "true";

            requestResponseContainer.setRequest(nonHttpRequest);
            requestResponseContainer.setResponse(nonHttpResponse);

            StepDataBeanMapping stepDataBeanMapping = traverserMockUtility.mockStepDataBeanMapping();
            DagNode dagNode = new DagNode("dummy dag node");
            ObjectNode parentNode = objectMapper.createObjectNode();


            AbstractStep mockedAbstractStep = nonHttpAbstractStepFacade.abstractStepClass(TestSingleNonHttpApplicationStep.class)
                    .shouldProceedWithParentObjectNonHttp(true)
                    .startSyncNonHttp(requestResponseContainer)
                    .executeNonHttp(requestResponseContainer)
                    .isValidResponseNonHttp(true)
                    .parseSyncResponseNonHttp(stepDataBeanMapping)
                    .isSyncCompleteNonHttp(true)
                    .build();

            DagNodePerItemTraversalService mockDagNodePerItemTraversalService = dagNodePerItemTraversalFacade.abstractStep(mockedAbstractStep)
                    .parentNode(parentNode)
                    .isHttpBased(false)
                    .build();

            doCallRealMethod().when(mockDagNodePerItemTraversalService).traverse();
            doCallRealMethod().when(mockDagNodePerItemTraversalService).ShouldProceedWithParentObjectNonHttp(any(), any());
            doCallRealMethod().when(mockDagNodePerItemTraversalService).setupNonHttp(any(), any(), any());
            doCallRealMethod().when(mockDagNodePerItemTraversalService).startSyncNonHttp(any(), any());
            doCallRealMethod().when(mockDagNodePerItemTraversalService).isValidResponseNonHttp(any(), any(), any());
            doCallRealMethod().when(mockDagNodePerItemTraversalService).parseSyncResponseNonHttp(any(), any(), any());
            doCallRealMethod().when(mockDagNodePerItemTraversalService).filterNonHttp(any(), any(), any());
            doCallRealMethod().when(mockDagNodePerItemTraversalService).isSyncCompleteNonHttp(any(), any(), any());
            doCallRealMethod().when(mockDagNodePerItemTraversalService).traverse();

            mockDagNodePerItemTraversalService.traverse();

            InOrder order = inOrder(mockDagNodePerItemTraversalService);
            order.verify(mockDagNodePerItemTraversalService, times(1)).ShouldProceedWithParentObjectNonHttp(any(), any());
            order.verify(mockDagNodePerItemTraversalService, times(1)).setupNonHttp(any(), any(), any());
            order.verify(mockDagNodePerItemTraversalService, times(1)).startSyncNonHttp(any(), any());
            order.verify(mockDagNodePerItemTraversalService, times(1)).executeNonHttp(any(), any());
            order.verify(mockDagNodePerItemTraversalService, times(1)).isValidResponseNonHttp(any(), any(), any());
            order.verify(mockDagNodePerItemTraversalService, times(0)).handleInValidResponseNonHttp(any(), any(), any());
            order.verify(mockDagNodePerItemTraversalService, times(1)).parseSyncResponseNonHttp(any(), any(), any());
            order.verify(mockDagNodePerItemTraversalService, times(1)).filterNonHttp(any(), any(), any());
            order.verify(mockDagNodePerItemTraversalService, times(1)).isSyncCompleteNonHttp(any(), any(), any());
        }
//
        @Test
        public void testWhenResponseIsInValidThenHandleInValidResponseIsCalled() throws Exception {

            RequestResponseContainer requestResponseContainer = traverserMockUtility.mockNonHttpRequestResponse();
            Object nonHttpRequest = "mkdir dir";
            Object nonHttpResponse = "true";

            requestResponseContainer.setRequest(nonHttpRequest);
            requestResponseContainer.setResponse(nonHttpResponse);

            StepDataBeanMapping stepDataBeanMapping = traverserMockUtility.mockStepDataBeanMapping();
            DagTraversalService.TraverseAction traverseAction = traverserMockUtility.mockTraverseAction();

            DagNode dagNode = new DagNode("dummy dag node");
            ObjectNode parentNode = objectMapper.createObjectNode();


            AbstractStep mockedAbstractStep = nonHttpAbstractStepFacade.abstractStepClass(TestSingleNonHttpApplicationStep.class)
                    .shouldProceedWithParentObjectNonHttp(true)
                    .startSyncNonHttp(requestResponseContainer)
                    .isValidResponseNonHttp(false)
                    .handleInValidResponseNonHttp(traverseAction)
                    .parseSyncResponseNonHttp(stepDataBeanMapping)
                    .isSyncCompleteNonHttp(true)
                    .build();

            DagNodePerItemTraversalService mockDagNodePerItemTraversalService = dagNodePerItemTraversalFacade.abstractStep(mockedAbstractStep)
                    .parentNode(parentNode)
                    .isHttpBased(false)
                    .build();

            doCallRealMethod().when(mockDagNodePerItemTraversalService).traverse();
            doCallRealMethod().when(mockDagNodePerItemTraversalService).ShouldProceedWithParentObjectNonHttp(any(), any());
            doCallRealMethod().when(mockDagNodePerItemTraversalService).setupNonHttp(any(), any(), any());
            doCallRealMethod().when(mockDagNodePerItemTraversalService).startSyncNonHttp(any(), any());
            doCallRealMethod().when(mockDagNodePerItemTraversalService).isValidResponseNonHttp(any(), any(), any());
            doCallRealMethod().when(mockDagNodePerItemTraversalService).parseSyncResponseNonHttp(any(), any(), any());
            doCallRealMethod().when(mockDagNodePerItemTraversalService).filterNonHttp(any(), any(), any());
            doCallRealMethod().when(mockDagNodePerItemTraversalService).isSyncCompleteNonHttp(any(), any(), any());
            doCallRealMethod().when(mockDagNodePerItemTraversalService).traverse();

            mockDagNodePerItemTraversalService.traverse();

            InOrder order = inOrder(mockDagNodePerItemTraversalService);
            order.verify(mockDagNodePerItemTraversalService, times(1)).ShouldProceedWithParentObjectNonHttp(any(), any());
            order.verify(mockDagNodePerItemTraversalService, times(1)).setupNonHttp(any(), any(), any());
            order.verify(mockDagNodePerItemTraversalService, times(1)).startSyncNonHttp(any(), any());
            order.verify(mockDagNodePerItemTraversalService, times(1)).isValidResponseNonHttp(any(), any(), any());
            order.verify(mockDagNodePerItemTraversalService, times(1)).handleInValidResponseNonHttp(any(), any(), any());
            order.verify(mockDagNodePerItemTraversalService, times(0)).parseSyncResponseNonHttp(any(), any(), any());
            order.verify(mockDagNodePerItemTraversalService, times(0)).filterNonHttp(any(), any(), any());
            order.verify(mockDagNodePerItemTraversalService, times(0)).isSyncCompleteNonHttp(any(), any(), any());
        }
//
        @Test
        public void testWhenResponseIsInValidAndTraverseActionIsAbortTransactionThenTransactionAborted() throws Exception {

            RequestResponseContainer requestResponseContainer = traverserMockUtility.mockNonHttpRequestResponse();
            Object nonHttpRequest = "mkdir dir";
            Object nonHttpResponse = "true";

            requestResponseContainer.setRequest(nonHttpRequest);
            requestResponseContainer.setResponse(nonHttpResponse);

            StepDataBeanMapping stepDataBeanMapping = traverserMockUtility.mockStepDataBeanMapping();
            DagTraversalService.TraverseAction traverseAction = traverserMockUtility.spyTraverseAction();
            traverseAction.abortTransaction();

            DagNode dagNode = new DagNode("dummy dag node");
            ObjectNode parentNode = objectMapper.createObjectNode();


            AbstractStep mockedAbstractStep = nonHttpAbstractStepFacade.abstractStepClass(TestSingleNonHttpApplicationStep.class)
                    .shouldProceedWithParentObjectNonHttp(true)
                    .startSyncNonHttp(requestResponseContainer)
                    .isValidResponseNonHttp(false)
                    .handleInValidResponseNonHttp(traverseAction)
                    .parseSyncResponseNonHttp(stepDataBeanMapping)
                    .isSyncCompleteNonHttp(true)
                    .build();

            DagNodePerItemTraversalService mockDagNodePerItemTraversalService = dagNodePerItemTraversalFacade.abstractStep(mockedAbstractStep)
                    .parentNode(parentNode)
                    .isHttpBased(false)
                    .build();

            doCallRealMethod().when(mockDagNodePerItemTraversalService).traverse();
            doCallRealMethod().when(mockDagNodePerItemTraversalService).ShouldProceedWithParentObjectNonHttp(any(), any());
            doCallRealMethod().when(mockDagNodePerItemTraversalService).setupNonHttp(any(), any(), any());
            doCallRealMethod().when(mockDagNodePerItemTraversalService).startSyncNonHttp(any(), any());
            doCallRealMethod().when(mockDagNodePerItemTraversalService).isValidResponseNonHttp(any(), any(), any());
            doCallRealMethod().when(mockDagNodePerItemTraversalService).handleInValidResponseNonHttp(any(), any(), any());
            doCallRealMethod().when(mockDagNodePerItemTraversalService).handleAbortTransactionActionNonHttp();
            doCallRealMethod().when(mockDagNodePerItemTraversalService).parseSyncResponseNonHttp(any(), any(), any());
            doCallRealMethod().when(mockDagNodePerItemTraversalService).filterNonHttp(any(), any(), any());
            doCallRealMethod().when(mockDagNodePerItemTraversalService).isSyncCompleteNonHttp(any(), any(), any());
            doCallRealMethod().when(mockDagNodePerItemTraversalService).traverse();

            mockDagNodePerItemTraversalService.traverse();

            InOrder order = inOrder(mockDagNodePerItemTraversalService);
            order.verify(mockDagNodePerItemTraversalService, times(1)).handleAbortTransactionActionNonHttp();
            order.verify(mockDagNodePerItemTraversalService, times(0)).parseSyncResponseNonHttp(any(), any(), any());
            order.verify(mockDagNodePerItemTraversalService, times(0)).filterNonHttp(any(), any(), any());
            order.verify(mockDagNodePerItemTraversalService, times(0)).isSyncCompleteNonHttp(any(), any(), any());
        }
//
        @Test
        public void testWhenResponseIsInValidAndTraverseActionIsHoldAndReTryThenSyncIsHoldForGiveMsAndReTry() throws Exception {

            RequestResponseContainer requestResponseContainer = traverserMockUtility.mockNonHttpRequestResponse();
            Object nonHttpRequest = "mkdir dir";
            Object nonHttpResponse = "true";

            requestResponseContainer.setRequest(nonHttpRequest);
            requestResponseContainer.setResponse(nonHttpResponse);

            StepDataBeanMapping stepDataBeanMapping = traverserMockUtility.mockStepDataBeanMapping();
            DagTraversalService.TraverseAction traverseAction = traverserMockUtility.spyTraverseAction();
            traverseAction.holdAndReTry(1, TimeUnit.MILLISECONDS);

            DagNode dagNode = new DagNode("dummy dag node");
            ObjectNode parentNode = objectMapper.createObjectNode();


            AbstractStep mockedAbstractStep = nonHttpAbstractStepFacade.abstractStepClass(TestSingleNonHttpApplicationStep.class)
                    .shouldProceedWithParentObjectNonHttp(true)
                    .startSyncNonHttp(requestResponseContainer)
                    .isValidResponseNonHttp(false, true)
                    .handleInValidResponseNonHttp(traverseAction)
                    .parseSyncResponseNonHttp(stepDataBeanMapping)
                    .isSyncCompleteNonHttp(true)
                    .build();

            DagNodePerItemTraversalService mockDagNodePerItemTraversalService = dagNodePerItemTraversalFacade.abstractStep(mockedAbstractStep)
                    .parentNode(parentNode)
                    .isHttpBased(true)
                    .build();

            doCallRealMethod().when(mockDagNodePerItemTraversalService).traverse();
            doCallRealMethod().when(mockDagNodePerItemTraversalService).ShouldProceedWithParentObjectNonHttp(any(), any());
            doCallRealMethod().when(mockDagNodePerItemTraversalService).setupNonHttp(any(), any(), any());
            doCallRealMethod().when(mockDagNodePerItemTraversalService).startSyncNonHttp(any(), any());
            doCallRealMethod().when(mockDagNodePerItemTraversalService).isValidResponseNonHttp(any(), any(), any());
            doCallRealMethod().when(mockDagNodePerItemTraversalService).handleInValidResponseNonHttp(any(), any(), any());
            doCallRealMethod().when(mockDagNodePerItemTraversalService).handleHoldAndRetryActionNonHttp(any());
            doCallRealMethod().when(mockDagNodePerItemTraversalService).parseSyncResponseNonHttp(any(), any(), any());
            doCallRealMethod().when(mockDagNodePerItemTraversalService).filterNonHttp(any(), any(), any());
            doCallRealMethod().when(mockDagNodePerItemTraversalService).isSyncCompleteNonHttp(any(), any(), any());
            doCallRealMethod().when(mockDagNodePerItemTraversalService).traverse();

            mockDagNodePerItemTraversalService.traverse();
            InOrder order = inOrder(mockDagNodePerItemTraversalService);
            order.verify(mockDagNodePerItemTraversalService, times(1)).handleHoldAndRetryActionNonHttp(traverseAction);
            order.verify(mockDagNodePerItemTraversalService, times(1)).parseSyncResponseNonHttp(any(), any(), any());
            order.verify(mockDagNodePerItemTraversalService, times(1)).filterNonHttp(any(), any(), any());
            order.verify(mockDagNodePerItemTraversalService, times(1)).isSyncCompleteNonHttp(any(), any(), any());
        }
//
        @Test
        public void testWhenResponseIsInValidAndTraverseActionIsRetryWithNewRequestThenSyncIsReTryWithNewRequest() throws Exception {

            RequestResponseContainer requestResponseContainer = traverserMockUtility.mockNonHttpRequestResponse();
            Object nonHttpRequest = "mkdir dir";
            Object nonHttpResponse = "true";

            requestResponseContainer.setRequest(nonHttpRequest);
            requestResponseContainer.setResponse(nonHttpResponse);


            StepDataBeanMapping stepDataBeanMapping = traverserMockUtility.mockStepDataBeanMapping();
            DagTraversalService.TraverseAction traverseAction = traverserMockUtility.spyTraverseAction();
            RequestResponseContainer newNonHttpRequestResponse = traverserMockUtility.mockNonHttpRequestResponse();
            traverseAction.retryWithNewRequestContainer(newNonHttpRequestResponse);

            DagNode dagNode = new DagNode("dummy dag node");
            ObjectNode parentNode = objectMapper.createObjectNode();


            AbstractStep mockedAbstractStep = nonHttpAbstractStepFacade.abstractStepClass(TestSingleNonHttpApplicationStep.class)
                    .shouldProceedWithParentObjectNonHttp(true)
                    .startSyncNonHttp(requestResponseContainer)
                    .isValidResponseNonHttp(false, true)
                    .handleInValidResponseNonHttp(traverseAction)
                    .parseSyncResponseNonHttp(stepDataBeanMapping)
                    .isSyncCompleteNonHttp(true)
                    .build();

            DagNodePerItemTraversalService mockDagNodePerItemTraversalService = dagNodePerItemTraversalFacade.abstractStep(mockedAbstractStep)
                    .parentNode(parentNode)
                    .isHttpBased(true)
                    .build();

            doCallRealMethod().when(mockDagNodePerItemTraversalService).traverse();
            doCallRealMethod().when(mockDagNodePerItemTraversalService).ShouldProceedWithParentObjectNonHttp(any(), any());
            doCallRealMethod().when(mockDagNodePerItemTraversalService).setupNonHttp(any(), any(), any());
            doCallRealMethod().when(mockDagNodePerItemTraversalService).startSyncNonHttp(any(), any());
            doCallRealMethod().when(mockDagNodePerItemTraversalService).isValidResponseNonHttp(any(), any(), any());
            doCallRealMethod().when(mockDagNodePerItemTraversalService).handleInValidResponseNonHttp(any(), any(), any());
            doCallRealMethod().when(mockDagNodePerItemTraversalService).handleRetryWithNewRequestNonHttp(any());
            doCallRealMethod().when(mockDagNodePerItemTraversalService).parseSyncResponseNonHttp(any(), any(), any());
            doCallRealMethod().when(mockDagNodePerItemTraversalService).filterNonHttp(any(), any(), any());
            doCallRealMethod().when(mockDagNodePerItemTraversalService).isSyncCompleteNonHttp(any(), any(), any());
            doCallRealMethod().when(mockDagNodePerItemTraversalService).traverse();

            mockDagNodePerItemTraversalService.traverse();

            InOrder order = inOrder(mockDagNodePerItemTraversalService);
            order.verify(mockDagNodePerItemTraversalService, times(1)).handleRetryWithNewRequestNonHttp(traverseAction);
            order.verify(mockDagNodePerItemTraversalService, times(1)).parseSyncResponseNonHttp(any(), any(), any());
            order.verify(mockDagNodePerItemTraversalService, times(1)).filterNonHttp(any(), any(), any());
            order.verify(mockDagNodePerItemTraversalService, times(1)).isSyncCompleteNonHttp(any(), any(), any());
        }
//
        @Test
        public void testWhenResponseIsInValidAndTraverseActionIsAbortCurrentParentThenSyncIsAbortCurrentParent() throws Exception {

            RequestResponseContainer requestResponseContainer = traverserMockUtility.mockNonHttpRequestResponse();
            Object nonHttpRequest = "mkdir dir";
            Object nonHttpResponse = "true";

            requestResponseContainer.setRequest(nonHttpRequest);
            requestResponseContainer.setResponse(nonHttpResponse);

            StepDataBeanMapping stepDataBeanMapping = traverserMockUtility.mockStepDataBeanMapping();
            DagTraversalService.TraverseAction traverseAction = traverserMockUtility.spyTraverseAction();
            traverseAction.abortCurrentParentAndContinueWithNextParentInstance();

            DagNode dagNode = new DagNode("dummy dag node");
            ObjectNode parentNode = objectMapper.createObjectNode();



            AbstractStep mockedAbstractStep = nonHttpAbstractStepFacade.abstractStepClass(TestSingleNonHttpApplicationStep.class)
                    .shouldProceedWithParentObjectNonHttp(true)
                    .startSyncNonHttp(requestResponseContainer)
                    .isValidResponseNonHttp(false, true)
                    .handleInValidResponseNonHttp(traverseAction)
                    .parseSyncResponseNonHttp(stepDataBeanMapping)
                    .isSyncCompleteNonHttp(true)
                    .build();

            DagNodePerItemTraversalService mockDagNodePerItemTraversalService = dagNodePerItemTraversalFacade.abstractStep(mockedAbstractStep)
                    .parentNode(parentNode)
                    .isHttpBased(true)
                    .build();

            doCallRealMethod().when(mockDagNodePerItemTraversalService).traverse();
            doCallRealMethod().when(mockDagNodePerItemTraversalService).ShouldProceedWithParentObjectNonHttp(any(), any());
            doCallRealMethod().when(mockDagNodePerItemTraversalService).setupNonHttp(any(), any(), any());
            doCallRealMethod().when(mockDagNodePerItemTraversalService).startSyncNonHttp(any(), any());
            doCallRealMethod().when(mockDagNodePerItemTraversalService).isValidResponseNonHttp(any(), any(), any());
            doCallRealMethod().when(mockDagNodePerItemTraversalService).handleInValidResponseNonHttp(any(), any(), any());
            doCallRealMethod().when(mockDagNodePerItemTraversalService).abortCurrentParentAndReTryWithNewParentNonHttp();
            doCallRealMethod().when(mockDagNodePerItemTraversalService).parseSyncResponseNonHttp(any(), any(), any());
            doCallRealMethod().when(mockDagNodePerItemTraversalService).filterNonHttp(any(), any(), any());
            doCallRealMethod().when(mockDagNodePerItemTraversalService).isSyncCompleteNonHttp(any(), any(), any());
            doCallRealMethod().when(mockDagNodePerItemTraversalService).traverse();

            mockDagNodePerItemTraversalService.traverse();

            InOrder order = inOrder(mockDagNodePerItemTraversalService);
            order.verify(mockDagNodePerItemTraversalService, times(1)).abortCurrentParentAndReTryWithNewParentNonHttp();
            order.verify(mockDagNodePerItemTraversalService, times(0)).parseSyncResponseNonHttp(any(), any(), any());
            order.verify(mockDagNodePerItemTraversalService, times(0)).filterNonHttp(any(), any(), any());
            order.verify(mockDagNodePerItemTraversalService, times(0)).isSyncCompleteNonHttp(any(), any(), any());
        }
    }


    @Test
    public void testProcessIntoBeanMethod() throws Exception {
        DagNodePerItemTraversalService dagNodePerItemTraversalService = dagNodePerItemTraversalFacade
                .build();

        DagNode currentNode = mockFacadeDagNode.build();
        List<String> savedList = new ArrayList<>();
        doAnswer(new Answer() {
            @Override
            public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
                List<String> sList = invocationOnMock.getArgument(0);
                savedList.addAll(sList);
                return null;
            }
        }).when(currentNode).saveSyncResult(anyList());

        doCallRealMethod().when(dagNodePerItemTraversalService).processIntoBean(any(), any(), any(), anyBoolean());

        ArrayNode jNodeList = objectMapper.createArrayNode();
        ObjectNode objectNode = objectMapper.createObjectNode();

        objectNode.put("name", "Amit Aggarwal");
        objectNode.put("company", "freshworks");
        jNodeList.add(objectNode);

        dagNodePerItemTraversalService.processIntoBean(currentNode, jNodeList, SimpleBean.class, true);

        assertThat(savedList.size(), Matchers.is(1));
        assertThat(savedList, Matchers.hasItem(Matchers.containsString("Amit Aggarwal")));
        assertThat(savedList, Matchers.hasItem((Matchers.containsString("freshworks"))));
    }

    @Test
    public void testProcessIntoComplexBeanMethod() throws Exception {
        DagNodePerItemTraversalService dagNodePerItemTraversalService = dagNodePerItemTraversalFacade
                .build();

        DagNode currentNode = mockFacadeDagNode.build();
        List<String> savedList = new ArrayList<>();
        doAnswer(new Answer() {
            @Override
            public Void answer(InvocationOnMock invocationOnMock) throws Throwable {
                List<String> sList = invocationOnMock.getArgument(0);
                savedList.addAll(sList);
                return null;
            }
        }).when(currentNode).saveSyncResult(anyList());

        doCallRealMethod().when(dagNodePerItemTraversalService).processIntoBean(any(), any(), any(), anyBoolean());

        ArrayNode jNodeList = objectMapper.createArrayNode();
        ObjectNode objectNode = objectMapper.createObjectNode();

        objectNode.put("name", "Amit Aggarwal");
        objectNode.put("company", "freshworks");

        ObjectNode addressNode = objectMapper.createObjectNode();
        addressNode.put("city", "bangalore");
        addressNode.put("state", "karnataka");
        addressNode.put("country", "India");

        objectNode.set("address", addressNode);
        jNodeList.add(objectNode);

        dagNodePerItemTraversalService.processIntoBean(currentNode, jNodeList, ComplexBean.class, true);

        assertThat(savedList.size(), Matchers.is(1));
        assertThat(savedList, Matchers.hasItem(Matchers.containsString("Amit Aggarwal")));
        assertThat(savedList, Matchers.hasItem((Matchers.containsString("freshworks"))));
        assertThat(savedList, Matchers.hasItem((Matchers.containsString("bangalore"))));
        assertThat(savedList, Matchers.hasItem((Matchers.containsString("karnataka"))));
        assertThat(savedList, Matchers.hasItem((Matchers.containsString("India"))));
    }
}
