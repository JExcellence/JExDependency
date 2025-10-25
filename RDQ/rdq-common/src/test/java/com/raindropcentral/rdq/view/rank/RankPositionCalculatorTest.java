package com.raindropcentral.rdq.view.rank;

import com.raindropcentral.rdq.config.item.IconSection;
import com.raindropcentral.rdq.database.entity.rank.RRank;
import com.raindropcentral.rdq.view.rank.grid.GridPosition;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RankPositionCalculatorTest {

    private static final IconSection ICON = new IconSection(new EvaluationEnvironmentBuilder());

    @Test
    void calculatePositionsAlignsChildrenUnderTheirParents() {
        final RankNode root = createNode("rank.root", 1, true);
        final RankNode leftChild = createNode("rank.child.left", 2, false);
        final RankNode rightChild = createNode("rank.child.right", 3, false);
        final RankNode grandChild = createNode("rank.grand.child", 4, false);

        connect(root, leftChild);
        connect(root, rightChild);
        connect(rightChild, grandChild);

        final Map<String, RankNode> hierarchy = toHierarchy(root, leftChild, rightChild, grandChild);

        final RankPositionCalculator calculator = new RankPositionCalculator();
        final Map<String, GridPosition> positions = calculator.calculatePositions(hierarchy);

        assertEquals(new GridPosition(3, 2), positions.get(identifier(root)),
            "Single root should be anchored at the predefined grid origin");
        assertEquals(new GridPosition(1, 7), positions.get(identifier(leftChild)),
            "First child should be offset to the left beneath the parent");
        assertEquals(new GridPosition(6, 7), positions.get(identifier(rightChild)),
            "Second child should be offset to the right beneath the parent");
        assertEquals(new GridPosition(6, 12), positions.get(identifier(grandChild)),
            "Grandchild should be placed directly beneath its single parent");
    }

    @Test
    void calculatePositionsDistributesMultipleRootsEvenly() {
        final RankNode alphaRoot = createNode("rank.alpha", 1, true);
        final RankNode betaRoot = createNode("rank.beta", 1, true);
        final RankNode betaChild = createNode("rank.beta.child", 2, false);

        connect(betaRoot, betaChild);

        final Map<String, RankNode> hierarchy = toHierarchy(alphaRoot, betaRoot, betaChild);

        final RankPositionCalculator calculator = new RankPositionCalculator();
        final Map<String, GridPosition> positions = calculator.calculatePositions(hierarchy);

        assertEquals(new GridPosition(1, 2), positions.get(identifier(alphaRoot)),
            "First root should be shifted left when multiple roots are present");
        assertEquals(new GridPosition(6, 2), positions.get(identifier(betaRoot)),
            "Second root should be shifted right when multiple roots are present");
        assertEquals(new GridPosition(6, 7), positions.get(identifier(betaChild)),
            "Child should remain vertically aligned beneath its root");
    }

    @Test
    void calculatePositionsReturnsEmptyWhenNoRootsArePresent() {
        final RankNode first = createNode("rank.loop.first", 2, false);
        final RankNode second = createNode("rank.loop.second", 2, false);

        connect(first, second);
        connect(second, first);

        final Map<String, RankNode> hierarchy = toHierarchy(first, second);

        final Logger logger = Logger.getLogger(RankPositionCalculator.class.getName());
        final Level previousLevel = logger.getLevel();
        logger.setLevel(Level.OFF);
        try {
            final RankPositionCalculator calculator = new RankPositionCalculator();
            final Map<String, GridPosition> positions = calculator.calculatePositions(hierarchy);

            assertTrue(positions.isEmpty(),
                "Calculator should return an empty position map when no root nodes can be identified");
        } finally {
            logger.setLevel(previousLevel);
        }
    }

    private static RankNode createNode(final String identifier, final int tier, final boolean initial) {
        final RRank rank = new RRank(
            identifier,
            identifier + ".name",
            identifier + ".description",
            "group." + identifier,
            identifier + ".prefix",
            identifier + ".suffix",
            ICON,
            initial,
            tier,
            1,
            null
        );
        return new RankNode(rank);
    }

    private static void connect(final RankNode parent, final RankNode child) {
        parent.addChild(child);
        child.addParent(parent);
    }

    private static Map<String, RankNode> toHierarchy(final RankNode... nodes) {
        final Map<String, RankNode> hierarchy = new HashMap<>();
        for (final RankNode node : nodes) {
            hierarchy.put(identifier(node), node);
        }
        return hierarchy;
    }

    private static String identifier(final RankNode node) {
        return node.getRank().getIdentifier();
    }
}
