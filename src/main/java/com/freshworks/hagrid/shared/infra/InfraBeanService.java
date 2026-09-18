package com.freshworks.hagrid.shared.infra;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.freshworks.hagrid.shared.infra.nitrite.NitriteService;

@Component
@Scope ("prototype")
public class InfraBeanService {

    public InfraService getInfraService(InfraConfigService infraConfigService) throws Exception {

        return  new NitriteService();

    }

}
