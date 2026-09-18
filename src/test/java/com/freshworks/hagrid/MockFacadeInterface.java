package com.freshworks.core;

import com.mongodb.client.MongoClient;
import org.springframework.util.ReflectionUtils;

public interface MockFacadeInterface {


    default public MockFacadeInterface configure() throws Exception{
        reset();
        return null;
    }

    public Object build() throws Exception;


    default public void reset() {
        ReflectionUtils.doWithFields(this.getClass(), field ->  {

                    field.setAccessible(true);
                    field.set(this, new ReturnableMockTypeList<>());
                },
                field -> field.getType() == ReturnableMockTypeList.class
        );
    }
}
