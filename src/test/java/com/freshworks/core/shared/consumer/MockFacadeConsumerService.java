package com.freshworks.core.shared.consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;

import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.freshworks.core.MockFacadeInterface;
import com.freshworks.core.ReturnableMockTypeList;
import com.freshworks.core.shared.infra.InfraDbCursor;

@Component
public class MockFacadeConsumerService implements MockFacadeInterface {

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    ConsumerService consumerService;

    ReturnableMockTypeList<InfraDbCursor> getAssetCursor = new ReturnableMockTypeList<>();

    // ReturnableMockTypeList<List<AbstractAsset>> getAssetListForGivenCursor;

    // ReturnableMockTypeList<Boolean> hasNextForAGivenCusor;


    @Override
    public MockFacadeConsumerService configure() throws Exception{

        reset();
        return this;
    }

    public MockFacadeConsumerService getAssetCursor(InfraDbCursor... getAssetCursor) {
        this.getAssetCursor.clear();
        this.getAssetCursor.add(getAssetCursor);
        return this;
    }


    @Override
    public ConsumerService build() throws Exception {
        consumerService = applicationContext.getBean(ConsumerService.class);
        ConsumerService consumerServiceSpy = Mockito.spy(consumerService);
        doNothing().when(consumerServiceSpy).configure(any());
        doAnswer(getAssetCursor.answer()).when(consumerServiceSpy).getAssetCursor(any(), any());
        doAnswer(getAssetCursor.answer()).when(consumerServiceSpy).getAssetCursor(any());

        return consumerServiceSpy;
    }
}
