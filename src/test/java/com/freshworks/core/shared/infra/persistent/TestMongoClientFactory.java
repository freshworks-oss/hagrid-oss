package com.freshworks.core.shared.infra.persistent;

import com.freshworks.core.shared.MockFacadeSyncServiceContainer;
import com.freshworks.core.shared.NamespaceService;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.analytics.AnalyticsFactory;
import com.freshworks.core.shared.infra.InfraConfigService;
import com.freshworks.core.shared.infra.MockFacadeInfraConfigService;
import com.mongodb.client.MongoClient;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;

@SpringBootTest
@EnabledIfSystemProperty(named = "spring.profiles.active", matches = ".*\\.unit\\.persistent")
public class TestMongoClientFactory {


    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    MockFacadeInfraConfigService mockFacadeInfraConfigService;

    @Autowired
    MockFacadeSyncServiceContainer mockFacadeSyncServiceContainer;

    @Autowired
    MongoClientFactory mongoClientFactory;


    @BeforeEach
    public void setup() throws Exception {

        mockFacadeInfraConfigService.configure().build();
        mockFacadeSyncServiceContainer.configure().build();
    }


    @Test
    public void testSingleMongoClientIsCreatedForMultipleMongoService() throws Exception {

        InfraConfigService infraConfigService = mockFacadeInfraConfigService.configure()
                .getDatabaseHost("mongodb")
                .getDatabasePort(27017)
                . getDatabaseAuthDb("admin")
                .getDatabaseUserName("admin")
                .getDatabasePassword("password12345")
                .getInfraType("")
                .build();

        AnalyticsFactory analyticsFactory = applicationContext.getBean(AnalyticsFactory.class);
        NamespaceService namespace = applicationContext.getBean(NamespaceService.class);
        namespace.setNamespace("dummy_namespace");

        SyncServiceContainer syncServiceContainer = mockFacadeSyncServiceContainer
                .add(namespace, NamespaceService.class)
                        .add(analyticsFactory, AnalyticsFactory.class)
                                .build();

        MongoClient mongoClient1  = mongoClientFactory.getMongoClientObject(syncServiceContainer,infraConfigService);
        MongoClient mongoClient2  = mongoClientFactory.getMongoClientObject(syncServiceContainer,infraConfigService);

        assertThat(mongoClient1, Matchers.equalToObject(mongoClient2));
//
    }
}
