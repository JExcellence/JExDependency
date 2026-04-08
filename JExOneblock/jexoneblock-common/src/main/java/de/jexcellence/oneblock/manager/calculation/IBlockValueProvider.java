package de.jexcellence.oneblock.manager.calculation;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;

/**
 * Interface for providing block values used in island level calculations.
 * Implementations should handle caching and configuration-based value lookups.
 * 
 * @author JExcellence
 * @since 2.0.0
 * @version 2.0.0
 */
public interface IBlockValueProvider {
    
    /**
     * Gets the value for a specific block material.
     * 
     * @param material the material to get value for
     * @return the block value, or 0.0 if not configured
     */
    double getValue(@NotNull Material material);
    
    /**
     * Gets the value for a specific block material with a default fallback.
     * 
     * @param material the material to get value for
     * @param defaultValue the default value if not configured
     * @return the block value or default value
     */
    double getValue(@NotNull Material material, double defaultValue);
    
    /**
     * Checks if a material has a configured value.
     * 
     * @param material the material to check
     * @return true if material has a configured value
     */
    boolean hasValue(@NotNull Material material);
    
    /**
     * Gets all configured block values.
     * 
     * @return map of all configured block values
     */
    @NotNull
    Map<Material, Double> getAllValues();
    
    /**
     * Gets values for multiple materials efficiently.
     * 
     * @param materials the materials to get values for
     * @return map of material values
     */
    @NotNull
    Map<Material, Double> getValues(@NotNull Iterable<Material> materials);
    
    /**
     * Gets all materials that have configured values.
     * 
     * @return set of materials with configured values
     */
    @NotNull
    Set<Material> getConfiguredMaterials();
    
    /**
     * Gets the total number of configured block values.
     * 
     * @return the number of configured block values
     */
    int getConfiguredValueCount();
    
    /**
     * Reloads block values from configuration.
     * This method should be called when configuration changes.
     */
    void reload();
    
    /**
     * Clears the internal cache if caching is implemented.
     */
    void clearCache();
}