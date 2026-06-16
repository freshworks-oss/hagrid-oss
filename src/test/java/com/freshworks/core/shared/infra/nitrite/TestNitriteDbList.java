package com.freshworks.core.shared.infra.nitrite;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.ArrayList;
import java.util.List;

import org.dizitart.no2.Nitrite;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@EnabledIfSystemProperty(named = "spring.profiles.active", matches = ".*\\.unit\\.nitrite")
public class TestNitriteDbList {

    Nitrite nitriteDb;
    @BeforeEach
    public void setup(){
        nitriteDb = Nitrite.builder()
            .openOrCreate();
    }


    @Test
    public void testAddMethod() throws Exception{

        NitriteDbList nitriteDbList = new NitriteDbList(nitriteDb, "some_name_space","some_name");

        nitriteDbList.add("{\"name\": \"amit\"}");
        assertThat(nitriteDbList.getListIndex().get(), is(1L));
        nitriteDbList.delete();
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

        NitriteDbList nitriteDbList = new NitriteDbList(nitriteDb,  "some_name_space","some_name");

        ArrayList<String> list = new ArrayList<>();

        for(int i=0; i<100; i++){
            String s = "{\"name\": \"" + i + " \"}";
            list.add(s);
        }

        List<Long> longList = nitriteDbList.addAndGetIndexBulk(list);
        assertThat(longList.size(), is(100));

        for(int i=0; i<100; i++){
            assertThat(longList.get(i), is(Long.valueOf(i)));
        }

        nitriteDbList.delete();
    }

    @Test
    public void testAddAndGetIndexMethod() throws Exception{
        NitriteDbList nitriteDbList = new NitriteDbList(nitriteDb,  "some_name_space","some_name");

        Long index = nitriteDbList.addAndGetIndex("{\"name\": \"amit\"}");
        assertThat(index, is(0L));
        nitriteDbList.delete();
    }


    @Test
    public void testGetByIndexMethod() throws Exception {
        NitriteDbList nitriteDbList = new NitriteDbList(nitriteDb,  "some_name_space","some_name");

        nitriteDbList.add("{\"name\": \"amit\"}");
        nitriteDbList.add("{\"name\": \"rahul\"}");
        String s = nitriteDbList.get(0);
        assertThat(s.contains("amit"), is(true));
        nitriteDbList.delete();
    }

    @Test
    public void testGetByIndexNElementsMethod() throws Exception {
        NitriteDbList nitriteDbList = new NitriteDbList(nitriteDb,  "some_name_space","some_name");

        nitriteDbList.add("{\"name\": \"amit\"}");
        nitriteDbList.add("{\"name\": \"rahul\"}");
        nitriteDbList.add("{\"name\": \"deepak\"}");
        nitriteDbList.add("{\"name\": \"praveen\"}");


        List<String> sList = nitriteDbList.get(0, 2);
        assertThat(sList.size(), is(2));
        assertThat(sList.get(0).contains("amit"), is(true));
        assertThat(sList.get(1).contains("rahul"), is(true));
        nitriteDbList.delete();
    }

    @Test
    public void testGetByDocumentIdNElementsMethod() throws Exception {

        NitriteDbList nitriteDbList = new NitriteDbList(nitriteDb,  "some_name_space","some_name");

        nitriteDbList.add("{\"name\": \"amit\"}");
        nitriteDbList.add("{\"name\": \"rahul\"}");
        nitriteDbList.add("{\"name\": \"deepak\"}");
        nitriteDbList.add("{\"name\": \"praveen\"}");

        List<Long> documentIdList = new ArrayList<>();
        documentIdList.add(1L);
        documentIdList.add(2L);

        List<String> sList = nitriteDbList.get(documentIdList);
        assertThat(sList.size(), is(2));
        assertThat(sList.get(0).contains("rahul"), is(true));
        assertThat(sList.get(1).contains("deepak"), is(true));
        nitriteDbList.delete();
    }

    @Test
    public void testIsEndOfListReachedMethod() throws Exception{
        NitriteDbList nitriteDbList = new NitriteDbList(nitriteDb,  "some_name_space","some_name");

        nitriteDbList.add("{\"name\": \"amit\"}");
        nitriteDbList.add("{\"name\": \"rahul\"}");
        assertThat(nitriteDbList.isEndOfListReached(0), is(false));
        assertThat(nitriteDbList.isEndOfListReached(1), is(false));
        assertThat(nitriteDbList.isEndOfListReached(2), is(true));
        nitriteDbList.delete();

    }

}
