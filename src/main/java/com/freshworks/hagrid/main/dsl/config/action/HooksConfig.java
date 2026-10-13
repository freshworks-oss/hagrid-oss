package com.freshworks.hagrid.main.dsl.config.action;


import com.freshworks.hagrid.main.dsl.config.requests.RequestSpec;
import com.freshworks.hagrid.main.dsl.runnable.context.ActionContext.ParseSyncResponse;
import com.freshworks.hagrid.main.dsl.runnable.request.ActionResponse;

import groovy.lang.Closure;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HooksConfig {

    RequestSpec requestSpec;

    public HooksConfig(RequestSpec requestSpec){
        this.requestSpec = requestSpec;
    }
    
    Closure<Void> setupClosure;
    Closure<Boolean> shouldSkipThisParentClosure;
    Closure<ActionResponse> executeClosure;
    Closure<Boolean> shouldPassthroughResponse;
    Closure<ParseSyncResponse> parseSyncResponseClosure;
    Closure<Boolean> hasMoreClosure;
    Closure<Void> actionCloseClosure;
    


    // Runs just once before any action has run  
    public void action_setup(Closure setupClosure){
        this.setupClosure = setupClosure;
    }

    public void should_skip_this_parent(Closure shouldSkipThisParentClosure){
        this.shouldSkipThisParentClosure = shouldSkipThisParentClosure;
    }

    public void execute(Closure executeClosure){
        this.executeClosure = executeClosure;
    }

    public void should_passthrough_response(Closure shouldPassthroughResponse){
        this.shouldPassthroughResponse = shouldPassthroughResponse;
    }

    // Runs just once after action is completed
    public void action_parse_response(Closure parseSyncResponseClosure){
        this.parseSyncResponseClosure = parseSyncResponseClosure;
    }

    // Runs to check for more pages 
    public void has_more(Closure hasMoreClosure){
        this.hasMoreClosure = hasMoreClosure;
    }

    // Runs just once after action is completed
    public void action_close(Closure actionCloseClosure){
        this.actionCloseClosure = actionCloseClosure;
    }
}
