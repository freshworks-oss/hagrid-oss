package com.freshworks.core.shared.infra;

import java.io.IOException;

import org.springframework.stereotype.Component;

import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.sync.ConnectorConfiguration;
@Component
public class InfraConfigService {

    ConnectorConfiguration connectorConfiguration;
    SyncServiceContainer syncServiceContainer;

    public void configure(SyncServiceContainer syncServiceContainer) throws IOException {
        this.syncServiceContainer = syncServiceContainer;
        this.connectorConfiguration = syncServiceContainer.getBean(ConnectorConfiguration.class);
    }

    public String getInfraDbLocation() throws IOException {

        return this.connectorConfiguration.getInfraDbLocation();
    }

    public String getInfraDbType() throws IOException {

        return this.connectorConfiguration.getInfraDbType();
    }
}


