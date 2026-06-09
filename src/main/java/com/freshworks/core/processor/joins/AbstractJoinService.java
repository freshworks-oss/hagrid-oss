package com.freshworks.core.processor.joins;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshworks.core.processor.AbstractAsset;
import com.freshworks.core.processor.AbstractBean;
import com.freshworks.core.processor.Annotations.FreshJoin;
import com.freshworks.core.shared.infra.InfraDbKeyValue;
import com.google.common.hash.BloomFilter;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@Scope(value="prototype")
public abstract class AbstractJoinService {

    BloomFilter<String> bloomFilter;

    public void configure(BloomFilter<String> bloomFilter){
        this.bloomFilter = bloomFilter;
    }


    public List<HashMap<String, AbstractAsset>> lookupStagingArea(InfraDbKeyValue abstractKeyValue, AbstractAsset abstractAsset, FreshJoin freshJoin) throws Exception {

        List<HashMap<String, AbstractAsset>> returnMap = new ArrayList<>();
//        This is case when abstract asset is related to left class.
        ObjectMapper objectMapper = new ObjectMapper();
        if(freshJoin.leftClass().getName().contains(abstractAsset.getClass().getName())){
            String fieldValue = JoinUtility.getLookupFieldValueOfLeftClass(abstractAsset, freshJoin);
            abstractKeyValue.putList(freshJoin.uniqueJoinName() + "/" + fieldValue +  "_left",objectMapper.writeValueAsString(abstractAsset));
            bloomFilter.put(freshJoin.uniqueJoinName() + "/" + fieldValue +  "_left");

            Boolean doesRightLookupExists = bloomFilter.mightContain(freshJoin.uniqueJoinName() + "/" + fieldValue +  "_right");
            List<String> listOfAllAbstractAssets = new ArrayList<>();
            if(Boolean.TRUE.equals(doesRightLookupExists)) {

                // Go into the database only and only when bloom filter say it may exists
                listOfAllAbstractAssets = abstractKeyValue.getList(freshJoin.uniqueJoinName() + "/" + fieldValue + "_right");
                for (String s : listOfAllAbstractAssets) {
                    AbstractAsset rightAbstractAsset = objectMapper.readValue(s, AbstractAsset.class);
                    HashMap<String, AbstractAsset> map = new HashMap<>();
                    map.put(rightAbstractAsset.getClass().getName(), rightAbstractAsset);
                    map.put(abstractAsset.getClass().getName(), abstractAsset);
                    returnMap.add(map);
                }
                return returnMap;
            }
            else{

                // If look up is not found then send partial so that LeftJoin can make asset and publish it 
                HashMap<String, AbstractAsset> map = new HashMap<>();
                map.put(abstractAsset.getClass().getName(), abstractAsset);
                returnMap.add(map);
                return returnMap;
            }
        }

        // This is the case when abstract bean is related to the right class, hence we need to perform the lookup now
        else if(freshJoin.rightClass().getName().contains(abstractAsset.getClass().getName())){
            String fieldValue = JoinUtility.getLookupFieldValueOfRightClass(abstractAsset, freshJoin);
            abstractKeyValue.putList(freshJoin.uniqueJoinName() + "/" + fieldValue +  "_right",objectMapper.writeValueAsString(abstractAsset));
            bloomFilter.put(freshJoin.uniqueJoinName() + "/" + fieldValue +  "_right");


            Boolean doesLeftLookupExists = bloomFilter.mightContain(freshJoin.uniqueJoinName() + "/" +fieldValue + "_left");
            List<String> listOfAllAbstractAssets = new ArrayList<>();

            if(Boolean.TRUE.equals(doesLeftLookupExists)){

                listOfAllAbstractAssets = abstractKeyValue.getList(freshJoin.uniqueJoinName() + "/" +fieldValue + "_left");
                log.debug("Size of left lookup class found in database is {}", listOfAllAbstractAssets.size());
                //TODO: Here unwrap each of the abstract beans, and perform the lookup
                for (String s: listOfAllAbstractAssets) {
                    AbstractAsset leftAbstractAsset = objectMapper.readValue(s, AbstractAsset.class);
                    
                    HashMap<String, AbstractAsset> map = new HashMap<>();
                    map.put(leftAbstractAsset.getClass().getName(), leftAbstractAsset);
                    map.put(abstractAsset.getClass().getName(), abstractAsset);
                    returnMap.add(map);
                }
                return returnMap;
            }
            else{
                // Else do not publish this asset as we do not support RIGHT JOIN
            }

        }

        // This is the case when the abstract bean is the left class BUT not the child node like Application, ServicePrinciple ( assume, mention in join)
        else{
            
            // This case should not be happening .. If a asset is neither left nor right then then flow should not come here 
            // As it should already been taken care by assetAssetDependencyService.
        }

        return returnMap;
    }


    public abstract List<AbstractAsset> getNonPrimitiveAsset(InfraDbKeyValue abstractKeyValue, String assetName, AbstractAsset abstractAsset, List<String> assetAssetDependencyList, FreshJoin freshJoin) throws Exception;

    public abstract AbstractAsset getPrimitiveAsset(String assetName, AbstractBean abstractBean, List<String> assetBeanDependencyList) throws Exception;

}
