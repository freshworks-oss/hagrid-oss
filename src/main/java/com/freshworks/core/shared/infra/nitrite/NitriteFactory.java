package com.freshworks.core.shared.infra.nitrite;

import java.util.concurrent.atomic.AtomicBoolean;

import org.dizitart.no2.Nitrite;
import org.dizitart.no2.rocksdb.RocksDBModule;
import org.h2.tools.Server;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.freshworks.core.shared.analytics.AnalyticsFactory;
import com.freshworks.core.shared.analytics.AnalyticsService;
import com.freshworks.core.shared.infra.InfraConfigService;
import com.freshworks.core.shared.synchronizers.GlobalNamespaceService;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.Getter;

@Getter
@Component
public class NitriteFactory {

    Nitrite nitriteDb;
    AnalyticsService analyticsService;
    GlobalNamespaceService globalNamespaceService;
    MeterRegistry meterRegistry;
    AtomicBoolean uniqueServer = new AtomicBoolean(false);
    AtomicBoolean uniqueClient = new AtomicBoolean(false);

    @Autowired
    public NitriteFactory(AnalyticsFactory analyticsFactory, GlobalNamespaceService globalNamespaceService, MeterRegistry meterRegistry){
        this.globalNamespaceService = globalNamespaceService;
        this.meterRegistry = meterRegistry;
        this.analyticsService = analyticsFactory.getAnalyticsService(this.globalNamespaceService.getGlobalNamespace());
    }

    public Nitrite getNitriteClient(String namespace, InfraConfigService infraConfigService) throws Exception {

        try{

            for(;;){

                if(uniqueClient.compareAndSet(false, true)) {

                     if(doesClientExists()) {
                        return nitriteDb;
                    }

                    String NitriteType = infraConfigService.getNitriteDatabaseType();
                    if(NitriteType.equalsIgnoreCase("file")){
  
                        nitriteDb = Nitrite.builder()
                        .loadModule(new RocksDBModule(infraConfigService.getNitriteDataPath()))
                        .openOrCreate();

                        return nitriteDb;
                    }

                    else{
                        // In case of in memory , I am returning nitrite db directly instead of creating just one instance. 
                        // If I create just one instance of nitrite db then when db get closed after 1st request is completed 
                        // then for second request, db will be found closed which will be problem. 
                        // Like inmemory i.e RAM , everytime new infra is set up, to simulate the same case, I am returning new RAM ( new in memory nitrite db)
                        return Nitrite.builder()
                        .openOrCreate();
                    }

                }
            }
        }

        finally {
            uniqueClient.set(false);
        }
    }

    public boolean doesClientExists() {

        if(nitriteDb != null && !nitriteDb.isClosed()) {

            return true;
        }
        else{
            return false;
        }
    }

}
