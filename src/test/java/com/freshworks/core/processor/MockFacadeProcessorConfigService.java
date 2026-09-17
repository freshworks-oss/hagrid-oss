package com.freshworks.core.processor;

import com.freshworks.core.MockFacadeInterface;
import com.freshworks.core.ReturnableMockTypeList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import static org.mockito.Mockito.doAnswer;

import org.mockito.Mockito;

@Component
public class MockFacadeProcessorConfigService implements MockFacadeInterface {

    @Autowired
    ApplicationContext applicationContext;

    ProcessorConfigService processorConfigService;
    ReturnableMockTypeList<Integer> getProcessorPollCount;
    ReturnableMockTypeList<Integer> getNumberOfParallelProcessor;


    public MockFacadeProcessorConfigService configure(){
        reset();
        getProcessorPollCount.add(1);
        getNumberOfParallelProcessor.add(1);
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

    @Override
    public ProcessorConfigService build() throws Exception {

        ProcessorConfigService processorConfigService = applicationContext.getBean(ProcessorConfigService.class);
        ProcessorConfigService processorConfigServiceSpy = Mockito.spy(processorConfigService);
        
        doAnswer(getProcessorPollCount.answer()).when(processorConfigServiceSpy).getProcessorPollCount();
        doAnswer(getNumberOfParallelProcessor.answer()).when(processorConfigServiceSpy).getNumberOfParallelProcessor();
        return processorConfigServiceSpy;
    }
}
