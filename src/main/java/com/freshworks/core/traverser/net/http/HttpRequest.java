package com.freshworks.core.traverser.net.http;

import com.freshworks.core.traverser.net.AbstractRequest;
import com.google.common.base.Strings;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.*;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@NoArgsConstructor
public class HttpRequest extends AbstractRequest {

    protected ClassicHttpRequest httpRequest;

    public HttpRequest(String url) {
        httpRequest = new HttpGet(url);
    }

    public void setHeaders(HashMap<String, Object> headers){

        for (Map.Entry<String, Object> entry: headers.entrySet()) {
            httpRequest.setHeader(entry.getKey(), entry.getValue());
        }
    }

    public void initGet(String url) throws URISyntaxException {
        this.httpRequest = new HttpGet(url);
    }

    public void initPost(String url) throws URISyntaxException {
        this.httpRequest = new HttpPost(url);
    }

    public void initPut(String url) throws URISyntaxException {
        this.httpRequest = new HttpPut(url);
    }

    public void initPatch(String url) throws URISyntaxException {
        httpRequest = new HttpPatch(url);
    }

    public void initDelete(String url) throws URISyntaxException {
        httpRequest = new HttpDelete(url);
    }

    public void setBodyAndContentType(String body, ContentType contentType) {
        if(body == null) { log.warn("Receiving the null body in the http request. Translating it to empty string and moving forward"); }
        body = Strings.nullToEmpty(body);
        ContentType contentTypeWithEncoding = ContentType.create(contentType.getMimeType(), StandardCharsets.UTF_8);
        StringEntity encodedBody = new StringEntity(body, contentTypeWithEncoding);
        httpRequest.setEntity(encodedBody);
    }

    public void setHeader(String key, String value){
        httpRequest.setHeader(key, value);
    }

    public HashMap<String, Object> getHeaders(){
        Header[] headers = httpRequest.getHeaders();
        HashMap<String, Object> map = new HashMap<>();
        for (Header header : headers) {
            map.put(header.getName(), header.getValue());
        }
        return map;
    }

    public String getRequestUri() throws URISyntaxException {
        return this.httpRequest.getUri().toString();
    }

    public String getRequestPath() throws URISyntaxException {
        return this.httpRequest.getPath().toString();
    }

    public void setURI(String uri) throws URISyntaxException {
        this.httpRequest.setUri(URI.create(uri));
    }
}
