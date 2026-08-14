package com.freshworks.core.shared.infra.nitrite;

import static org.hamcrest.MatcherAssert.assertThat;

import org.dizitart.no2.Nitrite;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import com.freshworks.core.shared.MockFacadeSyncServiceContainer;
import com.freshworks.core.shared.NamespaceService;
import com.freshworks.core.shared.infra.InfraConfigService;
import com.freshworks.core.shared.infra.MockFacadeInfraConfigService;

@SpringBootTest
@EnabledIfSystemProperty(named = "spring.profiles.active", matches = "unit")
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

        NamespaceService namespace = applicationContext.getBean(NamespaceService.class);
        namespace.setNamespace("dummy_namespace");

        Nitrite mongoClient1  = nitriteClientFactory.getNitriteClient("dummy_namespace",infraConfigService);
        Nitrite mongoClient2  = nitriteClientFactory.getNitriteClient("dummy_namespace",infraConfigService);

        assertThat(mongoClient1, Matchers.equalToObject(mongoClient2));
//
    }
}
