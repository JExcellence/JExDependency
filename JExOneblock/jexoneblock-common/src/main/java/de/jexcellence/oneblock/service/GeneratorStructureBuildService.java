package de.jexcellence.oneblock.service;

import de.jexcellence.oneblock.database.entity.generator.GeneratorDesign;
import de.jexcellence.oneblock.database.entity.generator.GeneratorDesignLayer;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Service for building generator structures using the new GeneratorDesign entities.
 * <p>
 * Handles animated building with particle effects and material consumption.
 * </p>
 *
 * @author JExcellence
 * @version 2.0.0
 * @since 2.0.0
 */
public class GeneratorStructureBuildService {

    private static final Logger LOGGER = Logger.getLogger("JExOneblock");
    private static final int DEFAULT_ANIMATION_SPEED = 5; // ticks between block placements

    private final Plugin plugin;
    private final Map<UUID, BuildProcess> activeBuildProcesses = new ConcurrentHashMap<>();
    
    private int animationSpeed = DEFAULT_ANIMATION_SPEED;
    private boolean soundEnabled = true;
    private double particleDensity = 1.0;

    public GeneratorStructureBuildService(@NotNull Plugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Checks if the build area is clear for a design at a location.
     *
     * @param centerLocation the center location
     * @param design the design to build
     * @return true if area is clear
     */
    public boolean isBuildAreaClear(@NotNull Location centerLocation, @NotNull GeneratorDesign design) {
        World world = centerLocation.getWorld();
        if (world == null) return false;

        int width = design.getWidth();
        int depth = design.getDepth();
        int height = design.getHeight();
        
        int startX = centerLocation.getBlockX() - width / 2;
        int startZ = centerLocation.getBlockZ() - depth / 2;
        int startY = centerLocation.getBlockY();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                for (int z = 0; z < depth; z++) {
                    Location loc = new Location(world, startX + x, startY + y, startZ + z);
                    if (loc.getBlock().getType() != Material.AIR) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Gets the required materials for a design.
     *
     * @param design the design
     * @return map of materials to required amounts
     */
    @NotNull
    public Map<Material, Integer> getRequiredMaterials(@NotNull GeneratorDesign design) {
        Map<Material, Integer> materials = new HashMap<>();
        
        for (GeneratorDesignLayer layer : design.getLayers()) {
            Material[][] pattern = layer.getPattern();
            if (pattern == null) continue;
            
            for (int x = 0; x < layer.getWidth(); x++) {
                for (int z = 0; z < layer.getDepth(); z++) {
                    Material mat = pattern[x][z];
                    if (mat != null && mat != Material.AIR) {
                        materials.merge(mat, 1, Integer::sum);
                    }
                }
            }
        }
        
        return materials;
    }

    /**
     * Checks if a player has all required materials.
     *
     * @param player the player
     * @param design the design
     * @return true if player has all materials
     */
    public boolean hasRequiredMaterials(@NotNull Player player, @NotNull GeneratorDesign design) {
        Map<Material, Integer> required = getRequiredMaterials(design);
        
        for (Map.Entry<Material, Integer> entry : required.entrySet()) {
            int playerAmount = countMaterialInInventory(player, entry.getKey());
            if (playerAmount < entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Gets missing materials for a player.
     *
     * @param player the player
     * @param design the design
     * @return map of missing materials to amounts needed
     */
    @NotNull
    public Map<Material, Integer> getMissingMaterials(@NotNull Player player, @NotNull GeneratorDesign design) {
        Map<Material, Integer> missing = new HashMap<>();
        Map<Material, Integer> required = getRequiredMaterials(design);
        
        for (Map.Entry<Material, Integer> entry : required.entrySet()) {
            int playerAmount = countMaterialInInventory(player, entry.getKey());
            int shortage = entry.getValue() - playerAmount;
            if (shortage > 0) {
                missing.put(entry.getKey(), shortage);
            }
        }
        return missing;
    }

    /**
     * Starts an automated build process.
     *
     * @param player the player building
     * @param design the design to build
     * @param centerLocation the center location
     * @return future containing build result
     */
    @NotNull
    public CompletableFuture<BuildResult> startAutoBuild(
            @NotNull Player player,
            @NotNull GeneratorDesign design,
            @NotNull Location centerLocation
    ) {
        if (activeBuildProcesses.containsKey(player.getUniqueId())) {
            return CompletableFuture.completedFuture(
                    new BuildResult(false, "Already building a structure", null)
            );
        }

        if (!hasRequiredMaterials(player, design)) {
            Map<Material, Integer> missing = getMissingMaterials(player, design);
            return CompletableFuture.completedFuture(
                    new BuildResult(false, "Missing materials: " + formatMissingMaterials(missing), null)
            );
        }

        if (!isBuildAreaClear(centerLocation, design)) {
            return CompletableFuture.completedFuture(
                    new BuildResult(false, "Build area is not clear", null)
            );
        }

        BuildProcess process = new BuildProcess(player, design, centerLocation);
        activeBuildProcesses.put(player.getUniqueId(), process);

        return process.start();
    }

    /**
     * Cancels an active build process.
     *
     * @param player the player
     */
    public void cancelBuild(@NotNull Player player) {
        BuildProcess process = activeBuildProcesses.remove(player.getUniqueId());
        if (process != null) {
            process.cancel();
        }
    }

    /**
     * Gets the build progress for a player.
     *
     * @param player the player
     * @return progress info, or null if not building
     */
    @Nullable
    public BuildProgress getBuildProgress(@NotNull Player player) {
        BuildProcess process = activeBuildProcesses.get(player.getUniqueId());
        if (process == null) return null;
        return process.getProgress();
    }

    /**
     * Checks if a player has an active build.
     *
     * @param player the player
     * @return true if building
     */
    public boolean hasActiveBuild(@NotNull Player player) {
        return activeBuildProcesses.containsKey(player.getUniqueId());
    }

    /**
     * Gets all active build processes.
     *
     * @return map of player UUIDs to design names
     */
    @NotNull
    public Map<UUID, String> getActiveBuildProcesses() {
        Map<UUID, String> processes = new HashMap<>();
        for (Map.Entry<UUID, BuildProcess> entry : activeBuildProcesses.entrySet()) {
            processes.put(entry.getKey(), entry.getValue().design.getDesignKey());
        }
        return processes;
    }

    // ==================== Configuration ====================

    public void setAnimationSpeed(int ticksBetweenBlocks) {
        this.animationSpeed = Math.max(1, ticksBetweenBlocks);
    }

    public void setSoundEnabled(boolean enabled) {
        this.soundEnabled = enabled;
    }

    public void setParticleDensity(double density) {
        this.particleDensity = Math.max(0.0, Math.min(2.0, density));
    }

    // ==================== Private Methods ====================

    private int countMaterialInInventory(@NotNull Player player, @NotNull Material material) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) {
                count += item.getAmount();
            }
        }
        return count;
    }

    private boolean removeMaterialFromInventory(@NotNull Player player, @NotNull Material material, int amount) {
        int remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack item = contents[i];
            if (item != null && item.getType() == material) {
                int itemAmount = item.getAmount();
                if (itemAmount <= remaining) {
                    remaining -= itemAmount;
                    contents[i] = null;
                } else {
                    item.setAmount(itemAmount - remaining);
                    remaining = 0;
                }
            }
        }
        
        player.getInventory().setContents(contents);
        return remaining == 0;
    }

    private String formatMissingMaterials(Map<Material, Integer> missing) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Material, Integer> entry : missing.entrySet()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(entry.getValue()).append("x ").append(formatMaterialName(entry.getKey()));
        }
        return sb.toString();
    }

    private String formatMaterialName(Material material) {
        return material.name().toLowerCase().replace('_', ' ');
    }

    // ==================== Build Process ====================

    private class BuildProcess {
        private final Player player;
        private final GeneratorDesign design;
        private final Location centerLocation;
        private final CompletableFuture<BuildResult> completionFuture;
        
        private BukkitTask buildTask;
        private int currentLayerIndex = 0;
        private int currentBlockIndex = 0;
        private List<BlockPlacement> currentLayerBlocks;
        private int totalBlocksPlaced = 0;
        private int totalBlocks = 0;
        private boolean cancelled = false;

        BuildProcess(@NotNull Player player, @NotNull GeneratorDesign design, @NotNull Location centerLocation) {
            this.player = player;
            this.design = design;
            this.centerLocation = centerLocation;
            this.completionFuture = new CompletableFuture<>();
            this.totalBlocks = calculateTotalBlocks();
        }

        private int calculateTotalBlocks() {
            int count = 0;
            for (GeneratorDesignLayer layer : design.getLayers()) {
                count += layer.getTotalBlocks();
            }
            return count;
        }

        @NotNull
        CompletableFuture<BuildResult> start() {
            player.sendMessage("§a§lStarting construction of " + design.getDesignKey() + "...");
            playStartSound();
            buildNextLayer();
            return completionFuture;
        }

        void cancel() {
            cancelled = true;
            if (buildTask != null) {
                buildTask.cancel();
            }
            completionFuture.complete(new BuildResult(false, "Construction cancelled", null));
            player.sendMessage("§c§lConstruction cancelled!");
        }

        BuildProgress getProgress() {
            int totalLayers = design.getLayers().size();
            double overallProgress = totalBlocks > 0 ? (double) totalBlocksPlaced / totalBlocks : 0.0;
            int blocksInLayer = currentLayerBlocks != null ? currentLayerBlocks.size() : 0;
            
            return new BuildProgress(
                    design.getDesignKey(),
                    currentLayerIndex + 1,
                    totalLayers,
                    currentBlockIndex,
                    blocksInLayer,
                    totalBlocksPlaced,
                    totalBlocks,
                    overallProgress
            );
        }

        private void buildNextLayer() {
            if (cancelled) return;
            
            List<GeneratorDesignLayer> layers = design.getLayers();
            if (currentLayerIndex >= layers.size()) {
                finishConstruction();
                return;
            }

            GeneratorDesignLayer layer = layers.get(currentLayerIndex);
            currentLayerBlocks = prepareLayerBlocks(layer);
            currentBlockIndex = 0;

            player.sendMessage("§6Building layer " + (currentLayerIndex + 1) + "/" + layers.size() + 
                    " (" + layer.getNameKey() + ")");

            buildNextBlock();
        }

        private void buildNextBlock() {
            if (cancelled) return;

            if (currentBlockIndex >= currentLayerBlocks.size()) {
                currentLayerIndex++;
                Bukkit.getScheduler().runTaskLater(plugin, this::buildNextLayer, 20L);
                return;
            }

            BlockPlacement placement = currentLayerBlocks.get(currentBlockIndex);
            
            // Create particle trail
            if (particleDensity > 0) {
                createParticleTrail(player.getEyeLocation(), placement.location(), placement.material());
            }

            buildTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!cancelled) {
                    placeBlock(placement);
                    currentBlockIndex++;
                    totalBlocksPlaced++;
                    
                    Bukkit.getScheduler().runTaskLater(plugin, this::buildNextBlock, animationSpeed);
                }
            }, 10L);
        }

        private List<BlockPlacement> prepareLayerBlocks(@NotNull GeneratorDesignLayer layer) {
            List<BlockPlacement> blocks = new ArrayList<>();
            World world = centerLocation.getWorld();
            if (world == null) return blocks;

            int layerY = centerLocation.getBlockY() + layer.getLayerIndex();
            Material[][] pattern = layer.getPattern();
            if (pattern == null) return blocks;

            int width = layer.getWidth();
            int depth = layer.getDepth();
            int startX = centerLocation.getBlockX() - width / 2;
            int startZ = centerLocation.getBlockZ() - depth / 2;

            for (int z = 0; z < depth; z++) {
                for (int x = 0; x < width; x++) {
                    Material material = pattern[x][z];
                    if (material != null && material != Material.AIR) {
                        Location blockLoc = new Location(world, startX + x, layerY, startZ + z);
                        blocks.add(new BlockPlacement(blockLoc, material));
                    }
                }
            }

            return blocks;
        }

        private void placeBlock(@NotNull BlockPlacement placement) {
            if (!removeMaterialFromInventory(player, placement.material(), 1)) {
                player.sendMessage("§cRan out of " + formatMaterialName(placement.material()) + "!");
                cancel();
                return;
            }

            placement.location().getBlock().setType(placement.material());
            
            if (soundEnabled) {
                playPlacementSound(placement.location(), placement.material());
            }
            
            if (particleDensity > 0) {
                spawnPlacementParticles(placement.location(), placement.material());
            }
        }

        private void createParticleTrail(@NotNull Location from, @NotNull Location to, @NotNull Material material) {
            World world = from.getWorld();
            if (world == null) return;

            org.bukkit.util.Vector direction = to.toVector().subtract(from.toVector()).normalize();
            double distance = from.distance(to);
            int particleCount = (int) (distance * particleDensity);

            for (int i = 0; i < particleCount; i++) {
                double d = (double) i / particleCount * distance;
                Location particleLoc = from.clone().add(direction.clone().multiply(d));
                
                long delay = (long) (d * 2);
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (!cancelled) {
                        world.spawnParticle(Particle.ENCHANT, particleLoc, 2, 0.1, 0.1, 0.1, 0.1);
                    }
                }, delay);
            }
        }

        private void spawnPlacementParticles(@NotNull Location location, @NotNull Material material) {
            World world = location.getWorld();
            if (world == null) return;

            Location center = location.clone().add(0.5, 0.5, 0.5);
            Particle particle = getParticleForMaterial(material);
            
            int count = (int) (10 * particleDensity);
            world.spawnParticle(particle, center, count, 0.3, 0.3, 0.3, 0.1);
            world.spawnParticle(Particle.HAPPY_VILLAGER, center, (int) (5 * particleDensity), 0.2, 0.2, 0.2, 0);
        }

        private Particle getParticleForMaterial(@NotNull Material material) {
            return switch (material) {
                case WATER -> Particle.DRIPPING_WATER;
                case LAVA -> Particle.LAVA;
                case BEACON -> Particle.END_ROD;
                case NETHERITE_BLOCK -> Particle.SOUL_FIRE_FLAME;
                case DIAMOND_BLOCK, EMERALD_BLOCK -> Particle.FIREWORK;
                case GOLD_BLOCK -> Particle.ENCHANTED_HIT;
                default -> Particle.CLOUD;
            };
        }

        private void playStartSound() {
            if (soundEnabled) {
                player.playSound(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, 1.0f);
            }
        }

        private void playPlacementSound(@NotNull Location location, @NotNull Material material) {
            Sound sound = switch (material) {
                case IRON_BLOCK, GOLD_BLOCK, DIAMOND_BLOCK, EMERALD_BLOCK -> Sound.BLOCK_METAL_PLACE;
                case NETHERITE_BLOCK -> Sound.BLOCK_NETHERITE_BLOCK_PLACE;
                case BEACON -> Sound.BLOCK_BEACON_ACTIVATE;
                case WATER -> Sound.ITEM_BUCKET_EMPTY;
                case LAVA -> Sound.ITEM_BUCKET_EMPTY_LAVA;
                default -> Sound.BLOCK_STONE_PLACE;
            };
            location.getWorld().playSound(location, sound, 0.8f, 1.2f);
        }

        private void finishConstruction() {
            activeBuildProcesses.remove(player.getUniqueId());

            Location center = centerLocation.clone().add(0.5, design.getHeight() + 1, 0.5);
            World world = center.getWorld();
            
            if (world != null) {
                world.spawnParticle(Particle.FIREWORK, center, 50, 2, 2, 2, 0.1);
            }
            
            if (soundEnabled) {
                player.playSound(center, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            }

            player.sendMessage("§a§l✓ Construction Complete!");
            player.sendMessage("§7Structure: §f" + design.getDesignKey());
            player.sendMessage("§7Total blocks placed: §f" + totalBlocksPlaced);

            completionFuture.complete(new BuildResult(true, "Construction complete", centerLocation));
        }
    }

    // ==================== Records ====================

    private record BlockPlacement(@NotNull Location location, @NotNull Material material) {}

    public record BuildResult(
            boolean success,
            @NotNull String message,
            @Nullable Location structureLocation
    ) {}

    public record BuildProgress(
            @NotNull String designKey,
            int currentLayer,
            int totalLayers,
            int currentBlockInLayer,
            int totalBlocksInLayer,
            int totalBlocksPlaced,
            int totalBlocks,
            double overallProgress
    ) {
        public int getProgressPercentage() {
            return (int) (overallProgress * 100);
        }
    }
}
