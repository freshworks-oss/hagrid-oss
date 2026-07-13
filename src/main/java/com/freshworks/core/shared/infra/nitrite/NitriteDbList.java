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
import org.dizitart.no2.collection.DocumentCursor;
import org.dizitart.no2.collection.FindOptions;
import org.dizitart.no2.collection.FindPlan;
import org.dizitart.no2.collection.NitriteCollection;
import org.dizitart.no2.filters.FluentFilter;
import org.dizitart.no2.filters.NitriteFilter;
import org.dizitart.no2.index.IndexOptions;
import org.dizitart.no2.index.IndexType;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshworks.core.processor.AbstractAsset;
import com.freshworks.core.shared.NamespaceService;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.analytics.AnalyticsFactory;
import com.freshworks.core.shared.analytics.AnalyticsService;
import com.freshworks.core.shared.infra.InfraDbCursorResponse;
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

    AnalyticsFactory analyticsFactory;
    AnalyticsService analyticsService;

    Nitrite nitriteDb;
    NitriteCollection nitriteCollection;

    AtomicLong listIndex = new AtomicLong(0);


    private final ReentrantReadWriteLock.WriteLock listAddLock = new ReentrantReadWriteLock().writeLock();

    protected NitriteDbList(Nitrite nitriteDb, String namespace, String listName) throws Exception {

        this.nitriteDb = nitriteDb;
        this.listName = namespace + "_" + listName;
        this.nitriteCollection = nitriteDb.getCollection(this.listName);
        this.nitriteCollection.createIndex(IndexOptions.indexOptions(IndexType.UNIQUE),"list_index");
    }

    @Override
    public void configure(SyncServiceContainer syncServiceContainer) throws Exception{

        NamespaceService namespace = syncServiceContainer.getBean(NamespaceService.class);
        AnalyticsFactory analyticsFactory = syncServiceContainer.getBean(AnalyticsFactory.class);
        analyticsService = analyticsFactory.getAnalyticsService(namespace.getNamespace());
    }


    @Override
    public void add(String s) throws Exception{

        s = s.replaceAll("\\.", "ENCODE_DOT");

        try{

            listAddLock.lock();
            long currentIndex = this.listIndex.get();
            insert(currentIndex, s);
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
            return currentIndex;
        }

        finally {
            listAddLock.unlock();
        }
    }

    @Override
    public Long addBulk(List<String> s) throws Exception{

        List<Long> documentIds = new ArrayList<>();
        if(s.isEmpty()){
            return  0L;
        }

        try{
            listAddLock.lock();
            for(int i=0; i<s.size(); i++){
                long currentIndex = this.listIndex.get();
                String ss = s.get(i).replaceAll("\\.", "ENCODE_DOT");
                insert(currentIndex, ss);
                documentIds.add(currentIndex);
            }
            return Long.valueOf(documentIds.size());
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
            for(int i=0; i<s.size(); i++){
                long currentIndex = this.listIndex.get();
                String ss = s.get(i).replaceAll("\\.", "ENCODE_DOT");
                insert(currentIndex, ss);
            }
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
        List<String> list = find(start, n);
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

            if(!isDatabaseOpen()){

                throw new IllegalStateException("Nitrite DB is closed and drop db operation has been asked to perform in the list");
            }

            this.nitriteCollection.drop();
        }

        finally {
            listAddLock.unlock();
        }
    }


    

    private void insert(long listIndex, String item) throws Exception{
        
        
        if (!isDatabaseOpen()){
            throw new IllegalStateException("Nitrite DB is closed and insert operation has been asked to perform in the list");
        }

        
        Map<String, Object> documentMap = new HashMap<>();
        // Check if this item can be converted to MAP i.e json  
        Map<String, Object> map = objectMapper.readValue(item, new TypeReference<HashMap<String, Object>>() {});
        documentMap.put("list_index", listIndex);
        documentMap.put("value", map);
        Document document = Document.createDocument(documentMap);   
        nitriteCollection.insert(document);
        
        this.listIndex.incrementAndGet();

    }

    private String find(long listIndex) throws Exception{

        if (!isDatabaseOpen()){
            throw new IllegalStateException("Nitrite DB is closed and find operation has been asked to perform in the list");
        }

        DocumentCursor cursor = this.nitriteCollection.find(where("list_index").eq(listIndex));

        FindPlan plan = cursor.getFindPlan();
        
        if (plan.getIndexScanFilter() != null) {
           analyticsService.debugLogEvent("NITRITE_DB_LIST","_message","SUCCESS: Index is being USED!", "targeted_fields", plan.getIndexDescriptor().getFields());
        } 

        // 2. Is it falling back to a full collection scan?
        if (plan.getCollectionScanFilter() != null) {
            analyticsService.errorLogEvent("NITRITE_DB_LIST","_message","FAILURE: Index is NOT being USED!. It is table scan being performed", "targeted_fields", "");
        }


        if(cursor.size() > 1){

            analyticsService.errorLogEvent("NITRITE_DB_LIST","_message","Item at list index " + listIndex + " are most than 1. It should not be the case");
            throw new IllegalStateException("Number of items at list index " + listIndex + " are most than 1. It should not be the case");
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

    private List<String>  find(long start, long limit) throws Exception{

        List<String> foundDocuments = new ArrayList<>();

        if (!isDatabaseOpen()){
            throw new IllegalStateException("Nitrite DB is closed and find operation has been asked to perform in the list");
        }

        FindOptions findOptions = new FindOptions();
        findOptions.limit(limit);
        DocumentCursor cursor = this.nitriteCollection.find(where("list_index").gte(start), findOptions);

        FindPlan plan = cursor.getFindPlan();
        
        if (plan.getIndexScanFilter() != null) {
           analyticsService.debugLogEvent("NITRITE_DB_LIST","_message","SUCCESS: Index is being USED!", "targeted_fields", plan.getIndexDescriptor().getFields());
        } 

        // 2. Is it falling back to a full collection scan?
        if (plan.getCollectionScanFilter() != null) {
            analyticsService.errorLogEvent("NITRITE_DB_LIST","_message","FAILURE: Index is being USED!. It is table scan being performed", "targeted_fields", "");
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

    @Override
    public InfraDbCursorResponse filter(NitriteFilter filter) throws Exception {
        
        FindOptions findOptions = new FindOptions();
        DocumentCursor documentCursor;
        if (filter != null){
            documentCursor = this.nitriteCollection.find(filter, findOptions);
        }
        else{
            documentCursor = this.nitriteCollection.find();
        }
        
        NitriteCursorResponse nitriteCursorResponse = new NitriteCursorResponse(documentCursor);
        return nitriteCursorResponse;
    }

    @Override
    public void removePublisher() throws Exception {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'removePublisher'");
    }
}
