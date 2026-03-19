package de.jexcellence.oneblock.service;

import de.jexcellence.oneblock.database.entity.evolution.EvolutionItem;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockCore;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockEvolution;
import de.jexcellence.oneblock.type.EEvolutionRarityType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Service for spawning evolution-specific chests and barrels with balanced loot
 * Uses barrels to avoid height collision issues
 */
public class EvolutionChestSpawningService {

    private static final int CHEST_DESPAWN_TIME = 3000;
    
    /**
     * Spawns a chest/barrel with evolution-specific loot
     */
    @Nullable
    public Block spawnEvolutionChest(@NotNull Player player, @NotNull OneblockCore core, 
                                    @NotNull Location spawnLocation, @NotNull EEvolutionRarityType rarity) {
        
        String evolution = core.getCurrentEvolution();
        int evolutionLevel = core.getEvolutionLevel();


        Location chestLocation = findSafeChestLocation(spawnLocation);
        if (chestLocation == null) {
            return null;
        }

        Block chestBlock = chestLocation.getBlock();
        chestBlock.setType(Material.BARREL);

        Bukkit.getScheduler().runTaskLater(Bukkit.getPluginManager().getPlugin("JExOneblock"), () -> {
            if (chestBlock.getState() instanceof org.bukkit.block.Container container) {

                var inventory = container.getInventory();

                List<ItemStack> evolutionItems = getItemsFromEvolution(evolution, rarity);
                
                if (!evolutionItems.isEmpty()) {
                    fillChestWithEvolutionItems(inventory, evolutionItems);
                } else {
                    fillChestWithBasicItems(inventory, rarity);
                }


                Bukkit.getScheduler().runTaskLater(Bukkit.getPluginManager().getPlugin("JExOneblock"), () -> {
                    if (chestBlock.getState() instanceof org.bukkit.block.Container nameContainer) {
                        String chestName = generateChestName(evolution, rarity);
                        nameContainer.setCustomName(chestName);
                        nameContainer.update();
                    }
                }, 1L);

                int itemCount = countItems(inventory);

                for (int i = 0; i < inventory.getSize(); i++) {
                    ItemStack item = inventory.getItem(i);
                    if (item != null && item.getType() != Material.AIR) {
                    }
                }
            } else {
            }
        }, 3L);

        scheduleChestRemoval(chestBlock, player);
        
        return chestBlock;
    }
    
    /**
     * Finds a safe location to place the chest
     */
    @Nullable
    private Location findSafeChestLocation(@NotNull Location baseLocation) {
        World world = baseLocation.getWorld();
        if (world == null) return null;

        for (int attempts = 0; attempts < 15; attempts++) {
            int offsetX = ThreadLocalRandom.current().nextInt(-4 , 5);
            int offsetZ = ThreadLocalRandom.current().nextInt(-4, 5);
            int offsetY = ThreadLocalRandom.current().nextInt(-1, 3);
            
            Location testLocation = baseLocation.clone().add(offsetX, offsetY, offsetZ);

            Block testBlock = world.getBlockAt(testLocation);
            Block belowBlock = world.getBlockAt(testLocation.clone().add(0, -1, 0));
            
            if (testBlock.getType() == Material.AIR && belowBlock.getType().isSolid()) {
                return testLocation;
            }
        }

        return baseLocation.clone().add(0, 1, 0);
    }
    
    /**
     * Gets items from the registered evolution with intelligent rarity fallback
     */
    @NotNull
    private List<ItemStack> getItemsFromEvolution(@NotNull String evolutionName, @NotNull EEvolutionRarityType rarity) {
        try {

            var evolutionFactory = de.jexcellence.oneblock.factory.EvolutionFactory.getInstance();
            var evolution = evolutionFactory.getCachedEvolution(evolutionName);
            
            if (evolution == null) {
                return new ArrayList<>();
            }


            var availableRarities = evolution.getItems().stream()
                .map(item -> item.getRarity())
                .distinct()
                .collect(Collectors.toList());

            List<ItemStack> items = getItemsFromEvolutionObject(evolution, rarity);
            
            if (!items.isEmpty()) {
                return items;
            }

            EEvolutionRarityType fallbackRarity = findHighestAvailableItemRarity(evolution, rarity);
            if (fallbackRarity != null && fallbackRarity != rarity) {
                items = getItemsFromEvolutionObject(evolution, fallbackRarity);
                return items;
            }

            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return new ArrayList<>();
    }
    
    /**
     * Gets items from evolution object for specific rarity
     */
    @NotNull
    private List<ItemStack> getItemsFromEvolutionObject(@NotNull OneblockEvolution evolution, @NotNull EEvolutionRarityType rarity) {

        return evolution.getItems().stream()
            .filter(evolutionItem -> {
                boolean rarityMatch = evolutionItem.getRarity() == rarity;
                boolean isValid = evolutionItem.isValid();
                boolean hasItemStacks = !evolutionItem.getItemStacks().isEmpty();
                return rarityMatch && isValid && hasItemStacks;
            })
            .flatMap(evolutionItem -> {
                return evolutionItem.getItemStacks().stream();
            })
            .filter(Objects::nonNull)
            .map(ItemStack::clone)
            .collect(Collectors.toList());
    }
    
    /**
     * Finds the highest available item rarity in an evolution that is <= the requested rarity
     */
    @Nullable
    private EEvolutionRarityType findHighestAvailableItemRarity(@NotNull OneblockEvolution evolution, @NotNull EEvolutionRarityType maxRarity) {
        var availableRarities = evolution.getItems().stream()
            .filter(EvolutionItem::isValid)
            .map(EvolutionItem::getRarity)
            .filter(rarity -> rarity.getTier() <= maxRarity.getTier())
            .distinct()
            .sorted((a, b) -> Integer.compare(b.getTier(), a.getTier()))
            .toList();
        
        return availableRarities.isEmpty() ? null : availableRarities.getFirst();
    }
    
    /**
     * Fills chest with basic items as final fallback in random slots
     */
    private void fillChestWithBasicItems(@NotNull Inventory inventory, @NotNull EEvolutionRarityType rarity) {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        List<ItemStack> basicItems = new ArrayList<>();

        basicItems.add(new ItemStack(Material.BREAD, random.nextInt(1, 4)));
        basicItems.add(new ItemStack(Material.COAL, random.nextInt(2, 8)));
        basicItems.add(new ItemStack(Material.IRON_INGOT, random.nextInt(1, 4)));

        if (rarity.getTier() >= EEvolutionRarityType.UNCOMMON.getTier()) {
            basicItems.add(new ItemStack(Material.GOLD_INGOT, random.nextInt(1, 3)));
        }
        if (rarity.getTier() >= EEvolutionRarityType.RARE.getTier()) {
            basicItems.add(new ItemStack(Material.DIAMOND, random.nextInt(1, 2)));
        }
        if (rarity.getTier() >= EEvolutionRarityType.EPIC.getTier()) {
            basicItems.add(new ItemStack(Material.EMERALD, random.nextInt(1, 3)));
        }
        if (rarity.getTier() >= EEvolutionRarityType.LEGENDARY.getTier()) {
            basicItems.add(new ItemStack(Material.NETHERITE_SCRAP, 1));
        }

        basicItems.add(new ItemStack(Material.EXPERIENCE_BOTTLE, random.nextInt(1, Math.max(2, rarity.getTier()))));

        List<Integer> availableSlots = new ArrayList<>();
        for (int i = 0; i < inventory.getSize(); i++) {
            availableSlots.add(i);
        }
        Collections.shuffle(availableSlots);

        int itemCount = Math.min(random.nextInt(3, 8), Math.min(basicItems.size(), availableSlots.size()));
        for (int i = 0; i < itemCount; i++) {
            ItemStack item = basicItems.get(random.nextInt(basicItems.size())).clone();
            int slot = availableSlots.get(i);
            inventory.setItem(slot, item);
        }

    }
    
    /**
     * Fills chest with evolution items in random slots
     */
    private void fillChestWithEvolutionItems(@NotNull Inventory inventory, @NotNull List<ItemStack> items) {
        if (items.isEmpty()) {
            return;
        }
        
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int itemCount = Math.min(random.nextInt(3, 8), items.size());
        int addedCount = 0;

        List<Integer> availableSlots = new ArrayList<>();
        for (int i = 0; i < inventory.getSize(); i++) {
            availableSlots.add(i);
        }
        Collections.shuffle(availableSlots);
        
        for (int i = 0; i < itemCount && i < availableSlots.size(); i++) {
            ItemStack item = items.get(random.nextInt(items.size())).clone();
            int slot = availableSlots.get(i);
            
            inventory.setItem(slot, item);
            addedCount++;
        }

    }
    
    private int random(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }
    
    /**
     * Counts non-empty items in an inventory
     */
    private int countItems(@NotNull Inventory inventory) {
        int count = 0;
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * Generates chest name based on evolution and rarity
     */
    @NotNull
    private String generateChestName(@NotNull String evolution, @NotNull EEvolutionRarityType rarity) {
        String legacyColor = convertToLegacyColor(rarity.getColorCode());
        return legacyColor + evolution + " Treasure Barrel";
    }
    
    /**
     * Converts Adventure color format to legacy format
     */
    @NotNull
    private String convertToLegacyColor(@NotNull String adventureColor) {
        return switch (adventureColor) {
            case "<white>" -> "§f";
            case "<green>" -> "§a";
            case "<blue>" -> "§9";
            case "<dark_purple>" -> "§5";
            case "<gold>" -> "§6";
            case "<red>" -> "§c";
            case "<aqua>" -> "§b";
            case "<light_purple>" -> "§d";
            case "<yellow>" -> "§e";
            case "<dark_aqua>" -> "§3";
            case "<dark_blue>" -> "§1";
            case "<gray>" -> "§7";
            case "<dark_red>" -> "§4";
            case "<dark_gray>" -> "§8";
            case "<black>" -> "§0";
            case "<obfuscated>" -> "§k";
            default -> "§f";
        };
    }
    
    /**
     * Schedules chest removal after timeout
     */
    private void scheduleChestRemoval(@NotNull Block chestBlock, @NotNull Player player) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (chestBlock.getType() == Material.BARREL || chestBlock.getType() == Material.CHEST) {
                    chestBlock.setType(Material.AIR);
                    if (player.isOnline()) {
                        player.sendMessage("§7A treasure barrel has disappeared...");
                    }
                }
            }
        }.runTaskLater(Bukkit.getPluginManager().getPlugin("JExOneblock"), CHEST_DESPAWN_TIME);
    }
}