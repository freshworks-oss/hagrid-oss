package com.freshworks.core.shared.infra.persistent;

import com.freshworks.core.shared.Namespace;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.analytics.AnalyticsFactory;
import com.freshworks.core.shared.analytics.AnalyticsService;
import com.freshworks.core.shared.infra.InfraConfigService;
import com.freshworks.core.shared.infra.InfraService;
import com.freshworks.freshindex.NamespaceService;
import com.freshworks.freshindex.index.JsonIndexService;
import com.freshworks.freshindex.index.query.JsonQueryService;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Indexes;
import org.bson.Document;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;


public class MongoService implements InfraService {

    MongoClient mongoClient;

    SyncServiceContainer syncServiceContainer;

    HashMap<String, HashMap<String, MongoDbQueue>> persistentQueueSingletonMap = new HashMap<>();
    ReentrantReadWriteLock.WriteLock uniqueQueue = new ReentrantReadWriteLock().writeLock();

    HashMap<String, HashMap<String, MongoDbList>> persistentListSingletonMap = new HashMap<>();
    ReentrantReadWriteLock.WriteLock uniqueList = new ReentrantReadWriteLock().writeLock();

    HashMap<String, HashMap<String, MongoDbKeyValue>> persistentKeyValueSingletonMap = new HashMap<>();
    ReentrantReadWriteLock.WriteLock uniqueKeyValue = new ReentrantReadWriteLock().writeLock();

    InfraConfigService infraConfigService;

    AnalyticsService analyticsService;

    String namespace;


    public MongoService() throws IOException {

    }



    @Override
    public void configure(SyncServiceContainer syncServiceContainer, InfraConfigService infraConfigService) throws Exception {
        this.syncServiceContainer = syncServiceContainer;
        this.infraConfigService = infraConfigService;
        this.namespace = syncServiceContainer.getBean(Namespace.class).getNamespace();
        MongoClientFactory mongoClientFactory = syncServiceContainer.getBean(MongoClientFactory.class);
        this.mongoClient = mongoClientFactory.getMongoClientObject(syncServiceContainer, infraConfigService);
        AnalyticsFactory analyticsFactory = syncServiceContainer.getBean(AnalyticsFactory.class);
        this.analyticsService = analyticsFactory.getAnalyticsService(namespace);
    }

    @Override
    public MongoDbQueue getProcessorQueue() throws Exception{

        return getMongoDbQueue(namespace, "processor");
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
    public NamespaceService getNamespaceService() throws Exception {

        NamespaceService namespaceService = syncServiceContainer.getBean(NamespaceService.class);
        return namespaceService;
    }

    @Override
    public void destroyFreshIndex() throws Exception{

        getNamespaceService().clearnNamespace(this.namespace);
    }


    public MongoDbList getPublisherList() throws Exception{

        return getMongoDbList(this.namespace, "publisher_list");
    }

    @Override
    public MongoDbKeyValue getKeyValue() throws Exception{

        return getMongoDbKeyValue(this.namespace, "key_value");
    }


    @Override
    public MongoDbList getInfraDbList(String listName) throws Exception{

        return getMongoDbList(this.namespace, listName);
    }

    @Override
    public String getNamespace() throws Exception{
        return this.namespace;
    }

    public void deleteDb() throws Exception{
        mongoClient.getDatabase(this.namespace).drop();

    }

    @Override
    public void destroy() throws Exception{

        persistentQueueSingletonMap.remove(this.namespace);
        persistentListSingletonMap.remove(this.namespace);
        persistentKeyValueSingletonMap.remove(this.namespace);

        // Now drop the persistent database also
        mongoClient.getDatabase(this.namespace).drop();

        // We need to clear the freshIndex as well.
        destroyFreshIndex();
    }

    private MongoDbQueue getMongoDbQueue(String namespace, String queueName) throws Exception{

        try{
                uniqueQueue.lock();
                if(persistentQueueSingletonMap.containsKey(namespace) && persistentQueueSingletonMap.get(namespace).containsKey(queueName)){
                    return persistentQueueSingletonMap.get(namespace).get(queueName);
                }

                else if (persistentQueueSingletonMap.containsKey(namespace) && !persistentQueueSingletonMap.get(namespace).containsKey(queueName)){

                    MongoDbQueue mongoDbQueue = new MongoDbQueue();
                    mongoDbQueue.configure(syncServiceContainer);
                    mongoDbQueue.setQueue(mongoClient.getDatabase(this.namespace).getCollection(queueName));
                    mongoDbQueue.getQueue().createIndex(Indexes.ascending("queue_index"));
                    persistentQueueSingletonMap.get(namespace).put(queueName, mongoDbQueue);
                    analyticsService.meterGauge(queueName + ".size", mongoDbQueue, x -> {
                        try {
                            return x.size();
                        } catch (Exception e) {
                            return -100;
                        }
                    });
                    return mongoDbQueue;
                }

                else{
                    MongoDbQueue mongoDbQueue = new MongoDbQueue();
                    mongoDbQueue.configure(syncServiceContainer);
                    mongoDbQueue.setQueue(mongoClient.getDatabase(this.namespace).getCollection(queueName));
                    mongoDbQueue.getQueue().createIndex(Indexes.ascending("queue_index"));
                    HashMap<String, MongoDbQueue> map = new HashMap<>();
                    map.put(queueName, mongoDbQueue);
                    analyticsService.meterGauge(queueName + ".size", mongoDbQueue, x -> {
                        try {
                            return x.size();
                        } catch (Exception e) {
                            return -100;
                        }
                    });
                    persistentQueueSingletonMap.put(namespace, map);
                    return mongoDbQueue;
                }
        }

        finally {

            uniqueQueue.unlock();
        }

    } // close

    private MongoDbList getMongoDbList(String namespace, String listName) throws Exception{
        // This should return singleton for single listname

        try{
            uniqueList.lock();
            if(persistentListSingletonMap.containsKey(namespace) && persistentListSingletonMap.get(namespace).containsKey(listName)){

                return persistentListSingletonMap.get(namespace).get(listName);
            }
            else if(persistentListSingletonMap.containsKey(namespace) && !persistentListSingletonMap.get(namespace).containsKey(listName)){
                MongoDbList mongoDbList = new MongoDbList();
                mongoDbList.configure(syncServiceContainer);
                mongoDbList.setList(mongoClient.getDatabase(this.namespace).getCollection(listName));
                mongoDbList.getList().createIndex(Indexes.ascending("list_index"));
                persistentListSingletonMap.get(namespace).put(listName, mongoDbList);
                analyticsService.meterGauge(listName + ".size", mongoDbList, x -> {
                    try {
                        return x.size();
                    } catch (Exception e) {
                        return -100;
                    }
                });
                return mongoDbList;
            }
            else{
                MongoDbList mongoDbList = new MongoDbList();
                mongoDbList.configure(syncServiceContainer);
                mongoDbList.setList(mongoClient.getDatabase(this.namespace).getCollection(listName));
                MongoCollection<Document> list = mongoDbList.getList();
                list.createIndex(Indexes.ascending("list_index"));
                HashMap<String, MongoDbList> map = new HashMap<>();
                map.put(listName, mongoDbList);
                persistentListSingletonMap.put(namespace, map);
                analyticsService.meterGauge(listName + ".size", mongoDbList, x -> {
                    try {
                        return x.size();
                    } catch (Exception e) {
                        return -100;
                    }
                });
                return mongoDbList;
            }

        }

        finally {
            uniqueList.unlock();
        }
    }

    private MongoDbKeyValue getMongoDbKeyValue(String namespace, String keyValueName) throws Exception{

        try{

            uniqueKeyValue.lock();
                if(persistentKeyValueSingletonMap.containsKey(namespace) && persistentKeyValueSingletonMap.get(namespace).containsKey(keyValueName)){

                    return persistentKeyValueSingletonMap.get(namespace).get(keyValueName);
                }
                else if (persistentKeyValueSingletonMap.containsKey(namespace) && !persistentKeyValueSingletonMap.get(namespace).containsKey(keyValueName)){
                    MongoDbKeyValue mongoDbKeyValue = new MongoDbKeyValue();
                    mongoDbKeyValue.configure(syncServiceContainer);
                    mongoDbKeyValue.setKeyValue(mongoClient.getDatabase(this.namespace).getCollection(keyValueName));
                    mongoDbKeyValue.getKeyValue().createIndex(Indexes.ascending("key"));
                    persistentKeyValueSingletonMap.get(namespace).put(keyValueName, mongoDbKeyValue);
                    return mongoDbKeyValue;
                }
                else{
                    MongoDbKeyValue mongoDbKeyValue = new MongoDbKeyValue();
                    mongoDbKeyValue.configure(syncServiceContainer);
                    mongoDbKeyValue.setKeyValue(mongoClient.getDatabase(this.namespace).getCollection(keyValueName));
                    mongoDbKeyValue.getKeyValue().createIndex(Indexes.ascending("key"));

                    HashMap<String, MongoDbKeyValue> map = new HashMap<>();
                    map.put(keyValueName, mongoDbKeyValue);
                    persistentKeyValueSingletonMap.put(namespace, map);
                    return mongoDbKeyValue;
                }
        }

        finally {

            uniqueKeyValue.unlock();
        }

    }

}
