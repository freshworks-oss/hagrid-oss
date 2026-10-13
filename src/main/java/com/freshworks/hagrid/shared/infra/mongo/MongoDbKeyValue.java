package com.freshworks.hagrid.shared.infra.mongo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshworks.hagrid.shared.SyncServiceContainer;
import com.freshworks.hagrid.shared.infra.InfraDbKeyValue;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Slf4j
@Getter
@Setter

public class MongoDbKeyValue implements InfraDbKeyValue {

    MongoCollection<Document> keyValue;

    ObjectMapper objectMapper = new ObjectMapper();

    private final ReentrantReadWriteLock.WriteLock keyAddLock = new ReentrantReadWriteLock().writeLock();

    protected MongoDbKeyValue(){
//        this.keyValue = mongoDb.getCollection(keyValueName);
//        keyValue.createIndex(Indexes.ascending("key"));
    }
    @Override
    public void configure(SyncServiceContainer syncServiceContainer) throws Exception{

    }

    @Override
    public void set(String key, String value) throws Exception{

        try{

            keyAddLock.lock();

            key = key.replaceAll("\\.", "ENCODE_DOT");
            value = value.replaceAll("\\.", "ENCODE_DOT");

            Bson filter = Filters.eq("key", key);
            Bson update = new Document("$set", new Document().append("value", Document.parse(value)));
            UpdateOptions updateOptions = new UpdateOptions().upsert(true);
            this.keyValue.updateOne(filter, update, updateOptions);
        }

        finally{
            keyAddLock.unlock();
        }
    }

    @Override
    public String get(String key) throws Exception{

        key = key.replaceAll("\\.", "ENCODE_DOT");
        Bson filter = Filters.eq("key", key);
        Document found = this.keyValue.find(filter).first();
        if(found == null){
            return null;
        }
        return objectMapper.writeValueAsString(found.get("value")).replaceAll("ENCODE_DOT", "\\.");
    }

    @Override
    public void putList(String key, List<String> value) throws Exception{

        try{

            keyAddLock.lock();

            key = key.replaceAll("\\.", "ENCODE_DOT");
            Bson filter = Filters.eq("key", key);

            List<Document> encodedValue = new ArrayList<>();
            for(int i = 0; i < value.size(); i++){
                encodedValue.add(Document.parse(value.get(i).replaceAll("\\.", "ENCODE_DOT")));
            }

            Bson update = new Document("$push", new Document().append("value", new Document().append("$each", encodedValue)));

            UpdateOptions updateOptions = new UpdateOptions().upsert(true);

            this.keyValue.updateOne(filter, update, updateOptions);
        }

        finally{

            keyAddLock.unlock();

        }
    }

    @Override
    public void putList(String key, String value) throws Exception{

        try{

            keyAddLock.lock();

            key = key.replaceAll("\\.", "ENCODE_DOT");
            value = value.replaceAll("\\.", "ENCODE_DOT");

            Bson filter = Filters.eq("key", key);

            Bson update = new Document("$push", new Document().append("value", Document.parse(value)));

            UpdateOptions updateOptions = new UpdateOptions().upsert(true);

            this.keyValue.updateOne(filter, update, updateOptions);
        }

        finally{

            keyAddLock.unlock();
        }

    }

    @Override
    public List<String> getList(String key) throws Exception{

        key = key.replaceAll("\\.", "ENCODE_DOT");

        List<String> result = new ArrayList<>();
        Document found = this.keyValue.find(Filters.eq("key", key)).first();
        if(found == null){
            return result;
        }

        for(Document item : found.getList("value", Document.class)){
            result.add(objectMapper.writeValueAsString(item).replaceAll("ENCODE_DOT", "\\."));
        }
        return result;
    }

    @Override
    public void delete() throws Exception{

        try{
            keyAddLock.lock();
            this.keyValue.drop();
        }

        finally{

            keyAddLock.unlock();
        }   
    }

    @Override
    public long size() {

        return this.keyValue.countDocuments();
    }
}