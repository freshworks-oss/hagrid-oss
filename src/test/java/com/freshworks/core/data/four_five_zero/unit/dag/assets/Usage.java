package com.freshworks.core.data.four_zero_zero.unit.dag.assets;

import com.freshworks.core.CustomRegExConditionComparator;
import com.freshworks.core.processor.AbstractAsset;
import com.freshworks.core.shared.SyncServiceContainer;
import lombok.Data;
import org.springframework.context.annotation.Conditional;

@Data

@Conditional(CustomRegExConditionComparator.class)
public class Usage extends AbstractAsset {

    String usage;
    SyncServiceContainer syncServiceContainer;

    public void setFromBean(com.freshworks.core.data.four_zero_zero.unit.dag.beans.Usage usage){

    }

    public void configure(SyncServiceContainer syncServiceContainer){
        this.syncServiceContainer = syncServiceContainer;
    }

    @Override
    public void transform() {

    }

    @Override
    public Object getUniqueIdentifier() {
        return null;
    }
}
