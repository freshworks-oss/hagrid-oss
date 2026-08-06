package com.freshworks.core.shared.consumer;

import com.freshworks.core.MockFacadeInterface;
import com.freshworks.core.ReturnableMockTypeList;
import com.freshworks.core.data.five_zero_zero.unit.fb.assets.FbComment;
import com.freshworks.core.processor.AbstractAsset;
import com.freshworks.core.shared.infra.InfraDbCursor;
import com.freshworks.core.shared.infra.nitrite.MockFacadeNitriteDbCursor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

import org.dizitart.no2.filters.NitriteFilter;
import org.mockito.Mockito;

@Component
public class MockFacadeConsumerService implements MockFacadeInterface {

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    MockFacadeNitriteDbCursor mockFacadeNitriteDbCursor;

    @Autowired
    ConsumerService consumerService;

    ReturnableMockTypeList<InfraDbCursor> getAssetCursor;

    // ReturnableMockTypeList<List<AbstractAsset>> getAssetListForGivenCursor;

    // ReturnableMockTypeList<Boolean> hasNextForAGivenCusor;


    @Override
    public MockFacadeConsumerService configure() throws Exception{

        reset();
        getAssetCursor.add(mockFacadeNitriteDbCursor.configure().build());

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
        doAnswer(getAssetCursor.answer()).when(consumerServiceSpy).getAssetCursor(any());
        doAnswer(getAssetCursor.answer()).when(consumerServiceSpy).getAssetCursor();

        return consumerServiceSpy;
    }
}
