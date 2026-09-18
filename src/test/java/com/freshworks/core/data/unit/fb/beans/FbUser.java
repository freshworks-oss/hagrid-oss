package com.freshworks.core.data.unit.fb.beans;

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
@Component("unit_fb_bean_fbUser")
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Profile("unit")
public class FbUser extends AbstractBean {

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
