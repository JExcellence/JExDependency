package de.jexcellence.oneblock.service;

import de.jexcellence.oneblock.database.entity.oneblock.OneblockIsland;
import de.jexcellence.oneblock.utility.workload.biome.DistributedBiomeChanger;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class BiomeService {
    
    private static final Logger LOGGER = Logger.getLogger("JExOneblock");
    
    private final DistributedBiomeChanger biomeChanger;
    private final Map<String, List<Biome>> biomesByCategory;
    private final Map<Biome, BiomeRequirement> biomeRequirements;
    
    public BiomeService(@NotNull DistributedBiomeChanger biomeChanger) {
        this.biomeChanger = biomeChanger;
        this.biomesByCategory = initializeBiomeCategories();
        this.biomeRequirements = initializeBiomeRequirements();
    }
    
    @NotNull
    public CompletableFuture<Boolean> changeBiome(
            @NotNull OneblockIsland island,
            @NotNull Biome biome,
            @NotNull Player player
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!canUseBiome(island, biome)) {
                    LOGGER.warning("Player " + player.getName() + " cannot use biome " + biome.name() + " on island " + island.getIdentifier());
                    return false;
                }
                
                var region = island.getRegion();
                if (region == null) {
                    LOGGER.severe("Island " + island.getIdentifier() + " has no region defined");
                    return false;
                }
                
                var world = org.bukkit.Bukkit.getWorld(region.getWorldName());
                if (world == null) {
                    LOGGER.severe("World " + region.getWorldName() + " not found for island " + island.getIdentifier());
                    return false;
                }
                
                int minX = region.getMinX();
                int minZ = region.getMinZ();
                int maxX = region.getMaxX();
                int maxZ = region.getMaxZ();
                
                LOGGER.info("Starting biome change to " + biome.name() + " for island " + island.getIdentifier() + 
                           " (region: " + minX + "," + minZ + " to " + maxX + "," + maxZ + ")");
                
                var changeResult = biomeChanger.change(
                    region,
                    world,
                    biome,
                    progress -> {
                        if (player.isOnline()) {
                            player.sendMessage("§7Changing biome to " + biome.name() + "... " + 
                                             String.format("%.1f", progress * 100) + "%");
                        }
                    },
                    null
                );
                
                changeResult.thenAccept(result -> {
                    if (result != null && result.getCompletionPercentage() >= 100.0) {
                        LOGGER.info("Successfully changed biome to " + biome.name() + " for island " + island.getIdentifier());
                        
                        if (player.isOnline()) {
                            player.sendMessage("§aIsland biome changed to " + biome.name() + "!");
                        }
                    } else {
                        LOGGER.warning("Failed to change biome for island " + island.getIdentifier());
                        if (player.isOnline()) {
                            player.sendMessage("§cFailed to change island biome. Please try again.");
                        }
                    }
                });
                
                return true;
                
            } catch (Exception e) {
                LOGGER.severe("Error changing biome for island " + island.getIdentifier() + ": " + e.getMessage());
                return false;
            }
        });
    }
    
    @NotNull
    public List<Biome> getAvailableBiomes(@NotNull OneblockIsland island) {
        return Arrays.stream(Biome.values())
            .filter(biome -> canUseBiome(island, biome))
            .sorted(Comparator.comparing(biome -> biome.name()))
            .toList();
    }
    
    public boolean canUseBiome(@NotNull OneblockIsland island, @NotNull Biome biome) {
        var requirement = biomeRequirements.get(biome);
        if (requirement == null) {
            return true;
        }
        
        if (island.getLevel() < requirement.minLevel()) {
            return false;
        }
        
        if (island.getOneblock() != null && island.getOneblock().getPrestigeLevel() < requirement.minPrestige()) {
            return false;
        }
        
        if (island.getIslandCoins() < requirement.coinCost()) {
            return false;
        }
        
        return true;
    }
    
    @NotNull
    public Map<String, List<Biome>> getBiomesByCategory() {
        return new HashMap<>(biomesByCategory);
    }
    
    public BiomeRequirement getBiomeRequirement(@NotNull Biome biome) {
        return biomeRequirements.get(biome);
    }
    
    @NotNull
    public List<Biome> getAvailableBiomesInCategory(@NotNull OneblockIsland island, @NotNull String category) {
        return biomesByCategory.getOrDefault(category, Collections.emptyList())
            .stream()
            .filter(biome -> canUseBiome(island, biome))
            .toList();
    }
    
    public long getBiomeCost(@NotNull Biome biome) {
        var requirement = biomeRequirements.get(biome);
        return requirement != null ? requirement.coinCost() : 0L;
    }
    
    private Map<String, List<Biome>> initializeBiomeCategories() {
        var categories = new HashMap<String, List<Biome>>();
        
        categories.put("Plains", Arrays.asList(
            Biome.PLAINS, Biome.SUNFLOWER_PLAINS, Biome.MEADOW
        ));
        
        categories.put("Forest", Arrays.asList(
            Biome.FOREST, Biome.FLOWER_FOREST, Biome.BIRCH_FOREST, 
            Biome.OLD_GROWTH_BIRCH_FOREST, Biome.DARK_FOREST
        ));
        
        categories.put("Taiga", Arrays.asList(
            Biome.TAIGA, Biome.OLD_GROWTH_PINE_TAIGA, Biome.OLD_GROWTH_SPRUCE_TAIGA,
            Biome.SNOWY_TAIGA
        ));
        
        categories.put("Mountains", Arrays.asList(
            Biome.WINDSWEPT_HILLS, Biome.WINDSWEPT_FOREST, Biome.WINDSWEPT_GRAVELLY_HILLS,
            Biome.GROVE, Biome.SNOWY_SLOPES, Biome.JAGGED_PEAKS, Biome.FROZEN_PEAKS
        ));
        
        categories.put("Desert", Arrays.asList(
            Biome.DESERT, Biome.BADLANDS, Biome.WOODED_BADLANDS, Biome.ERODED_BADLANDS,
            Biome.SAVANNA, Biome.SAVANNA_PLATEAU, Biome.WINDSWEPT_SAVANNA
        ));
        
        categories.put("Cold", Arrays.asList(
            Biome.SNOWY_PLAINS, Biome.ICE_SPIKES, Biome.SNOWY_BEACH, Biome.FROZEN_RIVER
        ));
        
        categories.put("Jungle", Arrays.asList(
            Biome.JUNGLE, Biome.SPARSE_JUNGLE, Biome.BAMBOO_JUNGLE
        ));
        
        categories.put("Swamp", Arrays.asList(
            Biome.SWAMP, Biome.MANGROVE_SWAMP
        ));
        
        categories.put("Ocean", Arrays.asList(
            Biome.OCEAN, Biome.DEEP_OCEAN, Biome.LUKEWARM_OCEAN, Biome.DEEP_LUKEWARM_OCEAN,
            Biome.WARM_OCEAN, Biome.COLD_OCEAN, Biome.DEEP_COLD_OCEAN, Biome.FROZEN_OCEAN,
            Biome.DEEP_FROZEN_OCEAN
        ));
        
        categories.put("Coastal", Arrays.asList(
            Biome.BEACH, Biome.STONY_SHORE, Biome.RIVER, Biome.MUSHROOM_FIELDS
        ));
        
        categories.put("Nether", Arrays.asList(
            Biome.NETHER_WASTES, Biome.CRIMSON_FOREST, Biome.WARPED_FOREST,
            Biome.SOUL_SAND_VALLEY, Biome.BASALT_DELTAS
        ));
        
        categories.put("End", Arrays.asList(
            Biome.THE_END, Biome.END_HIGHLANDS, Biome.END_MIDLANDS, 
            Biome.SMALL_END_ISLANDS, Biome.END_BARRENS
        ));
        
        categories.put("Caves", Arrays.asList(
            Biome.DRIPSTONE_CAVES, Biome.LUSH_CAVES, Biome.DEEP_DARK
        ));
        
        return categories;
    }
    
    private Map<Biome, BiomeRequirement> initializeBiomeRequirements() {
        var requirements = new HashMap<Biome, BiomeRequirement>();
        
        requirements.put(Biome.JUNGLE, new BiomeRequirement(10, 0, 1000L));
        requirements.put(Biome.DESERT, new BiomeRequirement(5, 0, 500L));
        requirements.put(Biome.TAIGA, new BiomeRequirement(8, 0, 750L));
        requirements.put(Biome.SNOWY_PLAINS, new BiomeRequirement(12, 0, 1500L));
        requirements.put(Biome.MUSHROOM_FIELDS, new BiomeRequirement(20, 0, 5000L));
        
        requirements.put(Biome.OCEAN, new BiomeRequirement(15, 0, 2000L));
        requirements.put(Biome.DEEP_OCEAN, new BiomeRequirement(25, 0, 7500L));
        requirements.put(Biome.WARM_OCEAN, new BiomeRequirement(18, 0, 3000L));
        requirements.put(Biome.FROZEN_OCEAN, new BiomeRequirement(22, 0, 4000L));
        
        requirements.put(Biome.WINDSWEPT_HILLS, new BiomeRequirement(15, 0, 2500L));
        requirements.put(Biome.JAGGED_PEAKS, new BiomeRequirement(30, 0, 10000L));
        requirements.put(Biome.FROZEN_PEAKS, new BiomeRequirement(35, 0, 15000L));
        
        requirements.put(Biome.BADLANDS, new BiomeRequirement(25, 0, 8000L));
        requirements.put(Biome.ICE_SPIKES, new BiomeRequirement(30, 0, 12000L));
        requirements.put(Biome.FLOWER_FOREST, new BiomeRequirement(20, 0, 6000L));
        
        requirements.put(Biome.NETHER_WASTES, new BiomeRequirement(40, 1, 20000L));
        requirements.put(Biome.CRIMSON_FOREST, new BiomeRequirement(45, 1, 25000L));
        requirements.put(Biome.WARPED_FOREST, new BiomeRequirement(45, 1, 25000L));
        requirements.put(Biome.SOUL_SAND_VALLEY, new BiomeRequirement(50, 1, 30000L));
        requirements.put(Biome.BASALT_DELTAS, new BiomeRequirement(50, 1, 30000L));
        
        requirements.put(Biome.THE_END, new BiomeRequirement(60, 2, 50000L));
        requirements.put(Biome.END_HIGHLANDS, new BiomeRequirement(65, 2, 75000L));
        requirements.put(Biome.SMALL_END_ISLANDS, new BiomeRequirement(70, 3, 100000L));
        
        requirements.put(Biome.DRIPSTONE_CAVES, new BiomeRequirement(35, 0, 15000L));
        requirements.put(Biome.LUSH_CAVES, new BiomeRequirement(40, 1, 20000L));
        requirements.put(Biome.DEEP_DARK, new BiomeRequirement(75, 3, 150000L));
        
        return requirements;
    }
    
    public record BiomeRequirement(
        int minLevel,
        int minPrestige,
        long coinCost
    ) {}
}