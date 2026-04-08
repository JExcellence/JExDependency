package de.jexcellence.oneblock.service;

import de.jexcellence.oneblock.database.entity.generator.EGeneratorDesignType;
import de.jexcellence.oneblock.database.entity.generator.GeneratorDesign;
import de.jexcellence.oneblock.database.entity.generator.PlayerGeneratorStructure;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockIsland;
import de.jexcellence.oneblock.factory.GeneratorDesignFactory;
import de.jexcellence.oneblock.repository.GeneratorDesignRepository;
import de.jexcellence.oneblock.repository.PlayerGeneratorStructureRepository;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Central manager for all generator structure operations.
 * <p>
 * Coordinates between design service, requirement service, build service,
 * detection service, and visualization service.
 * </p>
 *
 * @author JExcellence
 * @version 2.0.0
 * @since 2.0.0
 */
public class GeneratorStructureManager {
    
    private static final Logger LOGGER = Logger.getLogger("JExOneblock");
    
    private final Plugin plugin;
    private final GeneratorDesignRepository designRepository;
    private final PlayerGeneratorStructureRepository structureRepository;
    
    private GeneratorDesignRegistry designRegistry;
    private GeneratorDesignService designService;
    private GeneratorRequirementService requirementService;
    private GeneratorStructureDetectionService detectionService;
    private GeneratorStructureBuildService buildService;
    
    private boolean initialized = false;
    
    public GeneratorStructureManager(
            @NotNull Plugin plugin,
            @NotNull GeneratorDesignRepository designRepository,
            @NotNull PlayerGeneratorStructureRepository structureRepository
    ) {
        this.plugin = plugin;
        this.designRepository = designRepository;
        this.structureRepository = structureRepository;
    }
    
    /**
     * Initializes the generator structure system.
     *
     * @return future that completes when initialization is done
     */
    @NotNull
    public CompletableFuture<Void> initialize() {
        if (initialized) {
            return CompletableFuture.completedFuture(null);
        }
        
        LOGGER.info("Initializing Generator Structure Manager...");
        
        // Initialize requirement service first
        requirementService = new GeneratorRequirementService();
        requirementService.initialize();
        
        // Initialize design registry
        designRegistry = new GeneratorDesignRegistry(designRepository);
        
        // Initialize design service
        designService = new GeneratorDesignService(designRegistry, requirementService);
        
        // Initialize detection service
        detectionService = new GeneratorStructureDetectionService(designRegistry, requirementService, structureRepository);
        
        // Initialize build service
        buildService = new GeneratorStructureBuildService(plugin);
        
        // Load designs from database, create defaults if empty
        return designRegistry.initialize()
                .thenCompose(v -> {
                    if (designRegistry.getDesignCount() == 0) {
                        LOGGER.info("No designs found, creating default designs...");
                        return createDefaultDesigns();
                    }
                    return CompletableFuture.completedFuture(null);
                })
                .thenRun(() -> {
                    initialized = true;
                    LOGGER.info("Generator Structure Manager initialized with " + 
                            designRegistry.getDesignCount() + " designs");
                })
                .exceptionally(ex -> {
                    LOGGER.log(Level.SEVERE, "Failed to initialize Generator Structure Manager", ex);
                    return null;
                });
    }
    
    /**
     * Shuts down the generator structure system.
     */
    public void shutdown() {
        if (!initialized) return;
        
        LOGGER.info("Shutting down Generator Structure Manager...");
        
        if (designRegistry != null) {
            designRegistry.clearCache();
        }
        
        initialized = false;
        LOGGER.info("Generator Structure Manager shut down");
    }
    
    /**
     * Reloads the generator structure system.
     *
     * @return future that completes when reload is done
     */
    @NotNull
    public CompletableFuture<Void> reload() {
        LOGGER.info("Reloading Generator Structure Manager...");
        
        if (designRegistry != null) {
            return designRegistry.reload()
                    .thenRun(() -> LOGGER.info("Generator Structure Manager reloaded"));
        }
        
        return CompletableFuture.completedFuture(null);
    }
    
    // ==================== Design Operations ====================
    
    /**
     * Gets all available designs for a player.
     *
     * @param player the player
     * @return list of available designs
     */
    @NotNull
    public List<GeneratorDesign> getAvailableDesigns(@NotNull Player player) {
        return designService.getAvailableDesigns(player);
    }
    
    /**
     * Gets a design by key.
     *
     * @param designKey the design key
     * @return optional containing the design
     */
    @NotNull
    public Optional<GeneratorDesign> getDesign(@NotNull String designKey) {
        return Optional.ofNullable(designService.getDesign(designKey));
    }
    
    /**
     * Gets a design by type.
     *
     * @param type the design type
     * @return optional containing the design
     */
    @NotNull
    public Optional<GeneratorDesign> getDesign(@NotNull EGeneratorDesignType type) {
        return Optional.ofNullable(designService.getDesign(type));
    }
    
    /**
     * Checks if a player can unlock a design.
     *
     * @param player the player
     * @param design the design
     * @return true if player meets requirements
     */
    public boolean canUnlock(@NotNull Player player, @NotNull GeneratorDesign design) {
        return designService.canUnlock(player, design);
    }
    
    // ==================== Structure Operations ====================
    
    /**
     * Builds a structure for a player.
     *
     * @param player the player
     * @param design the design to build
     * @param location the build location
     * @return future containing the build result
     */
    @NotNull
    public CompletableFuture<BuildResult> buildStructure(
            @NotNull Player player,
            @NotNull GeneratorDesign design,
            @NotNull Location location
    ) {
        // Check requirements
        if (!canUnlock(player, design)) {
            return CompletableFuture.completedFuture(
                    new BuildResult(false, "Requirements not met", null)
            );
        }
        
        // Check if area is clear
        if (!buildService.isBuildAreaClear(location, design)) {
            return CompletableFuture.completedFuture(
                    new BuildResult(false, "Build area is not clear", null)
            );
        }
        
        // Check materials
        if (!buildService.hasRequiredMaterials(player, design)) {
            Map<Material, Integer> missing = buildService.getMissingMaterials(player, design);
            return CompletableFuture.completedFuture(
                    new BuildResult(false, "Missing materials: " + formatMissingMaterials(missing), null)
            );
        }
        
        // Start the build
        return buildService.startAutoBuild(player, design, location)
                .thenApply(result -> new BuildResult(result.success(), result.message(), null));
    }
    
    private String formatMissingMaterials(Map<Material, Integer> missing) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Material, Integer> entry : missing.entrySet()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(entry.getValue()).append("x ").append(entry.getKey().name().toLowerCase().replace('_', ' '));
        }
        return sb.toString();
    }
    
    /**
     * Validates a structure at a location.
     *
     * @param design the design to validate against
     * @param location the location
     * @return future containing validation result
     */
    @NotNull
    public CompletableFuture<ValidationResult> validateStructure(
            @NotNull GeneratorDesign design,
            @NotNull Location location
    ) {
        return detectionService.validateStructure(design, location)
                .thenApply(result -> new ValidationResult(result.valid(), result.message(), result.coreLocation()));
    }
    
    /**
     * Activates a structure for a player.
     *
     * @param player the player
     * @param island the player's island
     * @param design the design
     * @param location the structure location
     * @return future containing activation result
     */
    @NotNull
    public CompletableFuture<ActivationResult> activateStructure(
            @NotNull Player player,
            @NotNull OneblockIsland island,
            @NotNull GeneratorDesign design,
            @NotNull Location location
    ) {
        return detectionService.activateGenerator(player, island, design, location)
                .thenApply(result -> new ActivationResult(result.success(), result.message(), result.structure()));
    }
    
    /**
     * Destroys a player's structure.
     *
     * @param structure the structure to destroy
     * @return future containing success status
     */
    @NotNull
    public CompletableFuture<Boolean> destroyStructure(@NotNull PlayerGeneratorStructure structure) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                structureRepository.delete(structure.getId());
                return true;
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Failed to destroy structure", ex);
                return false;
            }
        });
    }
    
    /**
     * Activates a structure.
     *
     * @param structure the structure to activate
     * @return future containing success status
     */
    @NotNull
    public CompletableFuture<Boolean> activateStructure(@NotNull PlayerGeneratorStructure structure) {
        structure.setIsActive(true);
        return CompletableFuture.supplyAsync(() -> {
            try {
                structureRepository.update(structure);
                return true;
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Failed to activate structure", ex);
                return false;
            }
        });
    }
    
    /**
     * Deactivates a structure.
     *
     * @param structure the structure to deactivate
     * @return future containing success status
     */
    @NotNull
    public CompletableFuture<Boolean> deactivateStructure(@NotNull PlayerGeneratorStructure structure) {
        structure.setIsActive(false);
        return CompletableFuture.supplyAsync(() -> {
            try {
                structureRepository.update(structure);
                return true;
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Failed to deactivate structure", ex);
                return false;
            }
        });
    }
    
    /**
     * Gets all structures for an island.
     *
     * @param islandId the island ID
     * @return future containing list of structures
     */
    @NotNull
    public CompletableFuture<List<PlayerGeneratorStructure>> getStructures(@NotNull Long islandId) {
        return structureRepository.findByIslandIdAsync(islandId);
    }
    
    /**
     * Gets active structures for an island.
     *
     * @param islandId the island ID
     * @return future containing list of active structures
     */
    @NotNull
    public CompletableFuture<List<PlayerGeneratorStructure>> getActiveStructures(@NotNull Long islandId) {
        return structureRepository.findActiveByIslandIdAsync(islandId);
    }
    
    /**
     * Checks if a player has unlocked a design.
     *
     * @param player the player
     * @param design the design
     * @return future containing the result
     */
    @NotNull
    public CompletableFuture<Boolean> hasUnlockedDesign(@NotNull Player player, @NotNull GeneratorDesign design) {
        return structureRepository.hasUnlockedDesignAsync(player.getUniqueId(), design.getId());
    }
    
    // ==================== Visualization ====================
    
    /**
     * Shows a preview of a design at a location.
     *
     * @param player the player to show preview to
     * @param design the design to preview
     * @param location the preview location
     */
    public void showPreview(@NotNull Player player, @NotNull GeneratorDesign design, @NotNull Location location) {
        // TODO: Implement using StructureVisualizationService
    }
    
    /**
     * Hides the preview for a player.
     *
     * @param player the player
     */
    public void hidePreview(@NotNull Player player) {
        // TODO: Implement using StructureVisualizationService
    }
    
    // ==================== Private Methods ====================
    
    private CompletableFuture<Void> createDefaultDesigns() {
        List<GeneratorDesign> defaultDesigns = GeneratorDesignFactory.createAllDefaultDesigns();
        
        CompletableFuture<?>[] futures = defaultDesigns.stream()
                .map(designRegistry::register)
                .toArray(CompletableFuture[]::new);
        
        return CompletableFuture.allOf(futures)
                .thenRun(() -> LOGGER.info("Created " + defaultDesigns.size() + " default designs"));
    }
    
    // ==================== Getters ====================
    
    @NotNull
    public GeneratorDesignRegistry getDesignRegistry() {
        return designRegistry;
    }
    
    @NotNull
    public GeneratorDesignService getDesignService() {
        return designService;
    }
    
    @NotNull
    public GeneratorRequirementService getRequirementService() {
        return requirementService;
    }
    
    @NotNull
    public GeneratorStructureDetectionService getDetectionService() {
        return detectionService;
    }
    
    @NotNull
    public GeneratorStructureBuildService getBuildService() {
        return buildService;
    }
    
    public boolean isInitialized() {
        return initialized;
    }
    
    // ==================== Result Records ====================
    
    public record BuildResult(
            boolean success,
            @NotNull String message,
            @Nullable PlayerGeneratorStructure structure
    ) {}
    
    public record ValidationResult(
            boolean valid,
            @NotNull String message,
            @Nullable Location coreLocation
    ) {}
    
    public record ActivationResult(
            boolean success,
            @NotNull String message,
            @Nullable PlayerGeneratorStructure structure
    ) {}
}
