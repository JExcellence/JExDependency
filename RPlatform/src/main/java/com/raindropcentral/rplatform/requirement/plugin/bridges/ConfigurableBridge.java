package com.raindropcentral.rplatform.requirement.plugin.bridges;

import com.raindropcentral.rplatform.requirement.plugin.PluginIntegrationBridge;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Configuration-based bridge that uses reflection to integrate with any plugin.
 * Server owners can configure this in a YAML file without code changes.
 * 
 * Example config:
 * integrations:
 *   customskills:
 *     plugin: "CustomSkillsPlugin"
 *     category: "SKILLS"
 *     api-class: "com.example.CustomSkillsAPI"
 *     get-instance-method: "getInstance"
 *     get-value-method: "getSkillLevel"
 *     get-value-params: ["Player", "String"]
 */
public class ConfigurableBridge implements PluginIntegrationBridge {
	
	private static final Logger LOGGER = Logger.getLogger(ConfigurableBridge.class.getName());
	
	private final String integrationId;
	private final String pluginName;
	private final String category;
	private final String apiClassName;
	private final String getInstanceMethodName;
	private final String getValueMethodName;
	private final String[] getValueParamTypes;
	private final String[] availableKeys;
	
	private transient Object apiInstance;
	private transient Method getValueMethod;
	
	public ConfigurableBridge(
		@NotNull String integrationId,
		@NotNull String pluginName,
		@NotNull String category,
		@NotNull String apiClassName,
		@Nullable String getInstanceMethodName,
		@NotNull String getValueMethodName,
		@NotNull String[] getValueParamTypes,
		@NotNull String[] availableKeys
	) {
		this.integrationId = integrationId.toLowerCase();
		this.pluginName = pluginName;
		this.category = category.toUpperCase();
		this.apiClassName = apiClassName;
		this.getInstanceMethodName = getInstanceMethodName;
		this.getValueMethodName = getValueMethodName;
		this.getValueParamTypes = getValueParamTypes;
		this.availableKeys = availableKeys;
	}
	
	@Override
	@NotNull
	public String getIntegrationId() {
		return integrationId;
	}
	
	@Override
	@NotNull
	public String getPluginName() {
		return pluginName;
	}
	
	@Override
	@NotNull
	public String getCategory() {
		return category;
	}
	
	@Override
	public boolean isAvailable() {
		final Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
		return plugin != null && plugin.isEnabled();
	}
	
	@Override
	public double getValue(@NotNull Player player, @NotNull String key) {
		if (!isAvailable()) {
			return 0.0;
		}
		
		try {
			if (apiInstance == null) {
				initializeAPI();
			}
			
			if (getValueMethod == null) {
				initializeMethod();
			}
			
			if (apiInstance == null || getValueMethod == null) {
				return 0.0;
			}
			
			final Object result = getValueMethod.invoke(apiInstance, player, key);
			
			if (result instanceof Number) {
				return ((Number) result).doubleValue();
			}
			
			LOGGER.log(Level.WARNING, "getValue returned non-numeric result: " + result);
			return 0.0;
			
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Failed to get value from " + integrationId + " for key: " + key, e);
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
		return availableKeys.clone();
	}
	
	private void initializeAPI() throws Exception {
		final Class<?> apiClass = Class.forName(apiClassName);
		
		if (getInstanceMethodName != null && !getInstanceMethodName.isEmpty()) {
			// Singleton pattern
			final Method getInstanceMethod = apiClass.getMethod(getInstanceMethodName);
			apiInstance = getInstanceMethod.invoke(null);
		} else {
			// Direct instantiation
			apiInstance = apiClass.getDeclaredConstructor().newInstance();
		}
		
		LOGGER.log(Level.INFO, "Initialized API for " + integrationId);
	}
	
	private void initializeMethod() throws Exception {
		final Class<?> apiClass = apiInstance.getClass();
		final Class<?>[] paramTypes = new Class<?>[getValueParamTypes.length];
		
		for (int i = 0; i < getValueParamTypes.length; i++) {
			paramTypes[i] = getClassForName(getValueParamTypes[i]);
		}
		
		getValueMethod = apiClass.getMethod(getValueMethodName, paramTypes);
		LOGGER.log(Level.INFO, "Initialized method " + getValueMethodName + " for " + integrationId);
	}
	
	private Class<?> getClassForName(String className) throws ClassNotFoundException {
		return switch (className) {
			case "Player" -> Player.class;
			case "String" -> String.class;
			case "int" -> int.class;
			case "double" -> double.class;
			case "boolean" -> boolean.class;
			default -> Class.forName(className);
		};
	}
}
