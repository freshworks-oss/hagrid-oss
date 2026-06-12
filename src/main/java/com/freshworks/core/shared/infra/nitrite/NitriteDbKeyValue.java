package com.freshworks.core.shared.infra.nitrite;

import static org.dizitart.no2.filters.FluentFilter.where;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.dizitart.no2.Nitrite;
import org.dizitart.no2.collection.Document;
import org.dizitart.no2.collection.NitriteCollection;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.infra.InfraDbKeyValue;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter

public class NitriteDbKeyValue implements InfraDbKeyValue {


    String keyValueName;
    Nitrite nitriteDb;
    ObjectMapper objectMapper = new ObjectMapper();
    NitriteCollection nitriteCollection;


    private final ReentrantReadWriteLock.WriteLock keyAddLock = new ReentrantReadWriteLock().writeLock();

    protected NitriteDbKeyValue(Nitrite nitriteDb, String namespace, String keyValueName)  throws Exception{

        this.nitriteDb = nitriteDb;
        this.keyValueName = namespace + "_" + keyValueName;
        this.nitriteCollection = nitriteDb.getCollection(keyValueName);
        this.nitriteCollection.createIndex("key");

    }

    @Override
    public void configure(SyncServiceContainer syncServiceContainer) throws Exception{

    }

    @Override
    public void put(String key, String value) throws Exception{

        try{
            keyAddLock.lock();
            key = key.replaceAll("\\.", "ENCODE_DOT");
            value = value.replaceAll("\\.", "ENCODE_DOT");

            insert(key, value);
        }

        finally {

            keyAddLock.unlock();
        }
    }

    @Override
    public String get(String key) throws Exception{

        key = key.replaceAll("\\.", "ENCODE_DOT");
        return find(key);
    }

    @Override
    public void putList(String key, List<String> value) throws Exception{

        try{
            keyAddLock.lock();
            key = key.replaceAll("\\.", "ENCODE_DOT");

            String s = find(key);
            if(s != null){

                ArrayNode jsonNode = (ArrayNode) objectMapper.readTree(s);

                for(int i = 0; i < value.size(); i++){
                    String v = value.get(i).replaceAll("\\.", "ENCODE_DOT");
                    JsonNode x = objectMapper.readTree(v);
                    jsonNode.add(x);
                }

                insert(key, objectMapper.writeValueAsString(jsonNode));
            }
            else{
                ArrayNode arrayNode = objectMapper.createArrayNode();

                for(int i = 0; i < value.size(); i++){

                    String v = value.get(i).replaceAll("\\.", "ENCODE_DOT");
                    JsonNode x = objectMapper.readTree(v);
                    arrayNode.add(x);
                }

                insert(key, objectMapper.writeValueAsString(arrayNode));
            }
        }

        finally {

            keyAddLock.unlock();
        }
    }

    @Override
    public void putList(String key, String value) throws Exception{

        try{

            keyAddLock.lock();
            key = key.replaceAll("\\.", "ENCODE_DOT");
            value = value.replaceAll("\\.", "ENCODE_DOT");

            String s = find(key);
            if(s != null){

                ArrayNode jsonNode = (ArrayNode) objectMapper.readTree(s);
                JsonNode x = objectMapper.readTree(value);
                jsonNode.add(x);
                insert(key, objectMapper.writeValueAsString(jsonNode));
            }
            else{
                ArrayNode arrayNode = objectMapper.createArrayNode();
                JsonNode x = objectMapper.readTree(value);
                arrayNode.add(x);
                insert(key, objectMapper.writeValueAsString(arrayNode));
            }
        }
        finally {
            keyAddLock.unlock();
        }
    }

    @Override
    public List<String> getList(String key) throws Exception{
        key = key.replaceAll("\\.", "ENCODE_DOT");
        ArrayList<String> returnResult = new ArrayList<>();
        String s = find(key);
        JsonNode j = objectMapper.readTree(s);

        for(int i = 0; i < j.size(); i++){
            String ss = j.get(i).asText();
            returnResult.add(ss.replaceAll("ENCODE_DOT", "\\."));
        }
        return returnResult;
    }

    @Override
    public void delete() throws Exception{

        try {

            keyAddLock.lock();
            // Execute the drop table statement
            this.nitriteCollection.drop();

        }
        finally {
            keyAddLock.unlock();
        }
    }


    private void insert(String key, String value) throws Exception{

        try{    
            // Check if this item can be converted to MAP i.e json  
            Map<String, Object> map = objectMapper.readValue(value, new TypeReference<Map<String, Object>>() {});
            map.put("key", key);
            Document document = Document.createDocument(map);   
            nitriteCollection.insert(document);
        }   

        catch(JsonParseException e){
            
            Document document = Document.createDocument();
            document.put("key", key);
            document.put("value", value);  
            nitriteCollection.insert(document);
        }

    }

    private String  find(String key) throws Exception{

        Document doc = this.nitriteCollection.find(where("key").eq(key)).firstOrNull();

        if(doc != null){
            return objectMapper.writeValueAsString(doc);
        }
        else {
            return null;
        }
    }
}
