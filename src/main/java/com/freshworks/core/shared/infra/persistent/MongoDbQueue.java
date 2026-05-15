package com.freshworks.core.shared.infra.persistent;

import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.infra.InfraDbQueue;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Slf4j
@Getter
@Setter
public class MongoDbQueue implements InfraDbQueue {

    int publisherAttached = -100;
    int consumerAttached = -100;

    Timer timer;

    MongoCollection<Document> queue;

    AtomicLong queueIndex = new AtomicLong(0);
    volatile long popIndex ;

    private final ReentrantReadWriteLock.WriteLock queueAddLock = new ReentrantReadWriteLock().writeLock();
    private final ReentrantReadWriteLock.WriteLock queuePollLock = new ReentrantReadWriteLock().writeLock();

    ReentrantLock hasMoreDataLock = new ReentrantLock();
    final Condition hasNotMoreDataQueue = hasMoreDataLock.newCondition();

    protected MongoDbQueue(){
//        this.queue = mongoDb.getCollection(queueName);
//        this.queue.createIndex(Indexes.ascending("queue_index"));
    }


    @Override
    public void configure(SyncServiceContainer syncServiceContainer) throws Exception{
        MeterRegistry meterRegistry = syncServiceContainer.getBean(MeterRegistry.class);
        timer = meterRegistry.timer(queue + ".execution.time");
    }

    @Override
    public void add(String s) throws Exception{

        publisherAttached = 0;
        s = s.replaceAll("\\.", "ENCODE_DOT");

        try{
            queueAddLock.lock();
            long currentIndex = queueIndex.get();
            Document document = new Document();
            document.put("value", Document.parse(s));
            document.put("queue_index", currentIndex);
            this.queue.insertOne(document);
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

        // producer is still attached
        publisherAttached = 0;

        List<Document> documentArrayList = new ArrayList<>();

        // If result set is empty then just return, do not enter into loop
        if(s.isEmpty()){
            return;
        }

        try{
            queueAddLock.lock();
            long currentIndex = this.queueIndex.get();
            for(int i=0; i<s.size(); i++){
                String ss = s.get(i).replaceAll("\\.", "ENCODE_DOT");
                Document document = new Document();
                document.put("value", Document.parse(ss));
                document.put("queue_index", currentIndex);
                currentIndex = currentIndex + 1;
                documentArrayList.add(document);
            }

            this.queue.insertMany(documentArrayList);
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
            Bson bson = new Document("queue_index", popIndex);
            Document document = this.queue.find(bson).first();
            if(document != null){
                document = (Document) document.get("value");
                String s = String.valueOf(document.toJson());
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
            ArrayList<Long> list = new ArrayList<>();
            for(int i=0; i< n; i++){
                list.add(index);
                index = index + 1;
            }
            Bson bson = new Document("queue_index", new Document("$in",list));


//        Here fetched document could be lesser than int n, hence we need to count the real fetch document and add it to pop index

            try(MongoCursor<Document> it = this.queue.find(bson).iterator()){
                while (it.hasNext()){
                    Document document = it.next();
                    document = (Document) document.get("value");
                    String s = String.valueOf(document.toJson());
                    s = s.replaceAll("ENCODE_DOT", "\\.");
                    returnList.add(s);
                    this.popIndex = this.popIndex + 1;
                }
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
    public long size() throws Exception {
        return this.queueIndex.get();
    }

    @Override
    public Boolean isEmpty() throws Exception {
        if(this.popIndex >= this.queueIndex.get()){
            return true;
        }
        else{
            return false;
        }
    }

    @Override
    public void delete() throws Exception{
        this.queue.drop();
    }

    public void reset() throws Exception{
        this.popIndex = 0;
        this.queueIndex.set(this.queue.countDocuments());
    }
}