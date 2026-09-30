package com.freshworks.hagrid.shared.infra.nitrite;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.dizitart.no2.collection.Document;
import org.dizitart.no2.collection.DocumentCursor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.freshworks.hagrid.main.assets.GenericAsset;
import com.freshworks.hagrid.main.dsl.runnable.OutputModel;
import com.freshworks.hagrid.processor.AbstractAsset;
import com.freshworks.hagrid.shared.infra.InfraDbCursor;

import lombok.Getter;

@Getter
public class NitriteDbCursor implements InfraDbCursor{

    ObjectMapper objectMapper = new ObjectMapper();

    DocumentCursor documentCursor;
    Iterator<Document> cursorIterator;

    public NitriteDbCursor(DocumentCursor documentCursor){
        this.documentCursor = documentCursor;
        cursorIterator = documentCursor.iterator();
    }
    
    @Override
    public boolean hasNext() {
        
        return cursorIterator.hasNext();
    }

    @Override
    public <T extends AbstractAsset> T getNextAsset() throws Exception{
        
        Document document = cursorIterator.next();
        Object o  = document.get("value");
        String asset = objectMapper.writeValueAsString(o);
        asset = asset.replaceAll("ENCODE_DOT", "\\.");
        return objectMapper.readValue(asset, new TypeReference<T>() {});
    }

    @Override
    public ObjectNode getNextOutputModel() throws Exception{
        
        Document document = cursorIterator.next();
        Object o  = document.get("value");
        String asset = objectMapper.writeValueAsString(o);
        asset = asset.replaceAll("ENCODE_DOT", "\\.");
        GenericAsset genericAsset = objectMapper.readValue(asset, GenericAsset.class);
        return genericAsset.getOutputModel().getData();
    }

    @Override
    public long docSize() {
        return documentCursor.size();
    }

}