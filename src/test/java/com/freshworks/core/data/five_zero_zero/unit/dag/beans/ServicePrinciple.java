package com.freshworks.core.data.five_zero_zero.unit.dag.beans;

import com.freshworks.core.CustomRegExConditionComparator;
import com.freshworks.core.processor.AbstractBean;
import org.springframework.context.annotation.Conditional;

@Conditional(CustomRegExConditionComparator.class)
public class ServicePrinciple extends AbstractBean {
    @Override
    public void transform() {

    }
}
