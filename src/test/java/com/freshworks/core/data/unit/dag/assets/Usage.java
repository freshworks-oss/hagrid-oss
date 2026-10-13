package com.freshworks.core.data.unit.dag.assets;

import com.freshworks.core.processor.AbstractAsset;
import com.freshworks.core.shared.SyncServiceContainer;
import lombok.Data;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Data
@Profile("unit")
@Component("unit_dag_asset_Usage")
public class Usage extends AbstractAsset {

    String usage;
    SyncServiceContainer syncServiceContainer;

    public void setFromBean(com.freshworks.core.data.unit.dag.beans.Usage usage){

    }

    public void configure(SyncServiceContainer syncServiceContainer){
        this.syncServiceContainer = syncServiceContainer;
    }

    @Override
    public void transform() {

    }
}
