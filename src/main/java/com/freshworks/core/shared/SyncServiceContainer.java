package com.freshworks.core.shared;


import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class SyncServiceContainer {

    @Autowired
    public SyncServiceContainer(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    HashMap<String, Object> hagridManagedBeans = new HashMap<>();
    ApplicationContext applicationContext;


    public <T> T getBean(Class<T> bean) {

        if(hagridManagedBeans.containsKey(bean.getName())) {
            return (T) hagridManagedBeans.get(bean.getName());
        }
        else{
            return applicationContext.getBean(bean);
        }
    }

    public <T> T getBean(String bean) {

        if(hagridManagedBeans.containsKey(bean)) {
            return (T) hagridManagedBeans.get(bean);
        }
        else{
            try{
                Class<?> clazz = Class.forName(bean);
                return (T) applicationContext.getBean(clazz);
            }
            catch (Exception e){
                e.printStackTrace();
            }
        }

        return null;
    }

    public <T> void add(T bean) {

        String beanName = bean.getClass().getName();

        // It is to handle the case where Spring proxy the class and changes its name
        beanName = beanName.split("\\$\\$")[0];
        hagridManagedBeans.put(beanName, bean);
    }

    public <T> void add(T bean, Class<? super T> cl) {

        String beanName = bean.getClass().getName();
        // It is to handle the case where Spring proxy the class and changes its name
        beanName = beanName.split("\\$\\$")[0];

        if(cl == null){
            hagridManagedBeans.put(beanName, bean);
        }
        else{
            // This case is to handle polymorphism, where assume infraservice to provide mongoService bean
            // So here I am adding it by both so that bean can be received
            hagridManagedBeans.put(bean.getClass().getName(), bean);
            hagridManagedBeans.put(cl.getName(), bean);
        }
    }




    public void clear(){
        this.hagridManagedBeans.clear();
    }


}
