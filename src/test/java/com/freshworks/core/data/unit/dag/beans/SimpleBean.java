package com.freshworks.hagrid.data.unit.dag.beans;


import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.freshworks.hagrid.processor.AbstractBean;

@NoArgsConstructor
@Data
@Component("unit_dag_bean_SimpleBean")
@Profile("unit")
public class SimpleBean extends AbstractBean {

    String name;
    String company;

    @Override
    public void transform() {

    }
}
