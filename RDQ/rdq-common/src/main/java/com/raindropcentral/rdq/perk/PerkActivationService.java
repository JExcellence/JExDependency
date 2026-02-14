package com.raindropcentral.rdq.perk;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.perk.Perk;
import com.raindropcentral.rdq.database.entity.perk.PerkType;
import com.raindropcentral.rdq.database.entity.perk.PlayerPerk;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.database.repository.PlayerPerkRepository;
import com.raindropcentral.rdq.perk.handler.EventPerkHandler;
import com.raindropcentral.rdq.perk.handler.PotionPerkHandler;
import com.raindropcentral.rdq.perk.handler.SpecialPerkHandler;
import com.raindropcentral.rplatform.logging.CentralLogger;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service for managing perk activation, deactivation, and lifecycle.
 * <p>
 * This service handles:
 * - Activating and deactivating perk effects
 * - Managing cooldowns
 * - Handling player login/logout lifecycle
 * - Processing game events for event-triggered perks
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 */
public class PerkActivationService {
    
    private static final Logger LOGGER = CentralLogger.getLogger(PerkActivationService.class);
    private static final Gson GSON = new Gson();
    
    // Cooldown cleanup interval in ticks (60 seconds)
    private static final long COOLDOWN_CLEANUP_INTERVAL_TICKS = 1200L;
    
    private final RDQ plugin;
    private final PlayerPerkRepository playerPerkRepository;
    private final PerkManagementService perkManagementService;
    private final double cooldownMultiplier;
    
    // Effect handlers
    private final PotionPerkHandler potionPerkHandler;
    private final SpecialPerkHandler specialPerkHandler;
    private final EventPerkHandler eventPerkHandler;
    
    // Cache for active perks by player UUID
    private final Map<java.util.UUID, List<PlayerPerk>> activePerksCache = new ConcurrentHashMap<>();
    
    // Scheduled task for cleaning up expired cooldowns
    private org.bukkit.scheduler.BukkitTask cooldownCleanupTask;
    
    /**
     * Constructs a new PerkActivationService.
     *
     * @param plugin the RDQ plugin instance
     * @param playerPerkRepository the player perk repository
     * @param perkManagementService the perk management service
     * @param cooldownMultiplier the global cooldown multiplier
     */
    public PerkActivationService(
            @NotNull final RDQ plugin,
            @NotNull final PlayerPerkRepository playerPerkRepository,
            @NotNull final PerkManagementService perkManagementService,
            final double cooldownMultiplier
    ) {
        this.plugin = plugin;
        this.playerPerkRepository = playerPerkRepository;
        this.perkManagementService = perkManagementService;
        this.cooldownMultiplier = cooldownMultiplier;
        
        // Initialize effect handlers
        this.potionPerkHandler = new PotionPerkHandler(plugin);
        this.specialPerkHandler = new SpecialPerkHandler();
        this.eventPerkHandler = new EventPerkHandler();
    }
    
    // ==================== Activation/Deactivation Methods ====================
    
    /**
     * Activates a perk for a player, applying its effects.
     * Updates the PlayerPerk active state and delegates to appropriate effect handlers.
     *
     * @param player the Bukkit player
     * @param playerPerk the player perk to activate
     * @return a CompletableFuture containing true if activated successfully, false otherwise
     */
    public CompletableFuture<Boolean> activate(
            @NotNull final Player player,
            @NotNull final PlayerPerk playerPerk
    ) {
        // Validate perk is enabled
        if (!playerPerk.isEnabled()) {
            LOGGER.log(Level.WARNING, "Cannot activate perk {0} for player {1}: perk not enabled",
                    new Object[]{playerPerk.getPerk().getIdentifier(), player.getName()});
            return CompletableFuture.completedFuture(false);
        }
        
        // Check if already active
        if (playerPerk.isActive()) {
            LOGGER.log(Level.FINE, "Perk {0} already active for player {1}",
                    new Object[]{playerPerk.getPerk().getIdentifier(), player.getName()});
            return CompletableFuture.completedFuture(true);
        }
        
        // Check cooldown
        if (playerPerk.isOnCooldown()) {
            LOGGER.log(Level.FINE, "Cannot activate perk {0} for player {1}: on cooldown",
                    new Object[]{playerPerk.getPerk().getIdentifier(), player.getName()});
            return CompletableFuture.completedFuture(false);
        }
        
        Perk perk = playerPerk.getPerk();
        
        try {
            // Apply perk effects based on type
            boolean effectsApplied = applyPerkEffects(player, playerPerk);
            
            if (!effectsApplied) {
                LOGGER.log(Level.WARNING, "Failed to apply effects for perk {0} to player {1}",
                        new Object[]{perk.getIdentifier(), player.getName()});
                return CompletableFuture.completedFuture(false);
            }
            
            // Update PlayerPerk state
            playerPerk.recordActivation();
            
            // Save to database
            return CompletableFuture.supplyAsync(() -> {
                PlayerPerk saved = playerPerkRepository.save(playerPerk);
                LOGGER.log(Level.INFO, "Activated perk {0} for player {1}",
                        new Object[]{perk.getIdentifier(), player.getName()});
                
                // Update cache
                invalidateCache(player.getUniqueId());
                
                return true;
            });
                    
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error activating perk " + perk.getIdentifier() + 
                    " for player " + player.getName(), e);
            return CompletableFuture.completedFuture(false);
        }
    }
    
    /**
     * Deactivates a perk for a player, removing its effects.
     * Updates the PlayerPerk active state and delegates to appropriate effect handlers.
     *
     * @param player the Bukkit player
     * @param playerPerk the player perk to deactivate
     * @return a CompletableFuture containing true if deactivated successfully, false otherwise
     */
    public CompletableFuture<Boolean> deactivate(
            @NotNull final Player player,
            @NotNull final PlayerPerk playerPerk
    ) {
        // Check if already inactive
        if (!playerPerk.isActive()) {
            LOGGER.log(Level.FINE, "Perk {0} already inactive for player {1}",
                    new Object[]{playerPerk.getPerk().getIdentifier(), player.getName()});
            return CompletableFuture.completedFuture(true);
        }
        
        Perk perk = playerPerk.getPerk();
        
        try {
            // Remove perk effects based on type
            boolean effectsRemoved = removePerkEffects(player, playerPerk);
            
            if (!effectsRemoved) {
                LOGGER.log(Level.WARNING, "Failed to remove effects for perk {0} from player {1}",
                        new Object[]{perk.getIdentifier(), player.getName()});
                // Continue anyway to update state
            }
            
            // Update PlayerPerk state
            playerPerk.recordDeactivation();
            
            // Save to database
            return CompletableFuture.supplyAsync(() -> {
                PlayerPerk saved = playerPerkRepository.save(playerPerk);
                LOGGER.log(Level.INFO, "Deactivated perk {0} for player {1}",
                        new Object[]{perk.getIdentifier(), player.getName()});
                
                // Update cache
                invalidateCache(player.getUniqueId());
                
                return true;
            });
                    
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error deactivating perk " + perk.getIdentifier() + 
                    " for player " + player.getName(), e);
            return CompletableFuture.completedFuture(false);
        }
    }
    
    /**
     * Applies perk effects to a player based on the perk type.
     * Delegates to appropriate effect handlers.
     *
     * @param player the player
     * @param playerPerk the player perk
     * @return true if effects were applied successfully
     */
    private boolean applyPerkEffects(
            @NotNull final Player player,
            @NotNull final PlayerPerk playerPerk
    ) {
        Perk perk = playerPerk.getPerk();
        PerkType perkType = perk.getPerkType();
        
        // Parse config JSON
        JsonObject config = parseConfig(perk.getConfigJson());
        
        // Delegate to appropriate handler based on perk type and configuration
        switch (perkType) {
            case PASSIVE:
                // Check what type of passive perk this is
                if (config.has("potionEffectType")) {
                    return potionPerkHandler.applyPotionEffect(player, playerPerk);
                } else if (config.has("specialType")) {
                    return applySpecialEffect(player, playerPerk, config);
                }
                LOGGER.log(Level.WARNING, "Passive perk {0} has no recognized effect type", 
                        perk.getIdentifier());
                return false;
                
            case EVENT_TRIGGERED:
            case PERCENTAGE_BASED:
                // Register with event handler
                return eventPerkHandler.registerEventPerk(playerPerk);
                
            case COOLDOWN_BASED:
                // Manual activation perks - effects applied when triggered
                LOGGER.log(Level.FINE, "Cooldown-based perk {0} ready for manual activation", 
                        perk.getIdentifier());
                return true;
                
            default:
                LOGGER.log(Level.WARNING, "Unknown perk type: {0}", perkType);
                return false;
        }
    }
    
    /**
     * Applies special effect based on configuration.
     *
     * @param player the player
     * @param playerPerk the player perk
     * @param config the perk configuration
     * @return true if effect was applied successfully
     */
    private boolean applySpecialEffect(
            @NotNull final Player player,
            @NotNull final PlayerPerk playerPerk,
            @NotNull final JsonObject config
    ) {
        String specialType = config.get("specialType").getAsString().toUpperCase();
        
        switch (specialType) {
            case "FLY":
                specialPerkHandler.enableFly(player);
                return true;
            case "GLOW":
                specialPerkHandler.enableGlow(player);
                return true;
            case "NO_FALL_DAMAGE":
                specialPerkHandler.registerNoFallDamage(player);
                return true;
            case "KEEP_INVENTORY":
                specialPerkHandler.registerKeepInventory(player);
                return true;
            case "KEEP_EXPERIENCE":
                specialPerkHandler.registerKeepExperience(player);
                return true;
            default:
                LOGGER.log(Level.WARNING, "Unknown special type: {0}", specialType);
                return false;
        }
    }
    
    /**
     * Removes perk effects from a player based on the perk type.
     * Delegates to appropriate effect handlers.
     *
     * @param player the player
     * @param playerPerk the player perk
     * @return true if effects were removed successfully
     */
    private boolean removePerkEffects(
            @NotNull final Player player,
            @NotNull final PlayerPerk playerPerk
    ) {
        Perk perk = playerPerk.getPerk();
        PerkType perkType = perk.getPerkType();
        
        // Parse config JSON
        JsonObject config = parseConfig(perk.getConfigJson());
        
        // Delegate to appropriate handler based on perk type and configuration
        switch (perkType) {
            case PASSIVE:
                // Check what type of passive perk this is
                if (config.has("potionEffectType")) {
                    return potionPerkHandler.removePotionEffect(player, playerPerk);
                } else if (config.has("specialType")) {
                    return removeSpecialEffect(player, playerPerk, config);
                }
                LOGGER.log(Level.WARNING, "Passive perk {0} has no recognized effect type", 
                        perk.getIdentifier());
                return false;
                
            case EVENT_TRIGGERED:
            case PERCENTAGE_BASED:
                // Unregister from event handler
                return eventPerkHandler.unregisterEventPerk(playerPerk);
                
            case COOLDOWN_BASED:
                // Manual activation perks - no persistent effects to remove
                LOGGER.log(Level.FINE, "Cooldown-based perk {0} deactivated", perk.getIdentifier());
                return true;
                
            default:
                LOGGER.log(Level.WARNING, "Unknown perk type: {0}", perkType);
                return false;
        }
    }
    
    /**
     * Removes special effect based on configuration.
     *
     * @param player the player
     * @param playerPerk the player perk
     * @param config the perk configuration
     * @return true if effect was removed successfully
     */
    private boolean removeSpecialEffect(
            @NotNull final Player player,
            @NotNull final PlayerPerk playerPerk,
            @NotNull final JsonObject config
    ) {
        String specialType = config.get("specialType").getAsString().toUpperCase();
        
        switch (specialType) {
            case "FLY":
                specialPerkHandler.disableFly(player);
                return true;
            case "GLOW":
                specialPerkHandler.disableGlow(player);
                return true;
            case "NO_FALL_DAMAGE":
                specialPerkHandler.unregisterNoFallDamage(player);
                return true;
            case "KEEP_INVENTORY":
                specialPerkHandler.unregisterKeepInventory(player);
                return true;
            case "KEEP_EXPERIENCE":
                specialPerkHandler.unregisterKeepExperience(player);
                return true;
            default:
                LOGGER.log(Level.WARNING, "Unknown special type: {0}", specialType);
                return false;
        }
    }
    
    /**
     * Parses the config JSON string into a JsonObject.
     *
     * @param configJson the JSON string
     * @return the parsed JsonObject, or an empty object if parsing fails
     */
    private JsonObject parseConfig(String configJson) {
        if (configJson == null || configJson.isEmpty()) {
            return new JsonObject();
        }
        
        try {
            return GSON.fromJson(configJson, JsonObject.class);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to parse perk config JSON", e);
            return new JsonObject();
        }
    }

    
    // ==================== Cooldown Management Methods ====================
    
    /**
     * Checks if a perk is currently on cooldown.
     *
     * @param playerPerk the player perk to check
     * @return true if on cooldown, false otherwise
     */
    public boolean isOnCooldown(@NotNull final PlayerPerk playerPerk) {
        return playerPerk.isOnCooldown();
    }
    
    /**
     * Gets the remaining cooldown time in milliseconds.
     *
     * @param playerPerk the player perk
     * @return remaining cooldown in milliseconds, or 0 if not on cooldown
     */
    public long getRemainingCooldown(@NotNull final PlayerPerk playerPerk) {
        return playerPerk.getRemainingCooldownMillis();
    }
    
    /**
     * Starts a cooldown for a perk.
     * Applies the global cooldown multiplier to the duration.
     *
     * @param playerPerk the player perk
     * @param durationMillis the base cooldown duration in milliseconds
     * @return a CompletableFuture that completes when the cooldown is saved
     */
    public CompletableFuture<Void> startCooldown(
            @NotNull final PlayerPerk playerPerk,
            final long durationMillis
    ) {
        // Apply cooldown multiplier
        long adjustedDuration = (long) (durationMillis * cooldownMultiplier);
        
        // Set cooldown expiration
        playerPerk.startCooldown(adjustedDuration);
        
        LOGGER.log(Level.FINE, "Started cooldown for perk {0}: {1}ms (adjusted from {2}ms)",
                new Object[]{playerPerk.getPerk().getIdentifier(), adjustedDuration, durationMillis});
        
        // Save to database
        return CompletableFuture.runAsync(() -> {
            playerPerkRepository.save(playerPerk);
            LOGGER.log(Level.FINE, "Cooldown saved for perk {0}",
                    playerPerk.getPerk().getIdentifier());
        }).exceptionally(throwable -> {
            LOGGER.log(Level.SEVERE, "Failed to save cooldown for perk " + 
                    playerPerk.getPerk().getIdentifier(), throwable);
            return null;
        });
    }
    
    /**
     * Clears the cooldown for a perk.
     *
     * @param playerPerk the player perk
     * @return a CompletableFuture that completes when the cooldown is cleared
     */
    public CompletableFuture<Void> clearCooldown(@NotNull final PlayerPerk playerPerk) {
        playerPerk.clearCooldown();
        
        LOGGER.log(Level.FINE, "Cleared cooldown for perk {0}",
                playerPerk.getPerk().getIdentifier());
        
        // Save to database
        return CompletableFuture.runAsync(() -> {
            playerPerkRepository.save(playerPerk);
            LOGGER.log(Level.FINE, "Cooldown cleared and saved for perk {0}",
                    playerPerk.getPerk().getIdentifier());
        }).exceptionally(throwable -> {
            LOGGER.log(Level.SEVERE, "Failed to clear cooldown for perk " + 
                    playerPerk.getPerk().getIdentifier(), throwable);
            return null;
        });
    }

    
    // ==================== Lifecycle Methods ====================
    
    /**
     * Activates all enabled perks for a player when they log in.
     * This method should be called from the PlayerJoinEvent listener.
     *
     * @param player the Bukkit player
     * @param rdqPlayer the RDQ player entity
     * @return a CompletableFuture that completes when all perks are activated
     */
    public CompletableFuture<Void> activateAllEnabledPerks(
            @NotNull final Player player,
            @NotNull final RDQPlayer rdqPlayer
    ) {
        LOGGER.log(Level.INFO, "Activating all enabled perks for player {0}", player.getName());
        
        return perkManagementService.getEnabledPerksAsync(rdqPlayer)
                .thenCompose(enabledPerks -> {
                    if (enabledPerks.isEmpty()) {
                        LOGGER.log(Level.FINE, "No enabled perks found for player {0}", player.getName());
                        return CompletableFuture.completedFuture(null);
                    }
                    
                    // Activate each enabled perk
                    List<CompletableFuture<Boolean>> activationFutures = enabledPerks.stream()
                            .map(playerPerk -> activate(player, playerPerk))
                            .toList();
                    
                    // Wait for all activations to complete
                    return CompletableFuture.allOf(activationFutures.toArray(new CompletableFuture[0]))
                            .thenAccept(v -> {
                                long successCount = activationFutures.stream()
                                        .map(CompletableFuture::join)
                                        .filter(success -> success)
                                        .count();
                                
                                LOGGER.log(Level.INFO, "Activated {0}/{1} enabled perks for player {2}",
                                        new Object[]{successCount, enabledPerks.size(), player.getName()});
                            });
                })
                .exceptionally(throwable -> {
                    LOGGER.log(Level.SEVERE, "Failed to activate enabled perks for player " + 
                            player.getName(), throwable);
                    return null;
                });
    }
    
    /**
     * Deactivates all active perks for a player when they log out.
     * This method should be called from the PlayerQuitEvent listener.
     *
     * @param player the Bukkit player
     * @param rdqPlayer the RDQ player entity
     * @return a CompletableFuture that completes when all perks are deactivated
     */
    public CompletableFuture<Void> deactivateAllActivePerks(
            @NotNull final Player player,
            @NotNull final RDQPlayer rdqPlayer
    ) {
        LOGGER.log(Level.INFO, "Deactivating all active perks for player {0}", player.getName());
        
        return perkManagementService.getActivePerksAsync(rdqPlayer)
                .thenCompose(activePerks -> {
                    if (activePerks.isEmpty()) {
                        LOGGER.log(Level.FINE, "No active perks found for player {0}", player.getName());
                        return CompletableFuture.completedFuture(null);
                    }
                    
                    // Deactivate each active perk
                    List<CompletableFuture<Boolean>> deactivationFutures = activePerks.stream()
                            .map(playerPerk -> deactivate(player, playerPerk))
                            .toList();
                    
                    // Wait for all deactivations to complete
                    return CompletableFuture.allOf(deactivationFutures.toArray(new CompletableFuture[0]))
                            .thenAccept(v -> {
                                long successCount = deactivationFutures.stream()
                                        .map(CompletableFuture::join)
                                        .filter(success -> success)
                                        .count();
                                
                                LOGGER.log(Level.INFO, "Deactivated {0}/{1} active perks for player {2}",
                                        new Object[]{successCount, activePerks.size(), player.getName()});
                                
                                // Clear cache
                                invalidateCache(player.getUniqueId());
                            });
                })
                .exceptionally(throwable -> {
                    LOGGER.log(Level.SEVERE, "Failed to deactivate active perks for player " + 
                            player.getName(), throwable);
                    
                    // Clear cache anyway
                    invalidateCache(player.getUniqueId());
                    
                    return null;
                });
    }
    
    /**
     * Handles server restart scenarios by ensuring all active perks are properly saved.
     * This method should be called during plugin shutdown.
     *
     * @return a CompletableFuture that completes when all perks are saved
     */
    public CompletableFuture<Void> handleServerShutdown() {
        LOGGER.log(Level.INFO, "Handling server shutdown for perk system");
        
        // Stop potion effect refresh task
        potionPerkHandler.stopRefreshTask();
        
        // Clear all caches
        activePerksCache.clear();
        
        LOGGER.log(Level.INFO, "Perk system shutdown complete");
        
        return CompletableFuture.completedFuture(null);
    }

    
    // ==================== Event Handling Methods ====================
    
    /**
     * Handles a game event and processes event-triggered perks.
     * Checks for event-triggered perks, applies cooldown and trigger chance logic.
     *
     * @param player the Bukkit player
     * @param rdqPlayer the RDQ player entity
     * @param eventType the type of event that occurred
     * @param args additional event arguments
     * @return a CompletableFuture that completes when event processing is done
     */
    public CompletableFuture<Void> handleEvent(
            @NotNull final Player player,
            @NotNull final RDQPlayer rdqPlayer,
            @NotNull final String eventType,
            @NotNull final Object... args
    ) {
        LOGGER.log(Level.FINE, "Handling event {0} for player {1}", 
                new Object[]{eventType, player.getName()});
        
        // Delegate to event handler
        eventPerkHandler.processEvent(player, eventType, args);
        
        return CompletableFuture.completedFuture(null);
    }
    
    // ==================== Utility Methods ====================
    
    /**
     * Invalidates the active perks cache for a player.
     *
     * @param playerUuid the player UUID
     */
    private void invalidateCache(@NotNull final java.util.UUID playerUuid) {
        activePerksCache.remove(playerUuid);
    }
    
    /**
     * Gets the cooldown multiplier.
     *
     * @return the cooldown multiplier
     */
    public double getCooldownMultiplier() {
        return cooldownMultiplier;
    }
    
    /**
     * Gets the potion perk handler.
     *
     * @return the potion perk handler
     */
    public PotionPerkHandler getPotionPerkHandler() {
        return potionPerkHandler;
    }
    
    /**
     * Gets the special perk handler.
     *
     * @return the special perk handler
     */
    public SpecialPerkHandler getSpecialPerkHandler() {
        return specialPerkHandler;
    }
    
    /**
     * Gets the event perk handler.
     *
     * @return the event perk handler
     */
    public EventPerkHandler getEventPerkHandler() {
        return eventPerkHandler;
    }
    
    /**
     * Starts the scheduled tasks for perk management.
     * This includes the potion effect refresh task and cooldown cleanup task.
     */
    public void startScheduledTasks() {
        potionPerkHandler.startRefreshTask();
        startCooldownCleanupTask();
        LOGGER.log(Level.INFO, "Started perk system scheduled tasks");
    }
    
    /**
     * Stops the scheduled tasks for perk management.
     */
    public void stopScheduledTasks() {
        potionPerkHandler.stopRefreshTask();
        stopCooldownCleanupTask();
        LOGGER.log(Level.INFO, "Stopped perk system scheduled tasks");
    }
    
    /**
     * Starts the cooldown cleanup task that periodically clears expired cooldowns.
     */
    private void startCooldownCleanupTask() {
        if (cooldownCleanupTask != null && !cooldownCleanupTask.isCancelled()) {
            LOGGER.log(Level.WARNING, "Cooldown cleanup task is already running");
            return;
        }
        
        cooldownCleanupTask = org.bukkit.Bukkit.getScheduler().runTaskTimerAsynchronously(
                plugin.getPlugin(),
                () -> {
                    try {
                        // Find all player perks with expired cooldowns
                        List<PlayerPerk> allPlayerPerks = playerPerkRepository.findAll();
                        int clearedCount = 0;
                        
                        for (PlayerPerk playerPerk : allPlayerPerks) {
                            // Check if cooldown has expired
                            if (playerPerk.getCooldownExpiresAt() != null && 
                                !playerPerk.isOnCooldown()) {
                                
                                // Clear the expired cooldown
                                playerPerk.clearCooldown();
                                playerPerkRepository.save(playerPerk);
                                clearedCount++;
                                
                                LOGGER.log(Level.FINE, "Cleared expired cooldown for perk {0}",
                                        playerPerk.getPerk().getIdentifier());
                            }
                        }
                        
                        if (clearedCount > 0) {
                            LOGGER.log(Level.INFO, "Cleaned up {0} expired cooldowns", clearedCount);
                        }
                        
                    } catch (Exception e) {
                        LOGGER.log(Level.SEVERE, "Error in cooldown cleanup task", e);
                    }
                },
                COOLDOWN_CLEANUP_INTERVAL_TICKS,
                COOLDOWN_CLEANUP_INTERVAL_TICKS
        );
        
        LOGGER.log(Level.INFO, "Started cooldown cleanup task (interval: {0} ticks)",
                COOLDOWN_CLEANUP_INTERVAL_TICKS);
    }
    
    /**
     * Stops the cooldown cleanup task.
     */
    private void stopCooldownCleanupTask() {
        if (cooldownCleanupTask != null && !cooldownCleanupTask.isCancelled()) {
            cooldownCleanupTask.cancel();
            cooldownCleanupTask = null;
            LOGGER.log(Level.INFO, "Stopped cooldown cleanup task");
        }
    }
}
