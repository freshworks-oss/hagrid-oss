package com.freshworks.core.data.unit.dag.beans;


import com.freshworks.core.processor.AbstractBean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("unit")
@Component("unit_dag_bean_AppRoleAssignment")
public class AppRoleAssignment extends AbstractBean {
    @Override
    public void transform() {

    }
}
