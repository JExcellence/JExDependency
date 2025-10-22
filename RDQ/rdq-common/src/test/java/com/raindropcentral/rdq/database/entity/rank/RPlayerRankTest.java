package com.raindropcentral.rdq.database.entity.rank;

import com.raindropcentral.rdq.config.item.IconSection;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RPlayerRankTest {

    @Test
    void itRejectsNullArgumentsInConstructors() {
        final RDQPlayer player = createPlayer("PlayerOne");
        final RRankTree rankTree = createRankTree("tree.primary");
        final RRank rank = createRank("rank.novice", rankTree);

        assertThrows(NullPointerException.class, () -> new RPlayerRank(null, rank, rankTree),
            "Primary constructor should reject null player");
        assertThrows(NullPointerException.class, () -> new RPlayerRank(player, null, rankTree),
            "Primary constructor should reject null current rank");
        assertThrows(NullPointerException.class, () -> new RPlayerRank(player, rank, null),
            "Primary constructor should reject null rank tree");

        assertThrows(NullPointerException.class, () -> new RPlayerRank(null, rank, rankTree, true),
            "Secondary constructor should reject null player");
        assertThrows(NullPointerException.class, () -> new RPlayerRank(player, null, rankTree, true),
            "Secondary constructor should reject null current rank");
        assertThrows(NullPointerException.class, () -> new RPlayerRank(player, rank, null, true),
            "Secondary constructor should reject null rank tree");

        assertThrows(NullPointerException.class, () -> new RPlayerRank(null, rank),
            "Convenience constructor should reject null player");
        assertThrows(NullPointerException.class, () -> new RPlayerRank(player, null),
            "Convenience constructor should reject null rank");
    }

    @Test
    void itInfersRankTreeFromRankWhenOmitted() {
        final RDQPlayer player = createPlayer("PlayerTwo");
        final RRankTree rankTree = createRankTree("tree.secondary");
        final RRank rank = createRank("rank.apprentice", rankTree);

        final RPlayerRank playerRank = new RPlayerRank(player, rank);

        assertSame(rankTree, playerRank.getRankTree(),
            "Convenience constructor should infer the rank tree from the provided rank");
        assertSame(rank, playerRank.getCurrentRank(),
            "Convenience constructor should store the provided rank as current");
        assertSame(player, playerRank.getRdqPlayer(),
            "Convenience constructor should store the provided player");
    }

    @Test
    void itUpdatesMutableStateThroughSettersAndActivationHelpers() {
        final RDQPlayer player = createPlayer("PlayerThree");
        final RRankTree initialTree = createRankTree("tree.initial");
        final RRank initialRank = createRank("rank.initiate", initialTree);
        final RPlayerRank playerRank = new RPlayerRank(player, initialRank, initialTree);

        final RRankTree replacementTree = createRankTree("tree.replacement");
        final RRank replacementRank = createRank("rank.advanced", replacementTree);

        assertSame(initialRank, playerRank.getCurrentRank(), "Constructor should store the current rank");
        assertSame(initialTree, playerRank.getRankTree(), "Constructor should store the rank tree");
        assertTrue(playerRank.isActive(), "Player ranks should default to active");
        assertTrue(playerRank.belongsToRankTree(initialTree),
            "belongsToRankTree should recognise the stored tree instance");
        assertTrue(playerRank.belongsToRankTree(initialTree.getIdentifier()),
            "belongsToRankTree(String) should recognise the stored tree identifier");

        playerRank.setCurrentRank(replacementRank);
        assertSame(replacementRank, playerRank.getCurrentRank(),
            "setCurrentRank should update the stored rank reference");
        assertThrows(NullPointerException.class, () -> playerRank.setCurrentRank(null),
            "setCurrentRank should reject null");

        playerRank.setRankTree(replacementTree);
        assertSame(replacementTree, playerRank.getRankTree(),
            "setRankTree should update the stored tree reference");
        assertThrows(NullPointerException.class, () -> playerRank.setRankTree(null),
            "setRankTree should reject null");

        assertTrue(playerRank.belongsToRankTree(replacementTree),
            "belongsToRankTree should reflect updated tree references");
        assertFalse(playerRank.belongsToRankTree(initialTree),
            "belongsToRankTree should return false for non-matching tree references");
        assertTrue(playerRank.belongsToRankTree(replacementTree.getIdentifier()),
            "belongsToRankTree(String) should reflect updated tree identifiers");
        assertFalse(playerRank.belongsToRankTree(initialTree.getIdentifier()),
            "belongsToRankTree(String) should return false for non-matching identifiers");
        assertThrows(NullPointerException.class, () -> playerRank.belongsToRankTree((RRankTree) null),
            "belongsToRankTree should reject null tree references");
        assertThrows(NullPointerException.class, () -> playerRank.belongsToRankTree((String) null),
            "belongsToRankTree(String) should reject null identifiers");

        playerRank.deactivate();
        assertFalse(playerRank.isActive(), "deactivate should mark the rank as inactive");
        playerRank.activate();
        assertTrue(playerRank.isActive(), "activate should mark the rank as active");
    }

    @Test
    void itComparesEqualityAndHashCodesUsingPlayerAndTreeReferences() {
        final RDQPlayer sharedPlayer = createPlayer("PlayerFour");
        final RRankTree sharedTree = createRankTree("tree.shared");
        final RRank firstRank = createRank("rank.specialist", sharedTree);
        final RRank secondRank = createRank("rank.expert", sharedTree);

        final RPlayerRank first = new RPlayerRank(sharedPlayer, firstRank, sharedTree);
        final RPlayerRank second = new RPlayerRank(sharedPlayer, secondRank, sharedTree);

        assertEquals(first, second, "Equality should depend solely on player and rank tree references");
        assertEquals(first.hashCode(), second.hashCode(),
            "hashCode should depend solely on player and rank tree references");

        final RDQPlayer otherPlayer = createPlayer("PlayerFive");
        final RPlayerRank differentPlayer = new RPlayerRank(otherPlayer, firstRank, sharedTree);
        assertNotEquals(first, differentPlayer, "Different player references should break equality");

        final RRankTree otherTree = createRankTree("tree.other");
        final RPlayerRank differentTree = new RPlayerRank(sharedPlayer, firstRank, otherTree);
        assertNotEquals(first, differentTree, "Different tree references should break equality");

        final RRankTree structurallySimilarTree = createRankTree("tree.shared");
        final RRank similarRank = createRank("rank.specialist", structurallySimilarTree);
        final RPlayerRank structurallySimilar = new RPlayerRank(sharedPlayer, similarRank, structurallySimilarTree);
        assertNotEquals(first, structurallySimilar,
            "Equality should rely on object identity of the rank tree, not merely matching identifiers");
    }

    private static RDQPlayer createPlayer(final String name) {
        return new RDQPlayer(UUID.nameUUIDFromBytes(name.getBytes()), name);
    }

    private static RRank createRank(final String identifier, final RRankTree tree) {
        return new RRank(
            identifier,
            identifier + ".name",
            identifier + ".description",
            identifier + ".group",
            identifier + ".prefix",
            identifier + ".suffix",
            createIcon(),
            false,
            1,
            1,
            tree
        );
    }

    private static RRankTree createRankTree(final String identifier) {
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
