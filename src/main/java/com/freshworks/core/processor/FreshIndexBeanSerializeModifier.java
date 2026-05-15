package com.freshworks.core.processor;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.freshworks.core.processor.Annotations.FreshIndex;

import java.util.ArrayList;
import java.util.List;

public class FreshIndexBeanSerializeModifier extends BeanSerializerModifier {

    @Override
    public List<BeanPropertyWriter> changeProperties(SerializationConfig config,
                                                     BeanDescription beanDesc, List<BeanPropertyWriter> beanProperties) {

        List<BeanPropertyWriter> beanPropertyWriterList = new ArrayList<>();

        for(BeanPropertyWriter beanPropertyWriter: beanProperties){

            FreshIndex freshIndex = beanPropertyWriter.getAnnotation(FreshIndex.class);
            if(freshIndex != null){
                beanPropertyWriterList.add(beanPropertyWriter);
            }
        }

        return beanPropertyWriterList;
    }
}
