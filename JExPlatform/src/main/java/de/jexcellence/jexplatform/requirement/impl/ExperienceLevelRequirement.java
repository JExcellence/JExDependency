package de.jexcellence.jexplatform.requirement.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.jexcellence.jexplatform.requirement.AbstractRequirement;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Requires the player to have a minimum experience level.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class ExperienceLevelRequirement extends AbstractRequirement {

    @JsonProperty("level")
    private final int level;

    /**
     * Creates an experience level requirement.
     *
     * @param level             the minimum level
     * @param consumeOnComplete whether to subtract levels on fulfillment
     */
    public ExperienceLevelRequirement(@JsonProperty("level") int level,
                                      @JsonProperty("consumeOnComplete") boolean consumeOnComplete) {
        super("EXPERIENCE_LEVEL", consumeOnComplete);
        this.level = Math.max(0, level);
    }

    @Override
    public boolean isMet(@NotNull Player player) {
        return player.getLevel() >= level;
    }

    @Override
    public double calculateProgress(@NotNull Player player) {
        if (level <= 0) {
            return 1.0;
        }
        return Math.min(1.0, (double) player.getLevel() / level);
    }

    @Override
    public void consume(@NotNull Player player) {
        if (shouldConsume()) {
            player.setLevel(player.getLevel() - level);
        }
    }

    @Override
    public @NotNull String descriptionKey() {
        return "requirement.experience_level";
    }
}
