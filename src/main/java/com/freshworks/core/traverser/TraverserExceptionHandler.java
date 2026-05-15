package com.freshworks.core.traverser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.freshworks.core.traverser.exception.StepFailedException;

import java.lang.reflect.InvocationTargetException;

public class TraverserExceptionHandler {

    static ObjectMapper o = new ObjectMapper();
    public static void handleStepFailedException(StepFailedException e) throws JsonProcessingException, ClassNotFoundException, InterruptedException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {

        ObjectNode o = (ObjectNode) e.getJsonNode();
        o.put("clazz", e.getAbstractBean().getName());
        // I changed it to push it to publisher queue instead of by passing it, as it is more reliable
//        InfraFactory.getProcessorQueue().add(o.toString());
    }
}
