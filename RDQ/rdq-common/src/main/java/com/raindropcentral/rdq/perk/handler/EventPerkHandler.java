package com.raindropcentral.rdq.perk.handler;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.raindropcentral.rdq.database.entity.perk.Perk;
import com.raindropcentral.rdq.database.entity.perk.PerkType;
import com.raindropcentral.rdq.database.entity.perk.PlayerPerk;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handler for event-triggered and percentage-based perks.
 * <p>
 * This handler manages the registration, processing, and triggering of perks that
 * activate in response to game events. It handles cooldown checking, trigger chance
 * calculation, and event processing.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 */
public class EventPerkHandler {
    
    private static final Logger LOGGER = CentralLogger.getLoggerByName("RDQ");
    private static final Gson GSON = new Gson();
    
    // Track registered event perks by player UUID and event type
    private final Map<UUID, Map<String, Set<PlayerPerk>>> registeredPerks = new ConcurrentHashMap<>();
    
    /**
     * Constructs a new EventPerkHandler.
     */
    public EventPerkHandler() {
    }
    
    /**
     * Registers an event-triggered perk for a player.
     * The perk will be checked when the configured event occurs.
     *
     * @param playerPerk the player perk to register
     * @return true if registered successfully, false otherwise
     */
    public boolean registerEventPerk(@NotNull final PlayerPerk playerPerk) {
        Perk perk = playerPerk.getPerk();
        JsonObject config = parseConfig(perk.getConfigJson());
        
        // Extract trigger event
        String triggerEvent = config.has("triggerEvent") ? 
                config.get("triggerEvent").getAsString() : null;
        
        if (triggerEvent == null || triggerEvent.isEmpty()) {
            LOGGER.log(Level.WARNING, "Event perk {0} has no triggerEvent configured",
                    perk.getIdentifier());
            return false;
        }
        
        UUID playerUuid = playerPerk.getPlayer().getUniqueId();
        
        // Register the perk for this event type
        registeredPerks
                .computeIfAbsent(playerUuid, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(triggerEvent.toUpperCase(), k -> ConcurrentHashMap.newKeySet())
                .add(playerPerk);
        
        LOGGER.log(Level.INFO, "Registered event perk {0} for player {1} on event {2}",
                new Object[]{perk.getIdentifier(), playerUuid, triggerEvent});
        
        return true;
    }
    
    /**
     * Unregisters an event-triggered perk for a player.
     *
     * @param playerPerk the player perk to unregister
     * @return true if unregistered successfully, false otherwise
     */
    public boolean unregisterEventPerk(@NotNull final PlayerPerk playerPerk) {
        Perk perk = playerPerk.getPerk();
        JsonObject config = parseConfig(perk.getConfigJson());
        
        // Extract trigger event
        String triggerEvent = config.has("triggerEvent") ? 
                config.get("triggerEvent").getAsString() : null;
        
        if (triggerEvent == null || triggerEvent.isEmpty()) {
            LOGGER.log(Level.WARNING, "Event perk {0} has no triggerEvent configured",
                    perk.getIdentifier());
            return false;
        }
        
        UUID playerUuid = playerPerk.getPlayer().getUniqueId();
        
        // Unregister the perk
        Map<String, Set<PlayerPerk>> playerEvents = registeredPerks.get(playerUuid);
        if (playerEvents != null) {
            Set<PlayerPerk> perks = playerEvents.get(triggerEvent.toUpperCase());
            if (perks != null) {
                perks.remove(playerPerk);
                
                // Clean up empty sets
                if (perks.isEmpty()) {
                    playerEvents.remove(triggerEvent.toUpperCase());
                }
            }
            
            // Clean up empty maps
            if (playerEvents.isEmpty()) {
                registeredPerks.remove(playerUuid);
            }
        }
        
        LOGGER.log(Level.INFO, "Unregistered event perk {0} for player {1} on event {2}",
                new Object[]{perk.getIdentifier(), playerUuid, triggerEvent});
        
        return true;
    }
    
    /**
     * Processes an event for a player, checking all registered perks for that event type.
     *
     * @param player the player
     * @param eventType the type of event that occurred
     * @param args additional event arguments
     */
    public void processEvent(
            @NotNull final Player player,
            @NotNull final String eventType,
            @NotNull final Object... args
    ) {
        UUID playerUuid = player.getUniqueId();
        
        // Get registered perks for this player and event type
        Map<String, Set<PlayerPerk>> playerEvents = registeredPerks.get(playerUuid);
        if (playerEvents == null) {
            return;
        }
        
        Set<PlayerPerk> perks = playerEvents.get(eventType.toUpperCase());
        if (perks == null || perks.isEmpty()) {
            return;
        }
        
        LOGGER.log(Level.FINE, "Processing event {0} for player {1} with {2} registered perks",
                new Object[]{eventType, player.getName(), perks.size()});
        
        // Process each registered perk
        for (PlayerPerk playerPerk : perks) {
            try {
                processEventPerk(player, playerPerk, eventType, args);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error processing event perk " + 
                        playerPerk.getPerk().getIdentifier() + " for player " + player.getName(), e);
            }
        }
    }
    
    /**
     * Processes a single event perk for an event.
     *
     * @param player the player
     * @param playerPerk the player perk
     * @param eventType the event type
     * @param args event arguments
     */
    private void processEventPerk(
            @NotNull final Player player,
            @NotNull final PlayerPerk playerPerk,
            @NotNull final String eventType,
            @NotNull final Object... args
    ) {
        Perk perk = playerPerk.getPerk();
        
        // Check if perk is on cooldown
        if (playerPerk.isOnCooldown()) {
            LOGGER.log(Level.FINE, "Perk {0} is on cooldown for player {1}",
                    new Object[]{perk.getIdentifier(), player.getName()});
            return;
        }
        
        // Check trigger chance for percentage-based perks
        if (!shouldTrigger(playerPerk)) {
            LOGGER.log(Level.FINE, "Perk {0} did not trigger for player {1} (failed chance roll)",
                    new Object[]{perk.getIdentifier(), player.getName()});
            return;
        }
        
        // Trigger the perk effect
        LOGGER.log(Level.INFO, "Triggering event perk {0} for player {1}",
                new Object[]{perk.getIdentifier(), player.getName()});
        
        // Apply the perk effect based on configuration
        applyEventEffect(player, playerPerk, eventType, args);
        
        // Start cooldown if configured
        checkAndStartCooldown(playerPerk);
    }
    
    /**
     * Determines if a perk should trigger based on its trigger chance.
     * For non-percentage-based perks, always returns true.
     *
     * @param playerPerk the player perk
     * @return true if the perk should trigger, false otherwise
     */
    public boolean shouldTrigger(@NotNull final PlayerPerk playerPerk) {
        Perk perk = playerPerk.getPerk();
        
        // Only check trigger chance for percentage-based perks
        if (perk.getPerkType() != PerkType.PERCENTAGE_BASED) {
            return true;
        }
        
        JsonObject config = parseConfig(perk.getConfigJson());
        
        // Get trigger chance (default 100%)
        double triggerChance = config.has("triggerChance") ? 
                config.get("triggerChance").getAsDouble() : 100.0;
        
        // Roll random number between 0 and 100
        double roll = Math.random() * 100.0;
        
        boolean triggered = roll <= triggerChance;
        
        LOGGER.log(Level.FINE, "Trigger chance check for perk {0}: rolled {1}, needed <= {2}, result: {3}",
                new Object[]{perk.getIdentifier(), roll, triggerChance, triggered});
        
        return triggered;
    }
    
    /**
     * Checks if a perk has a cooldown configured and starts it if so.
     *
     * @param playerPerk the player perk
     * @return true if a cooldown was started, false otherwise
     */
    public boolean checkAndStartCooldown(@NotNull final PlayerPerk playerPerk) {
        Perk perk = playerPerk.getPerk();
        JsonObject config = parseConfig(perk.getConfigJson());
        
        // Get cooldown duration
        long cooldownMillis = config.has("cooldownMillis") ? 
                config.get("cooldownMillis").getAsLong() : 0L;
        
        if (cooldownMillis <= 0) {
            return false;
        }
        
        // Start cooldown
        playerPerk.startCooldown(cooldownMillis);
        
        LOGGER.log(Level.FINE, "Started cooldown for perk {0}: {1}ms",
                new Object[]{perk.getIdentifier(), cooldownMillis});
        
        return true;
    }
    
    /**
     * Applies the effect of an event-triggered perk.
     * This method should be extended to handle specific effect types.
     *
     * @param player the player
     * @param playerPerk the player perk
     * @param eventType the event type
     * @param args event arguments
     */
    private void applyEventEffect(
            @NotNull final Player player,
            @NotNull final PlayerPerk playerPerk,
            @NotNull final String eventType,
            @NotNull final Object... args
    ) {
        Perk perk = playerPerk.getPerk();
        JsonObject config = parseConfig(perk.getConfigJson());
        
        // TODO: Implement specific effect handling based on customConfig
        // For now, just log that the effect would be applied
        LOGGER.log(Level.INFO, "Applied event effect for perk {0} to player {1}",
                new Object[]{perk.getIdentifier(), player.getName()});
        
        // Example: If the perk has a healAmount in customConfig, heal the player
        if (config.has("customConfig")) {
            JsonObject customConfig = config.getAsJsonObject("customConfig");
            
            if (customConfig.has("healAmount")) {
                double healAmount = customConfig.get("healAmount").getAsDouble();
                double newHealth = Math.min(player.getHealth() + healAmount, player.getMaxHealth());
                player.setHealth(newHealth);
                
                LOGGER.log(Level.INFO, "Healed player {0} for {1} health",
                        new Object[]{player.getName(), healAmount});
            }
            
            // Add more effect types as needed
        }
    }
    
    /**
     * Cleans up tracking data for a player.
     * Should be called when a player logs out.
     *
     * @param playerUuid the player UUID
     */
    public void cleanupPlayer(@NotNull final UUID playerUuid) {
        registeredPerks.remove(playerUuid);
        
        LOGGER.log(Level.FINE, "Cleaned up event perks for player {0}", playerUuid);
    }
    
    /**
     * Gets the number of registered perks for a player.
     *
     * @param playerUuid the player UUID
     * @return the number of registered perks
     */
    public int getRegisteredPerkCount(@NotNull final UUID playerUuid) {
        Map<String, Set<PlayerPerk>> playerEvents = registeredPerks.get(playerUuid);
        if (playerEvents == null) {
            return 0;
        }
        
        return playerEvents.values().stream()
                .mapToInt(Set::size)
                .sum();
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
