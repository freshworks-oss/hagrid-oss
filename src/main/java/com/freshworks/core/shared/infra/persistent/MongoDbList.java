package com.freshworks.core.shared.infra.persistent;

import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.infra.InfraDbList;
import com.mongodb.client.MongoCollection;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;


@Slf4j
@Getter
@Setter

public class MongoDbList implements InfraDbList {

    MongoCollection<Document> list;

    AtomicLong listIndex = new AtomicLong(0);


    private final ReentrantReadWriteLock.WriteLock listAddLock = new ReentrantReadWriteLock().writeLock();

    protected MongoDbList(){
//        this.list = mongoDb.getCollection(list);
//        this.listIndex.set(0);
//        this.list.createIndex(Indexes.ascending("list_index"));
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
            Document document = new Document();
            document.put("value", s);
            document.put("list_index", currentIndex);
            this.list.insertOne(document);
            this.listIndex.incrementAndGet();
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
            Document document = new Document();
            document.put("value", s);
            document.put("list_index", currentIndex);
            this.list.insertOne(document);
            this.listIndex.incrementAndGet();
            return currentIndex;
        }

        finally {

            listAddLock.unlock();
        }
    }

    public List<Long> addAndGetIndexBulk(List<String> sList) throws Exception {

        try{
            listAddLock.lock();

            List<Document> documentArrayList = new ArrayList<>();
            List<Long> documentIds = new ArrayList<>();

            long currentIndex = this.listIndex.get();

            for(String s : sList){
                s = s.replaceAll("\\.", "ENCODE_DOT");
                Document document = new Document();
                document.put("value", s);
                document.put("list_index", currentIndex);
                documentIds.add(currentIndex);
                documentArrayList.add(document);
                currentIndex = currentIndex + 1;
            }

            this.list.insertMany(documentArrayList);
            this.listIndex.addAndGet(documentIds.size());
            return documentIds;
        }

        finally {
            listAddLock.unlock();
        }

    }

    @Override
    public void add(List<String> s) throws Exception{

        ArrayList<Document> documentArrayList = new ArrayList<>();

        // If result set is empty then just return, do not enter into loop
        if(s.isEmpty()){
            return;
        }

        try{
            listAddLock.lock();
            log.debug(" In for loop for MongodbList");
            long currentIndex = this.listIndex.get();
            for(int i=0; i<s.size(); i++){
                String ss = s.get(i).replaceAll("\\.", "ENCODE_DOT");
                Document document = new Document();
                document.put("value", ss);
                document.put("list_index", currentIndex);
                documentArrayList.add(document);
                currentIndex = currentIndex + 1;
            }
            this.list.insertMany(documentArrayList);
            this.listIndex.addAndGet(s.size());
        }

        finally {
            listAddLock.unlock();
        }
    }

    @Override
    public String get(int index) throws Exception {

        Bson bson = new Document("list_index", index);
        Document document = this.list.find(bson).first();
        if(document != null){
            String s = (String)document.get("value");
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
        ArrayList<Long> list = new ArrayList<>();
        for(int i=0; i< n; i++){
            list.add(index);
            index = index + 1;
        }
        Bson bson = new Document("list_index", new Document("$in",list));
        Iterator<Document> it = this.list.find(bson).iterator();

        while (it.hasNext()){
            Document document = it.next();
            String s = (String)document.get("value");
            String ss = s.replaceAll("ENCODE_DOT", "\\.");
            returnList.add(ss);
        }

        return returnList;
    }


    public List<String> get(List<Long> documentIdList) throws Exception {

        ArrayList<String> returnList = new ArrayList<>();

        Bson bson = new Document("list_index", new Document("$in",documentIdList));
        Iterator<Document> it = this.list.find(bson).iterator();

        while (it.hasNext()){
            Document document = it.next();
            String s = (String)document.get("value");
            String ss = s.replaceAll("ENCODE_DOT", "\\.");
            returnList.add(ss);
        }

        return returnList;
    }

    @Override
    public void deRegisterPublisher() throws Exception{

    }


    @Override
    public long size() throws Exception {
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
        this.list.drop();
    }
}
