package de.jexcellence.jexplatform.integration.skill;

import de.jexcellence.jexplatform.logging.JExLogger;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * EcoSkills reflection-based skill bridge.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public final class EcoSkillsSkillBridge extends AbstractReflectionSkillBridge
        implements SkillBridge {

    /**
     * Creates the EcoSkills bridge.
     *
     * @param logger the platform logger
     */
    EcoSkillsSkillBridge(@NotNull JExLogger logger) {
        super(logger);
    }

    @Override
    public @NotNull String pluginName() {
        return "EcoSkills";
    }

    @Override
    public boolean isAvailable() {
        return findClass("com.willfp.ecoskills.api.EcoSkillsAPI") != null;
    }

    @Override
    public @NotNull CompletableFuture<Integer> getSkillLevel(@NotNull Player player,
                                                              @NotNull String skillId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var apiClass = Class.forName("com.willfp.ecoskills.api.EcoSkillsAPI");
                var result = apiClass.getMethod("getSkillLevel", Player.class, String.class)
                        .invoke(null, player, skillId);
                return result instanceof Number n ? n.intValue() : 0;
            } catch (Exception e) {
                logger.debug("EcoSkills getSkillLevel failed: {}", e.getMessage());
                return 0;
            }
        });
    }

    @Override
    public @NotNull CompletableFuture<List<SkillDescriptor>> getAvailableSkills() {
        return CompletableFuture.completedFuture(List.of());
    }

    @Override
    public @NotNull CompletableFuture<Boolean> addSkillLevels(@NotNull Player player,
                                                               @NotNull String skillId,
                                                               int levels) {
        return CompletableFuture.completedFuture(false);
    }

    @Override
    public @NotNull CompletableFuture<Boolean> consumeSkillLevel(@NotNull Player player,
                                                                  @NotNull String skillId,
                                                                  int levels) {
        return CompletableFuture.completedFuture(false);
    }
}
