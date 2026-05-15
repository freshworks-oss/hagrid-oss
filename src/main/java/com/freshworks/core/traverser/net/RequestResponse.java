package com.freshworks.core.traverser.net;


public abstract class RequestResponse {

    String connectorName;
    AbstractRequest request;
    AbstractResponse response;


    public String getConnectorName() {
        return connectorName;
    }

    public void setConnectorName(String connectorName) {
        this.connectorName = connectorName;
    }

    public AbstractRequest getRequest() {
        return request;
    }

    public void setRequest(AbstractRequest request) {
        this.request = request;
    }

    public AbstractResponse getResponse() {
        return response;
    }

    public void setResponse(AbstractResponse response) {
        this.response = response;
    }

    public abstract void execute() throws Exception;
}
