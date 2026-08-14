package com.freshworks.core.traverser;

import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.analytics.AnalyticsFactory;
import com.freshworks.core.shared.analytics.AnalyticsService;
import com.freshworks.core.shared.sync.ConnectorConfiguration;
import com.freshworks.core.traverser.NodeRelationship.REL_SWITCH;
import com.google.common.collect.Lists;

import net.datafaker.providers.base.Relationship;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;


import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@EnabledIfSystemProperty(named = "spring.profiles.active", matches = "unit")
public class TestDagService {

    @Autowired
    DagService dagScannerService;

    @Autowired
    AnalyticsFactory analyticsFactory;

    AnalyticsService analyticsService;

    @SpyBean
    TraverseConfigService traverseConfigService;

    @Autowired
    SyncServiceContainer syncServiceContainer;

    List<Map<String, String>> stepData = new ArrayList<>();

    DagNode rootNode;

    String releaseVersion;

    Class<? extends AbstractStep> stepA;
    Class<? extends AbstractStep> stepB;
    Class<? extends AbstractStep> stepC;
    Class<? extends AbstractStep> application;
    Class<? extends AbstractStep> servicePrinciple;
    Class<? extends AbstractStep> appRoleAssignment;
    Class<? extends AbstractStep> users;
    Class<? extends AbstractStep> groups;
    Class<? extends AbstractStep> usages;
    Class<? extends AbstractStep> testIgnored;

    @BeforeEach
    public void beforeEach() throws Exception {
        
        stepA  = (Class<? extends AbstractStep>) Class.forName("com.freshworks.core.data.unit.dag.steps.loop.StepA");
        stepB  = (Class<? extends AbstractStep>) Class.forName("com.freshworks.core.data.unit.dag.steps.loop.StepB");
        stepC  = (Class<? extends AbstractStep>) Class.forName("com.freshworks.core.data.unit.dag.steps.loop.StepC");
        application  = (Class<? extends AbstractStep>) Class.forName("com.freshworks.core.data.unit.dag.steps.TestApplication");
        servicePrinciple  = (Class<? extends AbstractStep>) Class.forName("com.freshworks.core.data.unit.dag.steps.TestServicePrinciple");
        appRoleAssignment  = (Class<? extends AbstractStep>) Class.forName("com.freshworks.core.data.unit.dag.steps.TestAppRoleAssignment");
        users = (Class<? extends AbstractStep>) Class.forName("com.freshworks.core.data.unit.dag.steps.TestUser");
        groups = (Class<? extends AbstractStep>) Class.forName("com.freshworks.core.data.unit.dag.steps.TestGroup");
        usages = (Class<? extends AbstractStep>) Class.forName("com.freshworks.core.data.unit.dag.steps.TestUsage");
        testIgnored = (Class<? extends AbstractStep>) Class.forName("com.freshworks.core.data.unit.dag.steps.TestIgnored");
        
    }


    @Test
    public void testDagIsCreatedSuccessfully() throws IOException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {

        this.analyticsService = analyticsFactory.getAnalyticsService("abc");
        this.rootNode = this.dagScannerService.scanner(this.traverseConfigService, analyticsService);
        assertThat(this.rootNode, is(notNullValue()));
    }


    @Test
    public void testDagServiceWhenPathStartingFromTopNodeAreEnabled() throws Exception {

        ConnectorConfiguration connectorConfiguration = new ConnectorConfiguration();

        List<Class<? extends AbstractStep>> enablePath = new ArrayList<>();
        enablePath.add(application);
        enablePath.add(usages);

        connectorConfiguration.addPathToEnable(enablePath);

        this.rootNode = this.dagScannerService.scanner(this.traverseConfigService, analyticsService);

        this.analyticsService = analyticsFactory.getAnalyticsService("abc");
        this.dagScannerService.enableDisableDagPath(rootNode, connectorConfiguration.getEnabledDagPath());
        
        // Here assert that all relationship should be switched off execept relationship between parentNode --> application and application ---> usages

        // Validating if dag has application step 
        List<DagNode> children = this.rootNode.getImmediateChildren();

        for(DagNode node: children){

            if(node.getName().equalsIgnoreCase(application.getName())){

                NodeRelationship nodeRelationship = node.getParentRelationship(this.rootNode);
                assertThat(nodeRelationship.getRelSwitch(), is(REL_SWITCH.ON));

                // Assert that relationship between application usages is also enabled
                List<DagNode> nodeList = Lists.newArrayList(node.getChildrenRelationshipMap().keySet());

                for(DagNode appChild : nodeList){

                    if(appChild.getName().equalsIgnoreCase(usages.getName())){

                        NodeRelationship relationship = appChild.getParentRelationship(node);

                        assertThat(relationship.getRelSwitch(), is(REL_SWITCH.ON));
                    }
                }
                
            }

            else{

                NodeRelationship nodeRelationship = node.getParentRelationship(this.rootNode);
                assertThat(nodeRelationship.getRelSwitch(), is(REL_SWITCH.OFF));
            }
        }
    }

    @Test
    public void testDagServiceWhenPathStartingFromIntermediateNodeAreEnabled() throws Exception {

        ConnectorConfiguration connectorConfiguration = new ConnectorConfiguration();

        List<Class<? extends AbstractStep>> enablePath = new ArrayList<>();
        enablePath.add(servicePrinciple);
        enablePath.add(appRoleAssignment);
        enablePath.add(users);

        connectorConfiguration.addPathToEnable(enablePath);

        this.rootNode = this.dagScannerService.scanner(this.traverseConfigService, analyticsService);

        this.analyticsService = analyticsFactory.getAnalyticsService("abc");
        this.dagScannerService.enableDisableDagPath(rootNode, connectorConfiguration.getEnabledDagPath());
        
        // Here assert that all relationship should be switched off execept relationship between service principle --> app role assignment --> users

        // Validating if dag has application step 
        List<DagNode> allDagNodeList = this.rootNode.getNodesInDag();

        for(DagNode node: allDagNodeList){

            if(node.getName().equalsIgnoreCase(ParentStep.class.getName())){

                // skip it , do not do anything
            }
            else if(node.getName().equalsIgnoreCase(appRoleAssignment.getName())){

                List<NodeRelationship> nodeRelationshipList =  Lists.newArrayList(node.getParentRelationshipMap().values());
                assertThat(nodeRelationshipList.size(), is(1));
                assertThat(nodeRelationshipList.get(0).getRelSwitch(), is(REL_SWITCH.ON));
            }

            else if(node.getName().equalsIgnoreCase(users.getName())){

                List<NodeRelationship> nodeRelationshipList = Lists.newArrayList(node.getParentRelationshipMap().values());
                assertThat(nodeRelationshipList.size(), is(1));
                assertThat(nodeRelationshipList.get(0).getRelSwitch(), is(REL_SWITCH.ON));
            }

            else{

                List<NodeRelationship> nodeRelationshipList = Lists.newArrayList(node.getParentRelationshipMap().values());
                assertThat(nodeRelationshipList.get(0).getRelSwitch(), is(REL_SWITCH.OFF));
            }
        }
    }

    @Test
    public void testDagServiceWhenPathStartingFromTopNodeAreEnabledInRecursiveSteps() throws Exception {

        ConnectorConfiguration connectorConfiguration = new ConnectorConfiguration();

        List<Class<? extends AbstractStep>> enablePath = new ArrayList<>();
        enablePath.add(stepA);
        enablePath.add(stepB);

        connectorConfiguration.addPathToEnable(enablePath);

        this.rootNode = this.dagScannerService.scanner(this.traverseConfigService, analyticsService);

        this.analyticsService = analyticsFactory.getAnalyticsService("abc");
        this.dagScannerService.enableDisableDagPath(rootNode, connectorConfiguration.getEnabledDagPath());
        
        // Here assert that all relationship should be switched off execept relationship between parentNode --> application and application ---> usages

        // Validating if dag has application step 
        List<DagNode> children = this.rootNode.getImmediateChildren();

        for(DagNode node: children){

            if(node.getName().equalsIgnoreCase(stepA.getName())){

                NodeRelationship nodeRelationship = node.getParentRelationship(this.rootNode);
                assertThat(nodeRelationship.getRelSwitch(), is(REL_SWITCH.ON));

                // Assert that relationship between application usages is also enabled
                List<DagNode> nodeList = Lists.newArrayList(node.getChildrenRelationshipMap().keySet());

                for(DagNode stepAChild : nodeList){

                    if(stepAChild.getName().equalsIgnoreCase(stepB.getName())){

                        NodeRelationship relationship = stepAChild.getParentRelationship(node);

                        assertThat(relationship.getRelSwitch(), is(REL_SWITCH.ON));

                        NodeRelationship reverseRelationship = node.getParentRelationship(stepAChild);

                        assertThat(reverseRelationship.getRelSwitch(), is(REL_SWITCH.OFF));
                    }
                }
                
            }

            else{

                NodeRelationship nodeRelationship = node.getParentRelationship(this.rootNode);
                assertThat(nodeRelationship.getRelSwitch(), is(REL_SWITCH.OFF));
            }
        }
    }

    @Test
    public void testDagServiceWhenPathStartingFromIntermediateNodeAreEnabledInRecursiveSteps() throws Exception {


        ConnectorConfiguration connectorConfiguration = new ConnectorConfiguration();

        List<Class<? extends AbstractStep>> enablePath = new ArrayList<>();
        enablePath.add(stepB);
        enablePath.add(stepC);

        connectorConfiguration.addPathToEnable(enablePath);

        this.rootNode = this.dagScannerService.scanner(this.traverseConfigService, analyticsService);

        this.analyticsService = analyticsFactory.getAnalyticsService("abc");
        this.dagScannerService.enableDisableDagPath(rootNode, connectorConfiguration.getEnabledDagPath());
        
        // Here assert that all relationship should be switched off execept relationship between service principle --> app role assignment --> users

        // Validating if dag has application step 
        List<DagNode> allDagNodeList = this.rootNode.getNodesInDag();

        for(DagNode node: allDagNodeList){

            if(node.getName().equalsIgnoreCase(ParentStep.class.getName())){

                // skip it , do not do anything
            }
            else if(node.getName().equalsIgnoreCase(stepC.getName())){

                Set<DagNode> parentNodeSet = node.getParentRelationshipMap().keySet();

                DagNode stepBNode = null;
                
                for(DagNode parentNode: parentNodeSet){

                    if(parentNode != this.rootNode){
                        stepBNode = parentNode;
                    }
                }


                List<NodeRelationship> nodeRelationshipList =  Lists.newArrayList(node.getParentRelationshipMap().values());
                assertThat(nodeRelationshipList.size(), is(2));


                NodeRelationship nodeRelationship1 = node.getParentRelationship(stepBNode);
                assertThat(nodeRelationship1.getRelSwitch(), is(REL_SWITCH.ON));

                NodeRelationship nodeRelationship2 = node.getParentRelationship(this.rootNode);
                assertThat(nodeRelationship2.getRelSwitch(), is(REL_SWITCH.OFF));
            }

            else{

                List<NodeRelationship> nodeRelationshipList = Lists.newArrayList(node.getParentRelationshipMap().values());
                assertThat(nodeRelationshipList.get(0).getRelSwitch(), is(REL_SWITCH.OFF));
            }
        }

    }


    @Test
    public void testDagIsCorrectWhenSomeStepsAreStaticallyIgnored() throws IOException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {

        this.analyticsService = analyticsFactory.getAnalyticsService("abc");
        this.rootNode = this.dagScannerService.scanner(traverseConfigService, analyticsService);
        assertThat(this.rootNode.find(testIgnored.getName()), is(nullValue()));
    }


    @Test
    public void testDagIsCorrectWithRightHierarchy() throws IOException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {

        this.analyticsService = analyticsFactory.getAnalyticsService("abc");
        this.rootNode = this.dagScannerService.scanner(traverseConfigService, analyticsService);

        // Validating if dag has application step 
        List<DagNode> children = this.rootNode.getImmediateChildren();
        assertThat(children, hasItem(hasProperty("name", is(application.getName()))));

        // Validating if application node has service principle and usages as child
        DagNode node = this.rootNode.find(application.getName());
        children = node.getImmediateChildren();
        assertThat(children, hasItem(hasProperty("name", is(servicePrinciple.getName()))));
        assertThat(children, hasItem(hasProperty("name", is(usages.getName()))));

        // Validating if usages node do not have any child
        node = this.rootNode.find(usages.getName());
        children = node.getImmediateChildren();
        assertThat(children, is(empty()));

        // Validating if service principle node has appRoleAssignment as child
        node = this.rootNode.find(servicePrinciple.getName());
        children = node.getImmediateChildren();
        assertThat(children, hasItem(hasProperty("name", is(appRoleAssignment.getName()))));

        // Validating if app role assignment node has two child users and groups
        node = this.rootNode.find(appRoleAssignment.getName());
        children = node.getImmediateChildren();
        assertThat(children, hasItem(hasProperty("name", is(users.getName()))));
        assertThat(children, hasItem(hasProperty("name", is(groups.getName()))));

        // Validating if users node has no child
        node = this.rootNode.find(users.getName());
        children = node.getImmediateChildren();
        assertThat(children, is(empty()));

        // Validating if groups node has no child
        node = this.rootNode.find(groups.getName());
        children = node.getImmediateChildren();
        assertThat(children, is(empty()));


        // Now validate loop cases 

        // Validate step A is child of parent 
        children = this.rootNode.getImmediateChildren();
        // assertThat(children.size(), is(2));
        assertThat(children, hasItem(hasProperty("name", is(stepA.getName()))));
        assertThat(children, hasItem(hasProperty("name", is(stepC.getName()))));


        // Validate stepA has stepB as child 
        node = this.rootNode.find(stepA.getName());
        children = node.getImmediateChildren();
        assertThat(children.size(), is(1));
        assertThat(children, hasItem(hasProperty("name", is(stepB.getName()))));

        // Validate stepB has two children stepA and stepC
        node = this.rootNode.find(stepB.getName());
        children = node.getImmediateChildren();
        assertThat(children.size(), is(2));
        assertThat(children, hasItem(hasProperty("name", is(stepA.getName()))));
        assertThat(children, hasItem(hasProperty("name", is(stepC.getName()))));

        // Validate stepC has one child stepB
        node = this.rootNode.find(stepC.getName());
        children = node.getImmediateChildren();
        assertThat(children.size(), is(1));
        assertThat(children, hasItem(hasProperty("name", is(stepB.getName()))));

    }
}
