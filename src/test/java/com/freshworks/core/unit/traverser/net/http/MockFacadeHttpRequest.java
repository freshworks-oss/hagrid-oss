package com.freshworks.core.traverser.net.http;

import com.freshworks.core.MockFacadeInterface;
import com.freshworks.core.ReturnableMockTypeList;
import org.mockito.Mockito;
import org.springframework.stereotype.Component;

import java.net.URISyntaxException;
import java.util.HashMap;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Component
public class MockFacadeHttpRequest implements MockFacadeInterface {

    ReturnableMockTypeList<HashMap<String, Object>> getHeaders = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<String> getRequestUri = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<String> getRequestPath =  new ReturnableMockTypeList<>();



    public MockFacadeHttpRequest configure(){
        reset();

        getHeaders.add(new HashMap<>());
        getRequestUri.add("http://example.com/dummy/uri");
        getRequestPath.add("http://example.com/dummy/uri");

        return this;
    }


    public MockFacadeHttpRequest getHeaders(HashMap<String, Object>... getHeaders){
        this.getHeaders.clear();
        this.getHeaders.add(getHeaders);
        return this;
    }

    public MockFacadeHttpRequest getRequestUri(String... getRequestUri){
        this.getRequestUri.clear();
        this.getRequestUri.add(getRequestUri);
        return this;
    }

    public MockFacadeHttpRequest getRequestPath(String... getRequestPath){
        this.getRequestPath.clear();
        this.getRequestPath.add(getRequestPath);
        return this;
    }


    public HttpRequest build() throws URISyntaxException {

        HttpRequest httpRequest = new HttpRequest();
        httpRequest = Mockito.spy(httpRequest);

        doNothing().when(httpRequest).setHeaders(any(HashMap.class));

        doNothing().when(httpRequest).initGet(anyString());

        doNothing().when(httpRequest).initPost(anyString());

        doNothing().when(httpRequest).initPut(anyString());

        doNothing().when(httpRequest).initPatch(anyString());

        doNothing().when(httpRequest).initDelete(anyString());

        doNothing().when(httpRequest).setBodyAndContentType(anyString(), any());

        doNothing().when(httpRequest).setHeader(anyString(), anyString());

        doAnswer(getHeaders.answer()).when(httpRequest).getHeaders();

        doAnswer(getRequestUri.answer()).when(httpRequest).getRequestUri();

        doAnswer(getRequestPath.answer()).when(httpRequest).getRequestPath();

        doNothing().when(httpRequest).setURI(anyString());

        return httpRequest;
    }

}
