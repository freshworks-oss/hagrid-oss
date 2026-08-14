package com.freshworks.core.shared.infra.nitrite;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.dizitart.no2.collection.Document;
import org.dizitart.no2.collection.DocumentCursor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshworks.core.shared.infra.InfraDbCursor;

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
    public String getNext() throws Exception{
        
        Document document = cursorIterator.next();
        Object o  = document.get("value");
        String asset = objectMapper.writeValueAsString(o);
        asset = asset.replaceAll("ENCODE_DOT", "\\.");
        return asset;

    }


    @Override
    public long docSize() {
        return documentCursor.size();
    }

}