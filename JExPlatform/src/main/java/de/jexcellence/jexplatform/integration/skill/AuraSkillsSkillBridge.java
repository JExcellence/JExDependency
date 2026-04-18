package de.jexcellence.jexplatform.integration.skill;

import de.jexcellence.jexplatform.logging.JExLogger;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * AuraSkills reflection-based skill bridge.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public final class AuraSkillsSkillBridge extends AbstractReflectionSkillBridge
        implements SkillBridge {

    /**
     * Creates the AuraSkills bridge.
     *
     * @param logger the platform logger
     */
    AuraSkillsSkillBridge(@NotNull JExLogger logger) {
        super(logger);
    }

    @Override
    public @NotNull String pluginName() {
        return "AuraSkills";
    }

    @Override
    public boolean isAvailable() {
        return findClass("dev.aurelium.auraskills.api.AuraSkillsApi") != null;
    }

    @Override
    public @NotNull CompletableFuture<Integer> getSkillLevel(@NotNull Player player,
                                                              @NotNull String skillId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var apiClass = Class.forName("dev.aurelium.auraskills.api.AuraSkillsApi");
                var api = apiClass.getMethod("get").invoke(null);
                var userData = api.getClass().getMethod("getUser", java.util.UUID.class)
                        .invoke(api, player.getUniqueId());
                var skillsClass = Class.forName("dev.aurelium.auraskills.api.skill.Skills");
                var skill = resolveEnum(skillsClass, skillId);
                if (skill == null) return 0;
                var level = userData.getClass().getMethod("getSkillLevel", skill.getClass())
                        .invoke(userData, skill);
                return level instanceof Number n ? n.intValue() : 0;
            } catch (Exception e) {
                logger.debug("AuraSkills getSkillLevel failed: {}", e.getMessage());
                return 0;
            }
        });
    }

    @Override
    public @NotNull CompletableFuture<List<SkillDescriptor>> getAvailableSkills() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var skillsClass = Class.forName("dev.aurelium.auraskills.api.skill.Skills");
                var constants = skillsClass.getEnumConstants();
                if (constants == null) return List.<SkillDescriptor>of();
                return java.util.Arrays.stream(constants)
                        .map(c -> new SkillDescriptor(
                                ((Enum<?>) c).name(), ((Enum<?>) c).name()))
                        .toList();
            } catch (Exception e) {
                logger.debug("AuraSkills getAvailableSkills failed: {}", e.getMessage());
                return List.of();
            }
        });
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
