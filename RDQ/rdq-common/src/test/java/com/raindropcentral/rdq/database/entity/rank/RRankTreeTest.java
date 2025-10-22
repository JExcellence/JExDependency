package com.raindropcentral.rdq.database.entity.rank;

import com.raindropcentral.rdq.config.item.IconSection;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RRankTreeTest {

    @Test
    void itPopulatesConstructorFields() {
        final IconSection icon = createIcon();
        final RRankTree tree = new RRankTree(
            "tree.forest",
            "tree.forest.name",
            "tree.forest.description",
            icon,
            7,
            3,
            false,
            true
        );

        assertEquals("tree.forest", tree.getIdentifier(), "Constructor should persist the identifier");
        assertEquals("tree.forest.name", tree.getDisplayNameKey(), "Constructor should persist the display name key");
        assertEquals("tree.forest.description", tree.getDescriptionKey(), "Constructor should persist the description key");
        assertSame(icon, tree.getIcon(), "Constructor should retain the provided icon instance");
        assertEquals(7, tree.getDisplayOrder(), "Constructor should persist the display order");
        assertEquals(3, tree.getMinimumRankTreesToBeDone(), "Constructor should persist the minimum completion requirement");
        assertFalse(tree.isEnabled(), "Constructor should propagate the enabled flag");
        assertTrue(tree.isFinalRankTree(), "Constructor should propagate the final rank tree flag");
    }

    @Test
    void itReplacesConnectedTreesAndProtectsInternalState() {
        final RRankTree tree = createTree("tree.alpha");
        final RRankTree beta = createTree("tree.beta");
        final RRankTree gamma = createTree("tree.gamma");

        final List<RRankTree> connections = new ArrayList<>(List.of(beta, gamma));
        tree.setConnectedRankTrees(connections);

        assertIterableEquals(List.of(beta, gamma), tree.getConnectedRankTrees(),
            "setConnectedRankTrees should persist the supplied trees");
        assertThrows(UnsupportedOperationException.class,
            () -> tree.getConnectedRankTrees().add(createTree("tree.delta")),
            "getConnectedRankTrees should expose an immutable view");

        connections.add(createTree("tree.delta"));
        assertIterableEquals(List.of(beta, gamma), tree.getConnectedRankTrees(),
            "Mutating the source list should not impact the stored connections");

        tree.setConnectedRankTrees(List.of(gamma));
        assertIterableEquals(List.of(gamma), tree.getConnectedRankTrees(),
            "setConnectedRankTrees should replace previously stored connections");
    }

    @Test
    void itAllowsEnablingAndCompletionFlagsToBeToggled() {
        final RRankTree tree = new RRankTree(
            "tree.summit",
            "tree.summit.name",
            "tree.summit.description",
            createIcon(),
            2,
            1,
            true,
            false
        );

        tree.setEnabled(false);
        assertFalse(tree.isEnabled(), "setEnabled should update the enabled flag");
        tree.setEnabled(true);
        assertTrue(tree.isEnabled(), "setEnabled should allow re-enabling the tree");

        tree.setFinalRankTree(true);
        assertTrue(tree.isFinalRankTree(), "setFinalRankTree should update the final flag");
        tree.setFinalRankTree(false);
        assertFalse(tree.isFinalRankTree(), "setFinalRankTree should allow clearing the final flag");
    }

    private static RRankTree createTree(final String identifier) {
        return new RRankTree(
            identifier,
            identifier + ".name",
            identifier + ".description",
            createIcon(),
            1,
            0,
            true,
            false
        );
    }

    private static IconSection createIcon() {
        return new IconSection(new EvaluationEnvironmentBuilder());
    }
}
