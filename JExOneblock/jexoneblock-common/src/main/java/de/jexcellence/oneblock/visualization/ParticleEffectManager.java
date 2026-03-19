package de.jexcellence.oneblock.visualization;

import de.jexcellence.oneblock.database.entity.generator.EGeneratorDesignType;
import de.jexcellence.oneblock.database.entity.generator.GeneratorDesign;
import de.jexcellence.oneblock.database.entity.generator.PlayerGeneratorStructure;
import de.jexcellence.oneblock.visualization.effects.BuildParticleEffect;
import de.jexcellence.oneblock.visualization.effects.IdleParticleEffect;
import de.jexcellence.oneblock.visualization.effects.ValidationParticleEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Central manager for all generator particle effects.
 * <p>
 * Provides configuration and coordination of particle effects across the system.
 * </p>
 *
 * @author JExcellence
 * @version 2.0.0
 * @since 2.0.0
 */
public class ParticleEffectManager {
    
    private static final Logger LOGGER = Logger.getLogger("JExOneblock");
    
    private final Plugin plugin;
    private final StructureVisualizationService visualizationService;
    
    // Configuration
    private boolean effectsEnabled = true;
    private double particleDensity = 1.0;
    private int maxParticlesPerTick = 50;
    
    // Per-design particle configurations
    private final Map<EGeneratorDesignType, ParticleConfig> designConfigs = new EnumMap<>(EGeneratorDesignType.class);
    
    // Active effect tracking
    private final Map<Long, IdleParticleEffect> activeGeneratorEffects = new ConcurrentHashMap<>();
    
    public ParticleEffectManager(@NotNull Plugin plugin) {
        this.plugin = plugin;
        this.visualizationService = new StructureVisualizationService(plugin);
        initializeDefaultConfigs();
    }
    
    private void initializeDefaultConfigs() {
        // Initialize default particle configurations for each design type
        designConfigs.put(EGeneratorDesignType.FOUNDRY, new ParticleConfig(Particle.FLAME, 5, 0.2));
        designConfigs.put(EGeneratorDesignType.AQUATIC, new ParticleConfig(Particle.DRIPPING_WATER, 8, 0.3));
        designConfigs.put(EGeneratorDesignType.VOLCANIC, new ParticleConfig(Particle.LAVA, 4, 0.2));
        designConfigs.put(EGeneratorDesignType.CRYSTAL, new ParticleConfig(Particle.END_ROD, 6, 0.25));
        designConfigs.put(EGeneratorDesignType.MECHANICAL, new ParticleConfig(Particle.CRIT, 7, 0.2));
        designConfigs.put(EGeneratorDesignType.NATURE, new ParticleConfig(Particle.COMPOSTER, 5, 0.3));
        designConfigs.put(EGeneratorDesignType.NETHER, new ParticleConfig(Particle.SOUL_FIRE_FLAME, 5, 0.2));
        designConfigs.put(EGeneratorDesignType.END, new ParticleConfig(Particle.PORTAL, 10, 0.4));
        designConfigs.put(EGeneratorDesignType.ANCIENT, new ParticleConfig(Particle.SCULK_CHARGE_POP, 4, 0.2));
        designConfigs.put(EGeneratorDesignType.CELESTIAL, new ParticleConfig(Particle.END_ROD, 8, 0.3));
    }
    
    // ==================== Configuration ====================
    
    /**
     * Enables or disables all particle effects.
     *
     * @param enabled true to enable
     */
    public void setEffectsEnabled(boolean enabled) {
        this.effectsEnabled = enabled;
        if (!enabled) {
            stopAllEffects();
        }
    }
    
    /**
     * Sets the particle density multiplier.
     *
     * @param density density multiplier (0.0 to 2.0)
     */
    public void setParticleDensity(double density) {
        this.particleDensity = Math.max(0.0, Math.min(2.0, density));
    }
    
    /**
     * Sets the maximum particles per tick.
     *
     * @param max maximum particles
     */
    public void setMaxParticlesPerTick(int max) {
        this.maxParticlesPerTick = Math.max(10, max);
    }
    
    /**
     * Sets the particle configuration for a design type.
     *
     * @param type the design type
     * @param config the particle configuration
     */
    public void setDesignConfig(@NotNull EGeneratorDesignType type, @NotNull ParticleConfig config) {
        designConfigs.put(type, config);
    }
    
    /**
     * Gets the particle configuration for a design type.
     *
     * @param type the design type
     * @return the configuration, or null if not set
     */
    @Nullable
    public ParticleConfig getDesignConfig(@NotNull EGeneratorDesignType type) {
        return designConfigs.get(type);
    }
    
    // ==================== Effect Methods ====================
    
    /**
     * Shows a structure preview for a player.
     *
     * @param player the player
     * @param design the design
     * @param location the location
     */
    public void showPreview(@NotNull Player player, @NotNull GeneratorDesign design, @NotNull Location location) {
        if (!effectsEnabled) return;
        visualizationService.showStructureOutline(player, design, location);
    }
    
    /**
     * Hides the preview for a player.
     *
     * @param player the player
     */
    public void hidePreview(@NotNull Player player) {
        visualizationService.hidePreview(player);
    }
    
    /**
     * Plays build particles.
     *
     * @param location the location
     * @param material the material
     */
    public void playBuildEffect(@NotNull Location location, @NotNull Material material) {
        if (!effectsEnabled) return;
        visualizationService.playBuildParticles(location, material);
    }
    
    /**
     * Plays build trail effect.
     *
     * @param from source location
     * @param to destination location
     */
    public void playBuildTrail(@NotNull Location from, @NotNull Location to) {
        if (!effectsEnabled) return;
        visualizationService.playBuildTrail(from, to);
    }
    
    /**
     * Plays completion effect.
     *
     * @param location the location
     * @param design the design
     */
    public void playCompletionEffect(@NotNull Location location, @NotNull GeneratorDesign design) {
        if (!effectsEnabled) return;
        visualizationService.playCompletionEffect(location, design);
    }
    
    /**
     * Plays validation effect.
     *
     * @param location the location
     * @param valid whether valid
     */
    public void playValidationEffect(@NotNull Location location, boolean valid) {
        if (!effectsEnabled) return;
        visualizationService.playValidationEffect(location, valid);
    }
    
    /**
     * Starts idle effect for a generator.
     *
     * @param structure the structure
     */
    public void startIdleEffect(@NotNull PlayerGeneratorStructure structure) {
        if (!effectsEnabled) return;
        visualizationService.startIdleParticles(structure);
    }
    
    /**
     * Stops idle effect for a generator.
     *
     * @param structure the structure
     */
    public void stopIdleEffect(@NotNull PlayerGeneratorStructure structure) {
        visualizationService.stopIdleParticles(structure);
    }
    
    /**
     * Stops all active effects.
     */
    public void stopAllEffects() {
        visualizationService.stopAllIdleParticles();
        // Clear all preview sessions handled by visualization service
    }
    
    // ==================== Lifecycle ====================
    
    /**
     * Shuts down the particle effect manager.
     */
    public void shutdown() {
        stopAllEffects();
        visualizationService.shutdown();
    }
    
    // ==================== Getters ====================
    
    public boolean isEffectsEnabled() {
        return effectsEnabled;
    }
    
    public double getParticleDensity() {
        return particleDensity;
    }
    
    public int getMaxParticlesPerTick() {
        return maxParticlesPerTick;
    }
    
    @NotNull
    public StructureVisualizationService getVisualizationService() {
        return visualizationService;
    }
    
    // ==================== Particle Config ====================
    
    /**
     * Configuration for design-specific particles.
     */
    public record ParticleConfig(
            @NotNull Particle particle,
            int count,
            double spread
    ) {
        public ParticleConfig {
            if (count < 1) count = 1;
            if (spread < 0) spread = 0;
        }
    }
}
