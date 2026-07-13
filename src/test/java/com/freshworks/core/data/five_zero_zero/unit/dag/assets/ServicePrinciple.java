package com.freshworks.core.data.five_zero_zero.unit.dag.assets;

import com.freshworks.core.CustomRegExConditionComparator;
import com.freshworks.core.processor.AbstractAsset;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

@Conditional(CustomRegExConditionComparator.class)
@Component("unit_dag_asset_ServicePrinciple")
public class ServicePrinciple extends AbstractAsset {

    public void setFromBean(com.freshworks.core.data.five_zero_zero.unit.dag.beans.ServicePrinciple servicePrinciple){

    }

    @Override
    public void transform() {

    }
}
