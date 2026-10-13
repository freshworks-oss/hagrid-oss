package com.freshworks.core.processor;

import java.io.IOException;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.sync.ConnectorConfiguration;

@Component
@Scope(value="prototype")
public class ProcessorConfigService {

    ConnectorConfiguration connectorConfiguration;
    SyncServiceContainer syncServiceContainer;

    public void configure(SyncServiceContainer syncServiceContainer) throws ClassNotFoundException, IllegalAccessException, IOException {
        this.syncServiceContainer = syncServiceContainer;
        this.connectorConfiguration = syncServiceContainer.getBean(ConnectorConfiguration.class);

    }

    public int getProcessorPollCount() throws IOException {

        return this.connectorConfiguration.getProcessorPollCount();
    }

    public int getNumberOfParallelProcessor() throws IOException {

        return this.connectorConfiguration.getNumberOfParallelProcessor();
    }
}
