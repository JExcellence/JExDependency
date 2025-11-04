package com.raindropcentral.rdq.service;

import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Registry for perk-specific service listeners.
 * 
 * Consolidates the functionality previously scattered across:
 * - DoubleExperiencePerkService
 * - FlyPerkService
 * - PreventDeathPerkService
 * - TreasureHunterPerkService
 * - VampirePerkService
 * 
 * Responsibilities:
 * - Register perk-specific listeners
 * - Manage listener lifecycle (activation/deactivation)
 * - Trigger perk events
 * - Register Bukkit event handlers
 * 
 * Thread Safety:
 * - All operations are thread-safe using ConcurrentHashMap
 * - Async registration supported via CompletableFuture
 * 
 * @since 1.0.0
 */
public class PerkServiceRegistry {
    
    private static final Logger logger = LoggerFactory.getLogger(PerkServiceRegistry.class);
    
    private final Map<String, PerkServiceListener> listeners = new ConcurrentHashMap<>();
    private final PluginManager pluginManager;
    private volatile boolean initialized = false;
    
    /**
     * Create a new PerkServiceRegistry.
     * 
     * @param pluginManager The Bukkit PluginManager for event registration
     */
    public PerkServiceRegistry(@NotNull PluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }
    
    /**
     * Register a perk service listener.
     * 
     * @param perkId The perk ID
     * @param listener The listener to register
     * @throws IllegalArgumentException if perkId is blank or listener is null
     */
    public void register(@NotNull String perkId, @NotNull PerkServiceListener listener) {
        if (perkId.isBlank()) {
            throw new IllegalArgumentException("Perk ID cannot be blank");
        }

        listeners.put(perkId, listener);
        logger.debug("Registered perk service listener for perk: {}", perkId);
    }
    
    /**
     * Register all perk service listeners asynchronously.
     * 
     * This method should be called during plugin initialization.
     * It registers all listeners and their event handlers.
     * 
     * @param executor The executor to use for async operations
     * @return A CompletableFuture that completes when all listeners are registered
     */
    public CompletableFuture<Void> registerAllAsync(@NotNull Executor executor) {
        return CompletableFuture.runAsync(() -> {
            try {
                logger.info("Registering {} perk service listeners", listeners.size());
                
                for (PerkServiceListener listener : listeners.values()) {
                    try {
                        listener.registerEventHandlers(pluginManager);
                        logger.debug("Registered event handlers for perk: {}", listener.getPerkId());
                    } catch (Exception e) {
                        logger.error("Failed to register event handlers for perk: {}", 
                            listener.getPerkId(), e);
                    }
                }
                
                initialized = true;
                logger.info("Successfully registered all perk service listeners");
            } catch (Exception e) {
                logger.error("Failed to register perk service listeners", e);
                throw new RuntimeException("Perk service registration failed", e);
            }
        }, executor);
    }
    
    /**
     * Activate a perk for a player.
     * 
     * @param perkId The perk ID
     * @param player The player
     * @return true if the listener was found and activated, false otherwise
     */
    public boolean activate(@NotNull String perkId, @NotNull Player player) {
        PerkServiceListener listener = listeners.get(perkId);
        if (listener == null) {
            logger.warn("No listener registered for perk: {}", perkId);
            return false;
        }
        
        try {
            listener.onActivate(player);
            logger.debug("Activated perk {} for player {}", perkId, player.getName());
            return true;
        } catch (Exception e) {
            logger.error("Error activating perk {} for player {}", perkId, player.getName(), e);
            return false;
        }
    }
    
    /**
     * Deactivate a perk for a player.
     * 
     * @param perkId The perk ID
     * @param player The player
     * @return true if the listener was found and deactivated, false otherwise
     */
    public boolean deactivate(@NotNull String perkId, @NotNull Player player) {
        PerkServiceListener listener = listeners.get(perkId);
        if (listener == null) {
            logger.warn("No listener registered for perk: {}", perkId);
            return false;
        }
        
        try {
            listener.onDeactivate(player);
            logger.debug("Deactivated perk {} for player {}", perkId, player.getName());
            return true;
        } catch (Exception e) {
            logger.error("Error deactivating perk {} for player {}", perkId, player.getName(), e);
            return false;
        }
    }
    
    /**
     * Trigger a perk event for a player.
     * 
     * @param perkId The perk ID
     * @param player The player
     * @return true if the listener was found and triggered, false otherwise
     */
    public boolean trigger(@NotNull String perkId, @NotNull Player player) {
        PerkServiceListener listener = listeners.get(perkId);
        if (listener == null) {
            logger.warn("No listener registered for perk: {}", perkId);
            return false;
        }
        
        try {
            listener.onTrigger(player);
            logger.debug("Triggered perk {} for player {}", perkId, player.getName());
            return true;
        } catch (Exception e) {
            logger.error("Error triggering perk {} for player {}", perkId, player.getName(), e);
            return false;
        }
    }
    
    /**
     * Get a registered listener by perk ID.
     * 
     * @param perkId The perk ID
     * @return The listener, or null if not registered
     */
    @Nullable
    public PerkServiceListener getListener(@NotNull String perkId) {
        return listeners.get(perkId);
    }
    
    /**
     * Check if a listener is registered for a perk.
     * 
     * @param perkId The perk ID
     * @return true if a listener is registered, false otherwise
     */
    public boolean hasListener(@NotNull String perkId) {
        return listeners.containsKey(perkId);
    }
    
    /**
     * Get all registered listeners.
     * 
     * @return A collection of all registered listeners
     */
    @NotNull
    public Collection<PerkServiceListener> getAllListeners() {
        return listeners.values();
    }
    
    /**
     * Get the number of registered listeners.
     * 
     * @return The number of registered listeners
     */
    public int getListenerCount() {
        return listeners.size();
    }
    
    /**
     * Check if the registry has been initialized.
     * 
     * @return true if registerAllAsync() has completed successfully
     */
    public boolean isInitialized() {
        return initialized;
    }
    
    /**
     * Clear all registered listeners.
     * 
     * Use with caution - this should only be called during shutdown.
     */
    public void clear() {
        listeners.clear();
        initialized = false;
        logger.info("Cleared all perk service listeners");
    }
}
