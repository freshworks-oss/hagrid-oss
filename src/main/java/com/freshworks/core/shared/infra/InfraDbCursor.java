package com.freshworks.core.shared.infra;

import java.util.List;


public interface InfraDbCursor{

    public void refresh();

    public boolean hasMore();

    public long docSize();

    public List<String> getNext(int numberOfDocs);
}
