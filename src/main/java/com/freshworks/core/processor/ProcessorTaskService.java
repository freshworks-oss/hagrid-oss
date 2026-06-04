package com.freshworks.core.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshworks.core.processor.Annotations.FreshAsset;
import com.freshworks.core.processor.Annotations.FreshJoin;
import com.freshworks.core.processor.joins.AbstractJoinService;
import com.freshworks.core.shared.Namespace;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.analytics.AnalyticsService;
import com.freshworks.core.shared.infra.InfraService;
import com.freshworks.core.shared.sync.SyncStatusService;
import com.freshworks.core.shared.synchronizers.ServiceTree;
import com.freshworks.freshindex.index.JsonIndexService;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.hash.BloomFilter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Phaser;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
@Component
@Scope("prototype")
public class ProcessorTaskService implements Callable<Void> {

    @Getter
    String uuid;

    SyncServiceContainer syncServiceContainer;

    AnalyticsService analyticsService;

    ProcessorConfigService processorConfigService;

    BloomFilter<String> bloomFilter;

    ProcessorExecutorService processorExecutorService;

    JsonIndexService jsonIndexService;

    InfraService infraService;

    ImmutableListMultimap<String, String> assetBeanDependencyMap;

    ImmutableListMultimap<String, String> assetAssetDependencyMap;

    @Qualifier("NoopJoinService")
    AbstractJoinService noopJoinService;

    @Qualifier("LeftJoinService")
    AbstractJoinService leftJoinService;

    @Qualifier("InnerJoinService")
    AbstractJoinService innerJoinService;

    SyncStatusService syncStatusService;

    ObjectMapper objectMapper = new ObjectMapper();
    ObjectMapper freshIndexObjectMapper;

    Map<String, String> mainThreadMdcCopy;

    ServiceTree serviceTree;

    Phaser phaser;

    List<String> itemList;

    Namespace namespace;

    MeterRegistry meterRegistry;

    List<AbstractAsset> assetsReadyToBePublishedList = new ArrayList<>();

    ProcessorService.ProcessTaskTracker processTaskTracker;

    LinkedList<AbstractAsset> globalGeneratedAssetList = new LinkedList<>();
    LinkedList<AbstractAsset> tempListOfNewlyGeneratedAssets = new LinkedList<>();


    public ProcessorTaskService() {
    }

    public void configure(String parentPath, List<String> s, SyncServiceContainer syncServiceContainer,
            AnalyticsService analyticsService, ImmutableListMultimap<String, String> assetBeanDependencyMap, ImmutableListMultimap<String, String> assetAssetDependencyMap,
            ProcessorConfigService processorConfigService, BloomFilter<String> bloomFilter, InfraService infraService,
            JsonIndexService jsonIndexService, AbstractJoinService noopJoinService, AbstractJoinService leftJoinService,
            AbstractJoinService innerJoinService, SyncStatusService syncStatusService,
            ObjectMapper freshIndexObjectMapper, Phaser phaser,
            ProcessorService.ProcessTaskTracker processTaskTracker) {

        uuid = parentPath + "/" + UUID.randomUUID();

        this.analyticsService = analyticsService;
        this.processorConfigService = processorConfigService;
        this.bloomFilter = bloomFilter;
        this.infraService = infraService;
        this.jsonIndexService = jsonIndexService;
        this.noopJoinService = noopJoinService;
        this.leftJoinService = leftJoinService;
        this.innerJoinService = innerJoinService;
        this.syncStatusService = syncStatusService;
        this.freshIndexObjectMapper = freshIndexObjectMapper;
        this.assetBeanDependencyMap = assetBeanDependencyMap;
        this.assetAssetDependencyMap = assetAssetDependencyMap;
        this.syncServiceContainer = syncServiceContainer;
        this.meterRegistry = syncServiceContainer.getBean(MeterRegistry.class);
        this.namespace = this.syncServiceContainer.getBean(Namespace.class);
        this.serviceTree = this.syncServiceContainer.getBean(ServiceTree.class);
        this.phaser = phaser;
        this.itemList = s;
        this.processTaskTracker = processTaskTracker;

        mainThreadMdcCopy = MDC.getCopyOfContextMap();
    }

    @Override
    public Void call() throws Exception {

        try {

            analyticsService.infoEvent("HAGRID_PROCESSOR_TASK_SERVICE", "_message", "ProcessorServiceTask started",
                    "uuid", uuid, "namespace", namespace.getNamespace());
            processTaskTracker.incrementTotalProcessTask();
            MDC.setContextMap(mainThreadMdcCopy);
            // serviceTree.register(uuid);
            for (String bean : itemList) {

                if (Boolean.FALSE.equals(Thread.interrupted())) {
                    List<AbstractAsset> abstractAssetList = processBeanForAsset(bean);
                    tempListOfNewlyGeneratedAssets.addAll(abstractAssetList);

                    while(!tempListOfNewlyGeneratedAssets.isEmpty()) {
                        globalGeneratedAssetList.clear();
                        globalGeneratedAssetList.addAll(tempListOfNewlyGeneratedAssets);
                        tempListOfNewlyGeneratedAssets.clear();
                        for (AbstractAsset abstractAsset : globalGeneratedAssetList) {
                            processAssetForAsset(abstractAsset);
                        }
                    }


                } else {
                    // If thread is interrupted or asked to terminate then skip the list and publish
                    // whatever assets are generated
                    break;
                }
            }
            // Publish abstract assets of all items received by this process task
            ProcessorUtility.publishAbstractAsset(uuid, assetsReadyToBePublishedList, infraService, jsonIndexService, namespace, analyticsService, freshIndexObjectMapper, meterRegistry);

            if (Thread.interrupted()) {

                throw new InterruptedException("leaving process without completing as thread is interrupted");
            }

            else {
                processTaskTracker.incrementTotalSuccessfulTask();
                analyticsService.infoEvent("HAGRID_PROCESSOR_TASK_SERVICE", "_message", "returning because processor task is completed", "uuid", uuid, "namespace", namespace.getNamespace());
            }

        } catch (Exception e) {

            processTaskTracker.incrementTotalFailedTask();
            analyticsService.errorEvent("HAGRID_PROCESSOR_TASK_SERVICE", "_message", e.getMessage(), "stacktrace", Throwables.getStackTraceAsString(e), "uuid", uuid, "namespace", namespace.getNamespace());
        } finally {

            MDC.clear();

            // Clearing the thread interrupt flag if it is set so that when executor service
            // lend this thread to some other task then it should have this flag cleared.
            Thread.interrupted();

            phaser.arriveAndDeregister();
        }
        return null;
    }

    // Main method to create new assets from this bean
    protected List<AbstractAsset> processBeanForAsset(String bean) throws Exception {

        ArrayList<AbstractAsset> generatedAssetList = new ArrayList<>();

        checkArgument(!Strings.isNullOrEmpty(bean), "Input object can not be null. It must be not null");
        AbstractBean abstractBean = objectMapper.readValue(bean, AbstractBean.class);

        Set<String> assetBeanDependencyKeySet = assetBeanDependencyMap.keySet();

        for (String asset : assetBeanDependencyKeySet) {
            Class<?> assetClass = ProcessorUtility.getClassByClassName(asset);

            // Using this check to ignore the Assets which I do not want to continue in
            // debugging mode
            FreshAsset freshAsset = assetClass.getAnnotation(FreshAsset.class);
            if (freshAsset != null && freshAsset.ignore()) {
                continue;
            }

            AbstractAsset abstractAssetClassObject = null;
            List<String> assetBeanDependencyList = ProcessorUtility.getAssetBeanDependencyList(asset, assetBeanDependencyMap);
            checkArgument(assetBeanDependencyList.size() > 0, "A assets must be dependent on atleast one bean");

            abstractAssetClassObject = processPrimitiveAsset(asset, abstractBean, assetBeanDependencyList);

            if (abstractAssetClassObject != null){
                
                generatedAssetList.add(abstractAssetClassObject);
            }

        } // for loop is done

        return generatedAssetList;
            
    }


    // Main method to create new assets from this abstractAsset
    protected void processAssetForAsset(AbstractAsset abstractAsset) throws Exception {

        Set<String> assetAssetDependencyKeySet = assetAssetDependencyMap.keySet();

        for (String asset : assetAssetDependencyKeySet) {
            Class<?> assetClass = ProcessorUtility.getClassByClassName(asset);

            // Using this check to ignore the Assets which I do not want to continue in
            // debugging mode
            FreshAsset freshAsset = assetClass.getAnnotation(FreshAsset.class);
            if (freshAsset != null && freshAsset.ignore()) {
                continue;
            }

            List<String> assetAssetDependencyList = ProcessorUtility.getAssetAssetDependencyList(asset, assetAssetDependencyMap);
            checkArgument(assetAssetDependencyList.size() > 0, "A assets must be dependent on atleast one bean");

            List<AbstractAsset> newlyGeneratedAssets = processNonPrimitiveAsset(asset, abstractAsset, assetAssetDependencyList);
            tempListOfNewlyGeneratedAssets.addAll(newlyGeneratedAssets);
            
        } // Creation of non primitive is happening in continuous loop here 

    }

    private AbstractAsset processPrimitiveAsset(String asset, AbstractBean abstractBean, List<String> assetBeanDependencyList) throws Exception{

        AbstractAsset abstractAssetClassObject = null;

        AbstractJoinService abstractJoinService = this.noopJoinService;
        abstractAssetClassObject = abstractJoinService.getPrimitiveAsset(asset, abstractBean, assetBeanDependencyList);
        if (abstractAssetClassObject != null) {

            // adding container to syncServiceContainer
            abstractAssetClassObject.configure(syncServiceContainer);

            String beanClassName = abstractBean.getClass().getName();
            String assetName = abstractAssetClassObject.getClass().getName();
            analyticsService.debugEvent("HAGRID_PROCESSOR_TASK_SERVICE", "bean", beanClassName, "asset", abstractAssetClassObject.getClass().getName(), "join", "noop");

            analyticsService.debugEvent("HAGRID_PROCESSOR_TASK_SERVICE", "bean", beanClassName, "asset", abstractAssetClassObject.getClass().getName(), "method", "filter");
            Boolean shouldFilter = ProcessorUtility.shouldFilterAsset(abstractAssetClassObject);

            analyticsService.debugEvent("HAGRID_PROCESSOR_TASK_SERVICE", "bean", beanClassName, "asset",
            abstractAssetClassObject.getClass().getName(), "method", "filter");
            if (Boolean.TRUE.equals(shouldFilter)) {

                analyticsService.debugEvent("HAGRID_PROCESSOR_TASK_SERVICE", "bean", beanClassName, "asset", abstractAssetClassObject.getClass().getName(), "method", "transform", "uuid", uuid, "namespace", namespace.getNamespace());
                abstractAssetClassObject.transform();
                
                analyticsService.debugEvent("HAGRID_PROCESSOR_TASK_SERVICE", "bean", beanClassName, "asset", abstractAssetClassObject.getClass().getName(), "method", "transform", "uuid", uuid, "namespace", namespace.getNamespace());
                assetsReadyToBePublishedList.add(abstractAssetClassObject);
            }
        }

        return abstractAssetClassObject;
    }

    private List<AbstractAsset> processNonPrimitiveAsset(String asset, AbstractAsset abstractAsset, List<String> assetAssetDependencyList) throws Exception{

        List<AbstractAsset> newlyGeneratedAssets = new ArrayList<>();
        AbstractAsset abstractAssetClassObject = null;

        Class<?> assetClass = ProcessorUtility.getClassByClassName(asset);
        FreshJoin freshJoin = assetClass.getAnnotation(FreshJoin.class);

        HashSet<String> listOfLeftClassNameInFreshJoin = ProcessorUtility.getFreshJoinLeftClassNameList(freshJoin);

        if (freshJoin.leftClass().getName().contains(abstractAsset.getClass().getName()) || freshJoin.rightClass().getName().contains(abstractAsset.getClass().getName())) {
            
            AbstractJoinService abstractJoinService = null;

            if (freshJoin.join_type() == FreshJoin.JOIN_TYPE.INNER_JOIN) {
                abstractJoinService = innerJoinService;
            } 
            
            else if (freshJoin.join_type() == FreshJoin.JOIN_TYPE.LEFT_JOIN) {
                
                abstractJoinService = leftJoinService;
            } 
            
            else {
                throw new RuntimeException("Right join is not supported");
            }

            List<AbstractAsset> assetList = abstractJoinService.getNonPrimitiveAsset(infraService.getKeyValue(), asset, abstractAsset, assetAssetDependencyList, freshJoin);
            
            String assetClassName = abstractAsset.getClass().getName();
            String freshJoinClassName = freshJoin.getClass().getName();

            for (int i = 0; i < assetList.size(); i++) {
                
                abstractAssetClassObject = (AbstractAsset) assetList.get(i);
                String assetName = abstractAssetClassObject.getClass().getName();

                if (abstractAssetClassObject != null) {

                    // adding container to syncServiceContainer
                    abstractAssetClassObject.configure(syncServiceContainer);

                    analyticsService.debugEvent("HAGRID_PROCESSOR_TASK_SERVICE", "asset", assetClassName,"asset", assetName, "method", "filter", "uuid", uuid, "namespace", namespace.getNamespace());
                        
                    Boolean shouldFilter = ProcessorUtility.shouldFilterAsset(abstractAssetClassObject);
                    analyticsService.debugEvent("HAGRID_PROCESSOR_TASK_SERVICE", "asset", assetClassName, "asset", assetName, "method", "filter", "uuid", uuid, "namespace", namespace.getNamespace());

                    if (Boolean.TRUE.equals(shouldFilter)) {
                        analyticsService.debugEvent("HAGRID_PROCESSOR_TASK_SERVICE", "asset", assetClassName, "asset", assetName, "join", "left or inner", "uuid", uuid, "namespace", namespace.getNamespace());

                        analyticsService.debugEvent("HAGRID_PROCESSOR_TASK_SERVICE", "asset", assetClassName,"asset", assetName, "method", "transform", "uuid", uuid, "namespace", namespace.getNamespace());        
                            
                        abstractAssetClassObject.transform();
                                    
                        analyticsService.debugEvent("HAGRID_PROCESSOR_TASK_SERVICE", "asset", assetClassName, "asset", assetName, "method", "transform", "uuid", uuid, "namespace",namespace.getNamespace());
                            
                        newlyGeneratedAssets.add(abstractAssetClassObject);
                        assetsReadyToBePublishedList.add(abstractAssetClassObject);
                            
                    }// shouldFilter close 
                } // AbstractAsset object not null
            } // Loop through all assets close
        }

        return newlyGeneratedAssets;
        
    } // creation of non primitive asset is close here 

    public static void shutdownNow() throws InterruptedException {
        // TODO: Do the graceful shutdown of the processor
        throw new InterruptedException("TraverserService process got interrupted");
    }

}
