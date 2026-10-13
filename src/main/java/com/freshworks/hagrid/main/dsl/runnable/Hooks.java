package com.freshworks.hagrid.main.dsl.runnable;

import java.util.Map;

import com.freshworks.hagrid.main.dsl.runnable.context.ActionContext;
import com.freshworks.hagrid.main.dsl.runnable.context.ActionContext.ParseSyncResponse;
import com.freshworks.hagrid.main.dsl.runnable.request.ActionResponse;

import groovy.lang.Closure;
import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter 
public class Hooks {
    

    Closure<Void> setupClosure;
    Closure<Boolean> shouldSkipThisParentClosure;
    Closure<ActionResponse> executeClosure;
    Closure<Boolean> shoudlPassthroughResponseClosure;
    Closure<ParseSyncResponse> parseSyncResponseClosure;
    Closure<Boolean> hasMoreClosure;
    Closure<Void> actionCloseClosure;

    public boolean call_shouldSkipThisParent(ActionContext context){
        
        shouldSkipThisParentClosure.setDelegate(context);
        shouldSkipThisParentClosure.setResolveStrategy(Closure.DELEGATE_FIRST);
        return (boolean)shouldSkipThisParentClosure.call();
    }

    public void call_setupClosure(ActionContext context){
        
        setupClosure.setDelegate(context);
        setupClosure.setResolveStrategy(Closure.DELEGATE_FIRST);
        setupClosure.call();
    }

    public ActionResponse call_executeClosure(ActionContext context){
        
        executeClosure.setDelegate(context);
        executeClosure.setResolveStrategy(Closure.DELEGATE_FIRST);
        return executeClosure.call();
    }

    public boolean call_shouldPassthroughResponse(ActionContext context){
        
        shoudlPassthroughResponseClosure.setDelegate(context);
        shouldSkipThisParentClosure.setResolveStrategy(Closure.DELEGATE_FIRST);
        return shoudlPassthroughResponseClosure.call();
    }

    public ParseSyncResponse call_parseSyncResponseClosure(ActionContext context){
        
        parseSyncResponseClosure.setDelegate(context);
        parseSyncResponseClosure.setResolveStrategy(Closure.DELEGATE_FIRST);
        return parseSyncResponseClosure.call();
    }

    public boolean call_hasMoreClosure(ActionContext context){
        
        hasMoreClosure.setDelegate(context);
        hasMoreClosure.setResolveStrategy(Closure.DELEGATE_FIRST);
        return hasMoreClosure.call();
    }

    public void call_actionCloseClosure(ActionContext context){
        
        actionCloseClosure.setDelegate(context);
        actionCloseClosure.setResolveStrategy(Closure.DELEGATE_FIRST);
        actionCloseClosure.call();
    }
}
