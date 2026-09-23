package com.freshworks.hagrid.main;

import java.util.HashMap;

import com.freshworks.hagrid.main.dsl.services.ActionService;

public class SharedActionServiceMap {

    static HashMap<String, ActionService> sharedActionServiceStorage = new HashMap<>();

    public static void add(String namespace, String actionName, String subActionName, ActionService actionService){

        String uniqueKey = namespace + "_" + actionName + "_" + subActionName;
        sharedActionServiceStorage.put(uniqueKey, actionService);
    }

    public static ActionService get(String namespace, String actionName, String subActionName){

        String uniqueKey = namespace + "_" + actionName + "_" + subActionName;
        return sharedActionServiceStorage.get(uniqueKey);
    }

    public static void clear(String namespace, String actionName, String subActionName){

        String uniqueKey = namespace + "_" + actionName + "_" + subActionName;
        sharedActionServiceStorage.remove(uniqueKey);
    }
    
}
