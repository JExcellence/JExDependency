package de.jexcellence.oneblock.visualization.effects;

import de.jexcellence.oneblock.visualization.GeneratorParticleEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

/**
 * Particle effect for structure building animations.
 *
 * @author JExcellence
 * @version 2.0.0
 * @since 2.0.0
 */
public class BuildParticleEffect extends GeneratorParticleEffect {
    
    public BuildParticleEffect() {
        super(Particle.CLOUD, 5, 0.2, 0.2, 0.2, 0.02);
    }
    
    public BuildParticleEffect(@NotNull Particle particle, int count) {
        super(particle, count, 0.2, 0.2, 0.2, 0.02);
    }
    
    /**
     * Spawns build particles for a specific material.
     *
     * @param location the location
     * @param material the material being placed
     */
    public void spawnForMaterial(@NotNull Location location, @NotNull Material material) {
        World world = location.getWorld();
        if (world == null) return;
        
        Location center = location.clone().add(0.5, 0.5, 0.5);
        Particle materialParticle = getParticleForMaterial(material);
        
        // Material-specific particles
        world.spawnParticle(materialParticle, center, count, offsetX, offsetY, offsetZ, speed);
        
        // Success sparkles
        world.spawnParticle(Particle.HAPPY_VILLAGER, center, 3, 0.15, 0.15, 0.15, 0);
    }
    
    /**
     * Creates a particle trail from source to destination.
     *
     * @param from source location
     * @param to destination location
     * @param density particles per block
     */
    public void createTrail(@NotNull Location from, @NotNull Location to, double density) {
        World world = from.getWorld();
        if (world == null) return;
        
        org.bukkit.util.Vector direction = to.toVector().subtract(from.toVector()).normalize();
        double distance = from.distance(to);
        int particleCount = (int) (distance * density);
        
        for (int i = 0; i < particleCount; i++) {
            double d = (double) i / particleCount * distance;
            Location particleLoc = from.clone().add(direction.clone().multiply(d));
            world.spawnParticle(Particle.ENCHANT, particleLoc, 1, 0.05, 0.05, 0.05, 0.05);
        }
    }
    
    /**
     * Spawns placement completion effect.
     *
     * @param location the block location
     */
    public void spawnPlacementComplete(@NotNull Location location) {
        World world = location.getWorld();
        if (world == null) return;
        
        Location center = location.clone().add(0.5, 0.5, 0.5);
        world.spawnParticle(Particle.HAPPY_VILLAGER, center, 8, 0.3, 0.3, 0.3, 0);
        world.spawnParticle(Particle.END_ROD, center, 3, 0.2, 0.2, 0.2, 0.02);
    }
    
    @NotNull
    private Particle getParticleForMaterial(@NotNull Material material) {
        return switch (material) {
            case WATER -> Particle.DRIPPING_WATER;
            case LAVA -> Particle.LAVA;
            case BEACON -> Particle.END_ROD;
            case NETHERITE_BLOCK -> Particle.SOUL_FIRE_FLAME;
            case DIAMOND_BLOCK, EMERALD_BLOCK -> Particle.FIREWORK;
            case GOLD_BLOCK -> Particle.ENCHANTED_HIT;
            case IRON_BLOCK -> Particle.CRIT;
            case PRISMARINE, PRISMARINE_BRICKS, DARK_PRISMARINE -> Particle.DRIPPING_WATER;
            case MAGMA_BLOCK -> Particle.FLAME;
            case AMETHYST_BLOCK -> Particle.PORTAL;
            case MOSS_BLOCK -> Particle.COMPOSTER;
            case BLACKSTONE, GILDED_BLACKSTONE -> Particle.SMOKE;
            case END_STONE, PURPUR_BLOCK -> Particle.PORTAL;
            case DEEPSLATE, REINFORCED_DEEPSLATE -> Particle.ASH;
            default -> Particle.CLOUD;
        };
    }
}
