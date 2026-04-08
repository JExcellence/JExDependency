package de.jexcellence.oneblock.config;

import lombok.Getter;
import lombok.Setter;

/**
 * Configuration for manager functionality.
 * 
 * @author JExcellence
 * @since 2.0.0
 * @version 2.0.0
 */
@Getter
@Setter
public class ManagerConfiguration {
    
    // Oneblock Manager Configuration
    private int maxConcurrentBreaks = 5;
    private double experienceMultiplier = 1.0;
    private boolean prestigeEnabled = true;
    private boolean autoEvolutionProgression = true;
    
    // Region Manager Configuration
    private boolean enableMultiDimensional = true;
    private double netherCoordinateRatio = 8.0;
    private boolean playerTrackingEnabled = true;
    private boolean statisticsEnabled = true;
    private int safeLocationSearchRadius = 10;
    
    // Portal Manager Configuration
    private boolean enablePortalValidation = true;
    private boolean optimalPlacementEnabled = true;
    private boolean portalLinkingEnabled = true;
    private boolean safePlatformCreation = true;
    private int portalSearchRadius = 10;
    private int platformSize = 5;
    
    // Performance Configuration
    private boolean enableCaching = true;
    private int cacheExpiryMinutes = 30;
    private boolean asyncOperations = true;
    private int threadPoolSize = 4;
    
    // Safety Configuration
    private boolean enableSafetyChecks = true;
    private boolean validateLocations = true;
    private boolean checkWorldBoundaries = true;
    private boolean preventOverlappingRegions = true;
    
    // Logging Configuration
    private boolean enableDebugLogging = false;
    private boolean logPerformanceMetrics = true;
    private boolean logPlayerActions = true;
    
    /**
     * Validates the configuration values.
     * 
     * @throws IllegalArgumentException if any configuration value is invalid
     */
    public void validate() {
        if (maxConcurrentBreaks <= 0) {
            throw new IllegalArgumentException("maxConcurrentBreaks must be positive");
        }
        
        if (experienceMultiplier < 0) {
            throw new IllegalArgumentException("experienceMultiplier cannot be negative");
        }
        
        if (netherCoordinateRatio <= 0) {
            throw new IllegalArgumentException("netherCoordinateRatio must be positive");
        }
        
        if (safeLocationSearchRadius <= 0) {
            throw new IllegalArgumentException("safeLocationSearchRadius must be positive");
        }
        
        if (portalSearchRadius <= 0) {
            throw new IllegalArgumentException("portalSearchRadius must be positive");
        }
        
        if (platformSize <= 0) {
            throw new IllegalArgumentException("platformSize must be positive");
        }
        
        if (cacheExpiryMinutes <= 0) {
            throw new IllegalArgumentException("cacheExpiryMinutes must be positive");
        }
        
        if (threadPoolSize <= 0) {
            throw new IllegalArgumentException("threadPoolSize must be positive");
        }
    }
    
    /**
     * Gets the nether coordinate scaling factor.
     * 
     * @return the scaling factor for nether coordinates
     */
    public double getNetherScalingFactor() {
        return 1.0 / netherCoordinateRatio;
    }
    
    /**
     * Gets the platform radius based on platform size.
     * 
     * @return the platform radius
     */
    public int getPlatformRadius() {
        return platformSize / 2;
    }
    
    /**
     * Checks if features are enabled.
     * 
     * @return true if most features are enabled
     */
    public boolean areFeaturesEnabled() {
        return enableMultiDimensional && 
               enablePortalValidation && 
               optimalPlacementEnabled && 
               enableCaching;
    }
    
    /**
     * Gets cache expiry time in milliseconds.
     * 
     * @return cache expiry time in milliseconds
     */
    public long getCacheExpiryMillis() {
        return cacheExpiryMinutes * 60L * 1000L;
    }
}