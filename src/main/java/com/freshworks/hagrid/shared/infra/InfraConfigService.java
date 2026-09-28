package com.freshworks.hagrid.shared.infra;

import java.io.IOException;

import org.checkerframework.checker.units.qual.radians;
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


    // For backward compatibility
    // Use nitriteDb instead
    public String getMongoConnectionString(){

        return this.connectorConfiguration.getMongoConnectionString();
    }

    // For backward compatibility
    // Use nitriteDb instead
    public String getDatabaseUserName(){

        return this.connectorConfiguration.getDatabaseUserName();
    }

    // For backward compatibility
    // Use nitriteDb instead
    public String getDatabasePassword(){

        return this.connectorConfiguration.getDatabasePassword();
    }

    // For backward compatibility
    // Use nitriteDb instead
    public String getDatabaseAuthDb(){
        
        return this.connectorConfiguration.getDatabaseAuthDb();
    }

    // For backward compatibility
    // Use nitriteDb instead
    public String getDatabaseHost(){

        return this.connectorConfiguration.getDatabaseHost();
    }

    // For backward compatibility
    // Use nitriteDb instead
    public String getAdditionalParams(){

        return this.connectorConfiguration.getAdditionalParams();
    }

    // For backward compatibility
    // Use nitriteDb instead
    public int getDatabasePort(){

        return this.connectorConfiguration.getDatabasePort();
    }

}


