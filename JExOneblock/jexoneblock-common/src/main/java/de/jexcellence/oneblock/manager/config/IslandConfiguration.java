package de.jexcellence.oneblock.manager.config;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for island management settings.
 * Demonstrates type-safe configuration sections with validation.
 * 
 * @author JExcellence
 * @since 2.0.0
 * @version 2.0.0
 */
@CSAlways
public class IslandConfiguration extends AConfigSection {
    
    private Integer defaultSize;
    private Integer maxSize;
    private Integer spaceBetweenIslands;
    private Boolean enableNether;
    private Boolean enableEnd;
    private String defaultBiome;
    private Integer maxIslandsPerPlayer;
    private Boolean allowIslandReset;
    private String resetCooldown;
    private Map<String, Object> customSettings;
    
    public IslandConfiguration(EvaluationEnvironmentBuilder baseEnvironment) {
        super(baseEnvironment);
    }
    
    public int getDefaultSize() {
        return defaultSize == null ? 50 : defaultSize;
    }
    
    public int getMaxSize() {
        return maxSize == null ? 200 : maxSize;
    }
    
    public int getSpaceBetweenIslands() {
        return spaceBetweenIslands == null ? 100 : spaceBetweenIslands;
    }
    
    public boolean isEnableNether() {
        return enableNether == null || enableNether;
    }
    
    public boolean isEnableEnd() {
        return enableEnd == null || enableEnd;
    }
    
    public String getDefaultBiome() {
        return defaultBiome == null ? "PLAINS" : defaultBiome;
    }
    
    public int getMaxIslandsPerPlayer() {
        return maxIslandsPerPlayer == null ? 1 : maxIslandsPerPlayer;
    }
    
    public boolean isAllowIslandReset() {
        return allowIslandReset == null || allowIslandReset;
    }
    
    public Duration getResetCooldown() {
        return resetCooldown == null ? Duration.ofHours(24) : parseDuration(resetCooldown);
    }
    
    public Map<String, Object> getCustomSettings() {
        return customSettings == null ? new HashMap<>() : customSettings;
    }
    
    /**
     * Gets a custom setting value.
     * 
     * @param key the setting key
     * @param defaultValue the default value if not found
     * @param <T> the value type
     * @return the setting value or default
     */
    @SuppressWarnings("unchecked")
    public <T> T getCustomSetting(String key, T defaultValue) {
        Object value = getCustomSettings().get(key);
        if (value != null && defaultValue.getClass().isInstance(value)) {
            return (T) value;
        }
        return defaultValue;
    }
    
    /**
     * Sets a custom setting value.
     * 
     * @param key the setting key
     * @param value the setting value
     */
    public void setCustomSetting(String key, Object value) {
        if (customSettings == null) {
            customSettings = new HashMap<>();
        }
        customSettings.put(key, value);
    }
    
    /**
     * Parses a duration string (e.g., "24h", "30m", "45s").
     * 
     * @param durationStr the duration string
     * @return the parsed duration
     */
    private Duration parseDuration(String durationStr) {
        try {
            if (durationStr.endsWith("h")) {
                long hours = Long.parseLong(durationStr.substring(0, durationStr.length() - 1));
                return Duration.ofHours(hours);
            } else if (durationStr.endsWith("m")) {
                long minutes = Long.parseLong(durationStr.substring(0, durationStr.length() - 1));
                return Duration.ofMinutes(minutes);
            } else if (durationStr.endsWith("s")) {
                long seconds = Long.parseLong(durationStr.substring(0, durationStr.length() - 1));
                return Duration.ofSeconds(seconds);
            } else {
                // Assume seconds if no unit specified
                long seconds = Long.parseLong(durationStr);
                return Duration.ofSeconds(seconds);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid duration format: " + durationStr, e);
        }
    }
}