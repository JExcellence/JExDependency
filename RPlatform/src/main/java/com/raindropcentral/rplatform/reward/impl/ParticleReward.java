package com.raindropcentral.rplatform.reward.impl;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.raindropcentral.rplatform.reward.AbstractReward;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Reward that spawns particle effects around the player.
 */
@JsonTypeName("PARTICLE")
public final class ParticleReward extends AbstractReward {

    private final Particle particle;
    private final int count;
    private final double offsetX;
    private final double offsetY;
    private final double offsetZ;
    private final double extra;

    /**
     * Executes ParticleReward.
     */
    @JsonCreator
    public ParticleReward(
        @JsonProperty("particle") @NotNull Particle particle,
        @JsonProperty("count") int count,
        @JsonProperty("offsetX") double offsetX,
        @JsonProperty("offsetY") double offsetY,
        @JsonProperty("offsetZ") double offsetZ,
        @JsonProperty("extra") double extra
    ) {
        this.particle = particle;
        this.count = Math.max(1, count);
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.extra = extra;
    }

    /**
     * Gets typeId.
     */
    @Override
    public @NotNull String getTypeId() {
        return "PARTICLE";
    }

    /**
     * Executes grant.
     */
    @Override
    public @NotNull CompletableFuture<Boolean> grant(@NotNull Player player) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                player.getWorld().spawnParticle(
                    particle,
                    player.getLocation().add(0, 1, 0),
                    count,
                    offsetX,
                    offsetY,
                    offsetZ,
                    extra
                );
                return true;
            } catch (Exception e) {
                return false;
            }
        });
    }

    /**
     * Gets estimatedValue.
     */
    @Override
    public double getEstimatedValue() {
        return 0.0;
    }

    /**
     * Gets particle.
     */
    public Particle getParticle() {
        return particle;
    }

    /**
     * Gets count.
     */
    public int getCount() {
        return count;
    }

    /**
     * Gets offsetX.
     */
    public double getOffsetX() {
        return offsetX;
    }

    /**
     * Gets offsetY.
     */
    public double getOffsetY() {
        return offsetY;
    }

    /**
     * Gets offsetZ.
     */
    public double getOffsetZ() {
        return offsetZ;
    }

    /**
     * Gets extra.
     */
    public double getExtra() {
        return extra;
    }

    /**
     * Executes validate.
     */
    @Override
    public void validate() {
        if (particle == null) {
            throw new IllegalArgumentException("Particle cannot be null");
        }
    }
}
