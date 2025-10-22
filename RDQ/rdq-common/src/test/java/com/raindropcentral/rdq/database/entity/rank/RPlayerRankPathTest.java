package com.raindropcentral.rdq.database.entity.rank;

import com.raindropcentral.rdq.config.item.IconSection;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RPlayerRankPathTest {

    @Test
    void constructorRequiresNonNullPlayer() {
        final RRankTree rankTree = createRankTree("tree.pioneers");

        final NullPointerException exception = assertThrows(NullPointerException.class,
            () -> new RPlayerRankPath(null, rankTree),
            "Constructor should reject a null player reference");

        assertEquals("player cannot be null", exception.getMessage(),
            "Constructor should signal the violated player invariant");
    }

    @Test
    void constructorRequiresNonNullRankTree() {
        final RDQPlayer player = createPlayer("Pathfinder");

        final NullPointerException exception = assertThrows(NullPointerException.class,
            () -> new RPlayerRankPath(player, null),
            "Constructor should reject a null rank tree reference");

        assertEquals("rankTree cannot be null", exception.getMessage(),
            "Constructor should signal the violated rank tree invariant");
    }

    @Test
    void itClampsProgressAndPropagatesCompletionState() {
        final RPlayerRankPath path = new RPlayerRankPath(createPlayer("Explorer"), createRankTree("tree.explorers"));

        assertFalse(path.isCompleted(), "Rank paths should start incomplete");
        assertEquals(0.0, path.getCompletionPercentage(), 0.0001,
            "Rank paths should start with zero completion");

        path.setCompletionPercentage(-25.5);
        assertEquals(0.0, path.getCompletionPercentage(), 0.0001,
            "setCompletionPercentage should clamp negative values to zero");
        assertFalse(path.isCompleted(), "Clamping to zero should not mark the path completed");

        path.incrementCompletionPercentage(40.5);
        assertEquals(40.5, path.getCompletionPercentage(), 0.0001,
            "incrementCompletionPercentage should add to the current progress");
        assertFalse(path.isCompleted(), "Progress below 100 percent should keep the path incomplete");

        path.incrementCompletionPercentage(70.0);
        assertEquals(100.0, path.getCompletionPercentage(), 0.0001,
            "incrementCompletionPercentage should clamp progress to 100 percent");
        assertTrue(path.isCompleted(), "Reaching 100 percent should mark the path complete");

        path.setCompleted(false);
        assertFalse(path.isCompleted(), "setCompleted(false) should clear the completion flag");
        assertEquals(100.0, path.getCompletionPercentage(), 0.0001,
            "setCompleted(false) should not alter the recorded completion percentage");

        path.setCompletionPercentage(150.0);
        assertEquals(100.0, path.getCompletionPercentage(), 0.0001,
            "setCompletionPercentage should clamp values above 100 percent");
        assertTrue(path.isCompleted(), "Clamping to 100 percent should mark the path completed");

        path.setCompleted(false);
        path.setCompletionPercentage(-10.0);
        assertEquals(0.0, path.getCompletionPercentage(), 0.0001,
            "setCompletionPercentage should continue clamping to zero after clearing completion");
        assertFalse(path.isCompleted(), "Clamping to zero after clearing completion should keep the flag false");

        path.setCompleted(true);
        assertTrue(path.isCompleted(), "setCompleted(true) should mark the path complete");
        assertEquals(100.0, path.getCompletionPercentage(), 0.0001,
            "setCompleted(true) should force the completion percentage to 100 percent");
    }

    @Test
    void equalsAndHashCodeDependOnPlayerAndRankTree() {
        final UUID sharedPlayerId = UUID.randomUUID();
        final RDQPlayer playerPrimary = new RDQPlayer(sharedPlayerId, "Voyager");
        final RDQPlayer playerCopy = new RDQPlayer(sharedPlayerId, "Voyager");
        final RRankTree primaryTree = createRankTree("tree.shared");
        final RRankTree equivalentTree = createRankTree("tree.shared");

        final RPlayerRankPath first = new RPlayerRankPath(playerPrimary, primaryTree);
        final RPlayerRankPath sameIdentity = new RPlayerRankPath(playerCopy, equivalentTree);
        final RPlayerRankPath differentPlayer = new RPlayerRankPath(createPlayer("Trailblazer"), primaryTree);
        final RPlayerRankPath differentTree = new RPlayerRankPath(playerPrimary, createRankTree("tree.alternative"));

        assertEquals(first, sameIdentity, "Paths with equal players and trees should be considered equal");
        assertEquals(first.hashCode(), sameIdentity.hashCode(),
            "Equal paths should share the same hash code");
        assertNotEquals(first, differentPlayer,
            "Changing the player should break equality even when the tree matches");
        assertNotEquals(first, differentTree,
            "Changing the tree should break equality even when the player matches");
        assertNotEquals(first, null, "Paths should not be equal to null references");
        assertNotEquals(first, "not a path", "Paths should not be equal to other object types");
    }

    private static RDQPlayer createPlayer(final String name) {
        return new RDQPlayer(UUID.randomUUID(), name);
    }

    private static RRankTree createRankTree(final String identifier) {
        return new RRankTree(
            identifier,
            identifier + ".name",
            identifier + ".description",
            new IconSection(new EvaluationEnvironmentBuilder()),
            1,
            0,
            true,
            false
        );
    }
}
