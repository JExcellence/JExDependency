package de.jexcellence.jexplatform.reward.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.jexcellence.jexplatform.reward.AbstractReward;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Grants experience points or levels to the player.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class ExperienceReward extends AbstractReward {

    /** Whether to grant points or levels. */
    public enum ExperienceMode { POINTS, LEVELS }

    @JsonProperty("amount") private final int amount;
    @JsonProperty("mode") private final ExperienceMode mode;

    public ExperienceReward(@JsonProperty("amount") int amount,
                            @JsonProperty("mode") ExperienceMode mode) {
        super("EXPERIENCE");
        this.amount = amount;
        this.mode = mode != null ? mode : ExperienceMode.POINTS;
    }

    @Override
    public @NotNull CompletableFuture<Boolean> grant(@NotNull Player player) {
        switch (mode) {
            case POINTS -> player.giveExp(amount);
            case LEVELS -> player.setLevel(player.getLevel() + amount);
        }
        return CompletableFuture.completedFuture(true);
    }

    @Override public @NotNull String descriptionKey() { return "reward.experience"; }
    @Override public double estimatedValue() { return amount; }
}
