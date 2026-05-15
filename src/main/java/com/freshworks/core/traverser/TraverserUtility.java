package com.freshworks.core.traverser;

import com.freshworks.core.shared.infra.InfraDbKeyValue;
import com.freshworks.core.shared.infra.InfraDbList;
import com.freshworks.core.traverser.Annotations.FreshHierarchy;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TraverserUtility {


    public static int getClassRateLimit(String clazzName) throws ClassNotFoundException {

        Class<?> clazz = Class.forName(clazzName, false, TraverserUtility.class.getClassLoader());
        FreshHierarchy freshHierarchy = clazz.getAnnotation(FreshHierarchy.class);
        return freshHierarchy.rateLimit();
    }

    public static Class<?> getClassByClassName(String clazzName) throws ClassNotFoundException {

        return Class.forName(clazzName, false, TraverserUtility.class.getClassLoader());
    }

    public static AbstractStep getStepObject(String clazzName, InfraDbList infraDbList, InfraDbKeyValue infraDbKeyValue) throws ClassNotFoundException {

        try{
            Class<?> clazz = Class.forName(clazzName, false, TraverserUtility.class.getClassLoader());
            AbstractStep abstractStep = (AbstractStep) clazz.getConstructor(InfraDbList.class, InfraDbKeyValue.class).newInstance(infraDbList, infraDbKeyValue);
            return abstractStep;
        }
        catch(Exception e){
            log.warn("exception is {}", e.getStackTrace());
            return null;
        }
    }

    public static int getClassRateLimitDuration(String clazzName) throws ClassNotFoundException {

        Class<?> clazz = Class.forName(clazzName, false, TraverserUtility.class.getClassLoader());
        FreshHierarchy freshHierarchy = clazz.getAnnotation(FreshHierarchy.class);
        return freshHierarchy.duration();
    }

}
