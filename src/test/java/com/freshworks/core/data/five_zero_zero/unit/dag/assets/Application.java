package com.freshworks.core.data.five_zero_zero.unit.dag.assets;

import com.freshworks.core.CustomRegExConditionComparator;
import com.freshworks.core.processor.AbstractAsset;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

@Conditional(CustomRegExConditionComparator.class)
@Component("unit_dag_asset_Application")
public class Application extends AbstractAsset {

    String id;

    public void setFromBean(com.freshworks.core.data.five_zero_zero.unit.dag.beans.Application application){
        this.id = application.getClazz();
    }

    @Override
    public void transform() {

    }

}
