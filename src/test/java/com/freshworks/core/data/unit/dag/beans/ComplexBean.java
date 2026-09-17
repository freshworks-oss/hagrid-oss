package com.freshworks.core.data.unit.dag.beans;

import com.freshworks.core.processor.AbstractBean;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Data
@NoArgsConstructor

@Profile("unit")
@Component("unit_dag_bean_ComplexBean")
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
