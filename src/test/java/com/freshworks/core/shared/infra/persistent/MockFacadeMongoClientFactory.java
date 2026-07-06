package com.freshworks.core.shared.infra.persistent;

import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.freshworks.core.MockFacadeInterface;

@Component
public class MockFacadeMongoClientFactory implements MockFacadeInterface {

    @Autowired
    ApplicationContext applicationContext;

    MongoClientFactory mongoClientFactory;
//
//    @Autowired
//    MockFacadeMongoClient mockFacadeMongoClient;
//
//    ReturnableMockTypeList<MongoClient> getMongoClientObject;
//    ReturnableMockTypeList<Boolean> doesClientExists;


    @Override
    public MockFacadeMongoClientFactory configure() throws Exception {
        reset();
//        getMongoClientObject.add(mockFacadeMongoClient.configure().build());
//        doesClientExists.add(true);
        return this;
    }


//    public MockFacadeMongoClientFactory getMongoClientObject(MongoClient... mongoClients) {
//        this.getMongoClientObject.clear();
//        this.getMongoClientObject.add(mongoClients);
//        return this;
//    }
//
//    public MockFacadeMongoClientFactory doesClientExists(Boolean... doesClientExists) {
//        this.doesClientExists.clear();
//        this.doesClientExists.add(doesClientExists);
//        return this;
//    }


    @Override
    public MongoClientFactory build() throws Exception {

        mongoClientFactory = applicationContext.getBean(MongoClientFactory.class);
        MongoClientFactory mongoClientFactorySpy = Mockito.spy(mongoClientFactory);
//        doAnswer(getMongoClientObject.answer()).when(mongoClientFactorySpy).getMongoClientObject(any());
//        doAnswer(doesClientExists.answer()).when(mongoClientFactorySpy).doesClientExists();

        return mongoClientFactorySpy;
    }
}
