package com.freshworks.core.shared.infra.persistent;

import com.freshworks.core.shared.Namespace;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.analytics.AnalyticsFactory;
import com.freshworks.core.shared.analytics.AnalyticsService;
import com.freshworks.core.shared.infra.InfraConfigService;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.connection.ConnectionPoolSettings;
import com.mongodb.event.*;
import lombok.Getter;
import org.springframework.stereotype.Component;


import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.util.StringUtils;

@Getter
@Component
public class MongoClientFactory {


    MongoClient mongoClient;
    AtomicBoolean uniqueClient = new AtomicBoolean(false);
    public MongoClient getMongoClientObject(SyncServiceContainer syncServiceContainer, InfraConfigService infraConfigService) throws IOException {


        try{
            for(;;){

                if(uniqueClient.compareAndSet(false, true)) {

                    if(doesClientExists()) {
                        return mongoClient;
                    }

                    // Check for connection_string first to short-circuit legacy path
                    String configuredConnectionString = infraConfigService.getConnectionString();
                    String connectionUri;
                    MongoCredential credential = null;

                    if (StringUtils.hasText(configuredConnectionString)) {
                        connectionUri = configuredConnectionString;
                        // Only create credential if not already embedded in the connection string
                        ConnectionString parsed = new ConnectionString(configuredConnectionString);
                        if (parsed.getUsername() == null || parsed.getUsername().isEmpty()) {
                            String dbUserName = infraConfigService.getDatabaseUserName();
                            String dbPassword = infraConfigService.getDatabasePassword();
                            String databaseAuthDb = infraConfigService.getDatabaseAuthDb();
                            if (StringUtils.hasText(dbUserName) && StringUtils.hasText(dbPassword)) {
                                credential = MongoCredential.createCredential(dbUserName, databaseAuthDb, dbPassword.toCharArray());
                            }
                        }
                    } else {
                        String databaseAuthDb = infraConfigService.getDatabaseAuthDb();
                        String dbUserName = infraConfigService.getDatabaseUserName();
                        String dbPassword = infraConfigService.getDatabasePassword();
                        String dbHost = infraConfigService.getDatabaseHost();
                        String additionalParams = infraConfigService.getAdditionalParams();
                        Integer dbPort = infraConfigService.getDatabasePort();

                        if (!StringUtils.hasText(dbUserName) || !StringUtils.hasText(dbPassword)
                                || !StringUtils.hasText(databaseAuthDb) || !StringUtils.hasText(dbHost)) {
                            throw new IllegalStateException(
                                    "Missing required MongoDB configuration: dbUserName, dbPassword, databaseAuthDb, and dbHost must all be set when no connection_string is provided");
                        }

                        credential = MongoCredential.createCredential(dbUserName, databaseAuthDb, dbPassword.toCharArray());

                        if (dbHost.contains("-srv")) {
                            connectionUri = "mongodb+srv://" + dbHost +
                                (StringUtils.hasText(additionalParams) ? "/?" + additionalParams : "");
                        } else {
                            connectionUri = "mongodb://" + dbHost + ":" + dbPort +
                                (StringUtils.hasText(additionalParams) ? "/?" + additionalParams : "");
                        }
                    }
//            MongoClientSettings mongoClientSettings = MongoClientSettings.builder()
//                    .credential(credential)
//                    .readPreference(ReadPreference.secondaryPreferred())
//                    .retryWrites(false)
//                    .applyToSslSettings(builder -> builder
//                            .enabled(false))
//                    .applyToClusterSettings(builder -> builder.hosts(
//                                    Arrays.asList(serverAddress0
//                                    ))
//                            .requiredReplicaSetName("rs0"))
//                    .build();
//            mongoClient = MongoClients.create(mongoClientSettings);

                    ConnectionPoolListener connectionPoolListener = new ConnectionPoolListener() {

                        AnalyticsFactory analyticsFactory = syncServiceContainer.getBean(AnalyticsFactory.class);
                        Namespace namespace = syncServiceContainer.getBean(Namespace.class);
                        AnalyticsService analyticsService = analyticsFactory.getAnalyticsService(namespace.getNamespace());

                        @Override
                        public void connectionPoolCreated(ConnectionPoolCreatedEvent event) {

                            ConnectionPoolSettings connectionPoolSettings = event.getSettings();
                            analyticsService.infoLogEvent("HAGRID_INFRA_MONGO_MAX_CONNECTION",  "max_connection" ,connectionPoolSettings.getMaxSize());
                            analyticsService.infoLogEvent("HAGRID_INFRA_MONGO_MIN_CONNECTION", "min_connection", connectionPoolSettings.getMinSize());
                            analyticsService.infoLogEvent("HAGRID_INFRA_MONGO_MIN_CONNECTION",  "max_connection_idle_time", connectionPoolSettings.getMaxConnectionIdleTime(TimeUnit.SECONDS));
                            analyticsService.infoLogEvent("HAGRID_INFRA_MONGO_MIN_CONNECTION", "max_connecting_time", connectionPoolSettings.getMaxConnecting());
                        }

                        @Override
                        public void connectionCheckedOut(ConnectionCheckedOutEvent event) {
                            // Track checked out connections
                            analyticsService.meterCounter("HAGRID_INFRA_MONGO_CONNECTION_EVENT", "event", "checked_out");
                            analyticsService.debugLogEvent("HAGRID_INFRA_MONGO_CONNECTION_EVENT", "event", "checked_out");
                        }

                        @Override
                        public void connectionCheckedIn(ConnectionCheckedInEvent event) {
                            // Track checked in connections
                            analyticsService.meterCounter("HAGRID_INFRA_MONGO_CONNECTION_EVENT", "event", "checked_in");
                            analyticsService.debugLogEvent("HAGRID_INFRA_MONGO_CONNECTION_EVENT", "event", "checked_in");
                        }

                        @Override
                        public void connectionCreated(ConnectionCreatedEvent event) {
                            analyticsService.meterCounter("HAGRID_INFRA_MONGO_CONNECTION_EVENT", "event", "connection_created");
                            analyticsService.debugLogEvent("HAGRID_INFRA_MONGO_CONNECTION_EVENT", "event", "connection_created");
                        }

                        @Override
                        public void connectionClosed(ConnectionClosedEvent event) {
                            analyticsService.meterCounter("HAGRID_INFRA_MONGO_CONNECTION_EVENT", "event", "connection_closed");
                            analyticsService.debugLogEvent("HAGRID_INFRA_MONGO_CONNECTION_EVENT", "event", "connection_closed");
                        }

                        // Implement other methods as needed
                    };

                    ConnectionString connectionString = new ConnectionString(connectionUri);
                    MongoClientSettings.Builder settingsBuilder = MongoClientSettings.builder()
                            .applyConnectionString(connectionString)
                            .applyToConnectionPoolSettings(builder -> builder
                                    .maxSize(100) // Maximum number of connections in the pool
                                    .minSize(10)  // Minimum number of connections in the pool
                                    .maxWaitTime(10000, TimeUnit.MILLISECONDS) // Max wait time for a connection
                                    .maxConnectionIdleTime(60, TimeUnit.SECONDS) // Max idle time for a connection
                                    .addConnectionPoolListener(connectionPoolListener)
                            );
                    if (credential != null) {
                        settingsBuilder.credential(credential);
                    }
                    MongoClientSettings mongoClientSettings = settingsBuilder.build();

                    mongoClient = MongoClients.create(mongoClientSettings);
                    uniqueClient.set(false);
                    return mongoClient;
                }
            }
        }

        finally {
            uniqueClient.set(false);
        }
    }

    public boolean doesClientExists(){

        if(mongoClient != null) {
            return true;
        }
        else{
            return false;
        }
    }
}
