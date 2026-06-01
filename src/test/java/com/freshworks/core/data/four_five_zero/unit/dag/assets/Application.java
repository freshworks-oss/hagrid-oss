package com.freshworks.core.data.four_five_zero.unit.dag.assets;

import com.freshworks.core.CustomRegExConditionComparator;
import com.freshworks.core.processor.AbstractAsset;
import org.springframework.context.annotation.Conditional;

@Conditional(CustomRegExConditionComparator.class)
public class Application extends AbstractAsset {

    String id;

    public void setFromBean(com.freshworks.core.data.four_zero_zero.unit.dag.beans.Application application){
        this.id = application.getId();
    }

    @Override
    public void transform() {

    }

    @Override
    public Object getUniqueIdentifier() {
        return null;
    }
}
