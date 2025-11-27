package com.raindropcentral.rdq.requirement;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Flexible requirement that allows server owners to define custom logic.
 * <p>
 * The {@code CustomRequirement} provides a way for server owners to create their own
 * requirement logic without modifying the core plugin. It supports JavaScript scripting
 * for custom logic and custom data storage for configuration. This requirement is
 * perfect for unique server mechanics, custom plugin integrations, or specialized
 * progression systems.
 * </p>
 *
 * <ul>
 *   <li>Supports JavaScript scripting for custom requirement logic.</li>
 *   <li>Provides custom data storage for configuration and state.</li>
 *   <li>Safe execution environment with error handling.</li>
 *   <li>Helper methods for common Bukkit operations.</li>
 *   <li>Extensible design for future custom requirement types.</li>
 *   <li>Integrates with RequirementSection for flexible configuration.</li>
 * </ul>
 *
 * <p>
 * <b>Security Note:</b> Custom scripts are executed in a controlled environment,
 * but server owners should be cautious about the scripts they deploy.
 * </p>
 *
 * @author JExcellence
 * @version 1.1.0
 * @since TBD
 */
public class CustomRequirement extends AbstractRequirement {
    
    private static final Logger LOGGER = Logger.getLogger(CustomRequirement.class.getName());
    
    /**
     * Enumeration of supported custom requirement types.
     */
    public enum CustomType {
        /**
         * JavaScript-based custom requirement.
         */
        SCRIPT,
        
        /**
         * Plugin-based custom requirement (for future extension).
         */
        PLUGIN,
        
        /**
         * Data-driven custom requirement.
         */
        DATA
    }
    
    /**
     * The type of custom requirement.
     */
    @JsonProperty("customType")
    private final CustomType customType;
    
    /**
     * The custom script to execute (for SCRIPT type).
     */
    @JsonProperty("customScript")
    private final String customScript;
    
    /**
     * The progress calculation script (optional, for SCRIPT type).
     */
    @JsonProperty("progressScript")
    private final String progressScript;
    
    /**
     * The consumption script (optional, for SCRIPT type).
     */
    @JsonProperty("consumeScript")
    private final String consumeScript;
    
    /**
     * Custom data for configuration and state.
     */
    @JsonProperty("customData")
    private final Map<String, Object> customData;
    
    /**
     * Optional description for this custom requirement.
     */
    @JsonProperty("description")
    private final String description;
    
    /**
     * Whether to cache script compilation for performance.
     */
    @JsonProperty("cacheScripts")
    private final boolean cacheScripts;
    
    /**
     * Cached script engine for performance.
     * This is resolved at runtime and not serialized.
     */
    @JsonIgnore
    private transient ScriptEngine scriptEngine;
    
    /**
     * Constructs a {@code CustomRequirement} with a custom script.
     *
     * @param customScript The JavaScript code to execute for requirement checking.
     */
    public CustomRequirement(
        @NotNull final String customScript
    ) {
        this(CustomType.SCRIPT, customScript, null, null, new HashMap<>(), null, true);
    }
    
    /**
     * Constructs a {@code CustomRequirement} with script and custom data.
     *
     * @param customScript The JavaScript code to execute for requirement checking.
     * @param customData Custom data for configuration.
     */
    public CustomRequirement(
        @NotNull final String customScript,
        @NotNull final Map<String, Object> customData
    ) {
        this(CustomType.SCRIPT, customScript, null, null, customData, null, true);
    }
    
    /**
     * Constructs a {@code CustomRequirement} with full configuration options.
     *
     * @param customType The type of custom requirement.
     * @param customScript The main script to execute (can be null for non-script types).
     * @param progressScript The progress calculation script (can be null).
     * @param consumeScript The consumption script (can be null).
     * @param customData Custom data for configuration and state.
     * @param description Optional description for this requirement.
     * @param cacheScripts Whether to cache script compilation.
     */
    @JsonCreator
    public CustomRequirement(
        @JsonProperty("customType") @Nullable final CustomType customType,
        @JsonProperty("customScript") @Nullable final String customScript,
        @JsonProperty("progressScript") @Nullable final String progressScript,
        @JsonProperty("consumeScript") @Nullable final String consumeScript,
        @JsonProperty("customData") @Nullable final Map<String, Object> customData,
        @JsonProperty("description") @Nullable final String description,
        @JsonProperty("cacheScripts") @Nullable final Boolean cacheScripts
    ) {
        super(Type.CUSTOM);
        
        this.customType = customType != null ? customType : CustomType.SCRIPT;
        
        // Validate based on custom type
        if (this.customType == CustomType.SCRIPT) {
            if (customScript == null || customScript.trim().isEmpty()) {
                throw new IllegalArgumentException("Custom script cannot be null or empty for SCRIPT type.");
            }
        }
        
        this.customScript = customScript;
        this.progressScript = progressScript;
        this.consumeScript = consumeScript;
        this.customData = customData != null ? new HashMap<>(customData) : new HashMap<>();
        this.description = description;
        this.cacheScripts = cacheScripts != null ? cacheScripts : true;
    }
    
    /**
     * Checks if the custom requirement is met based on the configured logic.
     *
     * @param player The player whose state will be checked.
     * @return {@code true} if the custom requirement is met, {@code false} otherwise.
     */
    @Override
    public boolean isMet(
        @NotNull final Player player
    ) {
        return switch (this.customType) {
            case SCRIPT -> this.executeScript(player, this.customScript, Boolean.class, false);
            case PLUGIN -> this.executePluginLogic(player, "isMet");
            case DATA -> this.executeDataLogic(player, "isMet");
        };
    }
    
    /**
     * Calculates the progress towards fulfilling the custom requirement.
     * <p>
     * Progress calculation depends on the custom type:
     * <ul>
     *   <li><b>SCRIPT:</b> Executes the progressScript if provided, otherwise returns 1.0 if met, 0.0 if not.</li>
     *   <li><b>PLUGIN:</b> Delegates to plugin-specific logic.</li>
     *   <li><b>DATA:</b> Uses data-driven logic for progress calculation.</li>
     * </ul>
     * </p>
     *
     * @param player The player whose progress will be evaluated.
     * @return A double between 0.0 and 1.0 representing progress.
     */
    @Override
    public double calculateProgress(
        @NotNull final Player player
    ) {
        return switch (this.customType) {
            case SCRIPT -> {
                if (this.progressScript != null && !this.progressScript.trim().isEmpty()) {
                    yield this.executeScript(player, this.progressScript, Double.class, 0.0);
                } else {
                    // Default: 1.0 if met, 0.0 if not
                    yield this.isMet(player) ? 1.0 : 0.0;
                }
            }
            case PLUGIN -> this.executePluginProgressLogic(player);
            case DATA -> this.executeDataProgressLogic(player);
        };
    }
    
    /**
     * Consumes resources from the player to fulfill this requirement.
     * <p>
     * Consumption behavior depends on the custom type and configured scripts.
     * </p>
     *
     * @param player The player from whom resources will be consumed.
     */
    @Override
    public void consume(
        @NotNull final Player player
    ) {
        switch (this.customType) {
            case SCRIPT -> {
                if (this.consumeScript != null && !this.consumeScript.trim().isEmpty()) {
                    this.executeScript(player, this.consumeScript, Void.class, null);
                }
                // If no consume script, do nothing
            }
            case PLUGIN -> this.executePluginConsumeLogic(player);
            case DATA -> this.executeDataConsumeLogic(player);
        }
    }
    
    /**
     * Returns the translation key for this requirement's description.
     * <p>
     * This key can be used for localization and user-facing descriptions.
     * </p>
     *
     * @return The language key for this requirement's description.
     */
    @Override
    @NotNull
    public String getDescriptionKey() {
        return "requirement.custom";
    }
    
    /**
     * Gets the custom requirement type.
     *
     * @return The custom type.
     */
    @NotNull
    public CustomType getCustomType() {
        return this.customType;
    }
    
    /**
     * Gets the custom script.
     *
     * @return The custom script, or null if not applicable.
     */
    @Nullable
    public String getCustomScript() {
        return this.customScript;
    }
    
    /**
     * Gets the progress calculation script.
     *
     * @return The progress script, or null if not provided.
     */
    @Nullable
    public String getProgressScript() {
        return this.progressScript;
    }
    
    /**
     * Gets the consumption script.
     *
     * @return The consume script, or null if not provided.
     */
    @Nullable
    public String getConsumeScript() {
        return this.consumeScript;
    }
    
    /**
     * Gets the custom data map.
     *
     * @return Unmodifiable map of custom data.
     */
    @NotNull
    public Map<String, Object> getCustomData() {
        return Collections.unmodifiableMap(this.customData);
    }
    
    /**
     * Gets a specific value from custom data.
     *
     * @param key The data key.
     * @param defaultValue The default value if key is not found.
     * @param <T> The expected type.
     * @return The value or default value.
     */
    @JsonIgnore
    @SuppressWarnings("unchecked")
    public <T> T getCustomDataValue(
        @NotNull final String key,
        @Nullable final T defaultValue
    ) {
        final Object value = this.customData.get(key);
        if (value == null) {
            return defaultValue;
        }
        
        try {
            return (T) value;
        } catch (final ClassCastException e) {
            LOGGER.log(Level.WARNING, "Custom data value for key '" + key + "' is not of expected type", e);
            return defaultValue;
        }
    }
    
    /**
     * Gets the optional description for this custom requirement.
     *
     * @return The description, or null if not provided.
     */
    @Nullable
    public String getDescription() {
        return this.description;
    }
    
    /**
     * Gets whether script caching is enabled.
     *
     * @return True if scripts are cached, false otherwise.
     */
    public boolean isCacheScripts() {
        return this.cacheScripts;
    }
    
    /**
     * Checks if this requirement uses scripting.
     *
     * @return True if using SCRIPT type, false otherwise.
     */
    @JsonIgnore
    public boolean isScriptBased() {
        return this.customType == CustomType.SCRIPT;
    }
    
    /**
     * Checks if this requirement uses plugin integration.
     *
     * @return True if using PLUGIN type, false otherwise.
     */
    @JsonIgnore
    public boolean isPluginBased() {
        return this.customType == CustomType.PLUGIN;
    }
    
    /**
     * Checks if this requirement uses data-driven logic.
     *
     * @return True if using DATA type, false otherwise.
     */
    @JsonIgnore
    public boolean isDataBased() {
        return this.customType == CustomType.DATA;
    }
    
    /**
     * Validates the internal state of this custom requirement.
     *
     * @throws IllegalStateException If the requirement is in an invalid state.
     */
    @JsonIgnore
    public void validate() {
        if (this.customType == CustomType.SCRIPT) {
            if (this.customScript == null || this.customScript.trim().isEmpty()) {
                throw new IllegalStateException("Custom script cannot be null or empty for SCRIPT type.");
            }
            
            // Test script compilation
            try {
                this.getScriptEngine();
            } catch (final Exception e) {
                throw new IllegalStateException("Failed to initialize script engine", e);
            }
        }
        
        if (this.customData == null) {
            throw new IllegalStateException("Custom data cannot be null.");
        }
    }
    
    /**
     * Gets or creates the script engine for JavaScript execution.
     *
     * @return The script engine.
     */
    @NotNull
    private ScriptEngine getScriptEngine() {
        if (this.scriptEngine == null || !this.cacheScripts) {
            final ScriptEngineManager manager = new ScriptEngineManager();
            this.scriptEngine = manager.getEngineByName("JavaScript");
            
            if (this.scriptEngine == null) {
                throw new IllegalStateException("JavaScript engine not available");
            }
        }
        return this.scriptEngine;
    }
    
    /**
     * Executes a JavaScript script with the player context.
     *
     * @param player The player context.
     * @param script The script to execute.
     * @param expectedType The expected return type.
     * @param defaultValue The default value if execution fails.
     * @param <T> The return type.
     * @return The script result or default value.
     */
    @SuppressWarnings("unchecked")
    private <T> T executeScript(
        @NotNull final Player player,
        @NotNull final String script,
        @NotNull final Class<T> expectedType,
        @Nullable final T defaultValue
    ) {
        try {
            final ScriptEngine engine = this.getScriptEngine();
            
            // Set up script context
            engine.put("player", player);
            engine.put("customData", this.customData);
            engine.put("requirement", this);
            
            // Add helper functions and objects
            this.setupScriptHelpers(engine, player);
            
            // Execute script
            final Object result = engine.eval(script);
            
            if (result == null) {
                return defaultValue;
            }
            
            // Type conversion
            if (expectedType == Boolean.class && result instanceof Boolean) {
                return (T) result;
            } else if (expectedType == Double.class) {
                if (result instanceof Number) {
                    return (T) Double.valueOf(((Number) result).doubleValue());
                }
            } else if (expectedType == Void.class) {
                return null;
            }
            
            return (T) result;
            
        } catch (final ScriptException e) {
            LOGGER.log(Level.SEVERE, "Error executing custom script: " + script, e);
            return defaultValue;
        } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "Unexpected error in custom script execution", e);
            return defaultValue;
        }
    }
    
    /**
     * Sets up helper functions and variables for script execution.
     *
     * @param engine The script engine.
     * @param player The player context.
     */
    private void setupScriptHelpers(
        @NotNull final ScriptEngine engine,
        @NotNull final Player player
    ) {
        try {
            // Add Bukkit enums and classes for easy access
            engine.put("Statistic", Statistic.class);
            engine.put("EntityType", EntityType.class);
            
            // Add common helper functions
            engine.eval("""
                // Custom data helpers
                function getCustomData(key, defaultValue) {
                    var value = customData.get(key);
                    return value != null ? value : defaultValue;
                }
                
                function hasCustomData(key) {
                    return customData.containsKey(key);
                }
                
                // Logging helper
                function log(message) {
                    java.util.logging.Logger.getLogger('CustomRequirement').info(message);
                }
                
                // Kill count helpers
                function getKillCount(mobType) {
                    try {
                        var entityType = EntityType.valueOf(mobType.toUpperCase());
                        return player.getStatistic(Statistic.KILL_ENTITY, entityType);
                    } catch (e) {
                        log('Invalid mob type: ' + mobType);
                        return 0;
                    }
                }
                
                function getMobKills(mobType) {
                    return getKillCount(mobType);
                }
                
                // Death count helpers
                function getDeathCount() {
                    return player.getStatistic(Statistic.DEATHS);
                }
                
                function getDeathsByEntity(mobType) {
                    try {
                        var entityType = EntityType.valueOf(mobType.toUpperCase());
                        return player.getStatistic(Statistic.ENTITY_KILLED_BY, entityType);
                    } catch (e) {
                        log('Invalid mob type: ' + mobType);
                        return 0;
                    }
                }
                
                // Block helpers
                function getBlocksBroken(blockType) {
                    try {
                        var material = org.bukkit.Material.valueOf(blockType.toUpperCase());
                        return player.getStatistic(Statistic.MINE_BLOCK, material);
                    } catch (e) {
                        log('Invalid block type: ' + blockType);
                        return 0;
                    }
                }
                
                function getBlocksPlaced(blockType) {
                    try {
                        var material = org.bukkit.Material.valueOf(blockType.toUpperCase());
                        return player.getStatistic(Statistic.USE_ITEM, material);
                    } catch (e) {
                        log('Invalid block type: ' + blockType);
                        return 0;
                    }
                }
                
                // Distance helpers
                function getDistanceWalked() {
                    return player.getStatistic(Statistic.WALK_ONE_CM) / 100.0; // Convert to blocks
                }
                
                function getDistanceFlown() {
                    return player.getStatistic(Statistic.FLY_ONE_CM) / 100.0; // Convert to blocks
                }
                
                function getDistanceSwum() {
                    return player.getStatistic(Statistic.SWIM_ONE_CM) / 100.0; // Convert to blocks
                }
                
                // Time helpers
                function getPlayTime() {
                    return player.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20.0; // Convert to seconds
                }
                
                function getPlayTimeHours() {
                    return getPlayTime() / 3600.0; // Convert to hours
                }
                
                // Item helpers
                function getItemsUsed(itemType) {
                    try {
                        var material = org.bukkit.Material.valueOf(itemType.toUpperCase());
                        return player.getStatistic(Statistic.USE_ITEM, material);
                    } catch (e) {
                        log('Invalid item type: ' + itemType);
                        return 0;
                    }
                }
                
                function getItemsCrafted(itemType) {
                    try {
                        var material = org.bukkit.Material.valueOf(itemType.toUpperCase());
                        return player.getStatistic(Statistic.CRAFT_ITEM, material);
                    } catch (e) {
                        log('Invalid item type: ' + itemType);
                        return 0;
                    }
                }
                
                // Player state helpers
                function hasPermission(permission) {
                    return player.hasPermission(permission);
                }
                
                function getLevel() {
                    return player.getLevel();
                }
                
                function getExperience() {
                    return player.getTotalExperience();
                }
                
                function getHealth() {
                    return player.getHealth();
                }
                
                function getMaxHealth() {
                    return player.getMaxHealth();
                }
                
                function getFoodLevel() {
                    return player.getFoodLevel();
                }
                
                // Location helpers
                function getWorld() {
                    return player.getWorld().getName();
                }
                
                function getX() {
                    return player.getLocation().getX();
                }
                
                function getY() {
                    return player.getLocation().getY();
                }
                
                function getZ() {
                    return player.getLocation().getZ();
                }
                
                function distanceTo(x, y, z) {
                    var loc = player.getLocation();
                    var dx = loc.getX() - x;
                    var dy = loc.getY() - y;
                    var dz = loc.getZ() - z;
                    return Math.sqrt(dx*dx + dy*dy + dz*dz);
                }
                
                // Inventory helpers
                function hasItem(itemType, amount) {
                    try {
                        var material = org.bukkit.Material.valueOf(itemType.toUpperCase());
                        var itemStack = new org.bukkit.inventory.ItemStack(material, amount || 1);
                        return player.getInventory().containsAtLeast(itemStack, amount || 1);
                    } catch (e) {
                        log('Invalid item type: ' + itemType);
                        return false;
                    }
                }
                
                function getItemCount(itemType) {
                    try {
                        var material = org.bukkit.Material.valueOf(itemType.toUpperCase());
                        var count = 0;
                        var contents = player.getInventory().getContents();
                        for (var i = 0; i < contents.length; i++) {
                            var item = contents[i];
                            if (item != null && item.getType() == material) {
                                count += item.getAmount();
                            }
                        }
                        return count;
                    } catch (e) {
                        log('Invalid item type: ' + itemType);
                        return 0;
                    }
                }
                """);
        } catch (final ScriptException e) {
            LOGGER.log(Level.WARNING, "Failed to setup script helpers", e);
        }
    }
    
    /**
     * Executes plugin-based logic (placeholder for future extension).
     *
     * @param player The player.
     * @param operation The operation type.
     * @return The result.
     */
    private boolean executePluginLogic(
        @NotNull final Player player,
        @NotNull final String operation
    ) {
        // Placeholder for future plugin integration
        LOGGER.log(Level.WARNING, "Plugin-based custom requirements not yet implemented");
        return false;
    }
    
    /**
     * Executes data-driven logic (placeholder for future extension).
     *
     * @param player The player.
     * @param operation The operation type.
     * @return The result.
     */
    private boolean executeDataLogic(
        @NotNull final Player player,
        @NotNull final String operation
    ) {
        // Placeholder for future data-driven logic
        LOGGER.log(Level.WARNING, "Data-driven custom requirements not yet implemented");
        return false;
    }
    
    /**
     * Executes plugin-based progress logic (placeholder).
     *
     * @param player The player.
     * @return The progress.
     */
    private double executePluginProgressLogic(
        @NotNull final Player player
    ) {
        return 0.0;
    }
    
    /**
     * Executes data-driven progress logic (placeholder).
     *
     * @param player The player.
     * @return The progress.
     */
    private double executeDataProgressLogic(
        @NotNull final Player player
    ) {
        return 0.0;
    }
    
    /**
     * Executes plugin-based consume logic (placeholder).
     *
     * @param player The player.
     */
    private void executePluginConsumeLogic(
        @NotNull final Player player
    ) {
        // Placeholder
    }
    
    /**
     * Executes data-driven consume logic (placeholder).
     *
     * @param player The player.
     */
    private void executeDataConsumeLogic(
        @NotNull final Player player
    ) {
        // Placeholder
    }
}