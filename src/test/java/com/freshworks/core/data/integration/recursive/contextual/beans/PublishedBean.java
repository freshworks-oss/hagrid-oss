package com.freshworks.core.data.integration.recursive.contextual.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.freshworks.core.processor.AbstractBean;
import com.freshworks.core.shared.SyncServiceContainer;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Profile("integration")
@Component("recursive_contextual_bean_published_bean")
public class PublishedBean extends AbstractBean {

    String token;
    String context;

    @Override
    public void transform() {
    }
}
