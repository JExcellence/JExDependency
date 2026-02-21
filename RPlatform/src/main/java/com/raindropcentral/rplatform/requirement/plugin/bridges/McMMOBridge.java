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
 * Built-in bridge for mcMMO plugin
 */
public class McMMOBridge implements PluginIntegrationBridge {
	
	private static final Logger LOGGER = Logger.getLogger(McMMOBridge.class.getName());
	
	private Class<?> experienceAPI;
	private boolean initialized = false;
	
	@Override
	@NotNull
	public String getIntegrationId() {
		return "mcmmo";
	}
	
	@Override
	@NotNull
	public String getPluginName() {
		return "mcMMO";
	}
	
	@Override
	@NotNull
	public String getCategory() {
		return "SKILLS";
	}
	
	@Override
	public boolean isAvailable() {
		if (!initialized) {
			final var plugin = Bukkit.getPluginManager().getPlugin("mcMMO");
			if (plugin != null && plugin.isEnabled()) {
				try {
					experienceAPI = Class.forName("com.gmail.nossr50.api.ExperienceAPI");
					initialized = true;
					LOGGER.log(Level.INFO, "mcMMO integration initialized");
					return true;
				} catch (Exception e) {
					LOGGER.log(Level.WARNING, "Failed to initialize mcMMO API", e);
					initialized = true;
					return false;
				}
			}
			initialized = true;
			return false;
		}
		return experienceAPI != null;
	}
	
	@Override
	public double getValue(@NotNull Player player, @NotNull String key) {
		if (!isAvailable()) {
			return 0.0;
		}
		
		try {
			// Call: ExperienceAPI.getLevel(player, skillName)
			final var method = experienceAPI.getMethod("getLevel", Player.class, String.class);
			final Object result = method.invoke(null, player, key);
			return result instanceof Number ? ((Number) result).doubleValue() : 0.0;
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Failed to get mcMMO level for " + key, e);
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
		return new String[]{
			"acrobatics", "alchemy", "archery", "axes", "excavation",
			"fishing", "herbalism", "mining", "repair", "swords",
			"taming", "unarmed", "woodcutting"
		};
	}
	
	@Override
	@Nullable
	public String getDisplayName(@NotNull String key) {
		return key.substring(0, 1).toUpperCase() + key.substring(1);
	}
}
