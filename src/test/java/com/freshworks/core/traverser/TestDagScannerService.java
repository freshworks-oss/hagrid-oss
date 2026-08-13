package com.freshworks.core.traverser;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;

import java.util.ArrayList;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.freshworks.core.shared.analytics.AnalyticsFactory;
import com.freshworks.core.shared.analytics.AnalyticsService;

@SpringBootTest
@EnabledIfSystemProperty(named = "spring.profiles.active", matches = "unit")
public class TestDagScannerService {

    @Autowired
    List<AbstractStep> abstractStepList;

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

        mockFacadeDagScannerService.configure().build();
        mockFacadeTraverseConfigService.configure().build();

        application = (Class<? extends AbstractStep>) Class.forName("com.freshworks.core.data.unit.dag.steps.TestApplication");
        testIgnored = (Class<? extends AbstractStep>) Class.forName("com.freshworks.core.data.unit.dag.steps.TestIgnored");
        testAnotherInner = (Class<? extends AbstractStep>) Class.forName("com.freshworks.core.data.unit.dag.steps.anotherinner.TestAnotherInner");
        testInnerStep = (Class<? extends AbstractStep>) Class.forName("com.freshworks.core.data.unit.dag.steps.inner.TestInnerStep");
        testInnerMost = (Class<? extends AbstractStep>) Class.forName("com.freshworks.core.data.unit.dag.steps.inner.innermost.TestInnerMost");

    }


    @Test
    public void testCreateDagWithStepsInNestedPackage() throws Exception {

        DagService dagScannerService = mockFacadeDagScannerService.build();
        doCallRealMethod().when(dagScannerService).createDAG(any(),  any());

        AnalyticsService analyticsService = analyticsFactory.getAnalyticsService("some-random-namespace");

        DagNode rootNode = dagScannerService.createDAG(abstractStepList, analyticsService);
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
