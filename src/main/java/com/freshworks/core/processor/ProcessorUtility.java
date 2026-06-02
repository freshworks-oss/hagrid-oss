package com.freshworks.core.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshworks.core.processor.Annotations.FreshJoin;
import com.freshworks.core.shared.Namespace;
import com.freshworks.core.shared.analytics.AnalyticsService;
import com.freshworks.core.shared.infra.InfraService;
import com.freshworks.freshindex.index.JsonIndexService;
import com.google.common.base.Optional;
import com.google.common.collect.Multimap;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.io.IOException;
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
        listOfLeftClass.add(freshJoin.leftClass().getName().split("\\$")[0]);
        return listOfLeftClass;
    }


    public static String getLookupClassName(AbstractAsset abstractAsset, FreshJoin freshJoin){

        // Here lookup class name could be same as that of right class name.
        // Here lookup class name could be different right but lookup class would be the nested class of the right class
        String className = null;

        if ( freshJoin.rightClass().getName().startsWith(abstractAsset.getClass().getName())){
            className = freshJoin.rightClass().getName();
        }
        else {
            if(freshJoin.leftClass().getName().startsWith(abstractAsset.getClass().getName())){
                className = freshJoin.leftClass().getName();
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

    
    // TODO: This method need to optimise for insertion into freshIndex and
    // addAndGetIndex
    protected static void publishAbstractAsset(String uuid, List<AbstractAsset> assetsReadyToBePublishedList, InfraService infraService, JsonIndexService jsonIndexService, Namespace namespace, AnalyticsService analyticsService, ObjectMapper freshIndexObjectMapper, MeterRegistry meterRegistry) throws Exception {

        ObjectMapper objectMapper = new ObjectMapper();

        Timer timer = meterRegistry.timer("infra.execution.time", "type", "list", "name", "publisher_list");

        String errorString = timer.record(() -> {

            try {
                if (!assetsReadyToBePublishedList.isEmpty()) {
                    List<String> assetsReadyToBePublishedListInPublisherQueue = new ArrayList<>();
                    List<JsonNode> assetsReadyToBePublishedListInFreshIndex = new ArrayList<>();

                    for (AbstractAsset abstractAsset : assetsReadyToBePublishedList) {
                        assetsReadyToBePublishedListInPublisherQueue
                                .add(objectMapper.writeValueAsString(abstractAsset));
                        String s = freshIndexObjectMapper.writeValueAsString(abstractAsset);
                        JsonNode j = objectMapper.readTree(s);
                        assetsReadyToBePublishedListInFreshIndex.add(j);
                    }

                    List<Long> documentIdList = infraService.getPublisherList()
                            .addAndGetIndexBulk(assetsReadyToBePublishedListInPublisherQueue);
                    if (documentIdList.size() != assetsReadyToBePublishedList.size()) {
                        return "Assets ready to be published are not equal to assets published in publisher list";
                    }
                    analyticsService.meterCounterByIncrement("HAGRID_ASSET_IS_PUBLISHED",
                            assetsReadyToBePublishedList.size());
                    List<String> documentIdListString = new ArrayList<>();
                    for (Long id : documentIdList) {
                        documentIdListString.add(id.toString());
                    }

                    jsonIndexService.indexJsonStringBulk(assetsReadyToBePublishedListInFreshIndex,
                            documentIdListString);
                    assetsReadyToBePublishedList.clear();
                    return null;
                } else {
                    analyticsService.infoEvent("HAGRID_PROCESSOR_TASK_SERVICE", "_message", "No assets to be published",
                            "uuid", uuid, "namespace", namespace.getNamespace());
                    return null;
                }
            }

            catch (Exception e) {
                analyticsService.errorEvent("HAGRID_PROCESSOR_TASK_SERVICE", "_message", e.getMessage(), "uuid", uuid,
                        "namespace", namespace.getNamespace());
                return e.getMessage();
            }
        });

        if (errorString != null) {
            throw new Exception(errorString);
        }
    }


    protected static List<String> getAssetBeanDependencyList(String asset, Multimap<String, String> assetBeanDependencyMap)
            throws IOException {
        return (List<String>) assetBeanDependencyMap.get(asset);

    }


    protected static List<String> getAssetAssetDependencyList(String asset, Multimap<String, String> assetAssetDependencyMap)
            throws IOException {
        return (List<String>) assetAssetDependencyMap.get(asset);

    }

    protected Boolean isAssetDependsOnThisBean(List<String> assetBeanDependencyList, AbstractBean abstractBean) {
        return assetBeanDependencyList.contains(abstractBean.getClass().getName());
    }

    protected static Boolean isAssetDependsOnThisAsset(List<String> assetAssetDependencyList, AbstractAsset abstractAsset) {
        return assetAssetDependencyList.contains(abstractAsset.getClass().getName());
    }


    protected static boolean shouldFilterAsset(AbstractAsset abstractAssetClassObject) {
        return abstractAssetClassObject.filter();
        
    }

}
