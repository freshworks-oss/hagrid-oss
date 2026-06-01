package com.freshworks.core.data.four_five_zero.unit.dag.assets.inner;

import com.freshworks.core.CustomRegExConditionComparator;
import com.freshworks.core.data.four_zero_zero.unit.dag.beans.Application;
import com.freshworks.core.processor.AbstractAsset;
import org.springframework.context.annotation.Conditional;

@Conditional(CustomRegExConditionComparator.class)
public class TestInnerAsset extends AbstractAsset {


    public void setFromBean(Application application){

    }


    @Override
    public void transform() {

    }

    @Override
    public Object getUniqueIdentifier() {
        return null;
    }
}
