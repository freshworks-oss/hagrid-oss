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
@Component("LeftJoinService")
@Scope(value="prototype")
public class LeftJoinService extends AbstractJoinService {

    ObjectMapper objectMapper = new ObjectMapper();

    public LeftJoinService() {
        super();
    }

    @Override
    public List<AbstractAsset> getNonPrimitiveAsset(InfraDbKeyValue abstractKeyValue, String asset, AbstractAsset abstractAsset, List<String> assetAssetDependencyList, FreshJoin freshJoin) throws Exception {

        Class<?> assetClass =  Class.forName(asset, false, this.getClass().getClassLoader());
        checkNotNull(freshJoin, "When a assets depends on multiple items at child node then join condition must be provided with Freshjoin annotation");
        log.info("Fresh join annotation is mentioned. All Ok ");

        List<HashMap<String, AbstractAsset>> unwrappedAssetAssetsMapList = lookupStagingArea(abstractKeyValue, abstractAsset, freshJoin);
        log.debug("Saving main bean as key in mongodb {} " , abstractAsset.getClass().getName());

        List<AbstractAsset> returnList  = new ArrayList<>();

        if(unwrappedAssetAssetsMapList.isEmpty()){
            log.warn("Lookup failed for abstract bean {}", abstractAsset.getClass().getName());
            return returnList;
        }
        else{

            for(int i=0; i<unwrappedAssetAssetsMapList.size(); i++){
                List<Method> setterMethods = ProcessorUtility.getAllSetters(assetClass);
                AbstractAsset abstractAssetClassObject = (AbstractAsset) assetClass.getConstructor().newInstance();
                JoinUtility.invokeSetterOnAssetObjectByAsset(setterMethods, abstractAssetClassObject, unwrappedAssetAssetsMapList.get(i));
                log.debug("Asset generated is {}", abstractAssetClassObject.getClass().getName());
                returnList.add(abstractAssetClassObject);
            }

            return  returnList;
        }
    }

    @Override
    public AbstractAsset getPrimitiveAsset(String asset, AbstractBean abstractBean, List<String> assetStepDependencyList) throws Exception {
        return null;
    }
}
