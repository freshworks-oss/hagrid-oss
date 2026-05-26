package com.freshworks.core;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.*;

import java.util.Map;

public class CustomRegExConditionComparator implements Condition {

    String springTestSuite;
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {

        String activeProfile = context.getEnvironment().getProperty("spring.profiles.active");

        if (activeProfile == null) {
            return false;
        }

        String hagridVersion = activeProfile.split("\\.")[0];
        String testSuite = activeProfile.split("\\.")[1];

        String className = "";

        // If the annotation is applied to a class
        if (metadata instanceof AnnotationMetadata) {
            AnnotationMetadata classMetadata = (AnnotationMetadata) metadata;
            className = classMetadata.getClassName(); // Use getClassName() directly
        }
        // If the annotation is applied to a method
        else if (metadata instanceof MethodMetadata) {
            MethodMetadata methodMetadata = (MethodMetadata) metadata;
            className = methodMetadata.getDeclaringClassName();
        }

        if(className.contains(hagridVersion) && className.contains( "." + testSuite + ".")){
            return true;
        }
        else{
            return false;
        }


    }
}
