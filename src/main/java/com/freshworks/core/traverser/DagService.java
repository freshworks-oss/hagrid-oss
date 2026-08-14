package com.freshworks.core.traverser;

import static org.reflections.scanners.Scanners.SubTypes;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.checkerframework.checker.units.qual.s;
import org.reflections.Reflections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.freshworks.core.shared.NamespaceService;
import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.analytics.AnalyticsFactory;
import com.freshworks.core.shared.analytics.AnalyticsService;
import com.freshworks.core.shared.infra.InfraService;
import com.freshworks.core.shared.sync.ConnectorConfiguration;
import com.freshworks.core.traverser.Annotations.CustomDagNode;
import com.freshworks.core.traverser.Annotations.FreshHierarchy;
import com.freshworks.core.traverser.NodeRelationship.REL_SWITCH;
import com.google.common.collect.Lists;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Component
@Getter
@Setter
@Slf4j
public class DagService {

    @Autowired
    List<AbstractStep> abstractStepList;

    SyncServiceContainer syncServiceContainer;

    ReentrantReadWriteLock.WriteLock uniqueScan = new ReentrantReadWriteLock().writeLock();
    DagNode rootNode = null;
    AnalyticsFactory analyticsFactory;
    NamespaceService namespaceService;
    AnalyticsService analyticsService;
    ConnectorConfiguration connectorConfiguration;

    public void configure(SyncServiceContainer syncServiceContainer){
        this.syncServiceContainer = syncServiceContainer;
        this.namespaceService = this.syncServiceContainer.getBean(NamespaceService.class);
        this.analyticsFactory = this.syncServiceContainer.getBean(AnalyticsFactory.class);
        this.analyticsService = this.analyticsFactory.getAnalyticsService(this.namespaceService.getNamespace());
        this.connectorConfiguration = this.syncServiceContainer.getBean(ConnectorConfiguration.class);
    }


    public DagNode dagScanner(String namespace, TraverseConfigService traverseConfigService, InfraService infraService) throws Exception {

        try{
            uniqueScan.lock();
            // If Dag for the given stepLocation already exists then do not create it again
            if(rootNode != null){
                DagNode clonedDagNode = cloneDag(rootNode);

                // Below I am removing nodes which are switched Off by the customer
                enableDisableDagPath(clonedDagNode, connectorConfiguration.getEnabledDagPathList());
                init(clonedDagNode.getNodesInDag(), infraService);
                return clonedDagNode.getNodesInDag().get(0);
            }

            AnalyticsService analyticsService = analyticsFactory.getAnalyticsService(namespace);
            rootNode = scanner(traverseConfigService, analyticsService);
            DagNode clonedDagNode = cloneDag(rootNode);

            // Below I am removing nodes which are switched Off by the customer
            enableDisableDagPath(clonedDagNode, connectorConfiguration.getEnabledDagPathList());
            init(clonedDagNode.getNodesInDag(), infraService);
            return clonedDagNode.getNodesInDag().get(0);

        }

        finally {
            uniqueScan.unlock();
        }
    }


    public DagNode scanner(TraverseConfigService traverseConfigService, AnalyticsService analyticsService) throws ClassNotFoundException, IOException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {

        List<String> dagNodeShortNameTempStorage = new ArrayList();
        DagNode treeNode =   createDAG(abstractStepList, analyticsService);

        List<DagNode> nodeList = treeNode.getNodesInDag();

        for (DagNode node : nodeList) {
            String dagShortName = generateShortName(node.getName(), dagNodeShortNameTempStorage);
            node.setShortName(dagShortName);
        }

        return  treeNode;
    }


    protected Set<Class<? extends DagNode>> getNodes(Reflections reflections, String nodePath) throws IOException {
        Set<Class<? extends DagNode>> result = new HashSet<>();
        Set<Class<?>> set = reflections.get(SubTypes.of(DagNode.class).asClass());
        for (Class<?> clazz : set)
        {
            if (clazz.getName().contains(nodePath))
            {
                result.add((Class<? extends DagNode>) clazz);
            }
        }
        return result;
    }

    protected DagNode createDAG(List<AbstractStep> abstractStepList, AnalyticsService analyticsService) throws ClassNotFoundException, IOException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {

        HashMap<String, DagNode> nodeNameWithNodeObjectMap = new HashMap<>();
        DagNode root = new DagNode(ParentStep.class.getName());

        Set<Class<? extends AbstractStep>> steps = new HashSet<>();
        
        for(AbstractStep abstractStep : abstractStepList){
            steps.add(abstractStep.getClass());
        }

        // Go through each step
        for(Class<? extends AbstractStep> clazz : steps) {

            if(isStepIgnored(clazz.getName())){
                continue;
            }

            if(clazz.getName().equals(ParentStep.class.getName())){
                continue;
            }

            DagNode childNode = null;
            if(!nodeNameWithNodeObjectMap.containsKey(clazz.getName())) {
                // We can create this node here

                // check if custom node class is present
                childNode = getCustomNodeObject(clazz);
                if(childNode == null) {
                    childNode = new DagNode(clazz.getName());
                }
                nodeNameWithNodeObjectMap.put(clazz.getName(), childNode);
            }
            else{
                childNode = nodeNameWithNodeObjectMap.get(clazz.getName());
            }

            List<Class<?>> parentClasses = getParentClass(clazz);

            for(Class<?> parentClass : parentClasses) {
                DagNode parentNode = null;
                if(!nodeNameWithNodeObjectMap.containsKey(parentClass.getName()) && !parentClass.getName().equalsIgnoreCase(ParentStep.class.getName())) {

                    parentNode = getCustomNodeObject(parentClass);
                    if(parentNode == null) {
                        parentNode = new DagNode(parentClass.getName());
                    }
                    nodeNameWithNodeObjectMap.put(parentClass.getName(), parentNode);
                }
                else if(parentClass.getName().equalsIgnoreCase(ParentStep.class.getName())){
                    // It is the case when parentStep of the class is ParentStep
                    // Then assign root node directly
                    parentNode = root;
                }
                else{
                    parentNode = nodeNameWithNodeObjectMap.get(parentClass.getName());
                }
                parentNode.addChild(childNode);
            }
        }

        return root;

    }

    private static boolean isStepIgnored(String clazzName) throws ClassNotFoundException {

        Class<?> clazz = Class.forName(clazzName, false, DagService.class.getClassLoader());
        FreshHierarchy freshHierarchy = clazz.getAnnotation(FreshHierarchy.class);
        return freshHierarchy.ignore();
    }

    private List<Class<?>> getParentClass(Class<?> clazz){

        FreshHierarchy freshHierarchy = clazz.getAnnotation(FreshHierarchy.class);
        return List.of(freshHierarchy.parentClass());
    }

    public String generateShortName(String name, List<String> existingNameList) {

        String newSimpleClassName = "";

        if(name.equalsIgnoreCase(ParentStep.class.getName()))
            return "steps.Parent";


        String[] classNameParts = name.split("\\.");
        String simpleClassName = classNameParts[classNameParts.length - 1];


        if(existingNameList.contains(simpleClassName)){

            String package_name = "";
            boolean canCreateShortName = false;
            for(int i = classNameParts.length - 2; i >=0 ; i--){

                if(package_name.equals("")){
                    package_name = classNameParts[i];
                }

                else{
                    package_name = classNameParts[i] + "." + package_name;
                }
                
                newSimpleClassName = "steps." + "." + package_name + "." + simpleClassName;

                if(Boolean.FALSE.equals(existingNameList.contains(newSimpleClassName))){
                    existingNameList.add(newSimpleClassName);
                    canCreateShortName = true;
                    break;
                }
            }

            if(Boolean.FALSE.equals(canCreateShortName)){
                throw new IllegalStateException("Unable to create short names of the steps. It should not happen as short form is created using packages. Check code here");
            }

            return newSimpleClassName;
        }

        else{

            newSimpleClassName = "steps." + simpleClassName;
            existingNameList.add(newSimpleClassName);
            return newSimpleClassName;
        }
    }

    private DagNode getCustomNodeObject(Class<?> clazz) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {


        CustomDagNode customDagNode = clazz.getAnnotation(CustomDagNode.class);
        if(customDagNode != null){
            Class<? extends DagNode> customDagNodeClazz = customDagNode.parentClass();
            return customDagNodeClazz.getConstructor(String.class).newInstance(clazz.getName());
        }
        return null;
    }

    private void init(List<DagNode> dagNodeList, InfraService infraService) throws Exception{

        for (DagNode node : dagNodeList) {
            node.configInfra(infraService.getInfraDbList(node.getShortName()), infraService.getKeyValue());
        }
    }

    public DagNode cloneDag(DagNode rootNode) throws Exception{
        return DagNode.cloneDag(rootNode);
    }

    protected void enableDisableDagPath(DagNode rootNode, List<List<Class<? extends AbstractStep>>> allowedAbstractStep) throws Exception{


        List<List<String>> allowedStepMultiList = new ArrayList<>();

        for(List<Class<? extends AbstractStep>> stepList : allowedAbstractStep){

            List<String> allowedStepList = new ArrayList<>();

            for(Class<? extends AbstractStep> step : stepList){

                allowedStepList.add(step.getName());
            }

            allowedStepMultiList.add(allowedStepList);
        }

        // Only allowedStepMultiList will be allowed, otherwise all other path would be off 

        for(List<String> path : allowedStepMultiList){

            markRelationshipFeatureSwitchOn(rootNode, path);
            switchOffRelationshipWhichAreNotMarked(rootNode);
        }
        
    }

    protected void markRelationshipFeatureSwitchOn(DagNode rootNode, List<String> path) throws Exception{


        // For a given path, we must validate the first node, should be a node, which can be triggered
        // Check below how it is being done
        DagNode parentNode = null;

        for(int i=0; i < path.size(); i++){

        if (i == 0){

            // special check for 0th node of the path
            parentNode = rootNode.find(path.get(0));

            // Check if its parent is ParentStep.class
            if(parentNode.isInParentList(rootNode)){

                NodeRelationship nodeRelationship = parentNode.getParentRelationship(rootNode);
                nodeRelationship.enableFeature("should_be_enabled");

            }
        }

            // For other nodes in the path 
            else{

                DagNode chilNode = rootNode.find(path.get(i));
                NodeRelationship relationshipMap = chilNode.getParentRelationship(parentNode);
                relationshipMap.enableFeature("should_be_enabled");
                parentNode = chilNode;
            }
        }
    }

    protected void switchOffRelationshipWhichAreNotMarked(DagNode rootNode) throws Exception{

        List<DagNode> allDagNodeList = rootNode.getNodesInDag();

        for(DagNode node: allDagNodeList){

            
            if(node.getName().equalsIgnoreCase(ParentStep.class.getName())){

                // Do not do anything as this node is the root Node
            }

            else{


                List<NodeRelationship> nodeRelationshipList = Lists.newArrayList(node.getParentRelationshipMap().values());

                for(NodeRelationship relationship : nodeRelationshipList){

                    if(Boolean.FALSE.equals(relationship.hasFeature("should_be_enabled"))){

                        relationship.setRelSwitch(REL_SWITCH.OFF);
                    }

                    else{

                        // Clear feature flag
                        relationship.clearFeature("should_be_enabled");
                    }
                }
            }
        }
    }
}
