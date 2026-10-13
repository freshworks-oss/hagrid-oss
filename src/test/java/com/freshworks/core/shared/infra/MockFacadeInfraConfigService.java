package com.freshworks.core.shared.infra;

import com.freshworks.core.MockFacadeInterface;
import com.freshworks.core.ReturnableMockTypeList;
import com.freshworks.core.shared.MockFacadeSyncServiceContainer;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.sync.ConnectorConfiguration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mockito;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;


@Component
public class MockFacadeInfraConfigService implements MockFacadeInterface {

    @Autowired
    ApplicationContext applicationContext;

    InfraConfigService infraConfigService;

    ReturnableMockTypeList<String> getInfraType = new ReturnableMockTypeList<>();
    ReturnableMockTypeList<String> getInfraDbLocation = new ReturnableMockTypeList<>();

    @Override
    public MockFacadeInfraConfigService configure(){

        reset();
        getInfraType.add("");
        getInfraDbLocation.add("");

        return this;
    }

    public MockFacadeInfraConfigService getInfraType(String... getInfraType) {
        this.getInfraType.clear();
        this.getInfraType.add(getInfraType);
        return this;
    }

    public MockFacadeInfraConfigService getInfraDbLocation(String... getInfraDbLocation) {
        this.getInfraDbLocation.clear();
        this.getInfraDbLocation.add(getInfraDbLocation);
        return this;
    }

    @Override
    public InfraConfigService build() throws Exception {

        infraConfigService = applicationContext.getBean(InfraConfigService.class);
        InfraConfigService infraConfigServiceSpy = Mockito.spy(infraConfigService);
        doAnswer(getInfraType.answer()).when(infraConfigServiceSpy).getInfraDbType();
        doAnswer(getInfraDbLocation.answer()).when(infraConfigServiceSpy).getInfraDbLocation();

        return infraConfigServiceSpy;
    }
}
