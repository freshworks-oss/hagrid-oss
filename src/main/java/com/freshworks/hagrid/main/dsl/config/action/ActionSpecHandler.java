package com.freshworks.hagrid.main.dsl.config.action;

import groovy.lang.Closure;
import lombok.Getter;

@Getter
public class ActionSpecHandler {

    private final ActionSpec actionSpec;

    public ActionSpecHandler(ActionSpec actionSpec) {
        this.actionSpec = actionSpec;
    }

    public void actions(Closure closure) {
        closure.setDelegate(this.actionSpec);
        closure.setResolveStrategy(Closure.DELEGATE_FIRST);
        closure.call();
    }
}
