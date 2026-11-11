package com.freshworks.hagrid.integration.sync.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.freshworks.hagrid.shared.SyncServiceContainer;
import com.freshworks.hagrid.shared.consumer.ConsumerService;
import com.freshworks.hagrid.shared.infra.InfraDbCursor;
import com.freshworks.hagrid.shared.sync.ConnectorConfiguration;
import com.freshworks.hagrid.shared.sync.SyncService;
import com.freshworks.hagrid.shared.sync.SyncStatusService;
import com.google.common.collect.ImmutableMap;

@SpringBootTest
@EnabledIfSystemProperty(named = "spring.profiles.active", matches = "integration")
public class TestConnectorUsecase {

    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    SyncService syncService;


    
    // Case 1 - Demonstration of simple action
    @Test
    public void testConnectorUsecase_1() throws Exception {
        
        ConnectorConfiguration connectorConfiguration = new ConnectorConfiguration();

        ImmutableMap<String, String> x = ImmutableMap.<String, String>builder()
                .put("waitBetweenCommunityPaginationInMs", "0").build();

        SyncServiceContainer syncServiceContainer = syncService.configureWithDslDag(UUID.randomUUID().toString(), "usecase_1" , x, connectorConfiguration);
        syncService.startSync();
        SyncStatusService syncStatusService = syncServiceContainer.getBean(SyncStatusService.class);
        ConsumerService consumerService = syncServiceContainer.getBean(ConsumerService.class);
        syncStatusService.waitUntilSyncIsInProgress();
        
        InfraDbCursor infraDbCursor = consumerService.getCursorByModelName("github_employee");

        while(infraDbCursor.hasNext()){
            ObjectNode data = infraDbCursor.getNext();
            System.out.println(data);
        }

        // JsonNode response = objectMapper.readTree(output.getResponse().toString());

        // assertThat(response.has("api_data"), is(true));
        // assertThat(response.get("api_data").has("res_body"), is(true));
        // assertThat(response.get("api_data").get("res_body").has("employees"), is(true));
        // assertThat(response.get("api_data").get("res_body").get("employees").size(), is(4));

        // // It contains the meta data also. 
        // assertThat(response.has("api_data"), is(true));
        // assertThat(response.get("api_data").get("res_body").has("meta"), is(true));
        // assertThat(response.get("api_data").get("res_body").get("meta").has("process_on"), is(true));
    }


    // Case 2 - Demonstration of action with response path
    // @Test
    // public void testConnectorUsecase_2() throws Exception {
    //     UIPWorkerOutput output = uipConnector.execute("usecase_2", Collections.emptyMap());
    //     JsonNode response = objectMapper.readTree(output.getResponse().toString());

    //     assertThat(response.has("api_data"), is(true));
    //     assertThat(response.get("api_data").has("res_body"), is(true));
    //     assertThat(response.get("api_data").get("res_body").has("employees"), is(true));
    //     assertThat(response.get("api_data").get("res_body").get("employees").size(), is(4));

    //     // It does not contain the meta data. 
    //     assertThat(response.has("api_data"), is(true));
    //     assertThat(response.get("api_data").get("res_body").has("meta"), is(false));
    // }


    // // Case 3 - Demonstration of action with response path and meta data
    // @Test
    // public void testConnectorUsecase_3() throws Exception {
    //     UIPWorkerOutput output = uipConnector.execute("usecase_3", Collections.emptyMap());
    //     JsonNode response = objectMapper.readTree(output.getResponse().toString());

    //     assertThat(response.has("api_data"), is(true));
    //     assertThat(response.get("api_data").has("res_body"), is(true));
    //     assertThat(response.get("api_data").get("res_body").has("employees"), is(true));
    //     assertThat(response.get("api_data").get("res_body").get("employees").size(), is(3));

    //     // It does not contain the meta data. 
    //     assertThat(response.has("api_data"), is(true));
    //     assertThat(response.get("api_data").get("res_body").has("meta"), is(true));
    // }


    // // Case 4 Demonstration of introducing variables in the operation
    // @Test
    // public void testConnectorUsecase_4() throws Exception {
        
    //     UIPWorkerOutput output = uipConnector.execute("usecase_4", Collections.emptyMap());
    //     JsonNode response = objectMapper.readTree(output.getResponse().toString());

    //     assertThat(response.has("api_data"), is(true));
    //     assertThat(response.get("api_data").has("res_body"), is(true));
    //     assertThat(response.get("api_data").get("res_body").has("employees"), is(true));
    //     assertThat(response.get("api_data").get("res_body").get("employees").size(), is(4));

    //     // It contains the meta data also. 
    //     assertThat(response.has("api_data"), is(true));
    //     assertThat(response.get("api_data").get("res_body").has("meta"), is(true));
    //     assertThat(response.get("api_data").get("res_body").get("meta").has("process_on"), is(true));
    // }

    // // Case 5 Demonstration of operation has variable from input
    // @Test
    // public void testConnectorUsecase_5() throws Exception {
    //     Map<String, Object> input = new HashMap<>();
    //     input.put("host", "https://api.jsonbin.io");

    //     UIPWorkerOutput output = uipConnector.execute("usecase_5", input);
    //     JsonNode response = objectMapper.readTree(output.getResponse().toString());

    //     assertThat(response.has("api_data"), is(true));
    //     assertThat(response.get("api_data").has("res_body"), is(true));
    //     assertThat(response.get("api_data").get("res_body").has("employees"), is(true));
    //     assertThat(response.get("api_data").get("res_body").get("employees").size(), is(4));

    //     // It contains the meta data also. 
    //     assertThat(response.has("api_data"), is(true));
    //     assertThat(response.get("api_data").get("res_body").has("meta"), is(true));
    //     assertThat(response.get("api_data").get("res_body").get("meta").has("process_on"), is(true));
    // }

    // // Case 6 Demonstration of non http operation
    // @Test
    // public void testConnectorUsecase_6() throws Exception {
    //     Map<String, Object> input = new HashMap<>();
    //     input.put("from_value", "Amit");
    //     input.put("to_value", "Amit");

    //     UIPWorkerOutput output = uipConnector.execute("usecase_6", input);
    //     JsonNode response = objectMapper.readTree(output.getResponse().toString());

    //     assertThat(response.get("is_match").asBoolean(), is(true));
    //     assertThat(response.get("to_value").asText(), is("Amit"));
    //     assertThat(response.get("from_value").asText(), is("Amit"));
    // }

    // // Case 7 Demonstration of hooks and error handling
    // @Test
    // public void testConnectorUsecase_7() throws Exception {
    //     Map<String, Object> input = new HashMap<>();
    //     input.put("from_value", "Amit");
    //     input.put("to_value", "Aggarwal");

    //     UIPWorkerOutput output = uipConnector.execute("usecase_7", input);
    //     JsonNode response = objectMapper.readTree(output.getResponse().toString());

    //     assertThat(response.get("is_match").asBoolean(), is(false));
    //     assertThat(response.get("to_value").asText(), is("Aggarwal"));
    //     assertThat(response.get("from_value").asText(), is("Amit"));
    //     assertThat(response.get("any_error").asText(), is("true"));

    // }

    // // Case 8 Demonstration of pagination requests
    // @Test
    // public void testConnectorUsecase_8() throws Exception {
    //     UIPWorkerOutput output = uipConnector.execute("usecase_8", Collections.emptyMap());
    //     JsonNode response = objectMapper.readTree(output.getResponse().toString());

    //     assertThat(response.size(), is(23));
        
    // }

    // // // Case 9 Demonstration of graph based step
    // // @Test
    // // public void testConnectorUsecase_9() throws Exception {
    // //     UIPWorkerOutput output = uipConnector.execute("usecase_9", Collections.emptyMap());
    // //     JsonNode response = objectMapper.readTree(output.getResponse().toString());

    // //     assertThat(response.get("encriched_data").get("company_name").asText(), is("Freshworks"));
    // // }
}
