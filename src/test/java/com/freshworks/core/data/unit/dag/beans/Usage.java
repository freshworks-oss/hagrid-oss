package com.freshworks.core.data.unit.dag.beans;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.freshworks.core.processor.AbstractBean;
import com.freshworks.core.shared.SyncServiceContainer;

import lombok.Data;

@Data
@Component("unit_dag_bean_Usage")
@Profile("unit")
public class Usage extends AbstractBean {

    String usage;
    SyncServiceContainer syncServiceContainer;

    public void configure(SyncServiceContainer syncServiceContainer){
        this.syncServiceContainer = syncServiceContainer;
    }

    @Override
    public void transform() {

    }
}
