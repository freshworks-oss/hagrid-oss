package com.freshworks.core.shared.sync;

import java.util.concurrent.Phaser;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.freshworks.core.processor.AssetAssetDependencyService;
import com.freshworks.core.processor.AssetBeanDependencyService;
import com.freshworks.core.processor.ProcessorConfigService;
import com.freshworks.core.processor.ProcessorExecutorService;
import com.freshworks.core.processor.ProcessorService;
import com.freshworks.core.shared.NamespaceService;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.analytics.AnalyticsFactory;
import com.freshworks.core.shared.analytics.AnalyticsService;
import com.freshworks.core.shared.analytics.AppEventService;
import com.freshworks.core.shared.consumer.ConsumerService;
import com.freshworks.core.shared.executor.SharedExecutorService;
import com.freshworks.core.shared.infra.InfraBeanService;
import com.freshworks.core.shared.infra.InfraConfigService;
import com.freshworks.core.shared.infra.InfraService;
import com.freshworks.core.shared.synchronizers.GlobalNamespaceService;
import com.freshworks.core.traverser.AbstractStep;
import com.freshworks.core.traverser.DagNode;
import com.freshworks.core.traverser.DagService;
import com.freshworks.core.traverser.DagTraversalService;
import com.freshworks.core.traverser.NodeCycleService;
import com.freshworks.core.traverser.TraverseConfigService;
import com.freshworks.core.traverser.TraverserExecutorService;
import com.freshworks.core.traverser.net.http.HttpClientService;
import com.google.common.collect.ImmutableMap;

import lombok.extern.slf4j.Slf4j;


@Component
@Scope(value = "prototype")
@Slf4j
public class SyncService {

    ApplicationContext applicationContext;

    GlobalNamespaceService singletonUniqueIdentifier;

    SharedExecutorService sharedExecutorService;

    DagTraversalService dagTraversalService;

    NodeCycleService nodeCycleService;

    TraverseConfigService traverseConfigService;

    ProcessorService processorService;

    ProcessorConfigService processorConfigService;

    InfraService infraService;

    InfraConfigService infraConfigService;

    SyncStatusService syncStatusService;

    SyncServiceContainer syncServiceContainer;

    DagService dagScannerService;

    AssetBeanDependencyService assetBeanDependencyService;

    AssetAssetDependencyService assetAssetDependencyService;

    TraverserExecutorService traverserExecutorService;

    ProcessorExecutorService processorExecutorService;

    ConsumerService consumerService;

    HttpClientService httpClientService;

    NamespaceService namespace;

    AnalyticsFactory analyticsFactory;

    AppEventService appEventService;

    @Autowired
    public SyncService(ApplicationContext applicationContext){
        this.applicationContext = applicationContext;
    }

    public SyncServiceContainer startSync() throws Exception {

        this.traverserExecutorService = this.syncServiceContainer.getBean(TraverserExecutorService.class);
        this.processorExecutorService = this.syncServiceContainer.getBean(ProcessorExecutorService.class);
        this.nodeCycleService = syncServiceContainer.getBean(NodeCycleService.class);
        
        NamespaceService namespace = syncServiceContainer.getBean(NamespaceService.class);
        traverserExecutorService.submit(namespace.getNamespace(), this.dagTraversalService);
        traverserExecutorService.submit(namespace.getNamespace(), this.nodeCycleService);
        processorExecutorService.submit(namespace.getNamespace(), this.processorService);

        return syncServiceContainer;
    }

    protected SyncServiceContainer configureSync(String infraNameSpace, Class<? extends AbstractStep> stepClass, ImmutableMap<String, String> baggageMap, ConnectorConfiguration connectorConfiguration) throws Exception{

        // START: Moved classes from syncService constructor

        // Init the sync container
        this.syncServiceContainer = applicationContext.getBean(SyncServiceContainer.class);

        // Add Connector Configuration Object 
        this.syncServiceContainer.add(connectorConfiguration, ConnectorConfiguration.class);

        // Add unique Identifier
        this.singletonUniqueIdentifier = applicationContext.getBean(GlobalNamespaceService.class);
        this.syncServiceContainer.add(this.singletonUniqueIdentifier, GlobalNamespaceService.class);

        // Add AppEvent Service 
        this.appEventService = applicationContext.getBean(AppEventService.class);
        this.syncServiceContainer.add(this.appEventService, AppEventService.class);

        // Init namespace
        this.namespace = applicationContext.getBean(NamespaceService.class);
        this.namespace.setNamespace(infraNameSpace);
        this.syncServiceContainer.add(this.namespace, NamespaceService.class);


        // Analytics Factory
        this.analyticsFactory = applicationContext.getBean(AnalyticsFactory.class);
        this.syncServiceContainer.add(this.analyticsFactory, AnalyticsFactory.class);


        // Init SyncStatusService
        this.syncStatusService = applicationContext.getBean(SyncStatusService.class);
        this.syncStatusService.configure(syncServiceContainer);
        this.syncServiceContainer.add(this.syncStatusService, SyncStatusService.class);


        // Init Infra Module
        this.infraConfigService = syncServiceContainer.getBean(InfraConfigService.class);
        this.infraConfigService.configure(syncServiceContainer);

        InfraBeanService infraBeanService = syncServiceContainer.getBean(InfraBeanService.class);
        this.infraService = infraBeanService.getInfraService(infraConfigService, connectorConfiguration);
        this.infraService.configure(syncServiceContainer, infraConfigService);
        this.syncServiceContainer.add(this.infraService, InfraService.class);


        // Init Traverser Module
        this.traverserExecutorService = applicationContext.getBean(TraverserExecutorService.class);
        this.syncServiceContainer.add(this.traverserExecutorService, TraverserExecutorService.class);

        this.traverseConfigService = applicationContext.getBean(TraverseConfigService.class);
        this.traverseConfigService.configure(syncServiceContainer);
        this.syncServiceContainer.add(this.traverseConfigService, TraverseConfigService.class);

        this.dagScannerService = applicationContext.getBean(DagService.class);
        this.dagScannerService.configure(syncServiceContainer);
        DagNode rootNode = this.dagScannerService.dagScanner(namespace.getNamespace(), traverseConfigService, infraService);
        syncServiceContainer.add(rootNode, DagNode.class);


        this.httpClientService = applicationContext.getBean(HttpClientService.class);
        this.syncServiceContainer.add(httpClientService, HttpClientService.class);


        String parentDagTraverserServicePath = "/" + namespace.getNamespace() + "/" + "traverser" + "/" + "dag_traversal";
        DagNode startingNode = rootNode.find(stepClass.getName());

        // Init NodeCycle
        this.nodeCycleService = applicationContext.getBean(NodeCycleService.class);
        this.nodeCycleService.configure(parentDagTraverserServicePath, 1000 , namespace, startingNode, this.analyticsFactory);
        this.syncServiceContainer.add(this.nodeCycleService, NodeCycleService.class);


        this.dagTraversalService = applicationContext.getBean(DagTraversalService.class);
        this.dagTraversalService.configure(parentDagTraverserServicePath, startingNode, baggageMap, new Phaser(), this.syncServiceContainer);
        this.syncServiceContainer.add(this.dagTraversalService, DagTraversalService.class);

        // Init Processor Module
        this.processorExecutorService = applicationContext.getBean(ProcessorExecutorService.class);
        this.syncServiceContainer.add(this.processorExecutorService, ProcessorExecutorService.class);

        this.processorConfigService = applicationContext.getBean(ProcessorConfigService.class);
        this.processorConfigService.configure(syncServiceContainer);
        this.syncServiceContainer.add(processorConfigService, ProcessorConfigService.class);

        this.assetBeanDependencyService = applicationContext.getBean(AssetBeanDependencyService.class);
        this.assetBeanDependencyService.configure(syncServiceContainer);
        this.syncServiceContainer.add(assetBeanDependencyService, AssetBeanDependencyService.class);

        this.assetAssetDependencyService = applicationContext.getBean(AssetAssetDependencyService.class);
        this.syncServiceContainer.add(assetAssetDependencyService, AssetAssetDependencyService.class);

        String parentProcessorServicePath = "/" + namespace.getNamespace() + "/" + "processor" + "/" + "processor_service";
        this.processorService = applicationContext.getBean(ProcessorService.class);
        this.processorService.configure(parentProcessorServicePath, new Phaser(), syncServiceContainer, assetBeanDependencyService, assetAssetDependencyService, infraService, syncStatusService, processorConfigService);
        this.syncServiceContainer.add(this.processorService, ProcessorService.class);


        // Init Consumer Module
        this.consumerService = applicationContext.getBean(ConsumerService.class);
        this.consumerService.configure(syncServiceContainer);
        this.syncServiceContainer.add(this.consumerService, ConsumerService.class);


        // Init Shared Executor
        this.sharedExecutorService = applicationContext.getBean(SharedExecutorService.class);
        this.syncServiceContainer.add(this.sharedExecutorService, SharedExecutorService.class);

        // END

        return this.syncServiceContainer;
    }

    public void interruptSync() throws Exception {

        // This will interrupt the threads of both processor and traverser
        dagTraversalService.interruptSync();
        processorService.interruptSync();
    }

    public void shutdown() throws Exception {
        AnalyticsService analyticsService = analyticsFactory.getAnalyticsService(namespace.getNamespace());

        analyticsService.warnLogEvent("HAGRID_SYNC_SERVICE", "namespace", namespace, "_message", "this method MUST NOT be called from any of the step methods. For shutting down Sync from steps method, please call interruptSync method of SyncService class");

        // This will interrupt the threads of both processor and traverser
        dagTraversalService.interruptSync();
        processorService.interruptSync();


        // Wait until all threads have come back
        syncStatusService.waitUntilSyncIsInProgress();

        // Now destroy the infra
        this.infraService.destroy();
        this.syncServiceContainer.clear();
        AnalyticsFactory analyticsFactory = syncServiceContainer.getBean(AnalyticsFactory.class);
        String namespace = this.namespace.getNamespace();
        analyticsFactory.destroy(namespace);
    }
}







