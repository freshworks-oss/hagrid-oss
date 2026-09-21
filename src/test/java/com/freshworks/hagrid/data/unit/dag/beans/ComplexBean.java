package com.freshworks.hagrid.data.unit.dag.beans;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.freshworks.hagrid.processor.AbstractBean;

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
