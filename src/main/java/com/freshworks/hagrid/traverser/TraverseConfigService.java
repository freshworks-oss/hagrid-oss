package com.freshworks.hagrid.traverser;

import java.io.IOException;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.freshworks.hagrid.shared.SyncServiceContainer;
import com.freshworks.hagrid.shared.sync.ConnectorConfiguration;
import com.freshworks.hagrid.traverser.DagNode.NodeRateLimitObject;

@Component
@Scope(value="prototype")
public class TraverseConfigService {

    SyncServiceContainer syncServiceContainer;
    ConnectorConfiguration connectorConfiguration;
    DagNode rootNode;

    public void configure(SyncServiceContainer syncServiceContainer) throws ClassNotFoundException, IllegalAccessException, IOException {
        this.syncServiceContainer = syncServiceContainer;
        this.connectorConfiguration = syncServiceContainer.getBean(ConnectorConfiguration.class);
    }

    public int getTraverserThreadCount() throws IOException {

        return connectorConfiguration.getTraverserThreadCount();
    }

    public NodeRateLimitObject getRateLimitForStep(DagNode dagNode) throws Exception{

       this.rootNode = this.syncServiceContainer.getBean(DagNode.class);
       NodeRateLimitObject nodeRateLimitObject =  this.connectorConfiguration.getNodeRateLimitObject(dagNode.getName());

       if(nodeRateLimitObject == null){
        return dagNode.getNodeRateLimitObject();
       }
       else{
        return nodeRateLimitObject;
       }
    }

    public NodeRateLimitObject getRateLimitForStep(Class<? extends AbstractStep> clazz) throws Exception{

      this.rootNode = this.syncServiceContainer.getBean(DagNode.class);
       DagNode node = this.rootNode.find(clazz.getName());
       return getRateLimitForStep(node);
    }

    public NodeRateLimitObject getRateLimitForStep(String clazzName) throws Exception{

       this.rootNode = this.syncServiceContainer.getBean(DagNode.class);
       DagNode node = this.rootNode.find(clazzName);
       return getRateLimitForStep(node);
    }
}
