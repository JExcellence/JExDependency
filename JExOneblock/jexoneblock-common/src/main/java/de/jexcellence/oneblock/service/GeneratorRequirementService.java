package de.jexcellence.oneblock.service;

import com.raindropcentral.rplatform.requirement.AbstractRequirement;
import com.raindropcentral.rplatform.requirement.RequirementRegistry;
import com.raindropcentral.rplatform.requirement.RequirementService;
import de.jexcellence.oneblock.database.entity.generator.GeneratorDesign;
import de.jexcellence.oneblock.database.entity.generator.GeneratorDesignRequirement;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Service for checking generator design requirements.
 * Integrates with RPlatform RequirementService.
 *
 * @author JExcellence
 * @version 2.1.0
 * @since 2.0.0
 */
public class GeneratorRequirementService {

    private static final Logger LOGGER = Logger.getLogger("JExOneblock");

    private final RequirementService requirementService;
    private final RequirementRegistry requirementRegistry;
    private boolean initialized = false;

    public GeneratorRequirementService() {
        this.requirementService = RequirementService.getInstance();
        this.requirementRegistry = RequirementRegistry.getInstance();
    }

    /**
     * Initializes the service.
     */
    public void initialize() {
        if (initialized) return;

        // The OneBlockRequirementProvider is registered in JExOneblock.initializeComponents()
        // so we don't need to register it again here
        initialized = true;
        LOGGER.info("Generator requirement service initialized");
    }

    /**
     * Checks if all requirements for a generator design are met.
     */
    public boolean checkRequirements(@NotNull Player player, @NotNull GeneratorDesign design) {
        return getActiveRequirements(design).stream()
                .allMatch(req -> requirementService.isMet(player, req));
    }

    /**
     * Asynchronously checks if all requirements are met.
     */
    @NotNull
    public CompletableFuture<Boolean> checkRequirementsAsync(@NotNull Player player, @NotNull GeneratorDesign design) {
        return CompletableFuture.supplyAsync(() -> checkRequirements(player, design));
    }

    /**
     * Calculates overall progress towards meeting all requirements.
     */
    public double calculateProgress(@NotNull Player player, @NotNull GeneratorDesign design) {
        List<AbstractRequirement> requirements = getActiveRequirements(design);
        if (requirements.isEmpty()) return 1.0;
        return requirementService.calculateOverallProgress(player, requirements);
    }

    /**
     * Gets detailed progress for each requirement.
     */
    @NotNull
    public List<RequirementProgressDetail> getDetailedProgress(@NotNull Player player, @NotNull GeneratorDesign design) {
        List<GeneratorDesignRequirement> requirements = design.getRequirements();
        if (requirements == null || requirements.isEmpty()) {
            return List.of();
        }

        return requirements.stream()
                .filter(GeneratorDesignRequirement::getEnabled)
                .filter(req -> req.getRequirement() != null)
                .map(req -> {
                    AbstractRequirement requirement = req.getRequirement();
                    return new RequirementProgressDetail(
                            req,
                            requirementService.isMet(player, requirement),
                            requirementService.calculateProgress(player, requirement),
                            req.getDescriptionKey()
                    );
                })
                .toList();
    }

    /**
     * Consumes all requirements for a generator design.
     */
    public void consumeRequirements(@NotNull Player player, @NotNull GeneratorDesign design) {
        getActiveRequirements(design).stream()
                .filter(AbstractRequirement::shouldConsume)
                .forEach(req -> requirementService.consume(player, req));
    }

    /**
     * Asynchronously consumes all requirements.
     */
    @NotNull
    public CompletableFuture<Void> consumeRequirementsAsync(@NotNull Player player, @NotNull GeneratorDesign design) {
        return CompletableFuture.runAsync(() -> consumeRequirements(player, design));
    }

    /**
     * Gets unmet requirements for a design.
     */
    @NotNull
    public List<GeneratorDesignRequirement> getUnmetRequirements(@NotNull Player player, @NotNull GeneratorDesign design) {
        List<GeneratorDesignRequirement> requirements = design.getRequirements();
        if (requirements == null || requirements.isEmpty()) {
            return List.of();
        }

        return requirements.stream()
                .filter(GeneratorDesignRequirement::getEnabled)
                .filter(req -> req.getRequirement() != null)
                .filter(req -> !requirementService.isMet(player, req.getRequirement()))
                .toList();
    }

    /**
     * Clears requirement cache for a player.
     */
    public void clearCache(@NotNull Player player) {
        requirementService.clearCache(player.getUniqueId());
    }

    /**
     * Gets active requirements from a design.
     */
    @NotNull
    private List<AbstractRequirement> getActiveRequirements(@NotNull GeneratorDesign design) {
        List<GeneratorDesignRequirement> requirements = design.getRequirements();
        if (requirements == null || requirements.isEmpty()) {
            return List.of();
        }

        return requirements.stream()
                .filter(GeneratorDesignRequirement::getEnabled)
                .map(GeneratorDesignRequirement::getRequirement)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Requirement progress detail record.
     */
    public record RequirementProgressDetail(
            @NotNull GeneratorDesignRequirement requirement,
            boolean met,
            double progress,
            @NotNull String descriptionKey
    ) {
        public int getProgressPercentage() {
            return (int) (progress * 100);
        }

        public boolean isComplete() {
            return met;
        }
    }
}
