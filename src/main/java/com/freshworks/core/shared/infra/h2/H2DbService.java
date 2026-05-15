package com.freshworks.core.shared.infra.h2;

import com.freshworks.core.shared.Namespace;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.analytics.AnalyticsFactory;
import com.freshworks.core.shared.analytics.AnalyticsService;
import com.freshworks.core.shared.infra.InfraConfigService;
import com.freshworks.core.shared.infra.InfraService;
import com.freshworks.freshindex.NamespaceService;
import com.freshworks.freshindex.index.JsonIndexService;
import com.freshworks.freshindex.index.query.JsonQueryService;
import com.zaxxer.hikari.HikariDataSource;
import org.h2.tools.Server;

import java.io.IOException;
import java.sql.Connection;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;


public class H2DbService implements InfraService {

    // We are adding this locking implementation as we have identified a bug in H2 where if two concurrent threads
    // are deleting the same namespace at the same time then H2 throws errors of various kinds.

    ReentrantReadWriteLock.WriteLock schemaDeletionLock = new ReentrantReadWriteLock().writeLock();

    SyncServiceContainer syncServiceContainer;

    HikariDataSource hikariDataSource;

    static HashMap<String, HashMap<String, H2DbQueue>> persistentQueueSingletonMap = new HashMap<>();
    ReentrantReadWriteLock.WriteLock uniqueQueue = new ReentrantReadWriteLock().writeLock();

    static HashMap<String, HashMap<String, H2DbList>> persistentListSingletonMap = new HashMap<>();
    ReentrantReadWriteLock.WriteLock uniqueList = new ReentrantReadWriteLock().writeLock();

    static HashMap<String, HashMap<String, H2DbKeyValue>> persistentKeyValueSingletonMap = new HashMap<>();
    ReentrantReadWriteLock.WriteLock uniqueKeyValue = new ReentrantReadWriteLock().writeLock();

    InfraConfigService infraConfigService;

    AnalyticsService analyticsService;

    String dataPath;

    String namespace;
    H2Factory h2Factory;

    public H2DbService() throws IOException {

    }


    @Override
    public void configure(SyncServiceContainer syncServiceContainer, InfraConfigService infraConfigService) throws Exception {
        this.syncServiceContainer = syncServiceContainer;
        this.infraConfigService = infraConfigService;
        this.namespace = syncServiceContainer.getBean(Namespace.class).getNamespace();
        h2Factory = syncServiceContainer.getBean(H2Factory.class);
        this.hikariDataSource = h2Factory.getH2Client(this.namespace,infraConfigService);
        this.dataPath = infraConfigService.getH2DataPath();
        AnalyticsFactory analyticsFactory = syncServiceContainer.getBean(AnalyticsFactory.class);
        this.analyticsService = analyticsFactory.getAnalyticsService(namespace);
    }

    @Override
    public H2DbQueue getProcessorQueue() throws Exception{

        return getH2DbQueue(namespace, "processor");
    }

    @Override
    public JsonIndexService getJsonIndexService() throws Exception{

        JsonIndexService jsonIndexService = syncServiceContainer.getBean(JsonIndexService.class);
        jsonIndexService.configure(this.namespace);
        return  jsonIndexService;
    }

    @Override
    public JsonQueryService getJsonQueryService() throws Exception{
        JsonQueryService jsonQueryService = syncServiceContainer.getBean(JsonQueryService.class);
        jsonQueryService.configure(this.namespace);
        return  jsonQueryService;
    }

    @Override
    public NamespaceService getNamespaceService() throws Exception{

        NamespaceService namespaceService = syncServiceContainer.getBean(NamespaceService.class);
        return namespaceService;
    }

    @Override
    public void destroyFreshIndex() throws Exception{

        getNamespaceService().clearnNamespace(this.namespace);
    }


    public H2DbList getPublisherList() throws Exception{

        return getH2DbList(this.namespace, "publisher_list");
    }

    @Override
    public H2DbKeyValue getKeyValue() throws Exception{

        return getH2DbKeyValue(this.namespace, "key_value");
    }


    @Override
    public H2DbList getInfraDbList(String listName) throws Exception{

        return getH2DbList(this.namespace, listName);
    }

    @Override
    public String getNamespace() throws Exception {
        return this.namespace;
    }


    @Override
    public void destroy() throws Exception{
        try{

            // First take a lock so that no two concurrent schema deletion occur
            schemaDeletionLock.lock();

            persistentQueueSingletonMap.remove(this.namespace);
            persistentListSingletonMap.remove(this.namespace);
            persistentKeyValueSingletonMap.remove(this.namespace);


            // We need to clear the freshIndex as well.
            destroyFreshIndex();


            String sql = "DROP SCHEMA IF EXISTS " + sanitizeName(namespace)  + " CASCADE;";

            try(Connection connection = hikariDataSource.getConnection();){

                // There will be a case when threads are interrupted then JDBC will close the database file channel
                // Now if there are connections lies in hikari .. then with setting setConnectionTestQuery, Hikari
                // will check if connection is valid. If not then it will take another connection and will keep doing.
                // Once all say 100 ( 0 - 99 ) are exhausted, then it will create 100th connection again ( new connection )
                // which will succeed this time as new connection will open new file channel

                if(connection != null && connection.isValid(2)){
                    connection.createStatement().execute(sql);
                }
                else{
                    hikariDataSource = h2Factory.getHikariDataSource();
                    try(Connection connection2 = hikariDataSource.getConnection();){
                        connection2.createStatement().execute(sql);
                    }
                }

            }
        }

        finally {
            schemaDeletionLock.unlock();
        }
    }

    private H2DbQueue getH2DbQueue(String namespace, String queueName)  throws Exception{

        try{

            uniqueQueue.lock();
            if(persistentQueueSingletonMap.containsKey(namespace) && persistentQueueSingletonMap.get(namespace).containsKey(queueName)){
                return persistentQueueSingletonMap.get(namespace).get(queueName);
            }

            else if (persistentQueueSingletonMap.containsKey(namespace) && !persistentQueueSingletonMap.get(namespace).containsKey(queueName)){

                H2DbQueue H2DbQueue = new H2DbQueue(hikariDataSource, namespace, queueName);
                persistentQueueSingletonMap.get(namespace).put(queueName, H2DbQueue);
                return H2DbQueue;
            }

            else{
                H2DbQueue mongoDbQueue = new H2DbQueue(hikariDataSource, namespace, queueName);
                HashMap<String, H2DbQueue> map = new HashMap<>();
                map.put(queueName, mongoDbQueue);
                persistentQueueSingletonMap.put(namespace, map);
                return mongoDbQueue;
            }

        }
        finally {

            uniqueQueue.unlock();
        }
    } // close

    private H2DbList getH2DbList(String namespace, String listName) throws Exception{
        // This should return singleton for single listname

        try{

            uniqueList.lock();
            if(persistentListSingletonMap.containsKey(namespace) && persistentListSingletonMap.get(namespace).containsKey(listName)){
                return persistentListSingletonMap.get(namespace).get(listName);
            }

            else if(persistentListSingletonMap.containsKey(namespace) && !persistentListSingletonMap.get(namespace).containsKey(listName)){
                H2DbList mongoDbList = new H2DbList(hikariDataSource, namespace, listName);
                persistentListSingletonMap.get(namespace).put(listName, mongoDbList);
                return mongoDbList;
            }
            else{
                H2DbList mongoDbList = new H2DbList(hikariDataSource, namespace, listName);
                HashMap<String, H2DbList> map = new HashMap<>();
                map.put(listName, mongoDbList);
                persistentListSingletonMap.put(namespace, map);
                return mongoDbList;
            }

        }

        finally {
            uniqueList.unlock();
        }
    }

    private H2DbKeyValue getH2DbKeyValue(String namespace, String keyValueName) throws Exception{

        try{

            uniqueKeyValue.lock();
            if(persistentKeyValueSingletonMap.containsKey(namespace) && persistentKeyValueSingletonMap.get(namespace).containsKey(keyValueName)){
                return persistentKeyValueSingletonMap.get(namespace).get(keyValueName);
            }
            else if (persistentKeyValueSingletonMap.containsKey(namespace) && !persistentKeyValueSingletonMap.get(namespace).containsKey(keyValueName)){
                H2DbKeyValue mongoDbKeyValue = new H2DbKeyValue(hikariDataSource, namespace, keyValueName);
                persistentKeyValueSingletonMap.get(namespace).put(keyValueName, mongoDbKeyValue);
                return mongoDbKeyValue;
            }
            else{
                H2DbKeyValue mongoDbKeyValue = new H2DbKeyValue(hikariDataSource, namespace, keyValueName);
                HashMap<String, H2DbKeyValue> map = new HashMap<>();
                map.put(keyValueName, mongoDbKeyValue);
                persistentKeyValueSingletonMap.put(namespace, map);
                return mongoDbKeyValue;
            }
        }

        finally {
            uniqueKeyValue.unlock();
        }
    }

    private String sanitizeName(String name){

        return "h2_" + name.toLowerCase().replaceAll("\\.", "_").replaceAll("-", "_").replaceAll(":", "_");
    }

}
