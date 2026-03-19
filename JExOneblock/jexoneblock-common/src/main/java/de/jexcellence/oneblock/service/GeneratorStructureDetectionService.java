package de.jexcellence.oneblock.service;

import de.jexcellence.oneblock.database.entity.generator.GeneratorDesign;
import de.jexcellence.oneblock.database.entity.generator.GeneratorDesignLayer;
import de.jexcellence.oneblock.database.entity.generator.PlayerGeneratorStructure;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockIsland;
import de.jexcellence.oneblock.repository.PlayerGeneratorStructureRepository;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Service for detecting and validating generator structures using the new GeneratorDesign entities.
 *
 * @author JExcellence
 * @version 2.0.0
 * @since 2.0.0
 */
public class GeneratorStructureDetectionService {

    private static final Logger LOGGER = Logger.getLogger("JExOneblock");

    private final GeneratorDesignRegistry designRegistry;
    private final GeneratorRequirementService requirementService;
    private final PlayerGeneratorStructureRepository structureRepository;

    public GeneratorStructureDetectionService(
            @NotNull GeneratorDesignRegistry designRegistry,
            @NotNull GeneratorRequirementService requirementService,
            @NotNull PlayerGeneratorStructureRepository structureRepository
    ) {
        this.designRegistry = designRegistry;
        this.requirementService = requirementService;
        this.structureRepository = structureRepository;
    }

    /**
     * Scans for valid structures around a center location.
     *
     * @param centerLocation the center location to scan from
     * @param scanRadius the radius to scan
     * @return future containing list of detected structures
     */
    @NotNull
    public CompletableFuture<List<StructureDetectionResult>> scanForStructures(
            @NotNull Location centerLocation,
            int scanRadius
    ) {
        return CompletableFuture.supplyAsync(() -> {
            List<StructureDetectionResult> results = new ArrayList<>();
            
            for (GeneratorDesign design : designRegistry.getEnabledDesigns()) {
                for (int x = -scanRadius; x <= scanRadius; x++) {
                    for (int z = -scanRadius; z <= scanRadius; z++) {
                        Location testLocation = centerLocation.clone().add(x, 0, z);
                        
                        ValidationResult validation = validateStructureSync(design, testLocation);
                        if (validation.valid()) {
                            results.add(new StructureDetectionResult(
                                    design,
                                    testLocation,
                                    validation.coreLocation(),
                                    true,
                                    "Structure detected and valid"
                            ));
                        }
                    }
                }
            }
            
            return results;
        });
    }

    /**
     * Validates a structure at a location for a specific design.
     *
     * @param design the design to validate against
     * @param location the base location of the structure
     * @return future containing validation result
     */
    @NotNull
    public CompletableFuture<ValidationResult> validateStructure(
            @NotNull GeneratorDesign design,
            @NotNull Location location
    ) {
        return CompletableFuture.supplyAsync(() -> validateStructureSync(design, location));
    }

    /**
     * Synchronously validates a structure.
     */
    @NotNull
    private ValidationResult validateStructureSync(@NotNull GeneratorDesign design, @NotNull Location location) {
        List<GeneratorDesignLayer> layers = design.getLayers();
        if (layers == null || layers.isEmpty()) {
            return new ValidationResult(false, "Design has no layers defined", null, new ArrayList<>());
        }

        World world = location.getWorld();
        if (world == null) {
            return new ValidationResult(false, "Invalid world", null, new ArrayList<>());
        }

        List<BlockMismatch> mismatches = new ArrayList<>();
        Location coreLocation = null;

        for (GeneratorDesignLayer layer : layers) {
            int layerY = location.getBlockY() + layer.getLayerIndex();
            Material[][] pattern = layer.getPattern();
            
            if (pattern == null) continue;

            int width = layer.getWidth();
            int depth = layer.getDepth();
            int startX = location.getBlockX() - width / 2;
            int startZ = location.getBlockZ() - depth / 2;

            for (int x = 0; x < width; x++) {
                for (int z = 0; z < depth; z++) {
                    Material expected = pattern[x][z];
                    if (expected == null || expected == Material.AIR) continue;

                    Location blockLoc = new Location(world, startX + x, layerY, startZ + z);
                    Material actual = blockLoc.getBlock().getType();

                    if (actual != expected) {
                        mismatches.add(new BlockMismatch(blockLoc, expected, actual));
                    }
                }
            }

            // Find core location from core layer
            if (layer.getIsCoreLayer() && layer.getCoreOffsetX() != null && layer.getCoreOffsetZ() != null) {
                coreLocation = new Location(
                        world,
                        startX + layer.getCoreOffsetX(),
                        layerY,
                        startZ + layer.getCoreOffsetZ()
                );
            }
        }

        if (mismatches.isEmpty()) {
            return new ValidationResult(true, "Structure is valid", coreLocation, mismatches);
        } else {
            return new ValidationResult(
                    false,
                    "Structure has " + mismatches.size() + " incorrect blocks",
                    coreLocation,
                    mismatches
            );
        }
    }

    /**
     * Detects which design type matches a structure at a location.
     *
     * @param location the location to check
     * @return future containing the detected design, or empty if none match
     */
    @NotNull
    public CompletableFuture<Optional<GeneratorDesign>> detectDesignType(@NotNull Location location) {
        return CompletableFuture.supplyAsync(() -> {
            for (GeneratorDesign design : designRegistry.getEnabledDesigns()) {
                ValidationResult result = validateStructureSync(design, location);
                if (result.valid()) {
                    return Optional.of(design);
                }
            }
            return Optional.empty();
        });
    }

    /**
     * Activates a generator structure for a player.
     *
     * @param player the player
     * @param island the player's island
     * @param design the design to activate
     * @param structureLocation the structure location
     * @return future containing activation result
     */
    @NotNull
    public CompletableFuture<ActivationResult> activateGenerator(
            @NotNull Player player,
            @NotNull OneblockIsland island,
            @NotNull GeneratorDesign design,
            @NotNull Location structureLocation
    ) {
        return validateStructure(design, structureLocation)
                .thenCompose(validation -> {
                    if (!validation.valid()) {
                        return CompletableFuture.completedFuture(
                                new ActivationResult(false, validation.message(), null)
                        );
                    }

                    // Check requirements
                    if (!requirementService.checkRequirements(player, design)) {
                        return CompletableFuture.completedFuture(
                                new ActivationResult(false, "Requirements not met", null)
                        );
                    }

                    // Check if already has this design
                    return structureRepository.hasUnlockedDesignAsync(player.getUniqueId(), design.getId())
                            .thenCompose(hasDesign -> {
                                if (hasDesign) {
                                    return CompletableFuture.completedFuture(
                                            new ActivationResult(false, "Already have this generator type", null)
                                    );
                                }

                                // Create the structure using builder pattern
                                PlayerGeneratorStructure structure = PlayerGeneratorStructure.builder()
                                        .islandId(island.getId())
                                        .ownerId(player.getUniqueId())
                                        .design(design)
                                        .coreLocation(structureLocation)
                                        .isValid(true)
                                        .isActive(true)
                                        .builtAt(System.currentTimeMillis())
                                        .build();

                                // Consume requirements
                                requirementService.consumeRequirements(player, design);

                                return CompletableFuture.supplyAsync(() -> structureRepository.create(structure))
                                        .thenApply(saved -> new ActivationResult(
                                                true,
                                                "Generator activated successfully!",
                                                saved
                                        ));
                            });
                });
    }

    /**
     * Deactivates a generator structure.
     *
     * @param structure the structure to deactivate
     * @return future containing success status
     */
    @NotNull
    public CompletableFuture<Boolean> deactivateGenerator(@NotNull PlayerGeneratorStructure structure) {
        structure.setIsActive(false);
        structure.setIsValid(false);
        return CompletableFuture.supplyAsync(() -> {
            try {
                structureRepository.update(structure);
                return true;
            } catch (Exception ex) {
                LOGGER.warning("Failed to deactivate generator: " + ex.getMessage());
                return false;
            }
        });
    }

    /**
     * Validates all generators for an island.
     *
     * @param islandId the island ID
     * @return future containing validation summary
     */
    @NotNull
    public CompletableFuture<ValidationSummary> validateAllGenerators(@NotNull Long islandId) {
        return structureRepository.findByIslandIdAsync(islandId)
                .thenCompose(structures -> {
                    List<CompletableFuture<GeneratorValidationResult>> futures = new ArrayList<>();

                    for (PlayerGeneratorStructure structure : structures) {
                        if (!structure.getIsActive() || structure.getCoreLocation() == null) {
                            continue;
                        }

                        GeneratorDesign design = structure.getDesign();
                        if (design == null) continue;

                        Location location = structure.getCoreLocation();

                        CompletableFuture<GeneratorValidationResult> future = validateStructure(design, location)
                                .thenApply(result -> new GeneratorValidationResult(
                                        structure,
                                        result.valid(),
                                        result.message()
                                ));
                        futures.add(future);
                    }

                    return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                            .thenApply(v -> {
                                List<GeneratorValidationResult> results = futures.stream()
                                        .map(CompletableFuture::join)
                                        .toList();

                                int valid = (int) results.stream().filter(GeneratorValidationResult::valid).count();
                                int invalid = results.size() - valid;

                                return new ValidationSummary(results, valid, invalid);
                            });
                });
    }

    /**
     * Gets all active generators for an island.
     *
     * @param islandId the island ID
     * @return future containing list of active structures
     */
    @NotNull
    public CompletableFuture<List<PlayerGeneratorStructure>> getActiveGenerators(@NotNull Long islandId) {
        return structureRepository.findActiveByIslandIdAsync(islandId);
    }

    // ==================== Result Records ====================

    public record StructureDetectionResult(
            @NotNull GeneratorDesign design,
            @NotNull Location structureLocation,
            @Nullable Location coreLocation,
            boolean valid,
            @NotNull String message
    ) {}

    public record ValidationResult(
            boolean valid,
            @NotNull String message,
            @Nullable Location coreLocation,
            @NotNull List<BlockMismatch> mismatches
    ) {}

    public record BlockMismatch(
            @NotNull Location location,
            @NotNull Material expected,
            @NotNull Material actual
    ) {}

    public record ActivationResult(
            boolean success,
            @NotNull String message,
            @Nullable PlayerGeneratorStructure structure
    ) {}

    public record GeneratorValidationResult(
            @NotNull PlayerGeneratorStructure structure,
            boolean valid,
            @NotNull String message
    ) {}

    public record ValidationSummary(
            @NotNull List<GeneratorValidationResult> results,
            int validCount,
            int invalidCount
    ) {
        public int getTotalCount() {
            return results.size();
        }

        public boolean hasInvalidGenerators() {
            return invalidCount > 0;
        }
    }
}
