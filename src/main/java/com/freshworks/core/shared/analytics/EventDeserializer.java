package com.freshworks.core.shared.analytics;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class EventDeserializer extends StdDeserializer<Event> {

    public EventDeserializer() {
        this(null);
    }

    protected EventDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public Event deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {

        JsonNode node = jsonParser.getCodec().readTree(jsonParser);

        Event event = new Event();

        String id = node.get("id").asText();
        event.setId(id);

        String eventName = node.get("event").asText();
        event.setEvent(eventName);

        String severity = node.get("severity").asText();
        event.setSeverity(severity);

        String expiry = node.get("expiry").asText();
        event.setExpiry(expiry);

        String createdAt = node.get("createdAt").asText();
        event.setCreatedAt(createdAt);

        Iterator<Map.Entry<String, JsonNode>> allFields = node.fields();

        Map<String, Object> tags = new HashMap<>();
        while(allFields.hasNext()) {
            Map.Entry<String, JsonNode> entry = allFields.next();

            if(entry.getKey().equals("id") || entry.getKey().equals("event") || entry.getKey().equals("severity") || entry.getKey().equals("expiry") || entry.getKey().equals("createdAt")) {}
            else{

                if(entry.getValue().isTextual()){
                    tags.put(entry.getKey(), entry.getValue().asText());
                }
                else if (entry.getValue().isIntegralNumber()){
                    tags.put(entry.getKey(), entry.getValue().asInt());
                }
                else if (entry.getValue().isDouble()){
                    tags.put(entry.getKey(), entry.getValue().asDouble());
                }

                else if (entry.getValue().isLong()){
                    tags.put(entry.getKey(), entry.getValue().asLong());
                }
                else if (entry.getValue().isBoolean()){
                    tags.put(entry.getKey(), entry.getValue().asBoolean());
                }
                else if (entry.getValue().isNull()){
                    tags.put(entry.getKey(), null);
                }
                else if (entry.getValue().isObject()){
                    tags.put(entry.getKey(), entry.getValue());
                }

            }
        }

        event.setTags(tags);
        return event;
    }
}
