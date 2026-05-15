package com.freshworks.core.traverser.Annotations;

import java.lang.annotation.*;

@Documented
@Target(ElementType.TYPE)
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface FreshHierarchy {

    Class<?>[] parentClass() default Void.class;
    int rateLimit() default 0;
    int duration() default 1;

    boolean ignore() default false;
}

