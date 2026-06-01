package com.freshworks.core.traverser;

import com.freshworks.core.shared.SyncServiceContainer;
import com.freshworks.core.shared.analytics.AnalyticsFactory;
import com.freshworks.core.shared.analytics.AnalyticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;


import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.doReturn;

@SpringBootTest
@EnabledIfSystemProperty(named = "spring.profiles.active", matches = ".*\\.unit\\..*")
public class TestDagService {

    @Autowired
    DagScannerService dagScannerService;

    @Autowired
    AnalyticsFactory analyticsFactory;

    AnalyticsService analyticsService;

    @SpyBean
    TraverseConfigService traverseConfigService;

    @Autowired
    SyncServiceContainer syncServiceContainer;

    List<Map<String, String>> stepData = new ArrayList<>();

    DagNode dagNode;

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
        
        releaseVersion = System.getProperty("spring.profiles.active").split("\\.")[0];
        stepA  = (Class<? extends AbstractStep>) Class.forName("com.freshworks.core.data." + releaseVersion + ".unit.dag.steps.loop.StepA");
        stepB  = (Class<? extends AbstractStep>) Class.forName("com.freshworks.core.data." + releaseVersion + ".unit.dag.steps.loop.StepB");
        stepC  = (Class<? extends AbstractStep>) Class.forName("com.freshworks.core.data." + releaseVersion + ".unit.dag.steps.loop.StepC");
        application  = (Class<? extends AbstractStep>) Class.forName("com.freshworks.core.data." + releaseVersion + ".unit.dag.steps.TestApplication");
        servicePrinciple  = (Class<? extends AbstractStep>) Class.forName("com.freshworks.core.data." + releaseVersion + ".unit.dag.steps.TestServicePrinciple");
        appRoleAssignment  = (Class<? extends AbstractStep>) Class.forName("com.freshworks.core.data." + releaseVersion + ".unit.dag.steps.TestAppRoleAssignment");
        users = (Class<? extends AbstractStep>) Class.forName("com.freshworks.core.data." + releaseVersion + ".unit.dag.steps.TestUser");
        groups = (Class<? extends AbstractStep>) Class.forName("com.freshworks.core.data." + releaseVersion + ".unit.dag.steps.TestGroup");
        usages = (Class<? extends AbstractStep>) Class.forName("com.freshworks.core.data." + releaseVersion + ".unit.dag.steps.TestUsage");
        testIgnored = (Class<? extends AbstractStep>) Class.forName("com.freshworks.core.data." + releaseVersion + ".unit.dag.steps.TestIgnored");
        
    }


    @Test
    public void testDagIsCreatedSuccessfully() throws IOException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {


        String stepPath = "com.freshworks.core.data." + releaseVersion + ".unit.dag.steps";
        doReturn(stepPath).when(traverseConfigService).getStepLocation();
        this.analyticsService = analyticsFactory.getAnalyticsService("abc");
        this.dagNode = this.dagScannerService.scanner(this.traverseConfigService, analyticsService);
        assertThat(this.dagNode, is(notNullValue()));
    }


    @Test
    public void testDagIsCorrectWhenSomeStepsAreDropped() throws IOException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {

        String stepPath = "com.freshworks.core.data." + releaseVersion + ".unit.dag.steps";
        doReturn(stepPath).when(traverseConfigService).getStepLocation();

        this.analyticsService = analyticsFactory.getAnalyticsService("abc");
        this.dagNode = this.dagScannerService.scanner(traverseConfigService, analyticsService);
        DagNode nodeToDrop = this.dagNode.find(appRoleAssignment.getName());
        this.dagNode.dropSubtree(nodeToDrop);
        assertThat(this.dagNode.find(appRoleAssignment.getName()), is(nullValue()));
        assertThat(this.dagNode.find(users.getName()), is(nullValue()));
        assertThat(this.dagNode.find(groups.getName()), is(nullValue()));

        assertThat(this.dagNode.find(application.getName()), is(notNullValue()));
        assertThat(this.dagNode.find(usages.getName()), is(notNullValue()));
        assertThat(this.dagNode.find(servicePrinciple.getName()), is(notNullValue()));
    }

    @Test
    public void testDagIsCorrectWhenSomeStepsAreDroppedInRecursiveSteps() throws IOException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {

        String stepPath = "com.freshworks.core.data." + releaseVersion + ".unit.dag.steps.loop";
        doReturn(stepPath).when(traverseConfigService).getStepLocation();

        this.analyticsService = analyticsFactory.getAnalyticsService("abc");
        this.dagNode = this.dagScannerService.scanner(traverseConfigService, analyticsService);
        DagNode nodeToDrop = this.dagNode.find(stepB.getName());
        this.dagNode.dropSubtree(nodeToDrop);
        assertThat(this.dagNode.find(stepB.getName()), is(nullValue()));

        assertThat(this.dagNode.find(ParentStep.class.getName()), is(notNullValue()));
        assertThat(this.dagNode.find(stepA.getName()), is(notNullValue()));
        assertThat(this.dagNode.find(stepC.getName()), is(notNullValue()));

    }


    @Test
    public void testDagIsCorrectWhenSomeStepsAreStaticallyIgnored() throws IOException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {

        String stepPath = "com.freshworks.core.data." + releaseVersion + ".unit.dag.steps";
        doReturn(stepPath).when(traverseConfigService).getStepLocation();

        this.analyticsService = analyticsFactory.getAnalyticsService("abc");
        this.dagNode = this.dagScannerService.scanner(traverseConfigService, analyticsService);
        assertThat(this.dagNode.find(testIgnored.getName()), is(nullValue()));
    }


    @Test
    public void testDagIsCorrectWithRightHierarchy() throws IOException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {

        String stepPath = "com.freshworks.core.data." + releaseVersion + ".unit.dag.steps";
        doReturn(stepPath).when(traverseConfigService).getStepLocation();
        this.analyticsService = analyticsFactory.getAnalyticsService("abc");
        this.dagNode = this.dagScannerService.scanner(traverseConfigService, analyticsService);

        List<DagNode> children = this.dagNode.getSubtree();
        assertThat(children, hasItem(hasProperty("name", is(application.getName()))));

        DagNode node = this.dagNode.find(application.getName());
        children = node.getSubtree();
        assertThat(children, hasItem(hasProperty("name", is(servicePrinciple.getName()))));
        assertThat(children, hasItem(hasProperty("name", is(usages.getName()))));

        node = this.dagNode.find(usages.getName());
        children = node.getSubtree();
        assertThat(children, is(empty()));

        node = this.dagNode.find(servicePrinciple.getName());
        children = node.getSubtree();
        assertThat(children, hasItem(hasProperty("name", is(appRoleAssignment.getName()))));

        node = this.dagNode.find(appRoleAssignment.getName());
        children = node.getSubtree();
        assertThat(children, hasItem(hasProperty("name", is(users.getName()))));
        assertThat(children, hasItem(hasProperty("name", is(groups.getName()))));

        node = this.dagNode.find(users.getName());
        children = node.getSubtree();
        assertThat(children, is(empty()));

        node = this.dagNode.find(groups.getName());
        children = node.getSubtree();
        assertThat(children, is(empty()));
    }

    @Test
    public void testDagIsCorrectWithRightHierarchyWithRecursiveSteps() throws IOException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {

        String stepPath = "com.freshworks.core.data." + releaseVersion + ".unit.dag.steps.loop";
        doReturn(stepPath).when(traverseConfigService).getStepLocation();
        this.analyticsService = analyticsFactory.getAnalyticsService("abc");
        this.dagNode = this.dagScannerService.scanner(traverseConfigService, analyticsService);

        List<DagNode> children = this.dagNode.getSubtree();
        assertThat(children, hasItem(hasProperty("name", is(stepA.getName()))));
        assertThat(children, hasItem(hasProperty("name", is(stepC.getName()))));

        DagNode node = this.dagNode.find(stepA.getName());
        children = node.getSubtree();
        assertThat(children, hasItem(hasProperty("name", is(stepB.getName()))));

        node = this.dagNode.find(stepB.getName());
        children = node.getSubtree();
        assertThat(children, hasItem(hasProperty("name", is(stepA.getName()))));
        assertThat(children, hasItem(hasProperty("name", is(stepC.getName()))));

        node = this.dagNode.find(stepC.getName());
        children = node.getSubtree();
        assertThat(children, hasItem(hasProperty("name", is(stepB.getName()))));

    }
}
