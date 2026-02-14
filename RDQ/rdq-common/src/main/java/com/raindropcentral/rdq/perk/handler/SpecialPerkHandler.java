package com.raindropcentral.rdq.perk.handler;

import com.raindropcentral.rplatform.logging.CentralLogger;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handler for special ability perks.
 * <p>
 * This handler manages special abilities like flight, glow effect, fall damage immunity,
 * and death protection (keep inventory/experience). It also implements event listeners
 * for damage prevention and death protection.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 */
public class SpecialPerkHandler implements Listener {
    
    private static final Logger LOGGER = CentralLogger.getLogger(SpecialPerkHandler.class);
    
    // Track players with special abilities
    private final Set<UUID> flyPlayers = ConcurrentHashMap.newKeySet();
    private final Set<UUID> glowPlayers = ConcurrentHashMap.newKeySet();
    private final Set<UUID> noFallDamagePlayers = ConcurrentHashMap.newKeySet();
    private final Set<UUID> keepInventoryPlayers = ConcurrentHashMap.newKeySet();
    private final Set<UUID> keepExperiencePlayers = ConcurrentHashMap.newKeySet();
    
    /**
     * Constructs a new SpecialPerkHandler.
     */
    public SpecialPerkHandler() {
    }
    
    // ==================== Fly Methods ====================
    
    /**
     * Enables flight for a player.
     *
     * @param player the player to enable flight for
     */
    public void enableFly(@NotNull final Player player) {
        player.setAllowFlight(true);
        player.setFlying(true);
        flyPlayers.add(player.getUniqueId());
        
        LOGGER.log(Level.INFO, "Enabled flight for player {0}", player.getName());
    }
    
    /**
     * Disables flight for a player.
     *
     * @param player the player to disable flight for
     */
    public void disableFly(@NotNull final Player player) {
        // Only disable if we enabled it (don't interfere with creative mode, etc.)
        if (flyPlayers.remove(player.getUniqueId())) {
            player.setAllowFlight(false);
            player.setFlying(false);
            
            LOGGER.log(Level.INFO, "Disabled flight for player {0}", player.getName());
        }
    }
    
    // ==================== Glow Methods ====================
    
    /**
     * Enables the glow effect for a player.
     *
     * @param player the player to enable glow for
     */
    public void enableGlow(@NotNull final Player player) {
        player.setGlowing(true);
        glowPlayers.add(player.getUniqueId());
        
        LOGGER.log(Level.INFO, "Enabled glow effect for player {0}", player.getName());
    }
    
    /**
     * Disables the glow effect for a player.
     *
     * @param player the player to disable glow for
     */
    public void disableGlow(@NotNull final Player player) {
        // Only disable if we enabled it
        if (glowPlayers.remove(player.getUniqueId())) {
            player.setGlowing(false);
            
            LOGGER.log(Level.INFO, "Disabled glow effect for player {0}", player.getName());
        }
    }
    
    // ==================== No Fall Damage Methods ====================
    
    /**
     * Registers a player for fall damage immunity.
     *
     * @param player the player to register
     */
    public void registerNoFallDamage(@NotNull final Player player) {
        noFallDamagePlayers.add(player.getUniqueId());
        
        LOGGER.log(Level.INFO, "Registered no fall damage for player {0}", player.getName());
    }
    
    /**
     * Unregisters a player from fall damage immunity.
     *
     * @param player the player to unregister
     */
    public void unregisterNoFallDamage(@NotNull final Player player) {
        noFallDamagePlayers.remove(player.getUniqueId());
        
        LOGGER.log(Level.INFO, "Unregistered no fall damage for player {0}", player.getName());
    }
    
    /**
     * Event handler for fall damage cancellation.
     *
     * @param event the entity damage event
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(@NotNull final EntityDamageEvent event) {
        // Check if it's a player taking fall damage
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) {
            return;
        }
        
        // Check if player has no fall damage perk
        if (noFallDamagePlayers.contains(player.getUniqueId())) {
            event.setCancelled(true);
            
            LOGGER.log(Level.FINE, "Cancelled fall damage for player {0}", player.getName());
        }
    }
    
    // ==================== Keep Inventory Methods ====================
    
    /**
     * Registers a player for inventory protection on death.
     *
     * @param player the player to register
     */
    public void registerKeepInventory(@NotNull final Player player) {
        keepInventoryPlayers.add(player.getUniqueId());
        
        LOGGER.log(Level.INFO, "Registered keep inventory for player {0}", player.getName());
    }
    
    /**
     * Unregisters a player from inventory protection on death.
     *
     * @param player the player to unregister
     */
    public void unregisterKeepInventory(@NotNull final Player player) {
        keepInventoryPlayers.remove(player.getUniqueId());
        
        LOGGER.log(Level.INFO, "Unregistered keep inventory for player {0}", player.getName());
    }
    
    // ==================== Keep Experience Methods ====================
    
    /**
     * Registers a player for experience protection on death.
     *
     * @param player the player to register
     */
    public void registerKeepExperience(@NotNull final Player player) {
        keepExperiencePlayers.add(player.getUniqueId());
        
        LOGGER.log(Level.INFO, "Registered keep experience for player {0}", player.getName());
    }
    
    /**
     * Unregisters a player from experience protection on death.
     *
     * @param player the player to unregister
     */
    public void unregisterKeepExperience(@NotNull final Player player) {
        keepExperiencePlayers.remove(player.getUniqueId());
        
        LOGGER.log(Level.INFO, "Unregistered keep experience for player {0}", player.getName());
    }
    
    /**
     * Event handler for death event handling (keep inventory/experience).
     *
     * @param event the player death event
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerDeath(@NotNull final PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID playerUuid = player.getUniqueId();
        
        // Handle keep inventory
        if (keepInventoryPlayers.contains(playerUuid)) {
            event.setKeepInventory(true);
            event.getDrops().clear();
            
            LOGGER.log(Level.INFO, "Preserved inventory for player {0} on death", player.getName());
        }
        
        // Handle keep experience
        if (keepExperiencePlayers.contains(playerUuid)) {
            event.setKeepLevel(true);
            event.setDroppedExp(0);
            
            LOGGER.log(Level.INFO, "Preserved experience for player {0} on death", player.getName());
        }
    }
    
    // ==================== Cleanup Methods ====================
    
    /**
     * Cleans up tracking data for a player.
     * Should be called when a player logs out.
     *
     * @param player the player to clean up
     */
    public void cleanupPlayer(@NotNull final Player player) {
        UUID playerUuid = player.getUniqueId();
        
        // Disable any active effects
        disableFly(player);
        disableGlow(player);
        
        // Remove from tracking sets
        noFallDamagePlayers.remove(playerUuid);
        keepInventoryPlayers.remove(playerUuid);
        keepExperiencePlayers.remove(playerUuid);
        
        LOGGER.log(Level.FINE, "Cleaned up special perks for player {0}", player.getName());
    }
    
    /**
     * Checks if a player has flight enabled through perks.
     *
     * @param playerUuid the player UUID
     * @return true if flight is enabled, false otherwise
     */
    public boolean hasFlyEnabled(@NotNull final UUID playerUuid) {
        return flyPlayers.contains(playerUuid);
    }
    
    /**
     * Checks if a player has glow enabled through perks.
     *
     * @param playerUuid the player UUID
     * @return true if glow is enabled, false otherwise
     */
    public boolean hasGlowEnabled(@NotNull final UUID playerUuid) {
        return glowPlayers.contains(playerUuid);
    }
    
    /**
     * Checks if a player has no fall damage enabled through perks.
     *
     * @param playerUuid the player UUID
     * @return true if no fall damage is enabled, false otherwise
     */
    public boolean hasNoFallDamageEnabled(@NotNull final UUID playerUuid) {
        return noFallDamagePlayers.contains(playerUuid);
    }
    
    /**
     * Checks if a player has keep inventory enabled through perks.
     *
     * @param playerUuid the player UUID
     * @return true if keep inventory is enabled, false otherwise
     */
    public boolean hasKeepInventoryEnabled(@NotNull final UUID playerUuid) {
        return keepInventoryPlayers.contains(playerUuid);
    }
    
    /**
     * Checks if a player has keep experience enabled through perks.
     *
     * @param playerUuid the player UUID
     * @return true if keep experience is enabled, false otherwise
     */
    public boolean hasKeepExperienceEnabled(@NotNull final UUID playerUuid) {
        return keepExperiencePlayers.contains(playerUuid);
    }
}
