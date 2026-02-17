package com.raindropcentral.rdq.listener;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.perk.PerkActivationService;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Event listener for perk system lifecycle and trigger events.
 * <p>
 * This listener handles:
 * - Player join/quit events to activate/deactivate perks
 * - Game events that trigger event-based perks (death, damage, movement, etc.)
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 */
public class PerkEventListener implements Listener {
    
    private static final Logger LOGGER = CentralLogger.getLogger(PerkEventListener.class);
    
    private final RDQ rdq;
    private final PerkActivationService perkActivationService;
    
    /**
     * Constructs a new PerkEventListener.
     *
     * @param rdq the RDQ plugin instance
     */
    public PerkEventListener(
            @NotNull final RDQ rdq
    ) {
        this.rdq = rdq;
        this.perkActivationService = rdq.getPerkActivationService();
    }
    
    // ==================== Player Lifecycle Events ====================
    
    /**
     * Handles player join events to activate enabled perks.
     * Activates all perks that the player has enabled when they log in.
     *
     * @param event the player join event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(@NotNull final PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Check if perk system is initialized
        if (perkActivationService == null) {
            LOGGER.log(Level.WARNING, "Perk system not yet initialized, skipping perk activation for {0}", 
                    player.getName());
            return;
        }
        
        LOGGER.log(Level.FINE, "Player {0} joined, activating enabled perks", player.getName());
        
        // Execute asynchronously to avoid blocking the main thread
        rdq.getExecutor().submit(() -> {
            try {
                Optional<RDQPlayer> rdqPlayerOpt = findRDQPlayer(player);
                if (rdqPlayerOpt.isEmpty()) {
                    LOGGER.log(Level.WARNING, "RDQPlayer not found for {0}, cannot activate perks", 
                            player.getName());
                    return;
                }
                
                RDQPlayer rdqPlayer = rdqPlayerOpt.get();
                
                // Activate all enabled perks
                perkActivationService.activateAllEnabledPerks(player, rdqPlayer)
                        .thenAccept(v -> {
                            LOGGER.log(Level.INFO, "Successfully activated enabled perks for player {0}", 
                                    player.getName());
                        })
                        .exceptionally(throwable -> {
                            LOGGER.log(Level.SEVERE, "Failed to activate enabled perks for player " + 
                                    player.getName(), throwable);
                            return null;
                        });
                        
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error handling player join for perk system: " + 
                        player.getName(), e);
            }
        });
    }
    
    /**
     * Handles player quit events to deactivate active perks.
     * Deactivates all active perks and cleans up player data when they log out.
     *
     * @param event the player quit event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(@NotNull final PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        // Check if perk system is initialized
        if (perkActivationService == null) {
            return;
        }
        
        LOGGER.log(Level.FINE, "Player {0} quit, deactivating active perks", player.getName());
        
        // Execute asynchronously to avoid blocking the main thread
        rdq.getExecutor().submit(() -> {
            try {
                Optional<RDQPlayer> rdqPlayerOpt = findRDQPlayer(player);
                if (rdqPlayerOpt.isEmpty()) {
                    LOGGER.log(Level.WARNING, "RDQPlayer not found for {0}, cannot deactivate perks", 
                            player.getName());
                    
                    // Still clean up handler data
                    cleanupPlayerData(player);
                    return;
                }
                
                RDQPlayer rdqPlayer = rdqPlayerOpt.get();
                
                // Deactivate all active perks
                perkActivationService.deactivateAllActivePerks(player, rdqPlayer)
                        .thenAccept(v -> {
                            LOGGER.log(Level.INFO, "Successfully deactivated active perks for player {0}", 
                                    player.getName());
                            
                            // Clean up player data from handlers
                            cleanupPlayerData(player);
                        })
                        .exceptionally(throwable -> {
                            LOGGER.log(Level.SEVERE, "Failed to deactivate active perks for player " + 
                                    player.getName(), throwable);
                            
                            // Still clean up handler data
                            cleanupPlayerData(player);
                            return null;
                        });
                        
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error handling player quit for perk system: " + 
                        player.getName(), e);
                
                // Still clean up handler data
                cleanupPlayerData(player);
            }
        });
    }
    
    /**
     * Cleans up player data from all perk handlers.
     *
     * @param player the player to clean up
     */
    private void cleanupPlayerData(@NotNull final Player player) {
        try {
            // Clean up special perk handler data
            perkActivationService.getSpecialPerkHandler().cleanupPlayer(player);
            
            // Clean up event perk handler data
            perkActivationService.getEventPerkHandler().cleanupPlayer(player.getUniqueId());
            
            LOGGER.log(Level.FINE, "Cleaned up perk data for player {0}", player.getName());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error cleaning up perk data for player " + player.getName(), e);
        }
    }
    
    // ==================== Perk Trigger Events ====================
    
    /**
     * Handles player death events for death-related perks.
     * Triggers perks that activate on player death.
     *
     * @param event the player death event
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerDeath(@NotNull final PlayerDeathEvent event) {
        Player player = event.getEntity();
        
        // Check if perk system is initialized
        if (perkActivationService == null) {
            return;
        }
        
        LOGGER.log(Level.FINE, "Player {0} died, processing death-related perks", player.getName());
        
        // Execute asynchronously
        rdq.getExecutor().submit(() -> {
            try {
                Optional<RDQPlayer> rdqPlayerOpt = findRDQPlayer(player);
                if (rdqPlayerOpt.isEmpty()) {
                    return;
                }
                
                RDQPlayer rdqPlayer = rdqPlayerOpt.get();
                
                // Process death event for perks
                perkActivationService.handleEvent(player, rdqPlayer, "PLAYER_DEATH", event)
                        .exceptionally(throwable -> {
                            LOGGER.log(Level.SEVERE, "Failed to process death event for player " + 
                                    player.getName(), throwable);
                            return null;
                        });
                        
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error handling player death for perk system: " + 
                        player.getName(), e);
            }
        });
    }
    
    /**
     * Handles entity damage events for combat perks.
     * Triggers perks that activate when a player takes or deals damage.
     *
     * @param event the entity damage event
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDamage(@NotNull final EntityDamageEvent event) {
        // Only process player damage
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        
        // Check if perk system is initialized
        if (perkActivationService == null) {
            return;
        }
        
        LOGGER.log(Level.FINE, "Player {0} took damage, processing damage-related perks", player.getName());
        
        // Execute asynchronously
        rdq.getExecutor().submit(() -> {
            try {
                Optional<RDQPlayer> rdqPlayerOpt = findRDQPlayer(player);
                if (rdqPlayerOpt.isEmpty()) {
                    return;
                }
                
                RDQPlayer rdqPlayer = rdqPlayerOpt.get();
                
                // Process damage event for perks
                perkActivationService.handleEvent(player, rdqPlayer, "ENTITY_DAMAGE", event)
                        .exceptionally(throwable -> {
                            LOGGER.log(Level.SEVERE, "Failed to process damage event for player " + 
                                    player.getName(), throwable);
                            return null;
                        });
                        
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error handling entity damage for perk system: " + 
                        player.getName(), e);
            }
        });
    }
    
    /**
     * Handles entity damage by entity events for combat perks.
     * Triggers perks that activate when a player damages another entity.
     *
     * @param event the entity damage by entity event
     */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onEntityDamageByEntity(@NotNull final EntityDamageByEntityEvent event) {
        // Only process when a player is the damager
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        
        // Check if perk system is initialized
        if (perkActivationService == null) {
            return;
        }
        
        LOGGER.log(Level.FINE, "Player {0} dealt damage, processing combat perks", player.getName());
        
        // Execute asynchronously
        rdq.getExecutor().submit(() -> {
            try {
                Optional<RDQPlayer> rdqPlayerOpt = findRDQPlayer(player);
                if (rdqPlayerOpt.isEmpty()) {
                    return;
                }
                
                RDQPlayer rdqPlayer = rdqPlayerOpt.get();
                
                // Process damage by entity event for perks
                perkActivationService.handleEvent(player, rdqPlayer, "ENTITY_DAMAGE_BY_ENTITY", event)
                        .exceptionally(throwable -> {
                            LOGGER.log(Level.SEVERE, "Failed to process damage by entity event for player " + 
                                    player.getName(), throwable);
                            return null;
                        });
                        
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error handling entity damage by entity for perk system: " + 
                        player.getName(), e);
            }
        });
    }
    
    /**
     * Handles player move events for movement perks.
     * Triggers perks that activate when a player moves.
     * Note: This event fires very frequently, so processing is kept minimal.
     *
     * @param event the player move event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(@NotNull final PlayerMoveEvent event) {
        // Only process if the player actually moved (not just head rotation)
        if (event.getFrom().getBlockX() == event.getTo().getBlockX() &&
            event.getFrom().getBlockY() == event.getTo().getBlockY() &&
            event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }
        
        // Check if perk system is initialized
        if (perkActivationService == null) {
            return;
        }
        
        Player player = event.getPlayer();
        
        // Execute asynchronously (but don't log at FINE level to avoid spam)
        rdq.getExecutor().submit(() -> {
            try {
                Optional<RDQPlayer> rdqPlayerOpt = findRDQPlayer(player);
                if (rdqPlayerOpt.isEmpty()) {
                    return;
                }
                
                RDQPlayer rdqPlayer = rdqPlayerOpt.get();
                
                // Process move event for perks
                perkActivationService.handleEvent(player, rdqPlayer, "PLAYER_MOVE", event)
                        .exceptionally(throwable -> {
                            // Don't log at SEVERE level to avoid spam
                            LOGGER.log(Level.FINE, "Failed to process move event for player " + 
                                    player.getName(), throwable);
                            return null;
                        });
                        
            } catch (Exception e) {
                // Don't log at SEVERE level to avoid spam
                LOGGER.log(Level.FINE, "Error handling player move for perk system: " + 
                        player.getName(), e);
            }
        });
    }
    
    // ==================== Utility Methods ====================
    
    /**
     * Finds the RDQPlayer entity for a Bukkit player.
     *
     * @param player the Bukkit player
     * @return an Optional containing the RDQPlayer, or empty if not found
     */
    private Optional<RDQPlayer> findRDQPlayer(@NotNull final Player player) {
        try {
            // Use the cached repository's get method
            RDQPlayer rdqPlayer = rdq.getPlayerRepository().getCachedByKey().get(player.getUniqueId());
            return Optional.ofNullable(rdqPlayer);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to find RDQPlayer for " + player.getName(), e);
            return Optional.empty();
        }
    }
}
