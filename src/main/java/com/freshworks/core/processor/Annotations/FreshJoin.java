package com.freshworks.core.processor.Annotations;


import java.lang.annotation.*;

@Documented
@Target(ElementType.TYPE)
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(FreshJoins.class)
public @interface FreshJoin {

    Class<?> rightClass();

    String uniqueJoinName();

    JOIN_TYPE join_type();
    OnField [] onFieldList();

    @interface OnField{
        String rightClassFieldName();
        Class<?> leftClass();
        String leftClassFieldName();
    }

    enum JOIN_TYPE{
        INNER_JOIN,
        LEFT_JOIN,
        RIGHT_JOIN,
        NOOP_JOIN
    }
}
