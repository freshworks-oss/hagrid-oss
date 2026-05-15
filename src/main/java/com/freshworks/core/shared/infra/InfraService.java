package com.freshworks.core.shared.infra;


import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.freshindex.NamespaceService;
import com.freshworks.freshindex.index.JsonIndexService;
import com.freshworks.freshindex.index.query.JsonQueryService;

import java.io.IOException;


public interface InfraService {

    void configure(SyncServiceContainer syncServiceContainer, InfraConfigService infraConfigService) throws Exception;

    InfraDbQueue getProcessorQueue() throws Exception;

    JsonIndexService getJsonIndexService() throws Exception;

    JsonQueryService getJsonQueryService() throws Exception;

    NamespaceService getNamespaceService() throws Exception;

    void destroyFreshIndex() throws Exception;

    InfraDbKeyValue getKeyValue() throws Exception;

    InfraDbList getPublisherList() throws Exception;

    InfraDbList getInfraDbList(String listName) throws Exception;

    String getNamespace() throws Exception;

    void destroy() throws Exception;
}






