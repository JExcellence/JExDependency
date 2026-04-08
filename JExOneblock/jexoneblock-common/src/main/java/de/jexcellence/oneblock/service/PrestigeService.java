package de.jexcellence.oneblock.service;

import de.jexcellence.oneblock.database.entity.oneblock.OneblockIsland;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class PrestigeService {
    
    private static final Logger LOGGER = Logger.getLogger("JExOneblock");
    
    private final Map<Integer, PrestigeRequirements> prestigeRequirements;
    private final Map<Integer, PrestigeRewards> prestigeRewards;
    
    public PrestigeService() {
        this.prestigeRequirements = initializePrestigeRequirements();
        this.prestigeRewards = initializePrestigeRewards();
    }

    public boolean canPrestige(@NotNull OneblockIsland island) {
        var currentPrestige = island.getOneblock() != null ? island.getOneblock().getPrestigeLevel() : 0;
        var nextPrestige = currentPrestige + 1;
        var requirements = prestigeRequirements.get(nextPrestige);
        
        if (requirements == null) {
            return false;
        }
        
        var currentEvolutionLevel = island.getOneblock() != null ? island.getOneblock().getEvolutionLevel() : 1;
        if (currentEvolutionLevel < requirements.requiredEvolutionLevel()) {
            return false;
        }
        
        var totalBlocksBroken = island.getTotalBlocksBroken();
        if (totalBlocksBroken < requirements.requiredBlocksBroken()) {
            return false;
        }
        
        return true;
    }

    public PrestigeRequirements getRequirements(int prestigeLevel) {
        return prestigeRequirements.get(prestigeLevel);
    }

    public PrestigeRewards getRewards(int prestigeLevel) {
        return prestigeRewards.get(prestigeLevel);
    }

    public @NotNull CompletableFuture<Boolean> performPrestige(@NotNull OneblockIsland island, @NotNull Player player) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!canPrestige(island)) {
                    LOGGER.warning("Island " + island.getIdentifier() + " does not meet prestige requirements");
                    return false;
                }
                
                var currentPrestige = island.getOneblock() != null ? island.getOneblock().getPrestigeLevel() : 0;
                var nextPrestige = currentPrestige + 1;
                var requirements = prestigeRequirements.get(nextPrestige);
                var rewards = prestigeRewards.get(nextPrestige);
                
                if (requirements == null || rewards == null) {
                    LOGGER.warning("No prestige configuration found for level " + nextPrestige);
                    return false;
                }
                
                if (!consumePrestigeItems(player, requirements.requiredItems())) {
                    LOGGER.warning("Player " + player.getName() + " does not have required items for prestige");
                    return false;
                }
                
                if (island.getOneblock() != null) {
                    island.getOneblock().setEvolutionLevel(1);
                    island.getOneblock().setEvolutionExperience(0.0);
                    island.getOneblock().setCurrentEvolution("Stone");
                    island.getOneblock().setTotalBlocksBroken(0L);
                }
                
                island.setLevel(1);
                island.setExperience(0.0);
                
                if (island.getOneblock() != null) {
                    island.getOneblock().setPrestigeLevel(nextPrestige);
                    island.getOneblock().setPrestigePoints(island.getOneblock().getPrestigePoints() + rewards.prestigePoints());
                }
                
                var infrastructureService = getInfrastructureService();
                if (infrastructureService != null) {
                    var infrastructure = infrastructureService.getInfrastructure(island.getId(), player.getUniqueId());
                    if (infrastructure != null) {
                        var currentXpMultiplier = infrastructure.getPassiveXpMultiplier();
                        infrastructure.setPassiveXpMultiplier(currentXpMultiplier * rewards.xpMultiplier());
                        
                        var currentDropMultiplier = infrastructure.getPassiveDropMultiplier();
                        infrastructure.setPassiveDropMultiplier(currentDropMultiplier * rewards.dropMultiplier());
                        
                        infrastructure.setPrestigeLevel(nextPrestige);
                    }
                }
                
                LOGGER.info("Successfully prestiged island " + island.getIdentifier() + " to level " + nextPrestige);
                return true;
                
            } catch (Exception e) {
                LOGGER.severe("Failed to perform prestige: " + e.getMessage());
                return false;
            }
        });
    }
    
    public @NotNull String getPrestigeProgress(@NotNull OneblockIsland island) {
        if (!canPrestige(island)) {
            var currentPrestige = island.getOneblock() != null ? island.getOneblock().getPrestigeLevel() : 0;
            var nextPrestige = currentPrestige + 1;
            var requirements = prestigeRequirements.get(nextPrestige);
            
            if (requirements == null) {
                return "Maximum prestige level reached!";
            }
            
            var currentEvolutionLevel = island.getOneblock() != null ? island.getOneblock().getEvolutionLevel() : 1;
            var totalBlocksBroken = island.getTotalBlocksBroken();
            
            var progress = new StringBuilder();
            progress.append("Prestige Progress:\n");
            progress.append("Evolution Level: ").append(currentEvolutionLevel).append("/").append(requirements.requiredEvolutionLevel());
            if (currentEvolutionLevel >= requirements.requiredEvolutionLevel()) {
                progress.append(" ✓");
            }
            progress.append("\n");
            
            progress.append("Blocks Broken: ").append(totalBlocksBroken).append("/").append(requirements.requiredBlocksBroken());
            if (totalBlocksBroken >= requirements.requiredBlocksBroken()) {
                progress.append(" ✓");
            }
            progress.append("\n");
            
            return progress.toString();
        }
        
        return "Ready to prestige!";
    }
    
    public int getMaxPrestigeLevel() {
        return prestigeRequirements.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
    }
    
    private boolean consumePrestigeItems(@NotNull Player player, @NotNull Map<Material, Integer> requiredItems) {
        for (var entry : requiredItems.entrySet()) {
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
        
        for (var entry : requiredItems.entrySet()) {
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
        }
        
        return true;
    }
    
    private IInfrastructureService getInfrastructureService() {
        var plugin = Bukkit.getPluginManager().getPlugin("JExOneblock");
        if (plugin instanceof de.jexcellence.oneblock.JExOneblock jexPlugin) {
            return jexPlugin.getInfrastructureService();
        }
        return null;
    }
    
    private Map<Integer, PrestigeRequirements> initializePrestigeRequirements() {
        var requirements = new HashMap<Integer, PrestigeRequirements>();
        
        var items1 = new HashMap<Material, Integer>();
        items1.put(Material.DIAMOND_BLOCK, 64);
        items1.put(Material.EMERALD_BLOCK, 32);
        items1.put(Material.GOLD_BLOCK, 128);
        requirements.put(1, new PrestigeRequirements(50, 1000000L, items1));
        
        var items2 = new HashMap<Material, Integer>();
        items2.put(Material.NETHERITE_BLOCK, 16);
        items2.put(Material.DIAMOND_BLOCK, 128);
        items2.put(Material.BEACON, 4);
        requirements.put(2, new PrestigeRequirements(75, 5000000L, items2));
        
        var items3 = new HashMap<Material, Integer>();
        items3.put(Material.NETHERITE_BLOCK, 64);
        items3.put(Material.NETHER_STAR, 16);
        items3.put(Material.DRAGON_EGG, 1);
        requirements.put(3, new PrestigeRequirements(100, 25000000L, items3));
        
        var items4 = new HashMap<Material, Integer>();
        items4.put(Material.NETHERITE_BLOCK, 256);
        items4.put(Material.NETHER_STAR, 64);
        items4.put(Material.ELYTRA, 4);
        requirements.put(4, new PrestigeRequirements(150, 100000000L, items4));
        
        var items5 = new HashMap<Material, Integer>();
        items5.put(Material.NETHERITE_BLOCK, 1024);
        items5.put(Material.NETHER_STAR, 256);
        items5.put(Material.DRAGON_EGG, 4);
        items5.put(Material.ENCHANTED_GOLDEN_APPLE, 64);
        requirements.put(5, new PrestigeRequirements(200, 500000000L, items5));
        
        return requirements;
    }
    
    private Map<Integer, PrestigeRewards> initializePrestigeRewards() {
        var rewards = new HashMap<Integer, PrestigeRewards>();
        
        rewards.put(1, new PrestigeRewards(
            1.25,
            1.15,
            100L,
            List.of("Prestige-only automation modules", "Enhanced infrastructure capacity")
        ));
        
        rewards.put(2, new PrestigeRewards(
            1.5,
            1.3,
            250L,
            List.of("Advanced prestige generators", "Quantum processing modules")
        ));
        
        rewards.put(3, new PrestigeRewards(
            2.0,
            1.5,
            500L,
            List.of("Dimensional storage access", "Reality manipulation modules")
        ));
        
        rewards.put(4, new PrestigeRewards(
            3.0,
            2.0,
            1000L,
            List.of("Infinity-tier infrastructure", "Omnipotent automation")
        ));
        
        rewards.put(5, new PrestigeRewards(
            5.0,
            3.0,
            2500L,
            List.of("Ultimate prestige status", "Transcendent island capabilities", "Access to all content")
        ));
        
        return rewards;
    }
    
    public record PrestigeRequirements(
        int requiredEvolutionLevel,
        long requiredBlocksBroken,
        Map<Material, Integer> requiredItems
    ) {}
    
    public record PrestigeRewards(
        double xpMultiplier,
        double dropMultiplier,
        long prestigePoints,
        List<String> unlockedFeatures
    ) {}
}