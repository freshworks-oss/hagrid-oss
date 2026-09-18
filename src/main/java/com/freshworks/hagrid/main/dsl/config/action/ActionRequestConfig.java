package com.freshworks.hagrid.main.dsl.config.action;

import java.util.LinkedHashMap;
import com.freshworks.hagrid.main.dsl.config.requests.RequestConfig;
import com.freshworks.hagrid.main.dsl.config.requests.RequestSpec;

import groovy.lang.Closure;
import groovy.lang.GroovyObjectSupport;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ActionRequestConfig extends GroovyObjectSupport{
    
    RequestSpec requestSpec;
    RequestConfig requestConfig;
    LinkedHashMap<String, Closure> mapping;
    

     // This catches unquoted words used as properties/variables
    public Object propertyMissing(String name) {
        System.out.println("property missing catch " + name);
        return name; 
    }
}
