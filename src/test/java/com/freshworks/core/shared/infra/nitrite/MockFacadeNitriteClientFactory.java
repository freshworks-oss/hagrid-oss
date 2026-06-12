package com.freshworks.core.shared.infra.nitrite;

import com.freshworks.core.MockFacadeInterface;
import com.freshworks.core.shared.infra.nitrite.NitriteFactory;

import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.stereotype.Component;

@Component
public class MockFacadeNitriteClientFactory implements MockFacadeInterface {


    @SpyBean
    NitriteFactory nitriteClientFactory;

    @Override
    public MockFacadeNitriteClientFactory configure() throws Exception {
        reset();
        return this;
    }



    @Override
    public NitriteFactory build() throws Exception {

        return nitriteClientFactory;
    }
}
