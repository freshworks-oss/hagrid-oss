package com.freshworks.core.data.five_zero_zero.unit.dag.assets.inner.innermost;

import com.freshworks.core.CustomRegExConditionComparator;
import com.freshworks.core.data.five_zero_zero.unit.dag.beans.Application;
import com.freshworks.core.data.five_zero_zero.unit.dag.beans.ServicePrinciple;
import com.freshworks.core.processor.AbstractAsset;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

@Conditional(CustomRegExConditionComparator.class)
@Component("unit_dag_asset_TestInnerMostJoinedAsset")
public class TestInnerMostJoinedAsset extends AbstractAsset {

    public void setFromBean(Application application, ServicePrinciple servicePrinciple){

    }

    @Override
    public void transform() {

    }
}
