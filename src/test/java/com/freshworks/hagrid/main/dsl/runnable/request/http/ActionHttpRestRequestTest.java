package com.freshworks.hagrid.main.dsl.runnable.request.http;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.freshworks.hagrid.main.dsl.runnable.request.ActionResponse;
import com.freshworks.hagrid.main.dsl.runnable.request.ActionResponse.ACTION_HTTP_CODE;
import com.sun.net.httpserver.HttpServer;

class ActionHttpRestRequestTest {

    private HttpServer server;

    @BeforeEach
    void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/ok", exchange -> reply(exchange, 200, "[{\"name\":\"repo\"}]"));
        server.createContext("/denied", exchange -> reply(exchange, 401, "{\"message\":\"Bad credentials\"}"));
        server.createContext("/plain", exchange -> reply(exchange, 500, "boom"));
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    @Test
    void successIsOkWithStatusCode() throws Exception {
        ActionResponse response = call("/ok");
        assertEquals(ACTION_HTTP_CODE.OK, response.getHttpStatus());
        assertEquals(200, response.getStatusCode());
    }

    @Test
    void clientErrorIsBadResponseNotAnException() throws Exception {
        ActionResponse response = call("/denied");
        assertEquals(ACTION_HTTP_CODE.BAD, response.getHttpStatus());
        assertEquals(401, response.getStatusCode());
        assertEquals("Bad credentials", ((com.fasterxml.jackson.databind.JsonNode) response.getBody()).get("message").asText());
    }

    @Test
    void nonJsonErrorBodyIsKeptAsText() throws Exception {
        ActionResponse response = call("/plain");
        assertEquals(ACTION_HTTP_CODE.BAD, response.getHttpStatus());
        assertEquals(500, response.getStatusCode());
        assertEquals("boom", ((com.fasterxml.jackson.databind.JsonNode) response.getBody()).asText());
    }

    private ActionResponse call(String path) throws Exception {
        ActionHttpRestRequest request = new ActionHttpRestRequest();
        request.setMethod("GET");
        request.setHost("http://localhost:" + server.getAddress().getPort());
        request.setPath(path);
        request.setHeaders(new HashMap<>());
        return request.executeRequest();
    }

    private static void reply(com.sun.net.httpserver.HttpExchange exchange, int code, String body) throws java.io.IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", body.startsWith("boom") ? "text/plain" : "application/json");
        exchange.sendResponseHeaders(code, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
