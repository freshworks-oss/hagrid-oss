package com.freshworks.core.shared.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshworks.core.processor.AbstractAsset;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.infra.InfraService;
import com.freshworks.core.shared.sync.SyncStatusService;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Scope(value="prototype")
public class ConsumerService {


    InfraService infraService;
    SyncStatusService syncStatusService;


    public void configure(SyncServiceContainer syncServiceContainer) throws Exception {
        this.infraService = syncServiceContainer.getBean(InfraService.class);
        this.syncStatusService = syncServiceContainer.getBean(SyncStatusService.class);
    }


    // public <T extends AbstractAsset> List<T> getAssetByAssetType(Class<T> assetClass) throws Exception {

    //     String whenAssetFieldName = "$." + assetClass.getSimpleName() + "." + "clazz" ;
    //     Expression expression = Expression.expressionBuilder().whenAssetFieldName(whenAssetFieldName).is().whenAssetFieldValue(assetClass.getName()).build();
    //     return getAbstractAssets(expression, assetClass);
    // }

    // public <T extends AbstractAsset> AssetStreamResponse<T> streamAssetByAssetType(Class<T> assetClass, AssetStreamResponse.Token nextToken) throws Exception {

    //     ObjectMapper objectMapper = new ObjectMapper();

    //     String whenAssetFieldName = "$." + assetClass.getSimpleName() + "." + "clazz" ;
    //     Expression expression = Expression.expressionBuilder().whenAssetFieldName(whenAssetFieldName).is().whenAssetFieldValue(assetClass.getName()).build();
    //     List<String> docIdStrList = jsonQueryService.queryAssetByExpression(expression);
    //     List<String> docIdStrDuplicateList = new ArrayList<>(docIdStrList);

    //     List<Long> interestedStrList = docIdStrDuplicateList.stream().skip(nextToken.getStart()).limit(nextToken.getCount()).map(Long::parseLong).collect(Collectors.toList());

    //     List<String> abstractAssetList = infraService.getPublisherList().get(interestedStrList);

    //     // Here form the response
    //     AssetStreamResponse<T> assetStreamResponse = new AssetStreamResponse<T>();

    //     List<T> abstractAssetResponseList = abstractAssetList.stream().map(asset -> {
    //         try {
    //             return objectMapper.readValue(asset, assetClass);
    //         } catch (JsonProcessingException e) {
    //             throw new RuntimeException(e);
    //         }
    //     }).collect(Collectors.toList());

    //     assetStreamResponse.setAbstractAssetList(abstractAssetResponseList);

    //     AssetStreamResponse.Token newNextToken = new AssetStreamResponse.Token();
    //     newNextToken.setCount(nextToken.getCount());
    //     newNextToken.setStart(nextToken.getStart() + interestedStrList.size());
    //     assetStreamResponse.setNextToken(newNextToken);

    //     /**
    //      *  When hagrid is (completed OR failed) AND (start has reached the last index of the publisher list) then
    //      *  set the nextToken as null
    //      */
    //     if(syncStatusService.getSyncStatus() != 0 && nextToken.getStart() >= docIdStrList.size()){
    //         assetStreamResponse.setNextToken(null);
    //     }

    //     return assetStreamResponse;

    // }

    // public <T extends AbstractAsset> List<T> getAssetByAssetTypeAndFilter(Class<T> assetClass, Expression expression) throws Exception {

    //     String whenAssetFieldName = "$." + assetClass.getSimpleName() + "." + "clazz" ;
    //     Expression abstractAssetBasedExpression = Expression.expressionBuilder().whenAssetFieldName(whenAssetFieldName).is().whenAssetFieldValue(assetClass.getName()).build();
    //     Expression finalExpression = Expression.expressionJoiner().whenLeftExpressionIs(abstractAssetBasedExpression).whenJoinerIsAnd().whenRightExpressionIs(expression).build();
    //     return getAbstractAssets(finalExpression, assetClass);
    // }

    // private <T extends AbstractAsset> List<T> getAbstractAssets(Expression finalExpression,Class<T> assetClass) throws Exception {

    //     ObjectMapper objectMapper = new ObjectMapper();

    //     List<String> docIdStrList = jsonQueryService.queryAssetByExpression(finalExpression);
    //     List<Long> documentIdList =  docIdStrList.stream().map(Long::valueOf).collect(Collectors.toList());
    //     List<String> abstractAssetList = infraService.getPublisherList().get(documentIdList);
    //     return abstractAssetList.stream().map(asset -> {
    //         try {
    //             return objectMapper.readValue(asset, assetClass);
    //         } catch (JsonProcessingException e) {
    //             throw new RuntimeException(e);
    //         }
    //     }).collect(Collectors.toList());
    // }

}
