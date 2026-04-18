package de.jexcellence.jexplatform.reward.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.jexcellence.jexplatform.reward.AbstractReward;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Spawns particles at the player's location.
 *
 * @author JExcellence
 * @since 1.0.0
 */
public class ParticleReward extends AbstractReward {

    @JsonProperty("particle") private final String particle;
    @JsonProperty("count") private final int count;
    @JsonProperty("offsetX") private final double offsetX;
    @JsonProperty("offsetY") private final double offsetY;
    @JsonProperty("offsetZ") private final double offsetZ;

    /**
     * Creates a particle reward.
     *
     * @param particle the particle type name
     * @param count    the number of particles
     * @param offsetX  the x offset
     * @param offsetY  the y offset
     * @param offsetZ  the z offset
     */
    public ParticleReward(@JsonProperty("particle") @NotNull String particle,
                          @JsonProperty("count") int count,
                          @JsonProperty("offsetX") double offsetX,
                          @JsonProperty("offsetY") double offsetY,
                          @JsonProperty("offsetZ") double offsetZ) {
        super("PARTICLE");
        this.particle = particle;
        this.count = Math.max(1, count);
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
    }

    @Override
    public @NotNull CompletableFuture<Boolean> grant(@NotNull Player player) {
        try {
            var p = Particle.valueOf(particle.toUpperCase());
            var loc = player.getLocation();
            player.getWorld().spawnParticle(p, loc, count, offsetX, offsetY, offsetZ);
            return CompletableFuture.completedFuture(true);
        } catch (IllegalArgumentException e) {
            return CompletableFuture.completedFuture(false);
        }
    }

    @Override
    public @NotNull String descriptionKey() {
        return "reward.particle";
    }
}
