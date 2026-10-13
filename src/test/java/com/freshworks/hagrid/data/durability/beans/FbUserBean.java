package com.freshworks.hagrid.data.durability.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.freshworks.hagrid.processor.AbstractBean;
import com.freshworks.hagrid.shared.SyncServiceContainer;

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
@Profile("durability")
@Component 
public class FbUserBean extends AbstractBean {

    String user_id;
    String user_name;
    SyncServiceContainer syncServiceContainer;

    @Override
    public void configure(SyncServiceContainer syncServiceContainer){
        this.syncServiceContainer = syncServiceContainer;
    }

    @Override
    public void transform() {
    }
}
