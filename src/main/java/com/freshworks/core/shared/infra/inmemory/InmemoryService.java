package com.freshworks.core.shared.infra.inmemory;

import com.freshworks.core.shared.Namespace;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.analytics.AnalyticsFactory;
import com.freshworks.core.shared.analytics.AnalyticsService;
import com.freshworks.core.shared.infra.*;
import com.freshworks.freshindex.NamespaceService;
import com.freshworks.freshindex.index.JsonIndexService;
import com.freshworks.freshindex.index.query.JsonQueryService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


public class InmemoryService implements InfraService {

    SyncServiceContainer syncServiceContainer;

    InfraConfigService infraConfigService;
    String namespace;

    AnalyticsService analyticsService;

    HashMap<String, HashMap<String, InmemoryKeyValue>> inMemoryKeyValueSingletonMap = new HashMap<>();
    ReentrantReadWriteLock.WriteLock uniqueKeyValue = new ReentrantReadWriteLock().writeLock();

    HashMap<String, HashMap<String, InmemoryList>> inMemoryListSingletonMap = new HashMap<>();
    ReentrantReadWriteLock.WriteLock uniqueList = new ReentrantReadWriteLock().writeLock();


    HashMap<String, HashMap<String, InmemoryQueue>> inMemoryQueueSingletonMap = new HashMap<>();
    ReentrantReadWriteLock.WriteLock uniqueQueue = new ReentrantReadWriteLock().writeLock();



    public InmemoryService() throws IOException {


    }

    public void configure(SyncServiceContainer syncServiceContainer, InfraConfigService infraConfigService) throws Exception {
        this.syncServiceContainer = syncServiceContainer;;
        this.namespace = syncServiceContainer.getBean(Namespace.class).getNamespace();
        this.infraConfigService = infraConfigService;
        AnalyticsFactory analyticsFactory = syncServiceContainer.getBean(AnalyticsFactory.class);
        analyticsService = analyticsFactory.getAnalyticsService(namespace);
    }

    public InfraDbQueue getProcessorQueue() throws Exception {
        return getInmemoryQueue(this.namespace, "processor");
    }

    @Override
    public JsonIndexService getJsonIndexService() throws Exception{

        JsonIndexService jsonIndexService = syncServiceContainer.getBean(JsonIndexService.class);
        jsonIndexService.configure(namespace);
        return  jsonIndexService;
    }

    @Override
    public JsonQueryService getJsonQueryService() throws Exception{

        JsonQueryService jsonQueryService = syncServiceContainer.getBean(JsonQueryService.class);
        jsonQueryService.configure(namespace);
        return  jsonQueryService;
    }

    @Override
    public NamespaceService getNamespaceService() throws Exception{

        // Instead of this model, we can modify the freshIndex to have freshIndexService. Via this service we can extract,
        // JsonIndexService, JsonQueryService and Namespace Service. All this needs to be model in FreshIndex
        NamespaceService namespaceService = syncServiceContainer.getBean(NamespaceService.class);
        return namespaceService;
    }

    @Override
    public void destroyFreshIndex() throws Exception{

        getNamespaceService().clearnNamespace(namespace);
    }

    public InfraDbList getPublisherList() throws Exception{

        return getInmemoryList(this.namespace, "publisher_list");
    }

    public InfraDbKeyValue getKeyValue() throws Exception {
        return getInmemoryKeyValue(this.namespace, "key_value");
    }

    public InfraDbList getInfraDbList(String listName) throws Exception {
        return getInmemoryList(this.namespace, listName);
    }

    public String getNamespace() throws Exception{
        return this.namespace;
    }

    public void destroy() throws Exception{

        inMemoryListSingletonMap.remove(this.namespace);
        inMemoryKeyValueSingletonMap.remove(this.namespace);
        inMemoryQueueSingletonMap.remove(this.namespace);

        // We need to clear the freshIndex as well.
        destroyFreshIndex();
    }

    // TODO: It can be optimize - We do not need to lock on whole method. It should be locked only on namespace, so that no two key value with same name are created on same namespace.
    public InmemoryKeyValue getInmemoryKeyValue(String namespace,String keyValueName) throws Exception{

        try{

                uniqueKeyValue.lock();

                if(inMemoryKeyValueSingletonMap.containsKey(namespace) && inMemoryKeyValueSingletonMap.get(namespace).containsKey(keyValueName)){
                    return inMemoryKeyValueSingletonMap.get(namespace).get(keyValueName);
                }
                // If infra exists but keyvalue does not exists
                else if (inMemoryKeyValueSingletonMap.containsKey(namespace) && !inMemoryKeyValueSingletonMap.get(namespace).containsKey(keyValueName)){

                    InmemoryKeyValue inmemoryKeyValue = new InmemoryKeyValue();
                    inmemoryKeyValue.configure(syncServiceContainer);
                    inmemoryKeyValue.setKeyValue(keyValueName);
                    inmemoryKeyValue.setConcurrentHashMap(new ConcurrentHashMap<>());
                    inMemoryKeyValueSingletonMap.get(namespace).put(keyValueName, inmemoryKeyValue);
                    return inmemoryKeyValue;

                }

                else{

                    InmemoryKeyValue inmemoryKeyValue = new InmemoryKeyValue();
                    inmemoryKeyValue.configure(syncServiceContainer);
                    inmemoryKeyValue.setKeyValue(keyValueName);
                    inmemoryKeyValue.setConcurrentHashMap(new ConcurrentHashMap<>());

                    HashMap<String, InmemoryKeyValue> map = new HashMap<>();
                    map.put(keyValueName, inmemoryKeyValue);
                    inMemoryKeyValueSingletonMap.put(namespace, map);
                    return inmemoryKeyValue;
                }
            }

        finally {
            uniqueKeyValue.unlock();
        }

    }

    // TODO: It can be optimize - We do not need to lock on whole method. It should be locked only on namespace, so that no two queue with same name are created on same namespace.
    public InmemoryQueue getInmemoryQueue(String namespace,String queueName) throws Exception{

        try{

                uniqueQueue.lock();
                // If both infra and queueName exists
                if(inMemoryQueueSingletonMap.containsKey(namespace) && inMemoryQueueSingletonMap.get(namespace).containsKey(queueName)){
                    return inMemoryQueueSingletonMap.get(namespace).get(queueName);
                }

                // If infra exists but queueName does not exists
                else if (inMemoryQueueSingletonMap.containsKey(namespace) && !inMemoryQueueSingletonMap.get(namespace).containsKey(queueName)){

                    InmemoryQueue inmemoryQueue = new InmemoryQueue();
                    inmemoryQueue.configure(syncServiceContainer);
                    inmemoryQueue.setQueue(new LinkedList<>());
                    inmemoryQueue.setQueueName(queueName);
                    inMemoryQueueSingletonMap.get(namespace).put(queueName, inmemoryQueue);
                    analyticsService.meterGauge(queueName + ".size", inmemoryQueue, x -> {
                        try {
                            return x.size();
                        } catch (Exception e) {
                            return -100;
                        }
                    });
                    return inmemoryQueue;

                }

                // If both does not exist
                else {
                    InmemoryQueue inmemoryQueue = new InmemoryQueue();
                    inmemoryQueue.configure(syncServiceContainer);
                    inmemoryQueue.setQueue(new LinkedList<>());
                    inmemoryQueue.setQueueName(queueName);
                    HashMap<String, InmemoryQueue> map = new HashMap<>();
                    map.put(queueName, inmemoryQueue);
                    inMemoryQueueSingletonMap.put(namespace, map);
                    analyticsService.meterGauge(queueName + ".size", inmemoryQueue, x -> {
                        try {
                            return x.size();
                        } catch (Exception e) {
                            return -100;
                        }
                    });
                    return inmemoryQueue;
                }
        }

        finally {
            uniqueQueue.unlock();
        }

    }

    // TODO: It can be optimize - We do not need to lock on whole method. It should be locked only on namespace, so that no two list with same name are created on same namespace.
    public InmemoryList getInmemoryList(String namespace , String listName) throws Exception{

        try{

                uniqueList.lock();
                if(inMemoryListSingletonMap.containsKey(namespace) && inMemoryListSingletonMap.get(namespace).containsKey(listName)){
                    return inMemoryListSingletonMap.get(namespace).get(listName);
                }

                // If infra exists but list does not exists
                else if (inMemoryListSingletonMap.containsKey(namespace) && !inMemoryListSingletonMap.get(namespace).containsKey(listName)){

                    InmemoryList inmemoryList = new InmemoryList();
                    inmemoryList.configure(syncServiceContainer);
                    inmemoryList.setList(new ArrayList<>());
                    inmemoryList.setListName(listName);
                    inMemoryListSingletonMap.get(namespace).put(listName, inmemoryList);
                    analyticsService.meterGauge(listName + ".size", inmemoryList, x -> {
                        try {
                            return x.size();
                        } catch (Exception e) {
                            return -100;
                        }
                    });
                    return inmemoryList;

                }
                else{
                    InmemoryList inmemoryList = new InmemoryList();
                    inmemoryList.configure(syncServiceContainer);
                    inmemoryList.setList(new ArrayList<>());
                    inmemoryList.setListName(listName);

                    HashMap<String, InmemoryList> map = new HashMap<>();
                    map.put(listName, inmemoryList);
                    inMemoryListSingletonMap.put(namespace , map);
                    analyticsService.meterGauge(listName + ".size", inmemoryList, x -> {
                        try {
                            return x.size();
                        } catch (Exception e) {
                            return -100;
                        }
                    });
                    return inmemoryList;
                }
            }
        finally {

            uniqueList.unlock();
        }

    }
}
