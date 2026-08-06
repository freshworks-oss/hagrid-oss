package com.freshworks.core.shared.infra;

public interface InfraDbCursor{

    public boolean hasNext();

    public long docSize();

    public String getNext() throws Exception;
}
