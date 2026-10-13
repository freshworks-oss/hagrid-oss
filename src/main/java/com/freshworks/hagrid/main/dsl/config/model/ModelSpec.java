package com.freshworks.hagrid.main.dsl.config.model;

import java.util.ArrayList;
import java.util.List;

import org.apache.catalina.core.ApplicationContext;
import org.springframework.stereotype.Component;

import groovy.lang.Closure;
import groovy.lang.GroovyObjectSupport;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Component
public class ModelSpec extends GroovyObjectSupport {
    
    private final List<ApiModelConfig> apiModelList = new ArrayList<>();
    private final List<OutputModelConfig> outputModelList = new ArrayList<>();
    ApplicationContext applicationContext;

    public void api_model(Closure closure) {
        ApiModelConfig newModel = new ApiModelConfig();
        apiModelList.add(newModel);

        closure.setDelegate(newModel);
        closure.setResolveStrategy(Closure.DELEGATE_ONLY);
        closure.call();
    }

     // This catches unquoted words used as properties/variables
    public Object propertyMissing(String name) {
        System.out.println("property missing catch " + name);
        return name; 
    }

    public void output_model(Closure closure) {
        OutputModelConfig newModel = new OutputModelConfig();
        outputModelList.add(newModel);

        closure.setDelegate(newModel);
        closure.setResolveStrategy(Closure.DELEGATE_FIRST);
        closure.call();
    }

    public ApiModelConfig getApiModelByName(String name) {
        for (ApiModelConfig model : apiModelList) {
            if (model.getName().equalsIgnoreCase(name)) {
                return model;
            }
        }
        return null;
    }

    public OutputModelConfig getOutputModelByName(String name) {
        for (OutputModelConfig model : outputModelList) {
            if (model.getName().equalsIgnoreCase(name)) {
                return model;
            }
        }
        return null;
    }

    public boolean hasApiModelByName(String name) {
        for (ApiModelConfig model : apiModelList) {
            if (model.getName().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasOutputModelByName(String name) {
        for (OutputModelConfig model : outputModelList) {
            if (model.getName().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }
}
