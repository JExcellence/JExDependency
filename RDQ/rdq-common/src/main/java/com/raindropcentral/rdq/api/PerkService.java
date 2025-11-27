package com.raindropcentral.rdq.api;

import com.raindropcentral.rdq.perk.Perk;
import com.raindropcentral.rdq.perk.PlayerPerkState;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service interface for perk/ability operations.
 *
 * <p>This sealed interface defines the contract for perk management, including
 * perk unlocking, activation, deactivation, and cooldown tracking. Implementations
 * are provided by {@link FreePerkService} (single active perk) and
 * {@link PremiumPerkService} (multiple active perks with premium types).
 *
 * <p>All methods return {@link CompletableFuture} for async execution.
 *
 * @see FreePerkService
 * @see PremiumPerkService
 * @see Perk
 * @see PlayerPerkState
 */
public sealed interface PerkService permits FreePerkService, PremiumPerkService {

    /**
     * Retrieves perks available to a player based on their requirements.
     *
     * @param playerId the UUID of the player
     * @return a future containing perks the player can access
     */
    CompletableFuture<List<Perk>> getAvailablePerks(@NotNull UUID playerId);

    /**
     * Retrieves all registered perks.
     *
     * @return a future containing all enabled perks
     */
    CompletableFuture<List<Perk>> getAllPerks();

    /**
     * Retrieves a specific perk by ID.
     *
     * @param perkId the ID of the perk
     * @return a future containing the perk, or empty if not found
     */
    CompletableFuture<Optional<Perk>> getPerk(@NotNull String perkId);

    /**
     * Unlocks a perk for a player.
     *
     * <p>Checks requirements before unlocking. May deduct currency if
     * the perk has a cost requirement.
     *
     * @param playerId the UUID of the player
     * @param perkId the ID of the perk to unlock
     * @return a future containing true if unlocked successfully
     */
    CompletableFuture<Boolean> unlockPerk(@NotNull UUID playerId, @NotNull String perkId);

    /**
     * Activates a perk for a player.
     *
     * <p>The perk must be unlocked and not on cooldown. For toggleable perks,
     * effects are applied immediately. For event-based perks, the perk is
     * registered to trigger on the appropriate event.
     *
     * @param playerId the UUID of the player
     * @param perkId the ID of the perk to activate
     * @return a future containing true if activated successfully
     */
    CompletableFuture<Boolean> activatePerk(@NotNull UUID playerId, @NotNull String perkId);

    /**
     * Deactivates a perk for a player.
     *
     * <p>Removes active effects and starts the cooldown timer.
     *
     * @param playerId the UUID of the player
     * @param perkId the ID of the perk to deactivate
     * @return a future containing true if deactivated successfully
     */
    CompletableFuture<Boolean> deactivatePerk(@NotNull UUID playerId, @NotNull String perkId);

    /**
     * Gets the remaining cooldown duration for a perk.
     *
     * @param playerId the UUID of the player
     * @param perkId the ID of the perk
     * @return a future containing the remaining cooldown, or empty if not on cooldown
     */
    CompletableFuture<Optional<Duration>> getCooldownRemaining(@NotNull UUID playerId, @NotNull String perkId);

    /**
     * Gets the state of a specific perk for a player.
     *
     * @param playerId the UUID of the player
     * @param perkId the ID of the perk
     * @return a future containing the perk state, or empty if not found
     */
    CompletableFuture<Optional<PlayerPerkState>> getPlayerPerkState(@NotNull UUID playerId, @NotNull String perkId);

    /**
     * Gets all perk states for a player.
     *
     * @param playerId the UUID of the player
     * @return a future containing all perk states for the player
     */
    CompletableFuture<List<PlayerPerkState>> getPlayerPerks(@NotNull UUID playerId);

    /**
     * Cleans up perk data for a player.
     *
     * <p>Called when a player disconnects to remove active effects
     * and clear cached data.
     *
     * @param playerId the UUID of the player
     * @return a future that completes when cleanup is done
     */
    CompletableFuture<Void> cleanupPlayer(@NotNull UUID playerId);

    /**
     * Reloads perk configurations from disk.
     *
     * @return a future that completes when reload is finished
     */
    CompletableFuture<Void> reload();
}
