package com.freshworks.hagrid.data.unit.dag.beans;


import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.freshworks.hagrid.processor.AbstractBean;

@Profile("unit")
@Component("unit_dag_bean_ServicePrinciple")
public class ServicePrinciple extends AbstractBean {
    @Override
    public void transform() {

    }
}
