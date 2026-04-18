package de.jexcellence.jexplatform.requirement.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.jexcellence.jexplatform.requirement.AbstractRequirement;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Requires a minimum playtime based on Bukkit's {@link Statistic#PLAY_ONE_MINUTE}.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class PlaytimeRequirement extends AbstractRequirement {

    @JsonProperty("seconds")
    private final long seconds;

    /**
     * Creates a playtime requirement.
     *
     * @param seconds the minimum playtime in seconds
     */
    public PlaytimeRequirement(@JsonProperty("seconds") long seconds) {
        super("PLAYTIME");
        this.seconds = Math.max(0, seconds);
    }

    @Override
    public boolean isMet(@NotNull Player player) {
        return getPlaytimeSeconds(player) >= seconds;
    }

    @Override
    public double calculateProgress(@NotNull Player player) {
        if (seconds <= 0) {
            return 1.0;
        }
        return Math.min(1.0, (double) getPlaytimeSeconds(player) / seconds);
    }

    @Override
    public void consume(@NotNull Player player) {
        // Playtime is not consumable
    }

    @Override
    public @NotNull String descriptionKey() {
        return "requirement.playtime";
    }

    @SuppressWarnings("deprecation")
    private long getPlaytimeSeconds(@NotNull Player player) {
        // PLAY_ONE_MINUTE counts in ticks (despite the name)
        return player.getStatistic(Statistic.PLAY_ONE_MINUTE) / 20L;
    }
}
