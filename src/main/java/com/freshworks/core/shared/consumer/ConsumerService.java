package com.freshworks.core.shared.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshworks.core.processor.AbstractAsset;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.infra.InfraDbCursorResponse;
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


    public InfraDbCursorResponse getCursorForAssetFilter(NitriteFilter filter) throws Exception{

        InfraDbCursorResponse infraDbCursorResponse = this.infraDbList.filter(filter);
        cursorDocumentMapping.put(infraDbCursorResponse.hashCode(), new TreeMap<>());
        return infraDbCursorResponse;
    }

    public <T extends AbstractAsset> List<T> getAssetForGivenCursor(InfraDbCursorResponse infraDbCursorResponse, Class<T> assetClass, int numberOfDocs) throws Exception {

        ObjectMapper objectMapper = new ObjectMapper();
        TreeMap<String, String> map = cursorDocumentMapping.get(infraDbCursorResponse.hashCode());


        List<String> sList = infraDbCursorResponse.getNext(numberOfDocs);

        List<T> returnableList = new ArrayList<>();

        for(int i=0; i<sList.size(); i++){
            T x = objectMapper.readValue(sList.get(i), new TypeReference<T>() {});
            
            // Return this result only when it is not returned
            if(Boolean.FALSE.equals(map.containsKey(x.getUuid()))){
                map.put(x.getUuid(), UUID.randomUUID().toString());
                returnableList.add(x);
            }
            
        }

        return returnableList;
    }

    public boolean hasCursorNext(InfraDbCursorResponse infraDbCursorResponse, long waitForDurationInSeconds) throws Exception{

        TreeMap<String, String> map = cursorDocumentMapping.get(infraDbCursorResponse.hashCode());
        if(infraDbCursorResponse.hasMore()){
            // It means there is data to be consumed from existing cursor
            return true;
        }
        else if(syncStatusService.getSyncStatus() == 0){

            if(waitForDurationInSeconds == 0 ){
                // If duration is 0 then wait for alteast 10 seconds
                Thread.sleep(10 * 1000);
            }
            else{
                Thread.sleep(waitForDurationInSeconds * 1000);
            }
            

            // It means data has been consumed from previous cursor but sync is still in progress 
            NitriteFilter nitriteFilter = infraDbCursorResponse.getFilterQuery();
            infraDbCursorResponse = this.infraDbList.filter(nitriteFilter);
            return true;
        }
        else{

            // It means data has been consumed from previously constructured cursor and sync is either failed
            // or successful

            if(map.keySet().size() < infraDbCursorResponse.docSize() ){

                // It means that some more that has been added after sync is completed and last cursor is created
                // Create another cursor to consume remaining data

                NitriteFilter nitriteFilter = infraDbCursorResponse.getFilterQuery();
                infraDbCursorResponse = this.infraDbList.filter(nitriteFilter);
                return true;
            }

            else{
                // It means all data has been consumed
                cursorDocumentMapping.remove(infraDbCursorResponse.hashCode());
                return false;
            }
            
        }
    }

}
