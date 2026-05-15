package com.freshworks.core.traverser.net;

import java.util.HashMap;

public abstract class AbstractResponse {

    public abstract String getBody() throws Exception;

    public abstract int getCode();

    public abstract HashMap<String, Object> getHeaders();
    public abstract HashMap<String, Object> getHeaders(String name);

    public abstract HashMap<String, Object> getHeader(String name);
}
