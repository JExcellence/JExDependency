package com.raindropcentral.rdq.api;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Premium edition perk service with advanced features.
 *
 * <p>Extends the base perk service with support for multiple active perks
 * and access to premium perk types.
 *
 * @see PerkService
 * @see FreePerkService
 */
public non-sealed interface PremiumPerkService extends PerkService {

    /**
     * Gets the maximum number of perks a player can have active simultaneously.
     *
     * @param playerId the UUID of the player
     * @return a future containing the max active perks for this player
     */
    CompletableFuture<Integer> getMaxActivePerks(@NotNull UUID playerId);

    /**
     * Checks if a player has access to a premium perk.
     *
     * @param playerId the UUID of the player
     * @param perkId the ID of the perk
     * @return a future containing true if the player has premium access
     */
    CompletableFuture<Boolean> hasPremiumPerkAccess(@NotNull UUID playerId, @NotNull String perkId);
}
