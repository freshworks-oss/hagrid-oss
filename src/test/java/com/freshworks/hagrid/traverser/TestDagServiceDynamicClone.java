package com.freshworks.hagrid.traverser;

import com.freshworks.hagrid.shared.infra.InfraService;
import com.freshworks.hagrid.shared.sync.ConnectorConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The root DAG of a DSL action is created once and handed to every run of that action.
 * Each run must work on its own copy, or counters and state from one run leak into the next.
 */
class TestDagServiceDynamicClone {

    private DagService dagService;
    private DagNode root;
    private InfraService infraService;

    @BeforeEach
    void setUp() {
        ConnectorConfiguration configuration = mock(ConnectorConfiguration.class);
        when(configuration.getEnabledDagPathList()).thenReturn(List.of());
        dagService = new DagService();
        dagService.setConnectorConfiguration(configuration);
        infraService = mock(InfraService.class);

        root = new DagNode("root", true);
        root.addChild(new DagNode("child", true));
    }

    @Test
    void everyRunGetsItsOwnCopyOfTheDag() throws Exception {
        DagNode run1 = dagService.dagDynamicDagScanner("ns", root, null, infraService);
        DagNode run2 = dagService.dagDynamicDagScanner("ns", root, null, infraService);

        assertNotSame(root, run1);
        assertNotSame(run1, run2);
    }

    @Test
    void itemCountersOfOneRunDoNotLeakIntoTheNextRun() throws Exception {
        DagNode run1 = dagService.dagDynamicDagScanner("ns", root, null, infraService);
        run1.find("child").relationshipIncrementTotalItemsCount(run1);
        assertEquals(1, run1.find("child").getRelationshipTotalItemsCount(run1));

        DagNode run2 = dagService.dagDynamicDagScanner("ns", root, null, infraService);

        assertEquals(0, run2.find("child").getRelationshipTotalItemsCount(run2));
        assertEquals(0, root.find("child").getRelationshipTotalItemsCount(root));
    }
}
