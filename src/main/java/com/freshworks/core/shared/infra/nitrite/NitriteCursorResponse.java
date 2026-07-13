import java.util.ArrayList;
import java.util.List;

import org.dizitart.no2.collection.Document;
import org.dizitart.no2.collection.DocumentCursor;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshworks.core.processor.AbstractAsset;
import com.freshworks.core.shared.infra.InfraDbCursorResponse;

public class NitriteCursorResponse implements InfraDbCursorResponse{

    ObjectMapper objectMapper = new ObjectMapper();

    DocumentCursor documentCursor;

    public NitriteCursorResponse(DocumentCursor documentCursor){
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

}