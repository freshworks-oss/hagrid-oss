package com.freshworks.hagrid.main.dsl.services.resolvers.hooks;


import org.springframework.stereotype.Component;

import com.freshworks.hagrid.main.dsl.config.action.HooksConfig;
import com.freshworks.hagrid.main.dsl.runnable.Hooks;
import com.freshworks.hagrid.main.dsl.runnable.context.ActionContext;

@Component 
public class HooksResolver {


    public Hooks resolve(HooksConfig hooksConfig, ActionContext context){

        Hooks hook = new Hooks();
        hook.setSetupClosure(hooksConfig.getSetupClosure());
        hook.setShouldSkipThisParentClosure(hooksConfig.getShouldSkipThisParentClosure());
        hook.setExecuteClosure(hooksConfig.getExecuteClosure());
        hook.setShoudlPassthroughResponseClosure(hooksConfig.getShouldPassthroughResponse());
        hook.setParseSyncResponseClosure(hooksConfig.getParseSyncResponseClosure());
        hook.setHasMoreClosure(hooksConfig.getHasMoreClosure());
        hook.setActionCloseClosure(hooksConfig.getActionCloseClosure());
        return hook;
    }
    
}
