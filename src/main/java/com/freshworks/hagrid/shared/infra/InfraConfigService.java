package com.freshworks.hagrid.shared.infra;

import java.io.IOException;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.freshworks.hagrid.shared.SyncServiceContainer;
import com.freshworks.hagrid.shared.sync.ConnectorConfiguration;



@Component
@Scope ("prototype")
public class InfraConfigService {

    ConnectorConfiguration connectorConfiguration;
    SyncServiceContainer syncServiceContainer;

    public void configure(SyncServiceContainer syncServiceContainer) throws IOException {
        this.syncServiceContainer = syncServiceContainer;
        this.connectorConfiguration = syncServiceContainer.getBean(ConnectorConfiguration.class);

        System.out.println("db type is " + this.connectorConfiguration.getInfraDbType());
    }

    public String getInfraDbLocation() throws IOException {

        return this.connectorConfiguration.getInfraDbLocation();
    }

    public String getInfraDbType() throws IOException {

        return this.connectorConfiguration.getInfraDbType();
    }
}


