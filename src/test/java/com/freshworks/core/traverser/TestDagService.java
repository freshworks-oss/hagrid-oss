package com.freshworks.core.traverser;


import com.freshworks.core.data.four_zero_zero.unit.dag.steps.*;
import com.freshworks.core.data.four_zero_zero.unit.dag.steps.loop.StepA;
import com.freshworks.core.data.four_zero_zero.unit.dag.steps.loop.StepB;
import com.freshworks.core.data.four_zero_zero.unit.dag.steps.loop.StepC;
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

    @BeforeEach
    public void beforeEach() throws Exception {
        releaseVersion = System.getProperty("spring.profiles.active").split("\\.")[0];
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
        DagNode nodeToDrop = this.dagNode.find(TestAppRoleAssignment.class.getName());
        this.dagNode.dropSubtree(nodeToDrop);
        assertThat(this.dagNode.find(TestAppRoleAssignment.class.getName()), is(nullValue()));
        assertThat(this.dagNode.find(TestUser.class.getName()), is(nullValue()));
        assertThat(this.dagNode.find(TestGroup.class.getName()), is(nullValue()));

        assertThat(this.dagNode.find(TestApplication.class.getName()), is(notNullValue()));
        assertThat(this.dagNode.find(TestUsage.class.getName()), is(notNullValue()));
        assertThat(this.dagNode.find(TestServicePrinciple.class.getName()), is(notNullValue()));
    }

    @Test
    public void testDagIsCorrectWhenSomeStepsAreDroppedInRecursiveSteps() throws IOException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {

        String stepPath = "com.freshworks.core.data." + releaseVersion + ".unit.dag.steps.loop";
        doReturn(stepPath).when(traverseConfigService).getStepLocation();

        this.analyticsService = analyticsFactory.getAnalyticsService("abc");
        this.dagNode = this.dagScannerService.scanner(traverseConfigService, analyticsService);
        DagNode nodeToDrop = this.dagNode.find(StepB.class.getName());
        this.dagNode.dropSubtree(nodeToDrop);
        assertThat(this.dagNode.find(StepB.class.getName()), is(nullValue()));

        assertThat(this.dagNode.find(ParentStep.class.getName()), is(notNullValue()));
        assertThat(this.dagNode.find(StepA.class.getName()), is(notNullValue()));
        assertThat(this.dagNode.find(StepC.class.getName()), is(notNullValue()));

    }


    @Test
    public void testDagIsCorrectWhenSomeStepsAreStaticallyIgnored() throws IOException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {

        String stepPath = "com.freshworks.core.data." + releaseVersion + ".unit.dag.steps";
        doReturn(stepPath).when(traverseConfigService).getStepLocation();

        this.analyticsService = analyticsFactory.getAnalyticsService("abc");
        this.dagNode = this.dagScannerService.scanner(traverseConfigService, analyticsService);
        assertThat(this.dagNode.find(TestIgnored.class.getName()), is(nullValue()));
    }


    @Test
    public void testDagIsCorrectWithRightHierarchy() throws IOException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {

        String stepPath = "com.freshworks.core.data." + releaseVersion + ".unit.dag.steps";
        doReturn(stepPath).when(traverseConfigService).getStepLocation();
        this.analyticsService = analyticsFactory.getAnalyticsService("abc");
        this.dagNode = this.dagScannerService.scanner(traverseConfigService, analyticsService);

        List<DagNode> children = this.dagNode.getSubtree();
        assertThat(children, hasItem(hasProperty("name", is(TestApplication.class.getName()))));

        DagNode node = this.dagNode.find(TestApplication.class.getName());
        children = node.getSubtree();
        assertThat(children, hasItem(hasProperty("name", is(TestServicePrinciple.class.getName()))));
        assertThat(children, hasItem(hasProperty("name", is(TestUsage.class.getName()))));

        node = this.dagNode.find(TestUsage.class.getName());
        children = node.getSubtree();
        assertThat(children, is(empty()));

        node = this.dagNode.find(TestServicePrinciple.class.getName());
        children = node.getSubtree();
        assertThat(children, hasItem(hasProperty("name", is(TestAppRoleAssignment.class.getName()))));

        node = this.dagNode.find(TestAppRoleAssignment.class.getName());
        children = node.getSubtree();
        assertThat(children, hasItem(hasProperty("name", is(TestUser.class.getName()))));
        assertThat(children, hasItem(hasProperty("name", is(TestGroup.class.getName()))));

        node = this.dagNode.find(TestUser.class.getName());
        children = node.getSubtree();
        assertThat(children, is(empty()));

        node = this.dagNode.find(TestGroup.class.getName());
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
        assertThat(children, hasItem(hasProperty("name", is(StepA.class.getName()))));
        assertThat(children, hasItem(hasProperty("name", is(StepC.class.getName()))));

        DagNode node = this.dagNode.find(StepA.class.getName());
        children = node.getSubtree();
        assertThat(children, hasItem(hasProperty("name", is(StepB.class.getName()))));

        node = this.dagNode.find(StepB.class.getName());
        children = node.getSubtree();
        assertThat(children, hasItem(hasProperty("name", is(StepA.class.getName()))));
        assertThat(children, hasItem(hasProperty("name", is(StepC.class.getName()))));

        node = this.dagNode.find(StepC.class.getName());
        children = node.getSubtree();
        assertThat(children, hasItem(hasProperty("name", is(StepB.class.getName()))));

    }
}
