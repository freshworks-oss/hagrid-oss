package com.freshworks.core.traverser.Annotations;

import com.freshworks.core.traverser.DagNode;

import java.lang.annotation.*;

@Documented
@Target(ElementType.TYPE)
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface CustomDagNode {

    Class<? extends DagNode> parentClass() default DagNode.class;
}
