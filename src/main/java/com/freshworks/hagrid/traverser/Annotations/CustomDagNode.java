package com.freshworks.hagrid.traverser.Annotations;

import java.lang.annotation.*;

import com.freshworks.hagrid.traverser.DagNode;

@Documented
@Target(ElementType.TYPE)
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface CustomDagNode {

    Class<? extends DagNode> parentClass() default DagNode.class;
}
