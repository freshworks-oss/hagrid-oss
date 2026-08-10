package com.freshworks.core.data.integration.recursive.contextual.beans;

import org.springframework.context.annotation.Profile;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.freshworks.core.processor.AbstractBean;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Profile("integration")
public class GeneratedToken extends AbstractBean {

    String token;
    String context;

    @Override
    public void transform() {

    }
}
