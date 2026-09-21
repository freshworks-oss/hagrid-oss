package com.freshworks.hagrid.main.dsl.config.action;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import com.freshworks.hagrid.traverser.DagNode;
import com.freshworks.hagrid.traverser.ParentStep;
import com.freshworks.hagrid.main.dsl.config.model.ModelSpec;
import com.freshworks.hagrid.main.dsl.config.requests.RequestSpec;
import com.google.common.base.Strings;

import groovy.lang.Closure;
import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter 
public class CompositeActionConfig {
    
    String compositeActionName;
    List<ActionConfig> actionConfigList = new ArrayList<>();
    ModelSpec modelSpec;
    RequestSpec requestSpec;
    DagNode rootNode = new DagNode(ParentStep.class.getName());

    public CompositeActionConfig(ModelSpec modelSpec, RequestSpec requestSpec){
        this.modelSpec = modelSpec;
        this.requestSpec = requestSpec;
    }

    public void name(String compositeActionName){
        this.compositeActionName = compositeActionName;
    }


    public ActionConfig getActionByName(String actionName){

        for(ActionConfig actionConfig : actionConfigList){

            if (actionConfig.getName().equalsIgnoreCase(actionName)){
                return actionConfig;
            }
        }

        return null;
    }

    public void simple_action(Closure simpleActionClosure){

        ActionConfig newAction = new ActionConfig(this.modelSpec, this.requestSpec);
        actionConfigList.add(newAction);
        simpleActionClosure.setDelegate(newAction);
        simpleActionClosure.setResolveStrategy(Closure.DELEGATE_FIRST);
        simpleActionClosure.call();
    }

    public void sanityChecks(){

        // it means that it is simple action, 
        // so where developer may not have provided the name of the composite action explicitly, check 
        if(actionConfigList.size() == 1){

            if(Strings.isNullOrEmpty(compositeActionName)){

                this.compositeActionName = actionConfigList.get(0).getName();
            }
        }
    }

    /*
     *  In this method, we will create a dag like we do it in Hagrid 
     */
    public void createDag(){

        List<DagNode> rootNodeList = new ArrayList<>();
        List<ActionConfig> rootActionConfigList = new ArrayList<>();

        // Check if composite action has just one action 
        if(actionConfigList.size() == 1){
            ActionConfig actionConfig = actionConfigList.get(0);
            actionConfig.setRootNode(true);
            rootActionConfigList.add(actionConfig);
        }

        else{
            // First find list of rootnodes 
            for(ActionConfig actionConfig : actionConfigList){
                if(actionConfig.isRootNode){
                    rootActionConfigList.add(actionConfig);
                    break;
                }
            }
        }
    

        // Push all root nodes on the stack
        Deque<ActionConfig> stack = new ArrayDeque<>(rootActionConfigList);
        rootActionConfigList.forEach(stack::push);
        List<ActionConfig> alreadyTraversedActionConfig = new ArrayList<>();

        // Now find all child of this root node and push them onto stack
        while(Boolean.FALSE.equals(stack.isEmpty())){

            ActionConfig parentConfig = stack.pop();

            // Make sure do not traverse the actionConfig if it is already traversed
            // Cases in graph which have cycles
            if(alreadyTraversedActionConfig.contains(parentConfig)){

                continue;
            }

            // now check if parentConfig is root Node 

            DagNode parentNode;
            parentNode = new DagNode(parentConfig.getName());
            if(parentConfig.isRootNode){
                rootNodeList.add(parentNode);
            }
            

            // Create list of child nodes for this parentNode
            actionConfigList.stream()
            .filter( actionConfig -> {
        
                if(actionConfig.getParentActionName() != null && actionConfig.getParentActionName().equalsIgnoreCase(parentConfig.getName())){

                    return true;
                }

                return false;  
            })
            
            .forEach( actionConfig -> {
                    
                    // Check if this actionConfig is already traversed
                    if(!alreadyTraversedActionConfig.contains(actionConfig)){
                        stack.add(actionConfig);
                        DagNode childNode = new DagNode(actionConfig.getName());
                        parentNode.addChild(childNode);
                    }
                    else{
                        
                        // It means that this graph has cycles.
                        // It is the case when child is having pointer to its parent 
                        // Do not add this config to stack because it is already traversed

                        // However, we need to link childNode (which is a parent node) to parent Node ( which is a child now)

                        // Get the already created object parentNode ( which is a child now )
                        DagNode childNode = parentNode.find(actionConfig.getName());
                        parentNode.addChild(childNode);
                    }
                });

            // Once all child are added, add this parent to alreadyTraversedList
            alreadyTraversedActionConfig.add(parentConfig);
        }

        if(rootNodeList.isEmpty()){

            throw new IllegalStateException("Dag does not have any root node assigned. There must exists one root node");
        }

        for(DagNode node: rootNodeList){
            this.rootNode.addChild(node);
        }
    }
}
