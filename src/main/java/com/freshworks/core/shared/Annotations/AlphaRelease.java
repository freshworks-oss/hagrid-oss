package com.freshworks.core.shared.Annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Features which are annotated with this annotation i.e. @AlphaRelease, it means they are under ideation.
 * Devs has been testing this unit for some use case.
 */
@Retention(RetentionPolicy.RUNTIME) // Retain at runtime so it can be accessed via reflection
@Target({ElementType.METHOD, ElementType.TYPE}) // Can be applied to methods and classes
public @interface AlphaRelease {
    String sourceVersion() default "Version in which it is introduced";
    String targetVersion() default "Version in which it is targeted to be fully supported";
    String useCase() default "Mention list of use cases here, this unit may support";
    String message() default "This API is in alpha release mode. It is experimental mode or ideation mode";
}
