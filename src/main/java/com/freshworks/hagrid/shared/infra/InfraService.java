package com.freshworks.hagrid.shared.infra;

import com.freshworks.hagrid.shared.SyncServiceContainer;
import com.freshworks.hagrid.shared.sync.ConnectorConfiguration;

public interface InfraService {

    void configure(SyncServiceContainer syncServiceContainer, InfraConfigService infraConfigService) throws Exception;

    InfraDbQueue getProcessorQueue() throws Exception;

    InfraDbKeyValue getKeyValue() throws Exception;

    InfraDbList getPublisherList() throws Exception;

    InfraDbList getInfraDbList(String listName) throws Exception;

    String getNamespace() throws Exception;

    void destroy() throws Exception;
}






