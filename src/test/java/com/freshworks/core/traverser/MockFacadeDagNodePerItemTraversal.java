package com.freshworks.core.traverser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.freshworks.core.MockFacadeInterface;
import com.freshworks.core.ReturnableMockTypeList;
import com.freshworks.core.shared.MockFacadeSyncServiceContainer;
import com.freshworks.core.shared.NamespaceService;
import com.freshworks.core.shared.SimpleMockUtility;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.infra.InfraDbQueue;
import com.freshworks.core.shared.infra.nitrite.MockFacadeNitritedbQueue;
import com.freshworks.core.shared.sync.SyncStatusService;
import com.freshworks.core.traverser.exception.StepFailedException;
import com.freshworks.core.traverser.net.MockFacadeRequestResponseContainer;
import com.freshworks.core.traverser.net.http.HttpRequestResponse;
import com.freshworks.core.traverser.net.http.MockFacadeHttpRequestResponse;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import io.github.bucket4j.Bucket;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URISyntaxException;
import java.util.concurrent.Phaser;
import java.util.concurrent.Semaphore;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * This facade is for testing DagNodePerItemTraversalService.
 */
@Component
public class MockFacadeDagNodePerItemTraversal implements MockFacadeInterface {

    @Autowired
    SimpleMockUtility traverserMockUtility;

    ObjectMapper objectMapper = new ObjectMapper();

    ReturnableMockTypeList<AbstractStep> abstractStep = new ReturnableMockTypeList<>();
    @Autowired
    MockFacadeHttpAbstractStep mockFacadeHttpAbstractStep;

    ReturnableMockTypeList<JsonNode> parentNodeData = new ReturnableMockTypeList<>();
    ReturnableMockTypeList<DagNode> parentNode = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<Phaser> waitUntilAllPerItemTraversalIsDonePhaser = new ReturnableMockTypeList<>();


    ReturnableMockTypeList<InfraDbQueue> processorQueue = new ReturnableMockTypeList<>();
    @Autowired
    private MockFacadeNitritedbQueue mockFacadeNitritedbQueue;

    ReturnableMockTypeList<TraverseConfigService> traverseConfigService = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<SyncStatusService> syncStatusService = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<SyncServiceContainer> syncServiceContainer = new ReturnableMockTypeList<>();
    @Autowired
    MockFacadeSyncServiceContainer mockFacadeSyncServiceContainer;


    ReturnableMockTypeList<ImmutableMap<String, String>> baggageMap = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<DagNode> currentNode = new ReturnableMockTypeList<>();
    @Autowired
    MockFacadeDagNode mockFacadeDagNode;

    ReturnableMockTypeList<Semaphore> limitNumberOfConcurrentPerItemTraversalSemaphore = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<Bucket> rateLimitBucket = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<Boolean> isHttpBased = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<Boolean> ShouldProceedWithParentObjectHttp = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<Boolean> ShouldProceedWithParentObjectNonHttp = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<HttpRequestResponse> startSyncHttp = new ReturnableMockTypeList<>();
    @Autowired
    MockFacadeHttpRequestResponse mockFacadeHttpRequestResponse;

    ReturnableMockTypeList<RequestResponseContainer> startSyncNonHttp = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<HttpRequestResponse> executeHttp = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<RequestResponseContainer> executeNonHttp = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<Boolean> isValidResponseHttp = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<Boolean> isValidResponseNonHttp = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<DagTraversalService.TraverseAction> handleInValidResponseHttp = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<DagTraversalService.TraverseAction> handleInValidResponseNonHttp = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<StepDataBeanMapping> parseSyncResponseHttp = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<StepDataBeanMapping> parseSyncResponseNonHttp = new ReturnableMockTypeList<>();


    ReturnableMockTypeList<Boolean> filterHttp = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<Boolean> filterNonHttp = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<Optional<Boolean>> isSyncCompleteHttp =  new ReturnableMockTypeList<>();

    ReturnableMockTypeList<HttpRequestResponse> getNextSyncRequest = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<Boolean> isSyncCompleteNonHttp = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<Boolean> handleAbortTransactionActionHttp = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<Boolean> handleAbortTransactionActionNonHttp = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<Long> handleHoldAndRetryActionHttp = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<Long> handleHoldAndRetryActionNonHttp = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<HttpRequestResponse> handleRetryWithNewRequest = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<RequestResponseContainer> handleRetryWithNewRequestNonHttp = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<Boolean> abortCurrentParentAndReTryWithNewParentHttp = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<Boolean> abortCurrentParentAndReTryWithNewParentNonHttp = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<NamespaceService> namespace = new ReturnableMockTypeList<>();

    @Autowired
    private SimpleMockUtility simpleMockUtility;
    @Autowired
    private MockFacadeRequestResponseContainer mockFacadeRequestResponseContainer;


    public MockFacadeDagNodePerItemTraversal configure() throws Exception {

        reset();

        abstractStep.add(mockFacadeHttpAbstractStep.configure().build());

        ObjectNode objectNode = objectMapper.createObjectNode();
        objectNode.put("parent_key", "parent_value");
        parentNodeData.add(objectNode);

        waitUntilAllPerItemTraversalIsDonePhaser.add(simpleMockUtility.mockPhaser());
        processorQueue.add(mockFacadeNitritedbQueue.configure().build());

        traverseConfigService.add(simpleMockUtility.mockTraverseConfigService());

        syncStatusService.add(simpleMockUtility.mockSyncStatusService());

        baggageMap.add(simpleMockUtility.mockImmutableMap());

        namespace.add(simpleMockUtility.mockNamespace("dummy_namespace"));

        parentNode.add(mockFacadeDagNode.configure().build());

        currentNode.add(mockFacadeDagNode.configure().build());

        limitNumberOfConcurrentPerItemTraversalSemaphore.add(simpleMockUtility.mockSemaphore());

        rateLimitBucket.add(simpleMockUtility.mockBucket());

        isHttpBased.add(true);

        ShouldProceedWithParentObjectHttp.add(true);
        ShouldProceedWithParentObjectNonHttp.add(true);

        startSyncHttp.add(mockFacadeHttpRequestResponse.configure().build());
        startSyncNonHttp.add(mockFacadeRequestResponseContainer.configure().build());

        executeHttp.add(mockFacadeHttpRequestResponse.configure().build());
        executeNonHttp.add(mockFacadeRequestResponseContainer.configure().build());

        isValidResponseHttp.add(true);
        isValidResponseNonHttp.add(true);

        isValidResponseHttp.add(true);
        isValidResponseNonHttp.add(true);

        handleInValidResponseHttp.add(new DagTraversalService.TraverseAction());
        handleInValidResponseNonHttp.add(new DagTraversalService.TraverseAction());

        StepDataBeanMapping stepDataBeanMapping = new StepDataBeanMapping();
        parseSyncResponseHttp.add(stepDataBeanMapping);
        parseSyncResponseNonHttp.add(stepDataBeanMapping);

        isSyncCompleteHttp.add(Optional.fromNullable(true));
        isSyncCompleteNonHttp.add(true);

        getNextSyncRequest.add(mockFacadeHttpRequestResponse.configure().build());

        syncServiceContainer.add(mockFacadeSyncServiceContainer.configure().build());
        syncServiceContainer.next().add(namespace.next());

        handleAbortTransactionActionHttp.add(false);
        handleAbortTransactionActionNonHttp.add(false);

        handleHoldAndRetryActionHttp.add(1L);
        handleHoldAndRetryActionNonHttp.add(1L);

        handleRetryWithNewRequest.add(mockFacadeHttpRequestResponse.configure().build());
        handleRetryWithNewRequestNonHttp.add(mockFacadeRequestResponseContainer.configure().build());

        abortCurrentParentAndReTryWithNewParentHttp.add(false);
        abortCurrentParentAndReTryWithNewParentNonHttp.add(false);

        return this;
    }


    public MockFacadeDagNodePerItemTraversal namespace(NamespaceService... namespace) {
        this.namespace.clear();
        this.namespace.add(namespace);
        return this;
    }

    public MockFacadeDagNodePerItemTraversal abstractStep(AbstractStep... abstractStep) throws StepFailedException {
        this.abstractStep.clear();
        this.abstractStep.add(abstractStep);
        return this;
    }

    public MockFacadeDagNodePerItemTraversal parentNode(JsonNode... parentNode) throws StepFailedException {
        this.parentNodeData.clear();
        this.parentNodeData.add(parentNode);
        return this;
    }

    public MockFacadeDagNodePerItemTraversal waitUntilAllPerItemTraversalIsDonePhaser(Phaser... waitUntilAllPerItemTraversalIsDonePhaser
    ) throws StepFailedException {
        this.waitUntilAllPerItemTraversalIsDonePhaser.clear();
        this.waitUntilAllPerItemTraversalIsDonePhaser.add(waitUntilAllPerItemTraversalIsDonePhaser);
        return this;
    }

    public MockFacadeDagNodePerItemTraversal traverseConfigService(TraverseConfigService... traverseConfigService) throws StepFailedException {
        this.traverseConfigService.clear();
        this.traverseConfigService.add(traverseConfigService);
        return this;
    }

    public MockFacadeDagNodePerItemTraversal processorQueue(InfraDbQueue... processorQueue) throws StepFailedException {
        this.processorQueue.clear();
        this.processorQueue.add(processorQueue);
        return this;
    }


    public MockFacadeDagNodePerItemTraversal syncStatusService(SyncStatusService... syncStatusService) throws StepFailedException {
        this.syncStatusService.clear();
        this.syncStatusService.add(syncStatusService);
        return this;
    }

    public MockFacadeDagNodePerItemTraversal syncServiceContainer(SyncServiceContainer... syncServiceContainer) throws StepFailedException {
        this.syncServiceContainer.clear();
        this.syncServiceContainer.add(syncServiceContainer);
        return this;
    }

    public MockFacadeDagNodePerItemTraversal baggageMap(ImmutableMap<String, String>... baggageMap) throws StepFailedException {
        this.baggageMap.clear();
        this.baggageMap.add(baggageMap);
        return this;
    }

    public MockFacadeDagNodePerItemTraversal currentNode(DagNode... currentNode) throws StepFailedException {
        this.currentNode.clear();
        this.currentNode.add(currentNode);
        return this;
    }

    public MockFacadeDagNodePerItemTraversal limitNumberOfConcurrentPerItemTraversalSemaphore(Semaphore... limitNumberOfConcurrentPerItemTraversalSemaphore) throws StepFailedException {
        this.limitNumberOfConcurrentPerItemTraversalSemaphore.clear();
        this.limitNumberOfConcurrentPerItemTraversalSemaphore.add(limitNumberOfConcurrentPerItemTraversalSemaphore);
        return this;
    }

    public MockFacadeDagNodePerItemTraversal rateLimitBucket(Bucket... rateLimitBucket) throws StepFailedException {
        this.rateLimitBucket.clear();
        this.rateLimitBucket.add(rateLimitBucket);
        return this;
    }

    public MockFacadeDagNodePerItemTraversal isHttpBased(Boolean... isHttpBased) throws StepFailedException {
        this.isHttpBased.clear();
        this.isHttpBased.add(isHttpBased);
        return this;
    }


    public MockFacadeDagNodePerItemTraversal ShouldProceedWithParentObjectHttp(Boolean... ShouldProceedWithParentObjectHttp) throws StepFailedException {
        this.ShouldProceedWithParentObjectHttp.clear();
        this.ShouldProceedWithParentObjectHttp.add(ShouldProceedWithParentObjectHttp);
        return this;
    }

    public MockFacadeDagNodePerItemTraversal ShouldProceedWithParentObjectNonHttp(Boolean... ShouldProceedWithParentObjectNonHttp) throws StepFailedException {
        this.ShouldProceedWithParentObjectNonHttp.clear();
        this.ShouldProceedWithParentObjectNonHttp.add(ShouldProceedWithParentObjectNonHttp);
        return this;
    }


    public MockFacadeDagNodePerItemTraversal startSyncHttp(HttpRequestResponse... startSyncHttp){
        this.startSyncHttp.clear();
        this.startSyncHttp.add(startSyncHttp);
        return this;
    }


    public MockFacadeDagNodePerItemTraversal startSyncNonHttp(RequestResponseContainer... startSyncNonHttp){
        this.startSyncNonHttp.clear();
        this.startSyncNonHttp.add(startSyncNonHttp);
        return this;
    }

    public MockFacadeDagNodePerItemTraversal executeHttp(HttpRequestResponse... executeHttp){
        this.executeHttp.clear();
        this.executeHttp.add(executeHttp);
        return this;
    }

    public MockFacadeDagNodePerItemTraversal executeNonHttp(RequestResponseContainer... executeNonHttp){
        this.executeNonHttp.clear();
        this.executeNonHttp.add(executeNonHttp);
        return this;
    }

    public MockFacadeDagNodePerItemTraversal isValidResponseHttp(Boolean... isValidResponseHttp){
        this.isValidResponseHttp.clear();
        this.isValidResponseHttp.add(isValidResponseHttp);
        return this;
    }

    public MockFacadeDagNodePerItemTraversal isValidResponseNonHttp(Boolean... isValidResponseNonHttp){
        this.isValidResponseNonHttp.clear();
        this.isValidResponseNonHttp.add(isValidResponseNonHttp);
        return this;
    }


    public MockFacadeDagNodePerItemTraversal handleInValidResponseHttp(DagTraversalService.TraverseAction... handleInValidResponseHttp){
        this.handleInValidResponseHttp.clear();
        this.handleInValidResponseHttp.add(handleInValidResponseHttp);
        return this;
    }

    public MockFacadeDagNodePerItemTraversal handleInValidResponseNonHttp(DagTraversalService.TraverseAction... handleInValidResponseNonHttp){
        this.handleInValidResponseNonHttp.clear();
        this.handleInValidResponseNonHttp.add(handleInValidResponseNonHttp);
        return this;
    }

    public MockFacadeDagNodePerItemTraversal parseSyncResponseHttp(StepDataBeanMapping... parseSyncResponseHttp){
        this.parseSyncResponseHttp.clear();
        this.parseSyncResponseHttp.add(parseSyncResponseHttp);
        return this;
    }

    public MockFacadeDagNodePerItemTraversal parseSyncResponseNonHttp(StepDataBeanMapping... parseSyncResponseHttp){
        this.parseSyncResponseNonHttp.clear();
        this.parseSyncResponseNonHttp.add(parseSyncResponseHttp);
        return this;
    }

    public MockFacadeDagNodePerItemTraversal filterHttp(Boolean... filterHttp){
        this.filterHttp.clear();
        this.filterHttp.add(filterHttp);
        return this;
    }

    public MockFacadeDagNodePerItemTraversal filterNonHttp(Boolean... filterNonHttp){
        this.filterNonHttp.clear();
        this.filterNonHttp.add(filterNonHttp);
        return this;
    }

    public MockFacadeDagNodePerItemTraversal isSyncCompleteHttp(Optional<Boolean>... isSyncCompleteHttp){
        this.isSyncCompleteHttp.clear();
        this.isSyncCompleteHttp.add(isSyncCompleteHttp);
        return this;
    }

    public MockFacadeDagNodePerItemTraversal isSyncCompleteNonHttp(Boolean... isSyncCompleteNonHttp){
        this.isSyncCompleteNonHttp.clear();
        this.isSyncCompleteNonHttp.add(isSyncCompleteNonHttp);
        return this;
    }

    public MockFacadeDagNodePerItemTraversal getNextSyncRequest(HttpRequestResponse... getNextSyncRequest){
        this.getNextSyncRequest.clear();
        this.getNextSyncRequest.add(getNextSyncRequest);
        return this;
    }

    public MockFacadeDagNodePerItemTraversal handleAbortTransactionActionHttp(Boolean... handleAbortTransactionActionHttp){

        this.handleAbortTransactionActionHttp.clear();
        this.handleAbortTransactionActionHttp.add(handleAbortTransactionActionHttp);
        return this;
    }

    public MockFacadeDagNodePerItemTraversal handleAbortTransactionActionNonHttp(Boolean... handleAbortTransactionActionNonHttp){

        this.handleAbortTransactionActionNonHttp.clear();
        this.handleAbortTransactionActionNonHttp.add(handleAbortTransactionActionNonHttp);
        return this;
    }

    public MockFacadeDagNodePerItemTraversal handleHoldAndRetryActionHttp(Long...handleHoldAndRetryActionHttp){
        this.handleHoldAndRetryActionHttp.clear();
        this.handleHoldAndRetryActionHttp.add(handleHoldAndRetryActionHttp);
        return this;
    }

    public MockFacadeDagNodePerItemTraversal handleHoldAndRetryActionNonHttp(Long...handleHoldAndRetryActionNonHttp){
        this.handleHoldAndRetryActionNonHttp.clear();
        this.handleHoldAndRetryActionNonHttp.add(handleHoldAndRetryActionNonHttp);
        return this;
    }

    public MockFacadeDagNodePerItemTraversal handleRetryWithNewRequest(HttpRequestResponse... handleRetryWithNewRequest){
        this.handleRetryWithNewRequest.clear();
        this.handleRetryWithNewRequest.add(handleRetryWithNewRequest);
        return this;
    }

    public MockFacadeDagNodePerItemTraversal handleRetryWithNewRequestNonHttp(RequestResponseContainer... handleRetryWithNewRequestNonHttp){
        this.handleRetryWithNewRequestNonHttp.clear();
        this.handleRetryWithNewRequestNonHttp.add(handleRetryWithNewRequestNonHttp);
        return this;
    }

    public MockFacadeDagNodePerItemTraversal abortCurrentParentAndReTryWithNewParentHttp(Boolean... abortCurrentParentAndReTryWithNewParentHttp){
        this.abortCurrentParentAndReTryWithNewParentHttp.clear();
        this.abortCurrentParentAndReTryWithNewParentHttp.add(abortCurrentParentAndReTryWithNewParentHttp);
        return this;
    }

    public MockFacadeDagNodePerItemTraversal abortCurrentParentAndReTryWithNewParentNonHttp(Boolean... abortCurrentParentAndReTryWithNewParentNonHttp){
        this.abortCurrentParentAndReTryWithNewParentNonHttp.clear();
        this.abortCurrentParentAndReTryWithNewParentNonHttp.add(abortCurrentParentAndReTryWithNewParentNonHttp);
        return this;
    }


    /**
     * Whatever dev has passed, will be used as it is and rest will be mocked
     * @return
     * @throws URISyntaxException
     */
    public DagNodePerItemTraversalService build() throws Exception {

        DagNodePerItemTraversalService dagNodePerItemTraversalService = Mockito.spy(DagNodePerItemTraversalService.class);

        // Adding namespace so that analytics service can be fetched
        syncServiceContainer.next().add(namespace.next());
        dagNodePerItemTraversalService.configure("dummy-name-space", syncServiceContainer.next(), abstractStep.next(), parentNodeData.next(), currentNode.next(), parentNode.next(), limitNumberOfConcurrentPerItemTraversalSemaphore.next(), waitUntilAllPerItemTraversalIsDonePhaser.next(), rateLimitBucket.next(), processorQueue.next(), traverseConfigService.next(), baggageMap.next());

        doNothing().when(dagNodePerItemTraversalService).stepConfigure(any(), any());

        doAnswer(ShouldProceedWithParentObjectHttp.answer()).when(dagNodePerItemTraversalService).ShouldProceedWithParentObjectHttp(any(), any());
        doAnswer(ShouldProceedWithParentObjectNonHttp.answer()).when(dagNodePerItemTraversalService).ShouldProceedWithParentObjectNonHttp(any(), any());

        doNothing().when(dagNodePerItemTraversalService).setupHttp(any(), any(), any());
        doNothing().when(dagNodePerItemTraversalService).setupNonHttp(any(), any(), any());

        doAnswer(startSyncHttp.answer()).when(dagNodePerItemTraversalService).startSyncHttp(any(), any());
        doAnswer(startSyncNonHttp.answer()).when(dagNodePerItemTraversalService).startSyncNonHttp(any(), any());

        doAnswer(executeHttp.answer()).when(dagNodePerItemTraversalService).executeHttp(any());
        doAnswer(executeNonHttp.answer()).when(dagNodePerItemTraversalService).executeNonHttp(any(),any());

        doAnswer(isValidResponseHttp.answer()).when(dagNodePerItemTraversalService).isValidResponseHttp(any(), any(), any());
        doAnswer(isValidResponseNonHttp.answer()).when(dagNodePerItemTraversalService).isValidResponseNonHttp(any(), any(), any());

        doAnswer(handleInValidResponseHttp.answer()).when(dagNodePerItemTraversalService).handleInValidResponseHttp(any(), any(), any());
        doAnswer(handleInValidResponseNonHttp.answer()).when(dagNodePerItemTraversalService).handleInValidResponseNonHttp(any(), any(), any());

        doAnswer(parseSyncResponseHttp.answer()).when(dagNodePerItemTraversalService).parseSyncResponseHttp(any(), any(), any());
        doAnswer(parseSyncResponseNonHttp.answer()).when(dagNodePerItemTraversalService).parseSyncResponseNonHttp(any(), any(), any());

        doNothing().when(dagNodePerItemTraversalService).filterHttp(any(), any(), any());
        doNothing().when(dagNodePerItemTraversalService).filterNonHttp(any(), any(), any());

        doAnswer(isSyncCompleteHttp.answer()).when(dagNodePerItemTraversalService).isSyncCompleteHttp(any(), any(), any());
        doAnswer(isSyncCompleteNonHttp.answer()).when(dagNodePerItemTraversalService).isSyncCompleteNonHttp(any(), any(), any());

        doAnswer(getNextSyncRequest.answer()).when(dagNodePerItemTraversalService).getNextSyncRequest(any(), any(), any());

        doNothing().when(dagNodePerItemTraversalService).traverse();

        doNothing().when(dagNodePerItemTraversalService).processIntoBean(any(), any(), any(), anyBoolean());

        doAnswer(handleAbortTransactionActionHttp.answer()).when(dagNodePerItemTraversalService).handleAbortTransactionActionHttp();
        doAnswer(handleAbortTransactionActionNonHttp.answer()).when(dagNodePerItemTraversalService).handleAbortTransactionActionNonHttp();

        doAnswer(handleAbortTransactionActionHttp.answer()).when(dagNodePerItemTraversalService).handleAbortTransactionActionHttp();
        doAnswer(handleAbortTransactionActionNonHttp.answer()).when(dagNodePerItemTraversalService).handleAbortTransactionActionNonHttp();

        doAnswer(handleHoldAndRetryActionHttp.answer()).when(dagNodePerItemTraversalService).handleHoldAndRetryActionHttp(any());
        doAnswer(handleHoldAndRetryActionNonHttp.answer()).when(dagNodePerItemTraversalService).handleHoldAndRetryActionNonHttp(any());

        doAnswer(handleHoldAndRetryActionHttp.answer()).when(dagNodePerItemTraversalService).handleHoldAndRetryActionHttp(any());
        doAnswer(handleHoldAndRetryActionNonHttp.answer()).when(dagNodePerItemTraversalService).handleHoldAndRetryActionNonHttp(any());

        doAnswer(handleRetryWithNewRequest.answer()).when(dagNodePerItemTraversalService).handleRetryWithNewRequest(any());
        doAnswer(handleRetryWithNewRequestNonHttp.answer()).when(dagNodePerItemTraversalService).handleRetryWithNewRequestNonHttp(any());

        doAnswer(abortCurrentParentAndReTryWithNewParentHttp.answer()).when(dagNodePerItemTraversalService).abortCurrentParentAndReTryWithNewParentHttp();
        doAnswer(abortCurrentParentAndReTryWithNewParentNonHttp.answer()).when(dagNodePerItemTraversalService).abortCurrentParentAndReTryWithNewParentNonHttp();

        return dagNodePerItemTraversalService;
    }
}
