package de.jexcellence.oneblock.service;

import de.jexcellence.oneblock.database.entity.oneblock.OneblockIsland;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class UpgradeService {
    
    private static final Logger LOGGER = Logger.getLogger("JExOneblock");
    
    private final Map<UpgradeType, UpgradeConfiguration> upgradeConfigurations;
    
    public UpgradeService() {
        this.upgradeConfigurations = initializeUpgradeConfigurations();
    }

    public @NotNull CompletableFuture<Boolean> purchaseUpgrade(
            @NotNull OneblockIsland island,
            @NotNull UpgradeType type,
            @NotNull Player player
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var currentLevel = getCurrentLevel(island, type);
                var nextLevel = currentLevel + 1;
                var config = upgradeConfigurations.get(type);
                
                if (config == null) {
                    LOGGER.warning("No configuration found for upgrade type: " + type);
                    return false;
                }
                
                if (nextLevel > config.maxLevel()) {
                    LOGGER.warning("Island " + island.getIdentifier() + " is already at max level for " + type);
                    return false;
                }
                
                var requirements = getNextLevelRequirements(island, type);
                if (requirements == null) {
                    LOGGER.warning("No requirements found for " + type + " level " + nextLevel);
                    return false;
                }
                
                if (!canAffordUpgrade(island, type, player)) {
                    LOGGER.warning("Player " + player.getName() + " cannot afford " + type + " upgrade for island " + island.getIdentifier());
                    return false;
                }
                
                if (!consumeUpgradeResources(island, requirements, player)) {
                    LOGGER.warning("Failed to consume resources for " + type + " upgrade");
                    return false;
                }
                
                applyUpgrade(island, type, nextLevel);
                
                LOGGER.info("Successfully upgraded " + type + " to level " + nextLevel + " for island " + island.getIdentifier());
                return true;
                
            } catch (Exception e) {
                LOGGER.severe("Failed to purchase upgrade: " + e.getMessage());
                return false;
            }
        });
    }

    public int getCurrentLevel(@NotNull OneblockIsland island, @NotNull UpgradeType type) {
        return switch (type) {
            case SIZE_EXPANSION -> calculateSizeLevel(island.getCurrentSize());
            case MEMBER_SLOTS -> calculateMemberSlotLevel(island.getMemberCount());
            case STORAGE_CAPACITY -> calculateStorageCapacityLevel(island);
            case BIOME_TIER -> calculateBiomeTierLevel(island);
        };
    }

    public UpgradeRequirements getNextLevelRequirements(@NotNull OneblockIsland island, @NotNull UpgradeType type) {
        var currentLevel = getCurrentLevel(island, type);
        var nextLevel = currentLevel + 1;
        var config = upgradeConfigurations.get(type);
        
        if (config == null || nextLevel > config.maxLevel()) {
            return null;
        }
        
        return config.getLevelRequirements(nextLevel);
    }
    
    public boolean canAffordUpgrade(@NotNull OneblockIsland island, @NotNull UpgradeType type, @NotNull Player player) {
        var requirements = getNextLevelRequirements(island, type);
        if (requirements == null) {
            return false;
        }
        
        if (island.getIslandCoins() < requirements.coinCost()) {
            return false;
        }
        
        if (island.getLevel() < requirements.minLevel()) {
            return false;
        }
        
        for (var entry : requirements.materials().entrySet()) {
            var material = entry.getKey();
            var required = entry.getValue();
            
            var playerAmount = 0;
            for (var item : player.getInventory().getContents()) {
                if (item != null && item.getType() == material) {
                    playerAmount += item.getAmount();
                }
            }
            
            if (playerAmount < required) {
                return false;
            }
        }
        
        return true;
    }
    
    public int getMaxLevel(@NotNull UpgradeType type) {
        var config = upgradeConfigurations.get(type);
        return config != null ? config.maxLevel() : 0;
    }
    
    @NotNull
    public String getUpgradeBenefits(@NotNull UpgradeType type, int level) {
        return switch (type) {
            case SIZE_EXPANSION -> "Island size: " + getSizeForLevel(level) + "x" + getSizeForLevel(level);
            case MEMBER_SLOTS -> "Member slots: " + getMemberSlotsForLevel(level);
            case STORAGE_CAPACITY -> "Storage capacity: +" + (level * 25) + "%";
            case BIOME_TIER -> "Unlocks tier " + level + " biomes";
        };
    }
    
    private void applyUpgrade(@NotNull OneblockIsland island, @NotNull UpgradeType type, int newLevel) {
        switch (type) {
            case SIZE_EXPANSION -> {
                var newSize = getSizeForLevel(newLevel);
                island.setCurrentSize(newSize);
                if (island.getRegion() != null) {
                    island.getRegion().expand((newSize - island.getCurrentSize()) / 2);
                }
            }
            case MEMBER_SLOTS -> {
                // Member slots are handled dynamically based on level
            }
            case STORAGE_CAPACITY -> {
                // Storage capacity is handled by infrastructure system
            }
            case BIOME_TIER -> {
                // Biome tier unlocks are handled by BiomeService
            }
        }
    }
    
    private boolean consumeUpgradeResources(
            @NotNull OneblockIsland island,
            @NotNull UpgradeRequirements requirements,
            @NotNull Player player
    ) {
        if (!island.removeCoins(requirements.coinCost())) {
            return false;
        }
        
        for (var entry : requirements.materials().entrySet()) {
            var material = entry.getKey();
            var required = entry.getValue();
            
            var remaining = required;
            for (var item : player.getInventory().getContents()) {
                if (item != null && item.getType() == material && remaining > 0) {
                    var toRemove = Math.min(remaining, item.getAmount());
                    item.setAmount(item.getAmount() - toRemove);
                    remaining -= toRemove;
                    
                    if (item.getAmount() <= 0) {
                        player.getInventory().remove(item);
                    }
                }
            }
            
            if (remaining > 0) {
                LOGGER.warning("Failed to consume " + remaining + " " + material + " for upgrade");
                return false;
            }
        }
        
        return true;
    }
    
    private int calculateStorageCapacityLevel(@NotNull OneblockIsland island) {
        var plugin = org.bukkit.Bukkit.getPluginManager().getPlugin("JExOneblock");
        if (plugin instanceof de.jexcellence.oneblock.JExOneblock jexPlugin) {
            var infraService = jexPlugin.getInfrastructureService();
            if (infraService != null) {
                var infrastructure = infraService.getInfrastructure(Long.valueOf(island.getIdentifier()), island.getOwner().getUniqueId());
                if (infrastructure != null) {
                    return infrastructure.getStorageTier().ordinal();
                }
            }
        }
        return 0;
    }
    
    private int calculateBiomeTierLevel(@NotNull OneblockIsland island) {
        var level = calculateSizeLevel(island.getCurrentSize());
        return Math.min(4, level / 3);
    }

    private int calculateSizeLevel(int currentSize) {
        return Math.max(0, (currentSize - 50) / 25);
    }
    
    private int calculateMemberSlotLevel(int memberCount) {
        return Math.max(0, (memberCount - 5) / 3);
    }
    
    private int getSizeForLevel(int level) {
        return 50 + (level * 25);
    }
    
    private int getMemberSlotsForLevel(int level) {
        return 5 + (level * 3);
    }
    
    private Map<UpgradeType, UpgradeConfiguration> initializeUpgradeConfigurations() {
        var configs = new HashMap<UpgradeType, UpgradeConfiguration>();
        
        var sizeConfig = new UpgradeConfiguration(10);
        for (int level = 1; level <= 10; level++) {
            var materials = new HashMap<Material, Integer>();
            materials.put(Material.STONE, level * 64);
            materials.put(Material.IRON_INGOT, level * 16);
            if (level >= 5) materials.put(Material.DIAMOND, level * 4);
            if (level >= 8) materials.put(Material.NETHERITE_INGOT, level);
            
            sizeConfig.setLevelRequirements(level, new UpgradeRequirements(
                level * 5,
                level * 1000L,
                materials
            ));
        }
        configs.put(UpgradeType.SIZE_EXPANSION, sizeConfig);
        
        var memberConfig = new UpgradeConfiguration(8);
        for (int level = 1; level <= 8; level++) {
            var materials = new HashMap<Material, Integer>();
            materials.put(Material.GOLD_INGOT, level * 32);
            materials.put(Material.EMERALD, level * 8);
            if (level >= 4) materials.put(Material.DIAMOND, level * 2);
            
            memberConfig.setLevelRequirements(level, new UpgradeRequirements(
                level * 3,
                level * 750L,
                materials
            ));
        }
        configs.put(UpgradeType.MEMBER_SLOTS, memberConfig);
        
        var storageConfig = new UpgradeConfiguration(5);
        for (int level = 1; level <= 5; level++) {
            var materials = new HashMap<Material, Integer>();
            materials.put(Material.CHEST, level * 8);
            materials.put(Material.REDSTONE, level * 32);
            if (level >= 3) materials.put(Material.ENDER_CHEST, level * 2);
            
            storageConfig.setLevelRequirements(level, new UpgradeRequirements(
                level * 10,
                level * 2000L,
                materials
            ));
        }
        configs.put(UpgradeType.STORAGE_CAPACITY, storageConfig);
        
        var biomeConfig = new UpgradeConfiguration(4);
        for (int level = 1; level <= 4; level++) {
            var materials = new HashMap<Material, Integer>();
            materials.put(Material.GRASS_BLOCK, level * 64);
            materials.put(Material.WATER_BUCKET, level * 4);
            if (level >= 2) materials.put(Material.LAVA_BUCKET, level * 2);
            if (level >= 3) materials.put(Material.END_STONE, level * 32);
            if (level >= 4) materials.put(Material.NETHER_STAR, level);
            
            biomeConfig.setLevelRequirements(level, new UpgradeRequirements(
                level * 15,
                level * 5000L,
                materials
            ));
        }
        configs.put(UpgradeType.BIOME_TIER, biomeConfig);
        
        return configs;
    }
    
    public enum UpgradeType {
        SIZE_EXPANSION("Size Expansion", "Increases your island's protected area"),
        MEMBER_SLOTS("Member Slots", "Allows more players to join your island"),
        STORAGE_CAPACITY("Storage Capacity", "Increases infrastructure storage capacity"),
        BIOME_TIER("Biome Tier", "Unlocks access to higher tier biomes");
        
        private final String displayName;
        private final String description;
        
        UpgradeType(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }
    
    public record UpgradeRequirements(
        int minLevel,
        long coinCost,
        Map<Material, Integer> materials
    ) {}
    
    private static class UpgradeConfiguration {
        private final int maxLevel;
        private final Map<Integer, UpgradeRequirements> levelRequirements = new HashMap<>();
        
        public UpgradeConfiguration(int maxLevel) {
            this.maxLevel = maxLevel;
        }
        
        public int maxLevel() { return maxLevel; }
        
        public void setLevelRequirements(int level, UpgradeRequirements requirements) {
            levelRequirements.put(level, requirements);
        }
        
        public UpgradeRequirements getLevelRequirements(int level) {
            return levelRequirements.get(level);
        }
    }
}