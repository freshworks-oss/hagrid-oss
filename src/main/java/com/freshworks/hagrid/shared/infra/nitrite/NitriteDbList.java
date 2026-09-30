package com.freshworks.hagrid.shared.infra.nitrite;

import static org.dizitart.no2.filters.FluentFilter.where;

import java.util.ArrayList;
import org.dizitart.no2.filters.Filter;
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
import org.dizitart.no2.common.SortOrder;
import org.dizitart.no2.filters.AndFilter;
import org.dizitart.no2.filters.FluentFilter;
import org.dizitart.no2.filters.NitriteFilter;
import org.dizitart.no2.index.IndexOptions;
import org.dizitart.no2.index.IndexType;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.SpelNode;
import org.springframework.expression.spel.ast.IntLiteral;
import org.springframework.expression.spel.ast.Literal;
import org.springframework.expression.spel.ast.OpAnd;
import org.springframework.expression.spel.ast.OpEQ;
import org.springframework.expression.spel.ast.OpNE;
import org.springframework.expression.spel.ast.OpOr;
import org.springframework.expression.spel.ast.StringLiteral;
import org.springframework.expression.spel.ast.VariableReference;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshworks.hagrid.main.assets.GenericAsset;
import com.freshworks.hagrid.processor.AbstractAsset;
import com.freshworks.hagrid.shared.NamespaceService;
import com.freshworks.hagrid.shared.SyncServiceContainer;
import com.freshworks.hagrid.shared.analytics.AnalyticsFactory;
import com.freshworks.hagrid.shared.analytics.AnalyticsService;
import com.freshworks.hagrid.shared.infra.InfraDbList;

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

    ExpressionParser spelExpressionParser = new SpelExpressionParser();

    AtomicLong listIndex = new AtomicLong(0);


    private final ReentrantReadWriteLock.WriteLock listAddLock = new ReentrantReadWriteLock().writeLock();

    protected NitriteDbList(Nitrite nitriteDb, String namespace, String listName) throws Exception {

        this.nitriteDb = nitriteDb;
        this.listName = namespace + "_" + listName;
        this.nitriteCollection = nitriteDb.getCollection(this.listName);
        this.nitriteCollection.createIndex(IndexOptions.indexOptions(IndexType.UNIQUE),"list_index");
        this.nitriteCollection.createIndex(IndexOptions.indexOptions(IndexType.NON_UNIQUE),"value.created_at_ms");
    }

    @Override
    public void configure(SyncServiceContainer syncServiceContainer) throws Exception{

        NamespaceService namespace = syncServiceContainer.getBean(NamespaceService.class);
        AnalyticsFactory analyticsFactory = syncServiceContainer.getBean(AnalyticsFactory.class);
        analyticsService = analyticsFactory.getAnalyticsService(namespace.getNamespace());


        Filter.and(where("x").eq("b"), Filter.or(where("dbString").eq("b"), where("x").eq("y")));
        // (x == b) AND (("dbString" == "b") OR (x == y))
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

        Document subDocument = Document.createDocument(map);
        documentMap.put("list_index", listIndex);
        documentMap.put("value", subDocument);

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
    public <T extends AbstractAsset> NitriteDbCursor filterAsset(Class<T> assetClassType, SpelExpression spelExpression) throws Exception {
        
        
        if(assetClassType == null ){

            throw new IllegalArgumentException("asset class type can not be null. Consumer can consume asset by asset type only");
        }


        DocumentCursor documentCursor;
        FindOptions options = FindOptions.orderBy("value.created_at_ms", SortOrder.Ascending);


        if(spelExpression != null){

            String className = assetClassType.getName();
            className = className.replaceAll("\\.", "ENCODE_DOT");
            NitriteFilter mainFilter = where("value.clazz").eq(className);

            Filter developerFilter = spelToNitriteFilter(spelExpression.getAST(), false);
            
            System.out.println(" developer filter captured is");
            System.out.println(developerFilter.toString());

            Filter finalFilter = mainFilter.and(developerFilter);

            System.out.println("Final filter is ");
            System.out.println(finalFilter.toString());

            documentCursor = this.nitriteCollection.find(finalFilter, options);
        }

        else {

            String className = assetClassType.getName();
            className = className.replaceAll("\\.", "ENCODE_DOT");
            NitriteFilter filter = where("value.clazz").eq(className);

            documentCursor = this.nitriteCollection.find(filter, options);
        }
        
        NitriteDbCursor nitriteCursorResponse = new NitriteDbCursor(documentCursor);
        return nitriteCursorResponse;
    }

    @Override
    public NitriteDbCursor filterOutputModel(SpelExpression spelExpression) throws Exception {
        
        DocumentCursor documentCursor;
        FindOptions options = FindOptions.orderBy("value.created_at_ms", SortOrder.Ascending);


        if(spelExpression != null){

            String className = GenericAsset.class.getName();
            className = className.replaceAll("\\.", "ENCODE_DOT");
            NitriteFilter mainFilter = where("value.clazz").eq(className);

            Filter developerFilter = null;
            
            // I am creating a branch here, kind a patch. 
            // If asset is generic asset then filter expression provided by the developer should 
            // look into "value.outputModel."
            
            developerFilter = spelToNitriteFilter(spelExpression.getAST(), true);
            
            System.out.println(" developer filter captured is");
            System.out.println(developerFilter.toString());

            Filter finalFilter = mainFilter.and(developerFilter);

            System.out.println("Final filter is ");
            System.out.println(finalFilter.toString());

            documentCursor = this.nitriteCollection.find(finalFilter, options);
        }

        else {

            String className = GenericAsset.class.getName();
            className = className.replaceAll("\\.", "ENCODE_DOT");
            NitriteFilter filter = where("value.clazz").eq(className);

            documentCursor = this.nitriteCollection.find(filter, options);
        }
        
        NitriteDbCursor nitriteCursorResponse = new NitriteDbCursor(documentCursor);
        return nitriteCursorResponse;
    }

    protected Filter spelToNitriteFilter(SpelNode node, boolean isDslBased){

        if (node == null) return null;
        
        // 1. Handle Logical Gates (AND / OR)
        if (node instanceof OpAnd) {

            return Filter.and(

                spelToNitriteFilter(node.getChild(0), isDslBased), spelToNitriteFilter(node.getChild(1), isDslBased)
            );

        }

        if (node instanceof OpOr) {
            
            return Filter.or(

                spelToNitriteFilter(node.getChild(0), isDslBased), spelToNitriteFilter(node.getChild(1), isDslBased)
            );
        }

        // 2. Handle Equality Comparisons (==, !=, <, >, etc.)
        if (node instanceof OpEQ) {

            
            if(node.getChild(1) instanceof StringLiteral stringNode){

                if(isDslBased){
                    return where("value.outputModel.data" +  node.getChild(0).toStringAST()).eq(stringNode.getLiteralValue().getValue());
                }

                else{
                    return where("value." +  node.getChild(0).toStringAST()).eq(stringNode.getLiteralValue().getValue());
                }
                
                
            }

            else if (node.getChild(1) instanceof IntLiteral intNode){

                if(isDslBased){
                    return where("value.outputModel.data" + node.getChild(0).toStringAST()).eq(intNode.getLiteralValue().getValue());
                }

                else{
                    return where("value." + node.getChild(0).toStringAST()).eq(intNode.getLiteralValue().getValue());
                }
                
            }

            else{

                throw new IllegalStateException("Can not determine whether it is string or int. Need to implement it");
            }
            
        }
        if (node instanceof OpNE) {
            return where(node.getChild(0).toStringAST().replace("#", "")).notEq( "\"" + node.getChild(1)  + "\"");
        }

        // Fallback or catch-all if you need to trace unsupported nodes
        throw new IllegalArgumentException("Unsupported SpEL node type: " + node.getClass().getSimpleName());
    }

    public void createIndexOnAssetField(String field){

        if(!this.nitriteCollection.hasIndex( "value." + field)){
            
            this.nitriteCollection.createIndex( "value." + field);
        }
    }

    public void createIndexOnOutputModelField(String field){

        if(!this.nitriteCollection.hasIndex( "value.outputModel.data." + field)){
            
            this.nitriteCollection.createIndex( "value.outputModel.data." + field);
        }
    }
}
