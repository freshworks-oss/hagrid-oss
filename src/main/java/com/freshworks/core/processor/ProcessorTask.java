package com.freshworks.core.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshworks.core.processor.Annotations.FreshAsset;
import com.freshworks.core.processor.Annotations.FreshJoin;
import com.freshworks.core.processor.joins.AbstractJoinService;
import com.freshworks.core.shared.Namespace;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.analytics.AnalyticsService;
import com.freshworks.core.shared.infra.InfraDbQueue;
import com.freshworks.core.shared.infra.InfraService;
import com.freshworks.core.shared.infra.persistent.MongoDbQueue;
import com.freshworks.core.shared.sync.SyncStatusService;
import com.freshworks.core.shared.synchronizers.ServiceTree;
import com.freshworks.freshindex.index.JsonIndexService;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.hash.BloomFilter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.observation.annotation.Observed;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicLong;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
@Component
@Scope("prototype")
public class ProcessorTask implements Callable<Void> {

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

    public ProcessorTask() {
    }

    public void configure(String parentPath, List<String> s, SyncServiceContainer syncServiceContainer,
            AnalyticsService analyticsService, ImmutableListMultimap<String, String> assetBeanDependencyMap,
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
            for (String item : itemList) {

                if (Boolean.FALSE.equals(Thread.interrupted())) {
                    processItem(item);
                } else {
                    // If thread is interrupted or asked to terminate then skip the list and publish
                    // whatever assets are generated
                    break;
                }
            }
            // Publish abstract assets of all items received by this process task
            publishAbstractAsset();

            if (Thread.interrupted()) {

                throw new InterruptedException("leaving process without completing as thread is interrupted");
            }

            else {
                processTaskTracker.incrementTotalSuccessfulTask();
                analyticsService.infoEvent("HAGRID_PROCESSOR_TASK_SERVICE", "_message",
                        "returning because processor task is completed", "uuid", uuid, "namespace",
                        namespace.getNamespace());
            }

        } catch (Exception e) {

            processTaskTracker.incrementTotalFailedTask();
            analyticsService.errorEvent("HAGRID_PROCESSOR_TASK_SERVICE", "_message", e.getMessage(), "stacktrace",
                    Throwables.getStackTraceAsString(e), "uuid", uuid, "namespace", namespace.getNamespace());
        } finally {

            MDC.clear();

            // Clearing the thread interrupt flag if it is set so that when executor service
            // lend this thread to some other task then it should have this flag cleared.
            Thread.interrupted();

            phaser.arriveAndDeregister();
        }
        return null;
    }

    protected void processItem(String item) throws Exception {

        checkArgument(!Strings.isNullOrEmpty(item), "Input object can not be null. It must be not null");
        AbstractBean abstractBean = objectMapper.readValue(item, AbstractBean.class);

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
            List<String> assetBeanDependencyList = getAssetBeanDependencyList(asset, assetBeanDependencyMap);
            checkArgument(assetBeanDependencyList.size() > 0, "A assets must be dependent on atleast one bean");

            FreshJoin freshJoin = assetClass.getAnnotation(FreshJoin.class);

            // NOOP bean is a bean which is not a part of lookup, neither listed in right
            // class nor in left class
            Boolean isNoopBean = false;
            if (freshJoin == null || ProcessorUtility.getLookupClassName(abstractBean, freshJoin) != null) {
                isNoopBean = false;
            }

            else {
                isNoopBean = true;
            }

            if (Boolean.TRUE.equals(isNoopBean)) {
                AbstractJoinService abstractJoinService = this.noopJoinService;
                abstractAssetClassObject = abstractJoinService.getAsset(asset, abstractBean, assetBeanDependencyList);
                if (abstractAssetClassObject != null) {

                    // adding container to syncServiceContainer
                    abstractAssetClassObject.configure(syncServiceContainer);

                    String beanClassName = abstractBean.getClass().getName();
                    String assetName = abstractAssetClassObject.getClass().getName();

                    analyticsService.debugEvent("HAGRID_PROCESSOR_TASK_SERVICE", "bean", beanClassName, "asset",
                            abstractAssetClassObject.getClass().getName(), "join", "noop");

                    analyticsService.debugEvent("HAGRID_PROCESSOR_TASK_SERVICE", "bean", beanClassName, "asset",
                            abstractAssetClassObject.getClass().getName(), "method", "filter");
                    Boolean shouldFilter = shouldFilterAsset(abstractAssetClassObject);
                    analyticsService.debugEvent("HAGRID_PROCESSOR_TASK_SERVICE", "bean", beanClassName, "asset",
                            abstractAssetClassObject.getClass().getName(), "method", "filter");
                    if (Boolean.TRUE.equals(shouldFilter)) {

                        analyticsService.debugEvent("HAGRID_PROCESSOR_TASK_SERVICE", "bean", beanClassName, "asset",
                                abstractAssetClassObject.getClass().getName(), "method", "transform", "uuid", uuid,
                                "namespace", namespace.getNamespace());
                        abstractAssetClassObject.transform();
                        analyticsService.debugEvent("HAGRID_PROCESSOR_TASK_SERVICE", "bean", beanClassName, "asset",
                                abstractAssetClassObject.getClass().getName(), "method", "transform", "uuid", uuid,
                                "namespace", namespace.getNamespace());

                        if (abstractAssetClassObject.publishAsBean() == null) {
                            assetsReadyToBePublished(abstractAssetClassObject);
                        } else {
                            AbstractBean newlyConvertedBean = objectMapper.convertValue(abstractAssetClassObject,
                                    abstractAssetClassObject.publishAsBean());
                            infraService.getProcessorQueue().add(objectMapper.writeValueAsString(newlyConvertedBean));
                        }
                    }
                }
            } else {
                HashSet<String> listOfLeftClassNameInFreshJoin = ProcessorUtility
                        .getFreshJoinLeftClassNameList(freshJoin);
                if (listOfLeftClassNameInFreshJoin.contains(abstractBean.getClass().getName())
                        || freshJoin.rightClass().getName().contains(abstractBean.getClass().getName())) {
                    AbstractJoinService abstractJoinService = null;
                    if (freshJoin.join_type() == FreshJoin.JOIN_TYPE.INNER_JOIN) {
                        abstractJoinService = innerJoinService;
                    } else if (freshJoin.join_type() == FreshJoin.JOIN_TYPE.LEFT_JOIN) {
                        abstractJoinService = leftJoinService;
                    } else {
                        throw new RuntimeException("Right join is not supported");
                    }

                    List<Optional<AbstractAsset>> optionalList = abstractJoinService.getAssetWithFreshJoin(
                            infraService.getKeyValue(), asset, abstractBean, assetBeanDependencyList, freshJoin);
                    String beanClassName = abstractBean.getClass().getName();
                    String freshJoinClassName = freshJoin.getClass().getName();

                    for (int i = 0; i < optionalList.size(); i++) {
                        if (optionalList.get(i).isPresent()) {
                            abstractAssetClassObject = (AbstractAsset) optionalList.get(i).get();
                            String assetName = abstractAssetClassObject.getClass().getName();

                            if (abstractAssetClassObject != null) {

                                // adding container to syncServiceContainer
                                abstractAssetClassObject.configure(syncServiceContainer);

                                analyticsService.debugEvent("HAGRID_PROCESSOR_TASK_SERVICE", "bean", beanClassName,
                                        "asset", assetName, "method", "filter", "uuid", uuid, "namespace",
                                        namespace.getNamespace());
                                Boolean shouldFilter = shouldFilterAsset(abstractAssetClassObject);
                                analyticsService.debugEvent("HAGRID_PROCESSOR_TASK_SERVICE", "bean", beanClassName,
                                        "asset", assetName, "method", "filter", "uuid", uuid, "namespace",
                                        namespace.getNamespace());

                                if (Boolean.TRUE.equals(shouldFilter)) {
                                    analyticsService.debugEvent("HAGRID_PROCESSOR_TASK_SERVICE", "bean", beanClassName,
                                            "asset", assetName, "join", "left or inner", "uuid", uuid, "namespace",
                                            namespace.getNamespace());
                                    analyticsService.debugEvent("HAGRID_PROCESSOR_TASK_SERVICE", "bean", beanClassName,
                                            "asset", assetName, "method", "transform", "uuid", uuid, "namespace",
                                            namespace.getNamespace());
                                    abstractAssetClassObject.transform();
                                    analyticsService.debugEvent("HAGRID_PROCESSOR_TASK_SERVICE", "bean", beanClassName,
                                            "asset", assetName, "method", "transform", "uuid", uuid, "namespace",
                                            namespace.getNamespace());
                                    String uniqueAssetIdentifier = (String) abstractAssetClassObject
                                            .getUniqueIdentifier();
                                    abstractAssetClassObject.setUniqueIdentifier(uniqueAssetIdentifier);
                                    if (abstractAssetClassObject.publishAsBean() == null) {
                                        assetsReadyToBePublished(abstractAssetClassObject);
                                    } else {
                                        AbstractBean newlyConvertedBean = objectMapper.convertValue(
                                                abstractAssetClassObject, abstractAssetClassObject.publishAsBean());
                                        infraService.getProcessorQueue()
                                                .add(objectMapper.writeValueAsString(newlyConvertedBean));
                                    }
                                }
                            } // here
                        }
                    }
                }
            }
        }
    }

    protected void assetsReadyToBePublished(AbstractAsset abstractAssetClassObject) throws Exception {
        assetsReadyToBePublishedList.add(abstractAssetClassObject);
    }

    // TODO: This method need to optimise for insertion into freshIndex and
    // addAndGetIndex
    protected void publishAbstractAsset() throws Exception {

        Timer timer = meterRegistry.timer("infra.execution.time", "type", "list", "name", "publisher_list");

        String errorString = timer.record(() -> {

            try {
                if (!assetsReadyToBePublishedList.isEmpty()) {
                    List<String> assetsReadyToBePublishedListInPublisherQueue = new ArrayList<>();
                    List<JsonNode> assetsReadyToBePublishedListInFreshIndex = new ArrayList<>();

                    for (AbstractAsset abstractAsset : assetsReadyToBePublishedList) {
                        assetsReadyToBePublishedListInPublisherQueue
                                .add(objectMapper.writeValueAsString(abstractAsset));
                        String s = freshIndexObjectMapper.writeValueAsString(abstractAsset);
                        JsonNode j = objectMapper.readTree(s);
                        assetsReadyToBePublishedListInFreshIndex.add(j);
                    }

                    List<Long> documentIdList = infraService.getPublisherList()
                            .addAndGetIndexBulk(assetsReadyToBePublishedListInPublisherQueue);
                    if (documentIdList.size() != assetsReadyToBePublishedList.size()) {
                        return "Assets ready to be published are not equal to assets published in publisher list";
                    }
                    analyticsService.meterCounterByIncrement("HAGRID_ASSET_IS_PUBLISHED",
                            assetsReadyToBePublishedList.size());
                    List<String> documentIdListString = new ArrayList<>();
                    for (Long id : documentIdList) {
                        documentIdListString.add(id.toString());
                    }

                    jsonIndexService.indexJsonStringBulk(assetsReadyToBePublishedListInFreshIndex,
                            documentIdListString);
                    assetsReadyToBePublishedList.clear();
                    return null;
                } else {
                    analyticsService.infoEvent("HAGRID_PROCESSOR_TASK_SERVICE", "_message", "No assets to be published",
                            "uuid", uuid, "namespace", namespace.getNamespace());
                    return null;
                }
            }

            catch (Exception e) {
                analyticsService.errorEvent("HAGRID_PROCESSOR_TASK_SERVICE", "_message", e.getMessage(), "uuid", uuid,
                        "namespace", namespace.getNamespace());
                return e.getMessage();
            }
        });

        if (errorString != null) {
            throw new Exception(errorString);
        }
    }

    public static void shutdownNow() throws InterruptedException {
        // TODO: Do the graceful shutdown of the processor
        throw new InterruptedException("TraverserService process got interrupted");
    }

    protected Boolean isAssetDependsOnThisBean(List<String> assetStepDependencyList, AbstractBean abstractBean) {
        return assetStepDependencyList.contains(abstractBean.getClass().getName());
    }

    protected List<String> getAssetBeanDependencyList(String asset, Multimap<String, String> assetBeanDependencyMap)
            throws IOException {
        return (List<String>) assetBeanDependencyMap.get(asset);

    }

    protected Boolean shouldFilterAsset(AbstractAsset abstractAssetClassObject) {
        Optional<Boolean> opt = abstractAssetClassObject.filter();
        if (opt.isPresent() && Boolean.TRUE.equals(opt.get())) {
            return true;
        } else {
            return false;
        }
    }
}
