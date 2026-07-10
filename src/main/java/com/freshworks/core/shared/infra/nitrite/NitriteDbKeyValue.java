package com.freshworks.core.shared.infra.nitrite;

import static org.dizitart.no2.filters.FluentFilter.where;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import org.dizitart.no2.Nitrite;
import org.dizitart.no2.collection.Document;
import org.dizitart.no2.collection.DocumentCursor;
import org.dizitart.no2.collection.FindPlan;
import org.dizitart.no2.collection.NitriteCollection;
import org.dizitart.no2.common.WriteResult;
import org.dizitart.no2.index.IndexOptions;
import org.dizitart.no2.index.IndexType;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshworks.core.shared.NamespaceService;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.analytics.AnalyticsFactory;
import com.freshworks.core.shared.analytics.AnalyticsService;
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
    AnalyticsFactory analyticsFactory;
    AnalyticsService analyticsService;

    AtomicLong keyListSize = new AtomicLong(0);

    ObjectMapper objectMapper = new ObjectMapper();
    NitriteCollection nitriteCollection;


    private final ReentrantReadWriteLock.WriteLock keyAddLock = new ReentrantReadWriteLock().writeLock();

    protected NitriteDbKeyValue(Nitrite nitriteDb, String namespace, String keyValueName)  throws Exception{

        this.nitriteDb = nitriteDb;
        this.keyValueName = namespace + "_" + keyValueName;
        this.nitriteCollection = nitriteDb.getCollection(this.keyValueName);
        IndexOptions options = new IndexOptions();
        options.setIndexType(IndexType.NON_UNIQUE);
        this.nitriteCollection.createIndex(options,"key");

    }

    @Override
    public void configure(SyncServiceContainer syncServiceContainer) throws Exception{

        NamespaceService namespace = syncServiceContainer.getBean(NamespaceService.class);
        AnalyticsFactory analyticsFactory = syncServiceContainer.getBean(AnalyticsFactory.class);
        analyticsService = analyticsFactory.getAnalyticsService(namespace.getNamespace());

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
        return find(key).get(0);
    }

    @Override
    public void putList(String key, List<String> value) throws Exception{

        try{
            keyAddLock.lock();
            key = key.replaceAll("\\.", "ENCODE_DOT");

            for(int i = 0; i < value.size(); i++){
                String v = value.get(i).replaceAll("\\.", "ENCODE_DOT");
                insert(key, v);
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
            insert(key, value);
            
        }
        finally {
            keyAddLock.unlock();
        }
    }

    @Override
    public List<String> getList(String key) throws Exception{
        key = key.replaceAll("\\.", "ENCODE_DOT");
        ArrayList<String> returnResult = new ArrayList<>();
        List<String> sList = find(key);
        for(int i = 0; i < sList.size(); i++){
            returnResult.add(sList.get(i).replaceAll("ENCODE_DOT", "\\."));
        }

        return returnResult;
    }


    @Override
    public long size() {

        return keyListSize.get();
    }

    @Override
    public void delete() throws Exception{

        try {

            keyAddLock.lock();
        
            if (!isDatabaseOpen()){
                throw new IllegalStateException("Nitrite DB is closed and insert operation has been asked to perform in the key value");
            }

            // Execute the drop table statement
            this.nitriteCollection.drop();

        }
        finally {
            keyAddLock.unlock();
        }
    }

    private void insert(String key, String value) throws Exception{

        if (!isDatabaseOpen()){
            throw new IllegalStateException("Nitrite DB is closed and insert operation has been asked to perform in the key value");
        }

        Map<String, Object> valueMap = objectMapper.readValue(value, new TypeReference<HashMap<String, Object>>() {});

        Map<String, Object> documentMap = new HashMap<>();        
        documentMap.put("key", key);
        documentMap.put("value", valueMap);
        Document document = Document.createDocument(documentMap);

        nitriteCollection.insert(document);

        keyListSize.incrementAndGet();    
    }


    private List<String> find(String key) throws Exception{

        if (!isDatabaseOpen()){
            throw new IllegalStateException("Nitrite DB is closed and find operation has been asked to perform in the key value");
        }

        DocumentCursor cursor = this.nitriteCollection.find(where("key").eq(key));
        FindPlan plan = cursor.getFindPlan();
        
        if (plan.getIndexScanFilter() != null) {
           analyticsService.debugLogEvent("NITRITE_DB_KEYVALUE","_message","SUCCESS: Index is being USED!", "targeted_fields", plan.getIndexDescriptor().getFields());
        } 

        // 2. Is it falling back to a full collection scan?
        if (plan.getCollectionScanFilter() != null) {
            analyticsService.errorLogEvent("NITRITE_DB_KEYVALUE","_message","FAILURE: Index is being USED!. It is table scan being performed", "targeted_fields", "");
        }

        List<Map<String, Object>> valueList = new ArrayList();
        
        for(Document doc : cursor){

            Map<String, Object> valueMap = (Map<String, Object>) doc.get("value");
            valueList.add(valueMap);
        }


        List<String> valueStringList = new ArrayList<>();
        for(Map<String, Object> map : valueList){
            valueStringList.add(objectMapper.writeValueAsString(map));
        }
        return valueStringList;

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
