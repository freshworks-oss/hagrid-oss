package com.freshworks.hagrid.main.dsl.services;

import org.springframework.stereotype.Component;

import com.freshworks.hagrid.main.dsl.runnable.request.ActionRequest;
import com.freshworks.hagrid.main.dsl.runnable.request.ActionResponse;


@Component
public class RequestService {
            
    public ActionResponse execute(ActionRequest request) throws Exception{

        return request.executeRequest();
    }
}
