package de.jexcellence.oneblock.service;

import de.jexcellence.oneblock.database.entity.generator.EGeneratorDesignType;
import de.jexcellence.oneblock.database.entity.generator.GeneratorDesign;
import de.jexcellence.oneblock.repository.GeneratorDesignRepository;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Registry for generator designs with caching support.
 *
 * @author JExcellence
 * @version 2.0.0
 * @since 2.0.0
 */
public class GeneratorDesignRegistry {
    
    private static final Logger LOGGER = Logger.getLogger("JExOneblock");
    
    private final GeneratorDesignRepository repository;
    private final Map<String, GeneratorDesign> designCache = new ConcurrentHashMap<>();
    private final Map<EGeneratorDesignType, GeneratorDesign> typeCache = new ConcurrentHashMap<>();
    /**
     * -- GETTER --
     *  Whether the registry is initialized.
     *
     * @return true if initialized
     */
    @Getter
    private boolean initialized = false;
    
    public GeneratorDesignRegistry(GeneratorDesignRepository repository) {
        this.repository = repository;
    }
    
    /**
     * Initializes the registry by loading all designs from database.
     *
     * @return future that completes when initialization is done
     */
    @NotNull
    public CompletableFuture<Void> initialize() {
        return repository.findAllAsync(0, Integer.MAX_VALUE).thenAccept(designs -> {
            designCache.clear();
            typeCache.clear();
            
            for (GeneratorDesign design : designs) {
                designCache.put(design.getDesignKey(), design);
                if (design.getDesignType() != null) {
                    typeCache.put(design.getDesignType(), design);
                }
            }
            
            initialized = true;
            LOGGER.info("Generator design registry initialized with " + designs.size() + " designs");
        });
    }
    
    /**
     * Reloads all designs from database.
     *
     * @return future that completes when reload is done
     */
    @NotNull
    public CompletableFuture<Void> reload() {
        initialized = false;
        return initialize();
    }
    
    /**
     * Registers a new design.
     *
     * @param design the design to register
     * @return future containing the saved design
     */
    @NotNull
    public CompletableFuture<GeneratorDesign> register(@NotNull GeneratorDesign design) {
        return CompletableFuture.supplyAsync(() -> {
            GeneratorDesign saved = repository.create(design);
            designCache.put(saved.getDesignKey(), saved);
            if (saved.getDesignType() != null) {
                typeCache.put(saved.getDesignType(), saved);
            }
            LOGGER.info("Registered generator design: " + saved.getDesignKey());
            return saved;
        });
    }
    
    /**
     * Unregisters a design by key.
     *
     * @param designKey the design key
     * @return future that completes when done
     */
    @NotNull
    public CompletableFuture<Void> unregister(@NotNull String designKey) {
        GeneratorDesign design = designCache.remove(designKey);
        if (design != null && design.getDesignType() != null) {
            typeCache.remove(design.getDesignType());
        }
        
        if (design != null && design.getId() != null) {
            return CompletableFuture.runAsync(() -> repository.delete(design.getId()));
        }
        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * Gets a design by key.
     *
     * @param designKey the design key
     * @return the design, or null if not found
     */
    @Nullable
    public GeneratorDesign getDesign(@NotNull String designKey) {
        return designCache.get(designKey);
    }
    
    /**
     * Gets a design by type.
     *
     * @param type the design type
     * @return the design, or null if not found
     */
    @Nullable
    public GeneratorDesign getDesign(@NotNull EGeneratorDesignType type) {
        return typeCache.get(type);
    }
    
    /**
     * Gets a design by tier.
     *
     * @param tier the tier number
     * @return the design, or null if not found
     */
    @Nullable
    public GeneratorDesign getDesignByTier(int tier) {
        EGeneratorDesignType type = EGeneratorDesignType.fromTier(tier);
        return type != null ? typeCache.get(type) : null;
    }
    
    /**
     * Gets all registered designs.
     *
     * @return unmodifiable collection of designs
     */
    @NotNull
    public Collection<GeneratorDesign> getAllDesigns() {
        return Collections.unmodifiableCollection(designCache.values());
    }
    
    /**
     * Gets all enabled designs.
     *
     * @return list of enabled designs sorted by tier
     */
    @NotNull
    public List<GeneratorDesign> getEnabledDesigns() {
        return designCache.values().stream()
                .filter(d -> d.getEnabled() != null && d.getEnabled())
                .sorted(Comparator.comparingInt(GeneratorDesign::getTier))
                .toList();
    }
    
    /**
     * Gets designs within a tier range.
     *
     * @param minTier minimum tier (inclusive)
     * @param maxTier maximum tier (inclusive)
     * @return list of designs in range
     */
    @NotNull
    public List<GeneratorDesign> getDesignsByTierRange(int minTier, int maxTier) {
        return designCache.values().stream()
                .filter(d -> d.getTier() >= minTier && d.getTier() <= maxTier)
                .filter(d -> d.getEnabled() != null && d.getEnabled())
                .sorted(Comparator.comparingInt(GeneratorDesign::getTier))
                .toList();
    }
    
    /**
     * Checks if a design is registered.
     *
     * @param designKey the design key
     * @return true if registered
     */
    public boolean isRegistered(@NotNull String designKey) {
        return designCache.containsKey(designKey);
    }
    
    /**
     * Checks if a player has unlocked a specific design tier.
     *
     * @param island the island to check
     * @param tier the design tier
     * @return true if unlocked
     */
    public boolean hasUnlockedDesign(@NotNull de.jexcellence.oneblock.database.entity.oneblock.OneblockIsland island, @NotNull EGeneratorDesignType tier) {
        // For now, return true - this should be implemented based on your unlock logic
        // You might want to check island level, prestige, or other requirements
        return true;
    }
    
    /**
     * Checks if a design type is registered.
     *
     * @param type the design type
     * @return true if registered
     */
    public boolean isRegistered(@NotNull EGeneratorDesignType type) {
        return typeCache.containsKey(type);
    }
    
    /**
     * Checks if a design is enabled.
     *
     * @param designKey the design key
     * @return true if enabled
     */
    public boolean isEnabled(@NotNull String designKey) {
        GeneratorDesign design = designCache.get(designKey);
        return design != null && design.getEnabled() != null && design.getEnabled();
    }
    
    /**
     * Gets the count of registered designs.
     *
     * @return design count
     */
    public int getDesignCount() {
        return designCache.size();
    }
    
    /**
     * Gets the count of enabled designs.
     *
     * @return enabled design count
     */
    public int getEnabledDesignCount() {
        return (int) designCache.values().stream()
                .filter(d -> d.getEnabled() != null && d.getEnabled())
                .count();
    }
    
    /**
     * Clears the cache.
     */
    public void clearCache() {
        designCache.clear();
        typeCache.clear();
    }

    /**
     * Gets all design keys.
     *
     * @return set of design keys
     */
    @NotNull
    public Set<String> getDesignKeys() {
        return Collections.unmodifiableSet(designCache.keySet());
    }
    
    /**
     * Gets all design types that are registered.
     *
     * @return set of design types
     */
    @NotNull
    public Set<EGeneratorDesignType> getRegisteredTypes() {
        return Collections.unmodifiableSet(typeCache.keySet());
    }
}
