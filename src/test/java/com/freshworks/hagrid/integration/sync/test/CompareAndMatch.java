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
        
        map.put("to_value", fromValue);
        map.put("from_value", toValue);

        return map;

    }
    
}
