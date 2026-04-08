package de.jexcellence.oneblock.listener;

import com.raindropcentral.rplatform.logging.CentralLogger;
import de.jexcellence.jextranslate.i18n.I18n;
import de.jexcellence.oneblock.JExOneblock;
import de.jexcellence.oneblock.config.CoinConfiguration;
import de.jexcellence.oneblock.config.DropConfiguration;
import de.jexcellence.oneblock.config.OneblockGameplaySection;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockCore;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockEvolution;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockIsland;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockPlayer;
import de.jexcellence.oneblock.database.repository.OneblockIslandRepository;
import de.jexcellence.oneblock.database.repository.OneblockPlayerRepository;
import de.jexcellence.oneblock.manager.IslandStorageManager;
import de.jexcellence.oneblock.region.IslandRegionManager;
import de.jexcellence.oneblock.region.RegionBoundaryChecker;
import de.jexcellence.oneblock.service.EvolutionChestSpawningService;
import de.jexcellence.oneblock.service.EvolutionMobSpawningService;
import de.jexcellence.oneblock.service.IOneblockService;
import de.jexcellence.oneblock.service.IslandCacheService;
import de.jexcellence.oneblock.type.EEvolutionRarityType;
import de.jexcellence.oneblock.utility.OneblockEffectsManager;
import de.jexcellence.oneblock.utility.OneblockEventLogger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

public class OneblockBlockBreakListener implements Listener {

    private static final Logger LOGGER = CentralLogger.getLoggerByName("JExOneblock");

    private final JExOneblock plugin;
    private final IOneblockService oneblockService;
    private final OneblockPlayerRepository playerRepository;
    private final OneblockIslandRepository islandRepository;
    private final OneblockGameplaySection gameplayConfig;
    private final CoinConfiguration coinConfig;
    private final DropConfiguration dropConfig;
    private final OneblockEffectsManager effectsManager;
    private final OneblockEventLogger eventLogger;
    private final IslandCacheService islandCacheService;
    private final EvolutionMobSpawningService mobSpawningService;
    private final EvolutionChestSpawningService chestSpawningService;

    private final IslandRegionManager regionManager;
    private final RegionBoundaryChecker boundaryChecker;

    public OneblockBlockBreakListener(@NotNull JExOneblock plugin) {
        this.plugin = plugin;
        this.oneblockService = plugin.getOneblockService();
        
        this.playerRepository = plugin.getOneblockPlayerRepository();
        this.islandRepository = plugin.getOneblockIslandRepository();
        this.gameplayConfig = plugin.getGameplayConfig();
        
        this.coinConfig = new CoinConfiguration();
        this.dropConfig = new DropConfiguration();
        try {
            File configFile = new File(plugin.getPlugin().getDataFolder(), "configs/gameplay.yml");
            if (configFile.exists()) {
                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                
                coinConfig.loadFromConfig(config);
                dropConfig.loadFromConfig(config);
                
                LOGGER.info("CoinConfiguration and DropConfiguration loaded successfully from gameplay.yml");
            } else {
                LOGGER.warning("gameplay.yml not found, using default configurations");
            }
        } catch (Exception e) {
            LOGGER.warning("Configurations using defaults due to config loading error: " + e.getMessage());
        }
        
        OneblockEffectsManager tempEffectsManager = null;
        try {
            if (gameplayConfig != null) {
                tempEffectsManager = new OneblockEffectsManager(gameplayConfig);
                LOGGER.info("EffectsManager initialized successfully");
            } else {
                LOGGER.warning("EffectsManager not initialized due to missing GameplayConfig");
            }
        } catch (Exception e) {
            LOGGER.warning("EffectsManager not available: " + e.getMessage());
        }
        this.effectsManager = tempEffectsManager;
        
        this.eventLogger = new OneblockEventLogger(false, true, true);
        this.islandCacheService = plugin.getIslandCacheService();
        this.mobSpawningService = new EvolutionMobSpawningService();
        this.chestSpawningService = new EvolutionChestSpawningService();

        try {
            this.regionManager = plugin.getRegionManager();
            this.boundaryChecker = plugin.getBoundaryChecker();
            LOGGER.info("Region management integration initialized for OneBlock listener");
        } catch (Exception e) {
            LOGGER.warning("Region management not available for OneBlock listener: " + e.getMessage());
            throw new IllegalStateException("Region management is required for OneBlock functionality", e);
        }
        
        LOGGER.info("OneblockBlockBreakListener initialized - CobblestoneGenerator and OneBlock are separate systems");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBlockBreak(@NotNull BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Location location = block.getLocation();

        if (event.isCancelled()) {
            return;
        }

        if (!isOneblockBreak(player, location)) {
            return;
        }

        if (!validateOneBlockRegionPermissions(player, location)) {
            event.setCancelled(true);
            return;
        }

        event.setCancelled(true);

        CompletableFuture.runAsync(() -> processOneblockBreak(event, player, block), plugin.getExecutor())
            .exceptionally(throwable -> {
                LOGGER.severe("Error processing OneBlock break for player " + player.getName() + ": " + throwable.getMessage());
                throwable.printStackTrace();
                Bukkit.getScheduler().runTask(plugin.getPlugin(), () -> {
                    event.setCancelled(false);
                });
                return null;
            });
    }

    private boolean isOneblockBreak(@NotNull Player player, @NotNull Location location) {
        if (oneblockService == null) {
            return false;
        }
        
        try {
            var playerOpt = Optional.ofNullable(playerRepository.findByUuid(player.getUniqueId()));
            if (playerOpt.isEmpty()) {
                return false;
            }
            
            var oneblockPlayer = playerOpt.get();
            if (!oneblockPlayer.hasIsland()) {
                return false;
            }
            
            var islandOpt = Optional.ofNullable(islandRepository.findByOwner(oneblockPlayer.getUniqueId()));
            if (islandOpt.isEmpty()) {
                return false;
            }
            
            var island = islandOpt.get();
            var core = island.getOneblock();
            if (core == null) {
                return false;
            }
            
            return core.isAtLocation(location);
            
        } catch (Exception e) {
            LOGGER.warning("Error checking OneBlock location: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Validates that the player has permission to break the OneBlock at the given location.
     * This includes both region boundary checks and OneBlock-specific permissions.
     */
    private boolean validateOneBlockRegionPermissions(@NotNull Player player, @NotNull Location location) {
        try {
            if (player.hasPermission("jexoneblock.admin.bypass.region")) {
                return true;
            }

            if (!regionManager.hasPermission(player, location, de.jexcellence.oneblock.database.entity.region.RegionPermission.PermissionTypes.BREAK)) {
                var region = regionManager.findRegionAt(location);
                if (region == null) {
                    new I18n.Builder("region.no_region", player).build().sendMessage();
                } else {
                    new I18n.Builder("region.permission_denied", player)
                        .withPlaceholder("action", "break the OneBlock")
                        .build().sendMessage();
                }
                return false;
            }

            
            return true;
        } catch (Exception e) {
            LOGGER.warning("Error validating OneBlock region permissions for player " + player.getName() + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Updates an island with retry logic to handle OptimisticLockException
     */
    private CompletableFuture<OneblockIsland> updateIslandWithRetry(@NotNull OneblockIsland originalIsland, int maxRetries) {
        return CompletableFuture.supplyAsync(() -> {
            OneblockIsland currentIsland = originalIsland;
            
            for (int attempt = 1; attempt <= maxRetries; attempt++) {
                try {
                    OneblockIsland updated = islandRepository.update(currentIsland);
                    LOGGER.fine("Updated island: " + currentIsland.getIdentifier() + " (attempt " + attempt + ")");
                    return updated;
                } catch (Exception e) {
                    if (e.getCause() != null && e.getCause().getClass().getSimpleName().contains("OptimisticLock")) {
                        if (attempt < maxRetries) {
                            LOGGER.warning("OptimisticLockException on island " + currentIsland.getIdentifier() + 
                                          ", attempt " + attempt + "/" + maxRetries + ". Retrying with fresh entity...");

                            try {
                                Thread.sleep(50 * attempt);
                                
                                OneblockIsland freshIsland = islandRepository.findByIdentifier(currentIsland.getIdentifier());
                                if (freshIsland != null) {
                                    freshIsland.setExperience(currentIsland.getExperience());
                                    freshIsland.setLastVisited(currentIsland.getLastVisited());
                                    if (currentIsland.getOneblock() != null) {
                                        OneblockCore freshCore = freshIsland.getOneblock();
                                        OneblockCore staleCore = currentIsland.getOneblock();
                                        if (freshCore != null && staleCore != null) {
                                            freshCore.setEvolutionExperience(staleCore.getEvolutionExperience());
                                            freshCore.setBreakStreak(staleCore.getBreakStreak());
                                            freshCore.setTotalBlocksBroken(staleCore.getTotalBlocksBroken());
                                        }
                                    }
                                    currentIsland = freshIsland;
                                    LOGGER.info("Retrying with fresh island entity (attempt " + attempt + ")");
                                    continue;
                                }
                            } catch (Exception fetchEx) {
                                LOGGER.warning("Failed to fetch fresh island entity for retry: " + fetchEx.getMessage());
                            }
                        } else {
                            LOGGER.severe("Failed to update island " + currentIsland.getIdentifier() + 
                                          " after " + maxRetries + " attempts due to OptimisticLockException: " + e.getMessage());
                        }
                    }
                    
                    if (attempt == maxRetries) {
                        LOGGER.severe("Failed to update island: " + currentIsland.getIdentifier() + 
                                      " after " + maxRetries + " attempts: " + e.getMessage());
                        throw new RuntimeException("Failed to update island", e);
                    }
                }
            }
            throw new RuntimeException("Update retry loop completed without success");
        });
    }

    /**
     * Processes the OneBlock break with full async operations
     */
    private void processOneblockBreak(@NotNull BlockBreakEvent event, @NotNull Player player, @NotNull Block block) {
        Material originalBlockType = block.getType();
        Location blockLocation = block.getLocation().clone();

        playerRepository.findByUuidAsync(player.getUniqueId())
            .thenCompose(playerOpt -> {
                if (playerOpt.isEmpty()) {
                    LOGGER.warning("Player " + player.getName() + " not found in database during OneBlock break");
                    return CompletableFuture.completedFuture(null);
                }

                OneblockPlayer oneblockPlayer = playerOpt.get();
                if (!oneblockPlayer.hasIsland()) {
                    return CompletableFuture.completedFuture(null);
                }

                OneblockIsland cachedIsland = islandCacheService.getCachedIsland(oneblockPlayer.getUniqueId());
                if (cachedIsland != null) {
                    return CompletableFuture.completedFuture(Optional.of(cachedIsland))
                        .thenCompose(islandOpt -> {
                            if (islandOpt.isEmpty()) {
                                return CompletableFuture.completedFuture(null);
                            }

                            OneblockIsland island = islandOpt.get();

                            if (!isValidOneblockLocation(island, blockLocation)) {
                                return CompletableFuture.completedFuture(null);
                            }

                            return processBreak(event, player, oneblockPlayer, island, block, originalBlockType);
                        });
                } else {
                    return islandRepository.findByOwnerAsync(oneblockPlayer.getUniqueId())
                        .thenCompose(islandOpt -> {
                            if (islandOpt.isEmpty()) {
                                return CompletableFuture.completedFuture(null);
                            }

                            OneblockIsland island = islandOpt.get();

                            islandCacheService.loadPlayerIsland(oneblockPlayer.getUniqueId());

                            if (!isValidOneblockLocation(island, blockLocation)) {
                                return CompletableFuture.completedFuture(null);
                            }

                            return processBreak(event, player, oneblockPlayer, island, block, originalBlockType);
                        });
                }
            })
            .exceptionally(throwable -> {
                LOGGER.severe("Error in OneBlock break processing: " + throwable.getMessage());
                throwable.printStackTrace();
                return null;
            });
    }

    /**
     * Verifies the block location matches the island's OneBlock location
     */
    private boolean isValidOneblockLocation(@NotNull OneblockIsland island, @NotNull Location location) {
        OneblockCore core = island.getOneblock();
        return core != null && core.isAtLocation(location);
    }

    /**
     * Processes the actual OneBlock break with all systems integration
     */
    @NotNull
    private CompletableFuture<Void> processBreak(
            @NotNull BlockBreakEvent event,
            @NotNull Player player,
            @NotNull OneblockPlayer oneblockPlayer,
            @NotNull OneblockIsland island,
            @NotNull Block block,
            @NotNull Material originalBlockType
    ) {
        OneblockCore core = island.getOneblock();
        
        if (core == null) {
            LOGGER.warning("OneBlock core is null for island " + island.getIdentifier());
            return CompletableFuture.completedFuture(null);
        }

        EEvolutionRarityType rarity = determineBreakRarity(island, core);

        double experienceGained = calculateExperienceGained(rarity, core);

        long coinsGained = calculateCoinsGained(rarity, core);

        island.addExperience(experienceGained);
        island.addCoins(coinsGained);
        island.updateLastVisited();

        Material newBlockType = determineNewBlockType(island, core, rarity);

        eventLogger.logBlockBreak(player, island, core, originalBlockType, newBlockType, rarity, experienceGained, block.getLocation());

        if (rarity.getTier() >= EEvolutionRarityType.LEGENDARY.getTier()) {
            eventLogger.logRareBreak(player, island, rarity, newBlockType, block.getLocation());
        }

        if (core.getTotalBlocksBroken() % 50 == 0) {
            islandCacheService.markDirty(player.getUniqueId());
        }

        return CompletableFuture.runAsync(() -> {
            Bukkit.getScheduler().runTask(plugin.getPlugin(), () -> {
                executeMainThreadOperations(
                    event, player, island, core, block, 
                    originalBlockType, newBlockType, rarity, 
                    experienceGained
                );
            });
        });
    }
    
    /**
     * Calculates coins gained based on rarity and evolution level using dynamic ranges
     */
    private long calculateCoinsGained(@NotNull EEvolutionRarityType rarity, @NotNull OneblockCore core) {
        if (!coinConfig.isEnabled()) {
            return 0;
        }

        return coinConfig.calculateCoins(
            rarity,
            core.getEvolutionLevel(),
            core.getPrestigeLevel(),
            core.getBreakStreak()
        );
    }

    /**
     * Determines the rarity for this block break based on various factors
     */
    @NotNull
    private EEvolutionRarityType determineBreakRarity(@NotNull OneblockIsland island, @NotNull OneblockCore core) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double roll = random.nextDouble() * 100.0;

        double evolutionBonus = gameplayConfig != null ? 
            core.getEvolutionLevel() * gameplayConfig.getEvolutionLevelRarityBonus() : 0.0;

        double prestigeBonus = gameplayConfig != null ? 
            core.getPrestigeLevel() * gameplayConfig.getPrestigeLevelRarityBonus() : 0.0;

        double streakBonus = gameplayConfig != null ? 
            Math.min(core.getBreakStreak() * gameplayConfig.getStreakRarityBonus(), 
                    gameplayConfig.getMaxStreakBonus()) : 0.0;

        double totalBonus = (evolutionBonus + prestigeBonus + streakBonus) * 100.0;
        roll = Math.max(0.0, roll - totalBonus);

        Map<String, Double> rarityWeights = gameplayConfig != null ? 
            gameplayConfig.getRarityWeights() : getDefaultRarityWeights();
        
        double cumulative = 0.0;

        cumulative += rarityWeights.getOrDefault("omnipotent", 0.00001);
        if (roll < cumulative) return EEvolutionRarityType.OMNIPOTENT;
        
        cumulative += rarityWeights.getOrDefault("infinite", 0.00002);
        if (roll < cumulative) return EEvolutionRarityType.INFINITE;
        
        cumulative += rarityWeights.getOrDefault("cosmic", 0.00005);
        if (roll < cumulative) return EEvolutionRarityType.COSMIC;
        
        cumulative += rarityWeights.getOrDefault("ethereal", 0.0001);
        if (roll < cumulative) return EEvolutionRarityType.ETHEREAL;
        
        cumulative += rarityWeights.getOrDefault("transcendent", 0.0002);
        if (roll < cumulative) return EEvolutionRarityType.TRANSCENDENT;
        
        cumulative += rarityWeights.getOrDefault("celestial", 0.0005);
        if (roll < cumulative) return EEvolutionRarityType.CELESTIAL;
        
        cumulative += rarityWeights.getOrDefault("divine", 0.001);
        if (roll < cumulative) return EEvolutionRarityType.DIVINE;
        
        cumulative += rarityWeights.getOrDefault("mythical", 0.003);
        if (roll < cumulative) return EEvolutionRarityType.MYTHICAL;
        
        cumulative += rarityWeights.getOrDefault("unique", 0.015);
        if (roll < cumulative) return EEvolutionRarityType.UNIQUE;
        
        cumulative += rarityWeights.getOrDefault("special", 0.08);
        if (roll < cumulative) return EEvolutionRarityType.SPECIAL;
        
        cumulative += rarityWeights.getOrDefault("legendary", 0.4);
        if (roll < cumulative) return EEvolutionRarityType.LEGENDARY;
        
        cumulative += rarityWeights.getOrDefault("epic", 1.5);
        if (roll < cumulative) return EEvolutionRarityType.EPIC;
        
        cumulative += rarityWeights.getOrDefault("rare", 5.0);
        if (roll < cumulative) return EEvolutionRarityType.RARE;
        
        cumulative += rarityWeights.getOrDefault("uncommon", 18.0);
        if (roll < cumulative) return EEvolutionRarityType.UNCOMMON;

        return EEvolutionRarityType.COMMON;
    }
    
    private Map<String, Double> getDefaultRarityWeights() {
        Map<String, Double> weights = new HashMap<>();
        weights.put("common", 75.0);
        weights.put("uncommon", 18.0);
        weights.put("rare", 5.0);
        weights.put("epic", 1.5);
        weights.put("legendary", 0.4);
        weights.put("special", 0.08);
        weights.put("unique", 0.015);
        weights.put("mythical", 0.003);
        weights.put("divine", 0.001);
        weights.put("celestial", 0.0005);
        weights.put("transcendent", 0.0002);
        weights.put("ethereal", 0.0001);
        weights.put("cosmic", 0.00005);
        weights.put("infinite", 0.00002);
        weights.put("omnipotent", 0.00001);
        return weights;
    }

    /**
     * Calculates experience gained based on rarity and other factors
     */
    private double calculateExperienceGained(@NotNull EEvolutionRarityType rarity, @NotNull OneblockCore core) {
        double baseExperience = gameplayConfig != null ? gameplayConfig.getBaseExperiencePerBreak() : 1.0;
        double rarityMultiplier = rarity.getPassiveXpValue();
        double evolutionMultiplier = gameplayConfig != null ? 
            1.0 + (core.getEvolutionLevel() * gameplayConfig.getEvolutionExperienceMultiplier()) : 1.0;
        double prestigeMultiplier = gameplayConfig != null ? 
            1.0 + (core.getPrestigeLevel() * gameplayConfig.getPrestigeExperienceMultiplier()) : 1.0;
        
        return baseExperience * rarityMultiplier * evolutionMultiplier * prestigeMultiplier;
    }

    /**
     * Determines the new block type to place
     */
    @NotNull
    private Material determineNewBlockType(
            @NotNull OneblockIsland island,
            @NotNull OneblockCore core,
            @NotNull EEvolutionRarityType rarity
    ) {
        String evolutionName = core.getCurrentEvolution();
        
        LOGGER.info("Determining block for evolution: " + evolutionName + ", rarity: " + rarity);

        Material evolutionBlock = getBlockFromEvolution(evolutionName, rarity);
        if (evolutionBlock != null) {
            LOGGER.info("Using evolution block: " + evolutionBlock + " for " + evolutionName + " " + rarity);
            return evolutionBlock;
        }

        Map<String, Double> availableBlocks = null;
        if (gameplayConfig != null) {
            availableBlocks = gameplayConfig.getEvolutionBlocks(evolutionName, rarity);
        }
        
        if (availableBlocks != null && !availableBlocks.isEmpty()) {
            Material configBlock = selectWeightedBlock(availableBlocks);
            LOGGER.info("Using config block: " + configBlock + " for " + evolutionName + " " + rarity);
            return configBlock;
        }

        Material defaultBlock = getDefaultBlockForRarity(rarity);
        LOGGER.warning("Using fallback block: " + defaultBlock + " for " + evolutionName + " " + rarity + " - this should not happen in Genesis!");
        return defaultBlock;
    }
    
    /**
     * Gets a block from the actual evolution configuration based on current evolution level
     */
    @Nullable
    private Material getBlockFromEvolution(@NotNull String evolutionName, @NotNull EEvolutionRarityType rarity) {
        try {
            var evolutionFactory = de.jexcellence.oneblock.factory.EvolutionFactory.getInstance();

            if ("Genesis".equalsIgnoreCase(evolutionName)) {
                var evolution = evolutionFactory.getCachedEvolution("Genesis");
                if (evolution != null) {
                    return getBlockFromEvolutionObjectWithFallback(evolution, rarity);
                }
            }

            var evolution = evolutionFactory.getCachedEvolution(evolutionName);
            if (evolution != null) {
                Material block = getBlockFromEvolutionObjectWithFallback(evolution, rarity);
                if (block != null) {
                    return block;
                }
            }

            
        } catch (Exception e) {
            LOGGER.warning("Failed to get blocks from evolution " + evolutionName + ": " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Extracts a block from an evolution object for the given rarity, with intelligent fallback
     */
    @Nullable
    private Material getBlockFromEvolutionObjectWithFallback(@NotNull OneblockEvolution evolution, @NotNull EEvolutionRarityType requestedRarity) {
        Material block = getBlockFromEvolutionObject(evolution, requestedRarity);
        if (block != null) {
            return block;
        }

        EEvolutionRarityType fallbackRarity = findHighestAvailableRarity(evolution, requestedRarity);
        if (fallbackRarity != null && fallbackRarity != requestedRarity) {
            LOGGER.info("Rarity " + requestedRarity + " not available in " + evolution.getEvolutionName() + 
                       ", using highest available: " + fallbackRarity);
            return getBlockFromEvolutionObject(evolution, fallbackRarity);
        }
        
        return null;
    }
    
    /**
     * Finds the highest available rarity in an evolution that is <= the requested rarity
     */
    @Nullable
    private EEvolutionRarityType findHighestAvailableRarity(@NotNull OneblockEvolution evolution, @NotNull EEvolutionRarityType maxRarity) {
        var availableRarities = evolution.getBlocks().stream()
            .filter(block -> block.isValid())
            .map(block -> block.getRarity())
            .filter(rarity -> rarity.getTier() <= maxRarity.getTier())
            .distinct()
            .sorted((a, b) -> Integer.compare(b.getTier(), a.getTier()))
            .toList();
        
        return availableRarities.isEmpty() ? null : availableRarities.get(0);
    }
    
    /**
     * Extracts a block from an evolution object for the given rarity
     */
    @Nullable
    private Material getBlockFromEvolutionObject(@NotNull OneblockEvolution evolution, @NotNull EEvolutionRarityType rarity) {
        var evolutionBlocks = evolution.getBlocks().stream()
            .filter(block -> block.getRarity() == rarity && block.isValid())
            .findFirst();
        
        if (evolutionBlocks.isPresent()) {
            var blockConfig = evolutionBlocks.get();
            var materials = blockConfig.getMaterials();
            
            if (materials != null && !materials.isEmpty()) {
                ThreadLocalRandom random = ThreadLocalRandom.current();
                return materials.get(random.nextInt(materials.size()));
            }
        }
        
        return null;
    }

    /**
     * Gets default block for rarity as fallback - Genesis-appropriate blocks only
     */
    @NotNull
    private Material getDefaultBlockForRarity(@NotNull EEvolutionRarityType rarity) {
        return switch (rarity) {
            case COMMON -> Material.COBBLESTONE;
            case UNCOMMON -> Material.STONE;
            case RARE -> Material.COAL_ORE;
            case EPIC -> Material.IRON_ORE;
            case LEGENDARY -> Material.GOLD_ORE;
            case SPECIAL -> Material.DIAMOND_ORE;
            case UNIQUE -> Material.EMERALD_ORE;
            default -> Material.GRASS_BLOCK;
        };
    }

    /**
     * Selects a block based on weighted probabilities
     */
    @NotNull
    private Material selectWeightedBlock(@NotNull Map<String, Double> blocks) {
        double totalWeight = blocks.values().stream().mapToDouble(Double::doubleValue).sum();
        double random = ThreadLocalRandom.current().nextDouble() * totalWeight;
        
        double currentWeight = 0.0;
        for (Map.Entry<String, Double> entry : blocks.entrySet()) {
            currentWeight += entry.getValue();
            if (random <= currentWeight) {
                try {
                    return Material.valueOf(entry.getKey().toUpperCase());
                } catch (IllegalArgumentException e) {
                    LOGGER.warning("Invalid material in config: " + entry.getKey());
                }
            }
        }

        return Material.COBBLESTONE;
    }

    /**
     * Executes all main thread operations (block placement, effects, messages)
     */
    private void executeMainThreadOperations(
            @NotNull BlockBreakEvent event,
            @NotNull Player player,
            @NotNull OneblockIsland island,
            @NotNull OneblockCore core,
            @NotNull Block block,
            @NotNull Material originalBlockType,
            @NotNull Material newBlockType,
            @NotNull EEvolutionRarityType rarity,
            double experienceGained
    ) {

        event.setDropItems(false);

        handleEnchantedBlockDrops(player, block, originalBlockType, island);

        if (newBlockType.isBlock()) {
            block.setType(newBlockType, false);
        } else {
            LOGGER.warning("Attempted to set non-block material " + newBlockType + " as block, using COBBLESTONE instead");
            block.setType(Material.COBBLESTONE, false);
        }

        playRarityEffects(player, block.getLocation(), rarity);

        sendProgressMessages(player, island, core, rarity, experienceGained);

        checkEvolutionAdvancement(player, island, core);

        handleSpecialEvents(player, island, block, rarity);

        damagePlayerTool(player);
    }

    /**
     * Handles block drops with proper collection
     */
    private void handleBlockDrops(@NotNull Player player, @NotNull Block block, @NotNull Material originalBlockType, @NotNull OneblockIsland island) {
        IslandStorageManager storageManager = plugin.getIslandStorageManager();
        if (storageManager == null) {
            LOGGER.warning("Storage manager not available, items will be dropped normally");
            return;
        }

        block.getDrops(player.getInventory().getItemInMainHand(), player)
            .forEach(drop -> {
                storageManager.handleOneBlockDrop(player, island.getId(), drop);
            });
    }
    
    /**
     * Handles enchanted block drops (Fortune, Silk Touch, etc.)
     */
    private void handleEnchantedBlockDrops(@NotNull Player player, @NotNull Block block, @NotNull Material originalBlockType, @NotNull OneblockIsland island) {
        IslandStorageManager storageManager = plugin.getIslandStorageManager();
        if (storageManager == null) {
            ItemStack tool = player.getInventory().getItemInMainHand();
            Collection<ItemStack> drops = block.getDrops(tool, player);
            drops.forEach(drop -> player.getInventory().addItem(drop));
            return;
        }
        
        ItemStack tool = player.getInventory().getItemInMainHand();
        Location blockLocation = block.getLocation();

        Collection<ItemStack> drops = block.getDrops(tool, player);
        
        if (drops.isEmpty()) {
            ItemStack originalBlock = new ItemStack(originalBlockType, 1);
            storageManager.handleOneBlockDrop(player, island.getId(), originalBlock, blockLocation);
        } else {
            drops.forEach(drop -> {
                storageManager.handleOneBlockDrop(player, island.getId(), drop, blockLocation);
            });
        }
    }

    /**
     * Plays effects based on rarity
     */
    private void playRarityEffects(@NotNull Player player, @NotNull Location location, @NotNull EEvolutionRarityType rarity) {
        if (effectsManager != null) {
            effectsManager.playBreakEffects(player, location, rarity);

            if (rarity.getTier() >= EEvolutionRarityType.LEGENDARY.getTier()) {
                effectsManager.announceRareBreak(player, rarity);
            }
        } else {
            player.playSound(location, org.bukkit.Sound.BLOCK_STONE_BREAK, 1.0f, 1.0f);
            if (rarity.getTier() >= EEvolutionRarityType.LEGENDARY.getTier()) {
                String rarityName = convertToLegacyColors(rarity.getFormattedName());
                player.sendMessage("§6§lRARE DROP! §e" + rarityName);
            }
        }
    }
    
    /**
     * Converts MiniMessage format to legacy color codes properly
     */
    private String convertToLegacyColors(String miniMessage) {
        if (miniMessage == null) return "";
        
        return miniMessage
            .replace("<black>", "§0")
            .replace("<dark_blue>", "§1")
            .replace("<dark_green>", "§2")
            .replace("<dark_aqua>", "§3")
            .replace("<dark_red>", "§4")
            .replace("<dark_purple>", "§5")
            .replace("<gold>", "§6")
            .replace("<gray>", "§7")
            .replace("<dark_gray>", "§8")
            .replace("<blue>", "§9")
            .replace("<green>", "§a")
            .replace("<aqua>", "§b")
            .replace("<red>", "§c")
            .replace("<light_purple>", "§d")
            .replace("<yellow>", "§e")
            .replace("<white>", "§f")
            .replace("<bold>", "§l")
            .replace("<italic>", "§o")
            .replace("<underlined>", "§n")
            .replace("<strikethrough>", "§m")
            .replace("<obfuscated>", "§k")
            .replace("<reset>", "§r");
    }

    /**
     * Sends progress messages to the player
     */
    private void sendProgressMessages(
            @NotNull Player player,
            @NotNull OneblockIsland island,
            @NotNull OneblockCore core,
            @NotNull EEvolutionRarityType rarity,
            double experienceGained
    ) {
        net.kyori.adventure.text.Component actionBarComponent = new I18n.Builder("oneblock.break.progress", player)
            .withPlaceholder("evolution", core.getCurrentEvolution())
            .withPlaceholder("level", String.valueOf(core.getEvolutionLevel()))
            .withPlaceholder("experience", String.format("%.1f", core.getEvolutionExperience()))
            .withPlaceholder("rarity", rarity.getFormattedName())
            .withPlaceholder("experience_gained", String.format("%.1f", experienceGained))
            .withPlaceholder("total_blocks", String.valueOf(core.getTotalBlocksBroken()))
            .withPlaceholder("streak", String.valueOf(core.getBreakStreak()))
            .build().component();

        player.sendActionBar(actionBarComponent);


        if (rarity.getTier() >= EEvolutionRarityType.LEGENDARY.getTier()) {
            I18n rarityI18n = new I18n.Builder("oneblock.break.rare_drop", player)
                .withPlaceholders(Map.of(
                    "rarity", rarity.getFormattedName(),
                    "player", player.getName()
                ))
                .build();

            if (rarity.getTier() >= EEvolutionRarityType.DIVINE.getTier()) {
                Bukkit.broadcast((net.kyori.adventure.text.Component) rarityI18n.component());
            } else {
                rarityI18n.sendMessage();
            }
        }
    }

    /**
     * Checks and handles evolution advancement
     */
    private void checkEvolutionAdvancement(@NotNull Player player, @NotNull OneblockIsland island, @NotNull OneblockCore core) {
        if (gameplayConfig == null) {
            LOGGER.warning("GameplayConfig is null, skipping evolution advancement check");
            return;
        }
        
        double requiredExperience = gameplayConfig.getRequiredExperienceForLevel(core.getEvolutionLevel() + 1);
        
        if (core.getEvolutionExperience() >= requiredExperience) {
            int previousLevel = core.getEvolutionLevel();

            core.nextEvolutionLevel();

            eventLogger.logEvolutionLevelUp(player, island, core, previousLevel, core.getEvolutionLevel());

            new I18n.Builder("oneblock.evolution.level_up", player)
                .withPlaceholders(Map.of(
                    "evolution", core.getCurrentEvolution(),
                    "level", String.valueOf(core.getEvolutionLevel())
                ))
                .build().sendMessage();

            if (effectsManager != null) {
                effectsManager.playLevelUpEffects(player, core.getEvolutionLevel());
            } else {
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            }

            checkEvolutionChange(player, island, core);
        }
    }

    /**
     * Checks if the player should advance to a new evolution
     */
    private void checkEvolutionChange(@NotNull Player player, @NotNull OneblockIsland island, @NotNull OneblockCore core) {
        if (gameplayConfig == null) {
            LOGGER.warning("GameplayConfig is null, skipping evolution change check");
            return;
        }
        
        String nextEvolution = gameplayConfig.getNextEvolution(core.getCurrentEvolution(), core.getEvolutionLevel());
        
        if (nextEvolution != null && !nextEvolution.equals(core.getCurrentEvolution())) {
            String previousEvolution = core.getCurrentEvolution();
            core.changeEvolution(nextEvolution, true);

            eventLogger.logEvolutionChange(player, island, previousEvolution, nextEvolution, core.getEvolutionLevel());
            
            new I18n.Builder("oneblock.evolution.change", player)
                .withPlaceholder("evolution", nextEvolution)
                .build().sendMessage();

            if (effectsManager != null) {
                effectsManager.playEvolutionAdvancementEffects(player, nextEvolution);
            } else {
                player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
            }
        }
    }

    /**
     * Handles special events like chest spawning, entity spawning, etc.
     */
    private void handleSpecialEvents(@NotNull Player player, @NotNull OneblockIsland island, @NotNull Block block, @NotNull EEvolutionRarityType rarity) {
        if (gameplayConfig == null) {
            LOGGER.warning("GameplayConfig is null, skipping special events");
            return;
        }
        
        ThreadLocalRandom random = ThreadLocalRandom.current();

        double chestChance = gameplayConfig.getChestSpawnChance(rarity);
        if (random.nextDouble() < chestChance) {
            handleChestSpawn(player, block, rarity);
        }

        double entityChance = gameplayConfig.getEntitySpawnChance(rarity);
        if (random.nextDouble() < entityChance) {
            handleEntitySpawn(player, block, rarity);
        }

        double itemChance = gameplayConfig.getSpecialItemChance(rarity);
        if (random.nextDouble() < itemChance) {
            handleSpecialItemDrop(player, block, rarity);
        }
    }

    /**
     * Handles chest spawning
     */
    private void handleChestSpawn(@NotNull Player player, @NotNull Block block, @NotNull EEvolutionRarityType rarity) {
        OneblockPlayer oneblockPlayer = playerRepository.findByUuid(player.getUniqueId());
        if (oneblockPlayer == null || !oneblockPlayer.hasIsland()) {
            return;
        }
        
        OneblockIsland island = islandRepository.findByOwner(oneblockPlayer.getUniqueId());
        if (island == null || island.getOneblock() == null) {
            return;
        }
        
        OneblockCore core = island.getOneblock();

        Block chestBlock = chestSpawningService.spawnEvolutionChest(player, core, block.getLocation(), rarity);
        
        if (chestBlock != null) {
            new I18n.Builder("oneblock.event.chest_spawn", player)
                .withPlaceholder("rarity", rarity.getFormattedName())
                .build().sendMessage();
            player.playSound(block.getLocation(), org.bukkit.Sound.BLOCK_CHEST_OPEN, 1.0f, 1.2f);
        }
    }

    /**
     * Handles entity spawning
     */
    private void handleEntitySpawn(@NotNull Player player, @NotNull Block block, @NotNull EEvolutionRarityType rarity) {
        OneblockPlayer oneblockPlayer = playerRepository.findByUuid(player.getUniqueId());
        if (oneblockPlayer == null || !oneblockPlayer.hasIsland()) {
            return;
        }
        
        OneblockIsland island = islandRepository.findByOwner(oneblockPlayer.getUniqueId());
        if (island == null || island.getOneblock() == null) {
            return;
        }
        
        OneblockCore core = island.getOneblock();

        org.bukkit.entity.Entity entity = mobSpawningService.spawnEvolutionMob(player, core, block.getLocation(), rarity);
        
        if (entity != null) {
            boolean isHostile = entity instanceof org.bukkit.entity.Monster;

            new I18n.Builder(isHostile ? "oneblock.event.mob_spawn_hostile" : "oneblock.event.mob_spawn_friendly", player)
                .withPlaceholder("rarity", rarity.getFormattedName())
                .withPlaceholder("mob_type", entity.getType().name())
                .build().sendMessage();

            if (isHostile) {
                player.playSound(block.getLocation(), org.bukkit.Sound.ENTITY_ZOMBIE_AMBIENT, 1.0f, 0.8f);
            } else {
                player.playSound(block.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_AMBIENT, 1.0f, 1.2f);
            }
        }
    }

    /**
     * Handles special item drops
     */
    private void handleSpecialItemDrop(@NotNull Player player, @NotNull Block block, @NotNull EEvolutionRarityType rarity) {
        OneblockPlayer oneblockPlayer = playerRepository.findByUuid(player.getUniqueId());
        if (oneblockPlayer == null || !oneblockPlayer.hasIsland()) {
            return;
        }
        
        OneblockIsland island = islandRepository.findByOwner(oneblockPlayer.getUniqueId());
        if (island == null || island.getOneblock() == null) {
            return;
        }
        
        OneblockCore core = island.getOneblock();
        String evolution = core.getCurrentEvolution();
        int evolutionLevel = core.getEvolutionLevel();
        Location blockLocation = block.getLocation();

        ItemStack specialItem = generateSpecialItem(evolution, evolutionLevel, rarity, core);
        
        if (specialItem != null) {
            IslandStorageManager storageManager = plugin.getIslandStorageManager();
            if (storageManager != null) {
                storageManager.handleOneBlockDrop(player, island.getId(), specialItem, blockLocation);

                new I18n.Builder("oneblock.event.special_item", player)
                    .withPlaceholder("rarity", rarity.getFormattedName())
                    .withPlaceholder("item", specialItem.getType().name())
                    .build().sendMessage();
                player.playSound(blockLocation, org.bukkit.Sound.ENTITY_ITEM_PICKUP, 1.0f, 1.5f);
            }
        }
    }
    
    /**
     * Generates evolution and rarity specific special items
     */
    @Nullable
    private ItemStack generateSpecialItem(@NotNull String evolution, int evolutionLevel, @NotNull EEvolutionRarityType rarity, @NotNull OneblockCore core) {
        ItemStack evolutionItem = getItemFromEvolution(evolution, rarity);
        if (evolutionItem != null) {
            int amount = dropConfig.calculateDropAmount(
                rarity,
                evolutionLevel,
                core.getPrestigeLevel()
            );
            evolutionItem.setAmount(amount);
            return evolutionItem;
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();

        Material baseMaterial = switch (evolution.toLowerCase()) {
            case "genesis" -> Material.APPLE;
            case "terra" -> Material.EMERALD;
            case "aqua" -> Material.PRISMARINE_CRYSTALS;
            case "ignis" -> Material.BLAZE_POWDER;
            case "ventus" -> Material.FEATHER;
            case "stone" -> Material.IRON_INGOT;
            case "iron" -> Material.IRON_INGOT;
            case "gold" -> Material.GOLD_INGOT;
            case "diamond" -> Material.DIAMOND;
            case "nether" -> Material.NETHERITE_SCRAP;
            case "end" -> Material.ENDER_PEARL;
            case "cosmic", "stellar", "galactic" -> Material.NETHER_STAR;
            default -> Material.STICK;
        };

        int amount = dropConfig.calculateDropAmount(
            rarity,
            evolutionLevel,
            core.getPrestigeLevel()
        );
        
        return new ItemStack(baseMaterial, amount);
    }
    
    /**
     * Gets an item from the actual evolution configuration
     */
    @Nullable
    private ItemStack getItemFromEvolution(@NotNull String evolutionName, @NotNull EEvolutionRarityType rarity) {
        try {
            var evolutionFactory = de.jexcellence.oneblock.factory.EvolutionFactory.getInstance();

            if ("Genesis".equalsIgnoreCase(evolutionName)) {
                var evolution = evolutionFactory.getCachedEvolution("Genesis");
                if (evolution != null) {
                    return getItemFromEvolutionObjectWithFallback(evolution, rarity);
                }
            }

            var evolution = evolutionFactory.getCachedEvolution(evolutionName);
            if (evolution != null) {
                ItemStack item = getItemFromEvolutionObjectWithFallback(evolution, rarity);
                if (item != null) {
                    return item;
                }
            }
            
        } catch (Exception e) {
            LOGGER.warning("Failed to get items from evolution " + evolutionName + ": " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Extracts an item from an evolution object for the given rarity with intelligent fallback
     */
    @Nullable
    private ItemStack getItemFromEvolutionObjectWithFallback(@NotNull OneblockEvolution evolution, @NotNull EEvolutionRarityType requestedRarity) {
        ItemStack item = getItemFromEvolutionObject(evolution, requestedRarity);
        if (item != null) {
            return item;
        }

        EEvolutionRarityType fallbackRarity = findHighestAvailableItemRarity(evolution, requestedRarity);
        if (fallbackRarity != null && fallbackRarity != requestedRarity) {
            LOGGER.info("Item rarity " + requestedRarity + " not available in " + evolution.getEvolutionName() + 
                       ", using highest available: " + fallbackRarity);
            return getItemFromEvolutionObject(evolution, fallbackRarity);
        }
        
        return null;
    }
    
    /**
     * Finds the highest available item rarity in an evolution that is <= the requested rarity
     */
    @Nullable
    private EEvolutionRarityType findHighestAvailableItemRarity(@NotNull OneblockEvolution evolution, @NotNull EEvolutionRarityType maxRarity) {
        var availableRarities = evolution.getItems().stream()
            .filter(item -> item.isValid())
            .map(item -> item.getRarity())
            .filter(rarity -> rarity.getTier() <= maxRarity.getTier())
            .distinct()
            .sorted((a, b) -> Integer.compare(b.getTier(), a.getTier()))
            .toList();
        
        return availableRarities.isEmpty() ? null : availableRarities.get(0);
    }
    
    /**
     * Extracts an item from an evolution object for the given rarity
     */
    @Nullable
    private ItemStack getItemFromEvolutionObject(@NotNull OneblockEvolution evolution, @NotNull EEvolutionRarityType rarity) {
        var evolutionItems = evolution.getItems().stream()
            .filter(item -> item.getRarity() == rarity && item.isValid())
            .toList();
        
        if (!evolutionItems.isEmpty()) {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            var selectedItem = evolutionItems.get(random.nextInt(evolutionItems.size()));

            var itemStacks = selectedItem.getItemStacks();
            if (itemStacks != null && !itemStacks.isEmpty()) {
                return itemStacks.get(random.nextInt(itemStacks.size())).clone();
            }
        }
        
        return null;
    }

    /**
     * Damages the player's tool
     */
    private void damagePlayerTool(@NotNull Player player) {
        ItemStack tool = player.getInventory().getItemInMainHand();
        if (tool.getType().getMaxDurability() > 0) {
            var meta = tool.getItemMeta();
            if (meta instanceof org.bukkit.inventory.meta.Damageable damageable) {
                int currentDamage = damageable.getDamage();
                damageable.setDamage(currentDamage + 1);
                tool.setItemMeta(meta);

                if (damageable.getDamage() >= tool.getType().getMaxDurability()) {
                    player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
                }
            }
        }
    }
}