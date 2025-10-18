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
 * @version 1.1.0
 * @since TBD
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

    public CustomRequirement(final @NotNull String customScript) {
        this(CustomType.SCRIPT, customScript, null, null, new HashMap<>(), null, true);
    }

    public CustomRequirement(
            final @NotNull String customScript,
            final @NotNull Map<String, Object> customData
    ) {
        this(CustomType.SCRIPT, customScript, null, null, customData, null, true);
    }

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

    @Override
    public boolean isMet(final @NotNull Player player) {
        return switch (this.customType) {
            case SCRIPT -> this.executeScript(player, this.customScript, Boolean.class, false);
            case PLUGIN -> this.executePluginLogic(player, "isMet");
            case DATA -> this.executeDataLogic(player, "isMet");
        };
    }

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

    @Override
    @NotNull
    public String getDescriptionKey() {
        return "requirement.custom";
    }

    @NotNull
    public CustomType getCustomType() {
        return this.customType;
    }

    @Nullable
    public String getCustomScript() {
        return this.customScript;
    }

    @Nullable
    public String getProgressScript() {
        return this.progressScript;
    }

    @Nullable
    public String getConsumeScript() {
        return this.consumeScript;
    }

    @NotNull
    public Map<String, Object> getCustomData() {
        return Collections.unmodifiableMap(this.customData);
    }

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

    @Nullable
    public String getDescription() {
        return this.description;
    }

    public boolean isCacheScripts() {
        return this.cacheScripts;
    }

    @JsonIgnore
    public boolean isScriptBased() {
        return this.customType == CustomType.SCRIPT;
    }

    @JsonIgnore
    public boolean isPluginBased() {
        return this.customType == CustomType.PLUGIN;
    }

    @JsonIgnore
    public boolean isDataBased() {
        return this.customType == CustomType.DATA;
    }

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