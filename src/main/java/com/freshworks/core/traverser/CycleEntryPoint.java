package com.freshworks.core.traverser;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CycleEntryPoint {

    DagNode cycleNode;
    DagNode parentNode;
    NodeRelationship relationship;
}
