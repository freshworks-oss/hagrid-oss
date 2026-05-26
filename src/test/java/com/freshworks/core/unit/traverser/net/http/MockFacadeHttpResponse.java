package com.freshworks.core.traverser.net.http;

import com.freshworks.core.MockFacadeInterface;
import com.freshworks.core.ReturnableMockTypeList;
import org.apache.hc.core5.http.ParseException;
import org.mockito.Mockito;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@Component
public class MockFacadeHttpResponse implements MockFacadeInterface {

    ReturnableMockTypeList<HashMap<String, Object>> getHeaders = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<HashMap<String, Object>> getHeadersWithKey = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<String> getBody = new ReturnableMockTypeList<>();

    ReturnableMockTypeList<Integer> getCode = new ReturnableMockTypeList<>();


    public MockFacadeHttpResponse configure(){

        reset();

        getHeaders.add(new HashMap<>());
        getHeadersWithKey.add(new HashMap<>());
        getBody.add("{\"name\":\"amit\"}");
        getCode.add(200);
        return this;
    }


    public MockFacadeHttpResponse getHeaders(HashMap<String, Object> getHeaders) {
        this.getHeaders.clear();;
        this.getHeaders.add(getHeaders);
        return this;
    }

    public MockFacadeHttpResponse getHeadersWithKey(HashMap<String, Object> getHeadersWithKey) {
        this.getHeadersWithKey.clear();
        this.getHeadersWithKey.add(getHeadersWithKey);
        return this;
    }

    public MockFacadeHttpResponse getBody(String getBody) {
        this.getBody.clear();
        this.getBody.add(getBody);
        return this;
    }

    public MockFacadeHttpResponse getCode(int getCode) {
        this.getCode.clear();
        this.getCode.add(getCode);
        return this;
    }


    public HttpResponse build() throws IOException, ParseException {


        HttpResponse httpResponse = new HttpResponse();
        httpResponse = Mockito.spy(httpResponse);

        doAnswer(getHeaders.answer()).when(httpResponse).getHeaders();

        doAnswer(getHeadersWithKey.answer()).when(httpResponse).getHeader(anyString());

        doAnswer(getBody.answer()).when(httpResponse).getBody();

        doAnswer(getCode.answer()).when(httpResponse).getCode();

        return httpResponse;
    }
}
