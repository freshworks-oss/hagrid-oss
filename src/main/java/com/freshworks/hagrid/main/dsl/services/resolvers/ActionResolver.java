package com.freshworks.hagrid.main.dsl.services.resolvers;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshworks.hagrid.main.dsl.config.action.ActionConfig;
import com.freshworks.hagrid.main.dsl.config.action.HooksConfig;
import com.freshworks.hagrid.main.dsl.config.model.ApiModelConfig;
import com.freshworks.hagrid.main.dsl.config.requests.RequestConfig;
import com.freshworks.hagrid.main.dsl.runnable.Action;
import com.freshworks.hagrid.main.dsl.runnable.ApiModel;
import com.freshworks.hagrid.main.dsl.runnable.Hooks;
import com.freshworks.hagrid.main.dsl.runnable.context.ActionContext;
import com.freshworks.hagrid.main.dsl.runnable.request.ActionRequest;
import com.freshworks.hagrid.main.dsl.runnable.request.http.ActionHttpRequest;
import com.freshworks.hagrid.main.dsl.services.resolvers.hooks.HooksResolver;
import com.freshworks.hagrid.main.dsl.services.resolvers.request.RequestResolver;

@Component
public class ActionResolver {
    
    RequestResolver requestResolver;
    HooksResolver hooksResolver;

    ObjectMapper objectMapper = new ObjectMapper();

    public ActionResolver(RequestResolver requestResolver, HooksResolver hooksResolver){
        this.requestResolver = requestResolver;
        this.hooksResolver = hooksResolver;
    }

    public Action resolveActionName(String name, Map<String, Object> context){

        return null;
    }

    public ActionRequest resolveRequest(RequestConfig requestConfig, ActionContext context) throws Exception{

        return this.requestResolver.resolve(requestConfig, context);
    }

    public Hooks resolveHook(HooksConfig hooksConfig, ActionContext context) throws Exception{

        return this.hooksResolver.resolve(hooksConfig, context);
    }

}
