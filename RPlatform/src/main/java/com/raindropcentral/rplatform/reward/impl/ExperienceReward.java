package com.raindropcentral.rplatform.reward.impl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.raindropcentral.rplatform.reward.AbstractReward;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Reward that grants experience points or levels to a player.
 * <p>
 * This reward ensures that experience is granted on the main thread
 * to comply with Bukkit API requirements.
 * </p>
 *
 * @author RaindropCentral
 * @version 2.0.0
 * @since TBD
 */
public final class ExperienceReward extends AbstractReward {

    private static final Logger LOGGER = Logger.getLogger(ExperienceReward.class.getName());

    /**
     * Type of experience to grant.
     */
    public enum ExperienceType {
        /**
         * Experience points (raw XP).
         */
        POINTS,

        /**
         * Experience levels.
         */
        LEVELS
    }

    private final int amount;
    private final ExperienceType type;

    @JsonCreator
    public ExperienceReward(
        @JsonProperty("amount") int amount,
        @JsonProperty("experienceType") ExperienceType type
    ) {
        this.amount = amount;
        this.type = type != null ? type : ExperienceType.POINTS;
    }

    @Override
    public @NotNull String getTypeId() {
        return "EXPERIENCE";
    }

    @Override
    public @NotNull CompletableFuture<Boolean> grant(@NotNull Player player) {
        final CompletableFuture<Boolean> future = new CompletableFuture<>();

        // Get any plugin for scheduling (use first available)
        final Plugin plugin = Bukkit.getPluginManager().getPlugins()[0];

        // Must run on main thread
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                if (type == ExperienceType.LEVELS) {
                    player.giveExpLevels(amount);
                    LOGGER.fine("Granted " + amount + " experience levels to " + player.getName());
                } else {
                    player.giveExp(amount);
                    LOGGER.fine("Granted " + amount + " experience points to " + player.getName());
                }
                future.complete(true);
            } catch (Exception e) {
                LOGGER.warning("Failed to grant experience to " + player.getName() + ": " + e.getMessage());
                future.complete(false);
            }
        });

        return future;
    }

    @Override
    public double getEstimatedValue() {
        return type == ExperienceType.LEVELS ? amount * 100.0 : amount;
    }

    public int getAmount() {
        return amount;
    }

    public ExperienceType getExperienceType() {
        return type;
    }

    @Override
    public void validate() {
        if (amount <= 0) {
            throw new IllegalArgumentException("Experience amount must be positive");
        }
    }
}
