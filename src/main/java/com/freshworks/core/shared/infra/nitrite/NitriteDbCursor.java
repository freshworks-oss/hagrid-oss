package com.freshworks.core.shared.infra.nitrite;

import java.util.ArrayList;
import java.util.List;

import org.dizitart.no2.collection.Document;
import org.dizitart.no2.collection.DocumentCursor;
import org.dizitart.no2.filters.NitriteFilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshworks.core.shared.infra.InfraDbCursor;

import lombok.Getter;

@Getter
public class NitriteDbCursor implements InfraDbCursor{

    ObjectMapper objectMapper = new ObjectMapper();

    DocumentCursor documentCursor;
    NitriteFilter nitriteFilter;
    NitriteDbList nitriteDbList;

    public NitriteDbCursor(NitriteDbList nitriteDbList, NitriteFilter nitriteFilter, DocumentCursor documentCursor){
        this.nitriteDbList = nitriteDbList;
        this.nitriteFilter = nitriteFilter;
        this.documentCursor = documentCursor;
    }
    
    @Override
    public boolean hasNext() {
        
        return documentCursor.size() > 0;
    }

    @Override
    public String getNext() throws Exception{
        
        return objectMapper.writeValueAsString(this.documentCursor.firstOrNull());

    }


    @Override
    public long docSize() {
        return documentCursor.size();
    }

}