package com.freshworks.hagrid.main.dsl.runnable;

import com.freshworks.hagrid.*;

import groovy.lang.Closure;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Action {
 
    private String name;
    private Closure responsePathClosure;
    private Closure<Boolean> inputValidationClosure;

    private ApiModel apiModel;
    private OutputModel outputModel;

    private ActionRequest actionRequest;
    private ActionRequest paginateRequest;

    private Hooks hooks;
    
}
