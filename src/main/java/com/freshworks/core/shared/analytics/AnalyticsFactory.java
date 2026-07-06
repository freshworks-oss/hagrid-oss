package com.freshworks.core.shared.analytics;

import com.freshworks.core.shared.Annotations.BetaRelease;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@BetaRelease(sourceVersion = "3.0.0-beta", targetVersion = "3.1.0", useCase = "Provide structured logging framework along with metrics for hagrid")
@Component
@Slf4j
public class AnalyticsFactory {

    MeterRegistry meterRegistry;

    AnalyticsUtility analyticsUtility;

    HashMap<String, AnalyticsService> singletonHashMap = new HashMap<>();

    ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    @Value("${spring.connector.analytics.meter.consume.event.tags: false}")
    Boolean shouldPassTagsToMeterRegistry;

    @Autowired
    public AnalyticsFactory(MeterRegistry meterRegistry, AnalyticsUtility analyticsUtility) {
        this.meterRegistry = meterRegistry;
        this.analyticsUtility = analyticsUtility;
    }

    public AnalyticsService getAnalyticsService(String namespace) {

        try{
            readWriteLock.writeLock().lock();

            AnalyticsService analyticsService;
            if(singletonHashMap.containsKey(namespace)) {

                analyticsService = singletonHashMap.get(namespace);
            }
            else{

                analyticsService = new AnalyticsService(meterRegistry, analyticsUtility);
                analyticsService.configure(namespace, shouldPassTagsToMeterRegistry);

            }

            singletonHashMap.put(namespace, analyticsService);
            return analyticsService;
        }
        catch (Exception e){
            e.printStackTrace();
            return null;
        }
        finally {

            readWriteLock.writeLock().unlock();
        }
    }

    public void destroy(String namespace){
        AnalyticsService analyticsService = singletonHashMap.get(namespace);
        analyticsService.destroy();

        // Remove analytics service from the factory
        singletonHashMap.remove(namespace);

    }
}
