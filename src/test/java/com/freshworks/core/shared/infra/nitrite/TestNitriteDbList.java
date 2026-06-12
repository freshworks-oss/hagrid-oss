package com.freshworks.core.shared.infra.nitrite;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.ArrayList;
import java.util.List;

import org.dizitart.no2.Nitrite;
import org.dizitart.no2.rocksdb.RocksDBModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;

import com.freshworks.core.shared.infra.nitrite.NitriteDbList;

@SpringBootTest
@EnabledIfSystemProperty(named = "spring.profiles.active", matches = ".*\\.unit\\.nitrite")
public class TestNitriteDbList {

    Nitrite nitriteDb;
    @BeforeEach
    public void setup(){
        nitriteDb = Nitrite.builder()
            .loadModule(new RocksDBModule("/Users/aaggarwal/Documents/nitrite/demo/database.db"))
            .openOrCreate();
    }


    @Test
    public void testAddMethod() throws Exception{

        NitriteDbList h2DbList = new NitriteDbList(nitriteDb, "some_name_space","some_name");

        h2DbList.add("{\"name\": \"amit\"}");
        assertThat(h2DbList.getListIndex().get(), is(1L));
        h2DbList.delete();
    }

    @Test
    public void testAddListMethod() throws Exception{
        NitriteDbList h2DbList = new NitriteDbList(nitriteDb, "some_name_space","some_name");

        ArrayList<String> list = new ArrayList<>();
        list.add("{\"name\": \"amit\"}");
        list.add("{\"name\": \"rahul\"}");

        h2DbList.add(list);
        assertThat(h2DbList.getListIndex().get(), is(2L));
        h2DbList.delete();
    }

    @Test
    public void testAddAndGetIndexBulkMethod() throws Exception{

        NitriteDbList h2DbList = new NitriteDbList(nitriteDb,  "some_name_space","some_name");

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
        NitriteDbList h2DbList = new NitriteDbList(nitriteDb,  "some_name_space","some_name");

        Long index = h2DbList.addAndGetIndex("{\"name\": \"amit\"}");
        assertThat(index, is(0L));
        h2DbList.delete();
    }


    @Test
    public void testGetByIndexMethod() throws Exception {
        NitriteDbList h2DbList = new NitriteDbList(nitriteDb,  "some_name_space","some_name");

        h2DbList.add("{\"name\": \"amit\"}");
        h2DbList.add("{\"name\": \"rahul\"}");
        String s = h2DbList.get(0);
        assertThat(s.contains("amit"), is(true));
        h2DbList.delete();
    }

    @Test
    public void testGetByIndexNElementsMethod() throws Exception {
        NitriteDbList h2DbList = new NitriteDbList(nitriteDb,  "some_name_space","some_name");

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

        NitriteDbList h2DbList = new NitriteDbList(nitriteDb,  "some_name_space","some_name");

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
        NitriteDbList h2DbList = new NitriteDbList(nitriteDb,  "some_name_space","some_name");

        h2DbList.add("{\"name\": \"amit\"}");
        h2DbList.add("{\"name\": \"rahul\"}");
        assertThat(h2DbList.isEndOfListReached(0), is(false));
        assertThat(h2DbList.isEndOfListReached(1), is(false));
        assertThat(h2DbList.isEndOfListReached(2), is(true));
        h2DbList.delete();

    }

}
