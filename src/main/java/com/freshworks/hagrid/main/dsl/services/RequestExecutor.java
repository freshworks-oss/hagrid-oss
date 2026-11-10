package com.freshworks.hagrid.main.dsl.services;

import java.util.Map;

import com.freshworks.hagrid.main.dsl.runnable.request.ActionRequest;
import com.freshworks.hagrid.main.dsl.runnable.request.ActionResponse;

/**
 * Optional hook to change how a DSL request is executed (e.g. inject credentials).
 * When no implementation is registered, {@link RequestService} calls {@code request.executeRequest()}.
 */
public interface RequestExecutor {

    ActionResponse execute(ActionRequest request, Map<String, Object> actionInput) throws Exception;
}
