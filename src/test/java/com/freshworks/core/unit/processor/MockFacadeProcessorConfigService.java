package com.freshworks.core.processor;

import com.freshworks.core.MockFacadeInterface;
import com.freshworks.core.ReturnableMockTypeList;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.stereotype.Component;

import static org.mockito.Mockito.doAnswer;

@Component
public class MockFacadeProcessorConfigService implements MockFacadeInterface {

    @SpyBean
    ProcessorConfigService processorConfigServiceSpy;

    ReturnableMockTypeList<Integer> getProcessorPollCount;
    ReturnableMockTypeList<Integer> getNumberOfParallelProcessor;
    ReturnableMockTypeList<String> getAssetLocation;
    ReturnableMockTypeList<String> getBeanLocation;


    public MockFacadeProcessorConfigService configure(){
        reset();
        getProcessorPollCount.add(1);
        getNumberOfParallelProcessor.add(1);
        getAssetLocation.add("com.freshworks.core.data.fb.assets");
        getBeanLocation.add("com.freshworks.core.data.fb.beans");
        return this;
    }


    public MockFacadeProcessorConfigService getProcessorPollCount(Integer... getProcessorPollCount){
        this.getProcessorPollCount.clear();
        this.getProcessorPollCount.add(getProcessorPollCount);
        return this;
    }

    public MockFacadeProcessorConfigService getNumberOfParallelProcessor(Integer... getNumberOfParallelProcessor){
        this.getNumberOfParallelProcessor.clear();
        this.getNumberOfParallelProcessor.add(getNumberOfParallelProcessor);
        return this;
    }

    public MockFacadeProcessorConfigService getAssetLocation(String... getAssetLocation){
        this.getAssetLocation.clear();
        this.getAssetLocation.add(getAssetLocation);
        return this;
    }


    public MockFacadeProcessorConfigService getBeanLocation(String... getBeanLocation){
        this.getBeanLocation.clear();
        this.getBeanLocation.add(getBeanLocation);
        return this;
    }

    @Override
    public ProcessorConfigService build() throws Exception {

        doAnswer(getProcessorPollCount.answer()).when(processorConfigServiceSpy).getProcessorPollCount();
        doAnswer(getBeanLocation.answer()).when(processorConfigServiceSpy).getBeanLocation();
        doAnswer(getAssetLocation.answer()).when(processorConfigServiceSpy).getAssetLocation();
        doAnswer(getBeanLocation.answer()).when(processorConfigServiceSpy).getBeanLocation();
        return processorConfigServiceSpy;
    }
}
