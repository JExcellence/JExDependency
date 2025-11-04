package com.raindropcentral.rdq.service;

import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;

/**
 * Interface for perk-specific service listeners.
 * 
 * Consolidates the functionality previously scattered across:
 * - DoubleExperiencePerkService
 * - FlyPerkService
 * - PreventDeathPerkService
 * - TreasureHunterPerkService
 * - VampirePerkService
 * 
 * Lifecycle:
 * 1. onActivate() - Called when perk is activated for a player
 * 2. onDeactivate() - Called when perk is deactivated for a player
 * 3. onTrigger() - Called when perk event is triggered
 * 4. registerEventHandlers() - Called during initialization to register Bukkit listeners
 * 
 * @since 1.0.0
 */
public interface PerkServiceListener {
    
    /**
     * Called when a perk is activated for a player.
     * 
     * Use this to:
     * - Apply initial effects
     * - Set up player state
     * - Register temporary listeners
     * 
     * @param player The player who activated the perk
     */
    void onActivate(@NotNull Player player);
    
    /**
     * Called when a perk is deactivated for a player.
     * 
     * Use this to:
     * - Remove effects
     * - Clean up player state
     * - Unregister temporary listeners
     * 
     * @param player The player who deactivated the perk
     */
    void onDeactivate(@NotNull Player player);
    
    /**
     * Called when a perk event is triggered.
     * 
     * Use this to:
     * - Apply perk effects
     * - Trigger special abilities
     * - Handle perk-specific logic
     * 
     * @param player The player who triggered the perk
     */
    void onTrigger(@NotNull Player player);
    
    /**
     * Register event handlers with the plugin manager.
     * 
     * Called during PerkServiceRegistry initialization.
     * Register all Bukkit listeners needed for this perk here.
     * 
     * @param manager The Bukkit PluginManager
     */
    void registerEventHandlers(@NotNull PluginManager manager);
    
    /**
     * Get the perk ID this listener handles.
     * 
     * @return The perk ID
     */
    @NotNull String getPerkId();
}
