package com.freshworks.core.traverser.net.http;

import com.freshworks.core.shared.analytics.AnalyticsFactory;
import com.freshworks.core.shared.analytics.AnalyticsService;
import com.freshworks.core.shared.synchronizers.GlobalNamespaceService;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.pool.PoolStats;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.ssl.TrustStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLContext;

@Component
public class HttpClientService {

    CloseableHttpClient closeableHttpClient;

    PoolStats poolStats;

    PoolingHttpClientConnectionManager poolingHttpClientConnectionManager;

    AnalyticsService analyticsService;

    String globalNamespace;

    GlobalNamespaceService globalNamespaceService;

    @Autowired
    public HttpClientService(AnalyticsFactory analyticsFactory, GlobalNamespaceService globalNamespaceService) {
        this.globalNamespaceService = globalNamespaceService;
        globalNamespace = this.globalNamespaceService.getGlobalNamespace();
        this.analyticsService = analyticsFactory.getAnalyticsService(globalNamespace);
        this.closeableHttpClient = createHttpClient();
    }

    private CloseableHttpClient createHttpClient(){
        try{

            final TrustStrategy acceptingTrustStrategy = (cert, authType) -> true;
            final SSLContext sslContext = SSLContexts.custom()
                    .loadTrustMaterial(null, acceptingTrustStrategy)
                    .build();
            final SSLConnectionSocketFactory sslsf =
                    new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);
            final Registry<ConnectionSocketFactory> socketFactoryRegistry =
                    RegistryBuilder.<ConnectionSocketFactory> create()
                            .register("https", sslsf)
                            .register("http", new PlainConnectionSocketFactory())
                            .build();

            poolingHttpClientConnectionManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
            poolingHttpClientConnectionManager.setMaxTotal(1000);
            poolingHttpClientConnectionManager.setDefaultMaxPerRoute(1000);

            poolStats = poolingHttpClientConnectionManager.getTotalStats();

            this.analyticsService.meterGauge("httpclient.connections.leased", poolingHttpClientConnectionManager, cm -> cm.getTotalStats().getLeased());
            this.analyticsService.meterGauge("httpclient.connections.available", poolingHttpClientConnectionManager, cm -> cm.getTotalStats().getAvailable());
            this.analyticsService.meterGauge("httpclient.connections.pending", poolingHttpClientConnectionManager, cm -> cm.getTotalStats().getPending());

            closeableHttpClient = HttpClients.custom().setConnectionManager(poolingHttpClientConnectionManager).build();
            return closeableHttpClient;
        }

        catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    public void execute(String namespace, HttpRequestResponse httpRequestResponse) throws Exception{

        HttpResponse httpResponse = closeableHttpClient.execute(httpRequestResponse.getRequest().httpRequest, response ->{
            HttpResponse httpResponseObject = new HttpResponse();
            HttpEntity entity = response.getEntity();
            String bodyAsString = null;
            if(entity != null ) {
                bodyAsString = EntityUtils.toString(entity);
            }

            int statusCode = response.getCode();
            Header[] headers = response.getHeaders();
            httpResponseObject.configure(bodyAsString, statusCode, headers);
            response.close();
            return httpResponseObject;
        });

        httpRequestResponse.setResponse(httpResponse);
    }
}
