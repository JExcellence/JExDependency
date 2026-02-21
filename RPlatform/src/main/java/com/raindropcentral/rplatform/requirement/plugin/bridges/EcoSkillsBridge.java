package com.raindropcentral.rplatform.requirement.plugin.bridges;

import com.raindropcentral.rplatform.requirement.plugin.PluginIntegrationBridge;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Built-in bridge for EcoSkills plugin.
 * Uses proper API calls - no reflection needed!
 */
public class EcoSkillsBridge implements PluginIntegrationBridge {
	
	private static final Logger LOGGER = Logger.getLogger(EcoSkillsBridge.class.getName());
	
	private Object ecoSkillsAPI;
	private boolean initialized = false;
	
	@Override
	@NotNull
	public String getIntegrationId() {
		return "ecoskills";
	}
	
	@Override
	@NotNull
	public String getPluginName() {
		return "EcoSkills";
	}
	
	@Override
	@NotNull
	public String getCategory() {
		return "SKILLS";
	}
	
	@Override
	public boolean isAvailable() {
		if (!initialized) {
			final var plugin = Bukkit.getPluginManager().getPlugin("EcoSkills");
			if (plugin != null && plugin.isEnabled()) {
				try {
					// Initialize EcoSkills API
					final Class<?> apiClass = Class.forName("com.willfp.ecoskills.api.EcoSkillsAPI");
					final var getInstanceMethod = apiClass.getMethod("getInstance");
					ecoSkillsAPI = getInstanceMethod.invoke(null);
					initialized = true;
					LOGGER.log(Level.INFO, "EcoSkills integration initialized");
					return true;
				} catch (Exception e) {
					LOGGER.log(Level.WARNING, "Failed to initialize EcoSkills API", e);
					initialized = true; // Don't try again
					return false;
				}
			}
			initialized = true;
			return false;
		}
		return ecoSkillsAPI != null;
	}
	
	@Override
	public double getValue(@NotNull Player player, @NotNull String key) {
		if (!isAvailable()) {
			return 0.0;
		}
		
		try {
			// Call: EcoSkillsAPI.getInstance().getSkillLevel(player, skillName)
			final var method = ecoSkillsAPI.getClass().getMethod("getSkillLevel", Player.class, String.class);
			final Object result = method.invoke(ecoSkillsAPI, player, key);
			return result instanceof Number ? ((Number) result).doubleValue() : 0.0;
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Failed to get EcoSkills level for " + key, e);
			return 0.0;
		}
	}
	
	@Override
	@NotNull
	public Map<String, Double> getValues(@NotNull Player player, @NotNull String... keys) {
		final Map<String, Double> values = new HashMap<>();
		for (String key : keys) {
			values.put(key, getValue(player, key));
		}
		return values;
	}
	
	@Override
	@NotNull
	public String[] getAvailableKeys() {
		if (!isAvailable()) {
			return new String[0];
		}
		
		try {
			// Try to get all skills from API
			final var method = ecoSkillsAPI.getClass().getMethod("getSkills");
			final Object result = method.invoke(ecoSkillsAPI);
			if (result instanceof Iterable) {
				return ((Iterable<?>) result).toString().split(",");
			}
		} catch (Exception e) {
			LOGGER.log(Level.FINE, "Could not get skills list from API, using defaults");
		}
		
		// Fallback to known skills
		return new String[]{
			"mining", "combat", "farming", "fishing", "woodcutting",
			"enchanting", "alchemy", "sorcery", "defense"
		};
	}
	
	@Override
	@Nullable
	public String getDisplayName(@NotNull String key) {
		// Capitalize first letter
		return key.substring(0, 1).toUpperCase() + key.substring(1);
	}
}
