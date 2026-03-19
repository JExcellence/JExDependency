package de.jexcellence.oneblock.visualization;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Base class for generator particle effects.
 *
 * @author JExcellence
 * @version 2.0.0
 * @since 2.0.0
 */
public abstract class GeneratorParticleEffect {
    
    protected final Particle particle;
    protected final int count;
    protected final double offsetX;
    protected final double offsetY;
    protected final double offsetZ;
    protected final double speed;
    protected boolean active = false;
    
    protected GeneratorParticleEffect(
            @NotNull Particle particle,
            int count,
            double offsetX,
            double offsetY,
            double offsetZ,
            double speed
    ) {
        this.particle = particle;
        this.count = count;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.speed = speed;
    }
    
    /**
     * Spawns the particle effect at a location.
     *
     * @param location the location
     */
    public void spawn(@NotNull Location location) {
        if (location.getWorld() == null) return;
        location.getWorld().spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, speed);
    }
    
    /**
     * Spawns the particle effect for a specific player.
     *
     * @param player the player
     * @param location the location
     */
    public void spawnForPlayer(@NotNull Player player, @NotNull Location location) {
        player.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, speed);
    }
    
    /**
     * Starts the effect (for continuous effects).
     */
    public void start() {
        this.active = true;
    }
    
    /**
     * Stops the effect.
     */
    public void stop() {
        this.active = false;
    }
    
    /**
     * Checks if the effect is active.
     *
     * @return true if active
     */
    public boolean isActive() {
        return active;
    }
    
    /**
     * Gets the particle type.
     *
     * @return the particle
     */
    @NotNull
    public Particle getParticle() {
        return particle;
    }
    
    /**
     * Gets the particle count.
     *
     * @return the count
     */
    public int getCount() {
        return count;
    }
}
