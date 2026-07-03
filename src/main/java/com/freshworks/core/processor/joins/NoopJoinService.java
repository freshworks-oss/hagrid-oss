package com.freshworks.core.processor.joins;


import com.freshworks.core.processor.AbstractAsset;
import com.freshworks.core.processor.AbstractBean;
import com.freshworks.core.processor.Annotations.FreshJoin;
import com.freshworks.core.processor.ProcessorUtility;
import com.freshworks.core.shared.infra.InfraDbKeyValue;
import com.freshworks.core.traverser.TraverserUtility;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Slf4j
@Component("NoopJoinService")
@Scope(value="prototype")
public class NoopJoinService extends AbstractJoinService {

    public NoopJoinService() {
        super();
    }

    @Override
    public List<AbstractAsset> getNonPrimitiveAsset(InfraDbKeyValue abstractKeyValue, String assetName, AbstractAsset abstractAsset, FreshJoin freshJoin) throws Exception {
        List<AbstractAsset> returnList  = new ArrayList<>();
        return returnList;
    }

    @Override
    public AbstractAsset getPrimitiveAsset(String asset, AbstractBean abstractBean, List<String> assetBeanDependencyList) throws Exception {

        if(assetBeanDependencyList.contains(abstractBean.getClass().getName())){
            log.debug("Asset {} depends on single step and depends on this step {}", asset, Joiner.on(",").join(assetBeanDependencyList));
            Class<?> assetClass =  Class.forName(asset, false, this.getClass().getClassLoader());
            List<Method> setterMethods = ProcessorUtility.getAllSetters(assetClass);
            AbstractAsset abstractAssetClassObject = (AbstractAsset) assetClass.getConstructor().newInstance();
            HashMap<String, AbstractBean> unwrappedBeanClassMap = JoinUtility.unwrappedBeanToClassMap(Lists.newArrayList(abstractBean));
            log.debug("Classes unwrapped from main class are {}" , Joiner.on(",").withKeyValueSeparator("=").join(unwrappedBeanClassMap));

            JoinUtility.invokeSetterOnAssetObjectByBean(setterMethods, abstractAssetClassObject, unwrappedBeanClassMap);
            return abstractAssetClassObject;
        }
        else {
            log.debug("Asset {} does not depends on this bean {}", asset, abstractBean.getClass().getName());
            return null;
        }

    }
}
