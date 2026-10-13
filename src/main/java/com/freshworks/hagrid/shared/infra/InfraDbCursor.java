package com.freshworks.hagrid.shared.infra;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.freshworks.hagrid.main.dsl.runnable.OutputModel;
import com.freshworks.hagrid.processor.AbstractAsset;

public interface InfraDbCursor{

    public boolean hasNext();

    public long docSize();

    public <T extends AbstractAsset> T  getNextAsset() throws Exception;
    public ObjectNode  getNextOutputModel() throws Exception;
}
