package com.freshworks.core.processor.joins;

import static com.google.common.base.Preconditions.checkNotNull;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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

        Class<?> assetClass =  Class.forName(asset, false, this.getClass().getClassLoader());
        checkNotNull(freshJoin, "When a assets depends on multiple items at child node then join condition must be provided with Freshjoin annotation");
        log.info("Fresh join annotation is mentioned. All Ok ");

        List<HashMap<String, AbstractAsset>> unwrappedAssetAssetsMapList = lookupStagingArea(abstractKeyValue, abstractAsset, freshJoin);

        List<AbstractAsset> returnList  = new ArrayList<>();

        if(unwrappedAssetAssetsMapList.isEmpty()){
            return returnList;
        }
        else{
            List<Boolean> foundList = isAssetBeanDependencyAlreadyExists(assetAssetDependencyList, unwrappedAssetAssetsMapList);
            for(int i=0; i<foundList.size(); i++){
                if(Boolean.TRUE.equals(foundList.get(i))){
                    log.debug("Lookup found");
                    List<Method> setterMethods = ProcessorUtility.getAllSetters(assetClass);
                    AbstractAsset abstractAssetClassObject = (AbstractAsset) assetClass.getConstructor().newInstance();
                    JoinUtility.invokeSetterOnAssetObjectByAsset(setterMethods, abstractAssetClassObject, unwrappedAssetAssetsMapList.get(i));
                    returnList.add(abstractAssetClassObject);

                }
                
            }
            return  returnList;
        }
    }

    @Override
    public AbstractAsset getPrimitiveAsset(String asset, AbstractBean abstractBean, List<String> assetStepDependencyList) throws Exception {
        return null;
    }

    private List<Boolean> isAssetBeanDependencyAlreadyExists(List<String> assetAssetDependencyList, List<HashMap<String, AbstractAsset>> unwrappedAssetAssetsMapList) throws Exception {

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
