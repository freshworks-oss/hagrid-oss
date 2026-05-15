package com.freshworks.core.shared.infra.h2;

import com.freshworks.core.MockFacadeInterface;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.stereotype.Component;

@Component
public class MockFacadeH2ClientFactory implements MockFacadeInterface {


    @SpyBean
    H2Factory h2ClientFactory;

    @Override
    public MockFacadeH2ClientFactory configure() throws Exception {
        reset();
        return this;
    }



    @Override
    public H2Factory build() throws Exception {

        return h2ClientFactory;
    }
}
