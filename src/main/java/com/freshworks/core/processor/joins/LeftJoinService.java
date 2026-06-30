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
@Component("LeftJoinService")
@Scope(value="prototype")
public class LeftJoinService extends AbstractJoinService {

    ObjectMapper objectMapper = new ObjectMapper();

    public LeftJoinService() {
        super();
    }

    @Override
    public List<AbstractAsset> getNonPrimitiveAsset(InfraDbKeyValue abstractKeyValue, String asset, AbstractAsset abstractAsset, List<String> assetAssetDependencyList, FreshJoin freshJoin) throws Exception {

        String lookupTraceId = UUID.randomUUID().toString();

        Class<?> assetClass =  Class.forName(asset, false, this.getClass().getClassLoader());
        checkNotNull(freshJoin, "When a assets depends on multiple items at child node then join condition must be provided with Freshjoin annotation");

        this.analyticsService.debugLogEvent("HAGRID_JOIN_SERVICE", "_message", "Left join look up started" , "lookup_trace_id", lookupTraceId, "non_primitive_asset", asset, "incoming_asset", abstractAsset.getClass().getName(), "type" , "left_join", "lookup_name" , freshJoin.uniqueJoinName());
        List<HashMap<String, AbstractAsset>> unwrappedAssetAssetsMapList = lookupStagingArea(lookupTraceId, abstractKeyValue, abstractAsset, freshJoin);

        List<AbstractAsset> returnList  = new ArrayList<>();

        if(unwrappedAssetAssetsMapList.isEmpty()){

            this.analyticsService.debugLogEvent("HAGRID_JOIN_SERVICE", "_message", "Perfect look up not found. As Left join, will generate partial asset" , "lookup_trace_id", lookupTraceId, "non_primitive_asset", asset, "incoming_asset", abstractAsset.getClass().getName(), "type" , "left_join", "lookup_name" , freshJoin.uniqueJoinName());

            if (freshJoin.leftClass().getName().contains(abstractAsset.getClass().getName())){

                // Add self abstractAsset and generate non primitive asset
                HashMap<String, AbstractAsset> map = new HashMap<>();
                map.put(abstractAsset.getClass().getName(), abstractAsset);
                unwrappedAssetAssetsMapList.add(map);
            }

        }
        else{

            this.analyticsService.debugLogEvent("HAGRID_JOIN_SERVICE", "_message", "Perfect look up found. Will generat primitive assets of size given in tag" , "lookup_trace_id", lookupTraceId, "size" , unwrappedAssetAssetsMapList.size(), "non_primitive_asset", asset, "incoming_asset", abstractAsset.getClass().getName(), "type" , "left_join", "lookup_name" , freshJoin.uniqueJoinName());
        }

        for(int i=0; i<unwrappedAssetAssetsMapList.size(); i++){
            List<Method> setterMethods = ProcessorUtility.getAllSetters(assetClass);
            AbstractAsset abstractAssetClassObject = (AbstractAsset) assetClass.getConstructor().newInstance();
            JoinUtility.invokeSetterOnAssetObjectByAsset(setterMethods, abstractAssetClassObject, unwrappedAssetAssetsMapList.get(i));
            log.debug("Asset generated is {}", abstractAssetClassObject.getClass().getName());
            returnList.add(abstractAssetClassObject);
        }

        return  returnList;
        
    }

    @Override
    public AbstractAsset getPrimitiveAsset(String asset, AbstractBean abstractBean, List<String> assetStepDependencyList) throws Exception {
        return null;
    }
}
