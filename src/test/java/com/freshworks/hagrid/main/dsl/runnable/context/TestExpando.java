package com.freshworks.hagrid.main.dsl.runnable.context;


import static org.hamcrest.MatcherAssert.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
public class TestExpando {
    
    @Test 
    public void TestFluentExpando() throws Exception{
        
        ObjectMapper objectMapper = new ObjectMapper();
        String response = "{\n" + //
                        "  \"res_body\": {\n" + //
                        "    \"employees\": [\n" + //
                        "      {\n" + //
                        "        \"name\": \"Amit Aggarwal\",\n" + //
                        "        \"email\": \"amit.aggarwal@freshworks.com\",\n" + //
                        "        \"address\": \"Bangalore\",\n" + //
                        "        \"company\": \"freshworks\"\n" + //
                        "      },\n" + //
                        "      {\n" + //
                        "        \"name\": \"Poornima\",\n" + //
                        "        \"email\": \"piyer@freshworks.com\",\n" + //
                        "        \"address\": \"Chennai\",\n" + //
                        "        \"company\": \"freshworks\"\n" + //
                        "      },\n" + //
                        "      {\n" + //
                        "        \"name\": \"Dinesh\",\n" + //
                        "        \"email\": \"dinesh.m@freshworks.com\",\n" + //
                        "        \"address\": \"Chennai\",\n" + //
                        "        \"company\": \"freshworks\"\n" + //
                        "      },\n" + //
                        "      {\n" + //
                        "        \"name\": \"test Employee\",\n" + //
                        "        \"email\": \"####\",\n" + //
                        "        \"address\": \"Chennai\",\n" + //
                        "        \"company\": \"freshworks\"\n" + //
                        "      }\n" + //
                        "    ],\n" + //
                        "    \"meta\": {\n" + //
                        "      \"process_on\": \"today\",\n" + //
                        "      \"creator\": \"amit\"\n" + //
                        "    }\n" + //
                        "  }\n" + //
                        "}";

        Map<String, Object> sourceMap = new HashMap<>();
        sourceMap.put("key1", "value1");
        sourceMap.put("key2", "value2");
        sourceMap.put("key3", 5);

        Map<String, Object> nestedMap = new HashMap<>();
        nestedMap.put("nestedKey1", "nestedValue1");
        nestedMap.put("nestedKey2", "{\"name\": \"amit\"}");
        sourceMap.put("nested", nestedMap);


        FluentExpando expando = new FluentExpando(sourceMap);    
        FluentExpando fluentExpandoKey1 = (FluentExpando) expando.getProperty("key1");
        FluentExpando fluentExpandoKey2 = (FluentExpando) expando.getProperty("key2");
        FluentExpando fluentExpandoKey3 = (FluentExpando) expando.getProperty("key3");
        
        FluentExpando nestedFluentExpando = (FluentExpando) expando.getProperty("nested");
        FluentExpando nestedFluentExpandoKey1 = (FluentExpando) nestedFluentExpando.getProperty("nestedKey1");
        FluentExpando nestedFluentExpandoKey2 = (FluentExpando) nestedFluentExpando.getProperty("nestedKey2");
        
        assertThat(fluentExpandoKey1.getAsString(), Matchers.is("value1"));
        assertThat(fluentExpandoKey2.getAsString(), Matchers.is("value2"));
        assertThat(fluentExpandoKey3.getAsInt(), Matchers.is(5));
        assertThat(nestedFluentExpandoKey1.getAsString(), Matchers.is("nestedValue1"));
        
        Map map = nestedFluentExpandoKey2.getAsMap();

        assertThat(map.get("name"), Matchers.is("amit"));
    }

    @Test
    public void testComplexJson() throws Exception{

        ObjectMapper objectMapper = new ObjectMapper();
        String response = "{\n" + //
                        "  \"res_body\": {\n" + //
                        "    \"employees\": [\n" + //
                        "      {\n" + //
                        "        \"name\": \"Amit Aggarwal\",\n" + //
                        "        \"email\": \"amit.aggarwal@freshworks.com\",\n" + //
                        "        \"address\": \"Bangalore\",\n" + //
                        "        \"company\": \"freshworks\"\n" + //
                        "      },\n" + //
                        "      {\n" + //
                        "        \"name\": \"Poornima\",\n" + //
                        "        \"email\": \"piyer@freshworks.com\",\n" + //
                        "        \"address\": \"Chennai\",\n" + //
                        "        \"company\": \"freshworks\"\n" + //
                        "      },\n" + //
                        "      {\n" + //
                        "        \"name\": \"Dinesh\",\n" + //
                        "        \"email\": \"dinesh.m@freshworks.com\",\n" + //
                        "        \"address\": \"Chennai\",\n" + //
                        "        \"company\": \"freshworks\"\n" + //
                        "      },\n" + //
                        "      {\n" + //
                        "        \"name\": \"test Employee\",\n" + //
                        "        \"email\": \"####\",\n" + //
                        "        \"address\": \"Chennai\",\n" + //
                        "        \"company\": \"freshworks\"\n" + //
                        "      }\n" + //
                        "    ],\n" + //
                        "    \"meta\": {\n" + //
                        "      \"process_on\": \"today\",\n" + //
                        "      \"creator\": \"amit\"\n" + //
                        "    }\n" + //
                        "  }\n" + //
                        "}";


        FluentExpando fluentExpando = new FluentExpando(objectMapper.readValue(response, Map.class));
        
        FluentExpando resBody = (FluentExpando) fluentExpando.getProperty("res_body");
        List employees = (List) resBody.getProperty("employees");
        FluentExpando employee1 = (FluentExpando) employees.get(0);
        

    }   
}   
