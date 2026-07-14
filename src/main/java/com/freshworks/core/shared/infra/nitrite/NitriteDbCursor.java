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
    public boolean hasMore() {
        
        return documentCursor.size() > 0;
    }

    @Override
    public List<String> getNext(int n) {
        
        int i = 0;
        List<String> returnList = new ArrayList<>();

        for(Document doc : documentCursor){

            if(i < 100){
                returnList.add(objectMapper.writeValueAsString(doc));
                i = i + 1;
            }
            else{
                break;
            }
        }

        return returnList;

    }

    @Override
    public void refresh() {
        
         NitriteDbCursor nitriteCursorResponse = (NitriteDbCursor)this.nitriteDbList.filter(nitriteFilter);
         this.documentCursor = nitriteCursorResponse.getDocumentCursor();
    }

    @Override
    public long docSize() {
        return documentCursor.size();
    }

}