package com.freshworks.core.data.five_zero_zero.unit.dag.assets.inner;

import com.freshworks.core.CustomRegExConditionComparator;
import com.freshworks.core.data.five_zero_zero.unit.dag.beans.Application;
import com.freshworks.core.processor.AbstractAsset;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

@Conditional(CustomRegExConditionComparator.class)
@Component("unit_dag_asset_TestInnerAsset")
public class TestInnerAsset extends AbstractAsset {


    public void setFromBean(Application application){

    }


    @Override
    public void transform() {

    }

}
