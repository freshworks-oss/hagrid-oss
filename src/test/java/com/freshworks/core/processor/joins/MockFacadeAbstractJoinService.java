package com.freshworks.core.processor.joins;

import com.freshworks.core.MockFacadeInterface;
import com.freshworks.core.ReturnableMockTypeList;
import com.freshworks.core.processor.AbstractAsset;
import com.freshworks.core.processor.AbstractBean;
import com.google.common.base.Optional;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;

@Component
public class MockFacadeAbstractJoinService implements MockFacadeInterface {

    @SpyBean
    AbstractJoinService abstractJoinServiceSpy;

    ReturnableMockTypeList<List<HashMap<String, AbstractBean>>> lookupStagingArea;


    @Override
    public MockFacadeAbstractJoinService configure(){
        reset();
        lookupStagingArea.addNull();
        return this;
    }


    public MockFacadeAbstractJoinService lookupStagingArea(List<HashMap<String, AbstractBean>> lookupStagingArea){
        this.lookupStagingArea.clear();
        this.lookupStagingArea.add(lookupStagingArea);
        return this;
    }


    @Override
    public AbstractJoinService build() throws Exception {

        doNothing().when(abstractJoinServiceSpy).configure(any());
        doAnswer(lookupStagingArea.answer()).when(abstractJoinServiceSpy).lookupStagingArea(any(), any(), any());

        return abstractJoinServiceSpy;
    }
}
