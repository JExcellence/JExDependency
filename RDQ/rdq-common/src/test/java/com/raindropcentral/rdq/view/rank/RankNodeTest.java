package com.raindropcentral.rdq.view.rank;

import com.raindropcentral.rdq.config.item.IconSection;
import com.raindropcentral.rdq.database.entity.rank.RRank;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RankNodeTest {

    @Test
    void itAddsChildrenAndAvoidsDuplicates() {
        final RankNode parent = new RankNode(createRank("rank.parent", false));
        final RankNode firstChild = new RankNode(createRank("rank.child.one", false));
        final RankNode secondChild = new RankNode(createRank("rank.child.two", false));

        parent.addChild(firstChild);
        parent.addChild(secondChild);
        parent.addChild(firstChild);

        assertIterableEquals(List.of(firstChild, secondChild), parent.getChildren(),
            "addChild should store each child once and preserve insertion order");
        assertEquals(2, parent.getChildCount(), "getChildCount should reflect the stored children");
        assertThrows(UnsupportedOperationException.class, () -> parent.getChildren().add(firstChild),
            "getChildren should expose an unmodifiable view of the child list");
    }

    @Test
    void itAddsParentsAndAvoidsDuplicates() {
        final RankNode child = new RankNode(createRank("rank.child", false));
        final RankNode firstParent = new RankNode(createRank("rank.parent.one", false));
        final RankNode secondParent = new RankNode(createRank("rank.parent.two", false));

        child.addParent(firstParent);
        child.addParent(secondParent);
        child.addParent(firstParent);

        assertIterableEquals(List.of(firstParent, secondParent), child.getParents(),
            "addParent should store each parent once and preserve insertion order");
        assertEquals(2, child.getParentCount(), "getParentCount should reflect the stored parents");
        assertThrows(UnsupportedOperationException.class, () -> child.getParents().add(firstParent),
            "getParents should expose an unmodifiable view of the parent list");
    }

    @Test
    void itIdentifiesRootsUsingParentPresenceAndInitialRankFlag() {
        final RankNode detachedNode = new RankNode(createRank("rank.detached", false));
        assertTrue(detachedNode.isRoot(), "Nodes without parents should be treated as roots");

        final RankNode initialNode = new RankNode(createRank("rank.initial", true));
        initialNode.addParent(new RankNode(createRank("rank.parent", false)));
        assertTrue(initialNode.isRoot(), "Initial ranks should be treated as roots even with parents");

        final RankNode linkedNode = new RankNode(createRank("rank.linked", false));
        linkedNode.addParent(new RankNode(createRank("rank.upstream", false)));
        assertFalse(linkedNode.isRoot(), "Non-initial ranks with parents should not be roots");
    }

    @Test
    void itIdentifiesLeavesBasedOnChildren() {
        final RankNode candidate = new RankNode(createRank("rank.candidate", false));
        assertTrue(candidate.isLeaf(), "Nodes without children should be leaves");

        candidate.addChild(new RankNode(createRank("rank.successor", false)));
        assertFalse(candidate.isLeaf(), "Nodes with children should not be leaves");
    }

    @Test
    void itSummarisesRankStateInToString() {
        final RankNode node = new RankNode(createRank("rank.summary", false));
        node.addChild(new RankNode(createRank("rank.summary.child", false)));
        node.addParent(new RankNode(createRank("rank.summary.parent", false)));

        final String representation = node.toString();

        assertTrue(representation.contains("rank.summary"), "toString should include the rank identifier");
        assertTrue(representation.contains("children=1"), "toString should include the child count");
        assertTrue(representation.contains("parents=1"), "toString should include the parent count");
    }

    private static RRank createRank(final String identifier, final boolean initialRank) {
        return new RRank(
            identifier,
            identifier + ".name",
            identifier + ".description",
            identifier + ".group",
            identifier + ".prefix",
            identifier + ".suffix",
            createIcon(),
            initialRank,
            1,
            1,
            null
        );
    }

    private static IconSection createIcon() {
        return new IconSection(new EvaluationEnvironmentBuilder());
    }
}
