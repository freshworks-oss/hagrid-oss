package com.freshworks.core.processor.Annotations;


import java.lang.annotation.*;

@Documented
@Target(ElementType.FIELD)
@Inherited
@Retention(RetentionPolicy.RUNTIME)
public @interface FreshIndex {
}
