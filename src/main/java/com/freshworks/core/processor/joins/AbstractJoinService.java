package com.freshworks.core.processor.joins;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshworks.core.processor.AbstractAsset;
import com.freshworks.core.processor.AbstractBean;
import com.freshworks.core.processor.Annotations.FreshJoin;
import com.freshworks.core.processor.ProcessorUtility;
import com.freshworks.core.shared.constants.Constants;
import com.freshworks.core.shared.infra.InfraDbKeyValue;
import com.google.common.base.Optional;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.common.hash.BloomFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
@Component
@Scope(value="prototype")
public abstract class AbstractJoinService {

    BloomFilter<String> bloomFilter;

    public void configure(BloomFilter<String> bloomFilter){
        this.bloomFilter = bloomFilter;
    }

    public String getLookupFieldValueOfLeftClass(AbstractBean abstractBean, FreshJoin freshJoin) throws Exception {

        String fieldValueAsString = UUID.randomUUID().toString();

        FreshJoin.OnField[] onFields = freshJoin.onFieldList();

        for(int i=0; i<onFields.length; i++){
            FreshJoin.OnField onField = onFields[i];
            String s = onField.leftClass().getName();
            // Here s could be the nested class of the abstract bean, hence used the contains instead of equal
            if(s.contains(abstractBean.getClass().getName())){
                Object lookupObject = getLookupObject(freshJoin, abstractBean);
                Object fieldValue = getLookupFieldValue(onField, lookupObject);
                if (Objects.nonNull(fieldValue)) {
                    fieldValueAsString = fieldValue.toString();
                    break;
                }
            }

        }
        return fieldValueAsString;
    }

    public String getLookupFieldValueOfRightClass(AbstractBean abstractBean, FreshJoin freshJoin) throws Exception {

        String fieldValueAsString = UUID.randomUUID().toString();
        FreshJoin.OnField onFieldLookup = null;

        FreshJoin.OnField[] onFields = freshJoin.onFieldList();

        for(int i=0; i<onFields.length; i++){
            FreshJoin.OnField onField = onFields[i];
            String s = onField.leftClass().getName();

            if(ProcessorUtility.isLeafNodeByClazzName(s)){
                onFieldLookup = onField;
                break;
            }
        }
        Object lookupObject = getLookupObject(freshJoin, abstractBean);
        Object fieldValue = getLookupFieldValue(onFieldLookup, lookupObject);
        if (Objects.nonNull(fieldValue)) {
            fieldValueAsString = fieldValue.toString();
            return fieldValueAsString;
        }
        return fieldValueAsString;
    }


    public List<HashMap<String, AbstractBean>> lookupStagingArea(InfraDbKeyValue abstractKeyValue, AbstractBean abstractBean, FreshJoin freshJoin) throws Exception {

        List<HashMap<String, AbstractBean>> returnMap = new ArrayList<>();
//        This is case when abstract bean is related to left class and out of all left classes mentioned in the join, it is the child node.
        ObjectMapper objectMapper = new ObjectMapper();
        if(ProcessorUtility.isLeafNodeByClazzName(abstractBean.getClass().getName()) && !freshJoin.rightClass().getName().contains(abstractBean.getClass().getName())){
            String fieldValue = getLookupFieldValueOfLeftClass(abstractBean, freshJoin);
            abstractKeyValue.putList(freshJoin.uniqueJoinName() + "/" + fieldValue +  "_left",objectMapper.writeValueAsString(abstractBean));
            bloomFilter.put(freshJoin.uniqueJoinName() + "/" + fieldValue +  "_left");

            Boolean doesRightLookupExists = bloomFilter.mightContain(freshJoin.uniqueJoinName() + "/" + fieldValue +  "_right");
            List<String> listOfAllAbstractBeans = new ArrayList<>();
            if(Boolean.TRUE.equals(doesRightLookupExists)) {

                // Go into the database only and only when bloom filter say it may exists
                listOfAllAbstractBeans = abstractKeyValue.getList(freshJoin.uniqueJoinName() + "/" + fieldValue + "_right");
                for (String s : listOfAllAbstractBeans) {
                    AbstractBean rightAbstractBean = objectMapper.readValue(s, AbstractBean.class);
                    Boolean isLookupFound = compare(abstractBean, rightAbstractBean, freshJoin);
                    if (Boolean.TRUE.equals(isLookupFound)) {
                        log.debug("Attribute lookup found for left abstract bean {} with right abstract bean {}", objectMapper.writeValueAsString(abstractBean), objectMapper.writeValueAsString(rightAbstractBean));
                        ArrayList<AbstractBean> abstractBeanArrayList = new ArrayList<AbstractBean>();
                        abstractBeanArrayList.add(0, rightAbstractBean);
                        HashMap<String, AbstractBean> unwrappedRightBeansClassMap = unwrappedBeanToClassMap(abstractBeanArrayList);
                        abstractBeanArrayList.add(0, abstractBean);
                        HashMap<String, AbstractBean> unwrappedLeftBeansClassMap = unwrappedBeanToClassMap(abstractBeanArrayList);
                        unwrappedRightBeansClassMap.putAll(unwrappedLeftBeansClassMap);
                        returnMap.add(unwrappedLeftBeansClassMap);
                    }
                    log.debug("Attribute lookup failed for left abstract bean {} with right abstract bean {}", objectMapper.writeValueAsString(abstractBean), objectMapper.writeValueAsString(rightAbstractBean));
                }
                return returnMap;
            }
            else{

            }

            // Here for left bean, if look up not found still we are sending the unwrapped so that it can be filled up in the assets as per as soon as strategy.
            // We do not do it in right look up ( see below ), because we want to pass it on and when left will lookup will be found.
            ArrayList<AbstractBean> abstractBeanArrayList = new ArrayList<AbstractBean>();
            abstractBeanArrayList.add(abstractBean);
            returnMap.add(unwrappedBeanToClassMap(abstractBeanArrayList));
            return returnMap;
        }

        // This is the case when abstract bean is related to the right class, hence we need to perform the lookup now
        else if(freshJoin.rightClass().getName().contains(abstractBean.getClass().getName())){
            String fieldValue = getLookupFieldValueOfRightClass(abstractBean, freshJoin);
            abstractKeyValue.putList(freshJoin.uniqueJoinName() + "/" + fieldValue +  "_right",objectMapper.writeValueAsString(abstractBean));
            bloomFilter.put(freshJoin.uniqueJoinName() + "/" + fieldValue +  "_right");


            Boolean doesLeftLookupExists = bloomFilter.mightContain(freshJoin.uniqueJoinName() + "/" +fieldValue + "_left");
            List<String> listOfAllAbstractBeans = new ArrayList<>();

            if(Boolean.TRUE.equals(doesLeftLookupExists)){

                listOfAllAbstractBeans = abstractKeyValue.getList(freshJoin.uniqueJoinName() + "/" +fieldValue + "_left");
                log.debug("Size of left lookup class found in database is {}", listOfAllAbstractBeans.size());
                //           TODO: Here unwrap each of the abstract beans, and perform the lookup
                for (String s: listOfAllAbstractBeans) {
                    AbstractBean leftAbstractBean = objectMapper.readValue(s, AbstractBean.class);
                    Boolean isLookupFound = compare(leftAbstractBean, abstractBean, freshJoin);
                    if(Boolean.TRUE.equals(isLookupFound)){
                        log.debug("Attribute lookup found for left abstract bean {} with right abstract bean {}", objectMapper.writeValueAsString(leftAbstractBean), objectMapper.writeValueAsString(abstractBean));
                        ArrayList<AbstractBean> abstractBeanArrayList = new ArrayList<AbstractBean>();
                        abstractBeanArrayList.add(0, leftAbstractBean);
                        HashMap<String, AbstractBean> unwrappedLeftBeansClassMap = unwrappedBeanToClassMap(abstractBeanArrayList);
                        abstractBeanArrayList.add(0, abstractBean);
                        HashMap<String, AbstractBean> unwrappedRightBeansClassMap = unwrappedBeanToClassMap(abstractBeanArrayList);
                        unwrappedLeftBeansClassMap.putAll(unwrappedRightBeansClassMap);
                        returnMap.add(unwrappedLeftBeansClassMap);
                    }
                    log.debug("Attribute lookup failed for left abstract bean {} with right abstract bean {}", objectMapper.writeValueAsString(leftAbstractBean), objectMapper.writeValueAsString(abstractBean));
                }
                return returnMap;
            }
            else{

            }

        }

        // This is the case when the abstract bean is the left class BUT not the child node like Application, ServicePrinciple ( assume, mention in join)
        else{
            ArrayList<AbstractBean> abstractBeanArrayList = new ArrayList<AbstractBean>();
            abstractBeanArrayList.add(abstractBean);
            returnMap.add(unwrappedBeanToClassMap(abstractBeanArrayList));
            return returnMap;
        }

        return returnMap;
    }


    public Boolean compare ( AbstractBean leftAbstractBean, AbstractBean rightAbstractBean, FreshJoin freshJoin) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        ArrayList<AbstractBean> abstractBeanArrayList = new ArrayList<AbstractBean>();
        abstractBeanArrayList.add(leftAbstractBean);
        HashMap<String, AbstractBean> unwrappedLeftBeansClassMap = unwrappedBeanToClassMap(abstractBeanArrayList);

        abstractBeanArrayList.clear();
        abstractBeanArrayList.add(rightAbstractBean);
        HashMap<String, AbstractBean> unwrappedRightBeansClassMap = unwrappedBeanToClassMap(abstractBeanArrayList);
        Boolean commonParentsAreEqual = compareParent(unwrappedLeftBeansClassMap, unwrappedRightBeansClassMap);
        if(Boolean.TRUE.equals(commonParentsAreEqual)){
            log.debug("Parent lookup Found for left abstract bean {} with right abstract bean {}", objectMapper.writeValueAsString(leftAbstractBean), objectMapper.writeValueAsString(rightAbstractBean));
            return compareAttributes(unwrappedLeftBeansClassMap, rightAbstractBean, freshJoin);
        }
        else {

            log.warn("Parent lookup NOT Found for left abstract bean {} with right abstract bean {}", objectMapper.writeValueAsString(leftAbstractBean), objectMapper.writeValueAsString(rightAbstractBean));
            return false;
        }

    }


    public Boolean compareParent(HashMap<String, AbstractBean> unwrappedLeftBeansClassMap, HashMap<String, AbstractBean> unwrappedRightBeansClassMap){

        MapDifference<String, AbstractBean> mapDifference = Maps.difference(unwrappedLeftBeansClassMap, unwrappedRightBeansClassMap);

//        TODO: Here see how guava find the difference in values i.e. AbstractBean, do we have to implement the equals method?
        Map<String, MapDifference.ValueDifference<AbstractBean>> valueDifferenceMap = mapDifference.entriesDiffering();
        if(valueDifferenceMap.isEmpty()){
            return true;
        }
        else{
            return false;
        }
    }

    public Boolean compareAttributes(HashMap<String, AbstractBean> unwrappedLeftBeansClassMap, AbstractBean abstractBean, FreshJoin freshJoin) throws Exception {

        Boolean isLookupFound = false;
        FreshJoin.OnField[] onFields = freshJoin.onFieldList();

        for(int i=0; i<onFields.length; i++){
            FreshJoin.OnField onField = onFields[i];
            String s = onField.leftClass().getName();

            if(Boolean.FALSE.equals(unwrappedLeftBeansClassMap.containsKey(s))){
                log.warn("Fresh join onField contains a left class which is not the part of leftAbstract Bean that is being compared");
                return false;
            }
            Object lookupLeftClassObject = getLookupObject(freshJoin, unwrappedLeftBeansClassMap.get(s));
            Object fieldLeftValue = getLookupFieldValue(onField, lookupLeftClassObject);

            Object lookupRightObject = getLookupObject(freshJoin, abstractBean);
            Object fieldRightValue = getLookupFieldValue(onField, lookupRightObject);

            if(Objects.isNull(fieldLeftValue) || Objects.isNull(fieldRightValue))
            {
                isLookupFound = false;
                break;
            }
            else if(fieldLeftValue.equals(fieldRightValue)){
                isLookupFound = true;
            }
            else {
                isLookupFound = false;
                break;
            }
        }

        log.debug("Compare attribute for right bean {} is {}", abstractBean, isLookupFound);
        return isLookupFound;
    }

    public abstract List<Optional<AbstractAsset>> getAssetWithFreshJoin(InfraDbKeyValue abstractKeyValue, String assetName, AbstractBean abstractBean, List<String> assetStepDependencyList, FreshJoin freshJoin) throws Exception;

    public abstract AbstractAsset getAsset(String assetName, AbstractBean abstractBean, List<String> assetStepDependencyList) throws Exception;

    public Object getLookupObject(FreshJoin freshJoin, AbstractBean abstractBean) throws Exception {

        Class<?> lookupStepClass = Class.forName(ProcessorUtility.getLookupClassName(abstractBean, freshJoin));

        // Here it means, lookup class it nested class
        if(!lookupStepClass.getName().equals(abstractBean.getClass().getName())){
            String[] lookupClassNameSplit = lookupStepClass.getName().split("\\$");
            String nestedClassNameAsString = lookupClassNameSplit[lookupClassNameSplit.length - 1];
            Method getterMethod = abstractBean.getClass().getDeclaredMethod(Constants.GETTER_METHOD_PREFIX + nestedClassNameAsString.substring(0, 1).toUpperCase()
                    + nestedClassNameAsString.substring(1));
            log.debug("Lookup method to get the nested class is {}" , getterMethod.getName());

            // Here check the return type of getterMethod.
            // If return type of getterMethod is list then return 0th element of invoke method
            // otherwise return as it is


            // As of now, returned type of List or Map is not supported, checkout the

            if(Collection.class.isAssignableFrom(getterMethod.getReturnType())){

                throw new Exception("Return type of Array and Maps are not supported. Rather use map function of the bean to transform the array into singleton beans");
            }

            else{
                return getterMethod.invoke(abstractBean);
            }

        }
        else {
            return abstractBean;
        }
    }

    public Object getLookupFieldValue(FreshJoin.OnField freshJoinField, Object lookupObject) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        Class<?> lookupStepClass = lookupObject.getClass();
        Method getterMethod = lookupStepClass.getDeclaredMethod(Constants.GETTER_METHOD_PREFIX + getLookupField(lookupStepClass, freshJoinField).substring(0, 1).toUpperCase()
                + getLookupField(lookupStepClass, freshJoinField).substring(1));
        Object fieldValue = getterMethod.invoke(lookupObject);
        if (Objects.isNull(fieldValue)) {
            log.warn("lookup field value can not be null");
        }
        return fieldValue;
    }

    public String getLookupField(Class<?> lookupClass, FreshJoin.OnField freshJoinField){

        String fieldName;
        String className = lookupClass.getName();
        if ( freshJoinField.leftClass().getName().startsWith(className)){
            fieldName = freshJoinField.leftClassFieldName();
        }

        else{
            fieldName = freshJoinField.rightClassFieldName();
        }

        checkNotNull(fieldName, "lookup field name can not be determined");
        return fieldName;

    }

    public HashMap<String, AbstractBean> unwrappedBeanToClassMap(ArrayList<AbstractBean> abstractBeanList) throws ClassNotFoundException, IOException {

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


    public void invokeSetterOnAssetObject(List<Method> setterMethods, AbstractAsset abstractAssetClassObject, HashMap<String, AbstractBean> unwrappedStepClassMap) throws InvocationTargetException, IllegalAccessException {

        for (Method method: setterMethods) {
            Boolean doesAllMethodParameterExists = false;

            log.debug("Setting value for method {}" + method.getName());
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
                log.debug("Can not execute the method {} because one of its parameter is not yet fetched via steps", method.getName());
            }
        }
    }

}
