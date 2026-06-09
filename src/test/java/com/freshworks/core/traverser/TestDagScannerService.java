package com.freshworks.core.traverser;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.freshworks.core.shared.analytics.AnalyticsFactory;
import com.freshworks.core.shared.analytics.AnalyticsService;

@SpringBootTest
@EnabledIfSystemProperty(named = "spring.profiles.active", matches = ".*\\.unit\\..*")
public class TestDagScannerService {

    @Autowired
    MockFacadeDagScannerService mockFacadeDagScannerService;

    @Autowired
    MockFacadeTraverseConfigService mockFacadeTraverseConfigService;

    @Autowired
    AnalyticsFactory analyticsFactory;

    String releaseVersion;

    Class<? extends AbstractStep> application;
    Class<? extends AbstractStep> testIgnored;
    Class<? extends AbstractStep> testAnotherInner;
    Class<? extends AbstractStep> testInnerStep;
    Class<? extends AbstractStep> testInnerMost; 

    @BeforeEach
    public void beforeEach() throws Exception {

        releaseVersion = System.getProperty("spring.profiles.active").split("\\.")[0];
        mockFacadeDagScannerService.configure().build();
        mockFacadeTraverseConfigService.configure().build();

        application = (Class<? extends AbstractStep>) Class.forName("com.freshworks.core.data." + releaseVersion + ".unit.dag.steps.TestApplication");
        testIgnored = (Class<? extends AbstractStep>) Class.forName("com.freshworks.core.data." + releaseVersion + ".unit.dag.steps.TestIgnored");
        testAnotherInner = (Class<? extends AbstractStep>) Class.forName("com.freshworks.core.data." + releaseVersion + ".unit.dag.steps.anotherinner.TestAnotherInner");
        testInnerStep = (Class<? extends AbstractStep>) Class.forName("com.freshworks.core.data." + releaseVersion + ".unit.dag.steps.inner.TestInnerStep");
        testInnerMost = (Class<? extends AbstractStep>) Class.forName("com.freshworks.core.data." + releaseVersion + ".unit.dag.steps.inner.innermost.TestInnerMost");

    }

    @Test
    public void testScannerScanStepsInNestedPackage() throws Exception {

        DagScannerService dagScannerService = mockFacadeDagScannerService.build();
        doCallRealMethod().when(dagScannerService).getSteps(any(), anyString());

        String stepPath = "com.freshworks.core.data." + releaseVersion + ".unit.dag.steps";
        TraverseConfigService traverseConfigService = mockFacadeTraverseConfigService
                .getStepLocation(stepPath)
                .build();


        AnalyticsService analyticsService = analyticsFactory.getAnalyticsService("some-random-namespace");
        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forPackage(stepPath)));

        Set<Class<?>> scannedSteps = dagScannerService.getSteps(reflections, stepPath);

        assertThat(scannedSteps.contains(testInnerMost), Matchers.is(true));
        assertThat(scannedSteps.contains(testInnerStep), Matchers.is(true));
        assertThat(scannedSteps.contains(testAnotherInner), Matchers.is(true));
        assertThat(scannedSteps.contains(testIgnored), Matchers.is(true));
        assertThat(scannedSteps.contains(application), Matchers.is(true));

    }

    @Test
    public void testCreateDagWithStepsInNestedPackage() throws Exception {

        DagScannerService dagScannerService = mockFacadeDagScannerService.build();
        doCallRealMethod().when(dagScannerService).getSteps(any(), anyString());
        doCallRealMethod().when(dagScannerService).createDAG(any(), anyString(), any());

        String stepPath = "com.freshworks.core.data." + releaseVersion + ".unit.dag.steps";
        TraverseConfigService traverseConfigService = mockFacadeTraverseConfigService
                .getStepLocation(stepPath)
                .build();


        AnalyticsService analyticsService = analyticsFactory.getAnalyticsService("some-random-namespace");
        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forPackage(stepPath)));

        DagNode rootNode = dagScannerService.createDAG(reflections, stepPath, analyticsService);
        List<DagNode> children = new ArrayList<>(rootNode.getChildrenRelationshipMap().keySet());
        assertThat(children, Matchers.hasItem(hasProperty("name", Matchers.equalTo(testInnerStep.getName()))));
        assertThat(children, Matchers.hasItem(hasProperty("name", Matchers.equalTo(testAnotherInner.getName()))));
        assertThat(children, Matchers.hasItem(hasProperty("name", Matchers.equalTo(application.getName()))));
        assertThat(children, Matchers.not(Matchers.hasItem(hasProperty("name", Matchers.equalTo(testIgnored.getName())))));

        for(DagNode node : children){

            if(node.getName().equalsIgnoreCase(testInnerStep.getName())){

                List<DagNode> nestedChildren = new ArrayList<>(node.getChildrenRelationshipMap().keySet());

                assertThat(nestedChildren.size(), Matchers.is(1));
                assertThat(nestedChildren, Matchers.hasItem(hasProperty("name", Matchers.equalTo(testInnerMost.getName()))));
            }
        }
    }
}
