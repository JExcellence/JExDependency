package com.raindropcentral.rplatform.requirement.plugin;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Bridge interface for integrating external plugins with the requirement system.
 * Implementations handle specific plugin APIs (Skills, Jobs, Economy, etc.)
 */
public interface PluginIntegrationBridge {
	
	/**
	 * Gets the unique identifier for this integration (e.g., "ecoskills", "jobsreborn")
	 */
	@NotNull String getIntegrationId();
	
	/**
	 * Gets the plugin name this bridge integrates with
	 */
	@NotNull String getPluginName();
	
	/**
	 * Gets the category of this integration (e.g., "SKILLS", "JOBS", "ECONOMY")
	 */
	@NotNull String getCategory();
	
	/**
	 * Checks if the plugin is available and enabled
	 */
	boolean isAvailable();
	
	/**
	 * Gets a numeric value for a player (e.g., skill level, job level, balance)
	 * 
	 * @param player The player to check
	 * @param key The specific key to check (e.g., skill name, job name)
	 * @return The current value, or 0 if not found
	 */
	double getValue(@NotNull Player player, @NotNull String key);
	
	/**
	 * Gets multiple values for a player at once
	 * 
	 * @param player The player to check
	 * @param keys The keys to check
	 * @return Map of key to value
	 */
	@NotNull Map<String, Double> getValues(@NotNull Player player, @NotNull String... keys);
	
	/**
	 * Gets all available keys for this integration (e.g., all skill names)
	 */
	@NotNull String[] getAvailableKeys();
	
	/**
	 * Optional: Consume/deduct a value (for consumable requirements)
	 * 
	 * @return true if successfully consumed
	 */
	default boolean consume(@NotNull Player player, @NotNull String key, double amount) {
		return false; // Default: non-consumable
	}
	
	/**
	 * Gets a human-readable display name for a key
	 */
	@Nullable
	default String getDisplayName(@NotNull String key) {
		return key;
	}
}
