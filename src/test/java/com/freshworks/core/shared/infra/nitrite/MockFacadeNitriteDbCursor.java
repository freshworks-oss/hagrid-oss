package com.freshworks.core.shared.infra.nitrite;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;

import java.util.List;

import org.dizitart.no2.Nitrite;
import org.dizitart.no2.collection.DocumentCursor;
import org.dizitart.no2.collection.NitriteCollection;
import org.dizitart.no2.filters.NitriteFilter;
import org.mockito.Mockito;

import com.freshworks.core.MockFacadeInterface;
import com.freshworks.core.ReturnableMockTypeList;
import com.google.inject.internal.util.Lists;

public class MockFacadeNitriteDbCursor implements MockFacadeInterface {

    ReturnableMockTypeList<Boolean> hasMore = new ReturnableMockTypeList<>();
    ReturnableMockTypeList<Long> docSize = new ReturnableMockTypeList<>();
    ReturnableMockTypeList<List<String>> getNext = new ReturnableMockTypeList<>();
    
    NitriteDbList nitriteDbList;
    DocumentCursor documentCursor;
    NitriteFilter nitriteFilter;


    @Override
    public MockFacadeNitriteDbCursor configure(){

        reset();
        hasMore.add(false);
        docSize.add(10L);
        getNext.add(Lists.newArrayList("{\"name\":\"amit\"}", "{\"name\":\"rahul\"}"));
        return this;
    }


    public MockFacadeNitriteDbCursor hasMore(Boolean... hasMore){
        this.hasMore.clear();
        this.hasMore.add(hasMore);
        return this;
    }


    public MockFacadeNitriteDbCursor docSize(Long... docSize){
        this.docSize.clear();
        this.docSize.add(docSize);
        return this;
    }

    public MockFacadeNitriteDbCursor getNext(List<String>... docStringList){
        this.getNext.clear();
        this.getNext.add(docStringList);
        return this;
    }

    public MockFacadeNitriteDbCursor getNitriteDbList(NitriteDbList nitriteDbList){
        this.nitriteDbList = nitriteDbList;
        return this;
    }

    public MockFacadeNitriteDbCursor getDocumentCursor(DocumentCursor documentCursor){
        this.documentCursor = documentCursor;
        return this;
    }

    public MockFacadeNitriteDbCursor getNitriteFilter(NitriteFilter nitriteFilter){
        this.nitriteFilter = nitriteFilter;
        return this;
    }


    @Override
    public NitriteDbCursor build() throws Exception {
            
        NitriteDbCursor nitriteDbCursor = new NitriteDbCursor(nitriteDbList, nitriteFilter, documentCursor);
        nitriteDbCursor = Mockito.spy(nitriteDbCursor);

        doAnswer(hasMore.answer()).when(nitriteDbCursor).hasNext();
        doAnswer(getNext.answer()).when(nitriteDbCursor).getNext(anyInt());
        doAnswer(docSize.answer()).when(nitriteDbCursor).docSize();


        return nitriteDbCursor;

    }
    
}
