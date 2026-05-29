package com.freshworks.core.data.four_zero_zero.unit.dag.beans;

import com.freshworks.core.CustomRegExConditionComparator;
import com.freshworks.core.processor.AbstractBean;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.context.annotation.Conditional;

@Data
@NoArgsConstructor

@Conditional(CustomRegExConditionComparator.class)
public class ComplexBean extends AbstractBean {

    String name;
    String company;
    Address address;

    @Data
    public static class Address{
        String city;
        String state;
        String country;
    }

    @Override
    public void transform() {

    }
}
