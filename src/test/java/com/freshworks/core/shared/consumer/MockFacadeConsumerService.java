package com.freshworks.core.shared.consumer;

import com.freshworks.core.MockFacadeInterface;
import com.freshworks.core.ReturnableMockTypeList;
import com.freshworks.core.data.five_zero_zero.unit.fb.assets.FbComment;
import com.freshworks.core.processor.AbstractAsset;
import com.freshworks.core.shared.infra.InfraDbCursorResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.mockito.Mockito;

@Component
public class MockFacadeConsumerService implements MockFacadeInterface {

    @Autowired
    ApplicationContext applicationContext;

    ConsumerService consumerService;

    ReturnableMockTypeList<List<AbstractAsset>> getAssetByAssetType;

    ReturnableMockTypeList<InfraDbCursorResponse<AbstractAsset>> streamAssetByAssetType;

    ReturnableMockTypeList<List<AbstractAsset>> getAssetByAssetTypeAndFilter;



    @Override
    public MockFacadeConsumerService configure(){
        reset();
        List<AbstractAsset> assetList = new ArrayList<>();
        FbComment fbComment = new FbComment();
        fbComment.setComment_id("1");
        fbComment.setComment_text("This is comment text");
        assetList.add(fbComment);
        getAssetByAssetType.add(assetList);
        getAssetByAssetTypeAndFilter.add(assetList);

        InfraDbCursorResponse<AbstractAsset> assetStreamResponse = new InfraDbCursorResponse<>();
        InfraDbCursorResponse.Token token = new InfraDbCursorResponse.Token();
        token.setStart(0);
        token.setCount(1);
        assetStreamResponse.setAbstractAssetList(assetList);
        assetStreamResponse.setNextToken(token);
        streamAssetByAssetType.add(assetStreamResponse);

        return this;
    }

    public MockFacadeConsumerService getAssetByAssetType(List<AbstractAsset>... assetList) {
        this.getAssetByAssetType.clear();
        this.getAssetByAssetType.add(assetList);
        return this;
    }

    public MockFacadeConsumerService streamAssetByAssetType(InfraDbCursorResponse<AbstractAsset>... assetStreamResponse) {
        this.streamAssetByAssetType.clear();
        this.streamAssetByAssetType.add(assetStreamResponse);
        return this;
    }

    public MockFacadeConsumerService getAssetByAssetTypeAndFilter(List<AbstractAsset>... assetList) {
        this.getAssetByAssetTypeAndFilter.clear();
        this.getAssetByAssetTypeAndFilter.add(assetList);
        return this;
    }

    @Override
    public ConsumerService build() throws Exception {
        consumerService = applicationContext.getBean(ConsumerService.class);
        ConsumerService consumerServiceSpy = Mockito.spy(consumerService);
        doNothing().when(consumerServiceSpy).configure(any());
        // doAnswer(getAssetByAssetType.answer()).when(consumerServiceSpy).getAssetByAssetType(any());
        // doAnswer(streamAssetByAssetType.answer()).when(consumerServiceSpy).streamAssetByAssetType(any(), any());
        // doAnswer(getAssetByAssetTypeAndFilter.answer()).when(consumerServiceSpy).getAssetByAssetTypeAndFilter(any(), any());

        return consumerServiceSpy;
    }
}
