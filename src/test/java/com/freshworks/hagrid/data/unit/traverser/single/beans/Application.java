package com.freshworks.hagrid.data.unit.traverser.single.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.freshworks.hagrid.processor.AbstractBean;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Profile("unit")
@Component
public class Application extends AbstractBean {

    @Override
    public void transform() {

    }
}
