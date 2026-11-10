package com.freshworks.hagrid.main.dsl.runnable.context;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class FluentExpandoDeserializer extends StdDeserializer<FluentExpando> {

    static ObjectMapper objectMapper = new ObjectMapper();
    public FluentExpandoDeserializer() {
        this(null);
    }

    public FluentExpandoDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public FluentExpando deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        FluentExpando expando = new FluentExpando();

        // Loop through JSON tokens until we hit the end of the object
        while (p.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = p.currentName();
            p.nextToken(); // Move to the field value

            // Read the value dynamically (handles Strings, Numbers, Booleans, Maps, Lists)
            Object value = p.readValueAs(Object.class);
            
            if (value instanceof Map){

                // Populate your FluentExpando instance (adjust to your fluent method name, e.g., set() or put())
                expando.setProperty(fieldName, objectMapper.convertValue(value, FluentExpando.class));
            }

            else if (value instanceof Iterable){

                // Populate your FluentExpando instance (adjust to your fluent method name, e.g., set() or put())
                // Here check if value is a List of Maps or a List of primitive types

                boolean isArrayOfPrimitives = false;

                for (Object item : (List<?>) value) {
                    if (item instanceof Map) {
                        isArrayOfPrimitives = false;
                        break;
                    } else {
                        isArrayOfPrimitives = true;
                        break;
                    }
                }

                if (Boolean.FALSE.equals(isArrayOfPrimitives)){
                    expando.setProperty(fieldName, objectMapper.convertValue(value, new TypeReference<List<FluentExpando>>() {}));
                }
                else{

                    List<FluentExpando> primtiveList = new ArrayList<>();
                    for (Object item : (List<?>) value) {
                        // If it's a primitive type, wrap it in a FluentExpando
                        FluentExpando primitiveValue = new FluentExpando(item);
                        primtiveList.add(primitiveValue);
                    }
                    expando.setProperty(fieldName, primtiveList);
                }
                
            }

            else{
                // Populate your FluentExpando instance (adjust to your fluent method name, e.g., set() or put())
                FluentExpando primitiveValue = new FluentExpando(value);
                expando.setProperty(fieldName, primitiveValue);
            }
            
        }
        return expando;
    }
}
