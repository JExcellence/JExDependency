package de.jexcellence.oneblock.manager.calculation;

import de.jexcellence.oneblock.manager.config.CalculationConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Configuration-based block value provider with caching support.
 * This implementation loads block values from CalculationConfiguration
 * and provides efficient cached lookups.
 * 
 * @author JExcellence
 * @since 2.0.0
 * @version 2.0.0
 */
@Slf4j
public class BlockValueProvider implements IBlockValueProvider {
    
    private final CalculationConfiguration configuration;
    private final Map<Material, Double> valueCache;
    private final ReadWriteLock cacheLock;
    private volatile boolean cacheValid;
    
    // Default values for common materials
    private static final Map<Material, Double> DEFAULT_VALUES;
    
    static {
        Map<Material, Double> values = new java.util.HashMap<>();
        values.put(Material.STONE, 1.0);
        values.put(Material.COBBLESTONE, 1.0);
        values.put(Material.DIRT, 1.0);
        values.put(Material.GRASS_BLOCK, 1.5);
        values.put(Material.SAND, 1.0);
        values.put(Material.GRAVEL, 1.0);
        values.put(Material.COAL_ORE, 5.0);
        values.put(Material.IRON_ORE, 10.0);
        values.put(Material.GOLD_ORE, 15.0);
        values.put(Material.DIAMOND_ORE, 50.0);
        values.put(Material.EMERALD_ORE, 75.0);
        values.put(Material.NETHERITE_SCRAP, 100.0);
        DEFAULT_VALUES = java.util.Collections.unmodifiableMap(values);
    }
    
    /**
     * Creates a new block value provider.
     * 
     * @param configuration the calculation configuration
     */
    public BlockValueProvider(@NotNull CalculationConfiguration configuration) {
        this.configuration = configuration;
        this.valueCache = new ConcurrentHashMap<>();
        this.cacheLock = new ReentrantReadWriteLock();
        this.cacheValid = false;
        
        // Initialize cache
        reload();
    }
    
    @Override
    public double getValue(@NotNull Material material) {
        return getValue(material, 0.0);
    }
    
    @Override
    public double getValue(@NotNull Material material, double defaultValue) {
        ensureCacheValid();
        
        cacheLock.readLock().lock();
        try {
            Double value = valueCache.get(material);
            if (value != null) {
                return value;
            }
            
            // Check default values
            Double defaultVal = DEFAULT_VALUES.get(material);
            if (defaultVal != null) {
                return defaultVal;
            }
            
            return defaultValue;
        } finally {
            cacheLock.readLock().unlock();
        }
    }
    
    @Override
    public boolean hasValue(@NotNull Material material) {
        ensureCacheValid();
        
        cacheLock.readLock().lock();
        try {
            return valueCache.containsKey(material) || DEFAULT_VALUES.containsKey(material);
        } finally {
            cacheLock.readLock().unlock();
        }
    }
    
    @Override
    @NotNull
    public Map<Material, Double> getAllValues() {
        ensureCacheValid();
        
        cacheLock.readLock().lock();
        try {
            Map<Material, Double> result = new HashMap<>(DEFAULT_VALUES);
            result.putAll(valueCache);
            return result;
        } finally {
            cacheLock.readLock().unlock();
        }
    }
    
    @Override
    @NotNull
    public Map<Material, Double> getValues(@NotNull Iterable<Material> materials) {
        ensureCacheValid();
        
        Map<Material, Double> result = new HashMap<>();
        
        cacheLock.readLock().lock();
        try {
            for (Material material : materials) {
                Double value = valueCache.get(material);
                if (value != null) {
                    result.put(material, value);
                } else {
                    Double defaultVal = DEFAULT_VALUES.get(material);
                    if (defaultVal != null) {
                        result.put(material, defaultVal);
                    }
                }
            }
        } finally {
            cacheLock.readLock().unlock();
        }
        
        return result;
    }
    
    @Override
    @NotNull
    public Set<Material> getConfiguredMaterials() {
        ensureCacheValid();
        
        cacheLock.readLock().lock();
        try {
            Set<Material> result = Set.copyOf(DEFAULT_VALUES.keySet());
            result.addAll(valueCache.keySet());
            return result;
        } finally {
            cacheLock.readLock().unlock();
        }
    }
    
    @Override
    public int getConfiguredValueCount() {
        ensureCacheValid();
        
        cacheLock.readLock().lock();
        try {
            return valueCache.size() + DEFAULT_VALUES.size();
        } finally {
            cacheLock.readLock().unlock();
        }
    }
    
    @Override
    public void reload() {
        log.debug("Reloading block values from configuration");
        
        cacheLock.writeLock().lock();
        try {
            valueCache.clear();
            
            // Load values from configuration (convert String keys to Material)
            Map<String, Double> configValues = configuration.getBlockValues();
            if (configValues != null) {
                configValues.forEach((key, value) -> {
                    try {
                        Material material = Material.valueOf(key);
                        valueCache.put(material, value);
                    } catch (IllegalArgumentException e) {
                        log.warn("Unknown material in configuration: {}", key);
                    }
                });
            }
            
            cacheValid = true;
            log.info("Loaded {} block values from configuration", valueCache.size());
            
        } finally {
            cacheLock.writeLock().unlock();
        }
    }
    
    @Override
    public void clearCache() {
        log.debug("Clearing block value cache");
        
        cacheLock.writeLock().lock();
        try {
            valueCache.clear();
            cacheValid = false;
        } finally {
            cacheLock.writeLock().unlock();
        }
    }
    
    /**
     * Ensures the cache is valid and loaded.
     */
    private void ensureCacheValid() {
        if (!cacheValid) {
            reload();
        }
    }
    
    /**
     * Gets cache statistics for monitoring.
     * 
     * @return cache statistics
     */
    @NotNull
    public CacheStats getCacheStats() {
        cacheLock.readLock().lock();
        try {
            return new CacheStats(
                valueCache.size(),
                DEFAULT_VALUES.size(),
                cacheValid
            );
        } finally {
            cacheLock.readLock().unlock();
        }
    }
    
    /**
     * Cache statistics for monitoring.
     */
    public static class CacheStats {
        private final int configuredValues;
        private final int defaultValues;
        private final boolean valid;
        
        public CacheStats(int configuredValues, int defaultValues, boolean valid) {
            this.configuredValues = configuredValues;
            this.defaultValues = defaultValues;
            this.valid = valid;
        }
        
        public int getConfiguredValues() { return configuredValues; }
        public int getDefaultValues() { return defaultValues; }
        public int getTotalValues() { return configuredValues + defaultValues; }
        public boolean isValid() { return valid; }
        
        @Override
        public String toString() {
            return String.format("CacheStats{configured=%d, default=%d, total=%d, valid=%s}",
                configuredValues, defaultValues, getTotalValues(), valid);
        }
    }
}