package com.freshworks.hagrid.main.dsl.config.action;

import groovy.lang.Closure;
import groovy.lang.GroovyObjectSupport;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.freshworks.hagrid.main.dsl.config.model.ModelSpec;
import com.freshworks.hagrid.main.dsl.config.requests.RequestSpec;

/**
 * Holds one {@link ActionConfig} POJO per action block parsed from actions.dsl.
 */
@Getter
@Component
public class ActionSpec extends GroovyObjectSupport{

    private final List<CompositeActionConfig> actionList = new ArrayList<>();
    private ModelSpec modelSpec;
    private RequestSpec requestSpec;

    public ActionSpec(ModelSpec modelSpec, RequestSpec requestSpec){
        this.modelSpec = modelSpec;
        this.requestSpec = requestSpec;
    }


     // This catches unquoted words used as properties/variables
    public Object propertyMissing(String name) {
        System.out.println("property missing catch " + name);
        return name; 
    }

    // Redirect the resolution of simple_action lang via composite_action
    public void simple_action(Closure closure) throws Exception{

        // Once we have simple action closure, we can convert it to composition action closure
        Closure compositeActionClosure = simpleActionToCompositeActionClosure(closure);
        composite_action(compositeActionClosure);
    }

    public void composite_action(Closure closure) throws Exception{
        
        CompositeActionConfig compositeActionConfig = new CompositeActionConfig(this.modelSpec, this.requestSpec);

        closure.setDelegate(compositeActionConfig);
        closure.setResolveStrategy(Closure.DELEGATE_FIRST);
        closure.call();

        // Validate this duplicate name is present 

        for(CompositeActionConfig compositeActionConfig1 : actionList){

            if(compositeActionConfig1.getCompositeActionName().equals(compositeActionConfig.getCompositeActionName())){
                throw new IllegalArgumentException("Action names can not be duplicate. It must be unique");
            }
        }

        actionList.add(compositeActionConfig);
        // Once all actions of composite actions are resolved then
        // perform tree creation 

        compositeActionConfig.initChecks();
        compositeActionConfig.createDag();
        
    }

    public CompositeActionConfig getActionByName(String name) {
        for (CompositeActionConfig action : actionList) {
            if (action.getCompositeActionName().equalsIgnoreCase(name)) {
                return action;
            }
        }
        return null;
    }

    public boolean hasActionByName(String name) {
        for (CompositeActionConfig action : actionList) {
            if (action.getCompositeActionName().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }


    private Closure simpleActionToCompositeActionClosure(Closure simpleActionClosure){

        return new Closure<Void>(this) {

            public void doCall(){

                invokeMethod("simple_action", simpleActionClosure);
            }
        };
    }
}
