package com.freshworks.hagrid.shared.infra.mongo;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import com.freshworks.hagrid.shared.SyncServiceContainer;
import com.freshworks.hagrid.shared.infra.InfraBeanService;
import com.freshworks.hagrid.shared.infra.InfraConfigService;
import com.freshworks.hagrid.shared.infra.InfraService;
import com.freshworks.hagrid.shared.infra.nitrite.NitriteService;
import com.freshworks.hagrid.shared.sync.ConnectorConfiguration;
import com.freshworks.hagrid.shared.sync.ConnectorConfiguration.MongoDbConfiguration;

/**
 * Mongo settings in ConnectorConfiguration / InfraConfigService and the
 * "mongo" vs everything-else switch in InfraBeanService.
 *
 * Pure unit tests: no Spring context, no Mongo, no Docker. The Nitrite tests
 * (TestNitriteDbService) stub InfraConfigService with MockFacadeInfraConfigService
 * (a Mockito spy returning a canned getInfraDbType()); here we do the same with a
 * plain Mockito mock for the bean switch, and use the real InfraConfigService
 * wrapped around a real ConnectorConfiguration for the getter tests.
 */
@EnabledIfSystemProperty(named = "spring.profiles.active", matches = "unit")
public class TestMongoInfraConfig {

    private ConnectorConfiguration connectorConfiguration;
    private InfraConfigService infraConfigService;

    @BeforeEach
    void setup() throws Exception {
        connectorConfiguration = new ConnectorConfiguration();
        SyncServiceContainer container = mock(SyncServiceContainer.class);
        when(container.getBean(ConnectorConfiguration.class)).thenReturn(connectorConfiguration);
        infraConfigService = new InfraConfigService();
        infraConfigService.configure(container);
    }

    // ================================================================ ConnectorConfiguration

    @Nested
    class ConnectorConfigurationDefaults {

        @Test
        public void testInfraTypeDefaultsToFileAndLocationToDatabaseDir() {
            ConnectorConfiguration c = new ConnectorConfiguration();
            assertThat(c.getInfraDbType(), is("file"));
            assertThat(c.getInfraDbLocation(), is("./database"));
        }

        @Test
        public void testMongoDbConfigurationIsNullByDefault() {
            // FINDING: ConnectorConfiguration.mongoDbConfiguration is not initialised, while
            // MongoDbConfiguration itself has defaults (admin/password12345/localhost:27017).
            // Anything calling the Mongo getters without first calling setMongoDbConfiguration gets an NPE.
            assertThat(new ConnectorConfiguration().getMongoDbConfiguration(), is(nullValue()));
        }

        @Test
        public void testMongoDbConfigurationDefaults() {
            // FINDING: default credentials admin / password12345 are baked into main code.
            MongoDbConfiguration m = new MongoDbConfiguration();
            assertThat(m.getMongoConnectionString(), is(nullValue()));
            assertThat(m.getDatabaseUserName(), is("admin"));
            assertThat(m.getDatabasePassword(), is("password12345"));
            assertThat(m.getDatabaseAuthDb(), is("admin"));
            assertThat(m.getDatabaseHost(), is("localhost"));
            assertThat(m.getDatabasePort(), is(27017));
            assertThat(m.getAdditionalParams(), is(nullValue()));
        }

        @Test
        public void testMongoDbConfigurationSettersRoundTrip() {
            MongoDbConfiguration m = new MongoDbConfiguration();
            m.setMongoConnectionString("mongodb://x");
            m.setDatabaseUserName("u");
            m.setDatabasePassword("p");
            m.setDatabaseAuthDb("a");
            m.setDatabaseHost("h");
            m.setDatabasePort(1);
            m.setAdditionalParams("x=y");

            assertThat(m.getMongoConnectionString(), is("mongodb://x"));
            assertThat(m.getDatabaseUserName(), is("u"));
            assertThat(m.getDatabasePassword(), is("p"));
            assertThat(m.getDatabaseAuthDb(), is("a"));
            assertThat(m.getDatabaseHost(), is("h"));
            assertThat(m.getDatabasePort(), is(1));
            assertThat(m.getAdditionalParams(), is("x=y"));
        }
    }

    // ================================================================ InfraConfigService

    @Nested
    class InfraConfigServiceGetters {

        @Test
        public void testInfraTypeAndLocationPassThrough() throws Exception {
            connectorConfiguration.setInfraDbType("mongo");
            connectorConfiguration.setInfraDbLocation("/tmp/x");

            assertThat(infraConfigService.getInfraDbType(), is("mongo"));
            assertThat(infraConfigService.getInfraDbLocation(), is("/tmp/x"));
        }

        @Test
        public void testAllMongoGettersPassThroughToMongoDbConfiguration() {
            MongoDbConfiguration m = new MongoDbConfiguration();
            m.setMongoConnectionString("mongodb://x");
            m.setDatabaseUserName("u");
            m.setDatabasePassword("p");
            m.setDatabaseAuthDb("a");
            m.setDatabaseHost("h");
            m.setDatabasePort(4242);
            m.setAdditionalParams("x=y");
            connectorConfiguration.setMongoDbConfiguration(m);

            assertThat(infraConfigService.getMongoConnectionString(), is("mongodb://x"));
            assertThat(infraConfigService.getDatabaseUserName(), is("u"));
            assertThat(infraConfigService.getDatabasePassword(), is("p"));
            assertThat(infraConfigService.getDatabaseAuthDb(), is("a"));
            assertThat(infraConfigService.getDatabaseHost(), is("h"));
            assertThat(infraConfigService.getDatabasePort(), is(4242));
            assertThat(infraConfigService.getAdditionalParams(), is("x=y"));
        }

        @Test
        public void testDefaultsFlowThroughWhenOnlyEmptyMongoConfigurationIsSet() {
            connectorConfiguration.setMongoDbConfiguration(new MongoDbConfiguration());

            assertThat(infraConfigService.getDatabaseHost(), is("localhost"));
            assertThat(infraConfigService.getDatabasePort(), is(27017));
            assertThat(infraConfigService.getDatabaseUserName(), is("admin"));
            assertThat(infraConfigService.getMongoConnectionString(), is(nullValue()));
        }

        @Test
        public void testMongoGettersThrowNpeWhenMongoConfigurationNeverSet() {
            // FINDING: see testMongoDbConfigurationIsNullByDefault. Asserting CURRENT behaviour.
            // MongoClientFactory calls getMongoConnectionString() first, so a misconfigured app
            // dies with a bare NPE instead of a "mongo settings missing" message.
            assertThrows(NullPointerException.class, () -> infraConfigService.getMongoConnectionString());
            assertThrows(NullPointerException.class, () -> infraConfigService.getDatabasePort());
        }

        @Test
        public void testGettersThrowNpeBeforeConfigureIsCalled() {
            // Same shape as the Nitrite getters: configure(SyncServiceContainer) must run first.
            InfraConfigService unconfigured = new InfraConfigService();
            assertThrows(NullPointerException.class, unconfigured::getInfraDbType);
            assertThrows(NullPointerException.class, unconfigured::getDatabaseHost);
        }

        @Test
        public void testConfigureAsksTheContainerForConnectorConfiguration() throws Exception {
            ConnectorConfiguration other = new ConnectorConfiguration();
            other.setInfraDbType("mongo");
            SyncServiceContainer container = mock(SyncServiceContainer.class);
            when(container.getBean(ConnectorConfiguration.class)).thenReturn(other);

            InfraConfigService service = new InfraConfigService();
            service.configure(container);

            assertThat(service.getInfraDbType(), is("mongo"));
        }

        @Test
        public void testConfigurePrintsDbTypeToStdout() throws Exception {
            // FINDING: InfraConfigService.configure has a leftover System.out.println("db type is ...").
            // It bypasses the logger/analytics and fires for every InfraConfigService (prototype bean).
            connectorConfiguration.setInfraDbType("mongo");
            SyncServiceContainer container = mock(SyncServiceContainer.class);
            when(container.getBean(ConnectorConfiguration.class)).thenReturn(connectorConfiguration);

            PrintStream original = System.out;
            ByteArrayOutputStream captured = new ByteArrayOutputStream();
            System.setOut(new PrintStream(captured));
            try {
                new InfraConfigService().configure(container);
            } finally {
                System.setOut(original);
            }

            assertThat(captured.toString(), containsString("db type is mongo"));
        }
    }

    // ================================================================ InfraBeanService switch

    @Nested
    class InfraBeanServiceSwitch {

        private InfraBeanService beanService;

        @BeforeEach
        void create() {
            beanService = new InfraBeanService();
        }

        /** Same trick as Nitrite tests: a mock InfraConfigService that only answers getInfraDbType(). */
        private InfraService serviceFor(String type) throws Exception {
            InfraConfigService stub = mock(InfraConfigService.class);
            when(stub.getInfraDbType()).thenReturn(type);
            return beanService.getInfraService(stub);
        }

        @Test
        public void testMongoGivesMongoService() throws Exception {
            assertThat(serviceFor("mongo"), is(instanceOf(MongoService.class)));
        }

        @Test
        public void testMongoIsCaseInsensitive() throws Exception {
            assertThat(serviceFor("MONGO"), is(instanceOf(MongoService.class)));
            assertThat(serviceFor("Mongo"), is(instanceOf(MongoService.class)));
        }

        @Test
        public void testFileGivesNitriteService() throws Exception {
            assertThat(serviceFor("file"), is(instanceOf(NitriteService.class)));
        }

        @Test
        public void testInMemoryGivesNitriteService() throws Exception {
            assertThat(serviceFor("inmemory"), is(instanceOf(NitriteService.class)));
        }

        @Test
        public void testEachCallGivesANewServiceInstance() throws Exception {
            InfraConfigService stub = mock(InfraConfigService.class);
            when(stub.getInfraDbType()).thenReturn("mongo");

            assertThat(beanService.getInfraService(stub) != beanService.getInfraService(stub), is(true));
        }

        @Test
        public void testTypoMongodbSilentlyFallsBackToNitrite() throws Exception {
            // FINDING: "mongodb" is a very natural value, but only "mongo" matches. Anything else, typos
            // included, silently selects NitriteService (and NitriteFactory treats every non-"file"
            // type as IN-MEMORY), so the data would vanish on restart with no error or warning.
            assertThat(serviceFor("mongodb"), is(instanceOf(NitriteService.class)));
        }

        @Test
        public void testEmptyTypeFallsBackToNitrite() throws Exception {
            assertThat(serviceFor(""), is(instanceOf(NitriteService.class)));
        }

        @Test
        public void testBlankAndPaddedTypeFallBackToNitrite() throws Exception {
            // FINDING: no trim(): " mongo" (e.g. from a yaml/env value with a stray space) is not "mongo".
            assertThat(serviceFor("   "), is(instanceOf(NitriteService.class)));
            assertThat(serviceFor(" mongo"), is(instanceOf(NitriteService.class)));
            assertThat(serviceFor("mongo "), is(instanceOf(NitriteService.class)));
        }

        @Test
        public void testNullTypeThrowsNpe() {
            // FINDING: null type -> NullPointerException from getInfraDbType().equalsIgnoreCase(..).
            // Real configs rarely hit this (ConnectorConfiguration defaults to "file"), but a
            // programmatic setInfraDbType(null) would. Asserting CURRENT behaviour.
            assertThrows(NullPointerException.class, () -> serviceFor(null));
        }

        @Test
        public void testRealConfigurationEndToEnd() throws Exception {
            // real InfraConfigService + real ConnectorConfiguration, no mocks of the config
            assertThat(beanService.getInfraService(infraConfigService), is(instanceOf(NitriteService.class)));

            connectorConfiguration.setInfraDbType("mongo");
            assertThat(beanService.getInfraService(infraConfigService), is(instanceOf(MongoService.class)));
        }
    }
}
