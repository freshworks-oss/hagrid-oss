package com.freshworks.hagrid.main.dsl.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.freshworks.hagrid.main.dsl.runnable.request.ActionRequest;
import com.freshworks.hagrid.main.dsl.runnable.request.ActionResponse;


@Component
public class RequestService {

    // Optional: registered by the host application; absent means the default execution
    @Autowired(required = false)
    RequestExecutor requestExecutor;

    // The only place a request is executed: callers never call request.executeRequest() themselves
    public ActionResponse execute(ActionRequest request) throws Exception{

        if(requestExecutor != null){
            return requestExecutor.execute(request);
        }
        return request.executeRequest();
    }
}
