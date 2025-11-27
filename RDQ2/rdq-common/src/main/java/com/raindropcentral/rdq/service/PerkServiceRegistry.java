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

public class PerkServiceRegistry {
    
    private static final Logger logger = LoggerFactory.getLogger(PerkServiceRegistry.class);
    
    private final Map<String, PerkServiceListener> listeners = new ConcurrentHashMap<>();
    private final PluginManager pluginManager;
    private volatile boolean initialized = false;
    
    public PerkServiceRegistry(@NotNull PluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }
    
    public void register(@NotNull String perkId, @NotNull PerkServiceListener listener) {
        if (perkId.isBlank()) throw new IllegalArgumentException("Perk ID cannot be blank");
        listeners.put(perkId, listener);
        logger.debug("Registered perk service listener for perk: {}", perkId);
    }
    
    public CompletableFuture<Void> registerAllAsync(@NotNull Executor executor) {
        return CompletableFuture.runAsync(() -> {
            try {
                logger.info("Registering {} perk service listeners", listeners.size());
                
                listeners.values().forEach(listener -> {
                    try {
                        listener.registerEventHandlers(pluginManager);
                        logger.debug("Registered event handlers for perk: {}", listener.getPerkId());
                    } catch (Exception e) {
                        logger.error("Failed to register event handlers for perk: {}", listener.getPerkId(), e);
                    }
                });
                
                initialized = true;
                logger.info("Successfully registered all perk service listeners");
            } catch (Exception e) {
                logger.error("Failed to register perk service listeners", e);
                throw new RuntimeException("Perk service registration failed", e);
            }
        }, executor);
    }
    
    public boolean activate(@NotNull String perkId, @NotNull Player player) {
        var listener = listeners.get(perkId);
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
    
    public boolean deactivate(@NotNull String perkId, @NotNull Player player) {
        var listener = listeners.get(perkId);
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
    
    public boolean trigger(@NotNull String perkId, @NotNull Player player) {
        var listener = listeners.get(perkId);
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
    
    @Nullable
    public PerkServiceListener getListener(@NotNull String perkId) {
        return listeners.get(perkId);
    }
    
    public boolean hasListener(@NotNull String perkId) {
        return listeners.containsKey(perkId);
    }
    
    @NotNull
    public Collection<PerkServiceListener> getAllListeners() {
        return listeners.values();
    }
    
    public int getListenerCount() {
        return listeners.size();
    }
    
    public boolean isInitialized() {
        return initialized;
    }
    
    public void clear() {
        listeners.clear();
        initialized = false;
        logger.info("Cleared all perk service listeners");
    }
}
