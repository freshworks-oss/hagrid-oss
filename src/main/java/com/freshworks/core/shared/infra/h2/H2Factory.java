package com.freshworks.core.shared.infra.h2;

import com.freshworks.core.shared.analytics.AnalyticsFactory;
import com.freshworks.core.shared.analytics.AnalyticsService;
import com.freshworks.core.shared.infra.InfraConfigService;
import com.freshworks.core.shared.synchronizers.GlobalNamespaceService;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.micrometer.core.instrument.MeterRegistry;
import joptsimple.internal.Strings;
import lombok.Getter;
import org.h2.tools.Server;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;

@Getter
@Component
public class H2Factory {

    HikariDataSource hikariDataSource;
    AnalyticsService analyticsService;
    GlobalNamespaceService globalNamespaceService;
    MeterRegistry meterRegistry;
    Server h2Server;
    Server webServer;
    AtomicBoolean uniqueServer = new AtomicBoolean(false);
    AtomicBoolean uniqueClient = new AtomicBoolean(false);

    @Autowired
    public H2Factory(AnalyticsFactory analyticsFactory, GlobalNamespaceService globalNamespaceService, MeterRegistry meterRegistry){
        this.globalNamespaceService = globalNamespaceService;
        this.meterRegistry = meterRegistry;
        this.analyticsService = analyticsFactory.getAnalyticsService(this.globalNamespaceService.getGlobalNamespace());
    }

    public HikariDataSource getH2Client(String namespace, InfraConfigService infraConfigService) throws Exception {

        try{

            for(;;){

                if(uniqueClient.compareAndSet(false, true)) {

                     if(doesClientExists()) {
                        return hikariDataSource;
                    }

                    String h2Type = infraConfigService.getH2DatabaseType();
                    HikariConfig config = new HikariConfig();
                    String dbString="";

                    if(h2Type.equalsIgnoreCase("tcp")){

                          if(Boolean.FALSE.equals(Strings.isNullOrEmpty(infraConfigService.getH2HostAddress())) && Boolean.FALSE.equals(Strings.isNullOrEmpty(infraConfigService.getH2DataPath()))){
                              dbString =  "jdbc:h2:" + "tcp://" + infraConfigService.getH2HostAddress() + "/"  + "file:" + infraConfigService.getH2DataPath() + ";LOCK_TIMEOUT=60000;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;AUTO_RECONNECT=TRUE;MODE=MYSQL;TRACE_LEVEL_FILE=3";
                          }
                          else{
                              h2Server = Server.createTcpServer("-tcpPort", "9092", "-tcpAllowOthers", "-ifNotExists").start();
                              webServer = Server.createWebServer("-webAllowOthers", "-ifNotExists").start();
                              System.out.println("H2 Web Console started: " + webServer.getURL());


                              analyticsService.infoEvent("HAGRID_H2_DB_SERVICE", "_message", "starting local h2 server", "namespace", namespace );
                              dbString =  "jdbc:h2:" + "tcp://" + "localhost:9092" + "/"  + "file:" + "~/hagrid-database" + ";LOCK_TIMEOUT=60000;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;AUTO_RECONNECT=TRUE;MODE=MYSQL;TRACE_LEVEL_FILE=3";
                          }

                    }
                    else if (h2Type.equalsIgnoreCase("memory")){
                        dbString =  "jdbc:h2:" + "mem"  + ":"+ infraConfigService.getH2DataPath() + ";LOCK_TIMEOUT=60000;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;AUTO_RECONNECT=TRUE;MODE=MYSQL;TRACE_LEVEL_FILE=3";
                    }

                    config.setMaximumPoolSize(100);
                    config.setJdbcUrl(dbString);
                    config.setUsername("");
                    config.setPassword("");
                    config.setConnectionTestQuery("SELECT 1"); // Ensures DB is accessible
                    config.setMetricRegistry(meterRegistry);
                    hikariDataSource = new HikariDataSource(config);
                    uniqueClient.set(false);
                    return hikariDataSource;
                }
            }
        }

        finally {
            uniqueClient.set(false);
        }
    }

    public boolean doesClientExists() {

        if(hikariDataSource != null && !hikariDataSource.isClosed()) {

            // This is to handle the case when JDBC has closed the database file channel due to some reason
            // like threads are interrupted.
            // Then Hikari will check if it is open or not using a lightweight query
            try(Connection connection = hikariDataSource.getConnection();){
                if(connection != null && connection.isValid(2)) {
                    return true;
                }
                else{
                    analyticsService.warnEvent("INFRA_SERVICE_CONNECTION_FACTORY", "type", "h2", "_message", "Hikari can not make connection with DB. Creating new hikari data source");
                    hikariDataSource.close();
                    return false;
                }
            }
            catch (SQLException e){
                hikariDataSource.close();
                analyticsService.warnEvent("INFRA_SERVICE_CONNECTION_FACTORY", "type", "h2", "_message", e.getMessage() );
                throw new RuntimeException("Failed to create hikari data source", e);
            }
        }
        else{
            return false;
        }
    }

}
