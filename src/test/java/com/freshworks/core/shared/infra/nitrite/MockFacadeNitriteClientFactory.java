package com.freshworks.core.shared.infra.nitrite;

import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.freshworks.core.MockFacadeInterface;

@Component
public class MockFacadeNitriteClientFactory implements MockFacadeInterface {

    @Autowired
    ApplicationContext applicationContext;

    NitriteFactory nitriteClientFactory;

    @Override
    public MockFacadeNitriteClientFactory configure() throws Exception {
        reset();
        return this;
    }



    @Override
    public NitriteFactory build() throws Exception {
        nitriteClientFactory = applicationContext.getBean(NitriteFactory.class);
        NitriteFactory nitriteClientFactorySpy = Mockito.spy(nitriteClientFactory);
        return nitriteClientFactorySpy;
    }
}
