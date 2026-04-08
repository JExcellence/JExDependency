package de.jexcellence.oneblock.manager.calculation;

import de.jexcellence.oneblock.database.entity.oneblock.OneblockIsland;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for island level calculation managers.
 * Provides methods for calculating island levels based on block values and experience.
 * 
 * @author JExcellence
 * @since 2.0.0
 * @version 2.0.0
 */
public interface IIslandLevelCalculator {
    
    /**
     * Calculates the level for the specified island.
     * 
     * @param island the island to calculate level for
     * @return CompletableFuture with calculation result
     */
    @NotNull
    CompletableFuture<CalculationResult> calculateLevel(@NotNull OneblockIsland island);
    
    /**
     * Calculates the level for the specified island with progress reporting.
     * 
     * @param island the island to calculate level for
     * @param progressCallback callback for progress updates (0-100)
     * @return CompletableFuture with calculation result
     */
    @NotNull
    CompletableFuture<CalculationResult> calculateLevel(@NotNull OneblockIsland island, 
                                                       @NotNull ProgressCallback progressCallback);
    
    /**
     * Gets the current block counts for an island without full recalculation.
     * 
     * @param island the island to get block counts for
     * @return CompletableFuture with block counts
     */
    @NotNull
    CompletableFuture<Map<Material, Long>> getBlockCounts(@NotNull OneblockIsland island);
    
    /**
     * Calculates experience from block counts and values.
     * 
     * @param blockCounts the block counts
     * @return the calculated experience
     */
    double calculateExperience(@NotNull Map<Material, Long> blockCounts);
    
    /**
     * Calculates level from experience using the progression formula.
     * 
     * @param experience the experience amount
     * @return the calculated level
     */
    int calculateLevelFromExperience(double experience);
    
    /**
     * Gets performance metrics for the calculator.
     * 
     * @return performance metrics
     */
    @NotNull
    PerformanceMetrics getPerformanceMetrics();
    
    /**
     * Callback interface for progress reporting.
     */
    @FunctionalInterface
    interface ProgressCallback {
        /**
         * Called when calculation progress updates.
         * 
         * @param percentage the completion percentage (0-100)
         * @param message optional progress message
         */
        void onProgress(int percentage, @NotNull String message);
    }
    
    /**
     * Calculation result containing level, experience, and block counts.
     */
    class CalculationResult {
        private final int level;
        private final double experience;
        private final Map<Material, Long> blockCounts;
        private final long calculationTimeMs;
        private final long totalBlocks;
        
        public CalculationResult(int level, double experience, @NotNull Map<Material, Long> blockCounts, 
                               long calculationTimeMs) {
            this.level = level;
            this.experience = experience;
            this.blockCounts = Map.copyOf(blockCounts);
            this.calculationTimeMs = calculationTimeMs;
            this.totalBlocks = blockCounts.values().stream().mapToLong(Long::longValue).sum();
        }
        
        public int getLevel() { return level; }
        public double getExperience() { return experience; }
        @NotNull public Map<Material, Long> getBlockCounts() { return blockCounts; }
        public long getCalculationTimeMs() { return calculationTimeMs; }
        public long getTotalBlocks() { return totalBlocks; }
        
        public int getUniqueBlockTypes() {
            return blockCounts.size();
        }
        
        public double getExperiencePerBlock() {
            return totalBlocks > 0 ? experience / totalBlocks : 0.0;
        }
        
        @Override
        public String toString() {
            return String.format("CalculationResult{level=%d, experience=%.2f, blocks=%d, types=%d, time=%dms}",
                level, experience, totalBlocks, getUniqueBlockTypes(), calculationTimeMs);
        }
    }
    
    /**
     * Performance metrics for monitoring calculation performance.
     */
    class PerformanceMetrics {
        private final long totalCalculations;
        private final long totalCalculationTime;
        private final long averageCalculationTime;
        private final long lastCalculationTime;
        private final int activeCalculations;
        
        public PerformanceMetrics(long totalCalculations, long totalCalculationTime, 
                                long lastCalculationTime, int activeCalculations) {
            this.totalCalculations = totalCalculations;
            this.totalCalculationTime = totalCalculationTime;
            this.averageCalculationTime = totalCalculations > 0 ? totalCalculationTime / totalCalculations : 0;
            this.lastCalculationTime = lastCalculationTime;
            this.activeCalculations = activeCalculations;
        }
        
        public long getTotalCalculations() { return totalCalculations; }
        public long getTotalCalculationTime() { return totalCalculationTime; }
        public long getAverageCalculationTime() { return averageCalculationTime; }
        public long getLastCalculationTime() { return lastCalculationTime; }
        public int getActiveCalculations() { return activeCalculations; }
        
        @Override
        public String toString() {
            return String.format("PerformanceMetrics{total=%d, avgTime=%dms, lastTime=%dms, active=%d}",
                totalCalculations, averageCalculationTime, lastCalculationTime, activeCalculations);
        }
    }
}