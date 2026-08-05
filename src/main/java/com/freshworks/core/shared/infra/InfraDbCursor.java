package com.freshworks.core.shared.infra;

import java.util.List;


public interface InfraDbCursor{

    public void refresh() throws Exception;

    public boolean hasNext();

    public long docSize();

    public String getNext() throws Exception;
}
