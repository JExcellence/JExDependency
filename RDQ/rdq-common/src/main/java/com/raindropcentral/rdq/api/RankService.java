package com.raindropcentral.rdq.api;

import com.raindropcentral.rdq.rank.PlayerRankData;
import com.raindropcentral.rdq.rank.Rank;
import com.raindropcentral.rdq.rank.RankTree;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service interface for rank progression operations.
 *
 * <p>This sealed interface defines the contract for rank management, including
 * player rank retrieval, rank unlocking, and requirement checking. Implementations
 * are provided by {@link FreeRankService} (single active tree) and
 * {@link PremiumRankService} (multiple trees with cross-tree switching).
 *
 * <p>All methods return {@link CompletableFuture} for async execution.
 *
 * @see FreeRankService
 * @see PremiumRankService
 * @see PlayerRankData
 * @see RankTree
 */
public sealed interface RankService permits FreeRankService, PremiumRankService {

    /**
     * Retrieves the rank data for a player.
     *
     * @param playerId the UUID of the player
     * @return a future containing the player's rank data, or empty if not found
     */
    CompletableFuture<Optional<PlayerRankData>> getPlayerRanks(@NotNull UUID playerId);

    /**
     * Attempts to unlock a rank for a player.
     *
     * <p>This method checks all requirements before unlocking. If requirements
     * are not met, the unlock will fail and return false.
     *
     * @param playerId the UUID of the player
     * @param rankId the ID of the rank to unlock
     * @return a future containing true if the rank was unlocked, false otherwise
     */
    CompletableFuture<Boolean> unlockRank(@NotNull UUID playerId, @NotNull String rankId);

    /**
     * Checks if a player meets all requirements for a rank.
     *
     * @param playerId the UUID of the player
     * @param rankId the ID of the rank to check
     * @return a future containing true if all requirements are met
     */
    CompletableFuture<Boolean> checkRequirements(@NotNull UUID playerId, @NotNull String rankId);

    /**
     * Retrieves all available rank trees.
     *
     * @return a future containing the list of enabled rank trees
     */
    CompletableFuture<List<RankTree>> getAvailableRankTrees();

    /**
     * Retrieves a specific rank by ID.
     *
     * @param rankId the ID of the rank
     * @return a future containing the rank, or empty if not found
     */
    CompletableFuture<Optional<Rank>> getRank(@NotNull String rankId);

    /**
     * Retrieves a specific rank tree by ID.
     *
     * @param treeId the ID of the rank tree
     * @return a future containing the rank tree, or empty if not found
     */
    CompletableFuture<Optional<RankTree>> getRankTree(@NotNull String treeId);

    /**
     * Reloads rank configurations from disk.
     *
     * @return a future that completes when reload is finished
     */
    CompletableFuture<Void> reload();
}
