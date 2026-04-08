package de.jexcellence.oneblock.visualization.effects;

import de.jexcellence.oneblock.visualization.GeneratorParticleEffect;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

/**
 * Particle effect for structure validation feedback.
 *
 * @author JExcellence
 * @version 2.0.0
 * @since 2.0.0
 */
public class ValidationParticleEffect extends GeneratorParticleEffect {
    
    private static final Particle.DustOptions VALID_DUST = new Particle.DustOptions(Color.GREEN, 1.0f);
    private static final Particle.DustOptions INVALID_DUST = new Particle.DustOptions(Color.RED, 1.0f);
    private static final Particle.DustOptions MISSING_DUST = new Particle.DustOptions(Color.YELLOW, 1.0f);
    
    public ValidationParticleEffect() {
        super(Particle.DUST, 5, 0.1, 0.1, 0.1, 0);
    }
    
    /**
     * Spawns valid block indicator.
     *
     * @param location the block location
     */
    public void spawnValid(@NotNull Location location) {
        World world = location.getWorld();
        if (world == null) return;
        
        Location center = location.clone().add(0.5, 0.5, 0.5);
        world.spawnParticle(Particle.DUST, center, count, offsetX, offsetY, offsetZ, speed, VALID_DUST);
        world.spawnParticle(Particle.HAPPY_VILLAGER, center, 2, 0.1, 0.1, 0.1, 0);
    }
    
    /**
     * Spawns invalid block indicator.
     *
     * @param location the block location
     */
    public void spawnInvalid(@NotNull Location location) {
        World world = location.getWorld();
        if (world == null) return;
        
        Location center = location.clone().add(0.5, 0.5, 0.5);
        world.spawnParticle(Particle.DUST, center, count, offsetX, offsetY, offsetZ, speed, INVALID_DUST);
        world.spawnParticle(Particle.SMOKE, center, 3, 0.1, 0.1, 0.1, 0.02);
    }
    
    /**
     * Spawns missing block indicator.
     *
     * @param location the block location
     */
    public void spawnMissing(@NotNull Location location) {
        World world = location.getWorld();
        if (world == null) return;
        
        Location center = location.clone().add(0.5, 0.5, 0.5);
        world.spawnParticle(Particle.DUST, center, count, offsetX, offsetY, offsetZ, speed, MISSING_DUST);
    }
    
    /**
     * Spawns structure outline at a location.
     *
     * @param location the corner location
     * @param width structure width
     * @param height structure height
     * @param depth structure depth
     * @param valid whether the structure is valid
     */
    public void spawnOutline(@NotNull Location location, int width, int height, int depth, boolean valid) {
        World world = location.getWorld();
        if (world == null) return;
        
        Particle.DustOptions dust = valid ? VALID_DUST : INVALID_DUST;
        
        // Draw edges of the bounding box
        for (int i = 0; i <= width; i++) {
            // Bottom edges
            spawnEdgeParticle(world, location.clone().add(i, 0, 0), dust);
            spawnEdgeParticle(world, location.clone().add(i, 0, depth), dust);
            // Top edges
            spawnEdgeParticle(world, location.clone().add(i, height, 0), dust);
            spawnEdgeParticle(world, location.clone().add(i, height, depth), dust);
        }
        
        for (int i = 0; i <= depth; i++) {
            // Bottom edges
            spawnEdgeParticle(world, location.clone().add(0, 0, i), dust);
            spawnEdgeParticle(world, location.clone().add(width, 0, i), dust);
            // Top edges
            spawnEdgeParticle(world, location.clone().add(0, height, i), dust);
            spawnEdgeParticle(world, location.clone().add(width, height, i), dust);
        }
        
        for (int i = 0; i <= height; i++) {
            // Vertical edges
            spawnEdgeParticle(world, location.clone().add(0, i, 0), dust);
            spawnEdgeParticle(world, location.clone().add(width, i, 0), dust);
            spawnEdgeParticle(world, location.clone().add(0, i, depth), dust);
            spawnEdgeParticle(world, location.clone().add(width, i, depth), dust);
        }
    }
    
    private void spawnEdgeParticle(@NotNull World world, @NotNull Location location, @NotNull Particle.DustOptions dust) {
        world.spawnParticle(Particle.DUST, location, 1, 0, 0, 0, 0, dust);
    }
    
    /**
     * Spawns validation complete effect.
     *
     * @param location the center location
     * @param valid whether validation passed
     */
    public void spawnValidationComplete(@NotNull Location location, boolean valid) {
        World world = location.getWorld();
        if (world == null) return;
        
        if (valid) {
            world.spawnParticle(Particle.HAPPY_VILLAGER, location, 20, 1, 1, 1, 0);
            world.spawnParticle(Particle.FIREWORK, location, 10, 0.5, 0.5, 0.5, 0.1);
        } else {
            world.spawnParticle(Particle.SMOKE, location, 15, 1, 1, 1, 0.05);
            world.spawnParticle(Particle.DUST, location, 10, 1, 1, 1, 0, INVALID_DUST);
        }
    }
}
