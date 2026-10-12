package com.freshworks.hagrid.shared.infra.mongo;

import org.junit.jupiter.api.Assumptions;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MongoDBContainer;

/**
 * Gives Mongo tests a connection string.
 *
 * 1. HAGRID_TEST_MONGO_URI set -> use it, e.g. the docker-compose instance:
 *    mongodb://admin:password12345@localhost:27017/?authSource=admin
 * 2. Otherwise start a throwaway Testcontainers mongo (stopped by a shutdown hook).
 * 3. No env var and no Docker -> the calling test is skipped, not failed.
 *
 * Call connectionString() from @BeforeAll.
 */
public final class MongoTestSupport {

    private static MongoDBContainer container;

    private MongoTestSupport() {}

    public static synchronized String connectionString() {

        String uri = System.getenv("HAGRID_TEST_MONGO_URI");
        if (uri != null && !uri.isBlank()) {
            return uri;
        }

        Assumptions.assumeTrue(DockerClientFactory.instance().isDockerAvailable(),
                "Docker not available and HAGRID_TEST_MONGO_URI not set; skipping Mongo tests");

        if (container == null) {
            container = new MongoDBContainer("mongo:7.0");
            container.start();
            // Ryuk is disabled (podman), so stop the container ourselves
            Runtime.getRuntime().addShutdownHook(new Thread(container::stop));
        }
        return container.getConnectionString();
    }
}
