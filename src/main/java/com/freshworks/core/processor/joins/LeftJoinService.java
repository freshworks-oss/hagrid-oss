package com.freshworks.core.processor.joins;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshworks.core.processor.AbstractAsset;
import com.freshworks.core.processor.AbstractBean;
import com.freshworks.core.processor.Annotations.FreshJoin;
import com.freshworks.core.processor.ProcessorUtility;
import com.freshworks.core.shared.infra.InfraDbKeyValue;
import com.google.common.base.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
@Component("LeftJoinService")
@Scope(value="prototype")
public class LeftJoinService extends AbstractJoinService {

    ObjectMapper objectMapper = new ObjectMapper();

    public LeftJoinService() {
        super();
    }

    @Override
    public List<Optional<AbstractAsset>> getAssetWithFreshJoin(InfraDbKeyValue abstractKeyValue, String asset, AbstractBean abstractBean, List<String> assetBeanDependencyList, FreshJoin freshJoin) throws Exception {

        Class<?> assetClass =  Class.forName(asset, false, this.getClass().getClassLoader());
        checkNotNull(freshJoin, "When a assets depends on multiple items at child node then join condition must be provided with Freshjoin annotation");
        log.info("Fresh join annotation is mentioned. All Ok ");

        List<HashMap<String, AbstractBean>> unwrappedAssetBeansMapList = lookupStagingArea(abstractKeyValue, abstractBean, freshJoin);
        log.debug("Saving main bean as key in mongodb {} " , abstractBean.getClass().getName());

        List<Optional<AbstractAsset>> returnList  = new ArrayList<>();

        if(unwrappedAssetBeansMapList.isEmpty()){
            log.warn("Lookup failed for abstract bean {}", abstractBean.getClass().getName());
            returnList.add(Optional.absent());
            return returnList;
        }
        else{

            for(int i=0; i<unwrappedAssetBeansMapList.size(); i++){
                List<Method> setterMethods = ProcessorUtility.getAllSetters(assetClass);
                AbstractAsset abstractAssetClassObject = (AbstractAsset) assetClass.getConstructor().newInstance();

//        Optional<ArrayList<AbstractBean>> opt = getListOfBeansWhichAlreadyExists(assetBeanDependencyList, assetDocument);
//        ArrayList<AbstractBean> listOfDifferentBeansOnWhichThisAssetDepends = null;
//        if(opt.isPresent()){
//            listOfDifferentBeansOnWhichThisAssetDepends = opt.get();
//        }

//        HashMap<String, AbstractBean> unwrappedStepClassMap = unwrappedBeanToClassMap(listOfDifferentBeansOnWhichThisAssetDepends);
                invokeSetterOnAssetObject(setterMethods, abstractAssetClassObject, unwrappedAssetBeansMapList.get(i));
                log.debug("Asset generated is {}", abstractAssetClassObject.getClass().getName());
                returnList.add(Optional.fromNullable(abstractAssetClassObject));
            }

            return  returnList;
        }
    }

    @Override
    public AbstractAsset getAsset(String asset, AbstractBean abstractBean, List<String> assetStepDependencyList) throws Exception {
        return null;
    }
}
