package com.freshworks.hagrid.shared.analytics;

import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;


@Component
public class AppEventService {


    public boolean validate(String event, Map<String, Object> tags){

        if(event.equalsIgnoreCase("HAGRID_ASSET_PUBLISH_DONE")){
            return validateHagridAssetPublishDoneEvent(tags);
        }

        if (event.equalsIgnoreCase("HAGRID_BEAN_PUBLISH_DONE")){

            return validateHagridBeanPublishDoneEvent(tags);
        }

        return false;
        
    }

    private boolean validateHagridAssetPublishDoneEvent(Map<String, Object> tags){

        Set<String> requiredKeySet = Set.of("asset_name","asset");
        Set<String> giveKeySet = tags.keySet();

        return giveKeySet.containsAll(requiredKeySet);
    }

    private boolean validateHagridBeanPublishDoneEvent(Map<String, Object> tags){

        Set<String> requiredKeySet = Set.of("bean_name","bean");
        Set<String> giveKeySet = tags.keySet();

        return giveKeySet.containsAll(requiredKeySet);
    }
    

    
}
