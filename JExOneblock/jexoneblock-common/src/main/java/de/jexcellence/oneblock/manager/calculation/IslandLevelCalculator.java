package de.jexcellence.oneblock.manager.calculation;

import de.jexcellence.oneblock.database.entity.oneblock.OneblockIsland;
import de.jexcellence.oneblock.database.entity.oneblock.IslandRegion;
import de.jexcellence.oneblock.manager.config.CalculationConfiguration;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Simplified island level calculator with distributed workload processing.
 * This implementation removes excessive optimization complexity while maintaining
 * performance through efficient batch processing and proper resource management.
 */
public class IslandLevelCalculator implements IIslandLevelCalculator {
    
    private static final Logger LOGGER = Logger.getLogger(IslandLevelCalculator.class.getName());
    
    private final CalculationConfiguration configuration;
    private final IBlockValueProvider blockValueProvider;
    private final ExecutorService executorService;
    private final Semaphore calculationSemaphore;
    
    // Performance metrics
    private final AtomicLong totalCalculations = new AtomicLong(0);
    private final AtomicLong totalCalculationTime = new AtomicLong(0);
    private final AtomicLong lastCalculationTime = new AtomicLong(0);
    private final AtomicInteger activeCalculations = new AtomicInteger(0);
    
    private volatile boolean shutdown = false;
    
    public IslandLevelCalculator(@NotNull CalculationConfiguration configuration,
                                @NotNull IBlockValueProvider blockValueProvider,
                                @NotNull ExecutorService executorService) {
        this.configuration = configuration;
        this.blockValueProvider = blockValueProvider;
        this.executorService = executorService;
        this.calculationSemaphore = new Semaphore(configuration.getMaxConcurrentCalculations());
    }
    
    @Override
    @NotNull
    public CompletableFuture<CalculationResult> calculateLevel(@NotNull OneblockIsland island) {
        return calculateLevel(island, (percentage, message) -> {
            // Default no-op progress callback
        });
    }
    
    @Override
    @NotNull
    public CompletableFuture<CalculationResult> calculateLevel(@NotNull OneblockIsland island,
                                                              @NotNull ProgressCallback progressCallback) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            
            try {
                calculationSemaphore.acquire();
                activeCalculations.incrementAndGet();
                
                progressCallback.onProgress(0, "Starting calculation");
                
                IslandRegion region = island.getRegion();
                if (region == null) {
                    progressCallback.onProgress(100, "No region found");
                    return new CalculationResult(1, 0.0, Map.of(), 0);
                }
                
                World world = region.getCurrentWorld();
                if (world == null) {
                    progressCallback.onProgress(100, "World not available");
                    return new CalculationResult(1, 0.0, Map.of(), 0);
                }
                
                progressCallback.onProgress(10, "Scanning region");
                
                // Scan region with distributed workload processing
                Map<Material, Long> blockCounts = scanRegionDistributed(region, world, progressCallback);
                
                progressCallback.onProgress(80, "Calculating experience");
                
                // Calculate experience and level
                double experience = calculateExperience(blockCounts);
                int level = calculateLevelFromExperience(experience);
                
                progressCallback.onProgress(90, "Updating island stats");
                
                // Update island statistics
                island.setLevel(level);
                island.setExperience(experience);
                
                long calculationTime = System.currentTimeMillis() - startTime;
                
                // Update performance metrics
                totalCalculations.incrementAndGet();
                totalCalculationTime.addAndGet(calculationTime);
                lastCalculationTime.set(calculationTime);
                
                progressCallback.onProgress(100, "Calculation complete");
                
                LOGGER.fine("Calculated level " + level + " (exp: " + experience + ") for island " + 
                           island.getId() + " in " + calculationTime + "ms");
                
                return new CalculationResult(level, experience, blockCounts, calculationTime);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Calculation interrupted", e);
            } finally {
                activeCalculations.decrementAndGet();
                calculationSemaphore.release();
            }
        }, executorService);
    }
    
    @Override
    @NotNull
    public CompletableFuture<Map<Material, Long>> getBlockCounts(@NotNull OneblockIsland island) {
        return CompletableFuture.supplyAsync(() -> {
            IslandRegion region = island.getRegion();
            if (region == null) {
                return Map.of();
            }
            
            World world = region.getCurrentWorld();
            if (world == null) {
                return Map.of();
            }
            
            return scanRegionDistributed(region, world, (percentage, message) -> {
                // No-op progress callback for block count only
            });
        }, executorService);
    }
    
    @Override
    public double calculateExperience(@NotNull Map<Material, Long> blockCounts) {
        double totalExperience = 0.0;
        
        for (Map.Entry<Material, Long> entry : blockCounts.entrySet()) {
            Material material = entry.getKey();
            long count = entry.getValue();
            
            double blockValue = blockValueProvider.getValue(material);
            totalExperience += blockValue * count;
        }
        
        return totalExperience;
    }
    
    @Override
    public int calculateLevelFromExperience(double experience) {
        if (experience <= 0) {
            return 1;
        }
        
        // Simplified progression formula - single curve instead of multiple tiers
        // Uses square root progression for balanced growth
        return (int) Math.max(1, Math.sqrt(experience / 10.0)) + 1;
    }
    
    @Override
    @NotNull
    public PerformanceMetrics getPerformanceMetrics() {
        return new PerformanceMetrics(
            totalCalculations.get(),
            totalCalculationTime.get(),
            lastCalculationTime.get(),
            activeCalculations.get()
        );
    }
    
    /**
     * Scans the region using distributed workload processing.
     * Processes the region in batches to avoid overwhelming the server.
     */
    @NotNull
    private Map<Material, Long> scanRegionDistributed(@NotNull IslandRegion region,
                                                     @NotNull World world,
                                                     @NotNull ProgressCallback progressCallback) {
        Map<Material, Long> blockCounts = new ConcurrentHashMap<>();
        
        int minX = region.getMinX();
        int maxX = region.getMaxX();
        int minY = region.getMinY();
        int maxY = region.getMaxY();
        int minZ = region.getMinZ();
        int maxZ = region.getMaxZ();
        
        int totalBlocks = (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
        int processedBlocks = 0;
        int batchSize = configuration.getBatchSize();
        
        LOGGER.fine("Scanning region with " + totalBlocks + " total blocks in batches of " + batchSize);
        
        // Process in batches to distribute workload
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                int batchCount = 0;
                
                for (int z = minZ; z <= maxZ; z++) {
                    Block block = world.getBlockAt(x, y, z);
                    Material material = block.getType();
                    
                    if (!material.isAir()) {
                        blockCounts.merge(material, 1L, Long::sum);
                    }
                    
                    processedBlocks++;
                    batchCount++;
                    
                    // Yield control after processing a batch
                    if (batchCount >= batchSize) {
                        Thread.yield();
                        batchCount = 0;
                        
                        // Update progress
                        int progressPercentage = Math.min(90, (int) ((processedBlocks * 70.0) / totalBlocks) + 10);
                        progressCallback.onProgress(progressPercentage,
                            String.format("Processed %d/%d blocks", processedBlocks, totalBlocks));
                    }
                    
                    // Check for shutdown request
                    if (shutdown) {
                        LOGGER.warning("Calculation interrupted due to shutdown request");
                        break;
                    }
                }
                
                if (shutdown) {
                    break;
                }
            }
            
            if (shutdown) {
                break;
            }
        }
        
        LOGGER.fine("Scanned " + processedBlocks + " blocks, found " + blockCounts.size() + " unique materials");
        return blockCounts;
    }
    
    /**
     * Initialize the calculator
     */
    public void initialize() {
        LOGGER.info("Initializing Island Level Calculator with max " + 
                   configuration.getMaxConcurrentCalculations() + " concurrent calculations");
        
        // Validate configuration
        configuration.validate();
        
        // Initialize block value provider
        blockValueProvider.reload();
        
        LOGGER.info("Island Level Calculator initialized successfully");
    }
    
    /**
     * Shutdown the calculator
     */
    public void shutdown() {
        LOGGER.info("Shutting down Island Level Calculator");
        
        shutdown = true;
        
        // Wait for active calculations to complete
        int maxWaitSeconds = 30;
        int waitCount = 0;
        
        while (activeCalculations.get() > 0 && waitCount < maxWaitSeconds) {
            LOGGER.info("Waiting for " + activeCalculations.get() + " active calculations to complete...");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            waitCount++;
        }
        
        if (activeCalculations.get() > 0) {
            LOGGER.warning("Shutdown with " + activeCalculations.get() + " active calculations still running");
        }
        
        LOGGER.info("Island Level Calculator shutdown complete");
    }
    
    /**
     * Reload configuration
     */
    public void reload() {
        LOGGER.info("Reloading Island Level Calculator configuration");
        
        // Reload block value provider
        blockValueProvider.reload();
        
        LOGGER.info("Island Level Calculator configuration reloaded");
    }
    
    /**
     * Check health status
     */
    public boolean isHealthy() {
        // Check if we have too many active calculations
        int active = activeCalculations.get();
        int maxConcurrent = configuration.getMaxConcurrentCalculations();
        
        if (active > maxConcurrent * 2) {
            LOGGER.warning("Health check failed: too many active calculations (" + active + " > " + (maxConcurrent * 2) + ")");
            return false;
        }
        
        // Check if block value provider is healthy
        try {
            blockValueProvider.getValue(Material.STONE);
            return true;
        } catch (Exception e) {
            LOGGER.warning("Health check failed: block value provider error: " + e.getMessage());
            return false;
        }
    }
}