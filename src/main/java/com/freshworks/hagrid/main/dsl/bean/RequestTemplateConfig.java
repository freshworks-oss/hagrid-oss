package com.freshworks.hagrid.main.dsl.bean;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import com.freshworks.hagrid.main.dsl.config.requests.RequestSpec;
import com.freshworks.hagrid.main.dsl.config.requests.RequestSpecHandler;

import groovy.lang.Binding;
import groovy.lang.Closure;
import groovy.lang.GroovyShell;


@Configuration
public class RequestTemplateConfig {
    

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestTemplateConfig.class);
    private static final String REQUESTS_DSL_RESOURCE = "requests.groovy";

    @Bean
    public RequestSpec requestSpec() throws Exception {
        
        RequestSpec requestSpec = new RequestSpec();
        RequestSpecHandler requestSpecHandler =  new RequestSpecHandler(requestSpec);
        
        Binding binding = new Binding();
        GroovyShell groovyShell = new GroovyShell(binding);

        Closure<Void> requestsClosure = new Closure<Void>(null) {
            public void doCall(Closure closure) {
                requestSpecHandler.requests(closure);
            }
        };
        binding.setVariable("requests", requestsClosure);

        ClassPathResource resource = new ClassPathResource(REQUESTS_DSL_RESOURCE);
        try (InputStreamReader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            groovyShell.evaluate(reader);
        }

        LOGGER.info("Loaded {} actions from {}", requestSpec.getRequestList().size(), REQUESTS_DSL_RESOURCE);
        requestSpec.getRequestList().forEach(request ->
                LOGGER.info("Registered request='{}'",
                        request.getName()));

        return requestSpec;
    }
    
}
