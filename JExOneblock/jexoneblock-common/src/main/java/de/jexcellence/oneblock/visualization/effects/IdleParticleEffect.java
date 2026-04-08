package de.jexcellence.oneblock.visualization.effects;

import de.jexcellence.oneblock.database.entity.generator.EGeneratorDesignType;
import de.jexcellence.oneblock.visualization.GeneratorParticleEffect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

/**
 * Particle effect for active/idle generators.
 *
 * @author JExcellence
 * @version 2.0.0
 * @since 2.0.0
 */
public class IdleParticleEffect extends GeneratorParticleEffect {
    
    private final Plugin plugin;
    private final EGeneratorDesignType designType;
    private final Location coreLocation;
    private BukkitTask task;
    private int tickCounter = 0;
    
    public IdleParticleEffect(
            @NotNull Plugin plugin,
            @NotNull EGeneratorDesignType designType,
            @NotNull Location coreLocation
    ) {
        super(getParticleForDesign(designType), 3, 0.2, 0.2, 0.2, 0.02);
        this.plugin = plugin;
        this.designType = designType;
        this.coreLocation = coreLocation.clone().add(0.5, 0.5, 0.5);
    }
    
    @Override
    public void start() {
        if (active) return;
        super.start();
        
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 0L, 5L);
    }
    
    @Override
    public void stop() {
        super.stop();
        if (task != null) {
            task.cancel();
            task = null;
        }
    }
    
    private void tick() {
        if (!active) return;
        
        World world = coreLocation.getWorld();
        if (world == null) {
            stop();
            return;
        }
        
        tickCounter++;
        
        // Main particle effect
        world.spawnParticle(particle, coreLocation, count, offsetX, offsetY, offsetZ, speed);
        
        // Design-specific ambient effects
        spawnAmbientEffect(world);
        
        // Periodic burst effect every 4 seconds
        if (tickCounter % 80 == 0) {
            spawnBurstEffect(world);
        }
    }
    
    private void spawnAmbientEffect(@NotNull World world) {
        switch (designType) {
            case FOUNDRY -> {
                if (tickCounter % 4 == 0) {
                    world.spawnParticle(Particle.FLAME, coreLocation.clone().add(0, 0.3, 0), 1, 0.1, 0, 0.1, 0.01);
                }
            }
            case AQUATIC -> {
                world.spawnParticle(Particle.DRIPPING_WATER, coreLocation.clone().add(0, 0.5, 0), 1, 0.3, 0.1, 0.3, 0);
            }
            case VOLCANIC -> {
                if (tickCounter % 2 == 0) {
                    world.spawnParticle(Particle.LAVA, coreLocation, 1, 0.2, 0.1, 0.2, 0);
                    world.spawnParticle(Particle.SMOKE, coreLocation.clone().add(0, 0.5, 0), 1, 0.1, 0.2, 0.1, 0.01);
                }
            }
            case CRYSTAL -> {
                double angle = tickCounter * 0.1;
                double x = Math.cos(angle) * 0.3;
                double z = Math.sin(angle) * 0.3;
                world.spawnParticle(Particle.END_ROD, coreLocation.clone().add(x, 0.3, z), 1, 0, 0, 0, 0);
            }
            case MECHANICAL -> {
                if (tickCounter % 3 == 0) {
                    world.spawnParticle(Particle.CRIT, coreLocation, 2, 0.2, 0.2, 0.2, 0.05);
                }
            }
            case NATURE -> {
                world.spawnParticle(Particle.COMPOSTER, coreLocation.clone().add(0, 0.3, 0), 1, 0.3, 0.1, 0.3, 0);
            }
            case NETHER -> {
                world.spawnParticle(Particle.SOUL_FIRE_FLAME, coreLocation, 1, 0.2, 0.1, 0.2, 0.01);
            }
            case END -> {
                world.spawnParticle(Particle.PORTAL, coreLocation, 2, 0.3, 0.3, 0.3, 0.5);
            }
            case ANCIENT -> {
                if (tickCounter % 4 == 0) {
                    world.spawnParticle(Particle.SCULK_CHARGE_POP, coreLocation, 1, 0.2, 0.2, 0.2, 0);
                }
            }
            case CELESTIAL -> {
                double angle = tickCounter * 0.05;
                for (int i = 0; i < 3; i++) {
                    double a = angle + (i * Math.PI * 2 / 3);
                    double x = Math.cos(a) * 0.5;
                    double z = Math.sin(a) * 0.5;
                    world.spawnParticle(Particle.END_ROD, coreLocation.clone().add(x, 0.5, z), 1, 0, 0, 0, 0);
                }
            }
        }
    }
    
    private void spawnBurstEffect(@NotNull World world) {
        switch (designType) {
            case FOUNDRY -> world.spawnParticle(Particle.FLAME, coreLocation, 15, 0.3, 0.3, 0.3, 0.05);
            case AQUATIC -> world.spawnParticle(Particle.DRIPPING_WATER, coreLocation, 20, 0.5, 0.3, 0.5, 0.1);
            case VOLCANIC -> world.spawnParticle(Particle.LAVA, coreLocation, 10, 0.3, 0.5, 0.3, 0);
            case CRYSTAL -> world.spawnParticle(Particle.END_ROD, coreLocation, 15, 0.4, 0.4, 0.4, 0.05);
            case MECHANICAL -> world.spawnParticle(Particle.CRIT, coreLocation, 20, 0.4, 0.4, 0.4, 0.1);
            case NATURE -> world.spawnParticle(Particle.HAPPY_VILLAGER, coreLocation, 15, 0.5, 0.5, 0.5, 0);
            case NETHER -> world.spawnParticle(Particle.SOUL_FIRE_FLAME, coreLocation, 15, 0.4, 0.4, 0.4, 0.05);
            case END -> world.spawnParticle(Particle.REVERSE_PORTAL, coreLocation, 20, 0.5, 0.5, 0.5, 0.1);
            case ANCIENT -> world.spawnParticle(Particle.SCULK_SOUL, coreLocation, 10, 0.3, 0.3, 0.3, 0.02);
            case CELESTIAL -> world.spawnParticle(Particle.FIREWORK, coreLocation, 25, 0.5, 0.5, 0.5, 0.1);
        }
    }
    
    @NotNull
    private static Particle getParticleForDesign(@NotNull EGeneratorDesignType type) {
        return switch (type) {
            case FOUNDRY -> Particle.FLAME;
            case AQUATIC -> Particle.DRIPPING_WATER;
            case VOLCANIC -> Particle.LAVA;
            case CRYSTAL -> Particle.END_ROD;
            case MECHANICAL -> Particle.CRIT;
            case NATURE -> Particle.COMPOSTER;
            case NETHER -> Particle.SOUL_FIRE_FLAME;
            case END -> Particle.PORTAL;
            case ANCIENT -> Particle.SCULK_CHARGE_POP;
            case CELESTIAL -> Particle.END_ROD;
        };
    }
    
    @NotNull
    public EGeneratorDesignType getDesignType() {
        return designType;
    }
    
    @NotNull
    public Location getCoreLocation() {
        return coreLocation.clone();
    }
}
