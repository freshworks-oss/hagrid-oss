package com.freshworks.core.traverser.net;

import java.net.URISyntaxException;

public abstract class AbstractRequest {

    public abstract String getRequestUri() throws URISyntaxException;
}
