package com.freshworks.hagrid.shared.infra.mongo;

import org.bson.Document;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshworks.hagrid.processor.AbstractAsset;
import com.freshworks.hagrid.shared.infra.InfraDbCursor;
import com.mongodb.client.MongoCursor;

import lombok.Getter;

@Getter
public class MongoDbCursor<T extends AbstractAsset> implements InfraDbCursor{

    ObjectMapper objectMapper = new ObjectMapper();

    MongoCursor<Document> documentCursor;
    long docSize;

    public MongoDbCursor(MongoCursor<Document> documentCursor, long docSize){
        this.documentCursor = documentCursor;
        this.docSize = docSize;
    }
    
    @Override
    public boolean hasNext() {
        
        return documentCursor.hasNext();
    }

    @Override
    public T getNext() throws Exception{
        
        Document document = documentCursor.next();
        Object o  = document.get("value");
        String asset = objectMapper.writeValueAsString(o);
        asset = asset.replaceAll("ENCODE_DOT", "\\.");
        return objectMapper.readValue(asset, new TypeReference<T>() {});
    }


    @Override
    public long docSize() {
        return this.docSize;
    }

}