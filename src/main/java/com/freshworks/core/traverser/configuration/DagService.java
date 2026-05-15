package com.freshworks.core.traverser.configuration;


import com.freshworks.core.shared.analytics.AnalyticsFactory;
import com.freshworks.core.shared.analytics.AnalyticsService;
import com.freshworks.core.shared.infra.InfraService;
import com.freshworks.core.traverser.DagNode;
import com.freshworks.core.traverser.DagScannerService;
import com.freshworks.core.traverser.TraverseConfigService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Component
public class DagService {

    ReentrantReadWriteLock.WriteLock uniqueScan = new ReentrantReadWriteLock().writeLock();
    HashMap<String, DagNode> scannedRootDagNode = new HashMap<>();
    DagScannerService dagScannerService;
    AnalyticsFactory analyticsFactory;

    public DagService(DagScannerService dagScannerService, AnalyticsFactory analyticsFactory){
        this.dagScannerService = dagScannerService;
        this.analyticsFactory = analyticsFactory;
    }

    public DagNode dagScanner(String namespace, TraverseConfigService traverseConfigService, InfraService infraService) throws Exception {

        try{
            uniqueScan.lock();
            String stepLocation = traverseConfigService.getStepLocation();
            // If Dag for the given stepLocation already exists then do not create it again
            if(scannedRootDagNode.containsKey(stepLocation)){
                DagNode clonedDagNode = cloneDag(stepLocation);
                init(clonedDagNode.preOrder(), infraService);
                return clonedDagNode.preOrder().get(0);
            }

            AnalyticsService analyticsService = analyticsFactory.getAnalyticsService(namespace);
            DagNode dagNode = dagScannerService.scanner(traverseConfigService, analyticsService);
            scannedRootDagNode.put(traverseConfigService.getStepLocation(),dagNode);
            DagNode clonedDagNode = cloneDag(dagNode);
            init(clonedDagNode.preOrder(), infraService);
            return clonedDagNode.preOrder().get(0);

        }

        finally {
            uniqueScan.unlock();
        }
    }

    private void init(List<DagNode> dagNodeList, InfraService infraService) throws Exception{

        for (DagNode node : dagNodeList) {
            node.configInfra(infraService.getInfraDbList(node.getShortName()), infraService.getKeyValue());
        }
    }

    protected DagNode cloneDag(String stepLocation) throws Exception{
        return DagNode.cloneDag(scannedRootDagNode.get(stepLocation));
    }

    protected DagNode cloneDag(DagNode node) throws Exception{
        return DagNode.cloneDag(node);
    }

}
