package com.freshworks.core.data.unit.dag.assets.inner.innermost;

import com.freshworks.core.data.unit.dag.beans.ServicePrinciple;
import com.freshworks.core.processor.AbstractAsset;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("unit")
@Component("unit_dag_asset_TestInnerMostAsset")
public class TestInnerMostAsset extends AbstractAsset {


    public void setFromBean(ServicePrinciple testServicePrinciple) {

    }

    @Override
    public void transform() {

    }

}
