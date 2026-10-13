package com.freshworks.core.traverser;

import java.io.IOException;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.sync.ConnectorConfiguration;
import com.freshworks.core.shared.sync.ConnectorConfiguration.StepRateLimitObject;

@Component
@Scope(value="prototype")
public class TraverseConfigService {

    SyncServiceContainer syncServiceContainer;
    ConnectorConfiguration connectorConfiguration;

    public void configure(SyncServiceContainer syncServiceContainer) throws ClassNotFoundException, IllegalAccessException, IOException {
        this.syncServiceContainer = syncServiceContainer;
        this.connectorConfiguration = syncServiceContainer.getBean(ConnectorConfiguration.class);
    }

    public int getTraverserThreadCount() throws IOException {

        return connectorConfiguration.getTraverserThreadCount();
    }

    public StepRateLimitObject getRateLimitForStep(Class<? extends AbstractStep> stepClass){

        return this.connectorConfiguration.getStepRateLimit(stepClass);
    }
}
