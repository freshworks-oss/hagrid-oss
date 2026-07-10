
package com.freshworks.core.performance;

import static org.hamcrest.MatcherAssert.assertThat;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

import static org.dizitart.no2.filters.FluentFilter.where;

import org.checkerframework.checker.units.qual.s;
import org.dizitart.no2.Nitrite;
import org.dizitart.no2.collection.Document;
import org.dizitart.no2.collection.DocumentCursor;
import org.dizitart.no2.collection.NitriteCollection;
import org.dizitart.no2.index.IndexOptions;
import org.dizitart.no2.index.IndexType;
import org.dizitart.no2.rocksdb.RocksDBModule;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshworks.core.data.five_zero_zero.performance.fb.assets.FbUser;
import com.freshworks.core.data.five_zero_zero.performance.fb.assets.non_primitive_assets.FbUserComment;
import com.freshworks.core.data.five_zero_zero.performance.fb.steps.FbUserServer;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.consumer.ConsumerService;
import com.freshworks.core.shared.infra.nitrite.NitriteDbKeyValue;
import com.freshworks.core.shared.sync.SyncService;
import com.freshworks.core.shared.sync.SyncStatusService;
import com.freshworks.core.shared.synchronizers.ServiceTree;
import com.freshworks.core.traverser.ParentStep;
import com.google.common.collect.ImmutableMap;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@AutoConfigureObservability
@EnabledIfSystemProperty(named = "spring.profiles.active", matches = ".*\\.performance\\..*")
public class TestNitritePerformance {

        @Test
    public void populateNitriteWith10MRecords(){


        Nitrite nitriteDb = Nitrite.builder()
                        .loadModule(new RocksDBModule("/Users/aaggarwal/Documents/hagrid-releases/hagrid-oss/hagrid-oss/test-db/heavy-doc"))
                        .openOrCreate();

        NitriteCollection nitriteCollection = nitriteDb.getCollection("test_collection");

        nitriteCollection.dropAllIndices();

        IndexOptions options = new IndexOptions();
        options.setIndexType(IndexType.UNIQUE);
        nitriteCollection.createIndex(options,"key");

        options = new IndexOptions();
        options.setIndexType(IndexType.NON_UNIQUE);
        nitriteCollection.createIndex(options, "value");

        int targetSizeInBytes = 10 * 1024 * 1024;
        for(int i=0 ; i<1000000; i++){
            long start = System.currentTimeMillis();
            String value = "A".repeat(targetSizeInBytes); 
            Document document = Document.createDocument("key", i).put("value", value);
            nitriteCollection.insert(document);
            System.out.println("Document is inserted in " + (System.currentTimeMillis() - start));
        }
    }


    @Test
    public void populateNitriteWith10MRecordsHavingSameKey(){


        Nitrite nitriteDb = Nitrite.builder()
                        .loadModule(new RocksDBModule("/Users/aaggarwal/Documents/hagrid-releases/hagrid-oss/hagrid-oss/test-db/same_key"))
                        .openOrCreate();

        NitriteCollection nitriteCollection = nitriteDb.getCollection("test_collection");

        nitriteCollection.dropAllIndices();

        IndexOptions options = new IndexOptions();
        options.setIndexType(IndexType.NON_UNIQUE);
        nitriteCollection.createIndex(options,"key");

        options = new IndexOptions();
        options.setIndexType(IndexType.NON_UNIQUE);
        nitriteCollection.createIndex(options, "value");

        int targetSizeInBytes = 1024;
        for(int i=0 ; i<1000000; i++){
            long start = System.currentTimeMillis();
            String value = "A".repeat(targetSizeInBytes); 
            Document document = Document.createDocument("key", "1000").put("value", value);
            nitriteCollection.insert(document);
            System.out.println("Document is inserted in " + (System.currentTimeMillis() - start));
        }
    }

    private void resetNitriteDbKeyValue(String searchKey,  NitriteCollection nitriteCollection){

        System.out.println("search key is " + searchKey);
        boolean hasKey = nitriteCollection.hasIndex("value");

        if(hasKey){
            System.out.print("yes index is there on value");
        }
        else{
            System.out.println("NO index is there on value");
        }

        DocumentCursor dCursor = nitriteCollection.find(where("value").regex(".*" + searchKey + ".*"));
        System.out.println("Resetting "  + dCursor.size() + " number of keys");
        for (Document document : dCursor) {
            document.put("value", String.valueOf(document.get("key")));
            nitriteCollection.update(where("key").eq(document.get("key")), document);
            System.out.println("Document is reset");
        }

    }

    @Test
    public void testNitriteFindOperationPerformanceTest(){

        Nitrite nitriteDb = Nitrite.builder()
                        .loadModule(new RocksDBModule("/Users/aaggarwal/Documents/hagrid-releases/hagrid-oss/hagrid-oss/test-db/4.3.3"))
                        .openOrCreate();


        NitriteCollection nitriteCollection = nitriteDb.getCollection("test_collection");

        // Now find a key 
        long startTime = System.currentTimeMillis();
        for(int i=0; i < 10000; i++){
            System.out.println("Looking up key " + i);
            Document doc = nitriteCollection.find(where("key").eq(i)).firstOrNull();
            assertThat(doc.get("key"), Matchers.is(i));
            assertThat(doc.get("value"), Matchers.is(String.valueOf(i)));
        }

        System.out.println("Find time taken in ms " + (System.currentTimeMillis() - startTime));
    }

    @Test
    public void testNitriteInOperationPerformanceTest(){

        Nitrite nitriteDb = Nitrite.builder()
                        .loadModule(new RocksDBModule("/Users/aaggarwal/Documents/hagrid-releases/hagrid-oss/hagrid-oss/test-db/4.3.3"))
                        .openOrCreate();


        NitriteCollection nitriteCollection = nitriteDb.getCollection("test_collection");

        Integer arr[] = new Integer[10000];
        for(int i=0; i<10000; i++){
            arr[i] = i;
        }

        long startTime = System.currentTimeMillis();
        DocumentCursor docCursor = nitriteCollection.find(where("key").in(arr));
        assertThat(docCursor.size(), Matchers.is(10000L));
        System.out.println("in time taken in ms " + (System.currentTimeMillis() - startTime));
    
    }

    @Test
    public void testNitriteUpdateOperationPerformanceTest(){

        Integer[] updatedDocIds = new Integer[10000];

        Nitrite nitriteDb = Nitrite.builder()
                        .loadModule(new RocksDBModule("/Users/aaggarwal/Documents/hagrid-releases/hagrid-oss/hagrid-oss/test-db/4.3.3"))
                        .openOrCreate();


        NitriteCollection nitriteCollection = nitriteDb.getCollection("test_collection");

        long startTime = System.currentTimeMillis();
        for(int i=0; i<10000; i++){

            System.out.println("Updating document");
            updatedDocIds[i] = i;

            Document doc = nitriteCollection.find(where("key").eq(i)).firstOrNull();

            doc.put("value", String.valueOf(i) + "_updated");
            nitriteCollection.update(where("key").eq(i), doc);
            
            Document updatedDoc = nitriteCollection.find(where("key").eq(i)).firstOrNull();
            assertThat(updatedDoc.get("value"), Matchers.is(String.valueOf(i) + "_updated"));
        }

        System.out.println("updated time taken in ms " + (System.currentTimeMillis() - startTime));

        resetNitriteDbKeyValue("updated", nitriteCollection);
    
    }

}