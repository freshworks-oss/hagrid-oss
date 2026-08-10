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

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Component
@Getter
@Setter
@Slf4j
public class DagScannerService {

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
                trimDagNodes(clonedDagNode, connectorConfiguration.getAllowedAbstractStep());
                init(clonedDagNode.preOrder(), infraService);
                return clonedDagNode.preOrder().get(0);
            }

            AnalyticsService analyticsService = analyticsFactory.getAnalyticsService(namespace);
            rootNode = scanner(traverseConfigService, analyticsService);
            DagNode clonedDagNode = cloneDag(rootNode);

            // Below I am removing nodes which are switched Off by the customer
            trimDagNodes(clonedDagNode, connectorConfiguration.getAllowedAbstractStep());
            init(clonedDagNode.preOrder(), infraService);
            return clonedDagNode.preOrder().get(0);

        }

        finally {
            uniqueScan.unlock();
        }
    }


    public DagNode scanner(TraverseConfigService traverseConfigService, AnalyticsService analyticsService) throws ClassNotFoundException, IOException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {

        List<String> dagNodeShortNameTempStorage = new ArrayList();
        DagNode treeNode =   createDAG(abstractStepList, analyticsService);

        List<DagNode> nodeList = treeNode.preOrder();

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

        Class<?> clazz = Class.forName(clazzName, false, DagScannerService.class.getClassLoader());
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

    private void trimDagNodes(DagNode rootNode, List<AbstractStep> allowedAbstractStep){

        List<DagNode> nodesToDrop = new ArrayList<>();
        List<String> allowedSteps = new ArrayList<>();

        for(AbstractStep step : allowedAbstractStep){
            allowedSteps.add(step.getClass().getName());
        }

        // If allowed steps is not empty then allow only these steps else all steps will be allowed
        if(Boolean.FALSE.equals(allowedSteps.isEmpty())){
            
            Iterator<DagNode> it = rootNode.preOrder().iterator();

            while(it.hasNext()){

                DagNode node = it.next();

                if(Boolean.FALSE.equals(node.getName().equalsIgnoreCase(ParentStep.class.getName())) && Boolean.FALSE.equals(allowedSteps.contains(node.getName()))){

                    nodesToDrop.add(node);
                }
            }
        
            for(DagNode node: nodesToDrop){

                // Drop the sub-tree
                rootNode.dropSubtree(node);
            }
        }
    }
}
