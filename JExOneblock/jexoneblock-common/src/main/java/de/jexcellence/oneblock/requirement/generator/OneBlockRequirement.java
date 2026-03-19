package de.jexcellence.oneblock.requirement.generator;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.raindropcentral.rplatform.requirement.AbstractRequirement;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockIsland;
import de.jexcellence.oneblock.database.repository.OneblockIslandRepository;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Base class for all OneBlock-specific requirements.
 * <p>
 * Provides common functionality for island retrieval and progress calculation
 * patterns used across all OneBlock requirements.
 * </p>
 * <p>
 * All OneBlock requirements use {@link Type#CUSTOM} as their base type.
 * Specific type names (e.g., "EVOLUTION_LEVEL", "BLOCKS_BROKEN") are registered
 * with the {@link com.raindropcentral.rplatform.requirement.RequirementRegistry}
 * via the {@link OneBlockRequirementProvider}, which enables proper JSON
 * serialization/deserialization through Jackson's NamedType system.
 * </p>
 * <p>
 * <b>Repository Injection:</b> The {@link OneblockIslandRepository} must be set
 * via {@link #setIslandRepository(OneblockIslandRepository)} during plugin initialization
 * before any requirements are evaluated.
 * </p>
 *
 * @author JExcellence
 * @version 2.1.0
 * @since 2.0.0
 */
public abstract class OneBlockRequirement extends AbstractRequirement {

    @JsonIgnore
    private static volatile OneblockIslandRepository islandRepository;

    protected OneBlockRequirement() {
        super("ONEBLOCK_CUSTOM");
    }

    /**
     * Sets the island repository for all OneBlock requirements.
     * <p>
     * This should be called once during plugin initialization in JExOneblock.
     * </p>
     *
     * @param repository the island repository
     */
    public static void setIslandRepository(@NotNull OneblockIslandRepository repository) {
        islandRepository = repository;
    }

    /**
     * Gets the player's island from the repository.
     *
     * @param player the player
     * @return the island, or empty if not found
     * @throws IllegalStateException if the repository has not been set
     */
    @NotNull
    protected Optional<OneblockIsland> getPlayerIsland(@NotNull Player player) {
        if (islandRepository == null) {
            throw new IllegalStateException(
                "OneblockIslandRepository not set. Call OneBlockRequirement.setIslandRepository() during plugin initialization."
            );
        }
        
        return Optional.ofNullable(islandRepository.findByOwner(player.getUniqueId()));
    }

    /**
     * Gets the current value for this requirement from the player's island.
     *
     * @param island the player's island
     * @return the current value
     */
    protected abstract long getCurrentValue(@NotNull OneblockIsland island);

    /**
     * Gets the required value for this requirement.
     *
     * @return the required value
     */
    protected abstract long getRequiredValue();

    @Override
    public boolean isMet(@NotNull Player player) {
        return getPlayerIsland(player)
                .map(island -> getCurrentValue(island) >= getRequiredValue())
                .orElse(false);
    }

    @Override
    public double calculateProgress(@NotNull Player player) {
        long required = getRequiredValue();
        if (required <= 0) return 1.0;

        return getPlayerIsland(player)
                .map(island -> Math.min(1.0, (double) getCurrentValue(island) / required))
                .orElse(0.0);
    }

    @Override
    public void consume(@NotNull Player player) {
        // OneBlock requirements are typically not consumed
    }

    /**
     * Validates this requirement's configuration.
     *
     * @throws IllegalStateException if the configuration is invalid
     */
    public void validate() {
        if (getRequiredValue() < 0) {
            throw new IllegalStateException("Required value cannot be negative");
        }
    }

    /**
     * Clears the repository reference.
     * Call this when the plugin is disabled.
     */
    public static void clearRepository() {
        islandRepository = null;
    }
}
