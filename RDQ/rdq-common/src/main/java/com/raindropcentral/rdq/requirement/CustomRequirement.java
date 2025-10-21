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
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Flexible requirement that allows server owners to define custom logic.
 * <p>
 * The {@code CustomRequirement} provides a way for server owners to create their own
 * requirement logic without modifying the core plugin. It supports JavaScript scripting
 * for custom logic and custom data storage for configuration.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.1
 * @since 1.0.0
 */
public final class CustomRequirement extends AbstractRequirement {

    private static final Logger LOGGER = Logger.getLogger(CustomRequirement.class.getName());

    public enum CustomType {
        SCRIPT,
        PLUGIN,
        DATA
    }

    @JsonProperty("customType")
    private final CustomType customType;

    @JsonProperty("customScript")
    private final String customScript;

    @JsonProperty("progressScript")
    private final String progressScript;

    @JsonProperty("consumeScript")
    private final String consumeScript;

    @JsonProperty("customData")
    private final Map<String, Object> customData;

    @JsonProperty("description")
    private final String description;

    @JsonProperty("cacheScripts")
    private final boolean cacheScripts;

    @JsonIgnore
    private transient volatile ScriptEngine scriptEngine;

    @JsonIgnore
    private transient final ReadWriteLock scriptEngineLock = new ReentrantReadWriteLock();

    /**
     * Creates a script-based requirement using the supplied script content.
     *
     * @param customScript the JavaScript expression that determines whether the requirement is met
     */
    public CustomRequirement(final @NotNull String customScript) {
        this(CustomType.SCRIPT, customScript, null, null, new HashMap<>(), null, true);
    }

    /**
     * Creates a script-based requirement that can reference custom data.
     *
     * @param customScript the JavaScript expression that determines whether the requirement is met
     * @param customData   the key/value configuration exposed to the script engine
     */
    public CustomRequirement(
            final @NotNull String customScript,
            final @NotNull Map<String, Object> customData
    ) {
        this(CustomType.SCRIPT, customScript, null, null, customData, null, true);
    }

    /**
     * Creates a requirement with explicit configuration for scripting or plugin-based execution.
     *
     * @param customType     the execution strategy for the requirement
     * @param customScript   the script executed for {@link CustomType#SCRIPT} requirements
     * @param progressScript optional script that reports progress when using {@link CustomType#SCRIPT}
     * @param consumeScript  optional script that is executed when the requirement is consumed
     * @param customData     additional data exposed to scripts or external logic
     * @param description    human-readable explanation for the requirement used in UI elements
     * @param cacheScripts   whether script engines should be cached for reuse across evaluations
     */
    @JsonCreator
    public CustomRequirement(
            @JsonProperty("customType") final @Nullable CustomType customType,
            @JsonProperty("customScript") final @Nullable String customScript,
            @JsonProperty("progressScript") final @Nullable String progressScript,
            @JsonProperty("consumeScript") final @Nullable String consumeScript,
            @JsonProperty("customData") final @Nullable Map<String, Object> customData,
            @JsonProperty("description") final @Nullable String description,
            @JsonProperty("cacheScripts") final @Nullable Boolean cacheScripts
    ) {
        super(Type.CUSTOM);

        this.customType = customType != null ? customType : CustomType.SCRIPT;

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
     * Determines if the requirement has been satisfied for the supplied player.
     *
     * @param player the player being evaluated
     * @return {@code true} when the underlying strategy deems the requirement satisfied
     */
    @Override
    public boolean isMet(final @NotNull Player player) {
        return switch (this.customType) {
            case SCRIPT -> this.executeScript(player, this.customScript, Boolean.class, false);
            case PLUGIN -> this.executePluginLogic(player, "isMet");
            case DATA -> this.executeDataLogic(player, "isMet");
        };
    }

    /**
     * Calculates progress towards the requirement for the supplied player.
     *
     * @param player the player being evaluated
     * @return a value between {@code 0.0} and {@code 1.0} describing completion progress
     */
    @Override
    public double calculateProgress(final @NotNull Player player) {
        return switch (this.customType) {
            case SCRIPT -> {
                if (this.progressScript != null && !this.progressScript.trim().isEmpty()) {
                    yield this.executeScript(player, this.progressScript, Double.class, 0.0);
                } else {
                    yield this.isMet(player) ? 1.0 : 0.0;
                }
            }
            case PLUGIN -> this.executePluginProgressLogic(player);
            case DATA -> this.executeDataProgressLogic(player);
        };
    }

    /**
     * Consumes the requirement for the supplied player, triggering any configured side effects.
     *
     * @param player the player for which the requirement is being consumed
     */
    @Override
    public void consume(final @NotNull Player player) {
        switch (this.customType) {
            case SCRIPT -> {
                if (this.consumeScript != null && !this.consumeScript.trim().isEmpty()) {
                    this.executeScript(player, this.consumeScript, Void.class, null);
                }
            }
            case PLUGIN -> this.executePluginConsumeLogic(player);
            case DATA -> this.executeDataConsumeLogic(player);
        }
    }

    /**
     * Provides the translation key used to describe this requirement in resource bundles.
     *
     * @return the translation key for display purposes
     */
    @Override
    @NotNull
    public String getDescriptionKey() {
        return "requirement.custom";
    }

    /**
     * Retrieves the execution strategy backing the requirement.
     *
     * @return the selected {@link CustomType}
     */
    @NotNull
    public CustomType getCustomType() {
        return this.customType;
    }

    /**
     * Provides the script used when the requirement is configured for {@link CustomType#SCRIPT}.
     *
     * @return the script text or {@code null} when scripts are not used
     */
    @Nullable
    public String getCustomScript() {
        return this.customScript;
    }

    /**
     * Provides the script that reports progress for {@link CustomType#SCRIPT} requirements.
     *
     * @return the progress script or {@code null} when progress is derived implicitly
     */
    @Nullable
    public String getProgressScript() {
        return this.progressScript;
    }

    /**
     * Provides the script executed when the requirement is consumed.
     *
     * @return the consumption script or {@code null} when consumption logic is not scripted
     */
    @Nullable
    public String getConsumeScript() {
        return this.consumeScript;
    }

    /**
     * Supplies the custom data map made available to scripts and integrations.
     *
     * @return an unmodifiable view of the custom data
     */
    @NotNull
    public Map<String, Object> getCustomData() {
        return Collections.unmodifiableMap(this.customData);
    }

    /**
     * Fetches a typed value from the custom data map, supplying a default when the key is absent
     * or cannot be cast to the expected type.
     *
     * @param key          the data key to retrieve
     * @param defaultValue the fallback value returned when the key is missing or mismatched
     * @param <T>          the expected type of the value
     * @return the resolved value or {@code defaultValue} when unavailable
     */
    @JsonIgnore
    @SuppressWarnings("unchecked")
    public <T> T getCustomDataValue(
            final @NotNull String key,
            final @Nullable T defaultValue
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
     * Provides the description that elaborates on this requirement for display purposes.
     *
     * @return the optional description text
     */
    @Nullable
    public String getDescription() {
        return this.description;
    }

    /**
     * Indicates whether script engines should be cached instead of being created on every use.
     *
     * @return {@code true} when script engines are cached for reuse
     */
    public boolean isCacheScripts() {
        return this.cacheScripts;
    }

    /**
     * Determines whether this requirement relies on script execution.
     *
     * @return {@code true} if {@link CustomType#SCRIPT} is active
     */
    @JsonIgnore
    public boolean isScriptBased() {
        return this.customType == CustomType.SCRIPT;
    }

    /**
     * Determines whether this requirement delegates to plugin-provided logic.
     *
     * @return {@code true} if {@link CustomType#PLUGIN} is active
     */
    @JsonIgnore
    public boolean isPluginBased() {
        return this.customType == CustomType.PLUGIN;
    }

    /**
     * Determines whether this requirement is driven by data-centric logic.
     *
     * @return {@code true} if {@link CustomType#DATA} is active
     */
    @JsonIgnore
    public boolean isDataBased() {
        return this.customType == CustomType.DATA;
    }

    /**
     * Validates the configuration for the requirement, ensuring scripts and data are usable.
     */
    @JsonIgnore
    public void validate() {
        if (this.customType == CustomType.SCRIPT) {
            if (this.customScript == null || this.customScript.trim().isEmpty()) {
                throw new IllegalStateException("Custom script cannot be null or empty for SCRIPT type.");
            }
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

    @NotNull
    private ScriptEngine getScriptEngine() {
        if (!this.cacheScripts) {
            return this.createScriptEngine();
        }

        this.scriptEngineLock.readLock().lock();
        try {
            if (this.scriptEngine != null) {
                return this.scriptEngine;
            }
        } finally {
            this.scriptEngineLock.readLock().unlock();
        }

        this.scriptEngineLock.writeLock().lock();
        try {
            if (this.scriptEngine == null) {
                this.scriptEngine = this.createScriptEngine();
            }
            return this.scriptEngine;
        } finally {
            this.scriptEngineLock.writeLock().unlock();
        }
    }

    @NotNull
    private ScriptEngine createScriptEngine() {
        final ScriptEngineManager manager = new ScriptEngineManager();
        final ScriptEngine engine = manager.getEngineByName("JavaScript");
        if (engine == null) {
            throw new IllegalStateException("JavaScript engine not available");
        }
        return engine;
    }

    @SuppressWarnings("unchecked")
    private <T> T executeScript(
            final @NotNull Player player,
            final @NotNull String script,
            final @NotNull Class<T> expectedType,
            final @Nullable T defaultValue
    ) {
        try {
            final ScriptEngine engine = this.getScriptEngine();

            engine.put("player", player);
            engine.put("customData", this.customData);
            engine.put("requirement", this);

            this.setupScriptHelpers(engine, player);

            final Object result = engine.eval(script);
            if (result == null) {
                return defaultValue;
            }

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

    private void setupScriptHelpers(
            final @NotNull ScriptEngine engine,
            final @NotNull Player player
    ) {
        try {
            engine.put("Statistic", Statistic.class);
            engine.put("EntityType", EntityType.class);

            engine.eval("""
                function getCustomData(key, defaultValue) {
                    var value = customData.get(key);
                    return value != null ? value : defaultValue;
                }
                
                function hasCustomData(key) {
                    return customData.containsKey(key);
                }
                
                function log(message) {
                    java.util.logging.Logger.getLogger('CustomRequirement').info(message);
                }
                
                function getKillCount(mobType) {
                    try {
                        var entityType = EntityType.valueOf(mobType.toUpperCase());
                        return player.getStatistic(Statistic.KILL_ENTITY, entityType);
                    } catch (e) {
                        log('Invalid mob type: ' + mobType);
                        return 0;
                    }
                }
                
                function getPlayTime() {
                    return player.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20.0;
                }
                
                function hasPermission(permission) {
                    return player.hasPermission(permission);
                }
                
                function getLevel() {
                    return player.getLevel();
                }
                """);
        } catch (final ScriptException e) {
            LOGGER.log(Level.WARNING, "Failed to setup script helpers", e);
        }
    }

    private boolean executePluginLogic(final @NotNull Player player, final @NotNull String operation) {
        LOGGER.log(Level.WARNING, "Plugin-based custom requirements not yet implemented");
        return false;
    }

    private boolean executeDataLogic(final @NotNull Player player, final @NotNull String operation) {
        LOGGER.log(Level.WARNING, "Data-driven custom requirements not yet implemented");
        return false;
    }

    private double executePluginProgressLogic(final @NotNull Player player) {
        return 0.0;
    }

    private double executeDataProgressLogic(final @NotNull Player player) {
        return 0.0;
    }

    private void executePluginConsumeLogic(final @NotNull Player player) {
    }

    private void executeDataConsumeLogic(final @NotNull Player player) {
    }
}