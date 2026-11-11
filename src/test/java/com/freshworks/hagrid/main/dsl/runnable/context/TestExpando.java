package com.freshworks.hagrid.main.dsl.runnable.context;


import static org.hamcrest.MatcherAssert.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.fasterxml.jackson.databind.JsonNode;
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
        
        assertThat(fluentExpandoKey1.asString(), Matchers.is("value1"));
        assertThat(fluentExpandoKey2.asString(), Matchers.is("value2"));
        assertThat(fluentExpandoKey3.asInt(), Matchers.is(5));
        assertThat(nestedFluentExpandoKey1.asString(), Matchers.is("nestedValue1"));
        
        Map map = nestedFluentExpandoKey2.asMap();

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


        String response2 = "{\n" + //
                        "  \"_shared\": {},\n" + //
                        "  \"_operation\": {\n" + //
                        "    \"clazz\": \"com.freshworks.hagrid.main.dsl.runnable.request.http.ActionHttpRestRequest\",\n" + //
                        "    \"name\": null,\n" + //
                        "    \"type\": \"HTTP\",\n" + //
                        "    \"method\": \"GET\",\n" + //
                        "    \"host\": \"https://api.jsonbin.io/\",\n" + //
                        "    \"path\": \"v3/b/6ae86c41ac6210605a658274\",\n" + //
                        "    \"contentType\": null,\n" + //
                        "    \"query\": {},\n" + //
                        "    \"headers\": {\n" + //
                        "      \"X-Master-Key\": \"$2a$10$LrPVM0vD/9GZJerxV2.u5uCblZ6ZnYJOVPojOs6UL562cc2GMcov6\",\n" + //
                        "      \"X-Bin-Meta\": \"false\"\n" + //
                        "    },\n" + //
                        "    \"body\": \"\",\n" + //
                        "    \"executeClosure\": null,\n" + //
                        "    \"requestType\": \"HTTP\",\n" + //
                        "    \"requesSubType\": \"REST\"\n" + //
                        "  },\n" + //
                        "  \"_input\": {\n" + //
                        "    \"actionName\": \"usecase_1\"\n" + //
                        "  },\n" + //
                        "  \"_response\": {\n" + //
                        "    \"httpStatus\": \"OK\",\n" + //
                        "    \"body\": {\n" + //
                        "      \"res_body\": {\n" + //
                        "        \"employees\": [\n" + //
                        "          {\n" + //
                        "            \"name\": \"Amit Aggarwal\",\n" + //
                        "            \"email\": \"amit.aggarwal@freshworks.com\",\n" + //
                        "            \"address\": \"Bangalore\",\n" + //
                        "            \"company\": \"freshworks\"\n" + //
                        "          },\n" + //
                        "          {\n" + //
                        "            \"name\": \"Poornima\",\n" + //
                        "            \"email\": \"piyer@freshworks.com\",\n" + //
                        "            \"address\": \"Chennai\",\n" + //
                        "            \"company\": \"freshworks\"\n" + //
                        "          },\n" + //
                        "          {\n" + //
                        "            \"name\": \"Dinesh\",\n" + //
                        "            \"email\": \"dinesh.m@freshworks.com\",\n" + //
                        "            \"address\": \"Chennai\",\n" + //
                        "            \"company\": \"freshworks\"\n" + //
                        "          },\n" + //
                        "          {\n" + //
                        "            \"name\": \"test Employee\",\n" + //
                        "            \"email\": \"####\",\n" + //
                        "            \"address\": \"Chennai\",\n" + //
                        "            \"company\": \"freshworks\"\n" + //
                        "          }\n" + //
                        "        ],\n" + //
                        "        \"meta\": {\n" + //
                        "          \"process_on\": \"today\",\n" + //
                        "          \"creator\": \"amit\"\n" + //
                        "        }\n" + //
                        "      }\n" + //
                        "    },\n" + //
                        "    \"headers\": {\n" + //
                        "      \"Date\": [\n" + //
                        "        \"Tue, 10 Nov 2026 10:30:58 GMT\"\n" + //
                        "      ],\n" + //
                        "      \"Content-Type\": [\n" + //
                        "        \"application/json; charset=utf-8\"\n" + //
                        "      ],\n" + //
                        "      \"Content-Length\": [\n" + //
                        "        \"456\"\n" + //
                        "      ],\n" + //
                        "      \"Connection\": [\n" + //
                        "        \"keep-alive\"\n" + //
                        "      ],\n" + //
                        "      \"Server\": [\n" + //
                        "        \"cloudflare\"\n" + //
                        "      ],\n" + //
                        "      \"Nel\": [\n" + //
                        "        \"{\\\"report_to\\\":\\\"cf-nel\\\",\\\"success_fraction\\\":0.0,\\\"max_age\\\":604800}\"\n" + //
                        "      ],\n" + //
                        "      \"X-Powered-By\": [\n" + //
                        "        \"Express\"\n" + //
                        "      ],\n" + //
                        "      \"Access-Control-Allow-Origin\": [\n" + //
                        "        \"*\"\n" + //
                        "      ],\n" + //
                        "      \"ETag\": [\n" + //
                        "        \"W/\\\"1c8-pRxq50pxvy8D3mqsYJWmg2GwiZs\\\"\"\n" + //
                        "      ],\n" + //
                        "      \"Vary\": [\n" + //
                        "        \"Accept-Encoding\"\n" + //
                        "      ],\n" + //
                        "      \"cf-cache-status\": [\n" + //
                        "        \"DYNAMIC\"\n" + //
                        "      ],\n" + //
                        "      \"Report-To\": [\n" + //
                        "        \"{\\\"group\\\":\\\"cf-nel\\\",\\\"max_age\\\":604800,\\\"endpoints\\\":[{\\\"url\\\":\\\"https://a.nel.cloudflare.com/report/v4?s=UZkFGx4BAfxwcaWbNo7C9OC%2FJCroMKD6AEwlXjfWCBO1eA2nu2SsT4p%2BTQeyQnODxGNDwC9GM4W9zobEgSmgiS2WydMwYm60pl%2Bn%2BodwmmAAJY4Zsoz0wCHmPBX7%2BAPjLg%3D%3D\\\"}]}\"\n" + //
                        "      ],\n" + //
                        "      \"CF-RAY\": [\n" + //
                        "        \"a58462886e949cd7-DEL\"\n" + //
                        "      ],\n" + //
                        "      \"alt-svc\": [\n" + //
                        "        \"h3=\\\":443\\\"; ma=86400\"\n" + //
                        "      ]\n" + //
                        "    },\n" + //
                        "    \"model\": null\n" + //
                        "  },\n" + //
                        "  \"_parent_steps\": []\n" + //
                        "}";
        
        String response3 = "{\n" + //
                        "  \"_response\": {\n" + //
                        "    \"httpStatus\": \"OK\",\n" + //
                        "    \"body\": {\n" + //
                        "      \"res_body\": {\n" + //
                        "        \"employees\": [\n" + //
                        "          {\n" + //
                        "            \"name\": \"Amit Aggarwal\",\n" + //
                        "            \"email\": \"amit.aggarwal@freshworks.com\",\n" + //
                        "            \"address\": \"Bangalore\",\n" + //
                        "            \"company\": \"freshworks\"\n" + //
                        "          },\n" + //
                        "          {\n" + //
                        "            \"name\": \"Poornima\",\n" + //
                        "            \"email\": \"piyer@freshworks.com\",\n" + //
                        "            \"address\": \"Chennai\",\n" + //
                        "            \"company\": \"freshworks\"\n" + //
                        "          },\n" + //
                        "          {\n" + //
                        "            \"name\": \"Dinesh\",\n" + //
                        "            \"email\": \"dinesh.m@freshworks.com\",\n" + //
                        "            \"address\": \"Chennai\",\n" + //
                        "            \"company\": \"freshworks\"\n" + //
                        "          },\n" + //
                        "          {\n" + //
                        "            \"name\": \"test Employee\",\n" + //
                        "            \"email\": \"####\",\n" + //
                        "            \"address\": \"Chennai\",\n" + //
                        "            \"company\": \"freshworks\"\n" + //
                        "          }\n" + //
                        "        ],\n" + //
                        "        \"meta\": {\n" + //
                        "          \"process_on\": \"today\",\n" + //
                        "          \"creator\": \"amit\"\n" + //
                        "        }\n" + //
                        "      }\n" + //
                        "    },\n" + //
                        "    \"headers\": {\n" + //
                        "      \"Date\": [\n" + //
                        "        \"Tue, 10 Nov 2026 10:30:58 GMT\"\n" + //
                        "      ],\n" + //
                        "      \"Content-Type\": [\n" + //
                        "        \"application/json; charset=utf-8\"\n" + //
                        "      ],\n" + //
                        "      \"Content-Length\": [\n" + //
                        "        \"456\"\n" + //
                        "      ],\n" + //
                        "      \"Connection\": [\n" + //
                        "        \"keep-alive\"\n" + //
                        "      ],\n" + //
                        "      \"Server\": [\n" + //
                        "        \"cloudflare\"\n" + //
                        "      ],\n" + //
                        "      \"Nel\": [\n" + //
                        "        \"{\\\"report_to\\\":\\\"cf-nel\\\",\\\"success_fraction\\\":0.0,\\\"max_age\\\":604800}\"\n" + //
                        "      ],\n" + //
                        "      \"X-Powered-By\": [\n" + //
                        "        \"Express\"\n" + //
                        "      ],\n" + //
                        "      \"Access-Control-Allow-Origin\": [\n" + //
                        "        \"*\"\n" + //
                        "      ],\n" + //
                        "      \"ETag\": [\n" + //
                        "        \"W/\\\"1c8-pRxq50pxvy8D3mqsYJWmg2GwiZs\\\"\"\n" + //
                        "      ],\n" + //
                        "      \"Vary\": [\n" + //
                        "        \"Accept-Encoding\"\n" + //
                        "      ],\n" + //
                        "      \"cf-cache-status\": [\n" + //
                        "        \"DYNAMIC\"\n" + //
                        "      ],\n" + //
                        "      \"Report-To\": [\n" + //
                        "        \"{\\\"group\\\":\\\"cf-nel\\\",\\\"max_age\\\":604800,\\\"endpoints\\\":[{\\\"url\\\":\\\"https://a.nel.cloudflare.com/report/v4?s=UZkFGx4BAfxwcaWbNo7C9OC%2FJCroMKD6AEwlXjfWCBO1eA2nu2SsT4p%2BTQeyQnODxGNDwC9GM4W9zobEgSmgiS2WydMwYm60pl%2Bn%2BodwmmAAJY4Zsoz0wCHmPBX7%2BAPjLg%3D%3D\\\"}]}\"\n" + //
                        "      ],\n" + //
                        "      \"CF-RAY\": [\n" + //
                        "        \"a58462886e949cd7-DEL\"\n" + //
                        "      ],\n" + //
                        "      \"alt-svc\": [\n" + //
                        "        \"h3=\\\":443\\\"; ma=86400\"\n" + //
                        "      ]\n" + //
                        "    },\n" + //
                        "    \"model\": null\n" + //
                        "  },\n" + //
                        "  \"_parent_steps\": []\n" + //
                        "}";

        String response4 = "{\n" + //
                        "  \"_response\": {\n" + //
                        "    \"httpStatus\": \"OK\",\n" + //
                        "    \"body\": {\n" + //
                        "      \"res_body\": {\n" + //
                        "        \"employees\": [\n" + //
                        "          {\n" + //
                        "            \"name\": \"Amit Aggarwal\",\n" + //
                        "            \"email\": \"amit.aggarwal@freshworks.com\",\n" + //
                        "            \"address\": \"Bangalore\",\n" + //
                        "            \"company\": \"freshworks\"\n" + //
                        "          },\n" + //
                        "          {\n" + //
                        "            \"name\": \"Poornima\",\n" + //
                        "            \"email\": \"piyer@freshworks.com\",\n" + //
                        "            \"address\": \"Chennai\",\n" + //
                        "            \"company\": \"freshworks\"\n" + //
                        "          },\n" + //
                        "          {\n" + //
                        "            \"name\": \"Dinesh\",\n" + //
                        "            \"email\": \"dinesh.m@freshworks.com\",\n" + //
                        "            \"address\": \"Chennai\",\n" + //
                        "            \"company\": \"freshworks\"\n" + //
                        "          },\n" + //
                        "          {\n" + //
                        "            \"name\": \"test Employee\",\n" + //
                        "            \"email\": \"####\",\n" + //
                        "            \"address\": \"Chennai\",\n" + //
                        "            \"company\": \"freshworks\"\n" + //
                        "          }\n" + //
                        "        ],\n" + //
                        "        \"meta\": {\n" + //
                        "          \"process_on\": \"today\",\n" + //
                        "          \"creator\": \"amit\"\n" + //
                        "        }\n" + //
                        "      }\n" + //
                        "    },\n" + //
                        "    \"headers\": {\n" + //
                        "      \"Date\": [\n" + //
                        "        \"Tue, 10 Nov 2026 10:30:58 GMT\"\n" + //
                        "      ],\n" + //
                        "      \"Content-Type\": [\n" + //
                        "        \"application/json; charset=utf-8\"\n" + //
                        "      ],\n" + //
                        "      \"Content-Length\": [\n" + //
                        "        \"456\"\n" + //
                        "      ],\n" + //
                        "      \"Connection\": [\n" + //
                        "        \"keep-alive\"\n" + //
                        "      ],\n" + //
                        "      \"Server\": [\n" + //
                        "        \"cloudflare\"\n" + //
                        "      ],\n" + //
                        "      \"Nel\": [\n" + //
                        "        \"{\\\"report_to\\\":\\\"cf-nel\\\",\\\"success_fraction\\\":0.0,\\\"max_age\\\":604800}\"\n" + //
                        "      ],\n" + //
                        "      \"X-Powered-By\": [\n" + //
                        "        \"Express\"\n" + //
                        "      ],\n" + //
                        "      \"Access-Control-Allow-Origin\": [\n" + //
                        "        \"*\"\n" + //
                        "      ],\n" + //
                        "      \"ETag\": [\n" + //
                        "        \"W/\\\"1c8-pRxq50pxvy8D3mqsYJWmg2GwiZs\\\"\"\n" + //
                        "      ],\n" + //
                        "      \"Vary\": [\n" + //
                        "        \"Accept-Encoding\"\n" + //
                        "      ],\n" + //
                        "      \"cf-cache-status\": [\n" + //
                        "        \"DYNAMIC\"\n" + //
                        "      ],\n" + //
                        "      \"Report-To\": [\n" + //
                        "        \"{\\\"group\\\":\\\"cf-nel\\\",\\\"max_age\\\":604800,\\\"endpoints\\\":[{\\\"url\\\":\\\"https://a.nel.cloudflare.com/report/v4?s=UZkFGx4BAfxwcaWbNo7C9OC%2FJCroMKD6AEwlXjfWCBO1eA2nu2SsT4p%2BTQeyQnODxGNDwC9GM4W9zobEgSmgiS2WydMwYm60pl%2Bn%2BodwmmAAJY4Zsoz0wCHmPBX7%2BAPjLg%3D%3D\\\"}]}\"\n" + //
                        "      ],\n" + //
                        "      \"CF-RAY\": [\n" + //
                        "        \"a58462886e949cd7-DEL\"\n" + //
                        "      ],\n" + //
                        "      \"alt-svc\": [\n" + //
                        "        \"h3=\\\":443\\\"; ma=86400\"\n" + //
                        "      ]\n" + //
                        "    },\n" + //
                        "    \"model\": null\n" + //
                        "  }\n" + //
                        "}";
        // FluentExpando fluentExpando = new FluentExpando(objectMapper.readValue(response, Map.class));
        
        try{
            // String json = objectMapper.writeValueAsString(response);
            // System.out.println("Serialized JSON: " + json);

            // JsonNode node = objectMapper.readValue(response, JsonNode.class); // Validate JSON structure
            
            // assertThat(node.has("res_body"), Matchers.is(true));
            // assertThat(node.get("res_body").has("employees"), Matchers.is(true));
            // assertThat(node.get("res_body").get("employees").isArray(), Matchers.is(true));
            // assertThat(node.get("res_body").get("employees").size(), Matchers.is(4));
            // assertThat(node.get("res_body").get("employees").get(0).has("name"), Matchers.is(true));
            // assertThat(node.get("res_body").has("meta"), Matchers.is(true));

            FluentExpando fluentExpando = objectMapper.readValue(response, FluentExpando.class); // Validate JSON structure
            String json = objectMapper.writeValueAsString(fluentExpando);
            System.out.println(json);
            // FluentExpando resBody = (FluentExpando) fluentExpando.getProperty("res_body");
            // List list  = (List) resBody.getProperty("employees");
            // FluentExpando employee = (FluentExpando) list.get(0);
            // FluentExpando name = (FluentExpando) employee.getProperty("name");
            // System.out.println(name.asString());

            // String json = objectMapper.writeValueAsString(fluentExpando);
            // System.out.println("Serialized JSON: " + json);

        }

        catch(Exception e){
            e.printStackTrace();
        }
        
    }

    @Test 
    public void testComplexDeserialisation(){

        
    }
}   
