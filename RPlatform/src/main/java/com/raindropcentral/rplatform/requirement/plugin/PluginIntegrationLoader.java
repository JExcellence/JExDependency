package com.raindropcentral.rplatform.requirement.plugin;

import com.raindropcentral.rplatform.logging.CentralLogger;
import com.raindropcentral.rplatform.requirement.plugin.bridges.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Loads plugin integrations from configuration files.
 * Registers built-in bridges automatically and loads custom ones from config.
 */
public class PluginIntegrationLoader {
	
	private static final Logger LOGGER = CentralLogger.getLogger(PluginIntegrationLoader.class.getName());
	private static final String CONFIG_FILE = "plugin-integrations.yml";
	
	private final Plugin plugin;
	private final PluginIntegrationRegistry registry;
	
	public PluginIntegrationLoader(@NotNull Plugin plugin) {
		this.plugin = plugin;
		this.registry = PluginIntegrationRegistry.getInstance();
	}
	
	/**
	 * Loads all plugin integrations
	 */
	public void loadIntegrations() {
		// First, register all built-in bridges
		registerBuiltInBridges();
		
		// Then load configuration
		final File configFile = new File(plugin.getDataFolder(), CONFIG_FILE);
		
		// Create default config if it doesn't exist
		if (!configFile.exists()) {
			saveDefaultConfig(configFile);
		}
		
		try {
			final YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
			
			// Load supported plugins configuration (enable/disable)
			loadSupportedPlugins(config);
			
			// Load custom integrations
			loadCustomIntegrations(config);
			
			// Load auto-detection priorities
			loadAutoDetectionPriorities(config);
			
			LOGGER.log(Level.INFO, "Loaded plugin integrations: " + 
			           registry.getRegisteredIntegrations().size() + " total");
			
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "Failed to load plugin integrations", e);
		}
	}
	
	/**
	 * Registers all built-in bridges (we maintain these with proper API calls)
	 */
	private void registerBuiltInBridges() {
		// Skills plugins
		registry.register(new EcoSkillsBridge());
		registry.register(new McMMOBridge());
		// registry.register(new AuraSkillsBridge()); // TODO: Implement
		
		// Jobs plugins
		registry.register(new JobsRebornBridge());
		// registry.register(new EcoJobsBridge()); // TODO: Implement
		
		LOGGER.log(Level.INFO, "Registered " + registry.getRegisteredIntegrations().size() + " built-in bridges");
	}
	
	/**
	 * Loads supported plugins configuration (just enable/disable flags)
	 */
	private void loadSupportedPlugins(@NotNull YamlConfiguration config) {
		final ConfigurationSection section = config.getConfigurationSection("supported-plugins");
		if (section == null) {
			return;
		}
		
		for (String integrationId : section.getKeys(false)) {
			final boolean enabled = section.getBoolean(integrationId + ".enabled", true);
			
			if (!enabled) {
				registry.unregister(integrationId);
				LOGGER.log(Level.INFO, "Disabled plugin integration: " + integrationId);
			}
		}
	}
	
	/**
	 * Loads custom integrations (for unsupported plugins)
	 */
	private void loadCustomIntegrations(@NotNull YamlConfiguration config) {
		final ConfigurationSection section = config.getConfigurationSection("custom-integrations");
		if (section == null) {
			return;
		}
		
		int loaded = 0;
		for (String integrationId : section.getKeys(false)) {
			try {
				final ConfigurationSection integrationSection = section.getConfigurationSection(integrationId);
				if (integrationSection == null || !integrationSection.getBoolean("enabled", false)) {
					continue;
				}
				
				loadCustomIntegration(integrationId, integrationSection);
				loaded++;
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "Failed to load custom integration: " + integrationId, e);
			}
		}
		
		if (loaded > 0) {
			LOGGER.log(Level.INFO, "Loaded " + loaded + " custom plugin integrations");
		}
	}
	
	private void loadCustomIntegration(@NotNull String integrationId, @NotNull ConfigurationSection section) {
		final String pluginName = section.getString("plugin-name");
		final String category = section.getString("category");
		final String apiClass = section.getString("api-class");
		final String getInstanceMethod = section.getString("get-instance-method", "");
		final String getValueMethod = section.getString("get-value-method");
		final List<String> getValueParams = section.getStringList("get-value-params");
		final List<String> availableKeys = section.getStringList("available-keys");
		
		if (pluginName == null || category == null || apiClass == null || getValueMethod == null) {
			throw new IllegalArgumentException("Missing required configuration for integration: " + integrationId);
		}
		
		final ConfigurableBridge bridge = new ConfigurableBridge(
			integrationId,
			pluginName,
			category,
			apiClass,
			getInstanceMethod.isEmpty() ? null : getInstanceMethod,
			getValueMethod,
			getValueParams.toArray(new String[0]),
			availableKeys.toArray(new String[0])
		);
		
		registry.register(bridge);
	}
	
	private void loadAutoDetectionPriorities(@NotNull YamlConfiguration config) {
		// TODO: Implement priority system for auto-detection
		// This would allow server owners to prefer certain plugins when using "plugin: auto"
	}
	
	private void saveDefaultConfig(@NotNull File configFile) {
		try {
			configFile.getParentFile().mkdirs();
			
			try (InputStream in = plugin.getResource(CONFIG_FILE)) {
				if (in != null) {
					Files.copy(in, configFile.toPath());
					LOGGER.log(Level.INFO, "Created default " + CONFIG_FILE);
				}
			}
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Failed to save default " + CONFIG_FILE, e);
		}
	}
	
	/**
	 * Reloads all integrations
	 */
	public void reloadIntegrations() {
		registry.clear();
		loadIntegrations();
	}
}
