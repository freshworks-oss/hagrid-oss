package com.freshworks.hagrid.integration.sync.test;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component 
public class CompareAndMatch {

    public Map<String, String> compare(String fromValue, String toValue){

        Map<String, String> map = new HashMap<>();

        if(fromValue.equalsIgnoreCase(toValue)){
            map.put("is_match", "true");
        }
        else{
            map.put("is_match", "false");
        }
        
        map.put("to_value", toValue);
        map.put("from_value", fromValue);

        // I am adding any_error to true just to demonstrate the usecase of step_parse_response hook
        map.put("any_error", "true");

        return map;
    }
    
}
