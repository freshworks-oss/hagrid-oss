package com.freshworks.core.shared.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshworks.core.processor.AbstractAsset;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.infra.InfraDbCursor;
import com.freshworks.core.shared.infra.InfraDbList;
import com.freshworks.core.shared.infra.InfraService;
import com.freshworks.core.shared.sync.SyncStatusService;

import org.dizitart.no2.filters.NitriteFilter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.UUID;

@Component
@Scope(value="prototype")
public class ConsumerService {


    InfraService infraService;
    SyncStatusService syncStatusService;
    InfraDbList infraDbList;
    HashMap<Integer, TreeMap<String, String>> cursorDocumentMapping = new HashMap<>();


    public void configure(SyncServiceContainer syncServiceContainer) throws Exception {
        this.infraService = syncServiceContainer.getBean(InfraService.class);
        this.infraDbList = this.infraService.getPublisherList();
        this.syncStatusService = syncServiceContainer.getBean(SyncStatusService.class);
    }


    public InfraDbCursor initAssetCursor(NitriteFilter filter) throws Exception{

        InfraDbCursor infraDbCursor = this.infraDbList.filter(filter);
        cursorDocumentMapping.put(infraDbCursor.hashCode(), new TreeMap<>());
        return infraDbCursor;
    }

    public InfraDbCursor initAssetCursor() throws Exception{

        return initAssetCursor(null);
    }

    public <T extends AbstractAsset> List<T> getAssetListForGivenCursor(InfraDbCursor infraDbCursor, Class<T> assetClass, int numberOfDocs) throws Exception {

        ObjectMapper objectMapper = new ObjectMapper();
        TreeMap<String, String> map = cursorDocumentMapping.get(infraDbCursor.hashCode());

        List<T> returnableList = new ArrayList<>();

        while(infraDbCursor.hasNext()){
            T x = objectMapper.readValue(infraDbCursor.getNext(), new TypeReference<T>() {});
            
            // Return this result only when it is not returned
            if(Boolean.FALSE.equals(map.containsKey(x.getUuid()))){
                map.put(x.getUuid(), UUID.randomUUID().toString());
                returnableList.add(x);

                if(returnableList.size() >= numberOfDocs){
                    // break out of the loop
                    break;
                }
            }
        }

        return returnableList;
    }

    public boolean hasNextForAGivenCusor(InfraDbCursor infraDbCursor, long waitForDurationInMs) throws Exception{

        TreeMap<String, String> map = cursorDocumentMapping.get(infraDbCursor.hashCode());
        if(infraDbCursor.hasNext()){
            // It means there is data to be consumed from existing cursor
            return true;
        }
        else if(syncStatusService.getSyncStatus() == 0){

            if(waitForDurationInMs == 0 ){
                // If duration is 0 then wait for alteast 1 second
                Thread.sleep(1000);
            }
            else{
                Thread.sleep(waitForDurationInMs);
            }
            

            // It means data has been consumed from existing cursor but sync is still in progress. 
            // So there could be new data available. Hence referesh it
            infraDbCursor.refresh();
            return true;
        }
        else{

            // It means data has been consumed from previously constructured cursor and sync is either failed
            // or successful i.e. not in inprogress

            if(map.keySet().size() < infraDbCursor.docSize() ){

                // It means that some more that has been added after sync is completed and last cursor is created
                // Create another cursor to consume remaining data

                infraDbCursor.refresh();
                return true;
            }

            else{
                // It means all data has been consumed, remove the state maintaince of the cusor
                cursorDocumentMapping.remove(infraDbCursor.hashCode());
                return false;
            }
            
        }
    }

}
