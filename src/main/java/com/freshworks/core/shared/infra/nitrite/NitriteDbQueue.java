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
import org.dizitart.no2.collection.DocumentCursor;
import org.dizitart.no2.collection.FindOptions;
import org.dizitart.no2.collection.FindPlan;
import org.dizitart.no2.collection.NitriteCollection;
import org.dizitart.no2.index.IndexOptions;
import org.dizitart.no2.index.IndexType;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshworks.core.shared.NamespaceService;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.analytics.AnalyticsFactory;
import com.freshworks.core.shared.analytics.AnalyticsService;
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

    // -100 means they are not yet attached i.e no message is published nor consumed
    // 0 means they are attached i.e. in progress 
    // 1 means they are done i.e publisher is done publishing and consumer is done consuming.  

    int publisherAttached = -100;
    int consumerAttached = -100;

    ObjectMapper objectMapper = new ObjectMapper();

    AtomicLong queueIndex = new AtomicLong(0);

    AnalyticsFactory analyticsFactory;
    AnalyticsService analyticsService;

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
        this.nitriteCollection.createIndex(IndexOptions.indexOptions(IndexType.UNIQUE),"queue_index");
    }


    @Override
    public void configure(SyncServiceContainer syncServiceContainer) throws Exception{
        MeterRegistry meterRegistry = syncServiceContainer.getBean(MeterRegistry.class);
        timer = meterRegistry.timer(queueName + ".execution.time");
        NamespaceService namespace = syncServiceContainer.getBean(NamespaceService.class);
        AnalyticsFactory analyticsFactory = syncServiceContainer.getBean(AnalyticsFactory.class);
        analyticsService = analyticsFactory.getAnalyticsService(namespace.getNamespace());
    }

    @Override
    public void add(String s) throws Exception{

        // As soon as single message is published, publisher marked as attached
        publisherAttached = 0;
        s = s.replaceAll("\\.", "ENCODE_DOT");
        try{
            queueAddLock.lock();
            long currentIndex = this.queueIndex.get();
            insert(currentIndex, s);
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

        // As soon as single message is published, publisher marked as attached
        publisherAttached = 0;


        if(s.isEmpty()){
            return;
        }

        try{
            queueAddLock.lock();
            for(int i=0; i<s.size(); i++){
                long currentIndex = this.queueIndex.get();
                String ss = s.get(i).replaceAll("\\.", "ENCODE_DOT");
                insert(currentIndex, ss);
            }

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

        // As soon as single message is consumed, consumer marked as attached
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

        // As soon as single message is consumed, consumer marked as attached
        consumerAttached = 0;

        try{
            queuePollLock.lock();
            ArrayList<String> returnList = new ArrayList<>();
            List<String> list = find(this.popIndex, n);

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

            if(!isDatabaseOpen()){

                throw new IllegalStateException("Nitrite DB is closed and drop db operation has been asked to perform in the queue");
            }

            this.nitriteCollection.drop();
        }

        finally {
            queueAddLock.unlock();
        }
    }


    private void insert(long queueIndex, String item) throws Exception{

        if (!isDatabaseOpen()){
            throw new IllegalStateException("Nitrite DB is closed and insert operation has been asked to perform in the queue");
        }
            
        Map<String, Object> documentMap = new HashMap<>();
        // Check if this item can be converted to MAP i.e json 
        Map<String, Object> map = objectMapper.readValue(item, new TypeReference<HashMap<String, Object>>() {});
        documentMap.put("queue_index", queueIndex);
        documentMap.put("value", map);
        Document document = Document.createDocument(documentMap);   
        nitriteCollection.insert(document);
        
        this.queueIndex.incrementAndGet();
    }

    private String  find(long queueIndex) throws Exception{

        if (!isDatabaseOpen()){
            throw new IllegalStateException("Nitrite DB is closed and find operation has been asked to perform in the queue");
        }

        DocumentCursor cursor = this.nitriteCollection.find(where("queue_index").eq(queueIndex));

        FindPlan plan = cursor.getFindPlan();
        
        if (plan.getIndexScanFilter() != null) {
           analyticsService.debugLogEvent("NITRITE_DB_QUEUE","_message","SUCCESS: Index is being USED!", "targeted_fields", plan.getIndexDescriptor().getFields());
        } 

        // 2. Is it falling back to a full collection scan?
        if (plan.getCollectionScanFilter() != null) {
            analyticsService.errorLogEvent("NITRITE_DB_QUEUE","_message","FAILURE: Index is NOT being USED!. It is table scan being performed", "targeted_fields", "");
        }


        if(cursor.size() > 1){

            analyticsService.errorLogEvent("NITRITE_DB_QUEUE","_message","Item at queue index " + queueIndex + " are most than 1. It should not be the case");
            throw new IllegalStateException("Number of items at queue index " + queueIndex + " are most than 1. It should not be the case");
        }

        Document doc = cursor.firstOrNull();

        if(doc != null){
            Map<String, Object> valueMap = doc.get("value", Map.class);
            return objectMapper.writeValueAsString(valueMap);
        }
        else {
            return null;
        }
    }

    private List<String>  find(long start, long number) throws Exception{

        List<String> foundDocuments = new ArrayList<>();

        if (!isDatabaseOpen()){
            throw new IllegalStateException("Nitrite DB is closed and find operation has been asked to perform in the queue");
        }

        FindOptions findOptions = new FindOptions();
        findOptions.limit(number);
        DocumentCursor cursor = this.nitriteCollection.find(where("queue_index").gte(start), findOptions);

        FindPlan plan = cursor.getFindPlan();
        
        if (plan.getIndexScanFilter() != null) {
           analyticsService.debugLogEvent("NITRITE_DB_QUEUE","_message","SUCCESS: Index is being USED!", "targeted_fields", plan.getIndexDescriptor().getFields());
        } 

        // 2. Is it falling back to a full collection scan?
        if (plan.getCollectionScanFilter() != null) {
            analyticsService.errorLogEvent("NITRITE_DB_QUEUE","_message","FAILURE: Index is NOT being USED!. It is table scan being performed", "targeted_fields", "");
        }

        for(Document doc: cursor){
            Map<String, Object> valueMap = doc.get("value", Map.class);
            String docString =  objectMapper.writeValueAsString(valueMap);
            foundDocuments.add(docString);
        }

        return foundDocuments;
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
