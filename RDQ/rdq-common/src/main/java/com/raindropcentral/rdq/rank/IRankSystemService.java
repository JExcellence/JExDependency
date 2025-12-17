package com.raindropcentral.rdq.rank;

import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.database.entity.rank.RRank;
import com.raindropcentral.rdq.database.entity.rank.RRankTree;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Interface for rank system service that defines limits and capabilities
 * for the rank system. Free and Premium versions implement this differently.
 * 
 * @author JExcellence
 * @version 1.0.0
 * @since 6.0.0
 */
public interface IRankSystemService {

    /**
     * Checks if this is the premium version.
     * 
     * @return true if premium, false if free
     */
    boolean isPremium();

    /**
     * Gets the maximum number of rank trees a player can complete/absolve.
     * 
     * @return maximum absolvable rank trees
     */
    int getMaxAbsolvableRankTrees();

    /**
     * Gets the maximum number of ranks allowed per tree.
     * 
     * @return maximum ranks per tree
     */
    int getMaxRanksPerTree();

    /**
     * Gets the maximum number of concurrent active rank paths.
     * 
     * @return maximum active rank paths
     */
    int getMaxActiveRankPaths();

    /**
     * Checks if a player can start a new rank tree.
     * 
     * @param player the player to check
     * @return true if the player can start a new rank tree
     */
    boolean canStartNewRankTree(@NotNull RDQPlayer player);

    /**
     * Checks if a player can progress to a specific rank.
     * 
     * @param player the player to check
     * @param rank the target rank
     * @return true if the player can progress to this rank
     */
    boolean canProgressToRank(@NotNull RDQPlayer player, @NotNull RRank rank);

    /**
     * Checks if a rank tree should be shown as preview-only for a player.
     * 
     * @param player the player to check
     * @param rankTree the rank tree to check
     * @return true if the tree should be preview-only
     */
    boolean isPreviewOnly(@NotNull RDQPlayer player, @NotNull RRankTree rankTree);

    /**
     * Checks if a specific rank is beyond the free tier limit.
     * 
     * @param rank the rank to check
     * @param rankTree the rank tree containing the rank
     * @return true if the rank is beyond the limit
     */
    boolean isRankBeyondLimit(@NotNull RRank rank, @NotNull RRankTree rankTree);

    /**
     * Gets the number of rank trees the player has absolved/completed.
     * 
     * @param player the player to check
     * @return number of absolved rank trees
     */
    int getAbsolvedRankTreeCount(@NotNull RDQPlayer player);

    /**
     * Gets the number of currently active rank paths for a player.
     * 
     * @param player the player to check
     * @return number of active rank paths
     */
    int getActiveRankPathCount(@NotNull RDQPlayer player);

    /**
     * Gets a message explaining why an action is restricted (for free version).
     * 
     * @param restrictionType the type of restriction
     * @return the restriction message key for i18n
     */
    @NotNull String getRestrictionMessageKey(@NotNull RankRestrictionType restrictionType);

    /**
     * Types of restrictions that can occur in the free version.
     */
    enum RankRestrictionType {
        MAX_RANK_TREES_REACHED,
        MAX_RANKS_PER_TREE_REACHED,
        MAX_ACTIVE_PATHS_REACHED,
        RANK_TREE_PREVIEW_ONLY,
        RANK_BEYOND_LIMIT
    }
}
