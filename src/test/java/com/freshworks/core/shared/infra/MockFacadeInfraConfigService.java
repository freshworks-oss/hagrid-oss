package com.freshworks.core.shared.infra;

import com.freshworks.core.MockFacadeInterface;
import com.freshworks.core.ReturnableMockTypeList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mockito;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;


@Component
public class MockFacadeInfraConfigService implements MockFacadeInterface {

    @Autowired
    ApplicationContext applicationContext;

    InfraConfigService infraConfigService;

    ReturnableMockTypeList<String> getDatabaseUserName;

    ReturnableMockTypeList<String> getDatabasePassword;

    ReturnableMockTypeList<String> getDatabaseAuthDb;

    ReturnableMockTypeList<String> getDatabaseHost;

    ReturnableMockTypeList<String> getConnectionString;

    ReturnableMockTypeList<String> getAdditionalParams;

    ReturnableMockTypeList<Integer> getDatabasePort;

    ReturnableMockTypeList<String> getInfraType;
    ReturnableMockTypeList<String> getNitriteDataPath;

    ReturnableMockTypeList<String> getNitriteDatabaseType;


    @Override
    public MockFacadeInfraConfigService configure(){

        reset();
        getDatabaseUserName.add("admin");
        getDatabasePassword.add("password12345");
        getDatabaseAuthDb.add("admin");
        getDatabaseHost.add("mongodb");
        getAdditionalParams.add("dummy-additional-param=dummy-additional-value");
        getDatabasePort.add(27017);
        getInfraType.add("persistent");
        getNitriteDataPath.add("/Users/aaggarwal/Documents/hagrid-releases/hagrid-oss/hagrid-oss/database");
        getNitriteDatabaseType.add("file");
        getConnectionString.add("");

        return this;
    }

    public MockFacadeInfraConfigService getDatabaseUserName(String... getDatabaseUserName) {
        this.getDatabaseUserName.clear();
        this.getDatabaseUserName.add(getDatabaseUserName);
        return this;
    }

    public MockFacadeInfraConfigService getDatabasePassword(String... getDatabasePassword) {
        this.getDatabasePassword.clear();
        this.getDatabasePassword.add(getDatabasePassword);
        return this;
    }
    public MockFacadeInfraConfigService getDatabaseAuthDb(String... getDatabaseAuthDb) {
        this.getDatabaseAuthDb.clear();
        this.getDatabaseAuthDb.add(getDatabaseAuthDb);
        return this;
    }

    public MockFacadeInfraConfigService getDatabaseHost(String... getDatabaseHost) {
        this.getDatabaseHost.clear();
        this.getDatabaseHost.add(getDatabaseHost);
        return this;
    }

    public MockFacadeInfraConfigService getAdditionalParams(String... getAdditionalParams) {
        this.getAdditionalParams.clear();
        this.getAdditionalParams.add(getAdditionalParams);
        return this;
    }

    public MockFacadeInfraConfigService getDatabasePort(Integer... getDatabasePort) {
        this.getDatabasePort.clear();
        this.getDatabasePort.add(getDatabasePort);
        return this;
    }

    public MockFacadeInfraConfigService getInfraType(String... getInfraType) {
        this.getInfraType.clear();
        this.getInfraType.add(getInfraType);
        return this;
    }

    public MockFacadeInfraConfigService getNitriteDataPath(String... getNitriteDataPath) {
        this.getNitriteDataPath.clear();
        this.getNitriteDataPath.add(getNitriteDataPath);
        return this;
    }

    public MockFacadeInfraConfigService getConnectionString(String... getConnectionString) {
        this.getConnectionString.clear();
        this.getConnectionString.add(getConnectionString);
        return this;
    }

    public MockFacadeInfraConfigService getNitriteDatabaseType(String... getNitriteDatabaseType) {
        this.getNitriteDatabaseType.clear();
        this.getNitriteDatabaseType.add(getNitriteDatabaseType);
        return this;
    }

    @Override
    public InfraConfigService build() throws Exception {

        infraConfigService = applicationContext.getBean(InfraConfigService.class);
        InfraConfigService infraConfigServiceSpy = Mockito.spy(infraConfigService);

        doNothing().when(infraConfigServiceSpy).configure(any());
        doAnswer(getInfraType.answer()).when(infraConfigServiceSpy).getInfraDbType();
        doAnswer(getNitriteDataPath.answer()).when(infraConfigServiceSpy).getInfraDbLocation();
        doAnswer(getNitriteDatabaseType.answer()).when(infraConfigServiceSpy).getInfraDbType();

        return infraConfigServiceSpy;
    }
}
