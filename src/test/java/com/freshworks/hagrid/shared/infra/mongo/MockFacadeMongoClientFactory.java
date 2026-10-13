package com.freshworks.hagrid.shared.infra.mongo;

import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.freshworks.hagrid.MockFacadeInterface;

/** Mongo twin of MockFacadeNitriteClientFactory: a spy over the MongoClientFactory Spring bean. */
@Component
public class MockFacadeMongoClientFactory implements MockFacadeInterface {

    @Autowired
    ApplicationContext applicationContext;

    MongoClientFactory mongoClientFactory;

    @Override
    public MockFacadeMongoClientFactory configure() throws Exception {
        reset();
        return this;
    }

    @Override
    public MongoClientFactory build() throws Exception {
        mongoClientFactory = applicationContext.getBean(MongoClientFactory.class);
        MongoClientFactory mongoClientFactorySpy = Mockito.spy(mongoClientFactory);
        return mongoClientFactorySpy;
    }
}
