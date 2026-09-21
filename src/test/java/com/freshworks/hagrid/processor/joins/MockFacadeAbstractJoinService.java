package com.freshworks.hagrid.processor.joins;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;

import java.util.HashMap;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.mockito.Mockito;

import com.freshworks.hagrid.MockFacadeInterface;
import com.freshworks.hagrid.ReturnableMockTypeList;
import com.freshworks.hagrid.processor.AbstractBean;
import com.freshworks.hagrid.processor.joins.AbstractJoinService;

@Component
public class MockFacadeAbstractJoinService implements MockFacadeInterface {

    @Autowired
    ApplicationContext applicationContext;

    AbstractJoinService abstractJoinService;

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

        abstractJoinService = applicationContext.getBean(AbstractJoinService.class);
        AbstractJoinService abstractJoinServiceSpy = Mockito.spy(abstractJoinService);
        doNothing().when(abstractJoinServiceSpy).configure(any(), any());
        doAnswer(lookupStagingArea.answer()).when(abstractJoinServiceSpy).lookupStagingArea(any(), any(), any(), any());

        return abstractJoinServiceSpy;
    }
}
