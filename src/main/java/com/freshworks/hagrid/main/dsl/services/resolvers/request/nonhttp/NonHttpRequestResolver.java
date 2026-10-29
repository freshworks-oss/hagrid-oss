package com.freshworks.hagrid.main.dsl.services.resolvers.request.nonhttp;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.freshworks.hagrid.main.dsl.config.requests.RequestConfig;
import com.freshworks.hagrid.main.dsl.config.requests.RequestConfig.REQUEST_TYPE;
import com.freshworks.hagrid.main.dsl.runnable.request.non_http.ActionNonHttpRequest;

@Component 
public class NonHttpRequestResolver {
        
    public ActionNonHttpRequest resolve(RequestConfig requestConfig, Map<String, Object> context) throws Exception{

        ActionNonHttpRequest actionNonHttpRequest = new ActionNonHttpRequest();

        actionNonHttpRequest.setName(requestConfig.getName());
        
        // resolve type key here
        String type = requestConfig.getType().toString();
        actionNonHttpRequest.setType(REQUEST_TYPE.valueOf(resolveStr(type, context)));

        Map<String, String> customProperties = requestConfig.getCustomProperties();
        Map<String, String> resolvedProperties = new HashMap<>();
        for(Map.Entry<String, String> entry : customProperties.entrySet()){

            String unresolvedKey = entry.getKey();
            String unresolvedValue = entry.getValue();
            
            String resolvedKey = resolveStr(unresolvedKey, context);
            String resolvedValue = resolveStr(unresolvedValue, context);
            resolvedProperties.put(resolvedKey, resolvedValue);
        }

        actionNonHttpRequest.setCustomProperties(resolvedProperties);

        return actionNonHttpRequest;
    }

    public String resolveStr(String str, Map<String, Object> context){

        for(Map.Entry<String, Object> map : context.entrySet()){

            if(str.contains( "{{" + map.getKey() + "}}")){

               str = str.replace("{{" + map.getKey() + "}}", String.valueOf(map.getValue()));
            }
        }

        return str;
    }

}
