package com.freshworks.hagrid.main;

import java.util.HashMap;

import com.freshworks.hagrid.main.dsl.services.ActionService;

public class SharedActionServiceMap {

    static HashMap<Integer, ActionService> sharedActionServiceStorage = new HashMap<>();

    public static void add(Integer actionServiceHashCode, ActionService actionService){

        sharedActionServiceStorage.put(actionServiceHashCode, actionService);
    }

    public static ActionService get(Integer actionServiceHashCode){

        return sharedActionServiceStorage.get(actionServiceHashCode);
    }

    public static void clear(Integer actionServiceHashCode){

        sharedActionServiceStorage.remove(actionServiceHashCode);
    }
    
}
