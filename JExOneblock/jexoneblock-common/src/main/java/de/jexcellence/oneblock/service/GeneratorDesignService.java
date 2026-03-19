package de.jexcellence.oneblock.service;

import de.jexcellence.oneblock.database.entity.generator.EGeneratorDesignType;
import de.jexcellence.oneblock.database.entity.generator.GeneratorDesign;
import de.jexcellence.oneblock.repository.GeneratorDesignRepository;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Service for managing generator designs.
 * <p>
 * Provides methods for retrieving, filtering, and checking availability
 * of generator designs for players.
 * </p>
 *
 * @author JExcellence
 * @version 2.0.0
 * @since 2.0.0
 */
public class GeneratorDesignService {
    
    private static final Logger LOGGER = Logger.getLogger("JExOneblock");
    
    private final GeneratorDesignRegistry designRegistry;
    private final GeneratorRequirementService requirementService;
    
    public GeneratorDesignService(
            @NotNull GeneratorDesignRegistry designRegistry,
            @NotNull GeneratorRequirementService requirementService
    ) {
        this.designRegistry = designRegistry;
        this.requirementService = requirementService;
    }
    
    /**
     * Gets a design by its key.
     *
     * @param designKey the design key
     * @return the design if found, null otherwise
     */
    @Nullable
    public GeneratorDesign getDesign(@NotNull String designKey) {
        return designRegistry.getDesign(designKey);
    }
    
    /**
     * Gets a design by its type.
     *
     * @param type the design type
     * @return the design if found, null otherwise
     */
    @Nullable
    public GeneratorDesign getDesign(@NotNull EGeneratorDesignType type) {
        return designRegistry.getDesign(type);
    }
    
    /**
     * Gets a design by tier.
     *
     * @param tier the tier number
     * @return optional containing the design if found
     */
    @NotNull
    public Optional<GeneratorDesign> getDesignByTier(int tier) {
        return Optional.ofNullable(designRegistry.getDesignByTier(tier));
    }
    
    /**
     * Gets all registered designs.
     *
     * @return list of all designs
     */
    @NotNull
    public List<GeneratorDesign> getAllDesigns() {
        return List.copyOf(designRegistry.getAllDesigns());
    }
    
    /**
     * Gets all enabled designs sorted by tier.
     *
     * @return list of enabled designs
     */
    @NotNull
    public List<GeneratorDesign> getEnabledDesigns() {
        return designRegistry.getEnabledDesigns();
    }
    
    /**
     * Gets designs available to a player (enabled and requirements met).
     *
     * @param player the player
     * @return list of available designs
     */
    @NotNull
    public List<GeneratorDesign> getAvailableDesigns(@NotNull Player player) {
        return designRegistry.getEnabledDesigns().stream()
                .filter(design -> requirementService.checkRequirements(player, design))
                .toList();
    }
    
    /**
     * Asynchronously gets designs available to a player.
     *
     * @param player the player
     * @return future containing list of available designs
     */
    @NotNull
    public CompletableFuture<List<GeneratorDesign>> getAvailableDesignsAsync(@NotNull Player player) {
        return CompletableFuture.supplyAsync(() -> getAvailableDesigns(player));
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
        return designRegistry.getDesignsByTierRange(minTier, maxTier);
    }
    
    /**
     * Checks if a design is enabled.
     *
     * @param designKey the design key
     * @return true if enabled
     */
    public boolean isDesignEnabled(@NotNull String designKey) {
        return designRegistry.isEnabled(designKey);
    }
    
    /**
     * Checks if a design is enabled.
     *
     * @param type the design type
     * @return true if enabled
     */
    public boolean isDesignEnabled(@NotNull EGeneratorDesignType type) {
        GeneratorDesign design = designRegistry.getDesign(type);
        return design != null && design.getEnabled() != null && design.getEnabled();
    }
    
    /**
     * Checks if a player can unlock a design.
     *
     * @param player the player
     * @param design the design
     * @return true if player meets all requirements
     */
    public boolean canUnlock(@NotNull Player player, @NotNull GeneratorDesign design) {
        if (design.getEnabled() == null || !design.getEnabled()) {
            return false;
        }
        return requirementService.checkRequirements(player, design);
    }
    
    /**
     * Asynchronously checks if a player can unlock a design.
     *
     * @param player the player
     * @param design the design
     * @return future containing the result
     */
    @NotNull
    public CompletableFuture<Boolean> canUnlockAsync(@NotNull Player player, @NotNull GeneratorDesign design) {
        return requirementService.checkRequirementsAsync(player, design);
    }
    
    /**
     * Gets the unlock progress for a player on a design.
     *
     * @param player the player
     * @param design the design
     * @return progress from 0.0 to 1.0
     */
    public double getUnlockProgress(@NotNull Player player, @NotNull GeneratorDesign design) {
        return requirementService.calculateProgress(player, design);
    }
    
    /**
     * Gets the next design in the tier progression.
     *
     * @param currentDesign the current design
     * @return optional containing the next design if exists
     */
    @NotNull
    public Optional<GeneratorDesign> getNextTierDesign(@NotNull GeneratorDesign currentDesign) {
        int nextTier = currentDesign.getTier() + 1;
        return getDesignByTier(nextTier);
    }
    
    /**
     * Gets the previous design in the tier progression.
     *
     * @param currentDesign the current design
     * @return optional containing the previous design if exists
     */
    @NotNull
    public Optional<GeneratorDesign> getPreviousTierDesign(@NotNull GeneratorDesign currentDesign) {
        int prevTier = currentDesign.getTier() - 1;
        if (prevTier < 1) return Optional.empty();
        return getDesignByTier(prevTier);
    }
    
    /**
     * Gets the highest tier design available to a player.
     *
     * @param player the player
     * @return optional containing the highest available design
     */
    @NotNull
    public Optional<GeneratorDesign> getHighestAvailableDesign(@NotNull Player player) {
        return getAvailableDesigns(player).stream()
                .max((a, b) -> Integer.compare(a.getTier(), b.getTier()));
    }
    
    /**
     * Gets the count of designs.
     *
     * @return total design count
     */
    public int getDesignCount() {
        return designRegistry.getDesignCount();
    }
    
    /**
     * Gets the count of enabled designs.
     *
     * @return enabled design count
     */
    public int getEnabledDesignCount() {
        return designRegistry.getEnabledDesignCount();
    }
}
