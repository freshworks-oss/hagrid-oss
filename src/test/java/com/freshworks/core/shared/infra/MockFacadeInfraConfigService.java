package com.freshworks.core.shared.infra;

import com.freshworks.core.MockFacadeInterface;
import com.freshworks.core.ReturnableMockTypeList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.stereotype.Component;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;

@Component
public class MockFacadeInfraConfigService implements MockFacadeInterface {

    @SpyBean
    InfraConfigService infraConfigServiceSpy;

    ReturnableMockTypeList<String> getDatabaseUserName;

    ReturnableMockTypeList<String> getDatabasePassword;

    ReturnableMockTypeList<String> getDatabaseAuthDb;

    ReturnableMockTypeList<String> getDatabaseHost;

    ReturnableMockTypeList<String> getConnectionString;

    ReturnableMockTypeList<String> getAdditionalParams;

    ReturnableMockTypeList<Integer> getDatabasePort;

    ReturnableMockTypeList<String> getInfraType;
    ReturnableMockTypeList<String> getH2DataPath;

    ReturnableMockTypeList<String> getH2DatabaseType;


    @Override
    public MockFacadeInfraConfigService configure(){

        reset();
        getDatabaseUserName.add("admin");
        getDatabasePassword.add("password12345");
        getDatabaseAuthDb.add("admin");
        getDatabaseHost.add("localhost");
        getAdditionalParams.add("dummy-additional-param=dummy-additional-value");
        getDatabasePort.add(27017);
        getInfraType.add("persistent");
        getH2DataPath.add("/Users/aaggarwal/Documents/hagrid-releases/data/hagrid-3.7.0/some-database");
        getH2DatabaseType.add("file");
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

    public MockFacadeInfraConfigService getH2DataPath(String... getH2DataPath) {
        this.getH2DataPath.clear();
        this.getH2DataPath.add(getH2DataPath);
        return this;
    }

    public MockFacadeInfraConfigService getConnectionString(String... getConnectionString) {
        this.getConnectionString.clear();
        this.getConnectionString.add(getConnectionString);
        return this;
    }

    public MockFacadeInfraConfigService getH2DatabaseType(String... getH2DatabaseType) {
        this.getH2DatabaseType.clear();
        this.getH2DatabaseType.add(getH2DatabaseType);
        return this;
    }

    @Override
    public InfraConfigService build() throws Exception {

        doNothing().when(infraConfigServiceSpy).configure(any());
        doAnswer(getDatabaseUserName.answer()).when(infraConfigServiceSpy).getDatabaseUserName();
        doAnswer(getDatabasePassword.answer()).when(infraConfigServiceSpy).getDatabasePassword();
        doAnswer(getDatabaseAuthDb.answer()).when(infraConfigServiceSpy).getDatabaseAuthDb();
        doAnswer(getDatabaseHost.answer()).when(infraConfigServiceSpy).getDatabaseHost();
        doAnswer(getAdditionalParams.answer()).when(infraConfigServiceSpy).getAdditionalParams();
        doAnswer(getDatabasePort.answer()).when(infraConfigServiceSpy).getDatabasePort();
        doAnswer(getInfraType.answer()).when(infraConfigServiceSpy).getInfraType();
        doAnswer(getH2DataPath.answer()).when(infraConfigServiceSpy).getH2DataPath();
        doAnswer(getH2DatabaseType.answer()).when(infraConfigServiceSpy).getH2DatabaseType();
        doAnswer(getConnectionString.answer()).when(infraConfigServiceSpy).getConnectionString();

        return infraConfigServiceSpy;
    }
}
