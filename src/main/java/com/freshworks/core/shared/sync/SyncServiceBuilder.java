//package com.freshworks.core.shared.sync;
//
//import com.freshworks.core.processor.AssetBeanDependencyService;
//import com.freshworks.core.processor.ProcessorConfigService;
//import com.freshworks.core.processor.ProcessorExecutorService;
//import com.freshworks.core.processor.ProcessorService;
//import com.freshworks.core.shared.Annotations.BetaRelease;
//import com.freshworks.core.shared.Namespace;
//import com.freshworks.core.shared.SyncServiceContainer;
//import com.freshworks.core.shared.analytics.AnalyticsFactory;
//import com.freshworks.core.shared.analytics.AnalyticsService;
//import com.freshworks.core.shared.consumer.ConsumerService;
//import com.freshworks.core.shared.executor.SharedExecutorService;
//import com.freshworks.core.shared.infra.InfraBeanService;
//import com.freshworks.core.shared.infra.InfraConfigService;
//import com.freshworks.core.shared.infra.InfraService;
//import com.freshworks.core.shared.synchronizers.GlobalNamespaceService;
//import com.freshworks.core.traverser.*;
//import com.freshworks.core.traverser.configuration.DagService;
//import com.freshworks.core.traverser.net.http.HttpClientService;
//import com.freshworks.freshindex.index.query.JsonQueryService;
//import com.google.common.base.Preconditions;
//import com.google.common.collect.ImmutableMap;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.ApplicationContext;
//import org.springframework.context.annotation.Scope;
//import org.springframework.stereotype.Component;
//
//import java.io.IOException;
//import java.util.Map;
//import java.util.concurrent.Phaser;
//
//
//@Component
//@Scope(value = "prototype")
//@Slf4j
//
//// TODO: We should have this builder class which will allow developer to customize Hagrid configuration dynamically.
//// TODO: Dynamic configuration could be stepPath, beanPath, assetPath, stepRateLimit, InfraType, Dag modifications, baggageMap
//// TODO: All of these configurations can be done using this builder class.
//// TODO: If we go with previous approach of returning SyncContainer and then allow dev to configure things there, it would be error prone
//// TODO: because when we change say stepPath in traverse config then there are lots of other configurations needs to be redone.
//
//public class SyncServiceBuilder {
//
//    String namespaceStr;
//    String stepPath;
//    String beanPath;
//    String assetPath;
//    Class<? extends AbstractStep> stepClass;
//    ImmutableMap<String, String> baggageMap;
//
//    ApplicationContext applicationContext;
//
//    GlobalNamespaceService singletonUniqueIdentifier;
//
//    SharedExecutorService sharedExecutorService;
//
//    DagTraversalService dagTraversalService;
//
//    NodeCycleService nodeCycleService;
//
//    TraverseConfigService traverseConfigService;
//
//    ProcessorService processorService;
//
//    ProcessorConfigService processorConfigService;
//
//    InfraService infraService;
//
//    JsonQueryService jsonQueryService;
//
//    InfraConfigService infraConfigService;
//
//    SyncStatusService syncStatusService;
//
//    SyncServiceContainer syncServiceContainer;
//
//    DagService dagService;
//
//    AssetBeanDependencyService assetBeanDependencyService;
//
//    TraverserExecutorService traverserExecutorService;
//
//    ProcessorExecutorService processorExecutorService;
//
//    ConsumerService consumerService;
//
//    HttpClientService httpClientService;
//
//    AnalyticsFactory analyticsFactory;
//
//    Namespace namespace;
//
//    @Autowired
//    public SyncServiceBuilder(ApplicationContext applicationContext){
//        this.applicationContext = applicationContext;
//    }
//
//
//    public SyncServiceBuilder namespace(String namespace){
//        this.namespaceStr = namespace;
//        return this;
//    }
//
//    public SyncServiceBuilder stepPath(String stepPath){
//        this.stepPath = stepPath;
//        return this;
//    }
//
//    public SyncServiceBuilder beanPath(String beanPath){
//        this.beanPath = beanPath;
//        return this;
//    }
//
//    public SyncServiceBuilder assetPath(String assetPath){
//        this.assetPath = assetPath;
//        return this;
//    }
//
//    public SyncServiceBuilder stepClass(Class<? extends AbstractStep> stepClass){
//        this.stepClass = stepClass;
//        return this;
//    }
//
//    public SyncServiceBuilder baggageMap(Map<String, String> baggageMap){
//        this.baggageMap = ImmutableMap.copyOf(baggageMap);
//        return this;
//    }
//
//    public SyncServiceContainer build() throws Exception {
//
//        Preconditions.checkArgument(namespaceStr != null, "namespace required");
//
//        // START: Moved classes from syncService constructor
//
//        // Init the sync container
//        this.syncServiceContainer = applicationContext.getBean(SyncServiceContainer.class);
//        this.syncServiceContainer.add(this);
//
//        // Add unique Identifier
//        this.singletonUniqueIdentifier = applicationContext.getBean(GlobalNamespaceService.class);
//        this.syncServiceContainer.add(this);
//
//        // Init namespace
//        this.namespace = applicationContext.getBean(Namespace.class);
//        this.namespace.setNamespace(namespaceStr);
//        this.syncServiceContainer.add(this.namespace, Namespace.class);
//
//        // Analytics Factory
//        this.analyticsFactory   = applicationContext.getBean(AnalyticsFactory.class);
//        this.syncServiceContainer.add(this.analyticsFactory, AnalyticsFactory.class);
//
//
//        // Init SyncStatusService
//        this.syncStatusService = applicationContext.getBean(SyncStatusService.class);
//        this.syncStatusService.configure(syncServiceContainer);
//        this.syncServiceContainer.add(this.syncStatusService, SyncStatusService.class);
//
//        // Init Infra Module
//        this.infraConfigService = syncServiceContainer.getBean(InfraConfigService.class);
//        this.infraConfigService.configure(syncServiceContainer);
//
//        InfraBeanService infraBeanService = syncServiceContainer.getBean(InfraBeanService.class);
//        this.infraService = infraBeanService.getInfraService(infraConfigService);
//        this.infraService.configure(syncServiceContainer, infraConfigService);
//        this.syncServiceContainer.add(this.infraService, InfraService.class);
//
//        // Init Traverser Executor Module
//        this.traverserExecutorService = applicationContext.getBean(TraverserExecutorService.class);
//        this.syncServiceContainer.add(this.traverserExecutorService, TraverserExecutorService.class);
//
//
//        this.traverseConfigService = applicationContext.getBean(TraverseConfigService.class);
//        this.traverseConfigService.configure(syncServiceContainer);
//        this.syncServiceContainer.add(this.traverseConfigService, TraverseConfigService.class);
//
//    }
//
//
//    // TODO: When I try to set the step location, bean location dynamically ( i.e just before running the startSync), I am not able to do that using this method
//    // TODO: It is because, this method configure root node, everything based on steplocation from hagrid.yml
//    // TODO: After this, even if I change the stepLocation in travserserConfig, it wont have any effect
//    public SyncServiceContainer initSyncServiceContainer(String infraNameSpace, Class<? extends AbstractStep> stepClass, ImmutableMap<String, String> baggageMap) throws Exception{
//
//        // START: Moved classes from syncService constructor
//
//        // Init the sync container
//        this.syncServiceContainer = applicationContext.getBean(SyncServiceContainer.class);
//        this.syncServiceContainer.add(this);
//
//
//        // Add unique Identifier
//        this.singletonUniqueIdentifier = applicationContext.getBean(GlobalNamespaceService.class);
//        this.syncServiceContainer.add(this);
//
//
//        // Init namespace
//        this.namespace = applicationContext.getBean(Namespace.class);
//        this.namespace.setNamespace(infraNameSpace);
//        this.syncServiceContainer.add(this.namespace, Namespace.class);
//
//
//        // Analytics Factory
//        this.analyticsFactory   = applicationContext.getBean(AnalyticsFactory.class);
//        this.syncServiceContainer.add(this.analyticsFactory, AnalyticsFactory.class);
//
//
//        // Init SyncStatusService
//        this.syncStatusService = applicationContext.getBean(SyncStatusService.class);
//        this.syncStatusService.configure(syncServiceContainer);
//        this.syncServiceContainer.add(this.syncStatusService, SyncStatusService.class);
//
//
//        // Init Infra Module
//        this.infraConfigService = syncServiceContainer.getBean(InfraConfigService.class);
//        this.infraConfigService.configure(syncServiceContainer);
//
//        InfraBeanService infraBeanService = syncServiceContainer.getBean(InfraBeanService.class);
//        this.infraService = infraBeanService.getInfraService(infraConfigService);
//        this.infraService.configure(syncServiceContainer, infraConfigService);
//        this.syncServiceContainer.add(this.infraService, InfraService.class);
//
//
//        // Init Traverser Module
//        this.traverserExecutorService = applicationContext.getBean(TraverserExecutorService.class);
//        this.syncServiceContainer.add(this.traverserExecutorService, TraverserExecutorService.class);
//
//        this.traverseConfigService = applicationContext.getBean(TraverseConfigService.class);
//        this.traverseConfigService.configure(syncServiceContainer);
//        this.syncServiceContainer.add(this.traverseConfigService, TraverseConfigService.class);
//
//        this.dagService = applicationContext.getBean(DagService.class);
//        DagNode rootNode = this.dagService.dagScanner(namespace.getNamespace(), traverseConfigService, infraService);
//        syncServiceContainer.add(rootNode, DagNode.class);
//
//
//        this.httpClientService = applicationContext.getBean(HttpClientService.class);
//        this.syncServiceContainer.add(httpClientService, HttpClientService.class);
//
//
//        String parentDagTraverserServicePath = "/" + namespace.getNamespace() + "/" + "traverser" + "/" + "dag_traversal";
//        DagNode startingNode = rootNode.find(stepClass.getName());
//
//        // Init NodeCycle
//        this.nodeCycleService = applicationContext.getBean(NodeCycleService.class);
//        this.nodeCycleService.configure(parentDagTraverserServicePath, 1000 , namespace, startingNode, this.analyticsFactory);
//        this.syncServiceContainer.add(this.nodeCycleService, NodeCycleService.class);
//
//
//        this.dagTraversalService = applicationContext.getBean(DagTraversalService.class);
//        this.dagTraversalService.configure(parentDagTraverserServicePath, startingNode, baggageMap, new Phaser(), this.syncServiceContainer);
//        this.syncServiceContainer.add(this.dagTraversalService, DagTraversalService.class);
//
//        // Init Processor Module
//        this.processorExecutorService = applicationContext.getBean(ProcessorExecutorService.class);
//        this.syncServiceContainer.add(this.processorExecutorService, ProcessorExecutorService.class);
//
//        this.processorConfigService = applicationContext.getBean(ProcessorConfigService.class);
//        this.processorConfigService.configure(syncServiceContainer);
//        this.syncServiceContainer.add(processorConfigService, ProcessorConfigService.class);
//
//        this.assetBeanDependencyService = applicationContext.getBean(AssetBeanDependencyService.class);
//        this.syncServiceContainer.add(assetBeanDependencyService, AssetBeanDependencyService.class);
//
//        String parentProcessorServicePath = "/" + namespace.getNamespace() + "/" + "processor" + "/" + "processor_service";
//        this.processorService = applicationContext.getBean(ProcessorService.class);
//        this.processorService.configure(parentProcessorServicePath, new Phaser(), syncServiceContainer, assetBeanDependencyService, infraService, syncStatusService, processorConfigService);
//        this.syncServiceContainer.add(this.processorService, ProcessorService.class);
//
//
//
//        // Init Consumer Module
//        this.consumerService = applicationContext.getBean(ConsumerService.class);
//        this.consumerService.configure(syncServiceContainer);
//        this.syncServiceContainer.add(this.consumerService, ConsumerService.class);
//
//
//        // Init Shared Executor
//        this.sharedExecutorService = applicationContext.getBean(SharedExecutorService.class);
//        this.syncServiceContainer.add(this.sharedExecutorService, SharedExecutorService.class);
//
//        // END
//
//        return this.syncServiceContainer;
//    }
//
//    public void startSync(SyncServiceContainer syncServiceContainer) throws  Exception{
//
//        this.traverserExecutorService = syncServiceContainer.getBean(TraverserExecutorService.class);
//        this.processorExecutorService = syncServiceContainer.getBean(ProcessorExecutorService.class);
//        this.nodeCycleService = syncServiceContainer.getBean(NodeCycleService.class);
//
//        Namespace namespace = syncServiceContainer.getBean(Namespace.class);
//        traverserExecutorService.submit(namespace.getNamespace(), this.dagTraversalService);
//        traverserExecutorService.submit(namespace.getNamespace(), this.nodeCycleService);
//        processorExecutorService.submit(namespace.getNamespace(), this.processorService);
//
//    }
//
//    public SyncServiceContainer startSync(Class<? extends AbstractStep> stepClass, String infraNameSpace, ImmutableMap<String, String> baggageMap) throws Exception {
//
//
//        this.syncServiceContainer = initSyncServiceContainer(infraNameSpace, stepClass, baggageMap);
//        startSync(syncServiceContainer);
//        return syncServiceContainer;
//    }
//
//    @BetaRelease(sourceVersion = "4.0.0", useCase = "Abort Sync by interrupting threads", message = "this is in beta and under testing")
//    public void interruptSync() throws Exception {
//
//        // This will interrupt the threads of both processor and traverser
//        dagTraversalService.interruptSync();
//        processorService.interruptSync();
//    }
//
//    @BetaRelease(sourceVersion = "4.0.0", useCase = "shutdown sync gracefully", message = "this is in beta and under testing")
//    public void shutdown() throws Exception {
//        AnalyticsService analyticsService = analyticsFactory.getAnalyticsService(namespace.getNamespace());
//
//        analyticsService.warnEvent("HAGRID_SYNC_SERVICE", "namespace", namespace, "_message", "this method MUST NOT be called from any of the step methods. For shutting down Sync from steps method, please call interruptSync method of SyncService class");
//
//        // This will interrupt the threads of both processor and traverser
//        dagTraversalService.interruptSync();
//        processorService.interruptSync();
//
//
//        // Wait until all threads have come back
//        syncStatusService.waitUntilSyncIsInProgress();
//
//        // Now destroy the infra
//        this.infraService.destroy();
//        this.syncServiceContainer.clear();
//        AnalyticsFactory analyticsFactory = syncServiceContainer.getBean(AnalyticsFactory.class);
//        String namespace = this.namespace.getNamespace();
//        analyticsFactory.destroy(namespace);
//    }
//}
//
//
//
//
//
//
//
