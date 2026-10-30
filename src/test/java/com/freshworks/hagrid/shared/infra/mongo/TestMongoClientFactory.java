package com.freshworks.hagrid.shared.infra.mongo;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import com.freshworks.hagrid.shared.NamespaceService;
import com.freshworks.hagrid.shared.SyncServiceContainer;
import com.freshworks.hagrid.shared.analytics.AnalyticsFactory;
import com.freshworks.hagrid.shared.analytics.AnalyticsService;
import com.freshworks.hagrid.shared.infra.InfraConfigService;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.client.MongoClient;
import com.mongodb.client.internal.MongoClientImpl;

/**
 * Tests for MongoClientFactory (the Mongo twin of NitriteFactory, see TestNitriteClientFactory).
 *
 * No Spring and no Docker for the config tests: InfraConfigService and SyncServiceContainer
 * are Mockito mocks, and every test builds its OWN `new MongoClientFactory()` so the
 * (Spring singleton) factory bean is never touched and no client leaks into other test classes.
 * Only the "RealServer" nested class needs Mongo (MongoTestSupport).
 */
@EnabledIfSystemProperty(named = "spring.profiles.active", matches = "unit")
public class TestMongoClientFactory {

    // never resolvable (RFC 6761 .invalid), so a "-srv" lookup fails fast and offline
    private static final String UNRESOLVABLE_SRV_HOST = "cluster-srv.invalid";

    private MongoClientFactory factory;
    private SyncServiceContainer syncServiceContainer;
    private AnalyticsService analyticsService;
    private final List<MongoClient> toClose = new ArrayList<>();

    @BeforeEach
    void setup() {
        factory = new MongoClientFactory();

        // The factory asks the container for AnalyticsFactory + NamespaceService for its pool listener
        analyticsService = mock(AnalyticsService.class);
        AnalyticsFactory analyticsFactory = mock(AnalyticsFactory.class);
        when(analyticsFactory.getAnalyticsService(any())).thenReturn(analyticsService);
        NamespaceService namespaceService = mock(NamespaceService.class);
        when(namespaceService.getNamespace()).thenReturn("test_ns");
        syncServiceContainer = mock(SyncServiceContainer.class);
        when(syncServiceContainer.getBean(AnalyticsFactory.class)).thenReturn(analyticsFactory);
        when(syncServiceContainer.getBean(NamespaceService.class)).thenReturn(namespaceService);
    }

    @AfterEach
    void closeClients() {
        toClose.forEach(MongoClient::close);
        if (factory.getMongoClient() != null) {
            factory.getMongoClient().close();
        }
    }

    // ---- helpers

    private InfraConfigService config(String connectionString, String user, String password,
            String authDb, String host, int port, String additionalParams) {
        InfraConfigService c = mock(InfraConfigService.class);
        when(c.getMongoConnectionString()).thenReturn(connectionString);
        when(c.getDatabaseUserName()).thenReturn(user);
        when(c.getDatabasePassword()).thenReturn(password);
        when(c.getDatabaseAuthDb()).thenReturn(authDb);
        when(c.getDatabaseHost()).thenReturn(host);
        when(c.getDatabasePort()).thenReturn(port);
        when(c.getAdditionalParams()).thenReturn(additionalParams);
        return c;
    }

    private InfraConfigService legacyConfig(String user, String password, String authDb, String host, int port,
            String additionalParams) {
        return config(null, user, password, authDb, host, port, additionalParams);
    }

    private MongoClientSettings settingsOf(MongoClient client) {
        return ((MongoClientImpl) client).getSettings();
    }

    private MongoClient create(InfraConfigService c) throws IOException {
        return factory.getMongoClientObject(syncServiceContainer, c);
    }

    // ================================================================ connection-string path

    @Nested
    class ConnectionStringPath {

        @Test
        public void testConnectionStringIsUsedForHosts() throws Exception {
            // pretend Mongo is at a fake host: clients connect lazily so nothing is contacted
            MongoClient client = create(config("mongodb://u:p@fake-host-1.invalid:27555/?authSource=admin",
                    null, null, null, null, 0, null));

            assertThat(settingsOf(client).getClusterSettings().getHosts().get(0).toString(),
                    is("fake-host-1.invalid:27555"));
        }

        @Test
        public void testCredentialEmbeddedInConnectionStringIsKeptAndInfraCredentialsAreIgnored() throws Exception {
            MongoClient client = create(config("mongodb://uri_user:uri_pw@fake-host.invalid:27555/?authSource=uri_db",
                    "infra_user", "infra_pw", "infra_db", null, 0, null));

            MongoCredential credential = settingsOf(client).getCredential();
            assertThat(credential.getUserName(), is("uri_user"));
            assertThat(credential.getSource(), is("uri_db"));
        }

        @Test
        public void testInfraCredentialsFillTheGapWhenConnectionStringHasNoUser() throws Exception {
            MongoClient client = create(config("mongodb://fake-host.invalid:27555",
                    "infra_user", "infra_pw", "infra_db", null, 0, null));

            MongoCredential credential = settingsOf(client).getCredential();
            assertThat(credential.getUserName(), is("infra_user"));
            assertThat(credential.getSource(), is("infra_db"));
            assertThat(new String(credential.getPassword()), is("infra_pw"));
        }

        @Test
        public void testNoCredentialWhenConnectionStringHasNoUserAndInfraUserIsBlank() throws Exception {
            MongoClient client = create(config("mongodb://fake-host.invalid:27555", "", "pw", "db", null, 0, null));

            assertThat(settingsOf(client).getCredential(), is(nullValue()));
        }

        @Test
        public void testConnectionStringWinsOverLegacyHostSettings() throws Exception {
            // both set: the legacy host fields must be ignored (short-circuit)
            MongoClient client = create(config("mongodb://fake-host-1.invalid:27555", "u", "p", "admin",
                    "other-host.invalid", 1111, null));

            assertThat(settingsOf(client).getClusterSettings().getHosts().get(0).toString(),
                    is("fake-host-1.invalid:27555"));
        }

        @Test
        public void testMalformedConnectionStringFailsWithIllegalArgumentException() {
            assertThrows(IllegalArgumentException.class,
                    () -> create(config("this is not a uri", null, null, null, null, 0, null)));
        }

        @Test
        public void testBlankConnectionStringFallsBackToLegacyPath() throws Exception {
            // "   " counts as not configured (StringUtils.hasText), so the legacy fields are used
            MongoClient client = create(config("   ", "u", "p", "admin", "legacy-host.invalid", 27555, null));

            assertThat(settingsOf(client).getClusterSettings().getHosts().get(0).toString(),
                    is("legacy-host.invalid:27555"));
        }

        @Test
        public void testInfraUserWithBlankAuthDbOnUriWithoutUser() {
            // FINDING: connection string has no user, infra user+password are set but authDb is
            // null -> MongoCredential.createCredential rejects the null source with
            // IllegalArgumentException. The legacy path checks authDb up front and gives a clear
            // IllegalStateException, this path does not. Asserting CURRENT behaviour.
            assertThrows(IllegalArgumentException.class,
                    () -> create(config("mongodb://fake-host.invalid:27555", "u", "p", null, null, 0, null)));
        }
    }

    // ================================================================ host / port / credentials path

    @Nested
    class LegacyHostPortPath {

        @Test
        public void testHostPortAndCredentialAreUsed() throws Exception {
            MongoClient client = create(legacyConfig("alice", "secret", "authdb", "db-host.invalid", 27999, null));

            MongoClientSettings s = settingsOf(client);
            assertThat(s.getClusterSettings().getHosts().get(0).toString(), is("db-host.invalid:27999"));
            assertThat(s.getCredential().getUserName(), is("alice"));
            assertThat(s.getCredential().getSource(), is("authdb"));
            assertThat(new String(s.getCredential().getPassword()), is("secret"));
        }

        @Test
        public void testAdditionalParamsAreAppliedToTheUri() throws Exception {
            MongoClient client = create(legacyConfig("alice", "secret", "authdb", "db-host.invalid", 27999,
                    "appName=hagridTest&retryWrites=false"));

            MongoClientSettings s = settingsOf(client);
            assertThat(s.getApplicationName(), is("hagridTest"));
            assertThat(s.getRetryWrites(), is(false));
        }

        @Test
        public void testConnectionPoolSettingsAreForced() throws Exception {
            MongoClient client = create(legacyConfig("alice", "secret", "authdb", "db-host.invalid", 27999, null));

            var pool = settingsOf(client).getConnectionPoolSettings();
            assertThat(pool.getMaxSize(), is(100));
            assertThat(pool.getMinSize(), is(10));
            assertThat(pool.getMaxWaitTime(TimeUnit.MILLISECONDS), is(10000L));
            assertThat(pool.getMaxConnectionIdleTime(TimeUnit.SECONDS), is(60L));
        }

        @Test
        public void testSrvHostBuildsSrvUriAndIgnoresThePort() throws IOException {
            // "-srv" in the host -> mongodb+srv:// -> the cluster is configured by SRV host name, not by a host list.
            // Drivers before 5.2 threw on this 2-part host at client creation; newer ones create the client and
            // resolve DNS in the background, so assert on the cluster settings the srv branch produced.
            MongoClient client = create(legacyConfig("alice", "secret", "authdb", UNRESOLVABLE_SRV_HOST, 12345, null));

            assertThat(settingsOf(client).getClusterSettings().getSrvHost(), is(UNRESOLVABLE_SRV_HOST));
            assertThat(factory.doesClientExists(), is(true));
        }

        @Test
        public void testHostWithoutSrvDoesNotUseSrv() throws Exception {
            // a host that does not contain "-srv" must not trigger a DNS SRV lookup (would throw offline)
            MongoClient client = create(legacyConfig("alice", "secret", "authdb", "plain-host.invalid", 27017, null));

            assertThat(client, is(notNullValue()));
        }

        @Test
        public void testSrvDetectionIsCaseSensitive() throws Exception {
            // FINDING: "-SRV" (upper case) is not detected, so a mongodb+srv host written in
            // capitals is treated as a plain host:port and silently gets ":<port>" appended.
            MongoClient client = create(legacyConfig("alice", "secret", "authdb", "CLUSTER-SRV.invalid", 27017, null));

            assertThat(settingsOf(client).getClusterSettings().getHosts().get(0).toString(),
                    is("cluster-srv.invalid:27017"));
        }
    }

    // ================================================================ bad / missing config

    @Nested
    class MissingConfig {

        private void assertMissing(InfraConfigService c) {
            IllegalStateException e = assertThrows(IllegalStateException.class, () -> create(c));
            assertThat(e.getMessage(), containsString("Missing required MongoDB configuration"));
            assertThat(factory.doesClientExists(), is(false));
        }

        @Test
        public void testMissingUserNameFails() {
            assertMissing(legacyConfig(null, "p", "admin", "h", 27017, null));
        }

        @Test
        public void testMissingPasswordFails() {
            assertMissing(legacyConfig("u", null, "admin", "h", 27017, null));
        }

        @Test
        public void testMissingAuthDbFails() {
            assertMissing(legacyConfig("u", "p", null, "h", 27017, null));
        }

        @Test
        public void testMissingHostFails() {
            assertMissing(legacyConfig("u", "p", "admin", null, 27017, null));
        }

        @Test
        public void testBlankValuesCountAsMissing() {
            assertMissing(legacyConfig("  ", "p", "admin", "h", 27017, null));
            assertMissing(legacyConfig("u", "", "admin", "h", 27017, null));
        }

        @Test
        public void testNothingConfiguredFails() {
            assertMissing(config(null, null, null, null, null, 0, null));
        }

        @Test
        public void testFactoryRecoversAfterAFailedAttempt() throws Exception {
            // the finally block must release the CAS flag; otherwise the next caller would spin forever
            assertThrows(IllegalStateException.class,
                    () -> create(config(null, null, null, null, null, 0, null)));
            assertThat(factory.getUniqueClient().get(), is(false));

            MongoClient client = create(config("mongodb://fake-host.invalid:27555", null, null, null, null, 0, null));
            assertThat(client, is(notNullValue()));
        }

        @Test
        public void testNullInfraConfigServiceThrowsNullPointerException() {
            // FINDING: no null guard on the arguments (same as NitriteFactory); a clear
            // IllegalArgumentException would be friendlier than an NPE.
            assertThrows(NullPointerException.class, () -> factory.getMongoClientObject(syncServiceContainer, null));
        }
    }

    // ================================================================ singleton behaviour

    @Nested
    class Singleton {

        @Test
        public void testNoClientBeforeFirstCall() {
            assertThat(factory.doesClientExists(), is(false));
            assertThat(factory.getMongoClient(), is(nullValue()));
        }

        @Test
        public void testSameClientReturnedTwice() throws Exception {
            InfraConfigService c = config("mongodb://fake-host.invalid:27555", null, null, null, null, 0, null);

            MongoClient first = create(c);
            MongoClient second = create(c);

            assertThat(second, is(sameInstance(first)));
            assertThat(factory.doesClientExists(), is(true));
            assertThat(factory.getMongoClient(), is(sameInstance(first)));
        }

        @Test
        public void testSecondCallIgnoresItsConfigOnceAClientExists() throws Exception {
            // Existing client wins even if the new config points somewhere else (same as Nitrite's
            // "first one wins"), and the second config is not even read.
            MongoClient first = create(config("mongodb://fake-host.invalid:27555", null, null, null, null, 0, null));
            InfraConfigService other = config("mongodb://another-host.invalid:1", null, null, null, null, 0, null);

            MongoClient second = create(other);

            assertThat(second, is(sameInstance(first)));
            verify(other, org.mockito.Mockito.never()).getMongoConnectionString();
        }

        @Test
        public void testSeparateFactoriesGiveSeparateClients() throws Exception {
            InfraConfigService c = config("mongodb://fake-host.invalid:27555", null, null, null, null, 0, null);
            MongoClientFactory other = new MongoClientFactory();

            MongoClient mine = create(c);
            MongoClient theirs = other.getMongoClientObject(syncServiceContainer, c);
            toClose.add(theirs);

            assertThat(theirs, is(not(sameInstance(mine))));
        }

        @Test
        public void testConcurrentCallersGetOneClient() throws Exception {
            InfraConfigService c = config("mongodb://fake-host.invalid:27555", null, null, null, null, 0, null);
            int threads = 8;
            ExecutorService pool = Executors.newFixedThreadPool(threads);
            CyclicBarrier startTogether = new CyclicBarrier(threads);
            try {
                List<Future<MongoClient>> futures = new ArrayList<>();
                for (int i = 0; i < threads; i++) {
                    futures.add(pool.submit(() -> {
                        startTogether.await(5, TimeUnit.SECONDS);
                        return create(c);
                    }));
                }
                MongoClient expected = futures.get(0).get(15, TimeUnit.SECONDS);
                for (Future<MongoClient> f : futures) {
                    assertThat(f.get(15, TimeUnit.SECONDS), is(sameInstance(expected)));
                }
            } finally {
                pool.shutdownNow();
            }
        }

        @Test
        public void testClosedClientIsStillReturned() throws Exception {
            // ⚠ CONTRADICTS NITRITE: NitriteFactory.doesFileBasedClientExists checks !isClosed() and
            // builds a fresh db when the old one was closed. MongoClientFactory.doesClientExists only
            // checks != null, so a closed MongoClient is handed out forever.
            // FINDING: asserting CURRENT behaviour (closed client returned again).
            InfraConfigService c = config("mongodb://fake-host.invalid:27555", null, null, null, null, 0, null);
            MongoClient first = create(c);
            first.close();

            MongoClient second = create(c);

            assertThat(second, is(sameInstance(first)));
            assertThrows(IllegalStateException.class, () -> second.listDatabaseNames().first());
        }
    }

    // ================================================================ real server

    @Nested
    class RealServer {

        private String uri;

        @BeforeEach
        void connect() {
            uri = MongoTestSupport.connectionString();
        }

        @Test
        public void testRealConnectAndPing() throws Exception {
            MongoClient client = create(config(uri, null, null, null, null, 0, null));

            Document pong = client.getDatabase("admin").runCommand(new Document("ping", 1));

            assertThat(((Number) pong.get("ok")).intValue(), is(1));
        }

        @Test
        public void testLegacyHostPortPathConnectsWhenServerHasNoAuth() throws Exception {
            // Build host/port from the test server. A throwaway Testcontainers mongo has no auth,
            // and asking such a server to authenticate fails, so only do the real ping when the
            // connection string itself carries credentials (docker-compose mongo).
            ConnectionString parsed = new ConnectionString(uri);
            String[] hostPort = parsed.getHosts().get(0).split(":");
            String user = parsed.getUsername();
            if (user == null) {
                // no-auth server: just verify the legacy path builds the right target
                MongoClient client = create(legacyConfig("u", "p", "admin", hostPort[0],
                        Integer.parseInt(hostPort[1]), "directConnection=true"));
                assertThat(settingsOf(client).getClusterSettings().getHosts().get(0).toString(),
                        is(hostPort[0] + ":" + hostPort[1]));
                return;
            }
            String authDb = parsed.getCredential().getSource();
            MongoClient client = create(legacyConfig(user, new String(parsed.getPassword()), authDb,
                    hostPort[0], Integer.parseInt(hostPort[1]), "authSource=" + authDb));

            Document pong = client.getDatabase("admin").runCommand(new Document("ping", 1));
            assertThat(((Number) pong.get("ok")).intValue(), is(1));
        }

        @Test
        public void testPoolListenerReportsToAnalytics() throws Exception {
            MongoClient client = create(config(uri, null, null, null, null, 0, null));
            client.getDatabase("admin").runCommand(new Document("ping", 1));

            verify(analyticsService, timeout(2000).atLeastOnce())
                    .infoLogEvent(eq("HAGRID_INFRA_MONGO_MAX_CONNECTION"), eq("max_connection"), eq(100));
            verify(analyticsService, timeout(2000).atLeastOnce())
                    .meterCounter("HAGRID_INFRA_MONGO_CONNECTION_EVENT", "event", "connection_created");
            verify(analyticsService, timeout(2000).atLeastOnce())
                    .meterCounter("HAGRID_INFRA_MONGO_CONNECTION_EVENT", "event", "checked_out");
        }

        @Test
        public void testIdleTimeAndConnectingEventsUseTheMinConnectionEventName() throws Exception {
            // FINDING: copy/paste in MongoClientFactory.connectionPoolCreated: max_connection_idle_time and
            // max_connecting_time are both logged under "HAGRID_INFRA_MONGO_MIN_CONNECTION", so dashboards
            // mix them with min_connection. Asserting CURRENT behaviour.
            create(config(uri, null, null, null, null, 0, null));

            verify(analyticsService, timeout(2000))
                    .infoLogEvent(eq("HAGRID_INFRA_MONGO_MIN_CONNECTION"), eq("max_connection_idle_time"), any());
            verify(analyticsService, timeout(2000))
                    .infoLogEvent(eq("HAGRID_INFRA_MONGO_MIN_CONNECTION"), eq("max_connecting_time"), any());
        }
    }

    /**
     * Spring-wired cases. The rest of this class avoids Spring on purpose (every test builds its own factory); these two
     * need the real MongoClientFactory bean and the MockFacades, exactly like TestNitriteClientFactory does.
     */
    @Nested
    @org.springframework.boot.test.context.SpringBootTest
    @org.springframework.test.annotation.DirtiesContext(classMode = org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
    class SpringWiring {

        @org.springframework.beans.factory.annotation.Autowired
        com.freshworks.hagrid.shared.infra.MockFacadeInfraConfigService mockFacadeInfraConfigService;

        @org.springframework.beans.factory.annotation.Autowired
        com.freshworks.hagrid.shared.MockFacadeSyncServiceContainer mockFacadeSyncServiceContainer;

        @org.springframework.beans.factory.annotation.Autowired
        MongoClientFactory mongoClientFactory;

        @org.springframework.beans.factory.annotation.Autowired
        MockFacadeMongoClientFactory mockFacadeMongoClientFactory;

        /** Mirror of TestNitriteClientFactory.testSingleNitriteClientIsCreatedForMultipleNitriteService. */
        @Test
        public void testSingleMongoClientIsCreatedForMultipleMongoService() throws Exception {

            InfraConfigService infraConfigService = mockFacadeInfraConfigService.configure().build();
            org.mockito.Mockito.doReturn(MongoTestSupport.connectionString()).when(infraConfigService).getMongoConnectionString();
            org.mockito.Mockito.doReturn("").when(infraConfigService).getDatabaseUserName();
            org.mockito.Mockito.doReturn("").when(infraConfigService).getDatabasePassword();
            org.mockito.Mockito.doReturn("").when(infraConfigService).getDatabaseAuthDb();

            // Nitrite's factory takes the namespace as an argument; Mongo's reads it from the container, so register it there
            NamespaceService namespace = new NamespaceService();
            namespace.setNamespace("dummy_namespace");
            com.freshworks.hagrid.shared.SyncServiceContainer container = mockFacadeSyncServiceContainer.configure()
                    .add(namespace, NamespaceService.class).build();

            MongoClient mongoClient1 = mongoClientFactory.getMongoClientObject(container, infraConfigService);
            MongoClient mongoClient2 = mongoClientFactory.getMongoClientObject(container, infraConfigService);

            org.hamcrest.MatcherAssert.assertThat(mongoClient1, org.hamcrest.Matchers.equalToObject(mongoClient2));
            mongoClient1.close();
        }

        /** MockFacadeMongoClientFactory (mirror of MockFacadeNitriteClientFactory). */
        @Test
        public void testMockFacadeMongoClientFactoryBuildsASpyOverTheBean() throws Exception {
            MongoClientFactory spy = mockFacadeMongoClientFactory.configure().build();
            org.hamcrest.MatcherAssert.assertThat(spy, org.hamcrest.Matchers.notNullValue());
            org.hamcrest.MatcherAssert.assertThat(org.mockito.Mockito.mockingDetails(spy).isSpy(), org.hamcrest.Matchers.is(true));
        }
    }
}
