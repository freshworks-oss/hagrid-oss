package com.freshworks.hagrid.main.dsl.runnable.context;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class FluentExpandoSerializer extends StdSerializer<FluentExpando> {

    public FluentExpandoSerializer() {
        this(null);
    }

    public FluentExpandoSerializer(Class<FluentExpando> t) {
        super(t);
    }

    @Override
    public void serialize(FluentExpando value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        // Start the JSON object: {
        gen.writeStartObject();

        if (Boolean.FALSE.equals(value.isLeafNode())){
            
            // Assuming your FluentExpando has a method like getProperties() returning a Map<String, Object>
            for (Map.Entry<String, Object> entry : ((Map<String, Object>) value.getProperties()).entrySet()) {
                gen.writeFieldName(entry.getKey());
            
                // let Jackson handle nested types dynamically
                Object valueObject = entry.getValue();

                if (valueObject instanceof FluentExpando){
                    FluentExpando nestedFluentExpando = (FluentExpando) valueObject;
                
                    // Now check if this nestedFluentExpando is a leaf node or not
                    if (nestedFluentExpando.isLeafNode()) {
                        // If it's a leaf node, write the actual value directly
                        gen.writeObject(nestedFluentExpando.getActualValue());
                    } else {
                        // If it's not a leaf node, recursively serialize it
                        gen.writeObject(entry.getValue());
                    }
                }
                else{
                    gen.writeObject(entry.getValue());
                }             
            }

        }

        else{
            System.out.println("here I am ");
        }

        // End the JSON object: }
        gen.writeEndObject();
    }
}
