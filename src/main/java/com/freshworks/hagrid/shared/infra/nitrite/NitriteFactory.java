package com.freshworks.hagrid.shared.infra.nitrite;

import java.util.concurrent.atomic.AtomicBoolean;

import org.dizitart.no2.Nitrite;
import org.dizitart.no2.rocksdb.RocksDBModule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.freshworks.hagrid.shared.analytics.AnalyticsFactory;
import com.freshworks.hagrid.shared.analytics.AnalyticsService;
import com.freshworks.hagrid.shared.infra.InfraConfigService;
import com.freshworks.hagrid.shared.synchronizers.GlobalNamespaceService;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.Getter;

@Getter
@Component
public class NitriteFactory {

    Nitrite fileBasedNitriteDb;
    Nitrite inmemoryBasedNitriteDb;
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

                    String NitriteType = infraConfigService.getInfraDbType();
                    if(NitriteType.equalsIgnoreCase("file")){
                        
                        if(doesFileBasedClientExists()) {
                            return fileBasedNitriteDb;
                        }

                        fileBasedNitriteDb = Nitrite.builder()
                        .loadModule(new RocksDBModule(infraConfigService.getInfraDbLocation()))
                        .openOrCreate();

                        return fileBasedNitriteDb;
                    }

                    else{

                        if(doesInmemoryBasedClientExists()) {
                            return inmemoryBasedNitriteDb;
                        }

                        // In case of in memory , I am returning nitrite db directly instead of creating just one instance. 
                        // If I create just one instance of nitrite db then when db get closed after 1st request is completed 
                        // then for second request, db will be found closed which will be problem. 
                        // Like inmemory i.e RAM , everytime new infra is set up, to simulate the same case, I am returning new RAM ( new in memory nitrite db)
                        inmemoryBasedNitriteDb = Nitrite.builder()
                        .openOrCreate();
                        return inmemoryBasedNitriteDb;
                    }

                }
            }
        }

        finally {
            uniqueClient.set(false);
        }
    }

    public boolean doesFileBasedClientExists() {

        if(fileBasedNitriteDb != null && !fileBasedNitriteDb.isClosed()) {

            return true;
        }
        else{
            return false;
        }
    }

    public boolean doesInmemoryBasedClientExists() {

        if(inmemoryBasedNitriteDb != null && !inmemoryBasedNitriteDb.isClosed()) {

            return true;
        }
        else{
            return false;
        }
    }

}
