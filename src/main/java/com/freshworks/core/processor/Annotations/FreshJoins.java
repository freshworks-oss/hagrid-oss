package com.freshworks.core.processor.Annotations;


import java.lang.annotation.*;

@Documented
@Target(ElementType.TYPE)
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface FreshJoins {
    FreshJoin[] value();
}
