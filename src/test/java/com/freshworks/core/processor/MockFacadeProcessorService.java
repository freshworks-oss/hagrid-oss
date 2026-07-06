package com.freshworks.core.processor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;

import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.freshworks.core.MockFacadeInterface;
import com.freshworks.core.ReturnableMockTypeList;
import com.freshworks.core.shared.analytics.AnalyticsService;


@Component
public class MockFacadeProcessorService implements MockFacadeInterface {

    @Autowired
    ApplicationContext applicationContext;

    ReturnableMockTypeList<AnalyticsService> analyticsService;
    
    ProcessorService processorService;

    @Override
    public MockFacadeProcessorService configure(){
        reset();
        return this;
    }


    @Override
    public ProcessorService build() throws Exception {

        processorService = applicationContext.getBean(ProcessorService.class);
        ProcessorService processorServiceSpy = Mockito.spy(processorService);

        doNothing().when(processorServiceSpy).run();
        doNothing().when(processorServiceSpy).configure(anyString(), any(), any(), any(), any(), any(), any(),any());
        return processorServiceSpy;
    }
}
