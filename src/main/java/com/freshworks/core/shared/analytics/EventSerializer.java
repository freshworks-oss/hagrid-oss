package com.freshworks.core.shared.analytics;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.util.Map;

public class EventSerializer extends StdSerializer<Event> {

    public EventSerializer() {
        this(null);
    }


    protected EventSerializer(Class<Event> t) {
        super(t);
    }

    @Override
    public void serialize(Event event, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {

        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("id", event.getId());
        jsonGenerator.writeObjectField("event", event.getEvent());
        jsonGenerator.writeObjectField("severity", event.getSeverity());

        Map<String, Object> tags = event.getTags();
        if (tags != null) {
            for (Map.Entry<String, Object> entry : tags.entrySet()) {
                jsonGenerator.writeObjectField(entry.getKey(), entry.getValue());
            }



            jsonGenerator.writeObjectField("expiry", event.getEvent());
        jsonGenerator.writeObjectField("createdAt", event.getCreatedAt());

}

    }
}
