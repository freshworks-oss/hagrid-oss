package com.freshworks.core.data.four_five_zero.unit.dag.beans;

import com.freshworks.core.CustomRegExConditionComparator;
import com.freshworks.core.processor.AbstractBean;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.context.annotation.Conditional;

@NoArgsConstructor
@Data
@Conditional(CustomRegExConditionComparator.class)
public class SimpleBean extends AbstractBean {

    String name;
    String company;

    @Override
    public void transform() {

    }
}
