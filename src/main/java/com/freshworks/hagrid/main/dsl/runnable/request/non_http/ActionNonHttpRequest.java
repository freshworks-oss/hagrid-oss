package com.freshworks.hagrid.main.dsl.runnable.request.non_http;

import java.util.Map;

import com.freshworks.hagrid.main.dsl.config.requests.RequestConfig.REQUEST_SUB_TYPE;
import com.freshworks.hagrid.main.dsl.config.requests.RequestConfig.REQUEST_TYPE;
import com.freshworks.hagrid.main.dsl.runnable.request.ActionRequest;
import com.freshworks.hagrid.main.dsl.runnable.request.ActionResponse;

public class ActionNonHttpRequest implements ActionRequest{

    
    @Override
    public ActionResponse executeRequest() {
        
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'executeRequest'");
    }

    @Override
    public REQUEST_TYPE getRequestType() {
        
        return REQUEST_TYPE.NON_HTTP;
    }

    @Override
    public REQUEST_SUB_TYPE getRequesSubType() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getRequesSubType'");
    }
    
}
