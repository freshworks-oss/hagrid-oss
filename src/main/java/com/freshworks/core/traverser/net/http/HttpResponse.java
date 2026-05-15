package com.freshworks.core.traverser.net.http;

import com.freshworks.core.traverser.net.AbstractResponse;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class HttpResponse extends AbstractResponse {


    String responseString;
    Header[] responseHeaders;
    int statusCode;

    protected HttpResponse(){
    }

    public void configure(String response, int statusCode, Header[] headers){
        this.responseString = response;
        this.statusCode = statusCode;
        this.responseHeaders = headers;
    }

    public HashMap<String, Object> getHeaders(){
        Header[] headers = this.responseHeaders;
        HashMap<String, Object> map = new HashMap<>();
        for(int i=0; i<headers.length; i++){
            map.put(headers[i].getName(), headers[i].getValue());
        }
        return map;
    }

    public HashMap<String, Object> getHeaders(String name){
        Header[] headers = this.responseHeaders;
//        Header[] headers = this.closeableHttpResponse.getHeaders(name);
        HashMap<String, Object> map = new HashMap<>();
        for(int i=0; i<headers.length; i++){
            map.put(headers[i].getName(), headers[i].getValue());
        }
        return map;
    }

    public HashMap<String, Object> getHeader(String name){
//        HashMap<String, Object> map = new HashMap<>();
//        Header header = this.closeableHttpResponse.getHeaders(name)[0];
//        map.put(header.getName(), header.getValue());
//        return map;

        return null;
    }

    public String getBody() throws IOException, ParseException {

        return this.responseString;
    }

    public int getCode(){
        return this.statusCode;
    }

}
