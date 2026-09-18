package com.freshworks.core.data.unit.dag.assets;

import com.freshworks.core.processor.AbstractAsset;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("unit")
@Component("unit_dag_asset_ServicePrinciple")
public class ServicePrinciple extends AbstractAsset {

    public void setFromBean(com.freshworks.core.data.unit.dag.beans.ServicePrinciple servicePrinciple){

    }

    @Override
    public void transform() {

    }
}
