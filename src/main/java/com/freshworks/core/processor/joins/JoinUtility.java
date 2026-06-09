package com.freshworks.core.processor.joins;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.freshworks.core.processor.AbstractAsset;
import com.freshworks.core.processor.AbstractBean;
import com.freshworks.core.processor.ProcessorUtility;
import com.freshworks.core.processor.Annotations.FreshJoin;
import com.freshworks.core.shared.constants.Constants;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;

import static com.google.common.base.Preconditions.checkNotNull;

public class JoinUtility {


    public static HashMap<String, AbstractBean> unwrappedBeanToClassMap(ArrayList<AbstractBean> abstractBeanList) throws ClassNotFoundException, IOException {

        HashMap<String, AbstractBean> unwrappedStepClassMap = new HashMap<>();
        for(int i=0; i< abstractBeanList.size(); i++){
            AbstractBean abstractBean = abstractBeanList.get(i);
            unwrappedStepClassMap.put(abstractBean.getClass().getName(), abstractBean);
            while (abstractBean.hasParentBean()) {
                unwrappedStepClassMap.put(abstractBean.getParentBean().getClass().getName(), abstractBean.getParentBean());
                abstractBean = abstractBean.getParentBean();
            }
        }
        return unwrappedStepClassMap;
    }

    public static String getLookupField(Class<?> lookupClass, FreshJoin freshJoin){

        String fieldName;
        String className = lookupClass.getName();
        if ( freshJoin.leftClass().getName().startsWith(className)){
            fieldName = freshJoin.leftClassFieldName();
        }

        else{
            fieldName = freshJoin.rightClassFieldName();
        }

        checkNotNull(fieldName, "lookup field name can not be determined");
        return fieldName;

    }

    public static String getLookupFieldValueOfRightClass(AbstractAsset abstractBean, FreshJoin freshJoin) throws Exception {

        String fieldValueAsString = UUID.randomUUID().toString();
        String s = freshJoin.leftClass().getName();
        Object lookupObject = getLookupObject(freshJoin, abstractBean);
        Object fieldValue = getLookupFieldValue(freshJoin, lookupObject);
        if (Objects.nonNull(fieldValue)) {
            fieldValueAsString = fieldValue.toString();
            return fieldValueAsString;
        }
        return fieldValueAsString;
    }
    

    public static Object getLookupObject(FreshJoin freshJoin, AbstractAsset abstractAsset) throws Exception {

        Class<?> lookupStepClass = Class.forName(ProcessorUtility.getLookupClassName(abstractAsset, freshJoin));

        // Here it means, lookup class it nested class
        if(!lookupStepClass.getName().equals(abstractAsset.getClass().getName())){
            String[] lookupClassNameSplit = lookupStepClass.getName().split("\\$");
            String nestedClassNameAsString = lookupClassNameSplit[lookupClassNameSplit.length - 1];
            Method getterMethod = abstractAsset.getClass().getDeclaredMethod(Constants.GETTER_METHOD_PREFIX + nestedClassNameAsString.substring(0, 1).toUpperCase() + nestedClassNameAsString.substring(1));

            // Here check the return type of getterMethod.
            // If return type of getterMethod is list then return 0th element of invoke method
            // otherwise return as it is


            // As of now, returned type of List or Map is not supported, checkout the

            if(Collection.class.isAssignableFrom(getterMethod.getReturnType())){

                throw new Exception("Return type of Array and Maps are not supported. Rather use map function of the bean to transform the array into singleton beans");
            }

            else{
                return getterMethod.invoke(abstractAsset);
            }

        }
        else {
            return abstractAsset;
        }
    }



    public static Object getLookupFieldValue(FreshJoin freshJoin, Object lookupObject) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        Class<?> lookupStepClass = lookupObject.getClass();
        Method getterMethod = lookupStepClass.getDeclaredMethod(Constants.GETTER_METHOD_PREFIX + getLookupField(lookupStepClass, freshJoin).substring(0, 1).toUpperCase()
                + getLookupField(lookupStepClass, freshJoin).substring(1));
        Object fieldValue = getterMethod.invoke(lookupObject);
        if (Objects.isNull(fieldValue)) {
            
        }
        return fieldValue;
    }
   
    public static String getLookupFieldValueOfLeftClass(AbstractAsset abstractAsset, FreshJoin freshJoin) throws Exception {

        String fieldValueAsString = UUID.randomUUID().toString();
        String s = freshJoin.leftClass().getName();
        // Here s could be the nested class of the abstract bean, hence used the contains instead of equal
        if(s.contains(abstractAsset.getClass().getName())){
            Object lookupObject = getLookupObject(freshJoin, abstractAsset);
            Object fieldValue = getLookupFieldValue(freshJoin, lookupObject);
            if (Objects.nonNull(fieldValue)) {
                fieldValueAsString = fieldValue.toString();
            }
        }
        return fieldValueAsString;
    }

    public static void invokeSetterOnAssetObjectByBean(List<Method> setterMethods, AbstractAsset abstractAssetClassObject, HashMap<String, AbstractBean> unwrappedStepClassMap) throws InvocationTargetException, IllegalAccessException {

        for (Method method: setterMethods) {
            Boolean doesAllMethodParameterExists = false;
            Class<?> [] assetMethodParameterList = method.getParameterTypes();
            Object[] o = new Object[assetMethodParameterList.length];
            for(int i =0; i< assetMethodParameterList.length; i++){
                Object x = unwrappedStepClassMap.get(assetMethodParameterList[i].getName());
                if(x == null){
                    doesAllMethodParameterExists = false;
                    break;
                }
                else{
                    o[i] = unwrappedStepClassMap.get(assetMethodParameterList[i].getName());
                    doesAllMethodParameterExists = true;
                }

            }

            if(doesAllMethodParameterExists){
                method.invoke(abstractAssetClassObject,o);
            }
            else{
                
            }
        }
    }

        public static void invokeSetterOnAssetObjectByAsset(List<Method> setterMethods, AbstractAsset abstractAssetClassObject, HashMap<String, AbstractAsset> unwrappedStepClassMap) throws InvocationTargetException, IllegalAccessException {

        for (Method method: setterMethods) {
            Boolean doesAllMethodParameterExists = false;
            Class<?> [] assetMethodParameterList = method.getParameterTypes();
            Object[] o = new Object[assetMethodParameterList.length];
            for(int i =0; i< assetMethodParameterList.length; i++){
                Object x = unwrappedStepClassMap.get(assetMethodParameterList[i].getName());
                if(x == null){
                    doesAllMethodParameterExists = false;
                    break;
                }
                else{
                    o[i] = unwrappedStepClassMap.get(assetMethodParameterList[i].getName());
                    doesAllMethodParameterExists = true;
                }

            }

            if(doesAllMethodParameterExists){
                method.invoke(abstractAssetClassObject,o);
            }
            else{
                

            }
        }
    }
}
