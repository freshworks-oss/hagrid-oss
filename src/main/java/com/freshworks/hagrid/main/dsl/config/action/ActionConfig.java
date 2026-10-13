package com.freshworks.hagrid.main.dsl.config.action;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshworks.hagrid.main.dsl.config.model.ApiModelConfig;
import com.freshworks.hagrid.main.dsl.config.model.ModelSpec;
import com.freshworks.hagrid.main.dsl.config.model.OutputModelConfig;
import com.freshworks.hagrid.main.dsl.config.requests.RequestSpec;

import groovy.lang.Closure;
import groovy.lang.GroovyShell;
import lombok.Getter;
import lombok.Setter;

/**
 * POJO representing a single action defined in actions.dsl.
 */
@Getter
@Setter
public class ActionConfig{

    ObjectMapper objectMapper = new ObjectMapper();

    private String name;
    private Closure responsePathClosure;
    private Closure<Boolean> inputValidationClosure;

    private ApiModelConfig apiModelConfig;
    private List<OutputModelConfig> outputModelConfigList = new ArrayList();
    private ModelSpec modelSpec;

    private ActionRequestConfig actionRequestConfig;
    private ActionRequestConfig paginateRequestConfig;

    private HooksConfig hooksConfig;
    
    private RequestSpec requestSpec;

    String parentActionName;

    boolean isRootNode;

    GroovyShell groovyShell = new GroovyShell();

    public ActionConfig(ModelSpec modelSpec, RequestSpec requestSpec){
        this.modelSpec = modelSpec;
        this.requestSpec = requestSpec;    
        this.hooksConfig = new HooksConfig(requestSpec);
    }


    /**
     * Defines name of the action
     * @param name
     */
    public void name(String name) {
        this.name = name;
    }

    /**
     * Defines model name with which it is associated with 
     * @param modelName
     */
    public void api_model(String modelName){
        this.apiModelConfig = this.modelSpec.getApiModelByName(modelName);
    }

    /**
     * Defines model name with which it is associated with 
     * @param modelName
     */
    public void output(String[] outputModelNameArray){

        for(String output: outputModelNameArray){
            OutputModelConfig outputModelConfig = this.modelSpec.getOutputModelByName(output);
            this.outputModelConfigList.add(outputModelConfig);
        }
    }

    /**
     * Defines template and its mapping
     * @param modelName
     */

    public void request(String requestName, LinkedHashMap<String, String> mapping){
        
        this.actionRequestConfig = new ActionRequestConfig();
        this.actionRequestConfig.setRequestConfig(requestSpec.getRequestByName(requestName));

        LinkedHashMap<String, Closure> newMapping = new LinkedHashMap<>();

        for(Map.Entry<String, String> map : mapping.entrySet()){

            String s = "{-> " + map.getValue() + " }";
            Closure c = (Closure)groovyShell.evaluate(s);
            newMapping.put(map.getKey(), c);
        }

        this.actionRequestConfig.setMapping(newMapping);
    }


    public void request(String requestName){

        ActionRequestConfig actionRequest = new ActionRequestConfig();
        this.actionRequestConfig = actionRequest;
        this.actionRequestConfig.setRequestConfig(requestSpec.getRequestByName(requestName));
        this.actionRequestConfig.setMapping(new LinkedHashMap<>());
    }

    /**
     * Defines template and its mapping
     * @param modelName
     */

    public void paginate_request(String requestName, LinkedHashMap<String, String> mapping){
        
        this.paginateRequestConfig = new ActionRequestConfig();
        this.paginateRequestConfig.setRequestConfig(requestSpec.getRequestByName(requestName));

        LinkedHashMap<String, Closure> newMapping = new LinkedHashMap<>();

        for(Map.Entry<String, String> map : mapping.entrySet()){

            String s = "{-> " + map.getValue() + " }";
            Closure c = (Closure)groovyShell.evaluate(s);
            newMapping.put(map.getKey(), c);
        }

        this.paginateRequestConfig.setMapping(newMapping);
    }

    public void paginate_request(String requestName){

        ActionRequestConfig actionRequest = new ActionRequestConfig();
        this.paginateRequestConfig = actionRequest;
        this.paginateRequestConfig.setRequestConfig(requestSpec.getRequestByName(requestName));
        this.paginateRequestConfig.setMapping(new LinkedHashMap<>());
    }

    public void parent(String parentActionName){
        this.parentActionName = parentActionName;
    }
    
    public void is_root(Boolean isRoot){
        this.isRootNode = isRoot;
    }

    public void response_path(String responsePath){
        String s = "{-> " + responsePath + " }";
        this.responsePathClosure = (Closure) groovyShell.evaluate(s);
    }

    public void validate_input(Closure<Boolean> inputValidationClosure){

        this.inputValidationClosure = inputValidationClosure;
    }

    
    public void hooks(Closure closure){

        closure.setDelegate(this.hooksConfig);
        closure.setResolveStrategy(Closure.DELEGATE_FIRST);
        closure.call();
    }

    @Override 
    public boolean equals(Object other){

        ActionConfig actionConfig = (ActionConfig)other;

        if(this.name.equalsIgnoreCase(actionConfig.getName())){
            return true;
        }
        
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.name);
    }
}
