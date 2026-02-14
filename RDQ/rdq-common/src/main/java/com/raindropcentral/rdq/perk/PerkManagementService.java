package com.raindropcentral.rdq.perk;

import com.raindropcentral.rdq.database.entity.perk.Perk;
import com.raindropcentral.rdq.database.entity.perk.PerkCategory;
import com.raindropcentral.rdq.database.entity.perk.PlayerPerk;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.database.repository.PerkRepository;
import com.raindropcentral.rdq.database.repository.PlayerPerkRepository;
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
    
    private static final Logger LOGGER = CentralLogger.getLogger(PerkManagementService.class);
    
    private final PerkRepository perkRepository;
    private final PlayerPerkRepository playerPerkRepository;
    private final int maxEnabledPerksPerPlayer;
    
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
    
    // ==================== Perk Ownership Methods ====================
    
    /**
     * Grants a perk to a player, creating a PlayerPerk association.
     * If the player already has the perk, returns the existing association.
     *
     * @param player the player to grant the perk to
     * @param perk the perk to grant
     * @return a CompletableFuture containing the PlayerPerk association
     */
    public CompletableFuture<PlayerPerk> grantPerk(
            @NotNull final RDQPlayer player,
            @NotNull final Perk perk
    ) {
        return findByPlayerAndPerk(player, perk)
                .thenCompose(existingOpt -> {
                    if (existingOpt.isPresent()) {
                        PlayerPerk existing = existingOpt.get();
                        if (!existing.isUnlocked()) {
                            existing.setUnlocked(true);
                            PlayerPerk saved = playerPerkRepository.save(existing);
                            return CompletableFuture.completedFuture(saved);
                        }
                        LOGGER.log(Level.FINE, "Player {0} already has perk {1}", 
                                new Object[]{player.getUniqueId(), perk.getIdentifier()});
                        return CompletableFuture.completedFuture(existing);
                    }
                    
                    // Create new PlayerPerk association
                    PlayerPerk playerPerk = new PlayerPerk(player, perk);
                    playerPerk.setUnlocked(true);
                    
                    return CompletableFuture.supplyAsync(() -> {
                        PlayerPerk saved = playerPerkRepository.save(playerPerk);
                        LOGGER.log(Level.INFO, "Granted perk {0} to player {1}", 
                                new Object[]{perk.getIdentifier(), player.getUniqueId()});
                        return saved;
                    });
                })
                .exceptionally(throwable -> {
                    LOGGER.log(Level.SEVERE, "Failed to grant perk " + perk.getIdentifier() + 
                            " to player " + player.getUniqueId(), throwable);
                    return null;
                });
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
                                
                                playerPerk.setEnabled(true);
                                return CompletableFuture.supplyAsync(() -> {
                                    PlayerPerk saved = playerPerkRepository.save(playerPerk);
                                    LOGGER.log(Level.INFO, "Enabled perk {0} for player {1}", 
                                            new Object[]{perk.getIdentifier(), player.getUniqueId()});
                                    return true;
                                });
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
                    
                    playerPerk.setEnabled(false);
                    return CompletableFuture.supplyAsync(() -> {
                        PlayerPerk saved = playerPerkRepository.save(playerPerk);
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
     * Finds a PlayerPerk by player and perk using findAll and filtering.
     */
    private CompletableFuture<Optional<PlayerPerk>> findByPlayerAndPerk(
            @NotNull final RDQPlayer player,
            @NotNull final Perk perk
    ) {
        return CompletableFuture.supplyAsync(() -> {
            List<PlayerPerk> allPlayerPerks = playerPerkRepository.findAll();
            return allPlayerPerks.stream()
                    .filter(pp -> pp.getPlayer().getUniqueId().equals(player.getUniqueId()) && 
                                 pp.getPerk().getId().equals(perk.getId()))
                    .findFirst();
        });
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
     * Finds all unlocked PlayerPerks for a player.
     */
    private CompletableFuture<List<PlayerPerk>> findUnlockedByPlayer(@NotNull final RDQPlayer player) {
        return CompletableFuture.supplyAsync(() -> {
            List<PlayerPerk> allPlayerPerks = playerPerkRepository.findAll();
            return allPlayerPerks.stream()
                    .filter(pp -> pp.getPlayer().getUniqueId().equals(player.getUniqueId()) && pp.isUnlocked())
                    .collect(Collectors.toList());
        });
    }
    
    /**
     * Finds all enabled PlayerPerks for a player.
     */
    private CompletableFuture<List<PlayerPerk>> findEnabledByPlayer(@NotNull final RDQPlayer player) {
        return CompletableFuture.supplyAsync(() -> {
            List<PlayerPerk> allPlayerPerks = playerPerkRepository.findAll();
            return allPlayerPerks.stream()
                    .filter(pp -> pp.getPlayer().getUniqueId().equals(player.getUniqueId()) && pp.isEnabled())
                    .collect(Collectors.toList());
        });
    }
    
    /**
     * Finds all active PlayerPerks for a player.
     */
    private CompletableFuture<List<PlayerPerk>> findActiveByPlayer(@NotNull final RDQPlayer player) {
        return CompletableFuture.supplyAsync(() -> {
            List<PlayerPerk> allPlayerPerks = playerPerkRepository.findAll();
            return allPlayerPerks.stream()
                    .filter(pp -> pp.getPlayer().getUniqueId().equals(player.getUniqueId()) && pp.isActive())
                    .collect(Collectors.toList());
        });
    }
    
    /**
     * Finds all enabled Perks.
     */
    private CompletableFuture<List<Perk>> findAllEnabledPerks() {
        return CompletableFuture.supplyAsync(() -> {
            List<Perk> allPerks = perkRepository.findAll();
            return allPerks.stream()
                    .filter(Perk::isEnabled)
                    .collect(Collectors.toList());
        });
    }
    
    /**
     * Finds all enabled Perks by category.
     */
    private CompletableFuture<List<Perk>> findEnabledPerksByCategory(@NotNull final PerkCategory category) {
        return CompletableFuture.supplyAsync(() -> {
            List<Perk> allPerks = perkRepository.findAll();
            return allPerks.stream()
                    .filter(p -> p.isEnabled() && p.getCategory() == category)
                    .collect(Collectors.toList());
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
