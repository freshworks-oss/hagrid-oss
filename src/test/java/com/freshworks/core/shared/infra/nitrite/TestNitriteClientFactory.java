package com.freshworks.core.shared.infra.nitrite;

import com.freshworks.core.shared.MockFacadeSyncServiceContainer;
import com.freshworks.core.shared.Namespace;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.analytics.AnalyticsFactory;
import com.freshworks.core.shared.infra.InfraConfigService;
import com.freshworks.core.shared.infra.MockFacadeInfraConfigService;
import com.freshworks.core.shared.infra.persistent.MongoClientFactory;
import com.mongodb.client.MongoClient;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.hamcrest.MatcherAssert.assertThat;

import org.dizitart.no2.Nitrite;

@SpringBootTest
@EnabledIfSystemProperty(named = "spring.profiles.active", matches = ".*\\.unit\\.nitrite")
public class TestNitriteClientFactory {


    @Autowired
    ApplicationContext applicationContext;

    @Autowired
    MockFacadeInfraConfigService mockFacadeInfraConfigService;

    @Autowired
    MockFacadeSyncServiceContainer mockFacadeSyncServiceContainer;

    @Autowired
    NitriteFactory nitriteClientFactory;


    @BeforeEach
    public void setup() throws Exception {

        mockFacadeInfraConfigService.configure().build();
        mockFacadeSyncServiceContainer.configure().build();
    }


    @Test
    public void testSingleNitriteClientIsCreatedForMultipleNitriteService() throws Exception {

        InfraConfigService infraConfigService = mockFacadeInfraConfigService.configure()
                .getDatabaseHost("")
                .getDatabasePort(0)
                . getDatabaseAuthDb("admin")
                .getDatabaseUserName("admin")
                .getDatabasePassword("admin")
                .getInfraType("nitrite")
                .build();

        Namespace namespace = applicationContext.getBean(Namespace.class);
        namespace.setNamespace("dummy_namespace");

        Nitrite mongoClient1  = nitriteClientFactory.getNitriteClient("dummy_namespace",infraConfigService);
        Nitrite mongoClient2  = nitriteClientFactory.getNitriteClient("dummy_namespace",infraConfigService);

        assertThat(mongoClient1, Matchers.equalToObject(mongoClient2));
//
    }
}
