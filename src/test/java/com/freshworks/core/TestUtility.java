package com.freshworks.core;

public class TestUtility {
    
    public static void callMethod(Object instance, String methodName, String value) {
        try {
            // Look for a method matching the name that accepts a single String parameter
            java.lang.reflect.Method method = instance.getClass().getMethod(methodName, String.class);
            method.invoke(instance, value);
        } 
        
        catch (Exception e) {
            throw new RuntimeException("Failed to invoke setter: " + methodName + " on " + instance.getClass().getName(), e);
        }
    }

    public static String getReleaseVerion(){

        return  System.getProperty("spring.profiles.active").split("\\.")[0];
    }
}
