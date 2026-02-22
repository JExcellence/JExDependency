package com.raindropcentral.rdq.perk;

import com.raindropcentral.rdq.database.entity.perk.Perk;
import com.raindropcentral.rdq.database.entity.perk.PerkCategory;
import com.raindropcentral.rdq.database.entity.perk.PlayerPerk;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.database.repository.PerkRepository;
import com.raindropcentral.rdq.database.repository.PlayerPerkRepository;
import com.raindropcentral.rdq.perk.util.RetryableOperation;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Service for managing perk ownership, enable/disable states, and queries.
 * <p>
 * This service handles:
 * - Granting and revoking perks to/from players
 * - Enabling and disabling perks with limit checking
 * - Querying player perks by various states
 * - Checking perk limits
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 */
public class PerkManagementService {
    
    private static final Logger LOGGER = CentralLogger.getLoggerByName("RDQ");
    
    private final PerkRepository perkRepository;
    private final PlayerPerkRepository playerPerkRepository;
    private final int maxEnabledPerksPerPlayer;
    private com.raindropcentral.rdq.perk.cache.PlayerPerkCache cache;
    
    /**
     * Constructs a new PerkManagementService.
     *
     * @param perkRepository the perk repository
     * @param playerPerkRepository the player perk repository
     * @param maxEnabledPerksPerPlayer the maximum number of perks a player can have enabled
     */
    public PerkManagementService(
            @NotNull final PerkRepository perkRepository,
            @NotNull final PlayerPerkRepository playerPerkRepository,
            final int maxEnabledPerksPerPlayer
    ) {
        this.perkRepository = perkRepository;
        this.playerPerkRepository = playerPerkRepository;
        this.maxEnabledPerksPerPlayer = maxEnabledPerksPerPlayer;
    }
    
    /**
     * Sets the player perk cache.
     * Called after initialization to inject the cache.
     *
     * @param cache the player perk cache
     */
    public void setCache(@NotNull final com.raindropcentral.rdq.perk.cache.PlayerPerkCache cache) {
        this.cache = cache;
        LOGGER.log(Level.INFO, "PlayerPerkCache injected into PerkManagementService");
    }
    
    // ==================== Perk Ownership Methods ====================
    
    /**
     * Grants a perk to a player, creating a PlayerPerk association.
     * If the player already has the perk, returns the existing association.
     *
     * @param player the player to grant the perk to
     * @param perk the perk to grant
     * @param autoEnable whether to automatically enable the perk upon granting
     * @return a CompletableFuture containing the PlayerPerk association
     */
    public CompletableFuture<PlayerPerk> grantPerk(
            @NotNull final RDQPlayer player,
            @NotNull final Perk perk,
            final boolean autoEnable
    ) {
        return findByPlayerAndPerk(player, perk)
                .thenCompose(existingOpt -> {
                    if (existingOpt.isPresent()) {
                        PlayerPerk existing = existingOpt.get();
                        if (!existing.isUnlocked()) {
                            existing.setUnlocked(true);
                            if (autoEnable) {
                                existing.setEnabled(true);
                            }
                            // Update in cache (marks as dirty)
                            if (cache != null && cache.isCacheLoaded(player.getUniqueId())) {
                                cache.updatePlayerPerk(player.getUniqueId(), existing);
                            } else {
                                // Fallback to direct DB update
                                return CompletableFuture.supplyAsync(() -> {
                                    playerPerkRepository.update(existing);
                                    return existing;
                                });
                            }
                            LOGGER.log(Level.INFO, "Granted perk {0} to player {1} (unlocked=true, enabled={2})", 
                                    new Object[]{perk.getIdentifier(), player.getUniqueId(), autoEnable});
                        }
                        return CompletableFuture.completedFuture(existing);
                    }
                    
                    // Create new PlayerPerk association
                    PlayerPerk playerPerk = new PlayerPerk(player, perk);
                    playerPerk.setUnlocked(true);
                    // Note: enabled state is NOT set in DB, only in cache
                    
                    return CompletableFuture.supplyAsync(() -> {
                        try {
                            // Save to DB immediately for new entities (only unlocked=true)
                            PlayerPerk saved = playerPerkRepository.save(playerPerk);
                            
                            // If autoEnable, set enabled state in cache only
                            if (autoEnable) {
                                saved.setEnabled(true);
                            }
                            
                            // Add to cache
                            if (cache != null && cache.isCacheLoaded(player.getUniqueId())) {
                                cache.updatePlayerPerk(player.getUniqueId(), saved);
                            }
                            LOGGER.log(Level.INFO, "Granted perk {0} to player {1} (unlocked=true, enabled={2})", 
                                    new Object[]{perk.getIdentifier(), player.getUniqueId(), autoEnable});
                            return saved;
                        } catch (org.hibernate.exception.ConstraintViolationException e) {
                            // Race condition: another thread already created this PlayerPerk
                            // Query again to get the existing record
                            LOGGER.log(Level.WARNING, "Constraint violation when granting perk {0} to player {1}, fetching existing record", 
                                    new Object[]{perk.getIdentifier(), player.getUniqueId()});
                            try {
                                Optional<PlayerPerk> existing = findByPlayerAndPerkFromDB(player, perk).join();
                                if (existing.isPresent()) {
                                    PlayerPerk existingPerk = existing.get();
                                    // Only update unlocked state in DB if needed
                                    if (!existingPerk.isUnlocked()) {
                                        existingPerk.setUnlocked(true);
                                        playerPerkRepository.update(existingPerk);
                                    }
                                    
                                    // If autoEnable, set enabled state in cache only
                                    if (autoEnable) {
                                        existingPerk.setEnabled(true);
                                    }
                                    
                                    // Add to cache
                                    if (cache != null && cache.isCacheLoaded(player.getUniqueId())) {
                                        cache.updatePlayerPerk(player.getUniqueId(), existingPerk);
                                    }
                                    return existingPerk;
                                }
                            } catch (Exception ex) {
                                LOGGER.log(Level.SEVERE, "Failed to fetch existing PlayerPerk after constraint violation", ex);
                            }
                            throw e;
                        } catch (Exception e) {
                            // Check if the cause is a ConstraintViolationException
                            Throwable cause = e.getCause();
                            if (cause instanceof org.hibernate.exception.ConstraintViolationException) {
                                LOGGER.log(Level.WARNING, "Constraint violation when granting perk {0} to player {1}, fetching existing record", 
                                        new Object[]{perk.getIdentifier(), player.getUniqueId()});
                                try {
                                    Optional<PlayerPerk> existing = findByPlayerAndPerkFromDB(player, perk).join();
                                    if (existing.isPresent()) {
                                        PlayerPerk existingPerk = existing.get();
                                        // Only update unlocked state in DB if needed
                                        if (!existingPerk.isUnlocked()) {
                                            existingPerk.setUnlocked(true);
                                            playerPerkRepository.update(existingPerk);
                                        }
                                        
                                        // If autoEnable, set enabled state in cache only
                                        if (autoEnable) {
                                            existingPerk.setEnabled(true);
                                        }
                                        
                                        // Add to cache
                                        if (cache != null && cache.isCacheLoaded(player.getUniqueId())) {
                                            cache.updatePlayerPerk(player.getUniqueId(), existingPerk);
                                        }
                                        return existingPerk;
                                    }
                                } catch (Exception ex) {
                                    LOGGER.log(Level.SEVERE, "Failed to fetch existing PlayerPerk after constraint violation", ex);
                                }
                            }
                            throw e;
                        }
                    });
                })
                .exceptionally(throwable -> {
                    LOGGER.log(Level.SEVERE, "Failed to grant perk " + perk.getIdentifier() + 
                            " to player " + player.getUniqueId(), throwable);
                    return null;
                });
    }
    
    /**
     * Grants a perk to a player, creating a PlayerPerk association.
     * If the player already has the perk, returns the existing association.
     * This overload defaults to NOT auto-enabling the perk for backward compatibility.
     *
     * @param player the player to grant the perk to
     * @param perk the perk to grant
     * @return a CompletableFuture containing the PlayerPerk association
     * @deprecated Use {@link #grantPerk(RDQPlayer, Perk, boolean)} instead to explicitly specify auto-enable behavior
     */
    @Deprecated
    public CompletableFuture<PlayerPerk> grantPerk(
            @NotNull final RDQPlayer player,
            @NotNull final Perk perk
    ) {
        return grantPerk(player, perk, false);
    }
    
    /**
     * Revokes a perk from a player, deleting the PlayerPerk association.
     *
     * @param player the player to revoke the perk from
     * @param perk the perk to revoke
     * @return a CompletableFuture containing true if revoked, false if not found
     */
    public CompletableFuture<Boolean> revokePerk(
            @NotNull final RDQPlayer player,
            @NotNull final Perk perk
    ) {
        return deleteByPlayerAndPerk(player, perk)
                .thenApply(deleted -> {
                    if (deleted) {
                        LOGGER.log(Level.INFO, "Revoked perk {0} from player {1}", 
                                new Object[]{perk.getIdentifier(), player.getUniqueId()});
                    } else {
                        LOGGER.log(Level.FINE, "Player {0} does not have perk {1} to revoke", 
                                new Object[]{player.getUniqueId(), perk.getIdentifier()});
                    }
                    return deleted;
                })
                .exceptionally(throwable -> {
                    LOGGER.log(Level.SEVERE, "Failed to revoke perk " + perk.getIdentifier() + 
                            " from player " + player.getUniqueId(), throwable);
                    return false;
                });
    }
    
    /**
     * Checks if a player has unlocked a specific perk.
     *
     * @param player the player to check
     * @param perk the perk to check
     * @return true if the player has unlocked the perk, false otherwise
     */
    public boolean hasUnlocked(
            @NotNull final RDQPlayer player,
            @NotNull final Perk perk
    ) {
        try {
            return findByPlayerAndPerk(player, perk)
                    .thenApply(opt -> opt.map(PlayerPerk::isUnlocked).orElse(false))
                    .join();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to check if player " + player.getUniqueId() + 
                    " has unlocked perk " + perk.getIdentifier(), e);
            return false;
        }
    }
    
    /**
     * Asynchronously checks if a player has unlocked a specific perk.
     *
     * @param player the player to check
     * @param perk the perk to check
     * @return a CompletableFuture containing true if unlocked, false otherwise
     */
    public CompletableFuture<Boolean> hasUnlockedAsync(
            @NotNull final RDQPlayer player,
            @NotNull final Perk perk
    ) {
        return findByPlayerAndPerk(player, perk)
                .thenApply(opt -> opt.map(PlayerPerk::isUnlocked).orElse(false))
                .exceptionally(throwable -> {
                    LOGGER.log(Level.SEVERE, "Failed to check if player " + player.getUniqueId() + 
                            " has unlocked perk " + perk.getIdentifier(), throwable);
                    return false;
                });
    }

    
    // ==================== Enable/Disable Methods ====================
    
    /**
     * Enables a perk for a player.
     * Checks if the player has reached the maximum enabled perk limit.
     *
     * @param player the player
     * @param perk the perk to enable
     * @return a CompletableFuture containing true if enabled, false if limit reached or error
     */
    public CompletableFuture<Boolean> enablePerk(
            @NotNull final RDQPlayer player,
            @NotNull final Perk perk
    ) {
        return findByPlayerAndPerk(player, perk)
                .thenCompose(playerPerkOpt -> {
                    if (playerPerkOpt.isEmpty()) {
                        LOGGER.log(Level.WARNING, "Cannot enable perk {0} for player {1}: perk not unlocked", 
                                new Object[]{perk.getIdentifier(), player.getUniqueId()});
                        return CompletableFuture.completedFuture(false);
                    }
                    
                    PlayerPerk playerPerk = playerPerkOpt.get();
                    
                    if (!playerPerk.isUnlocked()) {
                        LOGGER.log(Level.WARNING, "Cannot enable perk {0} for player {1}: perk not unlocked", 
                                new Object[]{perk.getIdentifier(), player.getUniqueId()});
                        return CompletableFuture.completedFuture(false);
                    }
                    
                    if (playerPerk.isEnabled()) {
                        LOGGER.log(Level.FINE, "Perk {0} already enabled for player {1}", 
                                new Object[]{perk.getIdentifier(), player.getUniqueId()});
                        return CompletableFuture.completedFuture(true);
                    }
                    
                    // Check if player can enable another perk
                    return canEnableAnotherPerkAsync(player)
                            .thenCompose(canEnable -> {
                                if (!canEnable) {
                                    LOGGER.log(Level.INFO, "Player {0} has reached maximum enabled perk limit ({1})", 
                                            new Object[]{player.getUniqueId(), maxEnabledPerksPerPlayer});
                                    return CompletableFuture.completedFuture(false);
                                }
                                
                                // Update the entity
                                playerPerk.setEnabled(true);
                                // Update in cache (marks as dirty)
                                if (cache != null && cache.isCacheLoaded(player.getUniqueId())) {
                                    cache.updatePlayerPerk(player.getUniqueId(), playerPerk);
                                } else {
                                    // Fallback to direct DB update
                                    playerPerkRepository.update(playerPerk);
                                }
                                LOGGER.log(Level.INFO, "Enabled perk {0} for player {1}", 
                                        new Object[]{perk.getIdentifier(), player.getUniqueId()});
                                return CompletableFuture.completedFuture(true);
                            });
                })
                .exceptionally(throwable -> {
                    LOGGER.log(Level.SEVERE, "Failed to enable perk " + perk.getIdentifier() + 
                            " for player " + player.getUniqueId(), throwable);
                    return false;
                });
    }
    
    /**
     * Disables a perk for a player.
     *
     * @param player the player
     * @param perk the perk to disable
     * @return a CompletableFuture containing true if disabled, false if error
     */
    public CompletableFuture<Boolean> disablePerk(
            @NotNull final RDQPlayer player,
            @NotNull final Perk perk
    ) {
        return findByPlayerAndPerk(player, perk)
                .thenCompose(playerPerkOpt -> {
                    if (playerPerkOpt.isEmpty()) {
                        LOGGER.log(Level.WARNING, "Cannot disable perk {0} for player {1}: perk not found", 
                                new Object[]{perk.getIdentifier(), player.getUniqueId()});
                        return CompletableFuture.completedFuture(false);
                    }
                    
                    PlayerPerk playerPerk = playerPerkOpt.get();
                    
                    if (!playerPerk.isEnabled()) {
                        LOGGER.log(Level.FINE, "Perk {0} already disabled for player {1}", 
                                new Object[]{perk.getIdentifier(), player.getUniqueId()});
                        return CompletableFuture.completedFuture(true);
                    }
                    
                    return CompletableFuture.supplyAsync(() -> {
                        playerPerk.setEnabled(false);
                        // Update in cache (marks as dirty)
                        if (cache != null && cache.isCacheLoaded(player.getUniqueId())) {
                            cache.updatePlayerPerk(player.getUniqueId(), playerPerk);
                        } else {
                            // Fallback to direct DB update
                            playerPerkRepository.update(playerPerk);
                        }
                        LOGGER.log(Level.INFO, "Disabled perk {0} for player {1}", 
                                new Object[]{perk.getIdentifier(), player.getUniqueId()});
                        return true;
                    });
                })
                .exceptionally(throwable -> {
                    LOGGER.log(Level.SEVERE, "Failed to disable perk " + perk.getIdentifier() + 
                            " for player " + player.getUniqueId(), throwable);
                    return false;
                });
    }
    
    /**
     * Toggles a perk's enabled state for a player.
     *
     * @param player the player
     * @param perk the perk to toggle
     * @return a CompletableFuture containing true if toggled successfully, false if error
     */
    public CompletableFuture<Boolean> togglePerk(
            @NotNull final RDQPlayer player,
            @NotNull final Perk perk
    ) {
        return findByPlayerAndPerk(player, perk)
                .thenCompose(playerPerkOpt -> {
                    if (playerPerkOpt.isEmpty()) {
                        LOGGER.log(Level.WARNING, "Cannot toggle perk {0} for player {1}: perk not found", 
                                new Object[]{perk.getIdentifier(), player.getUniqueId()});
                        return CompletableFuture.completedFuture(false);
                    }
                    
                    PlayerPerk playerPerk = playerPerkOpt.get();
                    
                    if (playerPerk.isEnabled()) {
                        return disablePerk(player, perk);
                    } else {
                        return enablePerk(player, perk);
                    }
                })
                .exceptionally(throwable -> {
                    LOGGER.log(Level.SEVERE, "Failed to toggle perk " + perk.getIdentifier() + 
                            " for player " + player.getUniqueId(), throwable);
                    return false;
                });
    }

    
    // ==================== Query Methods ====================
    
    /**
     * Gets all unlocked perks for a player.
     *
     * @param player the player
     * @return a list of unlocked PlayerPerk entities
     */
    public List<PlayerPerk> getUnlockedPerks(@NotNull final RDQPlayer player) {
        try {
            return findUnlockedByPlayer(player).join();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to get unlocked perks for player " + player.getUniqueId(), e);
            return List.of();
        }
    }
    
    /**
     * Asynchronously gets all unlocked perks for a player.
     *
     * @param player the player
     * @return a CompletableFuture containing a list of unlocked PlayerPerk entities
     */
    public CompletableFuture<List<PlayerPerk>> getUnlockedPerksAsync(@NotNull final RDQPlayer player) {
        return findUnlockedByPlayer(player)
                .exceptionally(throwable -> {
                    LOGGER.log(Level.SEVERE, "Failed to get unlocked perks for player " + player.getUniqueId(), throwable);
                    return List.of();
                });
    }
    
    /**
     * Gets all enabled perks for a player.
     *
     * @param player the player
     * @return a list of enabled PlayerPerk entities
     */
    public List<PlayerPerk> getEnabledPerks(@NotNull final RDQPlayer player) {
        try {
            return findEnabledByPlayer(player).join();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to get enabled perks for player " + player.getUniqueId(), e);
            return List.of();
        }
    }
    
    /**
     * Asynchronously gets all enabled perks for a player.
     *
     * @param player the player
     * @return a CompletableFuture containing a list of enabled PlayerPerk entities
     */
    public CompletableFuture<List<PlayerPerk>> getEnabledPerksAsync(@NotNull final RDQPlayer player) {
        return findEnabledByPlayer(player)
                .exceptionally(throwable -> {
                    LOGGER.log(Level.SEVERE, "Failed to get enabled perks for player " + player.getUniqueId(), throwable);
                    return List.of();
                });
    }
    
    /**
     * Gets all active perks for a player.
     *
     * @param player the player
     * @return a list of active PlayerPerk entities
     */
    public List<PlayerPerk> getActivePerks(@NotNull final RDQPlayer player) {
        try {
            return findActiveByPlayer(player).join();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to get active perks for player " + player.getUniqueId(), e);
            return List.of();
        }
    }
    
    /**
     * Asynchronously gets all active perks for a player.
     *
     * @param player the player
     * @return a CompletableFuture containing a list of active PlayerPerk entities
     */
    public CompletableFuture<List<PlayerPerk>> getActivePerksAsync(@NotNull final RDQPlayer player) {
        return findActiveByPlayer(player)
                .exceptionally(throwable -> {
                    LOGGER.log(Level.SEVERE, "Failed to get active perks for player " + player.getUniqueId(), throwable);
                    return List.of();
                });
    }
    
    /**
     * Gets all available perks, optionally filtered by category.
     *
     * @param category the category to filter by, or null for all categories
     * @return a list of available Perk entities
     */
    public List<Perk> getAvailablePerks(@Nullable final PerkCategory category) {
        try {
            if (category == null) {
                return findAllEnabledPerks().join();
            } else {
                return findEnabledPerksByCategory(category).join();
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to get available perks for category " + category, e);
            return List.of();
        }
    }
    
    /**
     * Asynchronously gets all available perks, optionally filtered by category.
     *
     * @param category the category to filter by, or null for all categories
     * @return a CompletableFuture containing a list of available Perk entities
     */
    public CompletableFuture<List<Perk>> getAvailablePerksAsync(@Nullable final PerkCategory category) {
        CompletableFuture<List<Perk>> future;
        if (category == null) {
            future = findAllEnabledPerks();
        } else {
            future = findEnabledPerksByCategory(category);
        }
        
        return future.exceptionally(throwable -> {
            LOGGER.log(Level.SEVERE, "Failed to get available perks for category " + category, throwable);
            return List.of();
        });
    }
    
    /**
     * Gets a PlayerPerk association for a player and perk.
     *
     * @param player the player
     * @param perk the perk
     * @return an Optional containing the PlayerPerk, or empty if not found
     */
    public Optional<PlayerPerk> getPlayerPerk(
            @NotNull final RDQPlayer player,
            @NotNull final Perk perk
    ) {
        try {
            return findByPlayerAndPerk(player, perk).join();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to get player perk for player " + player.getUniqueId() + 
                    " and perk " + perk.getIdentifier(), e);
            return Optional.empty();
        }
    }
    
    /**
     * Asynchronously gets a PlayerPerk association for a player and perk.
     *
     * @param player the player
     * @param perk the perk
     * @return a CompletableFuture containing an Optional with the PlayerPerk, or empty if not found
     */
    public CompletableFuture<Optional<PlayerPerk>> getPlayerPerkAsync(
            @NotNull final RDQPlayer player,
            @NotNull final Perk perk
    ) {
        return findByPlayerAndPerk(player, perk)
                .exceptionally(throwable -> {
                    LOGGER.log(Level.SEVERE, "Failed to get player perk for player " + player.getUniqueId() + 
                            " and perk " + perk.getIdentifier(), throwable);
                    return Optional.empty();
                });
    }

    
    // ==================== Limit Checking Methods ====================
    
    /**
     * Checks if a player can enable another perk without exceeding the limit.
     *
     * @param player the player to check
     * @return true if the player can enable another perk, false otherwise
     */
    public boolean canEnableAnotherPerk(@NotNull final RDQPlayer player) {
        try {
            return canEnableAnotherPerkAsync(player).join();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to check if player " + player.getUniqueId() + 
                    " can enable another perk", e);
            return false;
        }
    }
    
    /**
     * Asynchronously checks if a player can enable another perk without exceeding the limit.
     *
     * @param player the player to check
     * @return a CompletableFuture containing true if the player can enable another perk, false otherwise
     */
    public CompletableFuture<Boolean> canEnableAnotherPerkAsync(@NotNull final RDQPlayer player) {
        return getEnabledPerkCountAsync(player)
                .thenApply(count -> count < maxEnabledPerksPerPlayer)
                .exceptionally(throwable -> {
                    LOGGER.log(Level.SEVERE, "Failed to check if player " + player.getUniqueId() + 
                            " can enable another perk", throwable);
                    return false;
                });
    }
    
    /**
     * Gets the number of enabled perks for a player.
     *
     * @param player the player
     * @return the count of enabled perks
     */
    public int getEnabledPerkCount(@NotNull final RDQPlayer player) {
        try {
            return getEnabledPerkCountAsync(player).join();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to get enabled perk count for player " + player.getUniqueId(), e);
            return 0;
        }
    }
    
    /**
     * Asynchronously gets the number of enabled perks for a player.
     *
     * @param player the player
     * @return a CompletableFuture containing the count of enabled perks
     */
    public CompletableFuture<Integer> getEnabledPerkCountAsync(@NotNull final RDQPlayer player) {
        return findEnabledByPlayer(player)
                .thenApply(list -> list.size())
                .exceptionally(throwable -> {
                    LOGGER.log(Level.SEVERE, "Failed to get enabled perk count for player " + player.getUniqueId(), throwable);
                    return 0;
                });
    }
    
    /**
     * Gets the maximum number of perks a player can have enabled.
     *
     * @return the maximum enabled perk limit
     */
    public int getMaxEnabledPerks() {
        return maxEnabledPerksPerPlayer;
    }
    
    /**
     * Gets the number of remaining perk slots for a player.
     *
     * @param player the player
     * @return the number of perks the player can still enable
     */
    public int getRemainingPerkSlots(@NotNull final RDQPlayer player) {
        int enabledCount = getEnabledPerkCount(player);
        return Math.max(0, maxEnabledPerksPerPlayer - enabledCount);
    }
    
    /**
     * Asynchronously gets the number of remaining perk slots for a player.
     *
     * @param player the player
     * @return a CompletableFuture containing the number of perks the player can still enable
     */
    public CompletableFuture<Integer> getRemainingPerkSlotsAsync(@NotNull final RDQPlayer player) {
        return getEnabledPerkCountAsync(player)
                .thenApply(enabledCount -> Math.max(0, maxEnabledPerksPerPlayer - enabledCount))
                .exceptionally(throwable -> {
                    LOGGER.log(Level.SEVERE, "Failed to get remaining perk slots for player " + player.getUniqueId(), throwable);
                    return 0;
                });
    }
    
    // ==================== Helper Methods (Repository Replacements) ====================
    
    /**
     * Finds a PlayerPerk by player and perk using cache or DB fallback.
     */
    private CompletableFuture<Optional<PlayerPerk>> findByPlayerAndPerk(
            @NotNull final RDQPlayer player,
            @NotNull final Perk perk
    ) {
        java.util.UUID playerId = player.getUniqueId();
        
        // Check if cache is loaded
        if (cache != null && cache.isCacheLoaded(playerId)) {
            return CompletableFuture.completedFuture(
                cache.getPlayerPerk(playerId, perk.getId())
            );
        }
        
        // Fallback to DB query
        LOGGER.log(Level.FINE, "Cache not loaded for player {0}, using DB fallback", playerId);
        return findByPlayerAndPerkFromDB(player, perk);
    }
    
    /**
     * Finds a PlayerPerk by player and perk using direct DB query (fallback).
     */
    private CompletableFuture<Optional<PlayerPerk>> findByPlayerAndPerkFromDB(
            @NotNull final RDQPlayer player,
            @NotNull final Perk perk
    ) {
        return RetryableOperation.executeWithRetry(() -> {
            try {
                List<PlayerPerk> allPlayerPerks = playerPerkRepository.findAll();
                Long playerId = player.getId();
                Long perkId = perk.getId();
                
                // Filter carefully to avoid lazy initialization issues
                for (PlayerPerk pp : allPlayerPerks) {
                    try {
                        Long ppPlayerId = pp.getPlayer().getId();
                        Long ppPerkId = pp.getPerk().getId();
                        if (ppPlayerId != null && ppPlayerId.equals(playerId) && 
                            ppPerkId != null && ppPerkId.equals(perkId)) {
                            return Optional.of(pp);
                        }
                    } catch (Exception e) {
                        // Skip this perk if we can't access it
                        LOGGER.log(Level.FINEST, "Skipping perk due to lazy initialization", e);
                    }
                }
                return Optional.empty();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to find player perk for player " + player.getUniqueId() + 
                        " and perk " + perk.getIdentifier(), e);
                throw e;
            }
        }, "findByPlayerAndPerkFromDB for player " + player.getUniqueId());
    }
    
    /**
     * Deletes a PlayerPerk by player and perk.
     */
    private CompletableFuture<Boolean> deleteByPlayerAndPerk(
            @NotNull final RDQPlayer player,
            @NotNull final Perk perk
    ) {
        return findByPlayerAndPerk(player, perk)
                .thenCompose(opt -> opt.map(playerPerk -> CompletableFuture.supplyAsync(() -> {
                    playerPerkRepository.delete(playerPerk.getId());
                    return true;
                })).orElseGet(() -> CompletableFuture.completedFuture(false)));
    }
    
    /**
     * Finds all unlocked PlayerPerks for a player using cache or DB fallback.
     */
    private CompletableFuture<List<PlayerPerk>> findUnlockedByPlayer(@NotNull final RDQPlayer player) {
        java.util.UUID playerId = player.getUniqueId();
        
        // Check if cache is loaded
        if (cache != null && cache.isCacheLoaded(playerId)) {
            return CompletableFuture.completedFuture(
                cache.getPlayerPerks(playerId, PlayerPerk::isUnlocked)
            );
        }
        
        // Fallback to DB query with retry
        return RetryableOperation.executeWithRetry(() -> {
            try {
                List<PlayerPerk> allPlayerPerks = playerPerkRepository.findAll();
                Long playerDbId = player.getId();
                return allPlayerPerks.stream()
                        .filter(pp -> {
                            try {
                                Long ppPlayerId = pp.getPlayer().getId();
                                return ppPlayerId != null && ppPlayerId.equals(playerDbId) && pp.isUnlocked();
                            } catch (Exception e) {
                                LOGGER.log(Level.FINEST, "Skipping perk due to lazy initialization", e);
                                return false;
                            }
                        })
                        .collect(Collectors.toList());
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to find unlocked perks for player " + player.getUniqueId(), e);
                throw e;
            }
        }, "findUnlockedByPlayer for player " + player.getUniqueId());
    }
    
    /**
     * Finds all enabled PlayerPerks for a player using cache or DB fallback.
     */
    private CompletableFuture<List<PlayerPerk>> findEnabledByPlayer(@NotNull final RDQPlayer player) {
        java.util.UUID playerId = player.getUniqueId();
        
        // Check if cache is loaded
        if (cache != null && cache.isCacheLoaded(playerId)) {
            return CompletableFuture.completedFuture(
                cache.getPlayerPerks(playerId, PlayerPerk::isEnabled)
            );
        }
        
        // Fallback to DB query with retry
        return RetryableOperation.executeWithRetry(() -> {
            try {
                List<PlayerPerk> allPlayerPerks = playerPerkRepository.findAll();
                Long playerDbId = player.getId();
                return allPlayerPerks.stream()
                        .filter(pp -> {
                            try {
                                Long ppPlayerId = pp.getPlayer().getId();
                                return ppPlayerId != null && ppPlayerId.equals(playerDbId) && pp.isEnabled();
                            } catch (Exception e) {
                                LOGGER.log(Level.FINEST, "Skipping perk due to lazy initialization", e);
                                return false;
                            }
                        })
                        .collect(Collectors.toList());
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to find enabled perks for player " + player.getUniqueId(), e);
                throw e;
            }
        }, "findEnabledByPlayer for player " + player.getUniqueId());
    }
    
    /**
     * Finds all active PlayerPerks for a player using cache or DB fallback.
     */
    private CompletableFuture<List<PlayerPerk>> findActiveByPlayer(@NotNull final RDQPlayer player) {
        java.util.UUID playerId = player.getUniqueId();
        
        // Check if cache is loaded
        if (cache != null && cache.isCacheLoaded(playerId)) {
            return CompletableFuture.completedFuture(
                cache.getPlayerPerks(playerId, PlayerPerk::isActive)
            );
        }
        
        // Fallback to DB query with retry
        return RetryableOperation.executeWithRetry(() -> {
            try {
                List<PlayerPerk> allPlayerPerks = playerPerkRepository.findAll();
                Long playerDbId = player.getId();
                return allPlayerPerks.stream()
                        .filter(pp -> {
                            try {
                                Long ppPlayerId = pp.getPlayer().getId();
                                return ppPlayerId != null && ppPlayerId.equals(playerDbId) && pp.isActive();
                            } catch (Exception e) {
                                LOGGER.log(Level.FINEST, "Skipping perk due to lazy initialization", e);
                                return false;
                            }
                        })
                        .collect(Collectors.toList());
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to find active perks for player " + player.getUniqueId(), e);
                throw e;
            }
        }, "findActiveByPlayer for player " + player.getUniqueId());
    }
    
    /**
     * Finds all enabled Perks.
     */
    private CompletableFuture<List<Perk>> findAllEnabledPerks() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<Perk> allPerks = perkRepository.findAll();
                return allPerks.stream()
                        .filter(Perk::isEnabled)
                        .collect(Collectors.toList());
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to find all enabled perks", e);
                return List.of();
            }
        });
    }
    
    /**
     * Finds all enabled Perks by category.
     */
    private CompletableFuture<List<Perk>> findEnabledPerksByCategory(@NotNull final PerkCategory category) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<Perk> allPerks = perkRepository.findAll();
                return allPerks.stream()
                        .filter(p -> p.isEnabled() && p.getCategory() == category)
                        .collect(Collectors.toList());
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to find enabled perks for category " + category, e);
                return List.of();
            }
        });
    }
    
    /**
     * Counts enabled PlayerPerks for a player.
     */
    private CompletableFuture<Long> countEnabledByPlayer(@NotNull final RDQPlayer player) {
        return findEnabledByPlayer(player)
                .thenApply(list -> (long) list.size());
    }
}
