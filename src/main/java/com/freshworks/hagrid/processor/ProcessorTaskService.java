package com.freshworks.hagrid.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshworks.hagrid.main.SharedActionServiceMap;
import com.freshworks.hagrid.main.assets.GenericAsset;
import com.freshworks.hagrid.main.beans.GenericBean;
import com.freshworks.hagrid.main.dsl.runnable.OutputModel;
import com.freshworks.hagrid.main.dsl.runnable.context.ActionContext;
import com.freshworks.hagrid.main.dsl.services.ActionService;
import com.freshworks.hagrid.main.dsl.services.OutputModelService;
import com.freshworks.hagrid.processor.Annotations.FreshAsset;
import com.freshworks.hagrid.processor.Annotations.FreshJoin;
import com.freshworks.hagrid.processor.joins.AbstractJoinService;
import com.freshworks.hagrid.shared.NamespaceService;
import com.freshworks.hagrid.shared.SyncServiceContainer;
import com.freshworks.hagrid.shared.analytics.AnalyticsService;
import com.freshworks.hagrid.shared.analytics.AppEventService;
import com.freshworks.hagrid.shared.infra.InfraService;
import com.freshworks.hagrid.shared.sync.SyncStatusService;
import com.freshworks.hagrid.shared.synchronizers.ServiceTree;
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

    Map<String, String> mainThreadMdcCopy;

    ServiceTree serviceTree;

    Phaser phaser;

    List<String> itemList;

    NamespaceService namespace;

    MeterRegistry meterRegistry;

    List<AbstractAsset> assetsReadyToBePublishedList = new ArrayList<>();

    ProcessorService.ProcessTaskTracker processTaskTracker;

    LinkedList<AbstractAsset> abstractAssetList = new LinkedList<>();

    AppEventService appEventService;

    OutputModelService outputModelService;

    public ProcessorTaskService() {
    }

    public void configure(String parentPath, List<String> s, SyncServiceContainer syncServiceContainer,
            AnalyticsService analyticsService, ImmutableListMultimap<String, String> assetBeanDependencyMap, ImmutableListMultimap<String, String> assetAssetDependencyMap,
            ProcessorConfigService processorConfigService, BloomFilter<String> bloomFilter, InfraService infraService,
            AbstractJoinService noopJoinService, AbstractJoinService leftJoinService,
            AbstractJoinService innerJoinService, OutputModelService outputModelService, SyncStatusService syncStatusService,Phaser phaser,
            ProcessorService.ProcessTaskTracker processTaskTracker) {

        uuid = parentPath + "/" + UUID.randomUUID();

        this.analyticsService = analyticsService;
        this.processorConfigService = processorConfigService;
        this.bloomFilter = bloomFilter;
        this.infraService = infraService;
        this.noopJoinService = noopJoinService;
        this.leftJoinService = leftJoinService;
        this.innerJoinService = innerJoinService;
        this.outputModelService = outputModelService;
        this.syncStatusService = syncStatusService;
        this.assetBeanDependencyMap = assetBeanDependencyMap;
        this.assetAssetDependencyMap = assetAssetDependencyMap;
        this.syncServiceContainer = syncServiceContainer;
        this.meterRegistry = syncServiceContainer.getBean(MeterRegistry.class);
        this.namespace = this.syncServiceContainer.getBean(NamespaceService.class);
        this.serviceTree = this.syncServiceContainer.getBean(ServiceTree.class);
        this.phaser = phaser;
        this.itemList = s;
        this.processTaskTracker = processTaskTracker;
        this.appEventService = syncServiceContainer.getBean(AppEventService.class);

        mainThreadMdcCopy = MDC.getCopyOfContextMap();
    }

    @Override
    public Void call() throws Exception {

        try {

            analyticsService.infoLogEvent("HAGRID_PROCESSOR_TASK_SERVICE", "_message", "ProcessorServiceTask started",
                    "uuid", uuid, "namespace", namespace.getNamespace());
            processTaskTracker.incrementTotalProcessTask();
            MDC.setContextMap(mainThreadMdcCopy);
            // serviceTree.register(uuid);

            // Clear abstractAssetList before using it, as it will be reused for every bean
            abstractAssetList.clear();
            for (String bean : itemList) {

                // Here I need to check if bean is DSL based bean or regular bean.
                // If bean is dsl based bean the asset creation will be done by ActionService
                JsonNode beanNode = objectMapper.readTree(bean);

                if(beanNode.has("isDslBasedBean")){

                    // It is DSL based bean
                    GenericBean genericBean = objectMapper.convertValue(beanNode, GenericBean.class);
                    ActionContext actionContext = genericBean.getActionContext();

                    List<OutputModel> outputModelList = this.outputModelService.getOutputModelFromBean(beanNode);

                    for(OutputModel outputModel : outputModelList){
                        GenericAsset genericAsset = syncServiceContainer.getBean(GenericAsset.class);
                        genericAsset.configure(syncServiceContainer);
                        genericAsset.setOutputModelService(outputModelService, actionContext);
                        genericAsset.setFromBean(genericBean);
                        genericAsset.filter();
                        genericAsset.transform();
                        assetsReadyToBePublishedList.add(genericAsset);
                    }
                }

                else{

                    // It is regular bean and it needs to processed by processor service as usual
                    if (Boolean.FALSE.equals(Thread.interrupted())) {
                        abstractAssetList = processBeanForAsset(bean);
                        while(true) {
                       
                            if (abstractAssetList.isEmpty()){
                                break;
                            }
                            processAssetForAsset(abstractAssetList.pop());           
                        }

                    } 
                    
                    else {
                        // If thread is interrupted or asked to terminate then skip the list and publish
                        // whatever assets are generated
                        break;
                    }
                }
            }
            // Publish abstract assets of all items received by this process task
            ProcessorUtility.publishAbstractAsset(uuid, assetsReadyToBePublishedList, infraService, namespace, analyticsService, meterRegistry, appEventService);

            if (Thread.interrupted()) {

                throw new InterruptedException("leaving process without completing as thread is interrupted");
            }

            else {
                processTaskTracker.incrementTotalSuccessfulTask();
                analyticsService.infoLogEvent("HAGRID_PROCESSOR_TASK_SERVICE", "_message", "returning because processor task is completed", "uuid", uuid, "namespace", namespace.getNamespace());
            }

        } catch (Exception e) {

            processTaskTracker.incrementTotalFailedTask();
            analyticsService.errorLogEvent("HAGRID_PROCESSOR_TASK_SERVICE", "_message", e.getMessage(), "stacktrace", Throwables.getStackTraceAsString(e), "uuid", uuid, "namespace", namespace.getNamespace());
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
    protected LinkedList<AbstractAsset> processBeanForAsset(String bean) throws Exception {

        LinkedList<AbstractAsset> generatedAssetList = new LinkedList<>();

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
            LinkedList<AbstractAsset> newlyGeneratedAssets = processNonPrimitiveAsset(asset, abstractAsset, assetAssetDependencyList);
            abstractAssetList.addAll(newlyGeneratedAssets);
            
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
            analyticsService.debugLogEvent("HAGRID_PROCESSOR_TASK_SERVICE", "bean", beanClassName, "asset", abstractAssetClassObject.getClass().getName(), "join", "noop");

            abstractAssetClassObject.transform();
            analyticsService.debugLogEvent("HAGRID_PROCESSOR_TASK_SERVICE", "bean", beanClassName, "asset", abstractAssetClassObject.getClass().getName(), "method", "transform", "uuid", uuid, "namespace", namespace.getNamespace());

            Boolean shouldFilter = ProcessorUtility.shouldFilterAsset(abstractAssetClassObject);
            analyticsService.debugLogEvent("HAGRID_PROCESSOR_TASK_SERVICE", "bean", beanClassName, "asset", abstractAssetClassObject.getClass().getName(), "method", "filter");
            if (Boolean.TRUE.equals(shouldFilter)) {

                analyticsService.debugLogEvent("HAGRID_PROCESSOR_TASK_SERVICE", "bean", beanClassName, "asset", abstractAssetClassObject.getClass().getName(), "type" , "published" , "uuid", uuid, "namespace", namespace.getNamespace());
                assetsReadyToBePublishedList.add(abstractAssetClassObject);

                analyticsService.meterCounter("HAGRID_ASSET_IS_PUBLISHED", "asset_name", abstractAssetClassObject.getClass().getName());
            }
        }

        return abstractAssetClassObject;
    }

    private LinkedList<AbstractAsset> processNonPrimitiveAsset(String asset, AbstractAsset abstractAsset, List<String> assetAssetDependencyList) throws Exception{

        LinkedList<AbstractAsset> newlyGeneratedAssets = new LinkedList<>();
        AbstractAsset abstractAssetClassObject = null;

        Class<?> assetClass = ProcessorUtility.getClassByClassName(asset);
        FreshJoin freshJoin = assetClass.getAnnotation(FreshJoin.class);

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

            List<AbstractAsset> assetList = abstractJoinService.getNonPrimitiveAsset(infraService.getKeyValue(), asset, abstractAsset, freshJoin);

            String assetClassName = abstractAsset.getClass().getName();

            for (int i = 0; i < assetList.size(); i++) {
                
                abstractAssetClassObject = (AbstractAsset) assetList.get(i);
                String assetName = abstractAssetClassObject.getClass().getName();

                if (abstractAssetClassObject != null) {

                    // adding container to syncServiceContainer
                    abstractAssetClassObject.configure(syncServiceContainer);
                    analyticsService.debugLogEvent("HAGRID_PROCESSOR_TASK_SERVICE", "from_asset", assetClassName,"to_asset", assetName, "method", "configure", "uuid", uuid, "namespace", namespace.getNamespace());
                    
                    abstractAssetClassObject.transform();
                    analyticsService.debugLogEvent("HAGRID_PROCESSOR_TASK_SERVICE", "from_asset", assetClassName, "to_asset", assetName, "method", "transform", "uuid", uuid, "namespace",namespace.getNamespace());

                    Boolean shouldFilter = ProcessorUtility.shouldFilterAsset(abstractAssetClassObject);
                    analyticsService.debugLogEvent("HAGRID_PROCESSOR_TASK_SERVICE", "from_asset", assetClassName, "to_asset", assetName, "method", "filter", "uuid", uuid, "namespace", namespace.getNamespace());
                    if (Boolean.TRUE.equals(shouldFilter)) {
                        analyticsService.debugLogEvent("HAGRID_PROCESSOR_TASK_SERVICE", "from_asset", assetClassName, "to_asset", assetName, "type" , "published",  "uuid", uuid, "namespace", namespace.getNamespace());

                        newlyGeneratedAssets.add(abstractAssetClassObject);
                        assetsReadyToBePublishedList.add(abstractAssetClassObject);

                        analyticsService.meterCounter("HAGRID_ASSET_IS_PUBLISHED", "asset_name", abstractAssetClassObject.getClass().getName());
                            
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
