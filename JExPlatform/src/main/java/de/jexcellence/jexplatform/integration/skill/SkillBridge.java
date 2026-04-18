package de.jexcellence.jexplatform.integration.skill;

import de.jexcellence.jexplatform.logging.JExLogger;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Sealed bridge for skill plugin integration.
 *
 * <p>Supports AuraSkills, EcoSkills, and mcMMO through reflection.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public sealed interface SkillBridge
        permits AuraSkillsSkillBridge, EcoSkillsSkillBridge, McMMOSkillBridge {

    /**
     * Descriptor for a skill type.
     *
     * @param id          the skill identifier
     * @param displayName the display name
     */
    record SkillDescriptor(@NotNull String id, @NotNull String displayName) { }

    /**
     * Returns the name of the backing skill plugin.
     *
     * @return the plugin name
     */
    @NotNull String pluginName();

    /**
     * Returns whether the backing plugin is available and loaded.
     *
     * @return {@code true} if available
     */
    boolean isAvailable();

    /**
     * Returns the skill level of a player for a specific skill.
     *
     * @param player  the player
     * @param skillId the skill identifier
     * @return a future resolving to the skill level
     */
    @NotNull CompletableFuture<Integer> getSkillLevel(@NotNull Player player,
                                                      @NotNull String skillId);

    /**
     * Returns all available skills.
     *
     * @return a future resolving to the list of skill descriptors
     */
    @NotNull CompletableFuture<List<SkillDescriptor>> getAvailableSkills();

    /**
     * Adds levels to a player's skill.
     *
     * @param player  the player
     * @param skillId the skill identifier
     * @param levels  the number of levels to add
     * @return a future resolving to {@code true} on success
     */
    @NotNull CompletableFuture<Boolean> addSkillLevels(@NotNull Player player,
                                                       @NotNull String skillId, int levels);

    /**
     * Consumes levels from a player's skill.
     *
     * @param player  the player
     * @param skillId the skill identifier
     * @param levels  the number of levels to consume
     * @return a future resolving to {@code true} on success
     */
    @NotNull CompletableFuture<Boolean> consumeSkillLevel(@NotNull Player player,
                                                          @NotNull String skillId, int levels);

    /**
     * Detects the best available skill plugin.
     *
     * @param plugin the owning plugin
     * @param logger the platform logger
     * @return the detected bridge, or empty if none available
     */
    static @NotNull Optional<SkillBridge> detect(@NotNull JavaPlugin plugin,
                                                 @NotNull JExLogger logger) {
        var pm = plugin.getServer().getPluginManager();

        if (pm.isPluginEnabled("AuraSkills")) {
            logger.info("Skill bridge: AuraSkills detected");
            return Optional.of(new AuraSkillsSkillBridge(logger));
        }
        if (pm.isPluginEnabled("EcoSkills")) {
            logger.info("Skill bridge: EcoSkills detected");
            return Optional.of(new EcoSkillsSkillBridge(logger));
        }
        if (pm.isPluginEnabled("mcMMO")) {
            logger.info("Skill bridge: mcMMO detected");
            return Optional.of(new McMMOSkillBridge(logger));
        }

        logger.warn("Skill bridge: no provider detected");
        return Optional.empty();
    }
}
