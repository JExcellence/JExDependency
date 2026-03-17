/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

package com.raindropcentral.rplatform.requirement.impl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.raindropcentral.rplatform.requirement.AbstractRequirement;
import com.raindropcentral.rplatform.requirement.plugin.PluginIntegrationBridge;
import com.raindropcentral.rplatform.requirement.plugin.PluginIntegrationRegistry;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Generic requirement for external plugin integrations.
 * Supports any plugin through the PluginIntegrationBridge system.
 *
 * <p>Examples:
 * - Skills: {"plugin": "ecoskills", "values": {"mining": 50, "combat": 30}}
 * - Jobs: {"plugin": "jobsreborn", "values": {"miner": 10}}
 * - Economy: {"plugin": "vault", "values": {"balance": 1000}}
 *
 * @author ItsRainingHP
 * @since 2.0.0
 * @version 1.0.0
 */
public class PluginRequirement extends AbstractRequirement {
	
	private static final Logger LOGGER = Logger.getLogger(PluginRequirement.class.getName());
	
	@JsonProperty("plugin")
	private final String pluginIntegrationId;
	
	@JsonProperty("category")
	private final String category;
	
	@JsonProperty("values")
	private final Map<String, Double> requiredValues;
	
	@JsonProperty("consumable")
	private final boolean consumable;
	
	@JsonProperty("description")
	private final String description;
	
	@JsonIgnore
	private transient PluginIntegrationBridge bridge;
	
	/**
	 * Simple constructor for single value.
	 */
	public PluginRequirement(@NotNull String pluginIntegrationId, @NotNull String key, double value) {
		this(pluginIntegrationId, null, Map.of(key, value), false, null);
	}
	
	/**
	 * Full constructor.
	 */
	@JsonCreator
	public PluginRequirement(
		@JsonProperty("plugin") @NotNull String pluginIntegrationId,
		@JsonProperty("category") @Nullable String category,
		@JsonProperty("values") @NotNull Map<String, Double> requiredValues,
		@JsonProperty("consumable") @Nullable Boolean consumable,
		@JsonProperty("description") @Nullable String description
	) {
		super("PLUGIN");
		
		if (pluginIntegrationId == null || pluginIntegrationId.trim().isEmpty()) {
			throw new IllegalArgumentException("Plugin integration ID cannot be null or empty");
		}
		
		if (requiredValues.isEmpty()) {
			throw new IllegalArgumentException("Required values cannot be null or empty");
		}
		
		for (Map.Entry<String, Double> entry : requiredValues.entrySet()) {
			if (entry.getKey() == null || entry.getKey().trim().isEmpty()) {
				throw new IllegalArgumentException("Value key cannot be null or empty");
			}
			if (entry.getValue() == null || entry.getValue() < 0) {
				throw new IllegalArgumentException("Value must be non-negative for key: " + entry.getKey());
			}
		}
		
		this.pluginIntegrationId = pluginIntegrationId.trim().toLowerCase(Locale.ROOT);
		this.category = category == null || category.trim().isEmpty()
			? null
			: category.trim().toUpperCase(Locale.ROOT);
		this.requiredValues = new HashMap<>(requiredValues);
		this.consumable = consumable != null && consumable;
		this.description = description;
	}
	
	/**
	 * Returns whether met.
	 */
	@Override
	public boolean isMet(@NotNull Player player) {
		final PluginIntegrationBridge bridge = getBridge();
		if (bridge == null) {
			LOGGER.log(Level.WARNING, "Plugin integration not available: " + pluginIntegrationId);
			return false;
		}
		
		for (Map.Entry<String, Double> entry : requiredValues.entrySet()) {
			final double currentValue = bridge.getValue(player, entry.getKey());
			if (currentValue < entry.getValue()) {
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Executes calculateProgress.
	 */
	@Override
	public double calculateProgress(@NotNull Player player) {
		final PluginIntegrationBridge bridge = getBridge();
		if (bridge == null) {
			return 0.0;
		}
		
		double totalProgress = 0.0;
		
		for (Map.Entry<String, Double> entry : requiredValues.entrySet()) {
			final double currentValue = bridge.getValue(player, entry.getKey());
			final double requiredValue = entry.getValue();
			
			if (requiredValue <= 0) {
				totalProgress += 1.0;
			} else {
				totalProgress += Math.min(1.0, currentValue / requiredValue);
			}
		}
		
		return Math.min(1.0, totalProgress / requiredValues.size());
	}
	
	/**
	 * Executes consume.
	 */
	@Override
	public void consume(@NotNull Player player) {
		if (!consumable) {
			return;
		}
		
		final PluginIntegrationBridge bridge = getBridge();
		if (bridge == null) {
			LOGGER.log(Level.WARNING, "Cannot consume - plugin integration not available: " + pluginIntegrationId);
			return;
		}
		
		for (Map.Entry<String, Double> entry : requiredValues.entrySet()) {
			final boolean success = bridge.consume(player, entry.getKey(), entry.getValue());
			if (!success) {
				LOGGER.log(Level.WARNING, "Failed to consume " + entry.getValue() + " of " + 
				           entry.getKey() + " from " + player.getName());
			}
		}
	}
	
	/**
	 * Gets descriptionKey.
	 */
	@Override
	@NotNull
	public String getDescriptionKey() {
		if (description != null) {
			return description;
		}
		return "requirement.plugin." + pluginIntegrationId;
	}
	
	/**
	 * Gets pluginIntegrationId.
	 */
	@NotNull
	public String getPluginIntegrationId() {
		return pluginIntegrationId;
	}
	
	/**
	 * Gets category.
	 */
	@Nullable
	public String getCategory() {
		return category;
	}
	
	/**
	 * Gets requiredValues.
	 */
	@NotNull
	public Map<String, Double> getRequiredValues() {
		return Collections.unmodifiableMap(requiredValues);
	}
	
	/**
	 * Returns whether consumable.
	 */
	public boolean isConsumable() {
		return consumable;
	}
	
	/**
	 * Gets description.
	 */
	@Nullable
	public String getDescription() {
		return description;
	}
	
	/**
	 * Gets current values for all required keys.
	 */
	@JsonIgnore
	@NotNull
	public Map<String, Double> getCurrentValues(@NotNull Player player) {
		final PluginIntegrationBridge bridge = getBridge();
		if (bridge == null) {
			return new HashMap<>();
		}
		
		final String[] keys = requiredValues.keySet().toArray(new String[0]);
		return bridge.getValues(player, keys);
	}
	
	/**
	 * Gets the bridge for this requirement, with auto-detection fallback.
	 */
	@Nullable
	private PluginIntegrationBridge getBridge() {
		if (bridge == null) {
			final PluginIntegrationRegistry registry = PluginIntegrationRegistry.getInstance();
			
			// Try exact match first
			bridge = registry.getBridge(pluginIntegrationId);
			
			// Fallback to category auto-detection
			if (bridge == null && category != null) {
				bridge = registry.detectBridge(category);
				if (bridge != null) {
					LOGGER.log(Level.INFO, "Auto-detected plugin integration for category " + 
					           category + ": " + bridge.getIntegrationId());
				}
			}
		}
		
		return bridge;
	}
	
	/**
	 * Validates this requirement.
	 */
	@JsonIgnore
	public void validate() {
		if (getBridge() == null) {
			throw new IllegalStateException("Plugin integration not found: " + pluginIntegrationId + 
			                                (category != null ? " (category: " + category + ")" : ""));
		}
	}
}
