package com.freshworks.core.traverser.net.http;

import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.traverser.net.RequestResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.ssl.TrustStrategy;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

@Slf4j
public class HttpRequestResponse extends RequestResponse {

    String connectorName;
    HttpRequest request;
    HttpResponse response;

    public String getConnectorName() {
        return connectorName;
    }

    public void setConnectorName(String connectorName) {
        this.connectorName = connectorName;
    }

    public HttpRequest getRequest() {
        return request;
    }

    public void setRequest(HttpRequest request) {
        this.request = request;
    }

    public HttpResponse getResponse() {
        return response;
    }

    public void setResponse(HttpResponse response) {
        this.response = response;
    }


    // I am keeping it here for backward compatibility. There are some places where developers are using this method
    // directly to execute their http requests, outside the context of hagrid
    // In future we should remove it

    public void execute()  throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {

        CloseableHttpClient closeableHttpClient = createHttpClient();
        try(closeableHttpClient){
            HttpResponse httpResponse = closeableHttpClient.execute(this.request.httpRequest, response ->{
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

            this.setResponse(httpResponse);
        }

    }

    protected CloseableHttpClient createHttpClient() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {


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

        return HttpClientBuilder.create().build();
    }
}
