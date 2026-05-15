package com.freshworks.core.shared.Annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
/**
 * Indicates that the annotated API is in the Beta phase.
 * This annotation is used to inform developers that the API has been unit wise tested, however
 * performance testing is not done yet.
 *
 */

@Retention(RetentionPolicy.RUNTIME) // Retain at runtime so it can be accessed via reflection
@Target({ElementType.METHOD, ElementType.TYPE}) // Can be applied to methods and classes
public @interface BetaRelease {
    String sourceVersion() default "Version in which it is introduced";
    String targetVersion() default "Version in which it is targeted to be fully supported";
    String useCase() default "Mention list of use cases here, this unit may support";
    String message() default "This API is in beta release mode. It can be changed anytime to add some unit or bug fix";
}
