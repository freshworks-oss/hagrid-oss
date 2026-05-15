package com.freshworks.core.shared.infra.h2;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

@SpringBootTest
@EnabledIfSystemProperty(named = "spring.profiles.active", matches = ".*\\.unit\\.h2")
public class TestH2DbList {

    HikariDataSource hikariDataSource;
    @BeforeEach
    public void setup(){

        HikariConfig config = new HikariConfig();
        String dbString =  "jdbc:h2:mem:testdb";
        config.setJdbcUrl(dbString);
        config.setUsername("");
        config.setPassword("");
        config.setIdleTimeout(60000); // 60 seconds
        hikariDataSource = new HikariDataSource(config);
    }


    @Test
    public void testAddMethod() throws Exception{

        H2DbList h2DbList = new H2DbList(hikariDataSource, "some_name_space","some_name");

        h2DbList.add("{\"name\": \"amit\"}");
        assertThat(h2DbList.getListIndex().get(), is(1L));
        h2DbList.delete();
    }

    @Test
    public void testAddListMethod() throws Exception{
        H2DbList h2DbList = new H2DbList(hikariDataSource, "some_name_space","some_name");

        ArrayList<String> list = new ArrayList<>();
        list.add("{\"name\": \"amit\"}");
        list.add("{\"name\": \"rahul\"}");

        h2DbList.add(list);
        assertThat(h2DbList.getListIndex().get(), is(2L));
        h2DbList.delete();
    }

    @Test
    public void testAddAndGetIndexBulkMethod() throws Exception{

        H2DbList h2DbList = new H2DbList(hikariDataSource,  "some_name_space","some_name");

        ArrayList<String> list = new ArrayList<>();

        for(int i=0; i<100; i++){
            String s = "{\"name\": \"" + i + " \"}";
            list.add(s);
        }

        List<Long> longList = h2DbList.addAndGetIndexBulk(list);
        assertThat(longList.size(), is(100));

        for(int i=0; i<100; i++){
            assertThat(longList.get(i), is(Long.valueOf(i)));
        }

        h2DbList.delete();
    }

    @Test
    public void testAddAndGetIndexMethod() throws Exception{
        H2DbList h2DbList = new H2DbList(hikariDataSource,  "some_name_space","some_name");

        Long index = h2DbList.addAndGetIndex("{\"name\": \"amit\"}");
        assertThat(index, is(0L));
        h2DbList.delete();
    }


    @Test
    public void testGetByIndexMethod() throws Exception {
        H2DbList h2DbList = new H2DbList(hikariDataSource,  "some_name_space","some_name");

        h2DbList.add("{\"name\": \"amit\"}");
        h2DbList.add("{\"name\": \"rahul\"}");
        String s = h2DbList.get(0);
        assertThat(s.contains("amit"), is(true));
        h2DbList.delete();
    }

    @Test
    public void testGetByIndexNElementsMethod() throws Exception {
        H2DbList h2DbList = new H2DbList(hikariDataSource,  "some_name_space","some_name");

        h2DbList.add("{\"name\": \"amit\"}");
        h2DbList.add("{\"name\": \"rahul\"}");
        h2DbList.add("{\"name\": \"deepak\"}");
        h2DbList.add("{\"name\": \"praveen\"}");


        List<String> sList = h2DbList.get(0, 2);
        assertThat(sList.size(), is(2));
        assertThat(sList.get(0).contains("amit"), is(true));
        assertThat(sList.get(1).contains("rahul"), is(true));
        h2DbList.delete();
    }

    @Test
    public void testGetByDocumentIdNElementsMethod() throws Exception {

        H2DbList h2DbList = new H2DbList(hikariDataSource,  "some_name_space","some_name");

        h2DbList.add("{\"name\": \"amit\"}");
        h2DbList.add("{\"name\": \"rahul\"}");
        h2DbList.add("{\"name\": \"deepak\"}");
        h2DbList.add("{\"name\": \"praveen\"}");

        List<Long> documentIdList = new ArrayList<>();
        documentIdList.add(1L);
        documentIdList.add(2L);

        List<String> sList = h2DbList.get(documentIdList);
        assertThat(sList.size(), is(2));
        assertThat(sList.get(0).contains("rahul"), is(true));
        assertThat(sList.get(1).contains("deepak"), is(true));
        h2DbList.delete();
    }

    @Test
    public void testIsEndOfListReachedMethod() throws Exception{
        H2DbList h2DbList = new H2DbList(hikariDataSource,  "some_name_space","some_name");

        h2DbList.add("{\"name\": \"amit\"}");
        h2DbList.add("{\"name\": \"rahul\"}");
        assertThat(h2DbList.isEndOfListReached(0), is(false));
        assertThat(h2DbList.isEndOfListReached(1), is(false));
        assertThat(h2DbList.isEndOfListReached(2), is(true));
        h2DbList.delete();

    }

}
