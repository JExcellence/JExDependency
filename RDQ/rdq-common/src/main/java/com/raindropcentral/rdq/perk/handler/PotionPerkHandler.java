package com.raindropcentral.rdq.perk.handler;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.perk.Perk;
import com.raindropcentral.rdq.database.entity.perk.PlayerPerk;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handler for potion effect perks.
 * <p>
 * This handler manages the application, removal, and continuous refresh of potion effects
 * for passive perks. It maintains a scheduled task that refreshes potion effects to ensure
 * they remain active while the perk is enabled.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 */
public class PotionPerkHandler {
    
    private static final Logger LOGGER = CentralLogger.getLoggerByName("RDQ");
    private static final Gson GSON = new Gson();
    
    // Refresh interval in ticks (10 seconds)
    private static final long REFRESH_INTERVAL_TICKS = 200L;
    
    private final RDQ plugin;
    
    // Track active potion perks by player UUID
    private final Map<UUID, Map<Long, PlayerPerk>> activePlayerPerks = new ConcurrentHashMap<>();
    
    // Scheduled task for refreshing potion effects
    private BukkitTask refreshTask;
    
    /**
     * Constructs a new PotionPerkHandler.
     *
     * @param plugin the RDQ plugin instance
     */
    public PotionPerkHandler(@NotNull final RDQ plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Applies a potion effect to a player based on the perk configuration.
     *
     * @param player the player to apply the effect to
     * @param playerPerk the player perk containing the effect configuration
     * @return true if the effect was applied successfully, false otherwise
     */
    public boolean applyPotionEffect(
            @NotNull final Player player,
            @NotNull final PlayerPerk playerPerk
    ) {
        Perk perk = playerPerk.getPerk();
        JsonObject config = parseConfig(perk.getConfigJson());
        
        // Extract potion effect configuration
        String potionEffectType = config.has("potionEffectType") ? 
                config.get("potionEffectType").getAsString() : null;
        
        if (potionEffectType == null || potionEffectType.isEmpty()) {
            LOGGER.log(Level.WARNING, "Perk " + perk.getIdentifier() + " has no potionEffectType configured");
            return false;
        }
        
        // Parse potion effect type using Registry API (modern approach)
        PotionEffectType effectType;
        try {
            // Convert to lowercase for namespaced key (e.g., "JUMP_BOOST" -> "jump_boost")
            String effectKey = potionEffectType.toLowerCase();
            effectType = org.bukkit.Registry.EFFECT.get(org.bukkit.NamespacedKey.minecraft(effectKey));
            
            if (effectType == null) {
                LOGGER.log(Level.WARNING, "Invalid potion effect type: " + potionEffectType + " for perk " + perk.getIdentifier());
                return false;
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to parse potion effect type: " + potionEffectType + " for perk " + perk.getIdentifier(), e);
            return false;
        }
        
        // Extract effect parameters
        int amplifier = config.has("amplifier") ? config.get("amplifier").getAsInt() : 0;
        int durationTicks = config.has("durationTicks") ? config.get("durationTicks").getAsInt() : 600;
        boolean ambient = config.has("ambient") && config.get("ambient").getAsBoolean();
        boolean particles = !config.has("particles") || config.get("particles").getAsBoolean();
        
        // Create potion effect
        PotionEffect effect = new PotionEffect(
                effectType,
                durationTicks,
                amplifier,
                ambient,
                particles,
                true // icon
        );
        
        // Apply potion effect on main thread (Bukkit requirement)
        Bukkit.getScheduler().runTask(plugin.getPlugin(), () -> {
            player.addPotionEffect(effect);
        });
        
        // Track this perk for refresh
        activePlayerPerks
                .computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>())
                .put(playerPerk.getId(), playerPerk);
        
        LOGGER.log(Level.INFO, "Applied potion effect " + effectType.getName() + " (amplifier " + amplifier + 
                ") to player " + player.getName() + " from perk " + perk.getIdentifier());
        
        return true;
    }
    
    /**
     * Removes a potion effect from a player.
     *
     * @param player the player to remove the effect from
     * @param playerPerk the player perk containing the effect configuration
     * @return true if the effect was removed successfully, false otherwise
     */
    public boolean removePotionEffect(
            @NotNull final Player player,
            @NotNull final PlayerPerk playerPerk
    ) {
        Perk perk = playerPerk.getPerk();
        JsonObject config = parseConfig(perk.getConfigJson());
        
        // Extract potion effect configuration
        String potionEffectType = config.has("potionEffectType") ? 
                config.get("potionEffectType").getAsString() : null;
        
        if (potionEffectType == null || potionEffectType.isEmpty()) {
            LOGGER.log(Level.WARNING, "Perk " + perk.getIdentifier() + " has no potionEffectType configured");
            return false;
        }
        
        // Parse potion effect type using Registry API (modern approach)
        PotionEffectType effectType;
        try {
            // Convert to lowercase for namespaced key (e.g., "JUMP_BOOST" -> "jump_boost")
            String effectKey = potionEffectType.toLowerCase();
            effectType = org.bukkit.Registry.EFFECT.get(org.bukkit.NamespacedKey.minecraft(effectKey));
            
            if (effectType == null) {
                LOGGER.log(Level.WARNING, "Invalid potion effect type: " + potionEffectType + " for perk " + perk.getIdentifier());
                return false;
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to parse potion effect type: " + potionEffectType + " for perk " + perk.getIdentifier(), e);
            return false;
        }
        
        // Remove the potion effect on main thread (Bukkit requirement)
        Bukkit.getScheduler().runTask(plugin.getPlugin(), () -> {
            player.removePotionEffect(effectType);
        });
        
        // Stop tracking this perk
        Map<Long, PlayerPerk> playerPerks = activePlayerPerks.get(player.getUniqueId());
        if (playerPerks != null) {
            playerPerks.remove(playerPerk.getId());
            if (playerPerks.isEmpty()) {
                activePlayerPerks.remove(player.getUniqueId());
            }
        }
        
        LOGGER.log(Level.INFO, "Removed potion effect " + effectType.getName() + " from player " + player.getName() + 
                " for perk " + perk.getIdentifier());
        
        return true;
    }
    
    /**
     * Refreshes a potion effect for a player.
     * This is called periodically to maintain continuous effects.
     *
     * @param player the player to refresh the effect for
     * @param playerPerk the player perk containing the effect configuration
     * @return true if the effect was refreshed successfully, false otherwise
     */
    public boolean refreshPotionEffect(
            @NotNull final Player player,
            @NotNull final PlayerPerk playerPerk
    ) {
        // Simply reapply the effect
        return applyPotionEffect(player, playerPerk);
    }
    
    /**
     * Starts the scheduled task for continuous effect refresh.
     * This task runs periodically to refresh all active potion effects.
     */
    public void startRefreshTask() {
        if (refreshTask != null && !refreshTask.isCancelled()) {
            LOGGER.log(Level.WARNING, "Refresh task is already running");
            return;
        }
        
        refreshTask = Bukkit.getScheduler().runTaskTimer(plugin.getPlugin(), () -> {
            // Iterate through all active player perks
            activePlayerPerks.forEach((playerUuid, perks) -> {
                Player player = Bukkit.getPlayer(playerUuid);
                
                // Skip if player is offline
                if (player == null || !player.isOnline()) {
                    return;
                }
                
                // Refresh each perk's potion effect
                perks.values().forEach(playerPerk -> {
                    try {
                        refreshPotionEffect(player, playerPerk);
                    } catch (Exception e) {
                        LOGGER.log(Level.SEVERE, "Error refreshing potion effect for perk " + 
                                playerPerk.getPerk().getIdentifier() + " for player " + player.getName(), e);
                    }
                });
            });
        }, REFRESH_INTERVAL_TICKS, REFRESH_INTERVAL_TICKS);
        
        LOGGER.log(Level.INFO, "Started potion effect refresh task (interval: {0} ticks)",
                REFRESH_INTERVAL_TICKS);
    }
    
    /**
     * Stops the scheduled task for continuous effect refresh.
     */
    public void stopRefreshTask() {
        if (refreshTask != null && !refreshTask.isCancelled()) {
            refreshTask.cancel();
            refreshTask = null;
            LOGGER.log(Level.INFO, "Stopped potion effect refresh task");
        }
    }
    
    /**
     * Cleans up tracking data for a player.
     * Should be called when a player logs out.
     *
     * @param playerUuid the player UUID
     */
    public void cleanupPlayer(@NotNull final UUID playerUuid) {
        activePlayerPerks.remove(playerUuid);
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
}
