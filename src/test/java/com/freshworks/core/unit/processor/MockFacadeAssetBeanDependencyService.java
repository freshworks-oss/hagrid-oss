package com.freshworks.core.processor;

import com.freshworks.core.MockFacadeInterface;
import com.freshworks.core.ReturnableMockTypeList;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;

@Component
public class MockFacadeAssetBeanDependencyService implements MockFacadeInterface {

    @SpyBean
    AssetBeanDependencyService assetBeanDependencyServiceSpy;

    ReturnableMockTypeList<ImmutableListMultimap<String, String>> scanner;
    ReturnableMockTypeList<List<String>> findDependencyOfAsset;


    @Override
    public MockFacadeAssetBeanDependencyService configure(){

        reset();
        Multimap<String, String> connectorConfigItemTable = ArrayListMultimap.create();
        connectorConfigItemTable.put("com.freshworks.core.three_seven_zero.assets.fb.data.FbComment", "com.freshworks.core.three_seven_zero.beans.fb.data.FbComment");
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

        doAnswer(scanner.answer()).when(assetBeanDependencyServiceSpy).scanner(anyString(), any());
        doAnswer(findDependencyOfAsset.answer()).when(assetBeanDependencyServiceSpy).findDependencyOfAsset(anyList(), any());

        return assetBeanDependencyServiceSpy;
    }
}
