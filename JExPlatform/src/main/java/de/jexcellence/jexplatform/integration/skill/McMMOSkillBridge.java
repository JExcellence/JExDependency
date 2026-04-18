package de.jexcellence.jexplatform.integration.skill;

import de.jexcellence.jexplatform.logging.JExLogger;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * mcMMO reflection-based skill bridge.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public final class McMMOSkillBridge extends AbstractReflectionSkillBridge
        implements SkillBridge {

    /**
     * Creates the mcMMO bridge.
     *
     * @param logger the platform logger
     */
    McMMOSkillBridge(@NotNull JExLogger logger) {
        super(logger);
    }

    @Override
    public @NotNull String pluginName() {
        return "mcMMO";
    }

    @Override
    public boolean isAvailable() {
        return findClass("com.gmail.nossr50.api.ExperienceAPI") != null;
    }

    @Override
    public @NotNull CompletableFuture<Integer> getSkillLevel(@NotNull Player player,
                                                              @NotNull String skillId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var apiClass = Class.forName("com.gmail.nossr50.api.ExperienceAPI");
                var result = apiClass.getMethod("getLevel", Player.class, String.class)
                        .invoke(null, player, skillId);
                return result instanceof Number n ? n.intValue() : 0;
            } catch (Exception e) {
                logger.debug("mcMMO getSkillLevel failed: {}", e.getMessage());
                return 0;
            }
        });
    }

    @Override
    public @NotNull CompletableFuture<List<SkillDescriptor>> getAvailableSkills() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var skillTypeClass = Class.forName(
                        "com.gmail.nossr50.datatypes.skills.PrimarySkillType");
                var constants = skillTypeClass.getEnumConstants();
                if (constants == null) return List.<SkillDescriptor>of();
                return java.util.Arrays.stream(constants)
                        .map(c -> new SkillDescriptor(
                                ((Enum<?>) c).name(), ((Enum<?>) c).name()))
                        .toList();
            } catch (Exception e) {
                logger.debug("mcMMO getAvailableSkills failed: {}", e.getMessage());
                return List.of();
            }
        });
    }

    @Override
    public @NotNull CompletableFuture<Boolean> addSkillLevels(@NotNull Player player,
                                                               @NotNull String skillId,
                                                               int levels) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var apiClass = Class.forName("com.gmail.nossr50.api.ExperienceAPI");
                apiClass.getMethod("addLevel", Player.class, String.class, int.class)
                        .invoke(null, player, skillId, levels);
                return true;
            } catch (Exception e) {
                logger.debug("mcMMO addSkillLevels failed: {}", e.getMessage());
                return false;
            }
        });
    }

    @Override
    public @NotNull CompletableFuture<Boolean> consumeSkillLevel(@NotNull Player player,
                                                                  @NotNull String skillId,
                                                                  int levels) {
        return CompletableFuture.completedFuture(false);
    }
}
