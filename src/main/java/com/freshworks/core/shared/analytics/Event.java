package com.freshworks.core.shared.analytics;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@JsonDeserialize(using = EventDeserializer.class)
@JsonSerialize(using = EventSerializer.class)
public class Event {

    String id;
    String event;
    String severity;
    Map<String, Object> tags = new HashMap<>();
    String expiry;
    String createdAt;

    public boolean hasTags(String... tags) {

        Boolean hasTags = false;

        AnalyticsUtility analyticsUtility = new AnalyticsUtility();
        Map<String, Object> tagMap = analyticsUtility.processTagListIntoMap(tags);
        for (String key : tagMap.keySet()) {

            if(tagMap.get(key).equals(this.tags.get(key))) {
                hasTags = true;
            }
            else{
                hasTags = false;
                return hasTags;
            }
        }

        return hasTags;
    }

}
