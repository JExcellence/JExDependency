package de.jexcellence.oneblock.utility.workload.level;

import de.jexcellence.oneblock.database.entity.oneblock.OneblockIsland;
import de.jexcellence.oneblock.utility.workload.IWorkload;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;

/**
 * High-performance block calculator workload with advanced algorithms and smart caching
 */
public class BlockCalculatorWorkload implements IWorkload {
    
    private final ConcurrentHashMap<Material, LongAdder> materialCounts;
    private final Map<String, Double> blockExperienceMap;
    private final OneblockIsland island;
    private final Consumer<CalculationResult> resultCallback;
    private final boolean useParallelProcessing;
    
    // Performance optimization constants
    private static final int PARALLEL_THRESHOLD = 1000;
    private static final double EXPERIENCE_MULTIPLIER = 100.0;
    
    public BlockCalculatorWorkload(
        final @NotNull ConcurrentHashMap<Block, Integer> blocks,
        final @NotNull Map<String, Double> blockExperienceMap,
        final @NotNull OneblockIsland island
    ) {
        this(blocks, blockExperienceMap, island, null, true);
    }
    
    public BlockCalculatorWorkload(
        final @NotNull ConcurrentHashMap<Block, Integer> blocks,
        final @NotNull Map<String, Double> blockExperienceMap,
        final @NotNull OneblockIsland island,
        final Consumer<CalculationResult> resultCallback,
        final boolean useParallelProcessing
    ) {
        this.materialCounts = convertBlocksToMaterials(blocks);
        this.blockExperienceMap = blockExperienceMap;
        this.island = island;
        this.resultCallback = resultCallback;
        this.useParallelProcessing = useParallelProcessing;
    }
    
    /**
     * Convert blocks to materials with LongAdder for better concurrent performance
     */
    private ConcurrentHashMap<Material, LongAdder> convertBlocksToMaterials(
        final @NotNull ConcurrentHashMap<Block, Integer> blocks
    ) {
        final ConcurrentHashMap<Material, LongAdder> materials = new ConcurrentHashMap<>();
        
        if (useParallelProcessing && blocks.size() > PARALLEL_THRESHOLD) {
            blocks.entrySet().parallelStream()
                .filter(entry -> isValidBlock(entry.getKey()))
                .forEach(entry -> {
                    final Material material = entry.getKey().getType();
                    final int count = entry.getValue();
                    materials.computeIfAbsent(material, k -> new LongAdder()).add(count);
                });
        } else {
            blocks.entrySet().stream()
                .filter(entry -> isValidBlock(entry.getKey()))
                .forEach(entry -> {
                    final Material material = entry.getKey().getType();
                    final int count = entry.getValue();
                    materials.computeIfAbsent(material, k -> new LongAdder()).add(count);
                });
        }
        
        return materials;
    }
    
    @Override
    public void compute() {
        try {
            final long startTime = System.nanoTime();
            final AtomicReference<Double> totalExperience = new AtomicReference<>(0.0);
            final AtomicReference<Long> totalBlocks = new AtomicReference<>(0L);
            
            // Calculate experience using optimized processing
            if (useParallelProcessing && materialCounts.size() > PARALLEL_THRESHOLD) {
                calculateParallel(totalExperience, totalBlocks);
            } else {
                calculateSequential(totalExperience, totalBlocks);
            }
            
            final double finalExperience = totalExperience.get();
            final long finalBlockCount = totalBlocks.get();
            
            // Calculate level using advanced algorithm
            final int calculatedLevel = calculateLevelAdvanced(finalExperience);
            
            // Update island data
            island.addExperience(finalExperience - island.getCurrentExperience());
            island.setCurrentLevel(calculatedLevel);
            
            final long endTime = System.nanoTime();
            final long processingTime = (endTime - startTime) / 1_000_000; // Convert to milliseconds
            
            // Create enhanced result object
            final CalculationResult result = new CalculationResult(
                finalExperience,
                calculatedLevel,
                finalBlockCount,
                materialCounts.size(),
                processingTime,
                convertToRegularMap(),
                calculateRarityDistribution()
            );
            
            // Call result callback if provided
            if (resultCallback != null) {
                resultCallback.accept(result);
            }
            
        } catch (final Exception e) {
            // Log error but don't throw to prevent workload system from stopping
            System.err.println("Error calculating island level: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Parallel calculation for large datasets
     */
    private void calculateParallel(
        final AtomicReference<Double> totalExperience,
        final AtomicReference<Long> totalBlocks
    ) {
        materialCounts.entrySet().parallelStream()
            .forEach(entry -> {
                final Material material = entry.getKey();
                final long amount = entry.getValue().sum();
                final double blockExperience = getBlockExperience(material);
                
                totalExperience.updateAndGet(value -> value + (amount * blockExperience));
                totalBlocks.updateAndGet(value -> value + amount);
            });
    }
    
    /**
     * Sequential calculation for smaller datasets
     */
    private void calculateSequential(
        final AtomicReference<Double> totalExperience,
        final AtomicReference<Long> totalBlocks
    ) {
        materialCounts.forEach((material, adder) -> {
            final long amount = adder.sum();
            final double blockExperience = getBlockExperience(material);
            
            totalExperience.updateAndGet(value -> value + (amount * blockExperience));
            totalBlocks.updateAndGet(value -> value + amount);
        });
    }
    
    /**
     * Advanced level calculation with multiple progression curves
     */
    private int calculateLevelAdvanced(final double experience) {
        if (experience <= 0) {
            return 1;
        }
        
        // Multi-tier progression system for better gameplay balance
        if (experience < 1000) {
            // Early game: Linear progression (levels 1-10)
            return Math.max(1, (int) (experience / 100)) + 1;
        } else if (experience < 10000) {
            // Early-mid game: Slight curve (levels 11-25)
            return 10 + (int) Math.sqrt((experience - 1000) / 60);
        } else if (experience < 100000) {
            // Mid game: Square root progression (levels 26-65)
            return 25 + (int) Math.sqrt((experience - 10000) / 150);
        } else if (experience < 1000000) {
            // Late game: Logarithmic progression (levels 66-90)
            return 65 + (int) (Math.log10((experience - 100000) / 1000) * 8);
        } else {
            // End game: Very slow progression (levels 91-100)
            return Math.min(100, 90 + (int) (Math.log((experience - 1000000) / 100000) / Math.log(2)));
        }
    }
    
    /**
     * Get block experience with smart caching and fallback values
     */
    private double getBlockExperience(final @NotNull Material material) {
        Double experience = blockExperienceMap.get(material.name());
        if (experience != null) {
            return experience;
        }
        
        // Fallback values based on material properties
        return calculateFallbackExperience(material);
    }
    
    /**
     * Calculate fallback experience values based on material properties
     */
    private double calculateFallbackExperience(final @NotNull Material material) {
        // Ores get higher values
        if (material.name().contains("_ORE")) {
            if (material.name().contains("DIAMOND")) return 50.0;
            if (material.name().contains("EMERALD")) return 45.0;
            if (material.name().contains("GOLD")) return 25.0;
            if (material.name().contains("IRON")) return 15.0;
            if (material.name().contains("COAL")) return 5.0;
            return 10.0; // Other ores
        }
        
        // Precious blocks
        if (material.name().contains("DIAMOND")) return 100.0;
        if (material.name().contains("EMERALD")) return 90.0;
        if (material.name().contains("GOLD")) return 50.0;
        if (material.name().contains("IRON")) return 25.0;
        
        // Building blocks get moderate values
        if (material.isSolid() && material.isBlock()) {
            return 1.0;
        }
        
        return 0.1; // Default minimal value
    }
    
    /**
     * Calculate rarity distribution for analytics
     */
    private Map<String, Long> calculateRarityDistribution() {
        Map<String, Long> distribution = new ConcurrentHashMap<>();
        
        materialCounts.forEach((material, adder) -> {
            String rarity = determineBlockRarity(material);
            distribution.merge(rarity, adder.sum(), Long::sum);
        });
        
        return distribution;
    }
    
    /**
     * Determine block rarity for analytics
     */
    private String determineBlockRarity(final @NotNull Material material) {
        double experience = getBlockExperience(material);
        
        if (experience >= 100) return "LEGENDARY";
        if (experience >= 50) return "EPIC";
        if (experience >= 25) return "RARE";
        if (experience >= 10) return "UNCOMMON";
        return "COMMON";
    }
    
    private boolean isValidBlock(final Block block) {
        return block != null && 
               !block.getType().isAir() && 
               block.getType().isSolid() &&
               block.getType() != Material.BEDROCK;
    }
    
    /**
     * Convert LongAdder map to regular map
     */
    private Map<Material, Long> convertToRegularMap() {
        Map<Material, Long> result = new ConcurrentHashMap<>();
        materialCounts.forEach((material, adder) -> result.put(material, adder.sum()));
        return result;
    }
    
    public ConcurrentHashMap<Material, LongAdder> getMaterialCounts() {
        return new ConcurrentHashMap<>(materialCounts);
    }
    
    public OneblockIsland getIsland() {
        return island;
    }
    
    /**
     * Enhanced calculation result with comprehensive statistics
     */
    public static class CalculationResult {
        private final double totalExperience;
        private final int calculatedLevel;
        private final long totalBlocks;
        private final int uniqueMaterials;
        private final long processingTimeMs;
        private final Map<Material, Long> materialBreakdown;
        private final Map<String, Long> rarityDistribution;
        private final long calculationTimestamp;
        
        public CalculationResult(
            final double totalExperience,
            final int calculatedLevel,
            final long totalBlocks,
            final int uniqueMaterials,
            final long processingTimeMs,
            final Map<Material, Long> materialBreakdown,
            final Map<String, Long> rarityDistribution
        ) {
            this.totalExperience = totalExperience;
            this.calculatedLevel = calculatedLevel;
            this.totalBlocks = totalBlocks;
            this.uniqueMaterials = uniqueMaterials;
            this.processingTimeMs = processingTimeMs;
            this.materialBreakdown = materialBreakdown;
            this.rarityDistribution = rarityDistribution;
            this.calculationTimestamp = System.currentTimeMillis();
        }
        
        // Getters
        public double getTotalExperience() { return totalExperience; }
        public int getCalculatedLevel() { return calculatedLevel; }
        public long getTotalBlocks() { return totalBlocks; }
        public int getUniqueMaterials() { return uniqueMaterials; }
        public long getProcessingTimeMs() { return processingTimeMs; }
        public Map<Material, Long> getMaterialBreakdown() { return materialBreakdown; }
        public Map<String, Long> getRarityDistribution() { return rarityDistribution; }
        public long getCalculationTimestamp() { return calculationTimestamp; }
        
        // Calculated properties
        public double getExperiencePerBlock() {
            return totalBlocks > 0 ? totalExperience / totalBlocks : 0.0;
        }
        
        public double getBlocksPerSecond() {
            return processingTimeMs > 0 ? (totalBlocks * 1000.0) / processingTimeMs : 0.0;
        }
        
        public String getPerformanceRating() {
            double blocksPerSecond = getBlocksPerSecond();
            if (blocksPerSecond > 10000) return "EXCELLENT";
            if (blocksPerSecond > 5000) return "GOOD";
            if (blocksPerSecond > 1000) return "FAIR";
            return "SLOW";
        }
        
        @Override
        public String toString() {
            return String.format(
                "CalculationResult{exp=%.2f, level=%d, blocks=%d, materials=%d, time=%dms, rate=%.0f blocks/s, rating=%s}",
                totalExperience, calculatedLevel, totalBlocks, uniqueMaterials, 
                processingTimeMs, getBlocksPerSecond(), getPerformanceRating()
            );
        }
    }
}