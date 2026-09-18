package com.freshworks.hagrid.main.dsl.config.requests;

import java.util.ArrayList;
import java.util.List;

import groovy.lang.Closure;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RequestSpec {
    
    List<RequestConfig> requestList = new ArrayList<>();

    public void request(Closure closure){

        RequestConfig request = new RequestConfig();
        requestList.add(request);

        closure.setDelegate(request);
        closure.setResolveStrategy(Closure.DELEGATE_FIRST);
        closure.call();
    }

    public RequestConfig getRequestByName(String name){

        for(RequestConfig request : requestList){

            if(request.getName().equalsIgnoreCase(name)){
                return request;
            }
        }

        return null;
    }
}
