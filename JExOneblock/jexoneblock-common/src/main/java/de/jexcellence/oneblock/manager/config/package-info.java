/**
 * Configuration management system for JExOneblock.
 * 
 * <p>This package provides a comprehensive configuration management system that implements
 * the ConfigManager and ConfigKeeper pattern with the following features:</p>
 * 
 * <h2>Core Components</h2>
 * 
 * <h2>Features</h2>
 * <ul>
 *   <li><strong>Type-safe configuration sections</strong> - Configurations are loaded into strongly-typed classes</li>
 *   <li><strong>Configuration validation</strong> - Built-in validation with custom validators</li>
 *   <li><strong>Caching with expiration policies</strong> - Automatic caching with configurable expiration</li>
 *   <li><strong>Cache invalidation strategies</strong> - Manual and automatic cache invalidation</li>
 *   <li><strong>Hot-reloading support</strong> - Automatic reload when configuration files change</li>
 *   <li><strong>Performance monitoring</strong> - Cache statistics and performance metrics</li>
 *   <li><strong>Error handling</strong> - Comprehensive error handling with fallback mechanisms</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create configuration manager
 * YamlConfigLoader loader = new YamlConfigLoader(plugin);
 * ConfigurationManager configManager = new ConfigurationManager(
 *     executorService, 
 *     scheduledExecutor, 
 *     loader
 * );
 * 
 * // Initialize and start
 * configManager.initialize().join();
 * configManager.start().join();
 * 
 * // Load configuration with validation
 * IslandConfiguration islandConfig = configManager.getConfiguration(
 *     "island", 
 *     IslandConfiguration.class
 * );
 * 
 * // Enable auto-reload
 * configManager.enableAutoReload("island", Duration.ofMinutes(5));
 * 
 * // Get configuration with fallback
 * CalculationConfiguration calcConfig = configManager.getConfigurationWithDefault(
 *     "calculation", 
 *     CalculationConfiguration.class, 
 *     new CalculationConfiguration() // default
 * );
 * }</pre>
 * 
 * <h2>Configuration Classes</h2>
 * <p>Configuration classes should follow these patterns:</p>
 * <ul>
 *   <li>Provide a constructor that accepts {@link org.bukkit.configuration.ConfigurationSection}</li>
 *   <li>Implement a {@code load(ConfigurationSection)} method</li>
 *   <li>Implement a {@code validate()} method for validation</li>
 *   <li>Provide static factory methods for validators</li>
 * </ul>
 * 
 * <h2>Hot-Reload Support</h2>
 * <p>The hot-reload system monitors configuration files for changes and automatically
 * triggers reloads. Listeners can be registered to receive notifications:</p>
 * <pre>{@code
 * HotReloadManager hotReload = new HotReloadManager(configDirectory, executorService);
 * hotReload.startMonitoring();
 * 
 * hotReload.addListener("island", event -> {
 *     if (event.shouldReload()) {
 *         configManager.reloadConfiguration("island");
 *     }
 * });
 * }</pre>
 * 
 * @author JExcellence
 * @since 2.0.0
 * @version 2.0.0
 */
package de.jexcellence.oneblock.manager.config;