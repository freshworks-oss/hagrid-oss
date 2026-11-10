package com.freshworks.hagrid.main.dsl.services;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.freshworks.hagrid.main.dsl.runnable.request.ActionRequest;
import com.freshworks.hagrid.main.dsl.runnable.request.ActionResponse;


@Component
public class RequestService {

    // Optional: registered by the host application; absent means the default execution
    @Autowired(required = false)
    RequestExecutor requestExecutor;

    public ActionResponse execute(ActionRequest request) throws Exception{

        return execute(request, null);
    }

    public ActionResponse execute(ActionRequest request, Map<String, Object> actionInput) throws Exception{

        if(requestExecutor != null){
            return requestExecutor.execute(request, actionInput);
        }
        return request.executeRequest();
    }
}
