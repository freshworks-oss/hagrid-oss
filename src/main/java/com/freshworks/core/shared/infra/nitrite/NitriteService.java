package com.freshworks.core.shared.infra.nitrite;

import com.freshworks.core.shared.NamespaceService;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.analytics.AnalyticsFactory;
import com.freshworks.core.shared.analytics.AnalyticsService;
import com.freshworks.core.shared.infra.InfraConfigService;
import com.freshworks.core.shared.infra.InfraService;
import com.zaxxer.hikari.HikariDataSource;

import org.dizitart.no2.Nitrite;

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
        this.namespace = syncServiceContainer.getBean(NamespaceService.class).getNamespace();
        nitriteFactory = syncServiceContainer.getBean(NitriteFactory.class);
        this.nitriteDb = nitriteFactory.getNitriteClient(this.namespace,infraConfigService);
        AnalyticsFactory analyticsFactory = syncServiceContainer.getBean(AnalyticsFactory.class);
        this.analyticsService = analyticsFactory.getAnalyticsService(namespace);
    }

    @Override
    public NitriteDbQueue getProcessorQueue() throws Exception{

        NitriteDbQueue nitriteDbQueue = getNitriteDbQueue(namespace, "processor");
        this.analyticsService.meterGauge("infra.collection.size", nitriteDbQueue, queue -> {
            try {
                double size = queue.size();
                return size;
            } catch (Exception e) {
                
                return 0d;
            }
        }, "name", nitriteDbQueue.getQueueName().substring(namespace.length()));

        return nitriteDbQueue;
    }

    public NitriteDbList getPublisherList() throws Exception{

        NitriteDbList nitriteDbList =  getNitriteDbList(this.namespace, "publisher_list");

        this.analyticsService.meterGauge("infra.collection.size", nitriteDbList, list -> {
            try {
                double size = list.size();
                return size;
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return 0D;
            }
        }, "name", "publisher_list");

        return nitriteDbList;

    }

    @Override
    public NitriteDbKeyValue getKeyValue() throws Exception{

        NitriteDbKeyValue nitriteDbKeyValue = getNitriteDbKeyValue(this.namespace, "key_value");

        this.analyticsService.meterGauge("infra.collection.size", nitriteDbKeyValue, keyValue -> {
            try {
                double size = keyValue.size();
                return size;
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return 0D;
            }
        }, "name", "key_value");

        return nitriteDbKeyValue;
    }


    @Override
    public NitriteDbList getInfraDbList(String listName) throws Exception{

        NitriteDbList nitriteDbList =  getNitriteDbList(this.namespace, listName);

        this.analyticsService.meterGauge("infra.collection.size", nitriteDbList, list -> {
            try {
                double size = list.size();
                return size;
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return 0D;
            }
        }, "name", listName);

        return nitriteDbList;
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

            if(Boolean.FALSE.equals(nitriteDb.isClosed())){
                nitriteDb.close();
            }
            
        }

        finally {
            schemaDeletionLock.unlock();
        }
    }

    private NitriteDbQueue getNitriteDbQueue(String namespace, String queueName)  throws Exception{

        try{

            uniqueQueue.lock();
            if(persistentQueueSingletonMap.containsKey(namespace) && persistentQueueSingletonMap.get(namespace).containsKey(queueName)){
                return persistentQueueSingletonMap.get(namespace).get(queueName);
            }

            else if (persistentQueueSingletonMap.containsKey(namespace) && !persistentQueueSingletonMap.get(namespace).containsKey(queueName)){

                if (!isDatabaseOpen()){
                    throw new IllegalStateException("Nitrite DB is closed and newNitriteDbQueue creation operation has been asked to perform in the nitrite service");
                }

                NitriteDbQueue nitriteDbDbQueue = new NitriteDbQueue(nitriteDb, namespace, queueName);
                nitriteDbDbQueue.configure(syncServiceContainer);
                persistentQueueSingletonMap.get(namespace).put(queueName, nitriteDbDbQueue);
                return nitriteDbDbQueue;
            }

            else{

                if (!isDatabaseOpen()){
                    throw new IllegalStateException("Nitrite DB is closed and newNitriteDbQueue creation operation has been asked to perform in the nitrite service");
                }

                NitriteDbQueue nitriteDbQueue = new NitriteDbQueue(nitriteDb, namespace, queueName);
                nitriteDbQueue.configure(syncServiceContainer);
                HashMap<String, NitriteDbQueue> map = new HashMap<>();
                map.put(queueName, nitriteDbQueue);
                persistentQueueSingletonMap.put(namespace, map);
                return nitriteDbQueue;
            }

        }
        finally {

            uniqueQueue.unlock();
        }
    } // close

    private NitriteDbList getNitriteDbList(String namespace, String listName) throws Exception{
        // This should return singleton for single listname

        try{

            uniqueList.lock();
            if(persistentListSingletonMap.containsKey(namespace) && persistentListSingletonMap.get(namespace).containsKey(listName)){
                return persistentListSingletonMap.get(namespace).get(listName);
            }

            else if(persistentListSingletonMap.containsKey(namespace) && !persistentListSingletonMap.get(namespace).containsKey(listName)){

                if (!isDatabaseOpen()){
                    throw new IllegalStateException("Nitrite DB is closed and newNitriteDbList creation operation has been asked to perform in the nitrite service");
                }


                NitriteDbList nitriteDbList = new NitriteDbList(nitriteDb, namespace, listName);
                nitriteDbList.configure(syncServiceContainer);
                persistentListSingletonMap.get(namespace).put(listName, nitriteDbList);
                return nitriteDbList;
            }
            else{

                if (!isDatabaseOpen()){
                    throw new IllegalStateException("Nitrite DB is closed and newNitriteDbList creation operation has been asked to perform in the nitrite service");
                }

                NitriteDbList nitriteDbList = new NitriteDbList(nitriteDb, namespace, listName);
                nitriteDbList.configure(syncServiceContainer);
                HashMap<String, NitriteDbList> map = new HashMap<>();
                map.put(listName, nitriteDbList);
                persistentListSingletonMap.put(namespace, map);
                return nitriteDbList;
            }

        }

        finally {
            uniqueList.unlock();
        }
    }

    private NitriteDbKeyValue getNitriteDbKeyValue(String namespace, String keyValueName) throws Exception{

        try{

            uniqueKeyValue.lock();
            if(persistentKeyValueSingletonMap.containsKey(namespace) && persistentKeyValueSingletonMap.get(namespace).containsKey(keyValueName)){
                return persistentKeyValueSingletonMap.get(namespace).get(keyValueName);
            }
            else if (persistentKeyValueSingletonMap.containsKey(namespace) && !persistentKeyValueSingletonMap.get(namespace).containsKey(keyValueName)){

                if (!isDatabaseOpen()){
                    throw new IllegalStateException("Nitrite DB is closed and newNitriteDbKeyValue creation operation has been asked to perform in the nitrite service");
                }

                NitriteDbKeyValue nitriteDbKeyValue = new NitriteDbKeyValue(nitriteDb, namespace, keyValueName);
                nitriteDbKeyValue.configure(syncServiceContainer);
                persistentKeyValueSingletonMap.get(namespace).put(keyValueName, nitriteDbKeyValue);
                return nitriteDbKeyValue;
            }
            else{

                if (!isDatabaseOpen()){
                    throw new IllegalStateException("Nitrite DB is closed and newNitriteDbKeyValue creation operation has been asked to perform in the nitrite service");
                }
                
                NitriteDbKeyValue nitriteDbKeyValue = new NitriteDbKeyValue(nitriteDb, namespace, keyValueName);
                nitriteDbKeyValue.configure(syncServiceContainer);
                HashMap<String, NitriteDbKeyValue> map = new HashMap<>();
                map.put(keyValueName, nitriteDbKeyValue);
                persistentKeyValueSingletonMap.put(namespace, map);
                return nitriteDbKeyValue;
            }
        }

        finally {
            uniqueKeyValue.unlock();
        }
    }

    private boolean isDatabaseOpen(){

        if (this.nitriteDb != null && Boolean.FALSE.equals(this.nitriteDb.isClosed())){
            return true;
        }
        else{
            return false;
        }
    }

}
