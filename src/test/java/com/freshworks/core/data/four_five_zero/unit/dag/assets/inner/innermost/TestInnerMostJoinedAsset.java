package com.freshworks.core.data.four_five_zero.unit.dag.assets.inner.innermost;

import com.freshworks.core.CustomRegExConditionComparator;
import com.freshworks.core.data.four_five_zero.unit.dag.beans.Application;
import com.freshworks.core.data.four_five_zero.unit.dag.beans.ServicePrinciple;
import com.freshworks.core.processor.AbstractAsset;
import org.springframework.context.annotation.Conditional;

@Conditional(CustomRegExConditionComparator.class)
public class TestInnerMostJoinedAsset extends AbstractAsset {

    public void setFromBean(Application application, ServicePrinciple servicePrinciple){

    }

    @Override
    public void transform() {

    }

    @Override
    public Object getUniqueIdentifier() {
        return null;
    }
}
