package com.freshworks.core.data.unit.dag.steps;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicNameValuePair;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
@Slf4j
public class Authentication {
    public static String getAuthtoken(){
        String accessToken = null;

        //Staging
        String azureadTenant = "freshworks.com";
        String azureadClientId = "a037b2a8-7458-4047-a5bd-83a83b06cff8";
        String azureadKey = "";

        String authenticationUrl = "https://login.microsoftonline.com/"+ azureadTenant  +"/oauth2/v2.0/token";

        HttpPost httpPost = new HttpPost(authenticationUrl);
        httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");

        ArrayList<NameValuePair>  postParameters = new ArrayList<NameValuePair>();
        postParameters.add(new BasicNameValuePair("grant_type", "client_credentials"));
        postParameters.add(new BasicNameValuePair("client_secret", azureadKey));
        postParameters.add(new BasicNameValuePair("client_id", azureadClientId));
        postParameters.add(new BasicNameValuePair("scope", "https://graph.microsoft.com/.default"));

        httpPost.setEntity(new UrlEncodedFormEntity(postParameters, StandardCharsets.UTF_8));

        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = (CloseableHttpResponse) client
                     .execute(httpPost)) {
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                // return it as a String
                String result = EntityUtils.toString(entity);
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode node = objectMapper.readTree(result);
                accessToken = node.get("access_token").asText();
            }
        }
        catch (Exception e){
            log.error("Error is {}", e.getStackTrace());
        }
        return accessToken;
    }
    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Token {
        String token_type;
        Long expires_in;
        Long ext_expires_in;
        String access_token;
    }
}