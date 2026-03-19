package de.jexcellence.oneblock.factory;

import de.jexcellence.oneblock.database.entity.evolution.CustomEvolution;
import de.jexcellence.oneblock.database.entity.evolution.*;
import de.jexcellence.oneblock.database.entity.evolution.PredefinedEvolution;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockEvolution;
import de.jexcellence.oneblock.type.EEvolutionRarityType;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.logging.Logger;

/**
 * Factory for creating and managing OneblockEvolution instances with supplier pattern for lazy initialization
 * Provides registration, discovery, and validation mechanisms for evolution configurations
 */
public class EvolutionFactory {
    
    private static final Logger log = Logger.getLogger("JExOneblock");
    private static final EvolutionFactory INSTANCE = new EvolutionFactory();
    
    private final Map<String, Supplier<OneblockEvolution>> evolutionSuppliers = new ConcurrentHashMap<>();
    private final Map<String, OneblockEvolution> cachedEvolutions = new ConcurrentHashMap<>();
    private final Set<String> validationErrors = ConcurrentHashMap.newKeySet();
    private final Map<String, String> pluginSources = new ConcurrentHashMap<>();
    private final Set<String> autoRegisteredEvolutions = ConcurrentHashMap.newKeySet();
    
    private EvolutionFactory() {
        // Private constructor for singleton pattern
    }
    
    /**
     * Gets the singleton instance of EvolutionFactory
     * @return the factory instance
     */
    public static @NotNull EvolutionFactory getInstance() {
        return INSTANCE;
    }
    
    /**
     * Registers an evolution supplier with lazy initialization
     * @param name the unique name for the evolution
     * @param supplier the supplier that creates the evolution instance
     * @return true if registration was successful
     */
    public boolean registerEvolution(@NotNull String name, @NotNull Supplier<OneblockEvolution> supplier) {
        if (name.trim().isEmpty()) {
            log.warning("Cannot register evolution with empty name");
            return false;
        }
        
        if (evolutionSuppliers.containsKey(name)) {
            log.warning("Evolution '" + name + "' is already registered, skipping registration");
            return false;
        }
        
        try {
            // Validate the supplier by creating a test instance
            OneblockEvolution testEvolution = supplier.get();
            List<String> errors = validateEvolution(testEvolution);
            
            if (!errors.isEmpty()) {
                log.severe("Evolution '" + name + "' failed validation: " + String.join(", ", errors));
                validationErrors.addAll(errors.stream()
                    .map(error -> name + ": " + error)
                    .collect(Collectors.toSet()));
                return false;
            }
            
            evolutionSuppliers.put(name, supplier);
            log.info("Successfully registered evolution: " + name);
            return true;
            
        } catch (Exception e) {
            log.severe("Failed to register evolution '" + name + "': " + e.getMessage());
            e.printStackTrace();
            validationErrors.add(name + ": Registration failed - " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Registers a predefined evolution with automatic supplier creation
     * @param evolution the predefined evolution to register
     * @return true if registration was successful
     */
    public boolean registerPredefinedEvolution(@NotNull PredefinedEvolution evolution) {
        return registerEvolution(evolution.getEvolutionName(), () -> evolution);
    }
    
    /**
     * Registers a custom evolution with automatic supplier creation
     * @param evolution the custom evolution to register
     * @return true if registration was successful
     */
    public boolean registerCustomEvolution(@NotNull CustomEvolution evolution) {
        return registerEvolution(evolution.getEvolutionName(), () -> {
            CustomEvolution copy = new CustomEvolution(
                evolution.getEvolutionName(),
                evolution.getLevel(),
                evolution.getExperienceToPass(),
                evolution.getDescription(),
                evolution.getCreatorName()
            );
            copy.setShowcase(evolution.getShowcase());
            copy.setCreatedAt(evolution.getCreatedAt());
            copy.setTemplate(evolution.isTemplate());
            copy.setDisabled(evolution.isDisabled());
            return copy;
        });
    }
    
    /**
     * Creates an evolution instance using lazy initialization
     * @param name the name of the evolution to create
     * @return the evolution instance, or null if not found
     */
    public @Nullable OneblockEvolution createEvolution(@NotNull String name) {
        if (!evolutionSuppliers.containsKey(name)) {
            log.warning("No evolution registered with name: " + name);
            return null;
        }
        
        try {
            OneblockEvolution evolution = evolutionSuppliers.get(name).get();
            log.fine("Created evolution instance: " + name);
            return evolution;
            
        } catch (Exception e) {
            log.severe("Failed to create evolution '" + name + "': " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Gets a cached evolution instance, creating it if necessary
     * @param name the name of the evolution
     * @return the cached evolution instance, or null if not found
     */
    public @Nullable OneblockEvolution getCachedEvolution(@NotNull String name) {
        return cachedEvolutions.computeIfAbsent(name, this::createEvolution);
    }
    
    /**
     * Gets all registered evolution names
     * @return set of registered evolution names
     */
    public @NotNull Set<String> getRegisteredEvolutionNames() {
        return new HashSet<>(evolutionSuppliers.keySet());
    }
    
    /**
     * Gets all cached evolution instances
     * @return collection of cached evolutions
     */
    public @NotNull Collection<OneblockEvolution> getCachedEvolutions() {
        return new ArrayList<>(cachedEvolutions.values());
    }
    
    /**
     * Preloads all registered evolutions into cache
     * This ensures all evolutions are available with their requirements loaded
     * @return number of evolutions preloaded
     */
    public int preloadAllEvolutions() {
        int preloaded = 0;
        for (String name : evolutionSuppliers.keySet()) {
            if (!cachedEvolutions.containsKey(name)) {
                OneblockEvolution evolution = createEvolution(name);
                if (evolution != null) {
                    cachedEvolutions.put(name, evolution);
                    preloaded++;
                }
            }
        }
        log.info("Preloaded " + preloaded + " evolutions with requirements");
        return preloaded;
    }
    
    /**
     * Gets all evolutions sorted by level
     * @return list of evolutions sorted by level
     */
    public @NotNull List<OneblockEvolution> getAllEvolutionsSortedByLevel() {
        // Ensure all evolutions are cached
        preloadAllEvolutions();
        
        return cachedEvolutions.values().stream()
            .sorted(Comparator.comparingInt(OneblockEvolution::getLevel))
            .collect(Collectors.toList());
    }
    
    /**
     * Gets an evolution by its level
     * @param level the level to find
     * @return the evolution at that level, or null if not found
     */
    public @Nullable OneblockEvolution getEvolutionByLevel(int level) {
        // Ensure all evolutions are cached
        preloadAllEvolutions();
        
        return cachedEvolutions.values().stream()
            .filter(evolution -> evolution.getLevel() == level)
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Checks if an evolution is registered
     * @param name the evolution name to check
     * @return true if the evolution is registered
     */
    public boolean isEvolutionRegistered(@NotNull String name) {
        return evolutionSuppliers.containsKey(name);
    }
    
    /**
     * Unregisters an evolution and removes it from cache
     * @param name the name of the evolution to unregister
     * @return true if the evolution was unregistered
     */
    public boolean unregisterEvolution(@NotNull String name) {
        boolean wasRegistered = evolutionSuppliers.remove(name) != null;
        cachedEvolutions.remove(name);
        
        if (wasRegistered) {
            log.info("Unregistered evolution: " + name);
        }
        
        return wasRegistered;
    }
    
    /**
     * Clears all registered evolutions and cache
     */
    public void clearAll() {
        int supplierCount = evolutionSuppliers.size();
        int cacheCount = cachedEvolutions.size();
        
        evolutionSuppliers.clear();
        cachedEvolutions.clear();
        validationErrors.clear();
        pluginSources.clear();
        autoRegisteredEvolutions.clear();
        
        log.info("Cleared " + supplierCount + " evolution suppliers and " + cacheCount + " cached instances");
    }
    
    /**
     * Gets evolutions by category (for PredefinedEvolution instances)
     * @param category the category to filter by
     * @return list of evolutions in the specified category
     */
    public @NotNull List<OneblockEvolution> getEvolutionsByCategory(@NotNull String category) {
        return cachedEvolutions.values().stream()
            .filter(evolution -> evolution instanceof PredefinedEvolution)
            .map(evolution -> (PredefinedEvolution) evolution)
            .filter(evolution -> evolution.belongsToCategory(category))
            .collect(Collectors.toList());
    }
    
    /**
     * Gets evolutions by level range
     * @param minLevel minimum level (inclusive)
     * @param maxLevel maximum level (inclusive)
     * @return list of evolutions in the specified level range
     */
    public @NotNull List<OneblockEvolution> getEvolutionsByLevelRange(int minLevel, int maxLevel) {
        return cachedEvolutions.values().stream()
            .filter(evolution -> evolution.getLevel() >= minLevel && evolution.getLevel() <= maxLevel)
            .sorted(Comparator.comparingInt(OneblockEvolution::getLevel))
            .collect(Collectors.toList());
    }
    
    /**
     * Gets default evolutions (for PredefinedEvolution instances)
     * @return list of default evolutions
     */
    public @NotNull List<PredefinedEvolution> getDefaultEvolutions() {
        return cachedEvolutions.values().stream()
            .filter(evolution -> evolution instanceof PredefinedEvolution)
            .map(evolution -> (PredefinedEvolution) evolution)
            .filter(PredefinedEvolution::isSystemDefault)
            .sorted(Comparator.comparingInt(PredefinedEvolution::getPriority).reversed())
            .collect(Collectors.toList());
    }
    
    /**
     * Gets evolutions that have content for a specific rarity
     * @param rarity the rarity to check for
     * @return list of evolutions with content for the specified rarity
     */
    public @NotNull List<OneblockEvolution> getEvolutionsWithRarity(@NotNull EEvolutionRarityType rarity) {
        return cachedEvolutions.values().stream()
            .filter(evolution -> evolution.hasContentForRarity(rarity))
            .collect(Collectors.toList());
    }
    
    /**
     * Registers an evolution from a plugin source
     * @param name the unique name for the evolution
     * @param supplier the supplier that creates the evolution instance
     * @param pluginName the name of the plugin registering this evolution
     * @return true if registration was successful
     */
    public boolean registerEvolutionFromPlugin(@NotNull String name, @NotNull Supplier<OneblockEvolution> supplier, @NotNull String pluginName) {
        boolean success = registerEvolution(name, supplier);
        if (success) {
            pluginSources.put(name, pluginName);
            log.info("Successfully registered evolution '" + name + "' from plugin '" + pluginName + "'");
        }
        return success;
    }
    
    /**
     * Automatically discovers and registers predefined evolutions from a package
     * @param packageName the package to scan for predefined evolutions
     * @return number of evolutions successfully registered
     */
    public int autoRegisterPredefinedEvolutions(@NotNull String packageName) {
        int registeredCount = 0;
        
        try {
            log.info("Starting auto-registration of predefined evolutions for package: " + packageName);
            // This would need to be implemented with classpath scanning
            // For now, we'll register known predefined evolutions
            registeredCount += autoRegisterKnownEvolutions();
            
            log.info("Auto-registered " + registeredCount + " predefined evolutions from package: " + packageName);
            
        } catch (Exception e) {
            log.severe("Failed to auto-register evolutions from package '" + packageName + "': " + e.getMessage());
            e.printStackTrace();
        }
        
        return registeredCount;
    }
    
    /**
     * Registers known predefined evolutions by discovering evolution classes
     * @return number of evolutions registered
     */
    private int autoRegisterKnownEvolutions() {
        int count = 0;
        
        // Register Genesis evolution first (level 1)
        count += registerPredefinedEvolutionClass("Genesis", GenesisEvolution.class);
        
        // Register all predefined evolution classes
        count += registerPredefinedEvolutionClass("Stone", StoneEvolution.class);
        count += registerPredefinedEvolutionClass("Coal", CoalEvolution.class);
        count += registerPredefinedEvolutionClass("Copper", CopperEvolution.class);
        count += registerPredefinedEvolutionClass("Iron", IronEvolution.class);
        count += registerPredefinedEvolutionClass("Gold", GoldEvolution.class);
        count += registerPredefinedEvolutionClass("Diamond", DiamondEvolution.class);
        count += registerPredefinedEvolutionClass("Nether", NetherEvolution.class);
        count += registerPredefinedEvolutionClass("End", EndEvolution.class);
        count += registerPredefinedEvolutionClass("Wood", WoodEvolution.class);
        count += registerPredefinedEvolutionClass("Bronze", BronzeEvolution.class);
        
        // Advanced evolutions
        count += registerPredefinedEvolutionClass("Terra", TerraEvolution.class);
        count += registerPredefinedEvolutionClass("Aqua", AquaEvolution.class);
        count += registerPredefinedEvolutionClass("Ignis", IgnisEvolution.class);
        count += registerPredefinedEvolutionClass("Ventus", VentusEvolution.class);
        count += registerPredefinedEvolutionClass("Electric", ElectricEvolution.class);
        count += registerPredefinedEvolutionClass("Bio", BioEvolution.class);
        count += registerPredefinedEvolutionClass("Cyber", CyberEvolution.class);
        count += registerPredefinedEvolutionClass("Digital", DigitalEvolution.class);
        count += registerPredefinedEvolutionClass("Nano", NanoEvolution.class);
        count += registerPredefinedEvolutionClass("Quantum", QuantumEvolution.class);
        
        // Themed evolutions
        count += registerPredefinedEvolutionClass("Castle", CastleEvolution.class);
        count += registerPredefinedEvolutionClass("Knight", KnightEvolution.class);
        count += registerPredefinedEvolutionClass("Crusader", CrusaderEvolution.class);
        count += registerPredefinedEvolutionClass("Explorer", ExplorerEvolution.class);
        count += registerPredefinedEvolutionClass("Artist", ArtistEvolution.class);
        count += registerPredefinedEvolutionClass("Factory", FactoryEvolution.class);
        
        // Cosmic evolutions
        count += registerPredefinedEvolutionClass("Solar", SolarEvolution.class);
        count += registerPredefinedEvolutionClass("Moon", MoonEvolution.class);
        count += registerPredefinedEvolutionClass("Stellar", StellarEvolution.class);
        count += registerPredefinedEvolutionClass("Nebula", NebulaEvolution.class);
        count += registerPredefinedEvolutionClass("Galactic", GalacticEvolution.class);
        count += registerPredefinedEvolutionClass("Cosmic", CosmicEvolution.class);
        count += registerPredefinedEvolutionClass("Universal", UniversalEvolution.class);
        count += registerPredefinedEvolutionClass("Dimensional", DimensionalEvolution.class);
        count += registerPredefinedEvolutionClass("Multiverse", MultiverseEvolution.class);
        count += registerPredefinedEvolutionClass("Infinity", InfinityEvolution.class);
        count += registerPredefinedEvolutionClass("Eternity", EternityEvolution.class);
        count += registerPredefinedEvolutionClass("Omnipotence", OmnipotenceEvolution.class);
        
        // Special evolutions
        count += registerPredefinedEvolutionClass("Void", VoidEvolution.class);
        count += registerPredefinedEvolutionClass("BlackHole", BlackHoleEvolution.class);
        count += registerPredefinedEvolutionClass("Supernova", SupernovaEvolution.class);
        count += registerPredefinedEvolutionClass("Eden", EdenEvolution.class);
        count += registerPredefinedEvolutionClass("Dragon", DragonEvolution.class);
        count += registerPredefinedEvolutionClass("Artemis", ArtemisEvolution.class);
        count += registerPredefinedEvolutionClass("Hephaestus", HephaestusEvolution.class);
        
        // Element evolutions
        count += registerPredefinedEvolutionClass("Argon", ArgonEvolution.class);
        count += registerPredefinedEvolutionClass("Helium", HeliumEvolution.class);
        count += registerPredefinedEvolutionClass("Krypton", KryptonEvolution.class);
        
        // Additional evolutions to reach 50+
        count += registerPredefinedEvolutionClass("Earth", EarthEvolution.class);
        
        return count;
    }
    
    /**
     * Registers a predefined evolution class by calling its create() method
     * @param name evolution name for logging
     * @param evolutionClass the evolution class with a static create() method
     * @return 1 if registered successfully, 0 otherwise
     */
    private int registerPredefinedEvolutionClass(String name, Class<?> evolutionClass) {
        try {
            log.info("Attempting to register predefined evolution: " + name + " from class " + evolutionClass.getSimpleName());
            
            // Call the static create() method
            var createMethod = evolutionClass.getDeclaredMethod("create");
            var evolution = (OneblockEvolution) createMethod.invoke(null);
            
            if (evolution == null) {
                log.warning("✗ Evolution class " + evolutionClass.getSimpleName() + " returned null from create() method");
                return 0;
            }
            
            log.fine("Created evolution object for " + name + ": " + evolution.getEvolutionName() + 
                    " (level " + evolution.getLevel() + ", exp " + evolution.getExperienceToPass() + ")");
            
            // Check if evolution is ready (has content)
            if (!evolution.isReady()) {
                log.warning("✗ Evolution " + name + " is not ready (no valid content)");
                return 0;
            }
            
            boolean registered = registerEvolution(evolution.getEvolutionName(), () -> {
                try {
                    var method = evolutionClass.getDeclaredMethod("create");
                    return (OneblockEvolution) method.invoke(null);
                } catch (Exception e) {
                    log.severe("Failed to create evolution " + name + " from class " + evolutionClass.getSimpleName() + ": " + e.getMessage());
                    return null;
                }
            });
            
            if (registered) {
                autoRegisteredEvolutions.add(name);
                return 1;
            } else {
                return 0;
            }
        } catch (Exception e) {
            log.severe("✗ Error auto-registering predefined evolution " + name + ": " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }

    
    /**
     * Unregisters all evolutions from a specific plugin
     * @param pluginName the name of the plugin
     * @return number of evolutions unregistered
     */
    public int unregisterEvolutionsFromPlugin(@NotNull String pluginName) {
        Set<String> evolutionsToRemove = pluginSources.entrySet().stream()
            .filter(entry -> pluginName.equals(entry.getValue()))
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
        
        int unregisteredCount = 0;
        for (String evolutionName : evolutionsToRemove) {
            if (unregisterEvolution(evolutionName)) {
                pluginSources.remove(evolutionName);
                autoRegisteredEvolutions.remove(evolutionName);
                unregisteredCount++;
            }
        }
        
        if (unregisteredCount > 0) {
            log.info("Unregistered " + unregisteredCount + " evolutions from plugin '" + pluginName + "'");
        }
        
        return unregisteredCount;
    }
    
    /**
     * Gets evolutions registered by a specific plugin
     * @param pluginName the plugin name
     * @return list of evolution names registered by the plugin
     */
    public @NotNull List<String> getEvolutionsByPlugin(@NotNull String pluginName) {
        return pluginSources.entrySet().stream()
            .filter(entry -> pluginName.equals(entry.getValue()))
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }
    
    /**
     * Gets the plugin source for an evolution
     * @param evolutionName the evolution name
     * @return the plugin name that registered this evolution, or null if not from a plugin
     */
    public @Nullable String getPluginSource(@NotNull String evolutionName) {
        return pluginSources.get(evolutionName);
    }
    
    /**
     * Checks if an evolution was auto-registered
     * @param evolutionName the evolution name
     * @return true if the evolution was auto-registered
     */
    public boolean isAutoRegistered(@NotNull String evolutionName) {
        return autoRegisteredEvolutions.contains(evolutionName);
    }
    
    /**
     * Reloads all auto-registered evolutions
     * @return number of evolutions reloaded
     */
    public int reloadAutoRegisteredEvolutions() {
        // Clear auto-registered evolutions
        Set<String> toReload = new HashSet<>(autoRegisteredEvolutions);
        for (String evolutionName : toReload) {
            unregisterEvolution(evolutionName);
        }
        autoRegisteredEvolutions.clear();
        
        // Re-register them
        return autoRegisterKnownEvolutions();
    }
    
    /**
     * Validates an evolution configuration
     * @param evolution the evolution to validate
     * @return list of validation errors, empty if valid
     */
    public @NotNull List<String> validateEvolution(@NotNull OneblockEvolution evolution) {
        List<String> errors = new ArrayList<>();
        
        if (evolution.getEvolutionName() == null || evolution.getEvolutionName().trim().isEmpty()) {
            errors.add("Evolution name cannot be null or empty");
        }
        
        if (evolution.getLevel() < 0) {
            errors.add("Evolution level cannot be negative");
        }
        
        if (evolution.getExperienceToPass() <= 0) {
            errors.add("Experience to pass must be greater than 0");
        }
        
        if (!evolution.isReady()) {
            errors.add("Evolution must have at least one valid content type");
        }
        
        // Type-specific validation
        if (evolution instanceof CustomEvolution) {
            CustomEvolution custom = (CustomEvolution) evolution;
            errors.addAll(custom.validateConfiguration());
        } else if (evolution instanceof PredefinedEvolution) {
            PredefinedEvolution predefined = (PredefinedEvolution) evolution;
            errors.addAll(predefined.validateConfiguration());
        }
        
        return errors;
    }
    
    /**
     * Gets all validation errors from registration attempts
     * @return set of validation error messages
     */
    public @NotNull Set<String> getValidationErrors() {
        return new HashSet<>(validationErrors);
    }
    
    /**
     * Clears all validation errors
     */
    public void clearValidationErrors() {
        validationErrors.clear();
    }
    
    /**
     * Validates all registered evolutions
     * @return map of evolution names to their validation errors
     */
    public @NotNull Map<String, List<String>> validateAllEvolutions() {
        Map<String, List<String>> validationResults = new HashMap<>();
        
        for (String name : evolutionSuppliers.keySet()) {
            try {
                OneblockEvolution evolution = createEvolution(name);
                if (evolution != null) {
                    List<String> errors = validateEvolution(evolution);
                    if (!errors.isEmpty()) {
                        validationResults.put(name, errors);
                    }
                }
            } catch (Exception e) {
                validationResults.put(name, Collections.singletonList("Failed to create evolution: " + e.getMessage()));
            }
        }
        
        return validationResults;
    }
    
    /**
     * Gets factory statistics
     * @return map containing factory statistics
     */
    public @NotNull Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("registeredEvolutions", evolutionSuppliers.size());
        stats.put("cachedEvolutions", cachedEvolutions.size());
        stats.put("validationErrors", validationErrors.size());
        stats.put("pluginSources", pluginSources.size());
        stats.put("autoRegisteredEvolutions", autoRegisteredEvolutions.size());
        
        long customCount = cachedEvolutions.values().stream()
            .filter(evolution -> evolution instanceof CustomEvolution)
            .count();
        stats.put("customEvolutions", customCount);
        
        long predefinedCount = cachedEvolutions.values().stream()
            .filter(evolution -> evolution instanceof PredefinedEvolution)
            .count();
        stats.put("predefinedEvolutions", predefinedCount);
        
        // Plugin statistics
        Map<String, Long> pluginStats = pluginSources.values().stream()
            .collect(Collectors.groupingBy(plugin -> plugin, Collectors.counting()));
        stats.put("evolutionsByPlugin", pluginStats);
        
        return stats;
    }
    
    @Override
    public String toString() {
        return "EvolutionFactory{" +
                "registeredEvolutions=" + evolutionSuppliers.size() +
                ", cachedEvolutions=" + cachedEvolutions.size() +
                ", validationErrors=" + validationErrors.size() +
                '}';
    }
}