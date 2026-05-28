package com.freshworks.core.processor.Annotations;


import java.lang.annotation.*;

import com.freshworks.core.processor.AbstractAsset;

@Documented
@Target(ElementType.TYPE)
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface FreshJoin {

    Class<? extends AbstractAsset> rightClass();
    String rightClassFieldName();
    Class<? extends AbstractAsset> leftClass();
    String leftClassFieldName();
    String uniqueJoinName();
    JOIN_TYPE join_type();
    
    enum JOIN_TYPE{
        INNER_JOIN,
        LEFT_JOIN,
        RIGHT_JOIN,
        NOOP_JOIN
    }
}
