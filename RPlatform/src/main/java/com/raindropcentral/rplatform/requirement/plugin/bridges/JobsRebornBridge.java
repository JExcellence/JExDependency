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
 * Built-in bridge for JobsReborn plugin
 */
public class JobsRebornBridge implements PluginIntegrationBridge {
	
	private static final Logger LOGGER = Logger.getLogger(JobsRebornBridge.class.getName());
	
	private Object jobsAPI;
	private boolean initialized = false;
	
	@Override
	@NotNull
	public String getIntegrationId() {
		return "jobsreborn";
	}
	
	@Override
	@NotNull
	public String getPluginName() {
		return "Jobs";
	}
	
	@Override
	@NotNull
	public String getCategory() {
		return "JOBS";
	}
	
	@Override
	public boolean isAvailable() {
		if (!initialized) {
			final var plugin = Bukkit.getPluginManager().getPlugin("Jobs");
			if (plugin != null && plugin.isEnabled()) {
				try {
					final Class<?> jobsClass = Class.forName("com.gamingmesh.jobs.Jobs");
					final var getInstanceMethod = jobsClass.getMethod("getInstance");
					jobsAPI = getInstanceMethod.invoke(null);
					initialized = true;
					LOGGER.log(Level.INFO, "JobsReborn integration initialized");
					return true;
				} catch (Exception e) {
					LOGGER.log(Level.WARNING, "Failed to initialize JobsReborn API", e);
					initialized = true;
					return false;
				}
			}
			initialized = true;
			return false;
		}
		return jobsAPI != null;
	}
	
	@Override
	public double getValue(@NotNull Player player, @NotNull String key) {
		if (!isAvailable()) {
			return 0.0;
		}
		
		try {
			// Get JobsPlayer
			final var getPlayerManagerMethod = jobsAPI.getClass().getMethod("getPlayerManager");
			final Object playerManager = getPlayerManagerMethod.invoke(jobsAPI);
			
			final var getJobsPlayerMethod = playerManager.getClass().getMethod("getJobsPlayer", Player.class);
			final Object jobsPlayer = getJobsPlayerMethod.invoke(playerManager, player);
			
			if (jobsPlayer == null) {
				return 0.0;
			}
			
			// Get job progression
			final var getJobProgressionMethod = jobsPlayer.getClass().getMethod("getJobProgression", String.class);
			final Object progression = getJobProgressionMethod.invoke(jobsPlayer, key);
			
			if (progression == null) {
				return 0.0;
			}
			
			// Get level
			final var getLevelMethod = progression.getClass().getMethod("getLevel");
			final Object level = getLevelMethod.invoke(progression);
			
			return level instanceof Number ? ((Number) level).doubleValue() : 0.0;
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Failed to get JobsReborn level for " + key, e);
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
			// Try to get all jobs from API
			final var getJobsMethod = jobsAPI.getClass().getMethod("getJobs");
			final Object jobs = getJobsMethod.invoke(jobsAPI);
			
			if (jobs instanceof Iterable) {
				return ((Iterable<?>) jobs).toString().split(",");
			}
		} catch (Exception e) {
			LOGGER.log(Level.FINE, "Could not get jobs list from API, using defaults");
		}
		
		// Fallback to common jobs
		return new String[]{
			"Miner", "Builder", "Woodcutter", "Digger", "Farmer",
			"Hunter", "Crafter", "Fisherman", "Weaponsmith", "Brewer", "Enchanter"
		};
	}
	
	@Override
	@Nullable
	public String getDisplayName(@NotNull String key) {
		return key;
	}
}
