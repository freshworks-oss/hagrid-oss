package com.freshworks.core.shared.infra;

import com.freshworks.core.shared.SyncServiceContainer;

public interface InfraService {

    void configure(SyncServiceContainer syncServiceContainer, InfraConfigService infraConfigService) throws Exception;

    InfraDbQueue getProcessorQueue() throws Exception;

    InfraDbKeyValue getKeyValue() throws Exception;

    InfraDbList getPublisherList() throws Exception;

    InfraDbList getInfraDbList(String listName) throws Exception;

    String getNamespace() throws Exception;

    void destroy() throws Exception;
}






