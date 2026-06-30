package com.freshworks.core.processor.joins;

import static com.google.common.base.Preconditions.checkNotNull;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshworks.core.processor.AbstractAsset;
import com.freshworks.core.processor.AbstractBean;
import com.freshworks.core.processor.ProcessorUtility;
import com.freshworks.core.processor.Annotations.FreshJoin;
import com.freshworks.core.shared.infra.InfraDbKeyValue;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component("InnerJoinService")
@Scope(value="prototype")
public class InnerJoinService extends AbstractJoinService {

    ObjectMapper objectMapper = new ObjectMapper();

    public InnerJoinService() {
        super();
    }

    @Override
    public List<AbstractAsset> getNonPrimitiveAsset(InfraDbKeyValue abstractKeyValue, String asset, AbstractAsset abstractAsset, List<String> assetAssetDependencyList, FreshJoin freshJoin) throws Exception {

        String lookupTraceId = UUID.randomUUID().toString();

        Class<?> assetClass =  Class.forName(asset, false, this.getClass().getClassLoader());
        checkNotNull(freshJoin, "When a assets depends on multiple items at child node then join condition must be provided with Freshjoin annotation");
    
        this.analyticsService.debugLogEvent("HAGRID_JOIN_SERVICE", "_message", "Inner join look up started" , "lookup_trace_id", lookupTraceId , "non_primitive_asset", asset, "incoming_asset", abstractAsset.getClass().getName(), "type" , "inner_join", "lookup_name" , freshJoin.uniqueJoinName());
        List<HashMap<String, AbstractAsset>> unwrappedAssetAssetsMapList = lookupStagingArea(lookupTraceId, abstractKeyValue, abstractAsset, freshJoin);
        List<AbstractAsset> returnList  = new ArrayList<>();

        if(unwrappedAssetAssetsMapList.isEmpty()){
            this.analyticsService.debugLogEvent("HAGRID_JOIN_SERVICE", "_message", "Perfect lookup can not be found. It may be possibles its corresponding assets may not come yet. Moving on as it is inner join which demands perfect lookup to generate non-primitive asset" , "lookup_trace_id", lookupTraceId, "non_primitive_asset", asset, "incoming_asset", abstractAsset.getClass().getName(), "type" , "inner_join", "lookup_name" , freshJoin.uniqueJoinName());
            return returnList;
        }
        else{
            this.analyticsService.debugLogEvent("HAGRID_JOIN_SERVICE", "_message", "Perfect lookup found. Will generate non primitive asset. Number of instances of non-primitive assets generated will be present in size tag" , "lookup_trace_id", lookupTraceId, "size", unwrappedAssetAssetsMapList.size() , "non_primitive_asset", asset, "incoming_asset", abstractAsset.getClass().getName(), "type" , "inner_join", "lookup_name" , freshJoin.uniqueJoinName());
            for(int i=0; i<unwrappedAssetAssetsMapList.size(); i++){
                List<Method> setterMethods = ProcessorUtility.getAllSetters(assetClass);
                AbstractAsset abstractAssetClassObject = (AbstractAsset) assetClass.getConstructor().newInstance();
                JoinUtility.invokeSetterOnAssetObjectByAsset(setterMethods, abstractAssetClassObject, unwrappedAssetAssetsMapList.get(i));
                returnList.add(abstractAssetClassObject);
            }

            return  returnList;
        }
    }

    @Override
    public AbstractAsset getPrimitiveAsset(String asset, AbstractBean abstractBean, List<String> assetStepDependencyList) throws Exception {
        return null;
    }

    private List<Boolean> isAssetAssetDependencyAlreadyExists(List<String> assetAssetDependencyList, List<HashMap<String, AbstractAsset>> unwrappedAssetAssetsMapList) throws Exception {

        List<Boolean> booleanList = new ArrayList<>();

        for(int i=0; i< unwrappedAssetAssetsMapList.size(); i++){
            ArrayList<String> unwrappedKeys = new ArrayList<>(unwrappedAssetAssetsMapList.get(i).keySet());

            // Here fix the case when setField is of primitive type like setName(String name)
            //
            if(unwrappedKeys.containsAll(assetAssetDependencyList)){
                booleanList.add(true);
            }
            else{
                booleanList.add(false);
            }
        }

        return booleanList;
    }
}
