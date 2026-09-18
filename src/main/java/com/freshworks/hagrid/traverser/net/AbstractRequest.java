package com.freshworks.hagrid.traverser.net;

import java.net.URISyntaxException;

public abstract class AbstractRequest {

    public abstract String getRequestUri() throws URISyntaxException;
}
