package com.freshworks.hagrid.main.dsl.runnable.request.http;

import java.util.Map;

import com.freshworks.hagrid.main.dsl.config.requests.RequestConfig.REQUEST_SUB_TYPE;
import com.freshworks.hagrid.main.dsl.config.requests.RequestConfig.REQUEST_TYPE;
import com.freshworks.hagrid.main.dsl.runnable.request.ActionRequest;
import com.freshworks.hagrid.main.dsl.runnable.request.ActionResponse;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ActionHttpRequest implements ActionRequest{

    @Override
    public ActionResponse executeRequest() throws Exception{
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'executeRequest'");
    }

    @Override
    public REQUEST_TYPE getRequestType() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getRequestType'");
    }

    @Override
    public REQUEST_SUB_TYPE getRequesSubType() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getRequesSubType'");
    }

}
