package com.freshworks.core.shared.infra.inmemory;

import com.freshworks.core.shared.infra.persistent.MongoDbList;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.EnabledIf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

@SpringBootTest
@EnabledIfSystemProperty(named = "spring.profiles.active", matches = ".*\\.unit\\.inmemory")
public class TestInMemoryList {

    @Test
    public void testAddMethod() throws Exception{

        InmemoryList inMemoryList = new InmemoryList();
        inMemoryList.setList(new ArrayList<>());
        inMemoryList.add("{\"name\": \"amit\"}");
        assertThat(inMemoryList.getListIndex().get(), is(1L));
    }

    @Test
    public void testAddListMethod() throws Exception{

        InmemoryList inMemoryList = new InmemoryList();
        inMemoryList.setList(new ArrayList<>());

        ArrayList<String> list = new ArrayList<>();
        list.add("{\"name\": \"amit\"}");
        list.add("{\"name\": \"rahul\"}");

        inMemoryList.add(list);
        assertThat(inMemoryList.getListIndex().get(), is(2L));
    }

    @Test
    public void testAddAndGetIndexBulkMethod() throws Exception{

        InmemoryList inMemoryList = new InmemoryList();
        inMemoryList.setList(new ArrayList<>());

        ArrayList<String> list = new ArrayList<>();

        for(int i=0; i<100; i++){
            String s = "{\"name\": \"" + i + " \"}";
            list.add(s);
        }

        List<Long> longList = inMemoryList.addAndGetIndexBulk(list);
        assertThat(longList.size(), is(100));

        for(int i=0; i<100; i++){
            assertThat(longList.get(i), is(Long.valueOf(i)));
        }
    }

    @Test
    public void testAddAndGetIndexMethod() throws Exception{

        InmemoryList inmemoryList = new InmemoryList();
        inmemoryList.setList(new ArrayList<>());

        Long index = inmemoryList.addAndGetIndex("{\"name\": \"amit\"}");
        assertThat(index, is(0L));

    }


    @Test
    public void testGetByIndexMethod() throws Exception {

        InmemoryList inmemoryList = new InmemoryList();
        inmemoryList.setList(Arrays.asList("amit", "rahul", "deepak"));


        String s = inmemoryList.get(2);
        assertThat(s.contains("deepak"), is(true));

    }

    @Test
    public void testGetByIndexNElementsMethod() throws Exception {

        InmemoryList inmemoryList = new InmemoryList();
        inmemoryList.setList(Arrays.asList("amit", "rahul", "deepak"));


        List<String> sList = inmemoryList.get(0, 2);
        assertThat(sList.size(), is(2));
        assertThat(sList.get(0).contains("amit"), is(true));
        assertThat(sList.get(1).contains("rahul"), is(true));
    }

    @Test
    public void testGetByDocumentIdNElementsMethod() throws Exception {

        InmemoryList inmemoryList = new InmemoryList();
        inmemoryList.setList(Arrays.asList("amit", "rahul", "deepak"));

        List<Long> documentIdList = new ArrayList<>();
        documentIdList.add(1L);
        documentIdList.add(2L);

        List<String> sList = inmemoryList.get(documentIdList);
        assertThat(sList.size(), is(2));
        assertThat(sList.get(0).contains("rahul"), is(true));
        assertThat(sList.get(1).contains("deepak"), is(true));
    }

    @Test
    public void testIsEndOfListReachedMethod() throws Exception{

        InmemoryList inmemoryList = new InmemoryList();
        inmemoryList.setList(new ArrayList<>());
        inmemoryList.add("{\"name\": \"amit\"}");
        inmemoryList.add("{\"name\": \"rahul\"}");
        assertThat(inmemoryList.isEndOfListReached(0), is(false));
        assertThat(inmemoryList.isEndOfListReached(1), is(false));
        assertThat(inmemoryList.isEndOfListReached(2), is(true));

    }

}
