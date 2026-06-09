package com.freshworks.core.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.freshworks.core.processor.joins.AbstractJoinService;
import com.freshworks.core.shared.Annotations.BetaRelease;
import com.freshworks.core.shared.Namespace;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.analytics.AnalyticsFactory;
import com.freshworks.core.shared.analytics.AnalyticsService;
import com.freshworks.core.shared.infra.InfraDbQueue;
import com.freshworks.core.shared.infra.InfraService;
import com.freshworks.core.shared.sync.SyncStatusService;
import com.freshworks.core.shared.synchronizers.ServiceTree;
import com.freshworks.freshindex.index.JsonIndexService;
import com.google.common.base.Charsets;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import lombok.Getter;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicLong;


@Component
@Scope(value="prototype")
public class ProcessorService implements Callable<Void> {

    @Getter
    String uuid;

    ServiceTree serviceTree;

    AnalyticsService analyticsService;
    ProcessorConfigService processorConfigService;

    ProcessorExecutorService processorExecutorService;

    JsonIndexService jsonIndexService;


    BloomFilter<String> bloomFilter = BloomFilter.create(Funnels.stringFunnel(Charsets.UTF_16), 1000000);


    SyncServiceContainer syncServiceContainer;

    InfraService infraService;

    AbstractJoinService noopJoinService;

    AbstractJoinService leftJoinService;

    AbstractJoinService innerJoinService;

    SyncStatusService syncStatusService;

    ObjectMapper freshIndexObjectMapper;
    Namespace namespace;

    Phaser phaser;

    Map<String, String> mainThreadMdcCopy;

    ImmutableListMultimap<String, String> assetBeanDependencyMap;
     
    ImmutableListMultimap<String, String> assetAssetDependencyMap;

    ProcessTaskTracker processTaskTracker;


    @Autowired
    public ProcessorService(@Qualifier("LeftJoinService") AbstractJoinService leftJoinService, @Qualifier("InnerJoinService") AbstractJoinService innerJoinService, @Qualifier("NoopJoinService") AbstractJoinService noopJoinService, ProcessorExecutorService processorExecutorService) throws IOException {

        this.innerJoinService = innerJoinService;
        this.innerJoinService.configure(this.bloomFilter);

        this.leftJoinService = leftJoinService;
        this.leftJoinService.configure(this.bloomFilter);

        this.noopJoinService = noopJoinService;
        this.noopJoinService.configure(this.bloomFilter);

        this.processorExecutorService = processorExecutorService;
        freshIndexObjectMapper = new ObjectMapper();
        freshIndexObjectMapper.registerModule(new SimpleModule(){

            @Override
            public void setupModule(SetupContext context) {
                super.setupModule(context);
                context.addBeanSerializerModifier(new FreshIndexBeanSerializeModifier());
            }
        });

        freshIndexObjectMapper.enable(SerializationFeature.WRAP_ROOT_VALUE);

    }


    @Override
    public Void call() throws Exception {

        try{
            analyticsService.infoEvent("HAGRID_PROCESSOR_SERVICE",  "_message", "Processor Service started", "uuid", uuid, "namespace" ,namespace.getNamespace());
            MDC.setContextMap(mainThreadMdcCopy);
            run();
            phaser.arriveAndAwaitAdvance();

            if(Thread.interrupted()){

                throw new InterruptedException("leaving process without completing as thread is interrupted");
            }

            else{

                if((processTaskTracker.getTotalFailedTask() == 0L) && (processTaskTracker.getTotalProcessTask() == processTaskTracker.getTotalProcessTask())){
                    analyticsService.infoEvent("HAGRID_PROCESSOR_SERVICE",  "_message", "returning because processor service is completed", "uuid", uuid, "namespace" ,namespace.getNamespace());
                    this.syncStatusService.setProcessorInSuccessful();
                }

                else{
                    analyticsService.warnEvent("HAGRID_PROCESSOR_SERVICE",  "_message", "returning because processor service is completed with errors", "uuid", uuid, "namespace" ,namespace.getNamespace());
                    this.syncStatusService.setProcessorInFailed();
                }
            }
        }

        catch (Exception e){
            analyticsService.errorEvent("HAGRID_PROCESSOR_SERVICE",  "_message", e.getClass().getName() + ": " + e.getMessage(), "stacktrace" , Throwables.getStackTraceAsString(e), "uuid", uuid, "namespace" ,namespace.getNamespace());
            this.syncStatusService.setProcessorInFailed();
        }

        finally {

            phaser.arriveAndDeregister();
            MDC.clear();

            // Clearing the thread interrupt flag if it is set so that when executor service lend this thread to some other task then it should have this flag cleared.
            Thread.interrupted();
        }

        return null;
    }

    public void configure(String parentServicePath, Phaser phaser, SyncServiceContainer syncServiceContainer, AssetBeanDependencyService assetBeanDependencyService, AssetAssetDependencyService assetAssetDependencyService, InfraService infraService, SyncStatusService syncStatusService, ProcessorConfigService processorConfigService) throws Exception {
        uuid = parentServicePath + "/" + UUID.randomUUID();
        this.serviceTree = syncServiceContainer.getBean(ServiceTree.class);
        this.syncServiceContainer = syncServiceContainer;
        AnalyticsFactory analyticsFactory = syncServiceContainer.getBean(AnalyticsFactory.class);
        this.namespace = syncServiceContainer.getBean(Namespace.class);
        this.analyticsService = analyticsFactory.getAnalyticsService(this.namespace.getNamespace());
        this.processorConfigService = processorConfigService;
        this.infraService = infraService;
        this.syncStatusService = syncStatusService;
        this.assetBeanDependencyMap = assetBeanDependencyService.scanner(this.namespace.getNamespace(), this.processorConfigService);
        this.assetAssetDependencyMap = assetAssetDependencyService.scanner(this.namespace.getNamespace(), this.processorConfigService);
        this.phaser = phaser;   
        mainThreadMdcCopy = MDC.getCopyOfContextMap();
        this.processTaskTracker = new ProcessTaskTracker();

    }

    public void run() throws Exception {
        String threadName = "ProcessorService" + "_" + Thread.currentThread().getName();
        Thread.currentThread().setName(threadName);

        analyticsService.debugEvent("HAGRID_PROCESSOR_SERVICE", "action", "started");

        this.jsonIndexService = infraService.getJsonIndexService();

        int processorPollCount = this.processorConfigService.getProcessorPollCount();
        int numberOfParallelProcessor = this.processorConfigService.getNumberOfParallelProcessor();

        if(numberOfParallelProcessor == 0 ){
            numberOfParallelProcessor = 10;
        }

        if(processorPollCount == 0){
            processorPollCount = 100;
        }

        InfraDbQueue processorQueue = infraService.getProcessorQueue();
        this.syncStatusService.setProcessorInProgress();
        int numberOfProcessorTaskScheduled = 0;

        // Self register to this phaser
        phaser.register();
        while(processorQueue.hasMoreData()  && Boolean.FALSE.equals(Thread.interrupted())){

            if(numberOfProcessorTaskScheduled < numberOfParallelProcessor){

                List<String> sList = processorQueue.poll(processorPollCount);
                phaser.register();
                ProcessorTaskService processorTask = getProcessorTask();
                String parentPath = uuid + "/" + "processor_service_task";
                processorTask.configure(parentPath, sList, syncServiceContainer, this.analyticsService, assetBeanDependencyMap, assetAssetDependencyMap, this.processorConfigService, this.bloomFilter, this.infraService, this.jsonIndexService , this.noopJoinService, this.leftJoinService, this.innerJoinService, this.syncStatusService, this.freshIndexObjectMapper, phaser, processTaskTracker);
                processorExecutorService.submit(namespace.getNamespace(), processorTask);
                numberOfProcessorTaskScheduled = numberOfProcessorTaskScheduled + 1;
            }
            else{
                phaser.arriveAndAwaitAdvance();
                numberOfProcessorTaskScheduled = 0;
            }
        }
    }

    @BetaRelease(sourceVersion = "3.6.0", useCase = "shutdown sync gracefully", message = "this is in beta and under testing")
    public boolean interruptSync() throws Exception {
        processorExecutorService.interruptSync( namespace.getNamespace());
        processorExecutorService.destroy(namespace.getNamespace());
        return true;
    }

    @Lookup
    public ProcessorTaskService getProcessorTask() {
        return null;
    }

    public static class ProcessTaskTracker{

        AtomicLong totalProcessTask = new AtomicLong(0);
        AtomicLong totalSuccessfulTask = new AtomicLong(0);
        AtomicLong totalFailedTask = new AtomicLong(0);


        public void incrementTotalProcessTask() {
            totalProcessTask.incrementAndGet();
        }
        public void incrementTotalSuccessfulTask() {
            totalSuccessfulTask.incrementAndGet();
        }
        public void incrementTotalFailedTask() {
            totalFailedTask.incrementAndGet();
        }

        public long getTotalProcessTask() {
            return totalProcessTask.get();
        }
        public long getTotalSuccessfulTask() {
            return totalSuccessfulTask.get();
        }
        public long getTotalFailedTask() {
            return totalFailedTask.get();
        }
    }
}

