package com.freshworks.hagrid.shared.infra;

import com.freshworks.hagrid.processor.AbstractAsset;

public interface InfraDbCursor<T extends AbstractAsset>{

    public boolean hasNext();

    public long docSize();

    public T  getNext() throws Exception;
}
