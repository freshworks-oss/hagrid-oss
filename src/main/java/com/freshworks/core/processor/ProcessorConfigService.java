package com.freshworks.core.processor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.freshworks.core.processor.constants.ProcessorConstants;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.constants.ConnectorConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.io.IOException;

@Component
@Scope(value="prototype")
public class ProcessorConfigService {

    private Resource resource;
    SyncServiceContainer syncServiceContainer;

    public void configure(SyncServiceContainer syncServiceContainer) throws ClassNotFoundException, IllegalAccessException, IOException {
        this.resource = findHagridYaml();
        this.syncServiceContainer = syncServiceContainer;

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
    public int getProcessorPollCount() throws IOException {

        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        JsonNode configJsonNode = objectMapper.readTree(resource.getInputStream());
        return configJsonNode.at(ProcessorConstants.PROCESSOR_POLL_COUNT).asInt();
    }

    public int getNumberOfParallelProcessor() throws IOException {

        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        JsonNode configJsonNode = objectMapper.readTree(resource.getInputStream());
        return configJsonNode.at(ProcessorConstants.NUMBER_OF_PARALLEL_PROCESSOR).asInt();
    }

    public String getAssetLocation() throws IOException {

        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        JsonNode configJsonNode = objectMapper.readTree(resource.getInputStream());
        return configJsonNode.at(ConnectorConstants.ASSET_PATH_IN_HAGRID_CONFIG).asText();
    }

    public String getBeanLocation() throws IOException {

        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        JsonNode configJsonNode = objectMapper.readTree(resource.getInputStream());
        return configJsonNode.at(ConnectorConstants.BEAN_PATH_IN_HAGRID_CONFIG).asText();
    }
}
