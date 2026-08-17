package com.freshworks.core.data.integration.recursive.json.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.freshworks.core.processor.AbstractBean;
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
@Profile("integration")
@Component("recursive_contextual_bean_non_primitive_key_value")
public class NonPrimitiveKeyValue extends AbstractBean {

    String Key;
    JsonNode jsonNode;

    @Override
    public void transform() {

    }
}
