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
import com.freshworks.core.TestUtility;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimap;

@Component
public class MockFacadeAssetAssetDependencyService implements MockFacadeInterface {

    @Autowired
    ApplicationContext applicationContext;

    AssetAssetDependencyService assetAssetDependencyService;    
    ReturnableMockTypeList<ImmutableListMultimap<String, String>> scanner;
    ReturnableMockTypeList<List<String>> findDependencyOfAsset;


    @Override
    public MockFacadeAssetAssetDependencyService configure(){

        reset();
        Multimap<String, String> connectorConfigItemTable = ArrayListMultimap.create();
        connectorConfigItemTable.put("com.freshworks.core." + TestUtility.getReleaseVerion() + ".assets.fb.data.FbComment", "com.freshworks.core." + TestUtility.getReleaseVerion() + ".beans.fb.data.FbComment");
        scanner.add(ImmutableListMultimap.copyOf(connectorConfigItemTable));

        List<String> dependencyList = new ArrayList<>();
        dependencyList.add("com.freshworks.core." + TestUtility.getReleaseVerion() + ".beans.fb.data.FbComment");
        findDependencyOfAsset.add(dependencyList);

        return this;
    }


    public MockFacadeAssetAssetDependencyService scanner(ImmutableListMultimap<String, String>... assetAssetDependencyMaps){
        this.scanner.clear();
        this.scanner.add(assetAssetDependencyMaps);
        return this;
    }

    public MockFacadeAssetAssetDependencyService findDependencyOfAsset(List<String>... findDependencyOfAsset){
        this.findDependencyOfAsset.clear();
        this.findDependencyOfAsset.add(findDependencyOfAsset);
        return this;
    }

    @Override
    public AssetAssetDependencyService build() throws Exception {
        assetAssetDependencyService = applicationContext.getBean(AssetAssetDependencyService.class);
        AssetAssetDependencyService assetAssetDependencyServiceSpy = Mockito.spy(assetAssetDependencyService);
        doAnswer(scanner.answer()).when(assetAssetDependencyServiceSpy).scanner(anyString(), any());
        doAnswer(findDependencyOfAsset.answer()).when(assetAssetDependencyServiceSpy).findDependencyOfAsset(anyList(), any());

        return assetAssetDependencyServiceSpy;
    }
}
