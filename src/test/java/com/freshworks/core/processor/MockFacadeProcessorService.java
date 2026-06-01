package com.freshworks.core.processor;

import com.freshworks.core.MockFacadeInterface;
import com.freshworks.core.ReturnableMockTypeList;
import com.freshworks.core.shared.analytics.AnalyticsService;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.stereotype.Component;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;

@Component
public class MockFacadeProcessorService implements MockFacadeInterface {


    ReturnableMockTypeList<AnalyticsService> analyticsService;
    @SpyBean
    ProcessorService processorServiceSpy;

    @Override
    public MockFacadeProcessorService configure(){
        reset();
        return this;
    }


    @Override
    public ProcessorService build() throws Exception {

        doNothing().when(processorServiceSpy).run();
        doNothing().when(processorServiceSpy).configure(anyString(), any(), any(), any(), any(), any(), any(),any());
        return processorServiceSpy;
    }
}
