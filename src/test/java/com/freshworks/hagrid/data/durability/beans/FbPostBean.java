package com.freshworks.core.data.durability.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.freshworks.hagrid.processor.AbstractBean;
import com.freshworks.hagrid.shared.SyncServiceContainer;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Profile("durability")
@Component 
public class FbPostBean extends AbstractBean {

    String post_id;
    String post_title;
    String post_text;

    SyncServiceContainer syncServiceContainer;

    @Override
    public void configure(SyncServiceContainer syncServiceContainer){
        this.syncServiceContainer = syncServiceContainer;
    }

    @Override
    public void transform() {

    }
}
