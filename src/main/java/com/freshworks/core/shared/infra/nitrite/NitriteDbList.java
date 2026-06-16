package com.freshworks.core.shared.infra.nitrite;

import static org.dizitart.no2.filters.FluentFilter.where;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.dizitart.no2.Nitrite;
import org.dizitart.no2.collection.Document;
import org.dizitart.no2.collection.NitriteCollection;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.infra.InfraDbList;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Getter
@Setter

public class NitriteDbList implements InfraDbList {

    ObjectMapper objectMapper = new ObjectMapper();

    String dbString;
    String listName;

    Nitrite nitriteDb;
    NitriteCollection nitriteCollection;

    AtomicLong listIndex = new AtomicLong(0);


    private final ReentrantReadWriteLock.WriteLock listAddLock = new ReentrantReadWriteLock().writeLock();

    protected NitriteDbList(Nitrite nitriteDb, String namespace, String listName) throws Exception {

        this.nitriteDb = nitriteDb;
        this.listName = namespace + "_" + listName;
        this.nitriteCollection = nitriteDb.getCollection(listName);
        this.nitriteCollection.createIndex("list_index");
    }

    @Override
    public void configure(SyncServiceContainer syncServiceContainer) throws Exception{

    }


    @Override
    public void add(String s) throws Exception{

        s = s.replaceAll("\\.", "ENCODE_DOT");

        try{

            listAddLock.lock();
            long currentIndex = this.listIndex.get();
            insert(currentIndex, s);
            listIndex.incrementAndGet();
        }

        finally {
            listAddLock.unlock();
        }
    }

    public Long addAndGetIndex(String s) throws Exception{

        s = s.replaceAll("\\.", "ENCODE_DOT");

        try{
            listAddLock.lock();
            long currentIndex = this.listIndex.get();
            insert(currentIndex, s);
            this.listIndex.incrementAndGet();
            return currentIndex;
        }

        finally {
            listAddLock.unlock();
        }
    }

    @Override
    public List<Long> addAndGetIndexBulk(List<String> s) throws Exception{

        List<Long> documentIds = new ArrayList<>();
        if(s.isEmpty()){
            return  documentIds;
        }

        try{
            listAddLock.lock();
            long currentIndex = this.listIndex.get();
            for(int i=0; i<s.size(); i++){
                String ss = s.get(i).replaceAll("\\.", "ENCODE_DOT");

                insert(currentIndex, ss);
                documentIds.add(currentIndex);
                currentIndex = currentIndex + 1;
            }

            this.listIndex.addAndGet(documentIds.size());
            return documentIds;
        }

        finally {

            listAddLock.unlock();
        }
    }

    @Override
    public void add(List<String> s) throws Exception{

        // If result set is empty then just return, do not enter into loop
        if(s.isEmpty()){
            return;
        }

        try{
            listAddLock.lock();
            long currentIndex = this.listIndex.get();
            for(int i=0; i<s.size(); i++){
                String ss = s.get(i).replaceAll("\\.", "ENCODE_DOT");
                insert(currentIndex, ss);
                currentIndex = currentIndex + 1;
            }

            this.listIndex.addAndGet(s.size());
        }

        finally {
            listAddLock.unlock();
        }
    }

    @Override
    public String get(int index) throws Exception {

        String s = find(index);
        if(s != null){
            return s.replaceAll("ENCODE_DOT", "\\.");
        }
        else{
            return null;
        }
    }


    @Override
    public List<String> get(int start, int n) throws Exception {

        ArrayList<String> returnList = new ArrayList<>();
        long index = start;
        ArrayList<String> list = new ArrayList<>();
        for(int i=0; i< n; i++){

            String s = find(index);
            if(s != null){
                list.add(s);
            }

            index = index + 1;
        }

        Iterator<String> it = list.iterator();

        while (it.hasNext()){
            String s = it.next();
            String ss = s.replaceAll("ENCODE_DOT", "\\.");
            returnList.add(ss);
        }

        return returnList;
    }


    public List<String> get(List<Long> documentIdList) throws Exception {

        ArrayList<String> returnList = new ArrayList<>();
        Iterator<Long> it = documentIdList.iterator();


        while (it.hasNext()){
            long id = it.next();
            String s = find(id);
            String ss = s.replaceAll("ENCODE_DOT", "\\.");
            returnList.add(ss);
        }

        return returnList;
    }

    @Override
    public void deRegisterPublisher() throws Exception{

    }

    @Override
    public long size() {
        return this.listIndex.get();
    }

    @Override
    public Boolean isEndOfListReached(int index) throws Exception{
        if(index < this.listIndex.get()){
            return false;
        }
        else{
            return true;
        }
    }

    @Override
    public void delete() throws Exception{

        try{

            listAddLock.lock();
            // Execute the drop table statement
            this.nitriteCollection.drop();
        }

        finally {
            listAddLock.unlock();
        }
    }


    private void insert(long listIndex, String item) throws Exception{
            
        Map<String, Object> documentMap = new HashMap<>();
        // Check if this item can be converted to MAP i.e json  
        Map<String, Object> map = objectMapper.readValue(item, new TypeReference<Map<String, Object>>() {});
        documentMap.put("list_index", listIndex);
        documentMap.put("value", map);
        Document document = Document.createDocument(documentMap);   
        nitriteCollection.insert(document);
    }

    private String find(long listIndex) throws Exception{

        Document doc = this.nitriteCollection.find(where("list_index").eq(listIndex)).firstOrNull();
        
        if(doc != null){
            Map<String, Object> valueMap = doc.get("value", Map.class);
            return objectMapper.writeValueAsString(valueMap);
        }
        
        else {
            return null;
        }
    }
}
