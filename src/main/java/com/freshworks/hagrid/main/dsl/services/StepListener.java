package com.freshworks.hagrid.main.dsl.services;

import com.freshworks.hagrid.main.dsl.config.action.ActionConfig;

/**
 * Optional hook, called each time a DSL step starts to run (HTTP or non-HTTP).
 * Used by tooling such as DSL coverage; when no implementation is registered nothing happens.
 */
public interface StepListener {

    void stepRun(ActionConfig step);
}
