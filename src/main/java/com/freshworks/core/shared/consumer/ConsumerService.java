package com.freshworks.core.shared.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshworks.core.processor.AbstractAsset;
import com.freshworks.core.shared.NamespaceService;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.analytics.AnalyticsFactory;
import com.freshworks.core.shared.analytics.AnalyticsService;
import com.freshworks.core.shared.analytics.AppEventService;
import com.freshworks.core.shared.infra.InfraDbCursor;
import com.freshworks.core.shared.infra.InfraDbList;
import com.freshworks.core.shared.infra.InfraService;
import com.freshworks.core.shared.sync.SyncStatusService;

import org.dizitart.no2.filters.NitriteFilter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;


import java.util.function.Consumer;

@Component
@Scope(value="prototype")
public class ConsumerService {


    InfraService infraService;
    SyncStatusService syncStatusService;
    InfraDbList infraDbList;
    AnalyticsFactory analyticsFactory;
    AnalyticsService analyticsService;
    NamespaceService namespaceService;
    AppEventService appEventService;

    public void configure(SyncServiceContainer syncServiceContainer) throws Exception {
        this.infraService = syncServiceContainer.getBean(InfraService.class);
        this.infraDbList = this.infraService.getPublisherList();
        this.syncStatusService = syncServiceContainer.getBean(SyncStatusService.class);
        this.analyticsFactory = syncServiceContainer.getBean(AnalyticsFactory.class);
        this.namespaceService = syncServiceContainer.getBean(NamespaceService.class);
        this.analyticsService = this.analyticsFactory.getAnalyticsService(this.namespaceService.getNamespace());
        this.appEventService = syncServiceContainer.getBean(AppEventService.class);
    }


        /**
     * Use this method to consume assets when sync is done 
     * @param filter
     * @return
     * @throws Exception
     */
    public <T extends AbstractAsset> InfraDbCursor<T> getAssetCursor(Class<T> assetClassType, NitriteFilter nitriteFilter) throws Exception{

        InfraDbCursor<T> infraDbCursor = this.infraDbList.filter(assetClassType, nitriteFilter);
        return infraDbCursor;
    }


    /**
     * Use this method to consume assets when sync is done 
     * @param filter
     * @return
     * @throws Exception
     */
    public <T extends AbstractAsset> InfraDbCursor<T> getAssetCursor(Class<T> assetClassType) throws Exception{

        InfraDbCursor<T> infraDbCursor = this.infraDbList.filter(assetClassType, null);
        return infraDbCursor;
    }

    /**
     * Use this method to consume stream of assets of particular type
     * @param abstractAsset
     * @param consumer
     */
    public void registerAssetCallback(Class<? extends AbstractAsset> abstractAsset, Consumer<AbstractAsset> consumer){

        ObjectMapper objectMapper = new ObjectMapper();
        this.analyticsService.registerEventCallback(AppEventService.APP_EVENT.HAGRID_ASSET_PUBLISH_DONE,

            params -> {
                Object object = params.get("asset");
                AbstractAsset asset = objectMapper.convertValue(object, AbstractAsset.class);
                consumer.accept(asset);
            }
        );
    }
}
