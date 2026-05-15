package com.freshworks.core.shared.Annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated API is going to be retired in next major release.
 * This annotation is used to inform developers that the API is going to be retired in next major release
 *
 */

@Retention(RetentionPolicy.RUNTIME) // Retain at runtime so it can be accessed via reflection
@Target({ElementType.METHOD, ElementType.TYPE}) // Can be applied to methods and classes
public @interface Retire {

    String targetVersion() default "Version in which it is targeted to be fully retire";
    String alternate() default "Mention alternate to this unit";
    String message() default "This API is going to be deprecated in next major release.";
}
