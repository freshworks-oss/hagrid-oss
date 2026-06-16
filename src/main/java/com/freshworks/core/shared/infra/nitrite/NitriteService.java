package com.freshworks.core.shared.infra.nitrite;

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

import org.dizitart.no2.Nitrite;
import org.h2.tools.Server;

import java.io.IOException;
import java.sql.Connection;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;


public class NitriteService implements InfraService {

    // We are adding this locking implementation as we have identified a bug in H2 where if two concurrent threads
    // are deleting the same namespace at the same time then H2 throws errors of various kinds.

    ReentrantReadWriteLock.WriteLock schemaDeletionLock = new ReentrantReadWriteLock().writeLock();

    SyncServiceContainer syncServiceContainer;

    Nitrite nitriteDb;

    static HashMap<String, HashMap<String, NitriteDbQueue>> persistentQueueSingletonMap = new HashMap<>();
    ReentrantReadWriteLock.WriteLock uniqueQueue = new ReentrantReadWriteLock().writeLock();

    static HashMap<String, HashMap<String, NitriteDbList>> persistentListSingletonMap = new HashMap<>();
    ReentrantReadWriteLock.WriteLock uniqueList = new ReentrantReadWriteLock().writeLock();

    static HashMap<String, HashMap<String, NitriteDbKeyValue>> persistentKeyValueSingletonMap = new HashMap<>();
    ReentrantReadWriteLock.WriteLock uniqueKeyValue = new ReentrantReadWriteLock().writeLock();

    InfraConfigService infraConfigService;

    AnalyticsService analyticsService;

    String dataPath;

    String namespace;
    NitriteFactory nitriteFactory;

    public NitriteService() throws IOException {

    }


    @Override
    public void configure(SyncServiceContainer syncServiceContainer, InfraConfigService infraConfigService) throws Exception {
        this.syncServiceContainer = syncServiceContainer;
        this.infraConfigService = infraConfigService;
        this.namespace = syncServiceContainer.getBean(Namespace.class).getNamespace();
        nitriteFactory = syncServiceContainer.getBean(NitriteFactory.class);
        this.nitriteDb = nitriteFactory.getNitriteClient(this.namespace,infraConfigService);
        AnalyticsFactory analyticsFactory = syncServiceContainer.getBean(AnalyticsFactory.class);
        this.analyticsService = analyticsFactory.getAnalyticsService(namespace);
    }

    @Override
    public NitriteDbQueue getProcessorQueue() throws Exception{

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


    public NitriteDbList getPublisherList() throws Exception{

        return getH2DbList(this.namespace, "publisher_list");
    }

    @Override
    public NitriteDbKeyValue getKeyValue() throws Exception{

        return getH2DbKeyValue(this.namespace, "key_value");
    }


    @Override
    public NitriteDbList getInfraDbList(String listName) throws Exception{

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

            if(Boolean.FALSE.equals(nitriteDb.isClosed())){
                nitriteDb.close();
            }
            
        }

        finally {
            schemaDeletionLock.unlock();
        }
    }

    private NitriteDbQueue getH2DbQueue(String namespace, String queueName)  throws Exception{

        try{

            uniqueQueue.lock();
            if(persistentQueueSingletonMap.containsKey(namespace) && persistentQueueSingletonMap.get(namespace).containsKey(queueName)){
                return persistentQueueSingletonMap.get(namespace).get(queueName);
            }

            else if (persistentQueueSingletonMap.containsKey(namespace) && !persistentQueueSingletonMap.get(namespace).containsKey(queueName)){

                NitriteDbQueue H2DbQueue = new NitriteDbQueue(nitriteDb, namespace, queueName);
                persistentQueueSingletonMap.get(namespace).put(queueName, H2DbQueue);
                return H2DbQueue;
            }

            else{
                NitriteDbQueue mongoDbQueue = new NitriteDbQueue(nitriteDb, namespace, queueName);
                HashMap<String, NitriteDbQueue> map = new HashMap<>();
                map.put(queueName, mongoDbQueue);
                persistentQueueSingletonMap.put(namespace, map);
                return mongoDbQueue;
            }

        }
        finally {

            uniqueQueue.unlock();
        }
    } // close

    private NitriteDbList getH2DbList(String namespace, String listName) throws Exception{
        // This should return singleton for single listname

        try{

            uniqueList.lock();
            if(persistentListSingletonMap.containsKey(namespace) && persistentListSingletonMap.get(namespace).containsKey(listName)){
                return persistentListSingletonMap.get(namespace).get(listName);
            }

            else if(persistentListSingletonMap.containsKey(namespace) && !persistentListSingletonMap.get(namespace).containsKey(listName)){
                NitriteDbList mongoDbList = new NitriteDbList(nitriteDb, namespace, listName);
                persistentListSingletonMap.get(namespace).put(listName, mongoDbList);
                return mongoDbList;
            }
            else{
                NitriteDbList mongoDbList = new NitriteDbList(nitriteDb, namespace, listName);
                HashMap<String, NitriteDbList> map = new HashMap<>();
                map.put(listName, mongoDbList);
                persistentListSingletonMap.put(namespace, map);
                return mongoDbList;
            }

        }

        finally {
            uniqueList.unlock();
        }
    }

    private NitriteDbKeyValue getH2DbKeyValue(String namespace, String keyValueName) throws Exception{

        try{

            uniqueKeyValue.lock();
            if(persistentKeyValueSingletonMap.containsKey(namespace) && persistentKeyValueSingletonMap.get(namespace).containsKey(keyValueName)){
                return persistentKeyValueSingletonMap.get(namespace).get(keyValueName);
            }
            else if (persistentKeyValueSingletonMap.containsKey(namespace) && !persistentKeyValueSingletonMap.get(namespace).containsKey(keyValueName)){
                NitriteDbKeyValue mongoDbKeyValue = new NitriteDbKeyValue(nitriteDb, namespace, keyValueName);
                persistentKeyValueSingletonMap.get(namespace).put(keyValueName, mongoDbKeyValue);
                return mongoDbKeyValue;
            }
            else{
                NitriteDbKeyValue mongoDbKeyValue = new NitriteDbKeyValue(nitriteDb, namespace, keyValueName);
                HashMap<String, NitriteDbKeyValue> map = new HashMap<>();
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
