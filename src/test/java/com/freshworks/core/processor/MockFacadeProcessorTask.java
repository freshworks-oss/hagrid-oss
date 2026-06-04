package com.freshworks.core.processor;

import com.freshworks.core.MockFacadeInterface;
import com.freshworks.core.ReturnableMockTypeList;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;

@Component
public class MockFacadeProcessorTask implements MockFacadeInterface {

    @SpyBean
    ProcessorTaskService processorTaskSpy;

    ReturnableMockTypeList<Boolean> isAssetDependsOnThisBean;
    ReturnableMockTypeList<List<String>> getAssetBeanDependencyList;
    ReturnableMockTypeList<Boolean> shouldFilterAsset;


    @Override
    public MockFacadeProcessorTask configure(){
        reset();
        isAssetDependsOnThisBean.add(true);
        getAssetBeanDependencyList.add(Arrays.asList(""));
        shouldFilterAsset.add(true);
        return this;
    }

    public MockFacadeProcessorTask isAssetDependsOnThisBean(Boolean... isAssetDependsOnThisBean){
        this.isAssetDependsOnThisBean.clear();;
        this.isAssetDependsOnThisBean.add(isAssetDependsOnThisBean);
        return this;
    }

    public MockFacadeProcessorTask getAssetBeanDependencyList(List<String>... getAssetBeanDependencyList){
        this.getAssetBeanDependencyList.clear();
        this.getAssetBeanDependencyList.add(getAssetBeanDependencyList);
        return this;
    }

    public MockFacadeProcessorTask shouldFilterAsset(Boolean... shouldFilterAsset){
        this.shouldFilterAsset.clear();;
        this.shouldFilterAsset.add(shouldFilterAsset);
        return this;
    }

    @Override
    public Object build() throws Exception {

        doNothing().when(processorTaskSpy).configure(anyString(), anyList(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        doNothing().when(processorTaskSpy).processBeanForAsset(anyString());
        return processorTaskSpy;
    }
}
