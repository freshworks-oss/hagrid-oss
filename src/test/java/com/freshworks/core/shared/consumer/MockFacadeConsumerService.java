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
import org.mockito.Mockito;

@Component
public class MockFacadeConsumerService implements MockFacadeInterface {

    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    MockFacadeNitriteDbCursor mockFacadeNitriteDbCursor;

    @Autowired
    ConsumerService consumerService;

    ReturnableMockTypeList<InfraDbCursor> initAssetCursor;

    ReturnableMockTypeList<List<AbstractAsset>> getAssetListForGivenCursor;

    ReturnableMockTypeList<Boolean> hasNextForAGivenCusor;


    @Override
    public MockFacadeConsumerService configure() throws Exception{

        reset();
        initAssetCursor.add(mockFacadeNitriteDbCursor.configure().build());

        List<AbstractAsset> assetList = new ArrayList<>();
        FbComment fbComment = new FbComment();
        fbComment.setComment_id("1");
        fbComment.setComment_text("This is comment text");
        assetList.add(fbComment);
        getAssetListForGivenCursor.add(assetList);

        hasNextForAGivenCusor.add(false);

        return this;
    }

    public MockFacadeConsumerService initAssetCursor(InfraDbCursor... initAssetCursor) {
        this.initAssetCursor.clear();
        this.initAssetCursor.add(initAssetCursor);
        return this;
    }

    public MockFacadeConsumerService getAssetListForGivenCursor(List<AbstractAsset>... getAssetListForGivenCursor) {
        this.getAssetListForGivenCursor.clear();
        this.getAssetListForGivenCursor.add(getAssetListForGivenCursor);
        return this;
    }

    public MockFacadeConsumerService hasNextForAGivenCusor(Boolean... hasNextForAGivenCusor) {
        this.hasNextForAGivenCusor.clear();
        this.hasNextForAGivenCusor.add(hasNextForAGivenCusor);
        return this;
    }


    @Override
    public ConsumerService build() throws Exception {
        consumerService = applicationContext.getBean(ConsumerService.class);
        ConsumerService consumerServiceSpy = Mockito.spy(consumerService);
        doNothing().when(consumerServiceSpy).configure(any());
        doAnswer(initAssetCursor.answer()).when(consumerServiceSpy).initAssetCursor(any());
        doAnswer(getAssetListForGivenCursor.answer()).when(consumerServiceSpy).getAssetListForGivenCursor(any(), any(), anyInt());
        doAnswer(hasNextForAGivenCusor.answer()).when(consumerServiceSpy).hasNextForAGivenCusor(any(), any());

        return consumerServiceSpy;
    }
}
