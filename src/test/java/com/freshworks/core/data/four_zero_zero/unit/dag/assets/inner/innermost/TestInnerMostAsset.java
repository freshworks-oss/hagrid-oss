package com.freshworks.core.data.four_zero_zero.unit.dag.assets.inner.innermost;

import com.freshworks.core.CustomRegExConditionComparator;
import com.freshworks.core.data.four_zero_zero.unit.dag.beans.ServicePrinciple;
import com.freshworks.core.processor.AbstractAsset;
import org.springframework.context.annotation.Conditional;

@Conditional(CustomRegExConditionComparator.class)
public class TestInnerMostAsset extends AbstractAsset {


    public void setFromBean(ServicePrinciple testServicePrinciple) {

    }

    @Override
    public void transform() {

    }

    @Override
    public Object getUniqueIdentifier() {
        return null;
    }
}
