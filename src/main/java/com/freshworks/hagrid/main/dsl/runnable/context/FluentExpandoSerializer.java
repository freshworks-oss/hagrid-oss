package com.freshworks.hagrid.main.dsl.runnable.context;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import lombok.val;

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
    
        try{
            if (Boolean.FALSE.equals(value.isLeafNode())){
            
                gen.writeStartObject();   
                // Assuming your FluentExpando has a method like getProperties() returning a Map<String, Object>
                for (Map.Entry<String, Object> entry : ((Map<String, Object>) value.getProperties()).entrySet()) {

                    // System.out.println("Writing field name " + entry.getKey());
                    gen.writeFieldName(entry.getKey());
            
                    // let Jackson handle nested types dynamically
                    Object valueObject = entry.getValue();

                    if (valueObject instanceof FluentExpando){
                        FluentExpando nestedFluentExpando = (FluentExpando) valueObject;
                
                        // Now check if this nestedFluentExpando is a leaf node or not
                        if (nestedFluentExpando.isLeafNode()) {
                            // If it's a leaf node, write the actual value directly
                            if (nestedFluentExpando.getActualValue() instanceof String){
                                gen.writeString(nestedFluentExpando.getActualValue().toString());
                            }

                            if (nestedFluentExpando.getActualValue() instanceof Integer){
                                gen.writeNumber(Integer.parseInt(nestedFluentExpando.getActualValue().toString()));
                            }

                            if (nestedFluentExpando.getActualValue() == null){
                                gen.writeNull();
                            }
                        } else {
                            // If it's not a leaf node, recursively serialize it
                            gen.writeObject(entry.getValue());
                        }
                    }
                    else{

                        // Write object as it is .. It could array of elements etc 
                        gen.writeObject(entry.getValue());
                    }             
                }

                // End the JSON object: }
                gen.writeEndObject();
            }

            else{
                if (value.actualValue instanceof String){
                    gen.writeString(value.getActualValue().toString());
                }

                if (value.actualValue instanceof Integer){
                    gen.writeNumber(Integer.parseInt(value.actualValue.toString()));
                }
            }
        }

        catch(Exception e){

            e.printStackTrace();
        }
    }
}
