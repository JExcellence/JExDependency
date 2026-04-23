package de.jexcellence.quests.api;

import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Immutable single-row snapshot for a rank leaderboard. Position is
 * 1-indexed for display purposes.
 *
 * @param position 1-indexed leaderboard rank (1 = top)
 * @param playerUuid player
 * @param playerName last-seen name (may be outdated for offline players)
 * @param treeIdentifier rank tree
 * @param rankIdentifier player's current rank identifier
 * @param rankOrderIndex the rank's ordinal position within the tree (higher = better)
 * @param promotedAt timestamp of their last promotion
 * @param treeCompleted whether the player has topped out the tree
 */
public record RankLeaderboardEntry(
        int position,
        @NotNull UUID playerUuid,
        @NotNull String playerName,
        @NotNull String treeIdentifier,
        @NotNull String rankIdentifier,
        int rankOrderIndex,
        @NotNull LocalDateTime promotedAt,
        boolean treeCompleted
) {
}
