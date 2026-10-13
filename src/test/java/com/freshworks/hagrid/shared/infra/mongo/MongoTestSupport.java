package com.freshworks.hagrid.shared.infra.mongo;

import java.util.concurrent.TimeUnit;

import org.bson.Document;
import org.junit.jupiter.api.Assumptions;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MongoDBContainer;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

/**
 * Gives Mongo tests a connection string.
 *
 * 1. HAGRID_TEST_MONGO_URI set -> use it as is (always wins).
 * 2. Otherwise -Dmongo.test.target picks where the tests run:
 *      local      the Mongo already running on your machine (fails loudly if it is not reachable)
 *      container  a throwaway Testcontainers mongo (stopped by a shutdown hook)
 *      auto       (default) local if it answers a ping, else a container
 *    The local URI defaults to mongodb://admin:password12345@localhost:27017/?authSource=admin and can be changed with
 *    -Dmongo.test.local.uri=...
 * 3. Container wanted but no Docker -> the calling test is skipped, not failed.
 *
 * Call connectionString() from @BeforeAll.
 */
public final class MongoTestSupport {

    private static final String DEFAULT_LOCAL_URI = "mongodb://admin:password12345@localhost:27017/?authSource=admin";

    private static MongoDBContainer container;

    private MongoTestSupport() {}

    public static synchronized String connectionString() {

        String uri = System.getenv("HAGRID_TEST_MONGO_URI");
        if (uri != null && !uri.isBlank()) {
            return uri;
        }

        String target = System.getProperty("mongo.test.target", "auto").trim().toLowerCase();
        String localUri = System.getProperty("mongo.test.local.uri", DEFAULT_LOCAL_URI);

        switch (target) {
            case "local":
                if (!isReachable(localUri)) {
                    throw new IllegalStateException("-Dmongo.test.target=local but nothing answers at " + localUri
                            + ". Start your local mongod or change -Dmongo.test.local.uri.");
                }
                return localUri;
            case "container":
                return containerConnectionString();
            case "auto":
                return isReachable(localUri) ? localUri : containerConnectionString();
            default:
                throw new IllegalArgumentException("-Dmongo.test.target must be local, container or auto, was: " + target);
        }
    }

    /** True when the server at this URI answers a ping within 2 seconds (wrong password counts as not reachable). */
    private static boolean isReachable(String uri) {
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(uri))
                .applyToClusterSettings(c -> c.serverSelectionTimeout(2, TimeUnit.SECONDS))
                .build();
        try (MongoClient client = MongoClients.create(settings)) {
            client.getDatabase("admin").runCommand(new Document("ping", 1));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static String containerConnectionString() {

        Assumptions.assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker not available and no reachable local Mongo; skipping Mongo tests");

        if (container == null) {
            container = new MongoDBContainer("mongo:7.0");
            container.start();
            // Ryuk is disabled (podman), so stop the container ourselves
            Runtime.getRuntime().addShutdownHook(new Thread(container::stop));
        }
        return container.getConnectionString();
    }
}
