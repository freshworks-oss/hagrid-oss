package com.freshworks.hagrid.main.dsl.bean;

import groovy.lang.Binding;
import groovy.lang.Closure;
import groovy.lang.GroovyShell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import com.freshworks.hagrid.main.dsl.config.model.ModelSpec;
import com.freshworks.hagrid.main.dsl.config.model.ModelSpecHandler;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Configuration
public class ModelSpecBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModelSpecBean.class);
    private static final String MODELS_DSL_RESOURCE = "models.groovy";

    @Bean
    public ModelSpec modelSpec() throws Exception {
        ModelSpec modelSpec = new ModelSpec();
        ModelSpecHandler modelSpecHandler = new ModelSpecHandler(modelSpec);

        Binding binding = new Binding();
        GroovyShell groovyShell = new GroovyShell(binding);

        Closure<Void> modelsClosure = new Closure<Void>(null) {
            public void doCall(Closure closure) {
                modelSpecHandler.models(closure);
            }
        };
        binding.setVariable("models", modelsClosure);

        ClassPathResource resource = new ClassPathResource(MODELS_DSL_RESOURCE);
        try (InputStreamReader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            groovyShell.evaluate(reader);
        }

        LOGGER.info("Loaded {} actions from {}", modelSpec.getApiModelList().size(), MODELS_DSL_RESOURCE);
        modelSpec.getApiModelList().forEach(model ->
                LOGGER.info("Registered model='{}'",
                        model.getName()));

        return modelSpec;
    }
}
