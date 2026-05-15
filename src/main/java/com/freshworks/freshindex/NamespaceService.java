package com.freshworks.freshindex;

import com.freshworks.freshindex.index.JsonIndexService;
import com.freshworks.freshindex.index.SummaryIndex;
import com.freshworks.freshindex.index.query.JsonQueryService;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Getter
@Setter
@Component
public class NamespaceService {

    HashMap<String, SummaryIndex> singletonSummaryIndexHashMap = new HashMap<>();

    AtomicBoolean setSyncVar = new AtomicBoolean(false);
    AtomicBoolean clearSyncVar = new AtomicBoolean(false);

    public void setSummaryIndexByNamespace(String namespace, SummaryIndex summaryIndex) {

        for(;;){

            if(setSyncVar.compareAndSet(false, true)){
                singletonSummaryIndexHashMap.put(namespace, summaryIndex);
                setSyncVar.set(false);
                break;
            }
        }
    }

    public SummaryIndex getSummaryIndexByNamespace(String namespace){

        return singletonSummaryIndexHashMap.get(namespace);
    }

    public void clearnNamespace(String namespace){

        for(;;){
            if(clearSyncVar.compareAndSet(false, true)){
                singletonSummaryIndexHashMap.remove(namespace);
                clearSyncVar.set(false);
                break;
            }
        }
    }

    public boolean containsNamespace(String namespace){

        return singletonSummaryIndexHashMap.containsKey(namespace);
    }

}
