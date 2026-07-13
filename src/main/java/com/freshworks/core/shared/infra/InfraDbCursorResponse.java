package com.freshworks.core.shared.infra;

import java.util.List;

import org.dizitart.no2.filters.NitriteFilter;

public interface InfraDbCursorResponse{

    public NitriteFilter getFilterQuery();

    public boolean hasMore();

    public long docSize();

    public List<String> getNext(int numberOfDocs);
}
