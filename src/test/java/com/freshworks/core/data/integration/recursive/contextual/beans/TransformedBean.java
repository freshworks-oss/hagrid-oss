package com.freshworks.core.data.integration.recursive.contextual.beans;

import org.springframework.context.annotation.Profile;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.freshworks.core.processor.AbstractBean;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Profile("integration")
public class TransformedBean extends AbstractBean {

    String token;
    JsonNode context;

    @Override
    public void transform() {

    }
}
