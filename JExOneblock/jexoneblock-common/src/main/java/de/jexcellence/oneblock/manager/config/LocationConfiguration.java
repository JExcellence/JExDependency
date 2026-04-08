package de.jexcellence.oneblock.manager.config;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for location management settings.
 * Handles island placement, spacing, and location finding algorithms.
 * 
 * @author JExcellence
 * @since 2.0.0
 * @version 2.0.0
 */
@CSAlways
public class LocationConfiguration extends AConfigSection {
    
    private Integer minimumIslandDistance;
    private Integer maximumSearchRadius;
    private Integer spiralStepSize;
    private Boolean enableLocationCaching;
    private String locationCacheExpiration;
    private Integer maxConcurrentLocationSearches;
    private Boolean enableSafetyChecks;
    private Integer safetyCheckRadius;
    private String locationReservationTimeout;
    private Boolean enableLocationStatistics;
    private Map<String, Object> worldSpecificSettings;
    
    public LocationConfiguration(EvaluationEnvironmentBuilder baseEnvironment) {
        super(baseEnvironment);
    }
    
    public int getMinimumIslandDistance() {
        return minimumIslandDistance == null ? 100 : minimumIslandDistance;
    }
    
    public int getMaximumSearchRadius() {
        return maximumSearchRadius == null ? 10000 : maximumSearchRadius;
    }
    
    public int getSpiralStepSize() {
        return spiralStepSize == null ? 50 : spiralStepSize;
    }
    
    public boolean isEnableLocationCaching() {
        return enableLocationCaching == null || enableLocationCaching;
    }
    
    public Duration getLocationCacheExpiration() {
        return locationCacheExpiration == null ? Duration.ofMinutes(10) : parseDuration(locationCacheExpiration);
    }
    
    public int getMaxConcurrentLocationSearches() {
        return maxConcurrentLocationSearches == null ? 3 : maxConcurrentLocationSearches;
    }
    
    public boolean isEnableSafetyChecks() {
        return enableSafetyChecks == null || enableSafetyChecks;
    }
    
    public int getSafetyCheckRadius() {
        return safetyCheckRadius == null ? 5 : safetyCheckRadius;
    }
    
    public Duration getLocationReservationTimeout() {
        return locationReservationTimeout == null ? Duration.ofMinutes(5) : parseDuration(locationReservationTimeout);
    }
    
    public boolean isEnableLocationStatistics() {
        return enableLocationStatistics == null || enableLocationStatistics;
    }
    
    public Map<String, Object> getWorldSpecificSettings() {
        return worldSpecificSettings == null ? new HashMap<>() : worldSpecificSettings;
    }
    
    /**
     * Gets a world-specific setting value.
     * 
     * @param worldName the world name
     * @param key the setting key
     * @param defaultValue the default value if not found
     * @param <T> the value type
     * @return the setting value or default
     */
    @SuppressWarnings("unchecked")
    public <T> T getWorldSetting(String worldName, String key, T defaultValue) {
        Object worldSettings = getWorldSpecificSettings().get(worldName);
        if (worldSettings instanceof Map) {
            Map<String, Object> settings = (Map<String, Object>) worldSettings;
            Object value = settings.get(key);
            if (value != null && defaultValue.getClass().isInstance(value)) {
                return (T) value;
            }
        }
        return defaultValue;
    }
    
    /**
     * Gets the minimum island distance for a specific world.
     * 
     * @param worldName the world name
     * @return the minimum distance for the world
     */
    public int getMinimumIslandDistance(String worldName) {
        return getWorldSetting(worldName, "minimum-island-distance", getMinimumIslandDistance());
    }
    
    /**
     * Gets the spiral step size for a specific world.
     * 
     * @param worldName the world name
     * @return the spiral step size for the world
     */
    public int getSpiralStepSize(String worldName) {
        return getWorldSetting(worldName, "spiral-step-size", getSpiralStepSize());
    }
    
    /**
     * Sets the minimum island distance.
     * 
     * @param distance the minimum distance
     */
    public void setMinimumIslandDistance(int distance) {
        this.minimumIslandDistance = distance;
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