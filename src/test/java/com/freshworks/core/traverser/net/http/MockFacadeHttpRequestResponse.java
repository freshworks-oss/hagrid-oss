package com.freshworks.core.traverser.net.http;

import com.freshworks.core.MockFacadeInterface;
import org.apache.hc.core5.http.ParseException;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import static org.mockito.Mockito.*;

@Component
public class MockFacadeHttpRequestResponse implements MockFacadeInterface {

    @Autowired
    MockFacadeHttpRequest mockFacadeHttpRequest;
    HttpRequest getRequest;


    @Autowired
    MockFacadeHttpResponse mockFacadeHttpResponse;
    HttpResponse getResponse;


    public MockFacadeHttpRequestResponse configure() throws IOException, ParseException, URISyntaxException {

        reset();
        getRequest = mockFacadeHttpRequest.configure().build();
        getResponse = mockFacadeHttpResponse.configure().build();
        return this;
    }


    public MockFacadeHttpRequestResponse getRequest(HttpRequest getRequest) {
        this.getRequest = getRequest;
        return this;
    }


    public MockFacadeHttpRequestResponse getResponse(HttpResponse getResponse) {
        this.getResponse = getResponse;
        return this;
    }


    public HttpRequestResponse build() throws IOException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, ParseException, URISyntaxException {

        HttpRequestResponse httpRequestResponse = new HttpRequestResponse();
        httpRequestResponse = Mockito.spy(httpRequestResponse);

        doReturn(getRequest).when(httpRequestResponse).getRequest();
        doReturn(getResponse).when(httpRequestResponse).getResponse();

        doNothing().when(httpRequestResponse).setRequest(getRequest);
        doNothing().when(httpRequestResponse).setResponse(getResponse);

        return httpRequestResponse;
    }

}
