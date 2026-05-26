package com.freshworks.core.shared.infra.persistent;


import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.EnabledIf;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@EnabledIfSystemProperty(named = "spring.profiles.active", matches = ".*\\.unit\\.persistent")
public class TestMongoDbList {

    @Test
    public void testAddMethod() throws Exception{

        MongoCollection<Document> mockedCollection = Mockito.mock(MongoCollection.class);

        MongoDbList mongoDbList = new MongoDbList();
        mongoDbList.setList(mockedCollection);
        mongoDbList.add("{\"name\": \"amit\"}");
        assertThat(mongoDbList.getListIndex().get(), is(1L));
    }

    @Test
    public void testAddListMethod() throws Exception{

        MongoCollection<Document> mockedCollection = Mockito.mock(MongoCollection.class);

        MongoDbList mongoDbList = new MongoDbList();
        mongoDbList.setList(mockedCollection);

        ArrayList<String> list = new ArrayList<>();
        list.add("{\"name\": \"amit\"}");
        list.add("{\"name\": \"rahul\"}");

        mongoDbList.add(list);
        assertThat(mongoDbList.getListIndex().get(), is(2L));
    }

    @Test
    public void testAddAndGetIndexBulkMethod() throws Exception{

        MongoCollection<Document> mockedCollection = Mockito.mock(MongoCollection.class);

        MongoDbList mongoDbList = new MongoDbList();
        mongoDbList.setList(mockedCollection);

        ArrayList<String> list = new ArrayList<>();

        for(int i=0; i<100; i++){
            String s = "{\"name\": \"" + i + " \"}";
            list.add(s);
        }

        List<Long> longList = mongoDbList.addAndGetIndexBulk(list);
        assertThat(longList.size(), is(100));

        for(int i=0; i<100; i++){
            assertThat(longList.get(i), is(Long.valueOf(i)));
        }
    }

    @Test
    public void testAddAndGetIndexMethod() throws Exception{

        MongoCollection<Document> mockedCollection = Mockito.mock(MongoCollection.class);

        MongoDbList mongoDbList = new MongoDbList();
        mongoDbList.setList(mockedCollection);

        Long index = mongoDbList.addAndGetIndex("{\"name\": \"amit\"}");
        assertThat(index, is(0L));

    }


    @Test
    public void testGetByIndexMethod() throws Exception {

        MongoCollection<Document> mockedCollection = Mockito.mock(MongoCollection.class);
        FindIterable<Document> mockedFindIterable = Mockito.mock(FindIterable.class);
        when(mockedCollection.find(ArgumentMatchers.any(Bson.class))).thenReturn(mockedFindIterable);

        Document mockedDocument = Mockito.mock(Document.class);
        when(mockedFindIterable.first()).thenReturn(mockedDocument);
        when(mockedDocument.get("value")).thenReturn("{\"name\": \"amit\"}");



        MongoDbList mongoDbList = new MongoDbList();
        mongoDbList.setList(mockedCollection);


        String s = mongoDbList.get(2);
        assertThat(s.contains("amit"), is(true));

    }

    @Test
    public void testGetByIndexNElementsMethod() throws Exception {

        MongoCollection<Document> mockedCollection = Mockito.mock(MongoCollection.class);
        FindIterable<Document> mockedFindIterable = Mockito.mock(FindIterable.class);
        MongoCursor<Document> mockedCursor = Mockito.mock(MongoCursor.class);
        Document mockedDocument = Mockito.mock(Document.class);

        when(mockedCollection.find(ArgumentMatchers.any(Bson.class))).thenReturn(mockedFindIterable);
        when(mockedFindIterable.iterator()).thenReturn(mockedCursor);
        when(mockedCursor.hasNext()).thenReturn(true, true, false);
        when(mockedCursor.next()).thenReturn(mockedDocument, mockedDocument);
        when(mockedDocument.get("value")).thenReturn("{\"name\": \"amit\"}","{\"name\": \"rahul\"}");



        MongoDbList mongoDbList = new MongoDbList();
        mongoDbList.setList(mockedCollection);


        List<String> sList = mongoDbList.get(0, 2);
        assertThat(sList.size(), is(2));
        assertThat(sList.get(0).contains("amit"), is(true));
        assertThat(sList.get(1).contains("rahul"), is(true));
    }

    @Test
    public void testGetByDocumentIdNElementsMethod() throws Exception {

        MongoCollection<Document> mockedCollection = Mockito.mock(MongoCollection.class);
        FindIterable<Document> mockedFindIterable = Mockito.mock(FindIterable.class);
        MongoCursor<Document> mockedCursor = Mockito.mock(MongoCursor.class);
        Document mockedDocument = Mockito.mock(Document.class);

        when(mockedCollection.find(ArgumentMatchers.any(Bson.class))).thenReturn(mockedFindIterable);
        when(mockedFindIterable.iterator()).thenReturn(mockedCursor);
        when(mockedCursor.hasNext()).thenReturn(true, true, false);
        when(mockedCursor.next()).thenReturn(mockedDocument, mockedDocument);
        when(mockedDocument.get("value")).thenReturn("{\"name\": \"amit\"}","{\"name\": \"rahul\"}");



        MongoDbList mongoDbList = new MongoDbList();
        mongoDbList.setList(mockedCollection);

        List<Long> documentIdList = new ArrayList<>();
        documentIdList.add(1L);
        documentIdList.add(2L);

        List<String> sList = mongoDbList.get(documentIdList);
        assertThat(sList.size(), is(2));
        assertThat(sList.get(0).contains("amit"), is(true));
        assertThat(sList.get(1).contains("rahul"), is(true));
    }

    @Test
    public void testIsEndOfListReachedMethod() throws Exception{
        MongoCollection<Document> mockedCollection = Mockito.mock(MongoCollection.class);
        MongoDbList mongoDbList = new MongoDbList();
        mongoDbList.setList(mockedCollection);
        mongoDbList.add("{\"name\": \"amit\"}");
        mongoDbList.add("{\"name\": \"rahul\"}");
        assertThat(mongoDbList.isEndOfListReached(0), is(false));
        assertThat(mongoDbList.isEndOfListReached(1), is(false));
        assertThat(mongoDbList.isEndOfListReached(2), is(true));

    }

}
