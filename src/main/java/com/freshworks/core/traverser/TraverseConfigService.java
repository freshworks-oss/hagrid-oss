package com.freshworks.core.traverser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.constants.ConnectorConstants;
import com.freshworks.core.traverser.Annotations.FreshHierarchy;
import com.freshworks.core.traverser.constants.TraverserConstants;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.reflections.scanners.Scanners.SubTypes;

@Component
@Scope(value="prototype")
public class TraverseConfigService {

    private Resource resource;

    private ObjectNode configurationNode;
    private ReentrantReadWriteLock rateLimitReentrantReadWriteLock = new ReentrantReadWriteLock();
    private SyncServiceContainer syncServiceContainer;

    public TraverseConfigService(){

        ObjectMapper mapper = new ObjectMapper();
        configurationNode = mapper.createObjectNode();
    }


    public void configure(SyncServiceContainer syncServiceContainer) throws ClassNotFoundException, IllegalAccessException, IOException {
        this.resource = findHagridYaml();
        this.syncServiceContainer = syncServiceContainer;
        configTraverserThreadCount();
        configStepLocation();
        configureBeanLocation();
        configureRateLimit();
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

    public int getTraverserThreadCount() throws IOException {

        return configurationNode.get("traverser_thread_count").asInt();
    }

    private void configTraverserThreadCount() throws IOException {

        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        JsonNode configJsonNode = objectMapper.readTree(resource.getInputStream());
        int traverserThreadCount = configJsonNode.at(TraverserConstants.TRAVERSER_THREAD_COUNT).asInt();
        configurationNode.put("traverser_thread_count", traverserThreadCount);
    }

    public void setTraverserThreadCount(int traverserThreadCount) throws IOException {
        configurationNode.put("traverser_thread_count", traverserThreadCount);
    }

    public String getStepLocation() throws IOException {

        return configurationNode.get("step_path").asText();
    }

    private void configStepLocation() throws IOException {

        // Check if this configuration node is already set or not.
        // If it is already set then do not pick the default one.
        if(configurationNode.get("step_path") == null){
            ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
            JsonNode configJsonNode = objectMapper.readTree(resource.getInputStream());
            String stepPath = configJsonNode.at(ConnectorConstants.STEP_PATH_IN_HAGRID_CONFIG).asText();
            configurationNode.put("step_path", stepPath);
        }
    }

    public void setStepLocation(String stepPath) throws IOException, ClassNotFoundException, IllegalAccessException {
        configurationNode.put("step_path", stepPath);
        configureRateLimit();
    }

    public String getBeanLocation() throws IOException {

        return configurationNode.get("bean_path").asText();
    }

    public void setBeanLocation(String beanLocation) throws IOException, ClassNotFoundException, IllegalAccessException {
        configurationNode.put("bean_path", beanLocation);
    }

    private void configureBeanLocation() throws IOException {

        // Check if this configuration node is already set or not.
        // If it is already set then do not pick the default one.
        if(configurationNode.get("bean_path") == null){
            ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
            JsonNode configJsonNode = objectMapper.readTree(resource.getInputStream());
            String bean_path = configJsonNode.at(ConnectorConstants.BEAN_PATH_IN_HAGRID_CONFIG).asText();
            configurationNode.put("bean_path", bean_path);
        }
    }


    private void configureRateLimit() throws ClassNotFoundException, IllegalAccessException {

        String stepPath = configurationNode.get("step_path").asText();
        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forPackage(stepPath)));

        Set<Class<?>> set = reflections.get(SubTypes.of(AbstractStep.class).asClass());

        for (Class<?> aClass : set) {
            Class<?> clazz = Class.forName(aClass.getName(), false, TraverserUtility.class.getClassLoader());
            FreshHierarchy freshHierarchy = clazz.getAnnotation(FreshHierarchy.class);

            if (freshHierarchy != null && !aClass.getName().toLowerCase().contains("ParentStep")) {
                setRateLimitForStep((Class<? extends AbstractStep>) aClass, freshHierarchy.rateLimit(), freshHierarchy.duration());
            }
        }
    }


    public void setRateLimitForStep(Class<? extends AbstractStep> stepClass, int numberOfAPICalls, int seconds) throws IllegalAccessException {

        try{
            rateLimitReentrantReadWriteLock.writeLock().lock();
            ObjectMapper objectMapper = new ObjectMapper();
            boolean isRateLimitNodeAvailable = configurationNode.has("rateLimit");

            if(isRateLimitNodeAvailable){

                ObjectNode rateLimitNode = (ObjectNode) configurationNode.get("rateLimit");
                ObjectNode rateLimitNodeForThisStep = objectMapper.createObjectNode();
                rateLimitNodeForThisStep.put("api_count", numberOfAPICalls);
                rateLimitNodeForThisStep.put("seconds", seconds);
                rateLimitNode.put(stepClass.getName(), rateLimitNodeForThisStep);
            }
            else{
                ObjectNode rateLimitNode = objectMapper.createObjectNode();
                configurationNode.put("rateLimit", rateLimitNode);
                ObjectNode rateLimitNodeForThisStep = objectMapper.createObjectNode();
                rateLimitNodeForThisStep.put("api_count", numberOfAPICalls);
                rateLimitNodeForThisStep.put("seconds", seconds);
                rateLimitNode.put(stepClass.getName(), rateLimitNodeForThisStep);
            }
        }
        catch(Exception e){
            e.printStackTrace();
        }
        finally {
            rateLimitReentrantReadWriteLock.writeLock().unlock();
        }
    }

    public JsonNode getRateLimitForStep(Class<? extends AbstractStep> stepClass){

        boolean isRateLimitNodeAvailable = configurationNode.has("rateLimit");

        if(isRateLimitNodeAvailable){
            ObjectNode rateLimitNode = (ObjectNode) configurationNode.get("rateLimit");
            boolean isRateLimitForThisStepAvailable = rateLimitNode.has(stepClass.getName());
            if(isRateLimitForThisStepAvailable){
                return rateLimitNode.get(stepClass.getName());
            }
            else{
                return null;
            }
        }
        else{
            return null;
        }
    }

    public JsonNode getConfigurationNode(){
        return configurationNode;
    }
}
