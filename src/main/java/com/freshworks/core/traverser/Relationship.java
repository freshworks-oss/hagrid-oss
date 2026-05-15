package com.freshworks.core.traverser;

import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

@Getter
@Setter
public class Relationship {

    private long totalItemsSynced;
    private long successfulItemsSynced;
    private long failedItemsSynced;
    private int status = -100;

    private ReentrantLock relationshipDataChangeLock = new ReentrantLock();

    private ReentrantLock relationshipStatusChangeLock = new ReentrantLock();
    private Condition relationshipStatusChangedCondition = relationshipStatusChangeLock.newCondition();

}
