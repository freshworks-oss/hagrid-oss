package com.freshworks.core.shared.infra;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.constants.InfraConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
@Component
public class InfraConfigService {

    private static final Pattern ENV_VAR_PATTERN = Pattern.compile("\\$\\{([^}]+)}");

    private Resource resource;
    SyncServiceContainer syncServiceContainer;

    public void configure(SyncServiceContainer syncServiceContainer) throws IOException {
        this.syncServiceContainer = syncServiceContainer;
        this.resource = findHagridYaml();
    }

    private Resource findHagridYaml() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        String profile = System.getProperty("spring.profiles.active", "");
        String fileName = "hagrid" + (profile.isEmpty() ? "" : profile) + ".yaml";
        Resource[] resources = resolver.getResources("classpath*:**/" + fileName);
        if (resources.length > 0) {
            return resources[0];
        } else {
            throw new FileNotFoundException(fileName + " not found in classpath");
        }
    }

    public String getDatabaseUserName() throws IOException {

        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        JsonNode configJsonNode = objectMapper.readTree(resource.getInputStream());
        String environment = configJsonNode.at(InfraConstants.INFRA_PATH+InfraConstants.ENVIRONMENT).asText();
        String infraType = configJsonNode.at(InfraConstants.INFRA_PATH +InfraConstants.INFRA_TYPE).asText();
        String usernamePath = InfraConstants.INFRA_PATH + "/" + infraType + "/" +  environment + InfraConstants.DATABASE_USERNAME_IN_HAGRID_CONFIG;
        String username  = configJsonNode.at(usernamePath).asText();
        if(username.contains("${")){
            return System.getenv(username.substring(2,username.length()-1));
        }
        return username;
    }

    public String getDatabasePassword() throws IOException {

        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        JsonNode configJsonNode = objectMapper.readTree(resource.getInputStream());
        String environment = configJsonNode.at(InfraConstants.INFRA_PATH+InfraConstants.ENVIRONMENT).asText();
        String infraType = configJsonNode.at(InfraConstants.INFRA_PATH+InfraConstants.INFRA_TYPE).asText();
        String passwordPath = InfraConstants.INFRA_PATH + "/" + infraType + "/" +  environment + InfraConstants.DATABASE_PASSWORD_IN_HAGRID_CONFIG;
        String password  = configJsonNode.at(passwordPath).asText();
        //Getting password from environment variable
        if(password.contains("${")){
            return System.getenv(password.substring(2,password.length()-1));
        }
        return password;
    }

    public String getDatabaseAuthDb() throws IOException {

        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        JsonNode configJsonNode = objectMapper.readTree(resource.getInputStream());
        String environment = configJsonNode.at(InfraConstants.INFRA_PATH+InfraConstants.ENVIRONMENT).asText();
        String infraType = configJsonNode.at(InfraConstants.INFRA_PATH+InfraConstants.INFRA_TYPE).asText();
        String authDbPath = InfraConstants.INFRA_PATH + "/" + infraType + "/" +  environment + InfraConstants.DATABASE_AUTHDB_IN_HAGRID_CONFIG;
        String authdb = configJsonNode.at(authDbPath).asText();
        if(authdb.contains("${")){
            return System.getenv(authdb.substring(2,authdb.length()-1));
        }
        return authdb;
    }

    public String getDatabaseHost() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        JsonNode configJsonNode = objectMapper.readTree(resource.getInputStream());
        String environment = configJsonNode.at(InfraConstants.INFRA_PATH+InfraConstants.ENVIRONMENT).asText();
        String infraType = configJsonNode.at(InfraConstants.INFRA_PATH+InfraConstants.INFRA_TYPE).asText();
        String hostPath = InfraConstants.INFRA_PATH + "/" + infraType + "/" +  environment + InfraConstants.DATABASE_HOST_IN_HAGRID_CONFIG;
        String host = configJsonNode.at(hostPath).asText();
        if(host.contains("${")){
            return System.getenv(host.substring(2,host.length()-1));
        }
        return host;
    }

    public String getAdditionalParams() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        JsonNode configJsonNode = objectMapper.readTree(resource.getInputStream());
        String environment = configJsonNode.at(InfraConstants.INFRA_PATH+InfraConstants.ENVIRONMENT).asText();
        String infraType = configJsonNode.at(InfraConstants.INFRA_PATH+InfraConstants.INFRA_TYPE).asText();
        String additionalParamsPath = InfraConstants.INFRA_PATH + "/" + infraType + "/" +  environment + InfraConstants.DATABASE_ADDITIONAL_PARAMS_IN_HAGRID_CONFIG;
        String additionalParams = configJsonNode.at(additionalParamsPath).asText();
        if(additionalParams.contains("${")){
            return System.getenv(additionalParams.substring(2,additionalParams.length()-1));
        }
        return additionalParams;
    }

    /**
     * Returns the full connection_string from hagrid.yaml if configured, or null if not present.
     * When set, this takes priority over individual host/port/params fields.
     */
    public String getConnectionString() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        JsonNode configJsonNode = objectMapper.readTree(resource.getInputStream());
        String environment = configJsonNode.at(InfraConstants.INFRA_PATH + InfraConstants.ENVIRONMENT).asText();
        String infraType = configJsonNode.at(InfraConstants.INFRA_PATH + InfraConstants.INFRA_TYPE).asText();
        String connectionStringPath = InfraConstants.INFRA_PATH + "/" + infraType + "/" + environment + InfraConstants.DATABASE_CONNECTION_STRING_IN_HAGRID_CONFIG;
        JsonNode node = configJsonNode.at(connectionStringPath);
        if (node.isMissingNode() || node.asText().isEmpty()) {
            return null;
        }
        String connectionString = node.asText();
        // Single-pass expansion of ${VAR} placeholders to avoid infinite loops
        // if an env var value itself contains ${...}
        Matcher matcher = ENV_VAR_PATTERN.matcher(connectionString);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String varName = matcher.group(1);
            String envValue = System.getenv(varName);
            if (envValue == null) envValue = "";
            matcher.appendReplacement(sb, Matcher.quoteReplacement(envValue));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    public Integer getDatabasePort() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        JsonNode configJsonNode = objectMapper.readTree(resource.getInputStream());
        String environment = configJsonNode.at(InfraConstants.INFRA_PATH+InfraConstants.ENVIRONMENT).asText();
        String infraType = configJsonNode.at(InfraConstants.INFRA_PATH+InfraConstants.INFRA_TYPE).asText();
        String portPath = InfraConstants.INFRA_PATH + "/" + infraType + "/" + environment + InfraConstants.DATABASE_PORT_IN_HAGRID_CONFIG;

        String port = configJsonNode.at(portPath).asText();
        if(port.contains("${")){
            return Integer.valueOf(System.getenv(port.substring(2,port.length()-1)));
        }
        return Integer.valueOf(port);
    }

    public Boolean isTestModeEnabled() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        JsonNode configJsonNode = objectMapper.readTree(resource.getInputStream());
        String testModePath = InfraConstants.TEST_MODE_IN_HAGRID_CONFIG;
        return configJsonNode.at(testModePath).asBoolean();
    }

    public String getTestModeHost() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        JsonNode configJsonNode = objectMapper.readTree(resource.getInputStream());
        String testModeHostPath = InfraConstants.TEST_MODE_HOST_IN_HAGRID_CONFIG;
        return configJsonNode.at(testModeHostPath).asText();
    }

    public String getInfraType() throws IOException {

        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        JsonNode configJsonNode = objectMapper.readTree(resource.getInputStream());
        String infraType = configJsonNode.at( InfraConstants.INFRA_PATH + InfraConstants.INFRA_TYPE).asText();

        return infraType;
    }

    public String getH2HostAddress() throws IOException {

        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        JsonNode configJsonNode = objectMapper.readTree(resource.getInputStream());
        String h2HostAddress = configJsonNode.at( InfraConstants.INFRA_PATH + InfraConstants.H2_HOST_ADDRESS).asText();

        return h2HostAddress;
    }

    public String getH2DataPath() throws IOException {

        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        JsonNode configJsonNode = objectMapper.readTree(resource.getInputStream());
        String h2DataPath = configJsonNode.at( InfraConstants.INFRA_PATH + InfraConstants.H2_DATA_PATH).asText();

        return h2DataPath;
    }

    public String getH2DatabaseType() throws IOException {

        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        JsonNode configJsonNode = objectMapper.readTree(resource.getInputStream());
        String h2DatabaseType = configJsonNode.at( InfraConstants.INFRA_PATH + InfraConstants.H2_DATABASE_TYPE).asText();

        return h2DatabaseType;
    }
}


