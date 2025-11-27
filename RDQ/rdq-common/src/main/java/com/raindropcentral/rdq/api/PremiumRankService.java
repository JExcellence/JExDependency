package com.raindropcentral.rdq.api;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Premium edition rank service with advanced features.
 *
 * <p>Extends the base rank service with support for multiple active rank trees
 * and cross-tree switching capabilities.
 *
 * @see RankService
 * @see FreeRankService
 */
public non-sealed interface PremiumRankService extends RankService {

    /**
     * Switches a player's active rank tree.
     *
     * <p>Subject to cooldown restrictions and tier threshold requirements.
     *
     * @param playerId the UUID of the player
     * @param fromTreeId the ID of the current tree
     * @param toTreeId the ID of the target tree
     * @return a future containing true if switch was successful
     */
    CompletableFuture<Boolean> switchRankTree(@NotNull UUID playerId, @NotNull String fromTreeId, @NotNull String toTreeId);

    /**
     * Checks if a player can switch rank trees.
     *
     * <p>Considers cooldown status and tier threshold requirements.
     *
     * @param playerId the UUID of the player
     * @return a future containing true if switching is allowed
     */
    CompletableFuture<Boolean> canSwitchRankTree(@NotNull UUID playerId);

    /**
     * Gets the maximum number of active rank trees allowed.
     *
     * @return the maximum active trees (-1 for unlimited)
     */
    int getMaxActiveRankTrees();
}
