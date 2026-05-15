package com.freshworks.core.shared.infra;

import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.infra.h2.H2DbService;
import com.freshworks.core.shared.infra.inmemory.InmemoryService;
import com.freshworks.core.shared.infra.persistent.MongoService;
import com.freshworks.core.shared.infra.redis.RedisService;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Component
public class InfraBeanService {

    public InfraService getInfraService(InfraConfigService infraConfigService) throws Exception {

        InfraService infraService;
        if(infraConfigService.getInfraType().equals("persistent")){
            infraService = new MongoService();
            return infraService;
        }

        else if (infraConfigService.getInfraType().equals("redis")){
            infraService = new RedisService();
            return infraService;

        }

        else if (infraConfigService.getInfraType().equals("h2")){
            infraService = new H2DbService();
            return infraService;
        }
        else{
            infraService = new InmemoryService();
            return infraService;
        }
    }

}
