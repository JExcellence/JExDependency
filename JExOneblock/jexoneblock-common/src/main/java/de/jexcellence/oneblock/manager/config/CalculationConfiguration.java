package de.jexcellence.oneblock.manager.config;

import de.jexcellence.configmapper.sections.AConfigSection;
import de.jexcellence.configmapper.sections.CSAlways;
import de.jexcellence.gpeee.interpreter.EvaluationEnvironmentBuilder;
import org.bukkit.Material;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for island level calculation settings.
 * Demonstrates complex configuration with nested sections and validation.
 * 
 * @author JExcellence
 * @since 2.0.0
 * @version 2.0.0
 */
@CSAlways
public class CalculationConfiguration extends AConfigSection {
    
    private Boolean useParallelProcessing;
    private Integer batchSize;
    private Integer maxConcurrentCalculations;
    private String cacheExpiration;
    private String calculationTimeout;
    private Map<String, Double> blockValues;
    private Boolean enableProgressReporting;
    private Integer progressReportInterval;
    private Boolean enablePerformanceMetrics;
    
    public CalculationConfiguration(EvaluationEnvironmentBuilder baseEnvironment) {
        super(baseEnvironment);
    }
    
    public boolean isUseParallelProcessing() {
        return useParallelProcessing == null || useParallelProcessing;
    }
    
    public int getBatchSize() {
        return batchSize == null ? 1000 : batchSize;
    }
    
    public int getMaxConcurrentCalculations() {
        return maxConcurrentCalculations == null ? 2 : maxConcurrentCalculations;
    }
    
    public Duration getCacheExpiration() {
        return cacheExpiration == null ? Duration.ofMinutes(5) : parseDuration(cacheExpiration);
    }
    
    public Duration getCalculationTimeout() {
        return calculationTimeout == null ? Duration.ofMinutes(10) : parseDuration(calculationTimeout);
    }
    
    public Map<String, Double> getBlockValues() {
        return blockValues == null ? new HashMap<>() : blockValues;
    }
    
    public boolean isEnableProgressReporting() {
        return enableProgressReporting == null || enableProgressReporting;
    }
    
    public int getProgressReportInterval() {
        return progressReportInterval == null ? 10 : progressReportInterval;
    }
    
    public boolean isEnablePerformanceMetrics() {
        return enablePerformanceMetrics == null || enablePerformanceMetrics;
    }
    
    /**
     * Gets the value for a specific block material.
     * 
     * @param material the material
     * @param defaultValue the default value if not configured
     * @return the block value
     */
    public double getBlockValue(Material material, double defaultValue) {
        Map<String, Double> values = getBlockValues();
        return values.getOrDefault(material.name(), defaultValue);
    }
    
    /**
     * Sets the value for a specific block material.
     * 
     * @param material the material
     * @param value the block value
     */
    public void setBlockValue(Material material, double value) {
        if (value < 0) {
            throw new IllegalArgumentException("Block value cannot be negative");
        }
        if (blockValues == null) {
            blockValues = new HashMap<>();
        }
        blockValues.put(material.name(), value);
    }
    
    /**
     * Checks if a material has a configured value.
     * 
     * @param material the material
     * @return true if material has a configured value
     */
    public boolean hasBlockValue(Material material) {
        return getBlockValues().containsKey(material.name());
    }
    
    /**
     * Gets the number of configured block values.
     * 
     * @return the number of configured block values
     */
    public int getBlockValueCount() {
        return getBlockValues().size();
    }
    
    /**
     * Parses a duration string (e.g., "5m", "10s", "1h").
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
    
    /**
     * Validates the configuration settings.
     * 
     * @throws IllegalArgumentException if configuration is invalid
     */
    public void validate() {
        if (getBatchSize() <= 0) {
            throw new IllegalArgumentException("Batch size must be positive");
        }
        if (getMaxConcurrentCalculations() <= 0) {
            throw new IllegalArgumentException("Max concurrent calculations must be positive");
        }
    }
}