/*
package com.raindropcentral.rdq.utility.perk;

import com.raindropcentral.rdq.database.entity.perk.RPerk;
import com.raindropcentral.rdq.database.entity.perk.RPlayerPerk;
import com.raindropcentral.rdq.database.repository.RPlayerPerkRepository;
import com.raindropcentral.rdq.type.EPerkType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

*/
/**
 * Service responsible for managing perk ownership, enablement, and limits.
 * <p>
 * This service handles:
 * <ul>
 *     <li>Perk ownership management (granting/revoking perks)</li>
 *     <li>Enabling/disabling perks with active perk limit enforcement</li>
 *     <li>Managing perk limits and quotas</li>
 *     <li>Querying perk ownership and status</li>
 * </ul>
 * </p>
 * <p>
 * This service is separate from {@link com.raindropcentral.rdq2.service.RPerkActivationService} which handles
 * the actual activation/deactivation of perk effects.
 * </p>
 *
 * Note: This class exposes synchronous methods and convenience asynchronous variants
 * that dispatch to the plugin's executor.
 *
 * @author JExcellence
 * @version 1.1.1
 * @since TBD
 *//*

public class RPerkManagementService {
    
    */
/**
     * Default maximum number of perks that can be enabled simultaneously.
     *//*

    private static final int DEFAULT_MAX_ENABLED_PERKS = 5;
    
    private final RDQImpl rdq;
    private final RPlayerPerkRepository RPlayerPerkRepository;
    
    */
/**
     * Preferred constructor.
     *
     * @param rdq plugin instance
     *//*

    public RPerkManagementService(
        final @NotNull RDQImpl rdq
    ) {
        
        this.rdq = rdq;
        this.RPlayerPerkRepository = rdq.getPlayerPerkRepository();
    }
    
    */
/**
     * Grants a perk to a player, creating the ownership association.
     *
     * @param player  the player to grant the perk to
     * @param perk    the perk to grant
     * @param enabled whether the perk should be initially enabled
     *
     * @return the created RPlayerPerk association
     *
     * @throws IllegalStateException if the player already owns the perk
     *//*

    public RPlayerPerk grantPerk(
        final @NotNull RDQPlayer player,
        final @NotNull RPerk perk,
        final boolean enabled
    ) {
        
        if (playerOwns(
            player,
            perk
        )) {
            throw new IllegalStateException(
                "Player " + player.getPlayerName() + " already owns perk " + perk.getIdentifier()
            );
        }
        
        final RPlayerPerk playerPerk = new RPlayerPerk(
            player,
            perk,
            enabled
        );
        return this.RPlayerPerkRepository.create(playerPerk);
    }
    
    */
/**
     * Grants a perk to a player with the perk disabled by default.
     *
     * @param player the player to grant the perk to
     * @param perk   the perk to grant
     *
     * @return the created RPlayerPerk association
     *//*

    public RPlayerPerk grantPerk(
        final @NotNull RDQPlayer player,
        final @NotNull RPerk perk
    ) {
        
        return grantPerk(
            player,
            perk,
            false
        );
    }
    
    */
/**
     * Async variant of {@link #grantPerk(rdqPlayer, RPerk, boolean)}.
     *//*

    public CompletableFuture<RPlayerPerk> grantPerkAsync(
        final @NotNull RDQPlayer player,
        final @NotNull RPerk perk,
        final boolean enabled
    ) {
        
        return CompletableFuture.supplyAsync(
            () -> this.grantPerk(
                player,
                perk,
                enabled
            ),
            this.rdq.getExecutor()
        );
    }
    
    */
/**
     * Revokes a perk from a player, removing the ownership association.
     *
     * @param player the player to revoke the perk from
     * @param perk   the perk to revoke
     *
     * @return true if the perk was revoked, false if the player didn't own it
     *//*

    public boolean revokePerk(
        final @NotNull RDQPlayer player,
        final @NotNull RPerk perk
    ) {
        
        final RPlayerPerk playerPerk = this.RPlayerPerkRepository.findByAttributes(Map.of(
            "player",
            player,
            "perk",
            perk
        ));
        if (playerPerk != null) {
            this.RPlayerPerkRepository.delete(playerPerk.getId());
            return true;
        }
        return false;
    }
    
    */
/**
     * Async variant of {@link #revokePerk(rdqPlayer, RPerk)}.
     *//*

    public CompletableFuture<Boolean> revokePerkAsync(
        final @NotNull RDQPlayer player,
        final @NotNull RPerk perk
    ) {
        
        return CompletableFuture.supplyAsync(
            () -> this.revokePerk(
                player,
                perk
            ),
            this.rdq.getExecutor()
        );
    }
    
    */
/**
     * Enables a perk for a player if they haven't reached their enabled perk limit.
     *
     * @param player          the player for whom to enable the perk
     * @param perk            the perk to enable
     * @param maxEnabledPerks the maximum number of enabled perks allowed
     *
     * @return true if the perk was enabled successfully, false otherwise
     *//*

    public boolean enablePerk(
        final @NotNull RDQPlayer player,
        final @NotNull RPerk perk,
        final int maxEnabledPerks
    ) {
        
        final RPlayerPerk playerPerk = this.RPlayerPerkRepository.findByAttributes(Map.of(
            "player",
            player,
            "perk",
            perk
        ));
        
        if (
            playerPerk == null
        ) {
            return false;
        }
        
        if (playerPerk.isEnabled()) {
            return false;
        }
        
        if (
            this.getEnabledPerkCount(player) >= maxEnabledPerks
        ) {
            return false;
        }
        
        playerPerk.setEnabled(true);
        this.RPlayerPerkRepository.update(playerPerk);
        return true;
    }
    
    */
/**
     * Enables a perk for a player using the default enabled perk limit.
     *//*

    public boolean enablePerk(
        final @NotNull RDQPlayer player,
        final @NotNull RPerk perk
    ) {
        
        return enablePerk(
            player,
            perk,
            DEFAULT_MAX_ENABLED_PERKS
        );
    }
    
    */
/**
     * Convenience overload to enable by perk identifier.
     *//*

    public boolean enablePerk(
        final @NotNull RDQPlayer player,
        final @NotNull String perkIdentifier,
        final int maxEnabledPerks
    ) {
        
        final RPlayerPerk playerPerk = this.RPlayerPerkRepository.findByAttributes(Map.of(
            "player",
            player,
            "perk.identifier",
            perkIdentifier
        ));
        if (playerPerk == null)
            return false;
        
        if (playerPerk.isEnabled())
            return false;
        
        if (getEnabledPerkCount(player) >= maxEnabledPerks) {
            return false;
        }
        
        playerPerk.setEnabled(true);
        this.RPlayerPerkRepository.update(playerPerk);
        return true;
    }
    
    */
/**
     * Convenience overload to enable by perk identifier with default limit.
     *//*

    public boolean enablePerk(
        final @NotNull RDQPlayer player,
        final @NotNull String perkIdentifier
    ) {
        
        return enablePerk(
            player,
            perkIdentifier,
            DEFAULT_MAX_ENABLED_PERKS
        );
    }
    
    */
/**
     * Async variant of {@link #enablePerk(rdqPlayer, RPerk, int)}.
     *//*

    public CompletableFuture<Boolean> enablePerkAsync(
        final @NotNull RDQPlayer player,
        final @NotNull RPerk perk,
        final int maxEnabledPerks
    ) {
        
        return CompletableFuture.supplyAsync(
            () -> enablePerk(
                player,
                perk,
                maxEnabledPerks
            ),
            rdq.getExecutor()
        );
    }
    
    */
/**
     * Disables a perk for a player.
     *
     * @param player the player for whom to disable the perk
     * @param perk   the perk to disable
     *
     * @return true if the perk was disabled successfully, false otherwise
     *//*

    public boolean disablePerk(
        final @NotNull RDQPlayer player,
        final @NotNull RPerk perk
    ) {
        
        final RPlayerPerk playerPerk = this.RPlayerPerkRepository.findByAttributes(Map.of(
            "player",
            player,
            "perk",
            perk
        ));
        if (playerPerk == null) {
            return false;
        }
        
        if (! playerPerk.isEnabled()) {
            return false;
        }
        
        playerPerk.setEnabled(false);
        this.RPlayerPerkRepository.update(playerPerk);
        return true;
    }
    
    */
/**
     * Convenience overload to disable by perk identifier.
     *//*

    public boolean disablePerk(
        final @NotNull RDQPlayer player,
        final @NotNull String perkIdentifier
    ) {
        
        final RPlayerPerk playerPerk = this.RPlayerPerkRepository.findByAttributes(Map.of(
            "player",
            player,
            "perk.identifier",
            perkIdentifier
        ));
        if (playerPerk == null)
            return false;
        
        if (! playerPerk.isEnabled())
            return false;
        
        playerPerk.setEnabled(false);
        this.RPlayerPerkRepository.update(playerPerk);
        return true;
    }
    
    */
/**
     * Async variant of {@link #disablePerk(rdqPlayer, RPerk)}.
     *//*

    public CompletableFuture<Boolean> disablePerkAsync(
        final @NotNull RDQPlayer player,
        final @NotNull RPerk perk
    ) {
        
        return CompletableFuture.supplyAsync(
            () -> disablePerk(
                player,
                perk
            ),
            rdq.getExecutor()
        );
    }
    
    */
/**
     * Toggles the enabled state of a perk for a player.
     *
     * @param player          the player for whom to toggle the perk
     * @param perk            the perk to toggle
     * @param maxEnabledPerks the maximum number of enabled perks allowed
     *
     * @return true if the toggle was successful, false otherwise
     *//*

    public boolean togglePerkEnabled(
        final @NotNull RDQPlayer player,
        final @NotNull RPerk perk,
        final int maxEnabledPerks
    ) {
        
        final RPlayerPerk playerPerk = this.RPlayerPerkRepository.findByAttributes(Map.of(
            "player",
            player,
            "perk",
            perk
        ));
        if (playerPerk == null) {
            return false;
        }
        
        if (playerPerk.isEnabled()) {
            playerPerk.setEnabled(false);
        } else {
            if (getEnabledPerkCount(player) >= maxEnabledPerks) {
                return false;
            }
            playerPerk.setEnabled(true);
        }
	    this.RPlayerPerkRepository.update(playerPerk);
	    return true;
    }
    
    */
/**
     * Convenience overload to toggle by perk identifier.
     *//*

    public boolean togglePerkEnabled(
        final @NotNull RDQPlayer player,
        final @NotNull String perkIdentifier,
        final int maxEnabledPerks
    ) {
        
        final RPlayerPerk playerPerk = this.RPlayerPerkRepository.findByAttributes(Map.of(
            "player",
            player,
            "perk.identifier",
            perkIdentifier
        ));
        if (playerPerk == null)
            return false;
        
        if (playerPerk.isEnabled()) {
            playerPerk.setEnabled(false);
        } else {
            if (getEnabledPerkCount(player) >= maxEnabledPerks) {
                return false;
            }
            playerPerk.setEnabled(true);
        }
	    this.RPlayerPerkRepository.update(playerPerk);
	    return true;
    }
    
    */
/**
     * Async variant of {@link #togglePerkEnabled(rdqPlayer, RPerk, int)}.
     *//*

    public CompletableFuture<Boolean> togglePerkEnabledAsync(
        final @NotNull RDQPlayer player,
        final @NotNull RPerk perk,
        final int maxEnabledPerks
    ) {
        
        return CompletableFuture.supplyAsync(
            () -> togglePerkEnabled(
                player,
                perk,
                maxEnabledPerks
            ),
            rdq.getExecutor()
        );
    }
    
    */
/**
     * Disables all perks for a player.
     *
     * @param player the player for whom to disable all perks
     *
     * @return the number of perks that were disabled
     *//*

    public int disableAllPerks(final @NotNull RDQPlayer player) {
        
        final List<RPlayerPerk> enabledPerks = this.RPlayerPerkRepository.findListByAttributes(Map.of(
            "player",
            player,
            "enabled",
            true
        ));
        for (final RPlayerPerk playerPerk : enabledPerks) {
            playerPerk.setEnabled(false);
            this.RPlayerPerkRepository.update(playerPerk);
        }
        return enabledPerks.size();
    }
    
    */
/**
     * Async variant of {@link #disableAllPerks(rdqPlayer)}.
     *//*

    public CompletableFuture<Integer> disableAllPerksAsync(final @NotNull RDQPlayer player) {
        
        return CompletableFuture.supplyAsync(
            () -> disableAllPerks(player),
            rdq.getExecutor()
        );
    }
    
    */
/**
     * Checks if a player owns a specific perk.
     *//*

    public boolean playerOwns(
        final @NotNull RDQPlayer player,
        final @NotNull RPerk perk
    ) {
        
        return this.RPlayerPerkRepository.findByAttributes(Map.of(
            "player",
            player,
            "perk",
            perk
        )) != null;
    }
    
    */
/**
     * Checks if a player owns a perk by identifier.
     *//*

    public boolean playerOwns(
        final @NotNull RDQPlayer player,
        final @NotNull String perkIdentifier
    ) {
        
        return this.RPlayerPerkRepository.findByAttributes(Map.of(
            "player",
            player,
            "perk.identifier",
            perkIdentifier
        )) != null;
    }
    
    */
/**
     * Checks if a player has a specific perk enabled.
     *//*

    public boolean playerHasEnabled(
        final @NotNull RDQPlayer player,
        final @NotNull RPerk perk
    ) {
        
        return this.RPlayerPerkRepository.findByAttributes(Map.of(
            "player",
            player,
            "perk",
            perk,
            "enabled",
            true
        )) != null;
    }
    
    */
/**
     * Gets the RPlayerPerk association for a player and perk.
     *//*

    public @Nullable RPlayerPerk getPlayerPerk(
        final @NotNull RDQPlayer player,
        final @NotNull RPerk perk
    ) {
        
        return this.RPlayerPerkRepository.findByAttributes(Map.of(
            "player",
            player,
            "perk",
            perk
        ));
    }
    
    */
/**
     * Gets the RPlayerPerk association for a player and perk identifier.
     *//*

    public @Nullable RPlayerPerk getPlayerPerk(
        final @NotNull RDQPlayer player,
        final @NotNull String perkIdentifier
    ) {
        
        return this.RPlayerPerkRepository.findByAttributes(Map.of(
            "player",
            player,
            "perk.identifier",
            perkIdentifier
        ));
    }
    
    */
/**
     * Gets all perks owned by a player.
     *//*

    public List<RPlayerPerk> getOwnedPlayerPerks(final @NotNull RDQPlayer player) {
        
        return this.RPlayerPerkRepository.findListByAttributes(Map.of(
            "player",
            player
        ));
    }
    
    */
/**
     * Gets all enabled perks for a player.
     *//*

    public List<RPlayerPerk> getEnabledPlayerPerks(final @NotNull RDQPlayer player) {
        
        return this.RPlayerPerkRepository.findListByAttributes(Map.of(
            "player",
            player,
            "enabled",
            true
        ));
    }
    
    */
/**
     * Gets all enabled perks of a specific type for a player.
     *//*

    public List<RPlayerPerk> getEnabledPlayerPerksByType(
        final @NotNull RDQPlayer player,
        final @NotNull EPerkType perkType
    ) {
        
        return this.RPlayerPerkRepository.findListByAttributes(Map.of(
            "player",
            player,
            "enabled",
            true,
            "perk.perkType",
            perkType
        ));
    }
    
    */
/**
     * Gets all active perks for a player.
     *//*

    public List<RPlayerPerk> getActivePlayerPerks(final @NotNull RDQPlayer player) {
        
        return this.RPlayerPerkRepository.findListByAttributes(Map.of(
            "player",
            player,
            "active",
            true
        ));
    }
    
    */
/**
     * Gets the number of perks owned by a player.
     *//*

    public int getOwnedPerkCount(final @NotNull RDQPlayer player) {
        
        return this.RPlayerPerkRepository.findListByAttributes(Map.of(
            "player",
            player
        )).size();
    }
    
    */
/**
     * Gets the number of enabled perks for a player.
     *//*

    public int getEnabledPerkCount(final @NotNull RDQPlayer player) {
        
        return this.RPlayerPerkRepository.findListByAttributes(Map.of(
            "player",
            player,
            "enabled",
            true
        )).size();
    }
    
    */
/**
     * Gets the number of active perks for a player.
     *//*

    public int getActivePerkCount(final @NotNull RDQPlayer player) {
        
        return this.RPlayerPerkRepository.findListByAttributes(Map.of(
            "player",
            player,
            "active",
            true
        )).size();
    }
    
    */
/**
     * Checks if a player can enable another perk without exceeding the limit.
     *//*

    public boolean canEnableAnotherPerk(
        final @NotNull RDQPlayer player,
        final int maxEnabledPerks
    ) {
        
        return getEnabledPerkCount(player) < maxEnabledPerks;
    }
    
    */
/**
     * Checks if a player can enable another perk using the default limit.
     *//*

    public boolean canEnableAnotherPerk(final @NotNull RDQPlayer player) {
        
        return canEnableAnotherPerk(
            player,
            DEFAULT_MAX_ENABLED_PERKS
        );
    }
    
    */
/**
     * Gets the default maximum number of enabled perks.
     *//*

    public static int getDefaultMaxEnabledPerks() {
        
        return DEFAULT_MAX_ENABLED_PERKS;
    }
    
    */
/**
     * Grants multiple perks to a player.
     *
     * @param player  the player to grant perks to
     * @param perks   the perks to grant
     * @param enabled whether the perks should be initially enabled
     *
     * @return the list of created RPlayerPerk associations
     *//*

    public List<RPlayerPerk> grantPerks(
        final @NotNull RDQPlayer player,
        final @NotNull List<RPerk> perks,
        final boolean enabled
    ) {
        
        final List<RPlayerPerk> playerPerks = perks.stream()
                                                   .filter(perk -> ! playerOwns(
                                                       player,
                                                       perk
                                                   ))
                                                   .map(perk -> new RPlayerPerk(
                                                       player,
                                                       perk,
                                                       enabled
                                                   ))
                                                   .toList();
        for (
            RPlayerPerk playerPerk : playerPerks
        ) {
            this.RPlayerPerkRepository.create(playerPerk);
        }
        
        return playerPerks;
    }
    
    */
/**
     * Revokes multiple perks from a player.
     *
     * @param player the player to revoke perks from
     * @param perks  the perks to revoke
     *
     * @return the number of perks that were revoked
     *//*

    public int revokePerks(
        final @NotNull RDQPlayer player,
        final @NotNull List<RPerk> perks
    ) {
        
        final List<RPlayerPerk> playerPerks = perks.stream()
                                                   .map(perk -> getPlayerPerk(
                                                       player,
                                                       perk
                                                   ))
                                                   .filter(java.util.Objects::nonNull)
                                                   .toList();
        for (
            RPlayerPerk playerPerk : playerPerks
        ) {
            this.RPlayerPerkRepository.delete(playerPerk.getId());
        }
        
        return playerPerks.size();
    }
    
    */
/**
     * Async variant of {@link #grantPerks(rdqPlayer, List, boolean)}.
     *//*

    public CompletableFuture<List<RPlayerPerk>> grantPerksAsync(
        final @NotNull RDQPlayer player,
        final @NotNull List<RPerk> perks,
        final boolean enabled
    ) {
        
        return CompletableFuture.supplyAsync(
            () -> grantPerks(
                player,
                perks,
                enabled
            ),
            rdq.getExecutor()
        );
    }
    
    */
/**
     * Async variant of {@link #revokePerks(rdqPlayer, List)}.
     *//*

    public CompletableFuture<Integer> revokePerksAsync(
        final @NotNull RDQPlayer player,
        final @NotNull List<RPerk> perks
    ) {
        
        return CompletableFuture.supplyAsync(
            () -> revokePerks(
                player,
                perks
            ),
            rdq.getExecutor()
        );
    }
    
    */
/**
     * Updates the last activated timestamp for a player perk.
     *
     * @param playerPerk the player perk to update
     *//*

    public void updateLastActivated(
        final @NotNull RPlayerPerk playerPerk
    ) {
        
        playerPerk.setLastActivated(LocalDateTime.now());
        this.RPlayerPerkRepository.update(playerPerk);
    }
    
    */
/**
     * Updates the last deactivated timestamp for a player perk.
     *
     * @param playerPerk the player perk to update
     *//*

    public void updateLastDeactivated(
        final @NotNull RPlayerPerk playerPerk
    ) {
        
        playerPerk.setLastDeactivated(LocalDateTime.now());
        this.RPlayerPerkRepository.update(playerPerk);
    }
    
    */
/**
     * Increments the activation count for a player perk.
     *
     * @param playerPerk the player perk to update
     *//*

    public void incrementActivationCount(
        final @NotNull RPlayerPerk playerPerk
    ) {
        
        playerPerk.incrementActivationCount();
        this.RPlayerPerkRepository.update(playerPerk);
    }
    
    */
/**
     * Adds usage time to a player perk's total.
     *
     * @param playerPerk      the player perk to update
     * @param usageTimeMillis the usage time to add in milliseconds
     *//*

    public void addUsageTime(
        final @NotNull RPlayerPerk playerPerk,
        final long usageTimeMillis
    ) {
        
        playerPerk.addUsageTime(usageTimeMillis);
        this.RPlayerPerkRepository.update(playerPerk);
    }
    
    */
/**
     * Cleans up all perk data for a player (useful when player leaves server).
     *
     * @param playerUniqueId the player UUID to clean up
     *//*

    public void cleanupPlayerData(
        final @NotNull UUID playerUniqueId
    ) {
        
        final List<RPlayerPerk> playerPerks = this.RPlayerPerkRepository.findListByAttributes(Map.of(
            "player.uniqueId",
            playerUniqueId
        ));
        for (
            final RPlayerPerk playerPerk : playerPerks
        ) {
            if (
                playerPerk.isActive()
            ) {
                playerPerk.setActive(false);
            }
            this.RPlayerPerkRepository.update(playerPerk);
        }
    }
    
}*/
