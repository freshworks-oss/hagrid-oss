package com.freshworks.core.traverser;

import com.freshworks.core.shared.analytics.AnalyticsService;
import com.freshworks.core.traverser.Annotations.CustomDagNode;
import com.freshworks.core.traverser.Annotations.FreshHierarchy;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import static org.reflections.scanners.Scanners.SubTypes;

@Component
@Getter
@Setter
@Slf4j
public class DagScannerService {

    public DagNode scanner(TraverseConfigService traverseConfigService, AnalyticsService analyticsService) throws ClassNotFoundException, IOException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {

        HashMap<String, String> dagNodeShortNameTempStorage = new HashMap<>();
        String stepPath = traverseConfigService.getStepLocation();
//      Creation of the DAG for the given connector
        Reflections  reflectionForSteps = new Reflections(new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forPackage(stepPath)));

        analyticsService.debugLogEvent("HAGRID_DAG_SCANNER_SERVICE", "step_path", stepPath);
        DagNode treeNode =   createDAG(reflectionForSteps, stepPath, analyticsService);

        List<DagNode> nodeList = treeNode.preOrder();

        for (DagNode node : nodeList) {
            String dagShortName = generateShortName(node.getName(), stepPath, dagNodeShortNameTempStorage);
            node.setShortName(dagShortName);
        }

        analyticsService.debugLogEvent("HAGRID_DAG_SCANNER_SERVICE", "step_path", stepPath);
        return  treeNode;
    }


    protected Set<Class<?>> getSteps(Reflections reflections, String stepPath) throws IOException {
        Set<Class<?>> result = new HashSet<>();
        Set<Class<?>> set = reflections.get(SubTypes.of(AbstractStep.class).asClass());
        for (Class<?> clazz : set)
        {
         if (clazz.getName().contains(stepPath))
            {
                result.add(clazz);
            }
        }
        return result;
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

    // TODO: Create a DAG which consider FreshJoin as well to decide which child should be left and which one should be right
//    protected DagNode createDAG(Reflections reflections, String stepPath, AnalyticsService analyticsService) throws ClassNotFoundException, IOException {
//
//        HashMap<String, DagNode> branchMap = new HashMap<>();
//
//        DagNode root = new DagNode(ParentStep.class.getName());
//        analyticsService.debugEvent("HAGRID_DAG_SCANNER_SERVICE", "step_path", stepPath, "step", ParentStep.class.getName());
//
//        Set<Class<?>> steps =
//                getSteps(reflections, stepPath);
//
//        for (Class<?> step: steps) {
//
//            if(step.getName().equals(ParentStep.class.getName())){
//                continue;
//            }
//            Class<?> parentClass = getParentClass(step);
//
//            if(parentClass != null){
//                if ( branchMap.containsKey(parentClass.getName())){
//                    branchMap.get(parentClass.getName()).add(new DagNode(step.getName()));
//                }
//                else{
//                    DagNode  parent = new DagNode(parentClass.getName());
//                    parent.add(new DagNode(step.getName()));
//                    branchMap.put(parent.getName(), parent);
//                }
//            }
//            else{
//                root.setName(step.getName());
//            }
//        }
//
//        Iterator<Map.Entry<String , DagNode>> iterator = branchMap.entrySet().iterator();
//
//        while(!branchMap.isEmpty()){
//            if(!iterator.hasNext())
//                iterator = branchMap.entrySet().iterator();
//
//            Map.Entry<String, DagNode> entry = iterator.next();
//            DagNode node = root.find(entry.getValue().getName());
//
//            if(node != null) {
//                for( DagNode v : entry.getValue().getSubtree()){
//                    node.add(v);
//                }
//                iterator.remove();
//            }
//        }
//
//        // Here filter the DAG to remove the subtrees where it has ignore true
//
//        Iterator<DagNode> it = root.preOrder().iterator();
//        DagNode node = it.next();
//        ArrayList<DagNode> nodeArrayList = new ArrayList<>();
//
//        while(it.hasNext()){
//            node = it.next();
//            if(Boolean.FALSE.equals(node.getName().equals(ParentStep.class.getName()))){
//                boolean isStepIgnored = isStepIgnored(node.getName());
//                if(isStepIgnored){
//                    nodeArrayList.add(node);
//                }
//                analyticsService.debugEvent("HAGRID_DAG_SCANNER_SERVICE", "step_path", stepPath, "step", node.getName());
//            }
//        }
//
//        for (int i = 0; i < nodeArrayList.size(); i++) {
//            node = nodeArrayList.get(i);
//            DagNode parent = node.getParentList();
//            parent.dropSubtree(nodeArrayList.get(i));
//            analyticsService.debugEvent("HAGRID_DAG_SCANNER_SERVICE", "step_path", stepPath, "step", node.getName());
//        }
//
//        return root;
//    }

    protected DagNode createDAG(Reflections stepReflections, String stepPath, AnalyticsService analyticsService) throws ClassNotFoundException, IOException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {

        HashMap<String, DagNode> nodeNameWithNodeObjectMap = new HashMap<>();
        DagNode root = new DagNode(ParentStep.class.getName());
        analyticsService.debugLogEvent("HAGRID_DAG_SCANNER_SERVICE", "step_path", stepPath, "step", ParentStep.class.getName());

        Set<Class<?>> steps = getSteps(stepReflections, stepPath);

        // Go through each step
        for(Class<?> clazz : steps) {

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

    public String generateShortName(String name , String stepPath , Map<String, String> shortNameMap) {
        if(name.equalsIgnoreCase(ParentStep.class.getName()))
            return "steps.Parent";
        String replace = name.replace(stepPath, "");
        String uniqueClassName = replace.substring(1);
        String duplicateKey = checkForDuplicate(uniqueClassName, shortNameMap);
        if(!"".equalsIgnoreCase(duplicateKey)){
            return "steps."+duplicateKey;
        }
        String key = generateKey(uniqueClassName, shortNameMap);
        shortNameMap.put(key , uniqueClassName);
        return "steps."+ key;
    }

    public  String generateKey(String input , Map<String, String> shortNameMap) {
        String[] parts = input.split("\\.");
        StringBuilder key = new StringBuilder();
        int[] charsToPick = new int[parts.length];

        // Initialize the number of characters to pick for each part (except the last)
        for (int i = 0; i < parts.length - 1; i++) {
            charsToPick[i] = 1; // Start with 1 character
        }

        boolean uniqueKey = false;

        while (!uniqueKey) {
            key.setLength(0); // Reset the key builder

            for (int i = 0; i < parts.length; i++) {
                if (i < parts.length - 1) {
                    // Pick the required number of characters from the current part
                    int pickLength = Math.min(charsToPick[i], parts[i].length());
                    key.append(parts[i].substring(0, pickLength));
                } else {
                    // Append the last part without changes
                    key.append(parts[i]);
                }
                if (i < parts.length - 1) {
                    key.append("."); // Add a dot between parts
                }
            }

            // Check if the key is already present in the map
            if (shortNameMap.containsKey(key.toString())) {
                // Find the first conflicting part
                String existingValue = shortNameMap.get(key.toString());
                String[] existingParts = existingValue.split("\\.");

                boolean canExpand = false;

                // Find the first conflicting or differing part
                for (int i = 0; i < parts.length - 1; i++) {
                    if (charsToPick[i] < parts[i].length()) {
                        if (parts[i].equals(existingParts[i]) || !parts[i].equals(existingParts[i])) {
                            charsToPick[i]++;
                            canExpand = true;
                            break; // Adjust the first conflicting/differing part
                        }
                    }
                }

                // If no part can expand further, throw an exception
                if (!canExpand) {
                    throw new IllegalStateException("Unable to generate a unique key for input: " + input);
                }
            } else {
                uniqueKey = true;
            }
        }

        return key.toString();
    }

    private String checkForDuplicate(String input , Map<String,String> shortNameMap) {

        if (shortNameMap.containsValue(input)) {
            for (Map.Entry<String, String> entry : shortNameMap.entrySet()) {
                if (entry.getValue().equals(input)) {
                    return entry.getKey(); // Return the existing key for the input
                }
            }
        }

        return "";
    }

    private DagNode getCustomNodeObject(Class<?> clazz) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {


        CustomDagNode customDagNode = clazz.getAnnotation(CustomDagNode.class);
        if(customDagNode != null){
            Class<? extends DagNode> customDagNodeClazz = customDagNode.parentClass();
            return customDagNodeClazz.getConstructor(String.class).newInstance(clazz.getName());
        }
        return null;
    }

}
