package de.jexcellence.quests.api;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Immutable snapshot of a player's position within a rank tree.
 *
 * @param playerUuid target player
 * @param treeIdentifier rank tree identifier
 * @param rankIdentifier current rank identifier
 * @param orderIndex rank's ordinal position within the tree (0 = lowest)
 * @param progressionPercent 0–100 advancement toward the next promotion
 * @param treeCompleted whether the player has topped out the tree
 */
public record RankSnapshot(
        @NotNull UUID playerUuid,
        @NotNull String treeIdentifier,
        @NotNull String rankIdentifier,
        int orderIndex,
        int progressionPercent,
        boolean treeCompleted
) {
}
