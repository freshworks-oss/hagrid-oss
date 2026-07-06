package com.freshworks.core.shared;

import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.mockito.Mockito;
import com.freshworks.core.MockFacadeInterface;

@Component
public class MockFacadeSyncServiceContainer implements MockFacadeInterface {


    SyncServiceContainer syncServiceContainer;

    @Autowired
    ApplicationContext applicationContext;

    HashMap<String, Object> hagridManagedBeans;


    @Override
    public MockFacadeSyncServiceContainer configure(){
        reset();
        this.hagridManagedBeans = new HashMap<>();
        return this;
    }



    public MockFacadeSyncServiceContainer hagridManagedBeans(HashMap<String, Object> hagridManagedBeans) {
        this.hagridManagedBeans = hagridManagedBeans;
        return this;
    }

    public MockFacadeSyncServiceContainer add(Object o, Class<?> clazz){
        this.hagridManagedBeans.put(clazz.getName(), o);
        return this;
    }

    @Override
    public SyncServiceContainer build(){

        syncServiceContainer = applicationContext.getBean(SyncServiceContainer.class);
        SyncServiceContainer syncServiceContainerSpy = Mockito.spy(syncServiceContainer);
        syncServiceContainerSpy.hagridManagedBeans = this.hagridManagedBeans;
        return syncServiceContainerSpy;
    }

}
