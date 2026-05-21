package com.freshworks.hagrid.assets;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.freshworks.core.processor.AbstractAsset;
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
public class Post extends AbstractAsset {

    String post_id;
    String post_title;
    String post_text;

    AnalyticsService analyticsService;

    @Override
    public void configure(SyncServiceContainer syncServiceContainer){
        AnalyticsFactory analyticsFactory = syncServiceContainer.getBean(AnalyticsFactory.class);
        Namespace namespace = syncServiceContainer.getBean(Namespace.class);
        analyticsService = analyticsFactory.getAnalyticsService(namespace.getNamespace());

    }

    public void setBatchFromBean(com.freshworks.hagrid.beans.Post post){

        post_id = post.getPost_id();
        post_title = post.getPost_title();
        post_text = post.getPost_text();
    }

    @Override
    public void transform() {
        analyticsService.meterCounter("ASSET_IS_CREATED", "asset_name", "post");
    }

    @Override
    public Object getUniqueIdentifier() {
        return null;
    }
}
