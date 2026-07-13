package com.freshworks.core.data.five_zero_zero.unit.dag.beans;

import com.freshworks.core.CustomRegExConditionComparator;
import com.freshworks.core.processor.AbstractBean;
import com.freshworks.core.shared.SyncServiceContainer;
import lombok.Data;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

@Data
@Component("unit_dag_bean_Usage")
@Conditional(CustomRegExConditionComparator.class)
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
