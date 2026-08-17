package com.freshworks.core.shared.infra;

import com.freshworks.core.processor.AbstractAsset;

public interface InfraDbCursor<T extends AbstractAsset>{

    public boolean hasNext();

    public long docSize();

    public T  getNext() throws Exception;
}
