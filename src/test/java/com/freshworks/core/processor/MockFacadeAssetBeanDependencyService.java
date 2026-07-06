package com.freshworks.core.processor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;

import java.util.ArrayList;
import java.util.List;

import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.freshworks.core.MockFacadeInterface;
import com.freshworks.core.ReturnableMockTypeList;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimap;

@Component
public class MockFacadeAssetBeanDependencyService implements MockFacadeInterface {

    
    AssetBeanDependencyService assetBeanDependencyService;

    @Autowired
    ApplicationContext applicationContext;


    ReturnableMockTypeList<ImmutableListMultimap<String, String>> scanner;
    ReturnableMockTypeList<List<String>> findDependencyOfAsset;


    @Override
    public MockFacadeAssetBeanDependencyService configure(){

        reset();
        Multimap<String, String> connectorConfigItemTable = ArrayListMultimap.create();
        connectorConfigItemTable.put("com.freshworks.core.four_five_zero.assets.fb.data.FbComment", "com.freshworks.core.four_five_zero.beans.fb.data.FbComment");
        scanner.add(ImmutableListMultimap.copyOf(connectorConfigItemTable));

        List<String> dependencyList = new ArrayList<>();
        dependencyList.add("com.freshworks.core.three_seven_zero.beans.fb.data.FbComment");
        findDependencyOfAsset.add(dependencyList);

        return this;
    }


    public MockFacadeAssetBeanDependencyService scanner(ImmutableListMultimap<String, String>... assetBeanDependencyMaps){
        this.scanner.clear();
        this.scanner.add(assetBeanDependencyMaps);
        return this;
    }

    public MockFacadeAssetBeanDependencyService findDependencyOfAsset(List<String>... findDependencyOfAsset){
        this.findDependencyOfAsset.clear();
        this.findDependencyOfAsset.add(findDependencyOfAsset);
        return this;
    }

    @Override
    public AssetBeanDependencyService build() throws Exception {

        assetBeanDependencyService = applicationContext.getBean(AssetBeanDependencyService.class);
        AssetBeanDependencyService assetBeanDependencyServiceSpy = Mockito.spy(assetBeanDependencyService);
        
        doAnswer(scanner.answer()).when(assetBeanDependencyServiceSpy).scanner(anyString(), any());
        doAnswer(findDependencyOfAsset.answer()).when(assetBeanDependencyServiceSpy).findDependencyOfAsset(anyList(), any());

        return assetBeanDependencyServiceSpy;
    }
}
