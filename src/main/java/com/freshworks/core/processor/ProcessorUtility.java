package com.freshworks.core.processor;

import com.freshworks.core.processor.Annotations.FreshJoin;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class ProcessorUtility {



    // I am not sure why do we need this function. As of now I am by default returning true;
    // If things works fine, then we can eliminate this function completely.
    public static Boolean isLeafNodeByClazzName(String clazzName){

//        HashMap<String, String> metaData = getStepNBeanDataByClassName(clazzName);
//        String stepName = metaData.get("step");
//        checkNotNull(rootNode, "Can not find DAG for the service %s", x.get(service));
//        TreeNode<String> actualNode = rootNode.find(stepName);
//        checkNotNull(actualNode, "Abstract bean's corresponding step is not found in tree");
//        if(actualNode.isLeaf()){
//            return true;
//        }
//        else{
//            return false;
//        }

        return true;
    }

    public static Class<?> getClassByClassName(String clazzName) throws ClassNotFoundException {

        return Class.forName(clazzName, false, ProcessorUtility.class.getClassLoader());
    }

    public static HashSet<String> getFreshJoinLeftClassNameList(FreshJoin freshJoin){
        HashSet<String> listOfLeftClass = new HashSet<>();

        FreshJoin.OnField[] onFields = freshJoin.onFieldList();
        for(int i=0; i<onFields.length; i++){
            listOfLeftClass.add(onFields[i].leftClass().getName().split("\\$")[0]);
        }

        return listOfLeftClass;
    }


    public static String getLookupClassName(AbstractBean abstractBean, FreshJoin freshJoin){

        // Here lookup class name could be same as that of right class name.
        // Here lookup class name could be different right but lookup class would be the nested class of the right class
        String className = null;

        if ( freshJoin.rightClass().getName().startsWith(abstractBean.getClass().getName())){
            className = freshJoin.rightClass().getName();
        }
        else {
            for (FreshJoin.OnField onField:
                    freshJoin.onFieldList()) {
                if(onField.leftClass().getName().startsWith(abstractBean.getClass().getName())){
                    className = onField.leftClass().getName();
                    break;
                }
            }
        }

        return className;
    }


    public static List<Method> getAllSetters(Class<?> c){
        Method[] allMethods = c.getDeclaredMethods();
        List<Method> setters = new ArrayList<>();
        for(Method method : allMethods) {
            if(method.getName().startsWith("set")) {
                setters.add(method);
            }
        }

        return setters;
    }
}
