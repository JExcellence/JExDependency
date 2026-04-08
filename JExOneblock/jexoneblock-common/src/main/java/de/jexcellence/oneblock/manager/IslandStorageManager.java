package de.jexcellence.oneblock.manager;

import de.jexcellence.oneblock.database.entity.storage.IslandStorage;
import de.jexcellence.oneblock.database.entity.storage.StorageTier;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class IslandStorageManager {
    
    private final Map<Long, IslandStorage> storageCache = new ConcurrentHashMap<>();
    private final Map<Long, Long> lastPassiveCheck = new ConcurrentHashMap<>();
    private final StorageNotificationManager notificationManager = new StorageNotificationManager();

    public IslandStorage getOrCreateStorage(Long islandId, UUID playerId) {
        return storageCache.computeIfAbsent(islandId, id -> {
            var storage = loadStorageFromDatabase(id, playerId);
            if (storage == null) {
                storage = new IslandStorage(id, playerId);
                saveStorageToDatabase(storage);
            }
            return storage;
        });
    }

    public void handleOneBlockDrop(Player player, Long islandId, ItemStack itemStack, org.bukkit.Location blockLocation) {
        var storage = getOrCreateStorage(islandId, player.getUniqueId());
        var material = itemStack.getType();
        var amount = itemStack.getAmount();
        
        var overflow = storage.storeItem(material, amount);
        
        if (overflow == 0) {
            notificationManager.handleStorageEvent(player, material, amount, true, blockLocation);
        } else if (overflow < amount) {
            var stored = amount - overflow;
            notificationManager.handleStorageEvent(player, material, (int) stored, true, blockLocation);
            notificationManager.handleStorageEvent(player, material, (int) overflow, false, blockLocation);
            
            var overflowStack = new ItemStack(material, (int) overflow);
            giveItemToPlayer(player, overflowStack);
        } else {
            notificationManager.handleStorageEvent(player, material, amount, false, blockLocation);
            giveItemToPlayer(player, itemStack);
        }
        
        processPassiveRewards(player, storage);
    }

    public void handleOneBlockDrop(Player player, Long islandId, ItemStack itemStack) {
        handleOneBlockDrop(player, islandId, itemStack, player.getLocation());
    }

    public boolean upgradeStorage(Player player, Long islandId, StorageTier newTier) {
        var storage = getOrCreateStorage(islandId, player.getUniqueId());
        
        if (!canUpgradeToTier(player, storage, newTier)) {
            return false;
        }
        
        if (consumeUpgradeRequirements(player, newTier)) {
            storage.upgradeTier(newTier);
            saveStorageToDatabase(storage);
            
            player.sendMessage("§a[Storage] §7Upgraded to §6" + newTier.getDisplayName() + "§7!");
            player.sendMessage("§7New passive bonuses: §a+" + (int)((newTier.getPassiveXpMultiplier() - 1) * 100) + "% XP§7, §b+" + (int)((newTier.getPassiveDropMultiplier() - 1) * 100) + "% Drops");
            
            return true;
        }
        
        return false;
    }

    public boolean retrieveItems(Player player, Long islandId, Material material, long amount) {
        var storage = getOrCreateStorage(islandId, player.getUniqueId());
        
        var retrieved = storage.retrieveItem(material, amount);
        if (retrieved > 0) {
            var itemStack = new ItemStack(material, (int) Math.min(retrieved, 64));
            giveItemToPlayer(player, itemStack);
            
            var remaining = retrieved - 64;
            while (remaining > 0) {
                var stackSize = (int) Math.min(remaining, 64);
                var remainingStack = new ItemStack(material, stackSize);
                giveItemToPlayer(player, remainingStack);
                remaining -= stackSize;
            }
            
            player.sendMessage("§a[Storage] §7Retrieved §f" + retrieved + "x " + material.name() + "§7!");
            saveStorageToDatabase(storage);
            return true;
        } else {
            player.sendMessage("§c[Storage] §7Not enough items in storage!");
            return false;
        }
    }
    
    private void processPassiveRewards(Player player, IslandStorage storage) {
        var currentTime = System.currentTimeMillis();
        var lastCheck = lastPassiveCheck.getOrDefault(storage.getIslandId(), currentTime);
        
        if (currentTime - lastCheck >= 300000) {
            storage.processPassiveRewards(player);
            lastPassiveCheck.put(storage.getIslandId(), currentTime);
        }
    }

    private boolean canUpgradeToTier(Player player, IslandStorage storage, StorageTier newTier) {
        var currentTier = storage.getCurrentTier();
        if (newTier.ordinal() != currentTier.ordinal() + 1) {
            player.sendMessage("§c[Storage] §7You must upgrade tiers in order!");
            return false;
        }
        
        var playerStage = getPlayerEvolutionStage(player);
        if (!newTier.canUnlockAtStage(playerStage)) {
            player.sendMessage("§c[Storage] §7You need to reach stage " + newTier.getMinStage() + " to unlock this tier!");
            return false;
        }
        
        for (var material : newTier.getRequiredMaterials()) {
            if (!player.getInventory().contains(material)) {
                player.sendMessage("§c[Storage] §7You need " + material.name() + " to upgrade!");
                return false;
            }
        }
        
        if (!hasEnoughCurrency(player, newTier.getUpgradeCost())) {
            player.sendMessage("§c[Storage] §7You need " + newTier.getUpgradeCost() + " coins to upgrade!");
            return false;
        }
        
        return true;
    }

    private boolean consumeUpgradeRequirements(Player player, StorageTier tier) {
        for (var material : tier.getRequiredMaterials()) {
            player.getInventory().removeItem(new ItemStack(material, 1));
        }
        
        deductCurrency(player, tier.getUpgradeCost());
        
        return true;
    }

    private void giveItemToPlayer(Player player, ItemStack itemStack) {
        var overflow = player.getInventory().addItem(itemStack);
        for (var overflowItem : overflow.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), overflowItem);
        }
    }

    private int getPlayerEvolutionStage(Player player) {
        return 1;
    }

    private boolean hasEnoughCurrency(Player player, long amount) {
        return true;
    }

    private void deductCurrency(Player player, long amount) {
        // Implement your economy deduction logic here
    }

    private IslandStorage loadStorageFromDatabase(Long islandId, UUID playerId) {
        return new IslandStorage(islandId, playerId);
    }

    private void saveStorageToDatabase(IslandStorage storage) {
        // For now, just log - you can implement database saving later
    }

    public IslandStorage getStorage(Long islandId) {
        return storageCache.get(islandId);
    }

    public void clearStorageCache(Long islandId) {
        storageCache.remove(islandId);
        lastPassiveCheck.remove(islandId);
    }

    public void handlePlayerLeave(UUID playerId) {
        notificationManager.cleanupPlayer(playerId);
    }

    public void syncWithInfrastructure(Long islandId, de.jexcellence.oneblock.database.entity.infrastructure.IslandInfrastructure infrastructure) {
        var storage = storageCache.get(islandId);
        if (storage != null && infrastructure != null) {
            try {
                infrastructure.getStoredItems().clear();
                infrastructure.getStoredItems().putAll(storage.getStoredItems());
                
                infrastructure.setStorageTier(storage.getCurrentTier());
                
                var totalCapacity = storage.getRarityCapacities().values().stream()
                    .mapToLong(Long::longValue)
                    .sum();
                
            } catch (Exception e) {
                System.err.println("Error syncing storage with infrastructure for island " + islandId + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}