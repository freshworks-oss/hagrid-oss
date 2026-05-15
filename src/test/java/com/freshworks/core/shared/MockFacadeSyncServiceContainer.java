package com.freshworks.core.shared;

import com.freshworks.core.MockFacadeInterface;
import com.freshworks.core.shared.sync.SyncStatusService;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.HashMap;

@Component
public class MockFacadeSyncServiceContainer implements MockFacadeInterface {


    @SpyBean
    SyncServiceContainer syncServiceContainerSpy;

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

        syncServiceContainerSpy.hagridManagedBeans = this.hagridManagedBeans;
        return syncServiceContainerSpy;
    }

}
