package com.freshworks.core.shared.infra.nitrite;

import static org.dizitart.no2.filters.FluentFilter.where;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.dizitart.no2.Nitrite;
import org.dizitart.no2.collection.Document;
import org.dizitart.no2.collection.NitriteCollection;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.infra.InfraDbQueue;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j; 

@Slf4j
@Getter
@Setter
public class NitriteDbQueue implements InfraDbQueue {

    int publisherAttached = -100;
    int consumerAttached = -100;

    ObjectMapper objectMapper = new ObjectMapper();

    AtomicLong queueIndex = new AtomicLong(0);

    String dbString;
    String queueName;

    Timer timer;

    Nitrite nitriteDb;
    NitriteCollection nitriteCollection;

    volatile long popIndex ;

    private final ReentrantReadWriteLock.WriteLock queueAddLock = new ReentrantReadWriteLock().writeLock();
    private final ReentrantReadWriteLock.WriteLock queuePollLock = new ReentrantReadWriteLock().writeLock();

    ReentrantLock hasMoreDataLock = new ReentrantLock();
    final Condition hasNotMoreDataQueue = hasMoreDataLock.newCondition();

    protected NitriteDbQueue(Nitrite nitriteDb, String namespace, String queueName)  throws Exception{

        this.nitriteDb = nitriteDb;
        this.queueName = namespace + "_" + queueName;
        this.nitriteCollection = nitriteDb.getCollection(this.queueName);
        this.nitriteCollection.createIndex("queue_index");
    }


    @Override
    public void configure(SyncServiceContainer syncServiceContainer) throws Exception{
        MeterRegistry meterRegistry = syncServiceContainer.getBean(MeterRegistry.class);
        timer = meterRegistry.timer(queueName + ".execution.time");
    }

    @Override
    public void add(String s) throws Exception{

        publisherAttached = 0;
        s = s.replaceAll("\\.", "ENCODE_DOT");
        try{
            queueAddLock.lock();
            long currentIndex = this.queueIndex.get();
            insert(currentIndex, s);
            this.queueIndex.incrementAndGet();
            hasMoreDataLock.lock();
            hasNotMoreDataQueue.signalAll();
            hasMoreDataLock.unlock();
        }

        finally {
            queueAddLock.unlock();
        }
    }


    @Override
    public void add(List<String> s) throws Exception{

        publisherAttached = 0;
        if(s.isEmpty()){
            return;
        }

        try{
            queueAddLock.lock();
            long currentIndex = this.queueIndex.get();
            for(int i=0; i<s.size(); i++){

                String ss = s.get(i).replaceAll("\\.", "ENCODE_DOT");
                insert(currentIndex, ss);
                currentIndex = currentIndex + 1;
            }

            this.queueIndex.addAndGet(s.size());
            hasMoreDataLock.lock();
            hasNotMoreDataQueue.signalAll();
            hasMoreDataLock.unlock();
        }

        finally {
            queueAddLock.unlock();
        }
    }


    @Override
    public String poll() throws Exception{

        consumerAttached = 0;
        try{
            queuePollLock.lock();
            String s = find(popIndex);
            if(s != null){
                this.popIndex = this.popIndex + 1;
                s = s.replaceAll("ENCODE_DOT", "\\.");
                return s;
            }
            else{
                return null;
            }
        }

        finally {
            queuePollLock.unlock();
        }
    }

    @Override
    public List<String> poll(int n) throws Exception{

        consumerAttached = 0;

        try{
            queuePollLock.lock();
            ArrayList<String> returnList = new ArrayList<>();
            long index = this.popIndex;
            ArrayList<String> list = new ArrayList<>();
            for(int i=0; i< n; i++){

                String s = find(index);

                if(s != null){
                    list.add(s);
                }

                index = index + 1;
            }

            Iterator<String> it = list.iterator();

//        Here fetched document could be lesser than int n, hence we need to count the real fetch document and add it to pop index
            while (it.hasNext()){
                String s = it.next();
                s = s.replaceAll("ENCODE_DOT", "\\.");
                returnList.add(s);
                this.popIndex = this.popIndex + 1;
            }

            return returnList;
        }

        finally {
            queuePollLock.unlock();
        }
    }

    @Override
    public boolean hasMoreData() throws Exception{

        try{
            hasMoreDataLock.lock();
            // It means that child so far has consumed less data than parent has fetched already
            if(this.popIndex < this.queueIndex.get()){
                return true;
            }

            // It means, producer is still not de attached from the queue
            else if(publisherAttached != 1){
                hasNotMoreDataQueue.await();
                return true;
            }

            else {
                return false;
            }
        }

        finally {
            hasMoreDataLock.unlock();
        }

    }

    @Override
    public void attachPublisher() throws Exception{

    }

    @Override
    public void removePublisher() throws Exception{

        hasMoreDataLock.lock();
        publisherAttached = 1;
        hasNotMoreDataQueue.signalAll();
        hasMoreDataLock.unlock();
    }

    @Override
    public long size() throws Exception{
        return this.queueIndex.get();
    }

    @Override
    public Boolean isEmpty() throws Exception{
        if(this.popIndex >= this.queueIndex.get()){
            return true;
        }
        else{
            return false;
        }
    }

    @Override
    public void delete() throws Exception{

        try {

            queueAddLock.lock();
            this.nitriteCollection.drop();
        }

        finally {
            queueAddLock.unlock();
        }
    }


    private void insert(long queueIndex, String item) throws Exception{

        try{
            
            Map<String, Object> documentMap = new HashMap<>();
            // Check if this item can be converted to MAP i.e json  
            Map<String, Object> map = objectMapper.readValue(item, new TypeReference<Map<String, Object>>() {});
            documentMap.put("queue_index", queueIndex);
            documentMap.put("value", map);
            Document document = Document.createDocument(documentMap);   
            nitriteCollection.insert(document);
        }   

        catch(JsonParseException e){
            
            Document document = Document.createDocument();
            document.put("queue_index", queueIndex);
            document.put("value", item);  
            nitriteCollection.insert(document);
        }
    }

    private String  find(long queueIndex) throws Exception{

        Document doc = this.nitriteCollection.find(where("queue_index").eq(queueIndex)).firstOrNull();

        if(doc != null){
            Map<String, Object> valueMap = doc.get("value", Map.class);
            return objectMapper.writeValueAsString(valueMap);
        }
        else {
            return null;
        }
    }

}
