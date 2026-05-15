package com.freshworks.core.shared.Annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Indicates that the annotated API is the strong candidate for release in production
 * This annotation is used to inform developers that the API has been unit wise tested as well as
 * performance tested, however, it is under UAT to fix any bugs
 */

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface ReleaseCandidate {
    String sourceVersion() default "Version in which it is introduced";
    String targetVersion() default "Version in which it is targeted to be fully supported";
    String useCase() default "Mention list of use cases here, this unit may support";
    String message() default "This API qualifies for release candidate. It is tested and candidate release in production";
}
