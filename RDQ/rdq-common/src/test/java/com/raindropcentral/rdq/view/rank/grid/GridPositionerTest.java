package com.raindropcentral.rdq.view.rank.grid;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GridPositionerTest {

    @Test
    void layoutSingleRootProducesLinearCoordinates() {
        final Map<String, TestNode> nodes = new HashMap<>();
        nodes.put("root", new TestNode(List.of("child"), List.of(), 0));
        nodes.put("child", new TestNode(List.of("leaf"), List.of("root"), 1));
        nodes.put("leaf", new TestNode(List.of(), List.of("child"), 2));

        final GridPositioner positioner = new GridPositioner(new GridPosition(0, 0), 2);

        final Map<String, GridPosition> positions = positioner.layout(
                nodes,
                TestNode::nextIds,
                TestNode::prevIds,
                TestNode::tier
        );

        assertEquals(new GridPosition(0, 0), positions.get("root"));
        assertEquals(new GridPosition(0, 2), positions.get("child"));
        assertEquals(new GridPosition(0, 4), positions.get("leaf"));
    }

    @Test
    void layoutMultipleRootsDistributesAcrossAnchor() {
        final Map<String, TestNode> nodes = new HashMap<>();
        nodes.put("gamma", new TestNode(List.of(), List.of(), 0));
        nodes.put("alpha", new TestNode(List.of("alpha-child"), List.of(), 0));
        nodes.put("beta", new TestNode(List.of("beta-child"), List.of(), 0));
        nodes.put("alpha-child", new TestNode(List.of(), List.of("alpha"), 1));
        nodes.put("beta-child", new TestNode(List.of(), List.of("beta"), 1));

        final GridPositioner positioner = new GridPositioner(new GridPosition(0, 0), 2);

        final Map<String, GridPosition> positions = positioner.layout(
                nodes,
                TestNode::nextIds,
                TestNode::prevIds,
                TestNode::tier
        );

        assertEquals(new GridPosition(-2, 0), positions.get("alpha"));
        assertEquals(new GridPosition(0, 0), positions.get("beta"));
        assertEquals(new GridPosition(2, 0), positions.get("gamma"));
        assertEquals(new GridPosition(-2, 2), positions.get("alpha-child"));
        assertEquals(new GridPosition(0, 2), positions.get("beta-child"));
    }

    @Test
    void layoutOrdersByTierAndAvoidsRepositioning() {
        final Map<String, TestNode> nodes = new HashMap<>();
        nodes.put("root", new TestNode(List.of("low-tier", "high-tier"), List.of(), 0));
        nodes.put("low-tier", new TestNode(List.of("shared"), List.of("root"), 0));
        nodes.put("high-tier", new TestNode(List.of("shared"), List.of("root"), 5));
        nodes.put("shared", new TestNode(List.of(), List.of("low-tier", "high-tier"), 10));

        final GridPositioner positioner = new GridPositioner(new GridPosition(0, 0), 2);

        final Map<String, GridPosition> positions = positioner.layout(
                nodes,
                TestNode::nextIds,
                TestNode::prevIds,
                TestNode::tier
        );

        assertNotNull(positions.get("shared"), "Shared node should be positioned");
        assertEquals(new GridPosition(-1, 2), positions.get("low-tier"));
        assertEquals(new GridPosition(1, 2), positions.get("high-tier"));
        assertEquals(new GridPosition(-1, 4), positions.get("shared"));
    }

    private static final class TestNode {

        private final List<String> nextIds;
        private final List<String> prevIds;
        private final int tier;

        private TestNode(final List<String> nextIds, final List<String> prevIds, final int tier) {
            this.nextIds = nextIds;
            this.prevIds = prevIds;
            this.tier = tier;
        }

        private List<String> nextIds() {
            return this.nextIds;
        }

        private List<String> prevIds() {
            return this.prevIds;
        }

        private int tier() {
            return this.tier;
        }
    }
}
