package com.freshworks.hagrid.main.dsl.bean;

import groovy.lang.Binding;
import groovy.lang.Closure;
import groovy.lang.GroovyShell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import com.freshworks.hagrid.main.dsl.config.action.ActionSpec;
import com.freshworks.hagrid.main.dsl.config.action.ActionSpecHandler;
import com.freshworks.hagrid.main.dsl.config.model.ModelSpec;
import com.freshworks.hagrid.main.dsl.config.requests.RequestSpec;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Configuration
public class ActionSpecBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActionSpecBean.class);
    private static final String ACTIONS_DSL_RESOURCE = "actions.groovy";

    @Bean
    public ActionSpec actionSpec(ModelSpec modelSpec, RequestSpec requestSpec) throws Exception {
        ActionSpec actionSpec = new ActionSpec(modelSpec, requestSpec);
        ActionSpecHandler actionSpecHandler = new ActionSpecHandler(actionSpec);

        Binding binding = new Binding();
        GroovyShell groovyShell = new GroovyShell(binding);

        Closure<Void> actionsClosure = new Closure<Void>(null) {
            public void doCall(Closure closure) {
                actionSpecHandler.actions(closure);
            }
        };

        binding.setVariable("actions", actionsClosure);

        ClassPathResource resource = new ClassPathResource(ACTIONS_DSL_RESOURCE);
        try (InputStreamReader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            groovyShell.evaluate(reader);
        }

        LOGGER.info("Loaded {} actions from {}", actionSpec.getActionList().size(), ACTIONS_DSL_RESOURCE);
        actionSpec.getActionList().forEach(action ->
                LOGGER.info("Registered action='{}'",
                        action.getCompositeActionName()));

        return actionSpec;
    }
}
