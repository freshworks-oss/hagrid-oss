package com.freshworks.hagrid.main.dsl.runnable;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.logging.log4j.util.Strings;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.freshworks.hagrid.main.dsl.config.model.OutputModelConfig;
import com.freshworks.hagrid.main.dsl.config.model.OutputModelConfig.ModelBinding;

import groovy.lang.Binding;
import groovy.lang.Closure;
import groovy.lang.GroovyShell;
import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter 
public class OutputModel {

    static ObjectMapper objectMapper = new ObjectMapper();
    
    String name;
    ObjectNode data = objectMapper.createObjectNode();

    @JsonIgnore
    Join join;    

    @JsonIgnore
    LinkedHashMap<String, ModelBinding> outputFieldMapping = new LinkedHashMap<>();

    @JsonIgnore
    Closure<Boolean> filterClosure = new Closure<Boolean>(null) {
        
        public Boolean doCall(JsonNode data){

            return true;
        }
    };

    @JsonIgnore
    Closure<Void> transformClosure = new Closure<Void>(null) {
        
        public void doCall(JsonNode data){
            
            
        }
    };

    public void transformModel(){
        this.transformClosure.call(data);
    }

    public boolean filterModel(){
        return this.filterClosure.call(data);
    }

    public boolean dependsOn(ApiModel model){

        if(outputFieldMapping.isEmpty()){

            // It means that no mapping is provided then 
        }

        for(Map.Entry<String, ModelBinding> entry : outputFieldMapping.entrySet()){

            String apiModelName = entry.getValue().getModelName();
            if(model.getName().equalsIgnoreCase(apiModelName)){
                return true;
            }
        }
        
        return false;
    }

    public boolean primitive(){

        if(this.join == null){
            return true;
        }

        return false;
    }
    
    public void populate(ApiModel model){

        String modelName = model.getName();
        JsonNode node = model.getData();

        for(Map.Entry<String, ModelBinding> entry : outputFieldMapping.entrySet()){

            String outputFieldName = entry.getKey();
            ModelBinding apiModelBinding = entry.getValue();

            if(apiModelBinding.getModelName().equalsIgnoreCase(modelName)){
                this.data.set(outputFieldName, node.get(apiModelBinding.getModelField()));
            }
        }
    }
    
    public void key(String keyLang){
        
        this.join = new Join();

        String keyDsl = "join " + keyLang;

        Binding binding = new Binding();
        binding.setVariable("join", this.join);

        GroovyShell groovyShell = new GroovyShell(binding);
        groovyShell.evaluate(keyDsl);
    }

    public Map getModelDataAsMap(){

        return objectMapper.convertValue(data, Map.class);
    }

    public ObjectNode getModelDataAsObjectNode(Map modelData){

        return objectMapper.convertValue(modelData, ObjectNode.class);
    }

    @Getter
    @Setter
    public class Join{

        String leftModelName;
        String leftModelKey;
        String operator;
        String rightModelName;
        String rightModelKey;

        public Join model(String modelName, String modekey){

            if(Strings.isNotBlank(leftModelName)){

                System.out.println("setting up left key");
                this.leftModelName = modelName;
                this.leftModelKey = modekey;
            }

            else{

                System.out.println("setting up right key");
                this.rightModelName = modelName;
                this.rightModelKey = modekey;
            }

            return this;
        }

        public Join with_operator(String operator){

            System.out.println("setting up operator");
            this.operator = operator;
            return this;
        }
    }
}
