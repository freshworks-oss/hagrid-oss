package com.freshworks.core.shared.infra;

import java.util.List;

public interface InfraDbCursorResponse{

    public boolean hasMore();

    public List<String> getNext(int numberOfDocs);

}
