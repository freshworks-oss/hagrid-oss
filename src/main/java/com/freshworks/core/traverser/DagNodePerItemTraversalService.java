package com.freshworks.core.traverser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.freshworks.core.processor.AbstractBean;
import com.freshworks.core.shared.NamespaceService;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.analytics.AnalyticsFactory;
import com.freshworks.core.shared.analytics.AnalyticsService;
import com.freshworks.core.shared.constants.Constants;
import com.freshworks.core.shared.infra.InfraConfigService;
import com.freshworks.core.shared.infra.InfraDbQueue;
import com.freshworks.core.shared.sync.SyncStatusService;
import com.freshworks.core.shared.synchronizers.ServiceTree;
import com.freshworks.core.traverser.exception.StepFailedException;
import com.freshworks.core.traverser.net.http.HttpClientService;
import com.freshworks.core.traverser.net.http.HttpRequestResponse;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import io.github.bucket4j.Bucket;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.tracing.Tracer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Phaser;
import java.util.concurrent.Semaphore;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
@Component
@Scope("prototype")
public class DagNodePerItemTraversalService implements Callable<Void> {

    @Getter
    String uuid;

    AbstractStep abstractStep;
    HttpAbstractStep httpAbstractStep;
    NonHttpAbstractStep nonHttpAbstractStep;

    MeterRegistry meterRegistry;

    String abstractStepName;

    JsonNode parentNodeData;
    Phaser dagNodePerParentPerItemPhaser;
    Phaser parentPhaser;

    Tracer tracer;

    InfraDbQueue processorQueue;

    NamespaceService namespace;
    TraverseConfigService traverseConfigService;

    InfraConfigService infraConfigService;

    SyncStatusService syncStatusService;

    ObjectMapper objectMapper = new ObjectMapper();

    SyncServiceContainer syncServiceContainer;

    AnalyticsService analyticsService;

    ImmutableMap<String, String> baggageMap;

    Map<String, String> mainThreadMdcCopy;

    DagNode currentNode;

    DagNode parentNode;

    Bucket rateLimitBucket;

    ServiceTree serviceTree;

    Semaphore limitNumberOfConcurrentPerItemTraversalSemaphore;


    public void configure(String parentUUId, SyncServiceContainer syncServiceContainer, AbstractStep abstractStep, JsonNode parentNodeData, DagNode currentNode, DagNode parentNode, Semaphore limitNumberOfConcurrentPerItemTraversalSemaphore, Phaser parentPhaser, Bucket rateLimitBucket, InfraDbQueue processorQueue, TraverseConfigService traverseConfigService, ImmutableMap<String, String> baggageMap){
        uuid = parentUUId + "/" + UUID.randomUUID();
        this.syncServiceContainer = syncServiceContainer;
        this.abstractStep = abstractStep;
        this.parentNodeData = parentNodeData;
        this.abstractStepName = this.abstractStep.getClass().getName();
        this.limitNumberOfConcurrentPerItemTraversalSemaphore = limitNumberOfConcurrentPerItemTraversalSemaphore;
        this.parentPhaser = parentPhaser;
        this.dagNodePerParentPerItemPhaser = new Phaser(1);
        this.processorQueue = processorQueue;
        this.syncStatusService = syncServiceContainer.getBean(SyncStatusService.class);
        this.traverseConfigService = traverseConfigService;
        this.baggageMap = baggageMap;
        this.currentNode = currentNode;
        this.parentNode = parentNode;
        this.rateLimitBucket = rateLimitBucket;
        this.serviceTree = syncServiceContainer.getBean(ServiceTree.class);
        AnalyticsFactory analyticsFactory = syncServiceContainer.getBean(AnalyticsFactory.class);
        namespace = syncServiceContainer.getBean(NamespaceService.class);
        this.analyticsService = analyticsFactory.getAnalyticsService(namespace.getNamespace());
        this.meterRegistry = syncServiceContainer.getBean(MeterRegistry.class);
        mainThreadMdcCopy = MDC.getCopyOfContextMap();
    }

    @Override
    public Void call() throws Exception {

        try{
            analyticsService.infoLogEvent("HAGRID_DAG_NODE_PER_ITEM", "step", abstractStepName, "_message", "DagNodePerItemTraversal started", "uuid", uuid,  "namespace" ,namespace.getNamespace());
            currentNode.relationshipIncrementTotalItemsCount(parentNode);

            long threadId = Thread.currentThread().threadId();
            //this.serviceTree.register(uuid);
            Thread.currentThread().setName( "traverser_" + threadId + "_" + this.abstractStep.getClass().getName());
            MDC.setContextMap(mainThreadMdcCopy);
            traverse();

            if(Thread.interrupted()){

                throw new InterruptedException("leaving process without completing as thread is interrupted");
            }

            else{
                currentNode.relationshipIncrementSuccessItemsCount(parentNode);
                analyticsService.infoLogEvent("HAGRID_DAG_NODE_PER_ITEM", "step", abstractStepName, "_message", "returning because step is completed", "uuid", uuid,  "namespace" ,namespace.getNamespace());
            }

            return null;
        }


        catch(Exception e){
//            e.printStackTrace();
            // TODO: Tracer when scoped a span, then it automatically set the spanId, traceId in MDC ( check Opentelemetry.java file) check https://javadoc.io/doc/io.zipkin.brave/brave/5.5.1/brave/propagation/ThreadLocalCurrentTraceContext.html,
            // so when analytics service logs an error then ideally it should lof spanId, traceId also. BUT it is not happening
            // Is it because this catch is running on different thread than try block
            // So as of now, we are missing traceId, spanId etc in below logs. but it is coming in log in try block.

            analyticsService.errorLogEvent("HAGRID_DAG_NODE_PER_ITEM", "_message", e.getClass().getName() + ": " + e.getMessage(), "stacktrace" , Throwables.getStackTraceAsString(e), "step", this.abstractStep.getClass().getName(),"namespace" ,namespace.getNamespace(), "uuid", uuid);
            currentNode.relationshipIncrementFailedItemsCount(parentNode);
            return null;
        }

        catch(Error e){

            analyticsService.errorLogEvent("HAGRID_DAG_NODE_PER_ITEM", "_message", e.getClass().getName() + ": " + e.getMessage(), "stacktrace" , Throwables.getStackTraceAsString(e), "step", this.abstractStep.getClass().getName(),"namespace" ,namespace.getNamespace(), "uuid", uuid);
            currentNode.relationshipIncrementFailedItemsCount(parentNode);
            return null;

        }

        finally {
            limitNumberOfConcurrentPerItemTraversalSemaphore.release();
            this.dagNodePerParentPerItemPhaser.arriveAndDeregister();
            this.parentPhaser.arriveAndDeregister();
            // clearing the items as this thread might be reuse
            MDC.clear();

            // Clearing the thread interrupt flag if it is set so that when executor service lend this thread to some other task then it should have this flag cleared.
            Thread.interrupted();
        }
    }

    protected void stepConfigure(AbstractStep abstractStep, SyncServiceContainer syncServiceContainer) throws InterruptedException {

        abstractStep.configure(syncServiceContainer);

        if(Boolean.TRUE.equals(isThreadInterrupted())){
            throw new InterruptedException("Thread is interrupted in step configure method");
        }
    }

    protected boolean ShouldProceedWithParentObjectHttp(HttpAbstractStep abstractStep, JsonNode parentNodeData) throws Exception {

        boolean opt = abstractStep.shouldProceedWithParentObject(baggageMap, parentNodeData);

        if (Boolean.TRUE.equals(isThreadInterrupted())){
            throw new InterruptedException("Thread is interrupted in step shouldProceedWithParentObjectHttp method");
        }

        return opt;
    }

    protected boolean ShouldProceedWithParentObjectNonHttp(NonHttpAbstractStep abstractStep, JsonNode parentNodeData) throws Exception {

        boolean opt = abstractStep.shouldProceedWithParentObjectNonHttp(baggageMap, parentNodeData);

        if (Boolean.TRUE.equals(isThreadInterrupted())){

            throw new InterruptedException("Thread is interrupted in step ShouldProceedWithParentObjectNonHttp method");
        }

        return opt;

    }

    protected void setupHttp(HttpAbstractStep abstractStep, JsonNode parentNodeData, ImmutableMap<String, String> baggageMap) throws Exception {

        abstractStep.setup(baggageMap, parentNodeData);
        analyticsService.debugLogEvent("HAGRID_DAG_NODE_PER_ITEM", "step", abstractStep.getClass().getName(), "command", "setup", "uuid", uuid, "namespace" ,namespace.getNamespace());

        if(Boolean.TRUE.equals(isThreadInterrupted())){
            throw new InterruptedException("Thread is interrupted in step setupHttp method");
        }
    }

    protected void setupNonHttp(NonHttpAbstractStep abstractStep, JsonNode parentNodeData, ImmutableMap<String, String> baggageMap) throws Exception {
        abstractStep.setupNonHttp(baggageMap, parentNodeData);
        analyticsService.debugLogEvent("HAGRID_DAG_NODE_PER_ITEM", "step", abstractStep.getClass().getName(), "command", "setup", "uuid", uuid, "namespace" ,namespace.getNamespace());

        if(Boolean.TRUE.equals(isThreadInterrupted())){
            throw new InterruptedException("Thread is interrupted in step setupNonHttp method");
        }
    }

    protected HttpRequestResponse startSyncHttp(HttpAbstractStep abstractStep, JsonNode parentNodeData) throws Exception {

        HttpRequestResponse httpRequestResponse = abstractStep.startSync(parentNodeData);

        if(Boolean.FALSE.equals(isThreadInterrupted()) && httpRequestResponse != null){
            analyticsService.debugLogEvent("HAGRID_DAG_NODE_PER_ITEM", "step", abstractStep.getClass().getName(), "command", "startSyncHttp", "uuid", uuid, "namespace" ,namespace.getNamespace());
            return httpRequestResponse;
        }

        else if (Boolean.TRUE.equals(isThreadInterrupted())){
            throw new InterruptedException("Thread is interrupted in step startSyncHttp method");
        }

        else if (httpRequestResponse == null){
            throw new InterruptedException("Returning because step method startSyncHttp return null");
        }

        else{
            throw new InterruptedException("Returning because step method startSyncHttp return value is unknown");
        }
    }

    protected RequestResponseContainer startSyncNonHttp(NonHttpAbstractStep abstractStep, JsonNode parentNodeData) throws Exception {

        RequestResponseContainer requestResponseContainer = abstractStep.startSyncNonHttp(parentNodeData);

        if(Boolean.FALSE.equals(isThreadInterrupted()) && requestResponseContainer != null){

            analyticsService.debugLogEvent("HAGRID_DAG_NODE_PER_ITEM", "step", abstractStep.getClass().getName(), "command", "startSyncNonHttp", "uuid", uuid, "namespace" ,namespace.getNamespace());
            return requestResponseContainer;
        }

        else if (Boolean.TRUE.equals(isThreadInterrupted())){
            throw new InterruptedException("Thread is interrupted in step startSyncNonHttp method");
        }

        else if (requestResponseContainer == null){
            throw new InterruptedException("Returning because step method startSyncNonHttp return null");
        }

        else {
            throw new InterruptedException("Returning because step method startSyncNonHttp return value is unknown");
        }
    }


    protected HttpRequestResponse executeHttp(HttpRequestResponse requestResponse) throws Exception {

        Timer timer = meterRegistry.timer("request.execution.time", "type", "http", "step", currentNode.getShortName());

        String exceptionString = timer.record(()->{

            try{
                long startTime = System.currentTimeMillis();
                HttpClientService httpClientService = syncServiceContainer.getBean(HttpClientService.class);
                httpClientService.execute(namespace.getNamespace(), requestResponse);
                long stopTime = System.currentTimeMillis();
                long diff = stopTime - startTime;
                analyticsService.debugLogEvent("HAGRID_DAG_NODE_PER_ITEM", "step", httpAbstractStep.getClass().getName(), "command", "executeHttp", "uuid", uuid, "namespace" ,namespace.getNamespace(), "uri", requestResponse.getRequest().getRequestUri(), "execute_time_taken_ms", diff);
                return null;
            }
            catch (Exception e){

                return e.getMessage();
            }
        });

        if(Boolean.FALSE.equals(isThreadInterrupted()) && exceptionString == null){
            return  requestResponse;
        }

        else if (Boolean.TRUE.equals(isThreadInterrupted())){
            throw new InterruptedException("Thread is interrupted before calling step executeHttp method");
        }

        else if (exceptionString != null){
            throw new Exception(exceptionString);
        }

        else {

            throw new InterruptedException("Returning because step method executeHttp return value is unknown");
        }
    }

    protected RequestResponseContainer executeNonHttp(NonHttpAbstractStep abstractStep, RequestResponseContainer requestResponse) throws Exception {

        Timer timer = meterRegistry.timer("request.execution.time", "type", "non-http", "step", currentNode.getShortName());

        timer.record(() ->{

            long startTime = System.currentTimeMillis();
            try {
                abstractStep.executeNonHttp(requestResponse);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            long stopTime = System.currentTimeMillis();
            long diff = stopTime - startTime;
            analyticsService.debugLogEvent("HAGRID_DAG_NODE_PER_ITEM", "step", abstractStep.getClass().getName(), "command", "executeNonHttp", "uuid", uuid, "namespace" ,namespace.getNamespace(), "command", requestResponse.getRequest(), "execute_time_taken_ms", diff);
            return null;
        });

        if(Boolean.FALSE.equals(isThreadInterrupted())){
            return  requestResponse;
        }

        else if (Boolean.TRUE.equals(isThreadInterrupted())){
            throw new InterruptedException("Thread is interrupted in step executeNonHttp method");
        }
        else{
            throw new InterruptedException("Returning because step method executeNonHttp return value is unknown");
        }

    }

    protected boolean isValidResponseHttp(HttpAbstractStep abstractStep, HttpRequestResponse httpRequestResponse, JsonNode parentNodeData) throws Exception {

        boolean x = abstractStep.isValidResponse(httpRequestResponse, parentNodeData);

        if(Boolean.FALSE.equals(isThreadInterrupted())){
            analyticsService.debugLogEvent("HAGRID_DAG_NODE_PER_ITEM", "step", abstractStep.getClass().getName(), "command", "isValidResponseHttp", "uuid", uuid, "namespace" ,namespace.getNamespace());
            return x;
        }

        else if (Boolean.TRUE.equals(isThreadInterrupted())){
            throw new InterruptedException("Thread is interrupted before calling step isValidResponseHttp method");
        }
        else{
            throw new InterruptedException("Returning because step method isValidResponseHttp return value is unknown");
        }

    }

    protected boolean isValidResponseNonHttp(NonHttpAbstractStep abstractStep, RequestResponseContainer requestResponseContainer, JsonNode parentNodeData) throws Exception {

        boolean x = abstractStep.isValidResponseNonHttp(requestResponseContainer, parentNodeData);

        if(Boolean.FALSE.equals(isThreadInterrupted())){
            analyticsService.debugLogEvent("HAGRID_DAG_NODE_PER_ITEM", "step", abstractStep.getClass().getName(), "command", "startSync", "uuid", uuid, "namespace" ,namespace.getNamespace());
            return x;
        }

        else if (Boolean.TRUE.equals(isThreadInterrupted())){
            throw new InterruptedException("Thread is interrupted before calling step isValidResponseNonHttp method");
        }

        else{
            throw new InterruptedException("Returning because step isValidResponseNonHttp return value is unknown");
        }
    }

    protected DagTraversalService.TraverseAction handleInValidResponseHttp(HttpAbstractStep abstractStep, HttpRequestResponse httpRequestResponse, JsonNode parentNodeData) throws Exception {

        DagTraversalService.TraverseAction action =  abstractStep.handleInvalidResponse(httpRequestResponse, parentNodeData);

        if(Boolean.FALSE.equals(isThreadInterrupted()) && action != null){
            analyticsService.debugLogEvent("HAGRID_DAG_NODE_PER_ITEM", "step", abstractStep.getClass().getName(), "command", "handleInValidResponseHttp", "uuid", uuid, "namespace" ,namespace.getNamespace());
            return action;
        }

        else if (Boolean.TRUE.equals(isThreadInterrupted())){
            throw new InterruptedException("Thread is interrupted before calling step handleInValidResponseHttp method");
        }

        else if (action == null){

            throw new InterruptedException("Returning because step handleInValidResponseHttp return value is null");
        }

        else{

            throw new InterruptedException("Returning because step handleInValidResponseHttp return value is unknown");
        }

    }

    protected DagTraversalService.TraverseAction handleInValidResponseNonHttp(NonHttpAbstractStep abstractStep, RequestResponseContainer httpRequestResponse, JsonNode parentNodeData) throws Exception {
        DagTraversalService.TraverseAction traverseAction =  abstractStep.handleInValidResponseNonHttp(httpRequestResponse, parentNodeData);

        if(Boolean.FALSE.equals(isThreadInterrupted()) && traverseAction != null){
            analyticsService.debugLogEvent("HAGRID_DAG_NODE_PER_ITEM", "step", abstractStep.getClass().getName(), "command", "handleInValidResponseHttp", "uuid", uuid, "namespace" ,namespace.getNamespace());
            return traverseAction;
        }
        else if (Boolean.TRUE.equals(isThreadInterrupted())){
            throw new InterruptedException("Thread is interrupted before calling step handleInValidResponseNonHttp method");
        }

        else if (traverseAction == null){
            throw new InterruptedException("Returning because step handleInValidResponseNonHttp return action is null");
        }

        else{
            throw new InterruptedException("Returning because step handleInValidResponseNonHttp return value is unknown");
        }

    }

    protected StepDataBeanMapping parseSyncResponseHttp(HttpAbstractStep abstractStep, HttpRequestResponse httpRequestResponse, JsonNode parentNodeData) throws Exception {

        StepDataBeanMapping stepDataBeanMapping = abstractStep.parseSyncResponse(httpRequestResponse, parentNodeData);

        if(Boolean.FALSE.equals(isThreadInterrupted()) && stepDataBeanMapping != null){
            analyticsService.debugLogEvent("HAGRID_DAG_NODE_PER_ITEM", "step", abstractStep.getClass().getName(), "command", "parseSyncResponseHttp", "uuid", uuid, "namespace" ,namespace.getNamespace());
            return stepDataBeanMapping;
        }

        else if (Boolean.TRUE.equals(isThreadInterrupted())){
            throw new InterruptedException("Thread is interrupted before calling step parseSyncResponseHttp method");
        }
        else if (stepDataBeanMapping == null){
            throw new InterruptedException("Returning because step parseSyncResponseHttp return value is null");
        }
        else{
            throw new InterruptedException("Returning because step parseSyncResponseHttp return value is unknown");
        }

    }

    protected StepDataBeanMapping parseSyncResponseNonHttp(NonHttpAbstractStep abstractStep, RequestResponseContainer requestResponseContainer, JsonNode parentNodeData) throws Exception {
        StepDataBeanMapping stepDataBeanMapping = abstractStep.parseSyncResponseNonHttp(requestResponseContainer, parentNodeData);

        if(Boolean.FALSE.equals(isThreadInterrupted()) && stepDataBeanMapping != null){
            analyticsService.debugLogEvent("HAGRID_DAG_NODE_PER_ITEM", "step", abstractStep.getClass().getName(), "command", "parseSyncResponseNonHttp", "uuid", uuid, "namespace" ,namespace.getNamespace());
            return stepDataBeanMapping;
        }

        else if (Boolean.TRUE.equals(isThreadInterrupted())){
            throw new InterruptedException("Thread is interrupted before calling parseSyncResponseNonHttp method");
        }
        else if (stepDataBeanMapping == null){
            throw new InterruptedException("Returning because step parseSyncResponseNonHttp return action is null");
        }

        else {
            throw new InterruptedException("Returning because step parseSyncResponseNonHttp return value is unknown");
        }

    }

    protected void filterHttp(HttpAbstractStep abstractStep, StepDataBeanMapping stepDataBeanMapping, JsonNode parentNodeData) throws Exception {

        abstractStep.filterResponse(stepDataBeanMapping, parentNodeData);
        analyticsService.debugLogEvent("HAGRID_DAG_NODE_PER_ITEM", "step", abstractStep.getClass().getName(), "command", "filterHttp", "uuid", uuid, "namespace" ,namespace.getNamespace());

        if(Boolean.TRUE.equals(isThreadInterrupted())){
            throw new InterruptedException("Thread is interrupted in step filterHttp method");
        }

    }

    protected void filterNonHttp(NonHttpAbstractStep abstractStep, StepDataBeanMapping StepDataBeanMapping, JsonNode parentNodeData) throws Exception {

        abstractStep.filterResponseNonHttp(StepDataBeanMapping, parentNodeData);
        analyticsService.debugLogEvent("HAGRID_DAG_NODE_PER_ITEM", "step", abstractStep.getClass().getName(), "command", "filterNonHttp", "uuid", uuid, "namespace" ,namespace.getNamespace());

        if(Boolean.TRUE.equals(isThreadInterrupted())){
            throw new InterruptedException("Thread is interrupted in step filterNonHttp method");
        }

    }

    protected boolean isSyncCompleteHttp(HttpAbstractStep abstractStep, HttpRequestResponse httpRequestResponse, JsonNode parentNodeData) throws Exception {

        boolean x = abstractStep.isSyncComplete(httpRequestResponse, parentNodeData);

        if(Boolean.FALSE.equals(isThreadInterrupted())){
            analyticsService.debugLogEvent("HAGRID_DAG_NODE_PER_ITEM", "step", abstractStep.getClass().getName(), "command", "isSyncCompleteHttp", "uuid", uuid, "namespace" ,namespace.getNamespace());
            return x;
        }

        else if (Boolean.TRUE.equals(isThreadInterrupted())){
            throw new InterruptedException("Thread is interrupted before calling isSyncCompleteHttp method");
        }

        else {
            throw new InterruptedException("Returning because step method isSyncComplete return value is unknown");
        }

    }

    protected Boolean isSyncCompleteNonHttp(NonHttpAbstractStep abstractStep, RequestResponseContainer requestResponseContainer, JsonNode parentNodeData) throws Exception {
        boolean x = abstractStep.isSyncCompleteNonHttp(requestResponseContainer, parentNodeData);

        if(Boolean.FALSE.equals(isThreadInterrupted())){
            analyticsService.debugLogEvent("HAGRID_DAG_NODE_PER_ITEM", "step", abstractStep.getClass().getName(), "command", "isSyncCompleteHttp", "uuid", uuid, "namespace" ,namespace.getNamespace());
            return x;
        }

        else if (Boolean.TRUE.equals(isThreadInterrupted())){
            throw new InterruptedException("Thread is interrupted before calling isSyncCompleteNonHttp method");
        }

        else {
            throw new InterruptedException("Returning because step method isSyncCompleteNonHttp return value is unknown");
        }

    }

    public HttpRequestResponse getNextSyncRequest(HttpAbstractStep abstractStep, HttpRequestResponse requestResponse, JsonNode parentNodeData ) throws Exception {

        HttpRequestResponse httpRequestResponse = abstractStep.getNextSyncRequest(requestResponse, parentNodeData);

        if(Boolean.FALSE.equals(isThreadInterrupted()) && httpRequestResponse != null){
            return httpRequestResponse;
        }

        else if (Boolean.TRUE.equals(isThreadInterrupted())){
            throw new InterruptedException("Thread is interrupted before calling getNextSyncRequest method");
        }

        else if (httpRequestResponse == null){
            throw new InterruptedException("Returning because getNextSyncRequest method is null");
        }

        else {
            throw new InterruptedException("Returning because step method getNextSyncRequest return value is unknown");
        }

    }

    protected boolean  handleAbortTransactionActionHttp() throws InterruptedException {

        if(Boolean.FALSE.equals(isThreadInterrupted())){
            analyticsService.debugLogEvent("HAGRID_DAG_NODE_PER_ITEM", "step", httpAbstractStep.getClass().getName(), "command", "handleAbortTransactionActionHttp", "uuid", uuid, "namespace" ,namespace.getNamespace());
            return true;
        }

        throw new InterruptedException("Thread is interrupted before calling handleAbortTransactionActionHttp method");

    }

    protected boolean handleAbortTransactionActionNonHttp() throws InterruptedException {

        if(Boolean.FALSE.equals(isThreadInterrupted())){
            analyticsService.debugLogEvent("HAGRID_DAG_NODE_PER_ITEM", "step", nonHttpAbstractStep.getClass().getName(), "command", "handleAbortTransactionActionNonHttp", "uuid", uuid, "namespace" ,namespace.getNamespace());
            return true;
        }

        throw new InterruptedException("Thread is interrupted before calling handleAbortTransactionActionNonHttp method");

    }

    protected long handleHoldAndRetryActionHttp(DagTraversalService.TraverseAction traverseAction) throws InterruptedException {

        if(Boolean.FALSE.equals(isThreadInterrupted())){
            analyticsService.debugLogEvent("HAGRID_DAG_NODE_PER_ITEM", "step", httpAbstractStep.getClass().getName(), "command", "handleHoldAndRetryActionHttp", "uuid", uuid, "namespace" ,namespace.getNamespace());
            return traverseAction.waitTimeInMilliseconds;
        }
        throw new InterruptedException("Thread is interrupted before calling handleHoldAndRetryActionHttp method");
    }

    protected long handleHoldAndRetryActionNonHttp(DagTraversalService.TraverseAction traverseAction) throws InterruptedException {

        if(Boolean.FALSE.equals(isThreadInterrupted())){
            analyticsService.debugLogEvent("HAGRID_DAG_NODE_PER_ITEM", "step", nonHttpAbstractStep.getClass().getName(), "command", "handleHoldAndRetryActionNonHttp", "uuid", uuid, "namespace" ,namespace.getNamespace());
            return traverseAction.waitTimeInMilliseconds;
        }

        throw new InterruptedException("Thread is interrupted before calling handleHoldAndRetryActionNonHttp method");
    }

    protected HttpRequestResponse handleRetryWithNewRequest(DagTraversalService.TraverseAction traverseAction) throws InterruptedException {

        if(Boolean.FALSE.equals(isThreadInterrupted())){
            analyticsService.debugLogEvent("HAGRID_DAG_NODE_PER_ITEM", "step", httpAbstractStep.getClass().getName(), "command", "handleRetryWithNewRequest", "uuid", uuid, "namespace" ,namespace.getNamespace());
            return traverseAction.requestResponse;
        }

        throw new InterruptedException("Thread is interrupted before calling handleRetryWithNewRequest method");

    }

    protected HttpRequestResponse handleRetryWithNewRequestNonHttp(DagTraversalService.TraverseAction traverseAction) throws InterruptedException {

        if(Boolean.FALSE.equals(isThreadInterrupted())){
            analyticsService.debugLogEvent("HAGRID_DAG_NODE_PER_ITEM", "step", nonHttpAbstractStep.getClass().getName(), "command", "handleRetryWithNewRequestNonHttp", "uuid", uuid, "namespace" ,namespace.getNamespace());
            return traverseAction.requestResponse;
        }

        throw new InterruptedException("Thread is interrupted before calling handleRetryWithNewRequestNonHttp method");

    }

    protected boolean abortCurrentParentAndReTryWithNewParentHttp() throws InterruptedException {

        if(Boolean.FALSE.equals(isThreadInterrupted())){
            analyticsService.debugLogEvent("HAGRID_DAG_NODE_PER_ITEM", "step", httpAbstractStep.getClass().getName(), "command", "abortCurrentParentAndReTryWithNewParentHttp", "uuid", uuid, "namespace" ,namespace.getNamespace());
            return true;
        }

        throw new InterruptedException("Thread is interrupted before calling abortCurrentParentAndReTryWithNewParentHttp method");
    }

    protected boolean abortCurrentParentAndReTryWithNewParentNonHttp() throws InterruptedException {

        if(Boolean.FALSE.equals(isThreadInterrupted())){
            analyticsService.debugLogEvent("HAGRID_DAG_NODE_PER_ITEM", "step", nonHttpAbstractStep.getClass().getName(), "command", "abortCurrentParentAndReTryWithNewParentNonHttp", "uuid", uuid, "namespace" ,namespace.getNamespace());
            return true;
        }

        throw new InterruptedException("Thread is interrupted before calling abortCurrentParentAndReTryWithNewParentNonHttp method");

    }

    public void traverse() throws Exception {

        String threadName = "DagNodePerItemTraversal" + "_" +  currentNode.getName() + "_" + abstractStepName;
        Thread.currentThread().setName(threadName);

        boolean isHttpBased = true;

        if(abstractStep instanceof HttpAbstractStep) {
            isHttpBased = true;
            httpAbstractStep = (HttpAbstractStep) abstractStep;
        }
        else if(abstractStep instanceof NonHttpAbstractStep) {
            isHttpBased = false;
            nonHttpAbstractStep = (NonHttpAbstractStep) abstractStep;
        }
        else {
            throw new Exception("Unknown instance type of the step object");
        }

        // First configure step with container services.
        stepConfigure(abstractStep, syncServiceContainer);

        if(isHttpBased){
            isHttpBased = true;
        }

        HttpRequestResponse requestResponse = null;
        RequestResponseContainer requestResponseContainer = null;

        Boolean shouldProceed;
        if(isHttpBased){
            shouldProceed = ShouldProceedWithParentObjectHttp(httpAbstractStep, parentNodeData);
        }
        else {
            shouldProceed = ShouldProceedWithParentObjectNonHttp(nonHttpAbstractStep,parentNodeData);
        }


        if(Boolean.TRUE.equals(shouldProceed)){

            if(isHttpBased){
                setupHttp(httpAbstractStep, parentNodeData, baggageMap);
                requestResponse = startSyncHttp(httpAbstractStep, parentNodeData);
            }
            else{
                setupNonHttp(nonHttpAbstractStep, parentNodeData, baggageMap);
                requestResponseContainer = startSyncNonHttp(nonHttpAbstractStep, parentNodeData);
            }

        }
        else{

            return;
        }

        // Iterate until all instances are fetched for the current node given the parentNode
        while (Boolean.FALSE.equals(Thread.interrupted())) {

            if(isHttpBased){
                rateLimitBucket.asBlocking().consume(1);
                executeHttp(requestResponse);
            }
            else{
                executeNonHttp(nonHttpAbstractStep, requestResponseContainer);
            }


            DagTraversalService.TraverseAction traverseAction = null;
            boolean isValidResponse= false;
            boolean isValidResponseNonHttp = false;

            if(isHttpBased){
                isValidResponse = isValidResponseHttp(httpAbstractStep, requestResponse, parentNodeData);
            }
            else{
                isValidResponseNonHttp = isValidResponseNonHttp(nonHttpAbstractStep, requestResponseContainer, parentNodeData);
            }

            if (isHttpBased && Boolean.FALSE.equals(isValidResponse)){
                traverseAction = handleInValidResponseHttp(httpAbstractStep, requestResponse, parentNodeData);

                if (traverseAction.traverse_event == DagTraversalService.TraverseAction.TRAVERSE_EVENT.ABORT_TRANSACTION){

                    boolean isTrue = handleAbortTransactionActionHttp();
                    if(isTrue){
                        break;
                    }
                }

                else if (traverseAction.traverse_event == DagTraversalService.TraverseAction.TRAVERSE_EVENT.ON_HOLD_AND_RETRY){

                    long msToWait = handleHoldAndRetryActionHttp(traverseAction);
                    Thread.sleep(msToWait);
                    continue;
                }


                else if (traverseAction.traverse_event == DagTraversalService.TraverseAction.TRAVERSE_EVENT.RETRY_WITH_NEW_REQUEST){
                    requestResponse = handleRetryWithNewRequest(traverseAction);
                    continue;
                }
                else if (traverseAction.traverse_event == DagTraversalService.TraverseAction.TRAVERSE_EVENT.ABORT_CURRENT_PARENT_AND_CONTINUE_WITH_NEXT_PARENT){
                    boolean shouldAbort = abortCurrentParentAndReTryWithNewParentHttp();
                    if(shouldAbort){
                        break;
                    }
                }

                else{
                    break;
                }
            }

            else if(Boolean.FALSE.equals(isHttpBased) && Boolean.FALSE.equals(isValidResponseNonHttp)){

//              Handle the rate limit response
                traverseAction = handleInValidResponseNonHttp(nonHttpAbstractStep, requestResponseContainer, parentNodeData);

                if (traverseAction.traverse_event == DagTraversalService.TraverseAction.TRAVERSE_EVENT.ABORT_TRANSACTION){

                    boolean isTrue = handleAbortTransactionActionNonHttp();
                    if(isTrue){
                        break;
                    }
                }
                else if (traverseAction.traverse_event == DagTraversalService.TraverseAction.TRAVERSE_EVENT.ON_HOLD_AND_RETRY){

                    long msToWait = handleHoldAndRetryActionNonHttp(traverseAction);
                    Thread.sleep(msToWait);
                    continue;
                }
                else if (traverseAction.traverse_event == DagTraversalService.TraverseAction.TRAVERSE_EVENT.RETRY_WITH_NEW_REQUEST){

                    requestResponse = handleRetryWithNewRequestNonHttp(traverseAction);
                    continue;
                }
                else if (traverseAction.traverse_event == DagTraversalService.TraverseAction.TRAVERSE_EVENT.ABORT_CURRENT_PARENT_AND_CONTINUE_WITH_NEXT_PARENT){

                    boolean shouldAbort = abortCurrentParentAndReTryWithNewParentNonHttp();
                    if(shouldAbort){
                        break;
                    }
                }

                else{
                    break;
                }
            }

            JsonNode jNodeList = null;
            StepDataBeanMapping stepDataBeanMapping;

            if(isHttpBased){
                stepDataBeanMapping = parseSyncResponseHttp(httpAbstractStep, requestResponse, parentNodeData);
                jNodeList = stepDataBeanMapping.getParseSyncedResponseData();
            }

            else{
                stepDataBeanMapping = parseSyncResponseNonHttp(nonHttpAbstractStep, requestResponseContainer, parentNodeData);
                jNodeList = stepDataBeanMapping.getParseSyncedResponseData();
            }


            if(isHttpBased){
                filterHttp(httpAbstractStep, stepDataBeanMapping, parentNodeData);
            }
            else{
                filterNonHttp(nonHttpAbstractStep, stepDataBeanMapping, parentNodeData);
            }


            if(jNodeList.isObject()){
                ArrayNode array = objectMapper.createArrayNode();
                array.add(jNodeList);
                jNodeList = array;
            }

            JsonNode finalJNodeList = jNodeList;

            processIntoBean(currentNode, finalJNodeList, stepDataBeanMapping.getBeanClass(), stepDataBeanMapping.isPassToChildNodes());

            if(isHttpBased){
//                Optional<Boolean> opt = abstractStep.isSyncComplete(requestResponse, parentNodeData);
                boolean opt = isSyncCompleteHttp(httpAbstractStep, requestResponse, parentNodeData);

                if(Boolean.FALSE.equals(opt)){
                    requestResponse = getNextSyncRequest(httpAbstractStep, requestResponse, parentNodeData);
                }
                else{
                    break;
                }
            }
            else{

                boolean opt = isSyncCompleteNonHttp(nonHttpAbstractStep, requestResponseContainer, parentNodeData);
                if(Boolean.FALSE.equals(opt)){
                    requestResponseContainer = nonHttpAbstractStep.getNextSyncRequestNonHttp(requestResponseContainer, parentNodeData);

                }
                else{
                    break;
                }
            }
        }

        abstractStep.closeSync();
    }

    public void processIntoBean(DagNode currentNode, JsonNode jNodeList, Class<? extends AbstractBean> abstractBeanClass, boolean isPassToChildNodes) throws Exception {

        ArrayList<String> saveAsParentList = new ArrayList<>();
        ArrayList<String> saveInProcessorQueue = new ArrayList<>();

        long time = System.currentTimeMillis();

        Iterator<JsonNode> iterator = jNodeList.iterator();

        Long traversedNode = 1l;

        // For the current request of the current node given the parent, convert all instances into beans of the respective node
        while (iterator.hasNext()) {

            ObjectNode o = null;
            JsonNode jNode = iterator.next();

            o = (ObjectNode) jNode;

            String clazzName = abstractBeanClass.getName();
            o.put(Constants.JsonTypeInfo_As_PROPERTY, clazzName);

            long serialTime = System.currentTimeMillis();
            AbstractBean abstractBean = objectMapper.convertValue(o, AbstractBean.class);

            // adding syncServiceContainer to bean
            abstractBean.configure(syncServiceContainer);


            String abstractBeanName = abstractBean.getClass().getName();


            // Here using map function, abstract bean can be transformed into set of other array beans;
            List<AbstractBean> abstractBeanList = abstractBean.map();

            Iterator<AbstractBean> abstractBeanIterator = abstractBeanList.iterator();
            while(abstractBeanIterator.hasNext()){
                abstractBean = abstractBeanIterator.next();

                // adding syncServiceContainer to bean
                abstractBean.configure(syncServiceContainer);

                abstractBean.setClazz(abstractBean.getClass().getName());

                Boolean filter  = abstractBean.filter();
                if (Boolean.TRUE.equals(filter)) {
                    abstractBean.transform();
                    abstractBean.setParentBean(parentNodeData);

                    // Here save this as well so that it can be used to process its child

                    String s = objectMapper.writeValueAsString(abstractBean);
                    analyticsService.meterCounter("HAGRID_BEAN_IS_PUBLISHED", "bean_name", abstractBean.getClass().getName());
                    saveInProcessorQueue.add(s);

                    if(isPassToChildNodes){
                        // Save only to parentList if developer want to pass it to child nodes.
                        saveAsParentList.add(s);
                    }
                }
                else{

                }
            }
        }

        saveIntoProcessorQueue(currentNode, saveInProcessorQueue);
        saveIntoNodeList(currentNode, saveAsParentList);
    }


    public void saveIntoProcessorQueue(DagNode node, List<String> saveInProcessorQueue) throws Exception {

        Timer timer = meterRegistry.timer("infra.execution.time","type", "queue", "name", "processor_queue");

        String erroString = timer.record(()->{

            try{
                long currentTime = System.currentTimeMillis();
                processorQueue.add(saveInProcessorQueue);
                long endTime = System.currentTimeMillis();
                long diff = endTime - currentTime;
                analyticsService.debugLogEvent("HAGRID_DAG_NODE_PER_ITEM", "step", abstractStep.getClass().getName(), "command", "save_into_process_queue", "uuid", uuid, "namespace" ,namespace.getNamespace(), "queue_size", saveInProcessorQueue.size(), "execute_time_taken_ms", diff);
                return null;
            }

            catch (Exception e){
                return e.getMessage();
            }
        });

        if (erroString != null){
            throw new Exception(erroString);
        }
    }

    public void saveIntoNodeList(DagNode node, List<String> saveAsParentList) throws Exception {

        Timer timer = meterRegistry.timer("infra.execution.time", "type", "list", "name", currentNode.getShortName());

        String errorString = timer.record(() ->{

            try{
                long currentTime = System.currentTimeMillis();
                node.saveSyncResult(saveAsParentList);
                long endTime = System.currentTimeMillis();
                long diff = endTime - currentTime;
                analyticsService.debugLogEvent("HAGRID_DAG_NODE_PER_ITEM", "step", abstractStep.getClass().getName(), "command", "save_into_node_list", "uuid", uuid, "namespace" , namespace.getNamespace(), "list_size", saveAsParentList.size(), "execute_time_taken_ms", diff);
                return null;
            }

            catch (Exception e){
                return e.getMessage();
            }
        });

        if(errorString != null){
            throw new Exception(errorString);
        }
    }

    private boolean isThreadInterrupted(){

        return Thread.currentThread().isInterrupted();
    }

}
