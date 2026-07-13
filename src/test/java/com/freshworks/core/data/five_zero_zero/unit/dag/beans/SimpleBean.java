package com.freshworks.core.data.five_zero_zero.unit.dag.beans;

import com.freshworks.core.CustomRegExConditionComparator;
import com.freshworks.core.processor.AbstractBean;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

@NoArgsConstructor
@Data
@Component("unit_dag_bean_SimpleBean")
@Conditional(CustomRegExConditionComparator.class)
public class SimpleBean extends AbstractBean {

    String name;
    String company;

    @Override
    public void transform() {

    }
}
