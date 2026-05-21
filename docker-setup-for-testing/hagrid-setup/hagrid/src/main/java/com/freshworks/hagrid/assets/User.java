package com.freshworks.hagrid.assets;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.freshworks.core.processor.AbstractAsset;
import com.freshworks.core.processor.Annotations.FreshIndex;
import com.freshworks.core.shared.Namespace;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.analytics.AnalyticsFactory;
import com.freshworks.core.shared.analytics.AnalyticsService;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class User extends AbstractAsset {

    String userId;
    String userName;

    @FreshIndex
    String group;

    @FreshIndex
    String type;


    AnalyticsService analyticsService;

    @Override
    public void configure(SyncServiceContainer syncServiceContainer){
        AnalyticsFactory analyticsFactory = syncServiceContainer.getBean(AnalyticsFactory.class);
        Namespace namespace = syncServiceContainer.getBean(Namespace.class);
        analyticsService = analyticsFactory.getAnalyticsService(namespace.getNamespace());

    }

    public void setBatchFromBean(com.freshworks.hagrid.beans.User dummy){

        userId = dummy.getUser_id();
        userName = dummy.getUser_name();
    }


    @Override
    public void transform() {

        analyticsService.meterCounter("ASSET_IS_CREATED", "asset_name", "user");
    }

    @Override
    public Object getUniqueIdentifier() {
        return null;
    }
}
