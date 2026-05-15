package com.freshworks.core.shared.synchronizers;

import com.freshworks.core.shared.Namespace;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.analytics.AnalyticsFactory;
import com.freshworks.core.shared.analytics.AnalyticsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Slf4j
@Service
public class ServiceTree {

    ServiceNode root;
    SyncServiceContainer syncServiceContainer;
    AnalyticsService analyticsService;

    ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public ServiceTree() {
        root = new ServiceNode("/");
    }


    public void configure(SyncServiceContainer syncServiceContainer){
        this.syncServiceContainer = syncServiceContainer;
        Namespace namespaceService = syncServiceContainer.getBean(Namespace.class);
        AnalyticsFactory analyticsFactory = syncServiceContainer.getBean(AnalyticsFactory.class);
        analyticsService = analyticsFactory.getAnalyticsService(namespaceService.getNamespace());
    }


    public String getParentServicePath(String servicePath){

        if(servicePath.equalsIgnoreCase("/")){
            return "/";
        }

        int lastSlashIndex = servicePath.lastIndexOf('/');

        if(lastSlashIndex != -1){
            return servicePath.substring(0, lastSlashIndex);
        }

        return null;
    }

    public ServiceNode getById(String servicePath){

        try{
            lock.readLock().lock();

            if(servicePath.equalsIgnoreCase("/")){
                return root;
            }

            List<String> servicePathParts = validateServicePath(servicePath);
            ServiceNode serviceNode = findMatchingServiceNodeGivenServicePath(root, servicePathParts, 0);
            return serviceNode;
        }

        finally {

            lock.readLock().unlock();
        }

    }

    public ServiceNode getRoot(){
        return root;
    }

    public void setTerminate(String servicePath){

        try{
            lock.writeLock().lock();
            List<String> servicePathParts = validateServicePath(servicePath);
            ServiceNode serviceNode = findMatchingServiceNodeGivenServicePath(root, servicePathParts, 0);

            if (serviceNode == null) {
                analyticsService.errorEvent("SERVICE_TERMINATION", "service_path" ,servicePath, "_message", "can not terminate service");
                throw new IllegalArgumentException("Can not terminate service: " + servicePath);
            }

            terminateServicePath(serviceNode);
        }

        finally {

            lock.writeLock().unlock();
        }
    }

    public boolean shouldTerminate(String servicePath){

        try{
            lock.readLock().lock();
            List<String> servicePathParts = validateServicePath(servicePath);
            ServiceNode serviceNode = findMatchingServiceNodeGivenServicePath(root, servicePathParts, 0);
            if (serviceNode == null) {
                analyticsService.errorEvent("SERVICE_TERMINATION", "service_path" , servicePath,  "_message", "Can find matching service node");
                throw new IllegalArgumentException("Can find matching service node: " + servicePath);
            }
            return serviceNode.getShouldTerminate();
        }

        finally {
            lock.readLock().unlock();
        }
    }

    /**
     * This method removes the service path and its children forcefully
     * @param servicePath
     */
    public void delete(String servicePath){

        try {

            lock.writeLock().lock();

            if (servicePath.equalsIgnoreCase("/")) {
                throw new IllegalArgumentException("Can not de register ROOT service");
            }

            List<String> servicePathParts = validateServicePath(servicePath);
            List<String> parentServicePathParts = new ArrayList<>(servicePathParts);
            parentServicePathParts.removeLast();

            ServiceNode parentServiceNode = findMatchingServiceNodeGivenServicePath(root, parentServicePathParts, 0);

            if(parentServiceNode != null) {
                HashMap<String, ServiceNode> children = parentServiceNode.getChildren();
                children.remove(servicePathParts.getLast());
            }

        }
        finally {
            lock.writeLock().unlock();
        }
    }

    public void deRegister(String servicePath) throws IllegalArgumentException, IllegalStateException {

        try{
            lock.writeLock().lock();

            if(servicePath.equalsIgnoreCase("/")){
                throw new IllegalArgumentException("Can not de register ROOT service");
            }

            List<String> servicePathParts = validateServicePath(servicePath);
            List<String> parentServicePathParts = new ArrayList<>(servicePathParts);
            parentServicePathParts.removeLast();


            ServiceNode serviceNode = findMatchingServiceNodeGivenServicePath(root, servicePathParts, 0);
            if (serviceNode == null) {
                analyticsService.errorEvent("DE-REGISTRATION", "service_path" ,servicePath, "_message", "Can not de-register service because it does not exists in service tree");
                throw new IllegalArgumentException("Can not de-register service: " + servicePath + " because it doesn't exist in service tree");
            }

            if (Boolean.FALSE.equals(serviceNode.getChildren().isEmpty())){
                analyticsService.errorEvent("DE-REGISTRATION", "service_path" ,servicePath, "_message", "Can not de-register service because child processes are still running");
                throw new IllegalArgumentException("Can not de-register service: " + servicePath + " because there are child processes still running");
            }

            ServiceNode parentServiceNode = findMatchingServiceNodeGivenServicePath(root, parentServicePathParts, 0);

            if (parentServiceNode == null) {
                analyticsService.errorEvent("DE-REGISTRATION", "service_path" ,servicePath, "_message", "Can not de-register service because its parent does not exist in service tree");
                throw new IllegalArgumentException("Can not de-register service: " + servicePath + " because it parent node doesn't exist in service tree");
            }

            deRegisterPrivate(parentServiceNode, servicePathParts.getLast(), servicePath);
        }

        finally {
            lock.writeLock().unlock();
        }

    }


    public void register(String servicePath){

        try{
            lock.writeLock().lock();
            List<String> servicePathParts = validateServicePath(servicePath);
            registerPrivate(root , servicePathParts, 0, servicePath);
        }
        finally {
            lock.writeLock().unlock();
        }
    }


    public int getNumberOfRegisteredKeys(){

        try{
            lock.readLock().lock();
            return getNumberOfRegisteredKeysPrivate(root, 0);
        }
        finally {
            lock.readLock().unlock();
        }
    }

    private int getNumberOfRegisteredKeysPrivate(ServiceNode node, int registeredKeys){

        registeredKeys = registeredKeys + 1;

        if(Boolean.FALSE.equals(node.getChildren().isEmpty())){
            HashMap<String, ServiceNode> children = node.getChildren();
            for (ServiceNode child : children.values()) {
                registeredKeys = getNumberOfRegisteredKeysPrivate(child, registeredKeys);
            }
        }
        return registeredKeys;
    }

    private void registerPrivate(ServiceNode node, List<String> servicePathParts, int index, String servicePath){

        HashMap<String, ServiceNode> children = node.getChildren();
        ServiceNode foundRelevantServiceNode = children.get(servicePathParts.get(index));
        if(foundRelevantServiceNode == null){

            // Then add all remaining child nodes
            for(int i = index; i < servicePathParts.size(); i++){
                node = node.addChild(servicePathParts.get(i));
                analyticsService.debugEvent("REGISTRATION", "service_path" , servicePathParts.subList(0,i));
                analyticsService.meterCounterByIncrement("SERVICE_REGISTER", 1);
            }
        }

        // Check further only if index is less than string length ..
        else if (index < servicePathParts.size() - 1){
            registerPrivate(foundRelevantServiceNode , servicePathParts, index+1, servicePath);
        }
    }

    protected ServiceNode findMatchingServiceNodeGivenServicePath(ServiceNode node, List<String> servicePathParts, int index) {

        if(servicePathParts.isEmpty()){
            return node;
        }

        HashMap<String, ServiceNode> children = node.getChildren();
        ServiceNode foundRelevantServiceNode = children.get(servicePathParts.get(index));

        if (foundRelevantServiceNode != null && index < servicePathParts.size() - 1) {
            foundRelevantServiceNode = findMatchingServiceNodeGivenServicePath(foundRelevantServiceNode, servicePathParts, index + 1);
        }
        else if (foundRelevantServiceNode != null & index == servicePathParts.size() - 1) {
            return foundRelevantServiceNode;
        }

        return foundRelevantServiceNode;
    }

    private void terminateServicePath(ServiceNode node) {

        node.setShouldTerminate(true);
        HashMap<String, ServiceNode> children = node.getChildren();

        for (ServiceNode child : children.values()) {
            terminateServicePath(child);
        }
    }

    protected void deRegisterPrivate(ServiceNode parentNode, String childIdToDeRegister, String servicePath) {

        // Remove this node from parent
        parentNode.getChildren().remove(childIdToDeRegister);
        analyticsService.debugEvent("DE-REGISTRATION", "service_path" , servicePath);
        analyticsService.meterCounterByIncrement("SERVICE_DE_REGISTER", 1);
    }

    protected List<String> validateServicePath(String servicePath) throws IllegalArgumentException{

        String sanitisedServicePath;

        if(servicePath == null){
            analyticsService.errorEvent("SERVICE_PATH_VALIDATION", "service_path" ,servicePath, "_message", "Service path cannot be null");
            throw new IllegalArgumentException("Service path cannot be null");
        }

        if(Boolean.FALSE.equals(isServicePathStartsWithSlash(servicePath))){
            analyticsService.errorEvent("SERVICE_PATH_VALIDATION", "service_path" ,servicePath, "_message", "service path must start with '/'");
            throw new IllegalArgumentException("service path must start with '/'");
        }

        if(Boolean.TRUE.equals(hasConsecutiveForwardSlashWithoutKeys(servicePath))){
            analyticsService.errorEvent("SERVICE_PATH_VALIDATION", "service_path" ,servicePath, "_message", "service path can not have consecutive forward slash like //");
            throw new IllegalArgumentException("service path can not have consecutive forward slash like //");
        }

        sanitisedServicePath = removeProceedingSlashIfPresent(servicePath);
        sanitisedServicePath = removeTrailingSlashIfPresent(sanitisedServicePath);
        return fromStringArray(sanitisedServicePath.split("/"));
    }

    protected boolean isServicePathStartsWithSlash(String servicePath){
        if(servicePath.startsWith("/")){
            return true;
        }
        return false;
    }

    private String removeProceedingSlashIfPresent(String servicePath){
        if(servicePath.startsWith("/")){
            return servicePath.substring(1);
        }
        return servicePath;
    }

    private String removeTrailingSlashIfPresent(String servicePath){
        if(servicePath.endsWith("/")){
            return servicePath.substring(0, servicePath.length() - 1);
        }
        return servicePath;
    }

    private boolean hasConsecutiveForwardSlashWithoutKeys(String servicePath){

        boolean hasConsecutiveForwardSlash = false;

        for(int i = 0; i < (servicePath.length() - 1); i++){
            if( i < servicePath.length() - 2){
                if(servicePath.charAt(i) == '/' && servicePath.charAt(i+1) == '/'){
                    hasConsecutiveForwardSlash = true;
                    return hasConsecutiveForwardSlash;
                }
            }
        }
        return hasConsecutiveForwardSlash;
    }

    private List<String> fromStringArray(String[] strArray){

        List<String> l = new ArrayList<>();
        for(String s : strArray){
            l.add(s);
        }
        return l;
    }



}
