package com.freshworks.core.traverser.configuration;

import com.freshworks.core.data.five_zero_zero.unit.dag.steps.*;
import com.freshworks.core.data.five_zero_zero.unit.dag.steps.loop.StepA;
import com.freshworks.core.traverser.DagNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

@SpringBootTest()
@EnabledIfSystemProperty(named = "spring.profiles.active", matches = ".*\\.unit\\..*")
public class TestDagService {

    @Autowired
    DagService dagInitialisation;


    @Test
    public void testCloneDagWhenDagHasMultipleChild() throws Exception{

        DagNode application = new DagNode(TestApplication.class.getName());
        DagNode servicePrincipal = new DagNode(TestServicePrinciple.class.getName());
        DagNode appRoleAssignment = new DagNode(TestAppRoleAssignment.class.getName());
        DagNode group = new DagNode(TestGroup.class.getName());
        DagNode usage = new DagNode(TestUsage.class.getName());
        DagNode user = new DagNode(TestUser.class.getName());

        application.addChild(servicePrincipal);
        servicePrincipal.addChild(appRoleAssignment);
        appRoleAssignment.addChild(group);
        appRoleAssignment.addChild(user);
        appRoleAssignment.addChild(usage);


        DagNode clonedApplication = dagInitialisation.cloneDag(application);

        assertThat(application.hashCode(), is(not(clonedApplication.hashCode())));

        DagNode clonedServicePrincipal = clonedApplication.find(TestServicePrinciple.class.getName());
        assertThat(servicePrincipal.hashCode(), is(not(clonedServicePrincipal.hashCode())));

        DagNode clonedAppRoleAssignment = clonedApplication.find(TestAppRoleAssignment.class.getName());
        assertThat(appRoleAssignment.hashCode(), is(not(clonedAppRoleAssignment.hashCode())));

        DagNode clonedGroup = clonedApplication.find(TestGroup.class.getName());
        assertThat(group.hashCode(), is(not(clonedGroup.hashCode())));

        DagNode clonedUser = clonedApplication.find(TestUser.class.getName());
        assertThat(user.hashCode(), is(not(clonedUser.hashCode())));

        DagNode clonedUsage = clonedApplication.find(TestUsage.class.getName());
        assertThat(usage.hashCode(), is(not(clonedUsage.hashCode())));


        assertThat(clonedApplication.getIsCloned(), is(true));
        assertThat(clonedServicePrincipal.getIsCloned(), is(true));
        assertThat(clonedAppRoleAssignment.getIsCloned(), is(true));
        assertThat(clonedGroup.getIsCloned(), is(true));
        assertThat(clonedUser.getIsCloned(), is(true));
        assertThat(clonedUsage.getIsCloned(), is(true));
    }

    @Test
    public void testCloneDagWhenDagHasSelfRecursiveSteps() throws Exception{

        DagNode stepANode = new DagNode(StepA.class.getName());
        stepANode.addChild(stepANode);
        DagNode clonedStepA = dagInitialisation.cloneDag(stepANode);
        assertThat(clonedStepA.hashCode(), is(not(stepANode.hashCode())));

        DagNode clonedStepAParent = clonedStepA.getParentRelationshipMap().keySet().stream().findFirst().get();
        assertThat(clonedStepAParent.hashCode(), is(not(stepANode.hashCode())));
        assertThat(clonedStepA.getParentRelationshipMap().keySet().size(), is(1));
    }
}
