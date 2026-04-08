package de.jexcellence.oneblock.visualization;

import de.jexcellence.oneblock.database.entity.generator.GeneratorDesign;
import de.jexcellence.oneblock.database.entity.generator.GeneratorDesignLayer;
import de.jexcellence.oneblock.database.entity.generator.PlayerGeneratorStructure;
import de.jexcellence.oneblock.visualization.effects.BuildParticleEffect;
import de.jexcellence.oneblock.visualization.effects.IdleParticleEffect;
import de.jexcellence.oneblock.visualization.effects.ValidationParticleEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Service for managing structure visualization effects.
 *
 * @author JExcellence
 * @version 2.0.0
 * @since 2.0.0
 */
public class StructureVisualizationService {
    
    private static final Logger LOGGER = Logger.getLogger("JExOneblock");
    
    private final Plugin plugin;
    private final BuildParticleEffect buildEffect;
    private final ValidationParticleEffect validationEffect;
    
    private final Map<UUID, PreviewSession> activePreviewSessions = new ConcurrentHashMap<>();
    private final Map<Long, IdleParticleEffect> activeIdleEffects = new ConcurrentHashMap<>();
    
    public StructureVisualizationService(@NotNull Plugin plugin) {
        this.plugin = plugin;
        this.buildEffect = new BuildParticleEffect();
        this.validationEffect = new ValidationParticleEffect();
    }
    
    // ==================== Preview Methods ====================
    
    /**
     * Shows a structure outline preview for a player.
     *
     * @param player the player
     * @param design the design to preview
     * @param location the preview location
     */
    public void showStructureOutline(@NotNull Player player, @NotNull GeneratorDesign design, @NotNull Location location) {
        hidePreview(player);
        
        PreviewSession session = new PreviewSession(player, design, location);
        activePreviewSessions.put(player.getUniqueId(), session);
        session.start();
    }
    
    /**
     * Shows a layer preview for a player.
     *
     * @param player the player
     * @param layer the layer to preview
     * @param location the base location
     */
    public void showLayerPreview(@NotNull Player player, @NotNull GeneratorDesignLayer layer, @NotNull Location location) {
        World world = location.getWorld();
        if (world == null) return;
        
        Material[][] pattern = layer.getPattern();
        if (pattern == null) return;
        
        int width = layer.getWidth();
        int depth = layer.getDepth();
        int startX = location.getBlockX() - width / 2;
        int startZ = location.getBlockZ() - depth / 2;
        int y = location.getBlockY() + layer.getLayerIndex();
        
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < depth; z++) {
                Material mat = pattern[x][z];
                if (mat != null && mat != Material.AIR) {
                    Location blockLoc = new Location(world, startX + x, y, startZ + z);
                    buildEffect.spawnForMaterial(blockLoc, mat);
                }
            }
        }
    }
    
    /**
     * Hides the preview for a player.
     *
     * @param player the player
     */
    public void hidePreview(@NotNull Player player) {
        PreviewSession session = activePreviewSessions.remove(player.getUniqueId());
        if (session != null) {
            session.stop();
        }
    }
    
    /**
     * Checks if a player has an active preview.
     *
     * @param player the player
     * @return true if preview is active
     */
    public boolean hasActivePreview(@NotNull Player player) {
        return activePreviewSessions.containsKey(player.getUniqueId());
    }
    
    // ==================== Build Effects ====================
    
    /**
     * Plays build particles for a block placement.
     *
     * @param location the block location
     * @param material the material being placed
     */
    public void playBuildParticles(@NotNull Location location, @NotNull Material material) {
        buildEffect.spawnForMaterial(location, material);
    }
    
    /**
     * Creates a particle trail from source to destination.
     *
     * @param from source location
     * @param to destination location
     */
    public void playBuildTrail(@NotNull Location from, @NotNull Location to) {
        buildEffect.createTrail(from, to, 2.0);
    }
    
    /**
     * Plays completion effect for a structure.
     *
     * @param location the center location
     * @param design the completed design
     */
    public void playCompletionEffect(@NotNull Location location, @NotNull GeneratorDesign design) {
        World world = location.getWorld();
        if (world == null) return;
        
        Location center = location.clone().add(
                design.getWidth() / 2.0,
                design.getHeight() / 2.0,
                design.getDepth() / 2.0
        );
        
        // Firework burst
        world.spawnParticle(org.bukkit.Particle.FIREWORK, center, 50, 2, 2, 2, 0.1);
        world.spawnParticle(org.bukkit.Particle.HAPPY_VILLAGER, center, 30, 2, 2, 2, 0);
        
        // Design-specific completion effect
        if (design.getDesignType() != null) {
            IdleParticleEffect tempEffect = new IdleParticleEffect(plugin, design.getDesignType(), center);
            tempEffect.spawn(center);
        }
    }
    
    // ==================== Validation Effects ====================
    
    /**
     * Plays validation effect for a block.
     *
     * @param location the block location
     * @param valid whether the block is valid
     */
    public void playValidationEffect(@NotNull Location location, boolean valid) {
        if (valid) {
            validationEffect.spawnValid(location);
        } else {
            validationEffect.spawnInvalid(location);
        }
    }
    
    /**
     * Plays missing block indicator.
     *
     * @param location the block location
     */
    public void playMissingBlockEffect(@NotNull Location location) {
        validationEffect.spawnMissing(location);
    }
    
    /**
     * Plays structure validation complete effect.
     *
     * @param location the center location
     * @param valid whether validation passed
     */
    public void playValidationComplete(@NotNull Location location, boolean valid) {
        validationEffect.spawnValidationComplete(location, valid);
    }
    
    // ==================== Idle Effects ====================
    
    /**
     * Starts idle particles for an active generator.
     *
     * @param structure the player's structure
     */
    public void startIdleParticles(@NotNull PlayerGeneratorStructure structure) {
        if (structure.getId() == null || structure.getDesign() == null) return;
        
        Location coreLoc = structure.getCoreLocation();
        if (coreLoc == null) return;
        
        stopIdleParticles(structure);
        
        IdleParticleEffect effect = new IdleParticleEffect(
                plugin,
                structure.getDesign().getDesignType(),
                coreLoc
        );
        activeIdleEffects.put(structure.getId(), effect);
        effect.start();
    }
    
    /**
     * Stops idle particles for a generator.
     *
     * @param structure the player's structure
     */
    public void stopIdleParticles(@NotNull PlayerGeneratorStructure structure) {
        if (structure.getId() == null) return;
        
        IdleParticleEffect effect = activeIdleEffects.remove(structure.getId());
        if (effect != null) {
            effect.stop();
        }
    }
    
    /**
     * Stops all idle particle effects.
     */
    public void stopAllIdleParticles() {
        for (IdleParticleEffect effect : activeIdleEffects.values()) {
            effect.stop();
        }
        activeIdleEffects.clear();
    }
    
    // ==================== Cleanup ====================
    
    /**
     * Shuts down the visualization service.
     */
    public void shutdown() {
        // Stop all previews
        for (PreviewSession session : activePreviewSessions.values()) {
            session.stop();
        }
        activePreviewSessions.clear();
        
        // Stop all idle effects
        stopAllIdleParticles();
    }
    
    // ==================== Preview Session ====================
    
    private class PreviewSession {
        private final Player player;
        private final GeneratorDesign design;
        private final Location location;
        private BukkitTask task;
        private int tickCounter = 0;
        
        PreviewSession(@NotNull Player player, @NotNull GeneratorDesign design, @NotNull Location location) {
            this.player = player;
            this.design = design;
            this.location = location;
        }
        
        void start() {
            task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 0L, 10L);
        }
        
        void stop() {
            if (task != null) {
                task.cancel();
                task = null;
            }
        }
        
        private void tick() {
            if (!player.isOnline()) {
                hidePreview(player);
                return;
            }
            
            tickCounter++;
            
            World world = location.getWorld();
            if (world == null) return;
            
            int width = design.getWidth();
            int height = design.getHeight();
            int depth = design.getDepth();
            
            int startX = location.getBlockX() - width / 2;
            int startZ = location.getBlockZ() - depth / 2;
            int startY = location.getBlockY();
            
            Location corner = new Location(world, startX, startY, startZ);
            
            // Draw outline
            validationEffect.spawnOutline(corner, width, height, depth, true);
            
            // Animate layer highlight
            int highlightLayer = tickCounter % height;
            for (int x = 0; x <= width; x++) {
                for (int z = 0; z <= depth; z++) {
                    if (x == 0 || x == width || z == 0 || z == depth) {
                        Location edgeLoc = corner.clone().add(x, highlightLayer, z);
                        world.spawnParticle(org.bukkit.Particle.END_ROD, edgeLoc, 1, 0, 0, 0, 0);
                    }
                }
            }
        }
    }
}
