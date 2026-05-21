package com.freshworks.hagrid.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.freshworks.core.processor.AbstractBean;
import com.freshworks.core.shared.Namespace;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.analytics.AnalyticsFactory;
import com.freshworks.core.shared.analytics.AnalyticsService;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Comment extends AbstractBean {

    String comment_id;
    String comment_text;
    String comment_title;

    @JsonIgnore
    AnalyticsService analyticsService;

    @Override
    public void configure(SyncServiceContainer syncServiceContainer){
        AnalyticsFactory analyticsFactory = syncServiceContainer.getBean(AnalyticsFactory.class);
        Namespace namespace = syncServiceContainer.getBean(Namespace.class);
        analyticsService = analyticsFactory.getAnalyticsService(namespace.getNamespace());

    }

    @Override
    public void transform() {

        analyticsService.meterCounter("BEAN_IS_CREATED", "bean_name", "comment");
    }
}
