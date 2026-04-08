package de.jexcellence.oneblock.config;

import org.jetbrains.annotations.NotNull;

/**
 * Enhanced configuration for OneBlock managers with additional features.
 * Extends the base ManagerConfiguration with advanced settings.
 * 
 * @author JExcellence
 * @since 2.0.0
 * @version 2.0.0
 */
public class EnhancedManagerConfiguration extends ManagerConfiguration {
    
    // Enhanced OneBlock settings
    private boolean advancedEvolutionEnabled = true;
    private double experienceBoostMultiplier = 1.5;
    private int maxPrestigeLevel = 10;
    private boolean autoEvolutionEnabled = false;
    private int evolutionCheckInterval = 300; // seconds
    
    // Enhanced region settings
    private boolean dynamicRegionSizing = true;
    private int maxRegionSize = 500;
    private boolean crossDimensionalRegions = false;
    private boolean regionBorderVisualization = true;
    
    // Enhanced portal settings
    private boolean advancedPortalLinking = true;
    private boolean portalNetworkEnabled = false;
    private int maxPortalsPerIsland = 5;
    private boolean portalCooldownEnabled = true;
    private int portalCooldownSeconds = 30;
    
    // Enhanced performance settings
    private boolean intelligentCaching = true;
    private int cachePreloadRadius = 3;
    private boolean asyncRegionOperations = true;
    private int maxConcurrentOperations = 10;
    private boolean memoryOptimization = true;
    
    // Enhanced safety settings
    private boolean advancedSafetyChecks = true;
    private boolean preventDataCorruption = true;
    private boolean backupBeforeOperations = false;
    private int maxRetryAttempts = 3;
    private boolean rollbackOnFailure = true;
    
    // Getters and setters
    
    public boolean isAdvancedEvolutionEnabled() { return advancedEvolutionEnabled; }
    public void setAdvancedEvolutionEnabled(boolean advancedEvolutionEnabled) { this.advancedEvolutionEnabled = advancedEvolutionEnabled; }
    
    public double getExperienceBoostMultiplier() { return experienceBoostMultiplier; }
    public void setExperienceBoostMultiplier(double experienceBoostMultiplier) { this.experienceBoostMultiplier = experienceBoostMultiplier; }
    
    public int getMaxPrestigeLevel() { return maxPrestigeLevel; }
    public void setMaxPrestigeLevel(int maxPrestigeLevel) { this.maxPrestigeLevel = maxPrestigeLevel; }
    
    public boolean isAutoEvolutionEnabled() { return autoEvolutionEnabled; }
    public void setAutoEvolutionEnabled(boolean autoEvolutionEnabled) { this.autoEvolutionEnabled = autoEvolutionEnabled; }
    
    public int getEvolutionCheckInterval() { return evolutionCheckInterval; }
    public void setEvolutionCheckInterval(int evolutionCheckInterval) { this.evolutionCheckInterval = evolutionCheckInterval; }
    
    public boolean isDynamicRegionSizing() { return dynamicRegionSizing; }
    public void setDynamicRegionSizing(boolean dynamicRegionSizing) { this.dynamicRegionSizing = dynamicRegionSizing; }
    
    public int getMaxRegionSize() { return maxRegionSize; }
    public void setMaxRegionSize(int maxRegionSize) { this.maxRegionSize = maxRegionSize; }
    
    public boolean isCrossDimensionalRegions() { return crossDimensionalRegions; }
    public void setCrossDimensionalRegions(boolean crossDimensionalRegions) { this.crossDimensionalRegions = crossDimensionalRegions; }
    
    public boolean isRegionBorderVisualization() { return regionBorderVisualization; }
    public void setRegionBorderVisualization(boolean regionBorderVisualization) { this.regionBorderVisualization = regionBorderVisualization; }
    
    public boolean isAdvancedPortalLinking() { return advancedPortalLinking; }
    public void setAdvancedPortalLinking(boolean advancedPortalLinking) { this.advancedPortalLinking = advancedPortalLinking; }
    
    public boolean isPortalNetworkEnabled() { return portalNetworkEnabled; }
    public void setPortalNetworkEnabled(boolean portalNetworkEnabled) { this.portalNetworkEnabled = portalNetworkEnabled; }
    
    public int getMaxPortalsPerIsland() { return maxPortalsPerIsland; }
    public void setMaxPortalsPerIsland(int maxPortalsPerIsland) { this.maxPortalsPerIsland = maxPortalsPerIsland; }
    
    public boolean isPortalCooldownEnabled() { return portalCooldownEnabled; }
    public void setPortalCooldownEnabled(boolean portalCooldownEnabled) { this.portalCooldownEnabled = portalCooldownEnabled; }
    
    public int getPortalCooldownSeconds() { return portalCooldownSeconds; }
    public void setPortalCooldownSeconds(int portalCooldownSeconds) { this.portalCooldownSeconds = portalCooldownSeconds; }
    
    public boolean isIntelligentCaching() { return intelligentCaching; }
    public void setIntelligentCaching(boolean intelligentCaching) { this.intelligentCaching = intelligentCaching; }
    
    public int getCachePreloadRadius() { return cachePreloadRadius; }
    public void setCachePreloadRadius(int cachePreloadRadius) { this.cachePreloadRadius = cachePreloadRadius; }
    
    public boolean isAsyncRegionOperations() { return asyncRegionOperations; }
    public void setAsyncRegionOperations(boolean asyncRegionOperations) { this.asyncRegionOperations = asyncRegionOperations; }
    
    public int getMaxConcurrentOperations() { return maxConcurrentOperations; }
    public void setMaxConcurrentOperations(int maxConcurrentOperations) { this.maxConcurrentOperations = maxConcurrentOperations; }
    
    public boolean isMemoryOptimization() { return memoryOptimization; }
    public void setMemoryOptimization(boolean memoryOptimization) { this.memoryOptimization = memoryOptimization; }
    
    public boolean isAdvancedSafetyChecks() { return advancedSafetyChecks; }
    public void setAdvancedSafetyChecks(boolean advancedSafetyChecks) { this.advancedSafetyChecks = advancedSafetyChecks; }
    
    public boolean isPreventDataCorruption() { return preventDataCorruption; }
    public void setPreventDataCorruption(boolean preventDataCorruption) { this.preventDataCorruption = preventDataCorruption; }
    
    public boolean isBackupBeforeOperations() { return backupBeforeOperations; }
    public void setBackupBeforeOperations(boolean backupBeforeOperations) { this.backupBeforeOperations = backupBeforeOperations; }
    
    public int getMaxRetryAttempts() { return maxRetryAttempts; }
    public void setMaxRetryAttempts(int maxRetryAttempts) { this.maxRetryAttempts = maxRetryAttempts; }
    
    public boolean isRollbackOnFailure() { return rollbackOnFailure; }
    public void setRollbackOnFailure(boolean rollbackOnFailure) { this.rollbackOnFailure = rollbackOnFailure; }
    
    /**
     * Validates the enhanced configuration settings.
     * 
     * @return true if configuration is valid
     */
    public boolean validateEnhancedSettings() {
        if (experienceBoostMultiplier <= 0) return false;
        if (maxPrestigeLevel < 0) return false;
        if (evolutionCheckInterval < 60) return false; // Minimum 1 minute
        if (maxRegionSize < 50) return false; // Minimum region size
        if (maxPortalsPerIsland < 1) return false;
        if (portalCooldownSeconds < 0) return false;
        if (cachePreloadRadius < 0) return false;
        if (maxConcurrentOperations < 1) return false;
        if (maxRetryAttempts < 0) return false;
        
        return true;
    }
    
    /**
     * Gets a summary of enhanced settings.
     * 
     * @return configuration summary
     */
    @NotNull
    public String getEnhancedSettingsSummary() {
        return String.format(
            "EnhancedManagerConfiguration{" +
            "advancedEvolution=%s, " +
            "experienceBoost=%.1fx, " +
            "maxPrestige=%d, " +
            "autoEvolution=%s, " +
            "dynamicRegions=%s, " +
            "maxRegionSize=%d, " +
            "advancedPortals=%s, " +
            "maxPortals=%d, " +
            "intelligentCaching=%s, " +
            "asyncOperations=%s, " +
            "advancedSafety=%s" +
            "}",
            advancedEvolutionEnabled,
            experienceBoostMultiplier,
            maxPrestigeLevel,
            autoEvolutionEnabled,
            dynamicRegionSizing,
            maxRegionSize,
            advancedPortalLinking,
            maxPortalsPerIsland,
            intelligentCaching,
            asyncRegionOperations,
            advancedSafetyChecks
        );
    }
}
