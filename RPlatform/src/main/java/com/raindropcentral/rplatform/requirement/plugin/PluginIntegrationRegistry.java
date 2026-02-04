package com.raindropcentral.rplatform.requirement.plugin;

import com.raindropcentral.rplatform.logging.CentralLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Registry for plugin integration bridges.
 * Allows dynamic registration of external plugin integrations.
 */
public class PluginIntegrationRegistry {
	
	private static final Logger LOGGER = CentralLogger.getLogger(PluginIntegrationRegistry.class.getName());
	private static PluginIntegrationRegistry instance;
	
	private final Map<String, PluginIntegrationBridge> bridgesByIntegrationId = new ConcurrentHashMap<>();
	private final Map<String, List<PluginIntegrationBridge>> bridgesByCategory = new ConcurrentHashMap<>();
	
	private PluginIntegrationRegistry() {}
	
	public static PluginIntegrationRegistry getInstance() {
		if (instance == null) {
			instance = new PluginIntegrationRegistry();
		}
		return instance;
	}
	
	/**
	 * Registers a plugin integration bridge
	 */
	public void register(@NotNull PluginIntegrationBridge bridge) {
		final String integrationId = bridge.getIntegrationId().toLowerCase();
		
		if (bridgesByIntegrationId.containsKey(integrationId)) {
			LOGGER.log(Level.WARNING, "Plugin integration already registered: " + integrationId);
			return;
		}
		
		bridgesByIntegrationId.put(integrationId, bridge);
		bridgesByCategory.computeIfAbsent(bridge.getCategory(), k -> new ArrayList<>()).add(bridge);
		
		LOGGER.log(Level.INFO, "Registered plugin integration: " + integrationId + 
		           " (plugin: " + bridge.getPluginName() + ", category: " + bridge.getCategory() + ")");
	}
	
	/**
	 * Gets a bridge by integration ID
	 */
	@Nullable
	public PluginIntegrationBridge getBridge(@NotNull String integrationId) {
		return bridgesByIntegrationId.get(integrationId.toLowerCase());
	}
	
	/**
	 * Gets all bridges for a category
	 */
	@NotNull
	public List<PluginIntegrationBridge> getBridgesByCategory(@NotNull String category) {
		return new ArrayList<>(bridgesByCategory.getOrDefault(category, List.of()));
	}
	
	/**
	 * Auto-detects and returns the first available bridge for a category
	 */
	@Nullable
	public PluginIntegrationBridge detectBridge(@NotNull String category) {
		return bridgesByCategory.getOrDefault(category, List.of()).stream()
			.filter(PluginIntegrationBridge::isAvailable)
			.findFirst()
			.orElse(null);
	}
	
	/**
	 * Gets all registered integration IDs
	 */
	@NotNull
	public Set<String> getRegisteredIntegrations() {
		return new HashSet<>(bridgesByIntegrationId.keySet());
	}
	
	/**
	 * Gets all registered categories
	 */
	@NotNull
	public Set<String> getRegisteredCategories() {
		return new HashSet<>(bridgesByCategory.keySet());
	}
	
	/**
	 * Unregisters a bridge
	 */
	public void unregister(@NotNull String integrationId) {
		final PluginIntegrationBridge bridge = bridgesByIntegrationId.remove(integrationId.toLowerCase());
		if (bridge != null) {
			bridgesByCategory.getOrDefault(bridge.getCategory(), List.of()).remove(bridge);
			LOGGER.log(Level.INFO, "Unregistered plugin integration: " + integrationId);
		}
	}
	
	/**
	 * Clears all registrations
	 */
	public void clear() {
		bridgesByIntegrationId.clear();
		bridgesByCategory.clear();
	}
}
