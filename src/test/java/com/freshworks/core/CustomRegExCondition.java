package com.freshworks.core;

import java.lang.annotation.*;

public class CustomRegExCondition {

    @Target({ElementType.TYPE, ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    public @interface ConditionalOnPropertiesMatch {
        String[] names();
        String[] values();
    }
}
