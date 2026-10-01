package com.freshworks.hagrid.shared.infra.mongo;

import com.freshworks.hagrid.processor.AbstractAsset;
import com.freshworks.hagrid.shared.NamespaceService;
import com.freshworks.hagrid.shared.SyncServiceContainer;
import com.freshworks.hagrid.shared.analytics.AnalyticsFactory;
import com.freshworks.hagrid.shared.analytics.AnalyticsService;
import com.freshworks.hagrid.shared.infra.InfraDbList;
import com.freshworks.hagrid.shared.infra.nitrite.NitriteDbCursor;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.result.InsertManyResult;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.dizitart.no2.collection.DocumentCursor;
import org.dizitart.no2.filters.Filter;
import org.dizitart.no2.filters.NitriteFilter;
import org.springframework.expression.spel.SpelNode;
import org.springframework.expression.spel.ast.IntLiteral;
import org.springframework.expression.spel.ast.OpAnd;
import org.springframework.expression.spel.ast.OpEQ;
import org.springframework.expression.spel.ast.OpNE;
import org.springframework.expression.spel.ast.OpOr;
import org.springframework.expression.spel.ast.StringLiteral;
import org.springframework.expression.spel.standard.SpelExpression;

import static org.dizitart.no2.filters.FluentFilter.where;

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

    AnalyticsFactory analyticsFactory;
    AnalyticsService analyticsService;

    private final ReentrantReadWriteLock.WriteLock listAddLock = new ReentrantReadWriteLock().writeLock();

    protected MongoDbList(){
//        this.list = mongoDb.getCollection(list);
//        this.listIndex.set(0);
//        this.list.createIndex(Indexes.ascending("list_index"));
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
            Document document = new Document();
            document.put("value", Document.parse(s));
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
            document.put("value", Document.parse(s));
            document.put("list_index", currentIndex);
            this.list.insertOne(document);
            this.listIndex.incrementAndGet();
            return currentIndex;
        }

        finally {

            listAddLock.unlock();
        }
    }

    public Long addBulk(List<String> sList) throws Exception {

        try{
            listAddLock.lock();

            List<Document> documentArrayList = new ArrayList<>();
            List<Long> documentIds = new ArrayList<>();

            long currentIndex = this.listIndex.get();

            for(String s : sList){
                s = s.replaceAll("\\.", "ENCODE_DOT");
                Document document = new Document();
                document.put("value", Document.parse(s));
                document.put("list_index", currentIndex);
                documentIds.add(currentIndex);
                documentArrayList.add(document);
                currentIndex = currentIndex + 1;
            }

            InsertManyResult insertManyResult = this.list.insertMany(documentArrayList);

            if(insertManyResult.getInsertedIds().size() != documentIds.size()){

                analyticsService.errorLogEvent("HAGRID_MONGO_LIST", "_message", "Number of documents to be inserted are not equal to the number of items actually inserted", "expected_insertion_count", documentIds.size(), "actual_insertion_count" , insertManyResult.getInsertedIds().size());
            }


            this.listIndex.addAndGet(documentIds.size());
            return (long) documentIds.size();
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
            long currentIndex = this.listIndex.get();
            for(int i=0; i<s.size(); i++){
                String ss = s.get(i).replaceAll("\\.", "ENCODE_DOT");
                Document document = new Document();
                document.put("value", Document.parse(ss));
                document.put("list_index", currentIndex);
                documentArrayList.add(document);
                currentIndex = currentIndex + 1;
            }
            InsertManyResult insertManyResult = this.list.insertMany(documentArrayList);


            if(insertManyResult.getInsertedIds().size() != documentArrayList.size()){

                analyticsService.errorLogEvent("HAGRID_MONGO_LIST", "_message", "Number of documents to be inserted are not equal to the number of items actually inserted", "expected_insertion_count", documentArrayList.size(), "actual_insertion_count" , insertManyResult.getInsertedIds().size());
            }

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

        try{

            listAddLock.lock();
            this.list.drop();
        }

        finally{

            listAddLock.unlock();
        }   
    }

    @Override
    public <T extends AbstractAsset> MongoDbCursor filterAsset(Class<T> assetClassType, SpelExpression spelExpression) throws Exception {
        
        
        if(assetClassType == null ){

            throw new IllegalArgumentException("asset class type can not be null. Consumer can consume asset by asset type only");
        }


        MongoCursor<Document> documentCursor;
        Long docSize = 0L;

        if(spelExpression != null){

            String className = assetClassType.getName();
            className = className.replaceAll("\\.", "ENCODE_DOT");
            Bson  mainFilter = Filters.eq("value.clazz", className);
            Bson developerFilter = spelToMongoFilter(spelExpression.getAST());

            System.out.println("Custom filter is");
            System.out.println(developerFilter.toString());

            Bson finalFilter = Filters.and(mainFilter, developerFilter);
            System.out.println("Final filter is");
            System.out.println(finalFilter.toString());

            documentCursor =  this.list.find(finalFilter).sort(Sorts.ascending("value.created_at_ms")).iterator();
            docSize = this.list.countDocuments(finalFilter);
        }

        else {

            String className = assetClassType.getName();
            className = className.replaceAll("\\.", "ENCODE_DOT");
            Bson  filter = Filters.eq("value.clazz", className);
            documentCursor =  this.list.find(filter).sort(Sorts.ascending("value.created_at_ms")).iterator();
            docSize = this.list.countDocuments(filter);
        }
        
        MongoDbCursor nitriteCursorResponse = new MongoDbCursor(documentCursor, docSize);
        return nitriteCursorResponse;
    }

    @Override
    public NitriteDbCursor filterOutputModel(String outputModelName, SpelExpression spelExpression) throws Exception {
        return  null;
    }
    protected Bson spelToMongoFilter(SpelNode node){

        if (node == null) return null;
        
        // 1. Handle Logical Gates (AND / OR)
        if (node instanceof OpAnd) {

            return Filters.and(

                spelToMongoFilter(node.getChild(0)), spelToMongoFilter(node.getChild(1))
            );

        }

        if (node instanceof OpOr) {
            
            return Filters.or(

                spelToMongoFilter(node.getChild(0)), spelToMongoFilter(node.getChild(1))
            );
        }

        // 2. Handle Equality Comparisons (==, !=, <, >, etc.)
        if (node instanceof OpEQ) {

            
            if(node.getChild(1) instanceof StringLiteral stringNode){

                return Filters.eq("value." + node.getChild(0).toStringAST(), stringNode.getLiteralValue().getValue());
            }

            else if (node.getChild(1) instanceof IntLiteral intNode){

                return Filters.eq("value." + node.getChild(0).toStringAST(), intNode.getLiteralValue().getValue());
            }

            else{

                throw new IllegalStateException("Can not determine whether it is string or int. Need to implement it");
            }
            
        }

        // Fallback or catch-all if you need to trace unsupported nodes
        throw new IllegalArgumentException("Unsupported SpEL node type: " + node.getClass().getSimpleName());
    }

    public void createIndexOnAssetField(String field){
        
        this.list.createIndex( Indexes.ascending("value." + field));
        
    }

    public void createIndexOnOutputModelField(String field){

        this.list.createIndex( Indexes.ascending("value.outputModel.data." + field));
        
    }
}