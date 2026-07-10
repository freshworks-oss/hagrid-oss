package com.freshworks.core.shared.infra;

import org.springframework.stereotype.Component;

import com.freshworks.core.shared.infra.nitrite.NitriteService;
import com.freshworks.core.shared.sync.ConnectorConfiguration;

@Component
public class InfraBeanService {

    public InfraService getInfraService(InfraConfigService infraConfigService, ConnectorConfiguration connectorConfiguration) throws Exception {

        return  new NitriteService();

    }

}
