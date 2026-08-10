package com.freshworks.core.traverser;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import org.apache.hc.core5.http.ParseException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.mockserver.integration.ClientAndServer;
import org.springframework.boot.test.context.SpringBootTest;

import com.freshworks.core.traverser.net.http.HttpRequest;
import com.freshworks.core.traverser.net.http.HttpRequestResponse;
import com.freshworks.core.traverser.net.http.HttpResponse;

@SpringBootTest
@EnabledIfSystemProperty(named = "spring.profiles.active", matches = "unit")
public class TestHttpRequestResponse {

    private static ClientAndServer server;

    @BeforeAll
    public static void beforeAll(){

        server = ClientAndServer.startClientAndServer(1080);
        returnEmptyHttp204StatusCodeResponse();
        returnValidHttp200StatusCodeResponse();
    }

    @Test
    public void testHttpRequestResponseWhenResponseBodyIsEmpty() throws URISyntaxException, IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, ParseException {

        HttpRequestResponse httpRequestResponse = new HttpRequestResponse();

        HttpRequest httpRequest = new HttpRequest("http://localhost:1080/Http/204");
        httpRequestResponse.setRequest(httpRequest);
        httpRequestResponse.execute();
        HttpResponse httpResponse = httpRequestResponse.getResponse();
        assertThat(httpResponse.getBody(), nullValue());
    }



    @Test
    public void testHttpRequestResponseWhenResponseBodyIsValid200() throws URISyntaxException, IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, ParseException {

        HttpRequestResponse httpRequestResponse = new HttpRequestResponse();

        HttpRequest httpRequest = new HttpRequest("http://localhost:1080/Http/200");
        httpRequestResponse.setRequest(httpRequest);
        httpRequestResponse.execute();
        HttpResponse httpResponse = httpRequestResponse.getResponse();
        assertThat(httpResponse.getBody(), notNullValue());
        assertThat(httpResponse.getBody(), containsString("amit"));
    }

    private static void returnEmptyHttp204StatusCodeResponse(){

        server.when(request()
                .withMethod("GET")
                .withPath("/http/204"))
                .respond(response()
                        .withStatusCode(204));
    }


    private static void returnValidHttp200StatusCodeResponse(){

        server.when(request()
                        .withMethod("GET")
                        .withPath("/http/200"))
                .respond(response()
                        .withStatusCode(200)
                        .withBody("{\"name\":\"amit\"}"));
    }

    @AfterAll
    public static void afterAll() {
        server.stop();
    }
}
