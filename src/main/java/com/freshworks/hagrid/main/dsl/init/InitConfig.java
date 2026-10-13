package com.freshworks.hagrid.main.dsl.init;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component 
@Getter 
@Setter 
public class InitConfig {

    @Value("${spring.connector.dsl.action:#{null}}")
    public String actionDslPath;

    @Value("${spring.connector.dsl.model:#{null}}")
    public String modelDslPath;

    @Value("${spring.connector.dsl.request:#{null}}")
    public String requestDslPath;
    
}
