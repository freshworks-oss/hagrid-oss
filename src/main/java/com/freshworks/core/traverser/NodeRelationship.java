package com.freshworks.core.traverser;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

@Getter
@Setter
public class NodeRelationship {

    public enum REL_SWITCH{

        ON,
        OFF
    }

    private long totalItemsSynced;
    private long successfulItemsSynced;
    private long failedItemsSynced;
    private int status = -100;
    private REL_SWITCH relSwitch = REL_SWITCH.ON;
    private HashMap<String, Boolean> featureMap = new HashMap<>();

    private ReentrantLock relationshipDataChangeLock = new ReentrantLock();

    private ReentrantLock relationshipStatusChangeLock = new ReentrantLock();
    private Condition relationshipStatusChangedCondition = relationshipStatusChangeLock.newCondition();

    public void enableFeature(String feature){
        featureMap.put(feature, true);
    }

    public void disableFeature(String feature){
        featureMap.put(feature, false);
    }

    public void clearFeature(String feature){

        if(featureMap.containsKey(feature)){
            featureMap.remove(feature);
        }
    }

    public boolean hasFeature(String feature ){

        return featureMap.containsKey(feature);
    }
}
