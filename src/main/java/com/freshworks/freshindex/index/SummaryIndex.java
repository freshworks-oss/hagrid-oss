package com.freshworks.freshindex.index;

import com.freshworks.freshindex.index.typeindex.BaseIndex;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
@Scope("prototype")
public class SummaryIndex {

    AtomicBoolean syncVar = new AtomicBoolean(false);
    TreeMap<String, BaseIndex> index = new TreeMap<>();

    public boolean containsBaseKey(String key){

        return index.containsKey(key);
    }

    public void putBaseIndex(String key, BaseIndex index){

        for(;;){
            if(syncVar.compareAndSet(false, true)){
                this.index.put(key, index);
                syncVar.set(false);
                break;
            }
        }
    }

    public BaseIndex getBaseIndex(String key){
        return this.index.get(key);
    }

    public void clearBaseIndex(){

        this.index.clear();
    }

}
