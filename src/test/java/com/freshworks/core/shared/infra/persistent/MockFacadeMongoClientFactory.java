package com.freshworks.core.shared.infra.persistent;

import com.freshworks.core.MockFacadeInterface;
import com.freshworks.core.ReturnableMockTypeList;
import com.freshworks.core.shared.infra.MockFacadeInfraConfigService;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.stereotype.Component;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

@Component
public class MockFacadeMongoClientFactory implements MockFacadeInterface {


    @SpyBean
    MongoClientFactory mongoClientFactorySpy;
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

//        doAnswer(getMongoClientObject.answer()).when(mongoClientFactorySpy).getMongoClientObject(any());
//        doAnswer(doesClientExists.answer()).when(mongoClientFactorySpy).doesClientExists();

        return mongoClientFactorySpy;
    }
}
