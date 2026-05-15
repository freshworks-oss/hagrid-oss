package com.freshworks.core.traverser.net;


import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;

public class Net {
    static PoolingHttpClientConnectionManager poolingHttpClientConnectionManager = new PoolingHttpClientConnectionManager();
    public static CloseableHttpClient closeableHttpClient = HttpClients.custom().setConnectionManager(poolingHttpClientConnectionManager).build();

}
