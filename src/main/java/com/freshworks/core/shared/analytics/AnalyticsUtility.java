package com.freshworks.core.shared.analytics;

import com.google.common.base.Preconditions;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class AnalyticsUtility {

    public boolean areTagListValid(Object... tags){
        return tags.length % 2 == 0;
    }

    public Map<String, Object> processTagListIntoMap(Object... tags){

        Preconditions.checkArgument(areTagListValid(tags), "tag list must be in key value pair to convert it to map");
        try{
            Map<String, Object> logMap = new HashMap<>();
            if(tags.length%2 != 0 ){

                return new HashMap<>();
            }
            else{

                for(int i=0; i<tags.length; i = i+2){
                    logMap.put((String)tags[i], tags[i+1]);
                }


                return logMap;
            }
        }

        catch (Exception e){
            return new HashMap<>();
        }
    }
}
