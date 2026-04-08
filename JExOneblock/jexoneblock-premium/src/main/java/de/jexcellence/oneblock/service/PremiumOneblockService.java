package de.jexcellence.oneblock.service;

import de.jexcellence.oneblock.JExOneblock;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockIsland;
import de.jexcellence.oneblock.database.entity.oneblock.OneblockPlayer;
import de.jexcellence.oneblock.database.entity.region.IslandRegion;
import de.jexcellence.oneblock.database.repository.OneblockIslandRepository;
import de.jexcellence.oneblock.database.repository.OneblockPlayerRepository;
import de.jexcellence.oneblock.region.IslandRegionManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class PremiumOneblockService implements IOneblockService {

    private static final Logger LOGGER = Logger.getLogger("JExOneblock");
    
    private final OneblockIslandRepository oneblockIslandRepository;
    private final OneblockPlayerRepository oneblockPlayerRepository;
    private final WorldManagementService worldManagementService;
    private final JExOneblock plugin;
    private final ConcurrentHashMap<UUID, String> playerIslandCache = new ConcurrentHashMap<>();
    private long nextIslandId = 1;

    public PremiumOneblockService(
            OneblockIslandRepository oneblockIslandRepository,
            OneblockPlayerRepository oneblockPlayerRepository,
            WorldManagementService worldManagementService,
            JExOneblock plugin
    ) {
        this.oneblockIslandRepository = oneblockIslandRepository;
        this.oneblockPlayerRepository = oneblockPlayerRepository;
        this.worldManagementService = worldManagementService;
        this.plugin = plugin;
        loadNextIslandId();
    }

    @Override
    public boolean isPremium() {
        return true;
    }

    @Override
    public @NotNull CompletableFuture<Void> createIsland(@NotNull Player player) {
        CompletableFuture<World> worldFuture = new CompletableFuture<>();
        
        Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("JExOneblock"), () -> {
            try {
                var availableWorlds = worldManagementService.getAvailableOneBlockWorlds().join();
                if (availableWorlds.isEmpty()) {
                    worldFuture.completeExceptionally(new RuntimeException("No suitable OneBlock worlds available"));
                    return;
                }

                World world = availableWorlds.get(0);
                LOGGER.info("Using OneBlock world: " + world.getName());
                worldFuture.complete(world);
            } catch (Exception e) {
                worldFuture.completeExceptionally(e);
            }
        });
        
        return worldFuture.thenCompose(world -> {
            if (world == null) {
                return CompletableFuture.failedFuture(new RuntimeException("Failed to create oneblock world"));
            }

            return CompletableFuture.supplyAsync(() -> {
                try {
                    OneblockPlayer playerRecord = oneblockPlayerRepository.findByUuid(player.getUniqueId());
                    if (playerRecord != null && playerRecord.getOwnedIsland() != null) {
                        throw new RuntimeException("Player already has an island");
                    }

                    // Use region management system for island location and region creation
                    IslandRegionManager regionManager = plugin.getRegionManager();
                    CompletableFuture<IslandRegion> regionFuture = regionManager.createIslandRegion(
                        player.getUniqueId(), 
                        world, 
                        150 // Premium islands get larger radius
                    );
                    
                    IslandRegion islandRegion = regionFuture.join();
                    Location islandLocation = islandRegion.getCenterLocation();

                    String islandIdentifier = "premium_island_" + nextIslandId;

                    if (playerRecord == null) {
                        playerRecord = new OneblockPlayer(player);
                        playerRecord = oneblockPlayerRepository.create(playerRecord);
                    }

                    var island = OneblockIsland.builder()
                        .owner(playerRecord)
                        .identifier(islandIdentifier)
                        .islandName(player.getName() + "'s Premium Island")
                        .description("Welcome to " + player.getName() + "'s premium island!")
                        .centerLocation(islandLocation.clone())
                        .currentSize(150) // Match region radius
                        .maximumSize(500)
                        .level(1)
                        .experience(0.0)
                        .privacy(false)
                        .islandCoins(0L)
                        .createdAt(LocalDateTime.now())
                        .lastVisited(LocalDateTime.now())
                        .build();

                    island.initializeEmbeddedComponents();

                    island = oneblockIslandRepository.create(island);

                    final OneblockIsland savedIsland = island;
                    final String finalIslandIdentifier = islandIdentifier;
                    oneblockPlayerRepository.findByUuidAsync(player.getUniqueId())
                        .thenCompose(optPlayer -> {
                            if (optPlayer.isPresent()) {
                                OneblockPlayer freshPlayer = optPlayer.get();
                                freshPlayer.setOwnedIsland(savedIsland);
                                return oneblockPlayerRepository.updateAsync(freshPlayer);
                            }
                            return CompletableFuture.completedFuture(null);
                        }).join();

                    CompletableFuture<Void> generationFuture = new CompletableFuture<>();
                    Bukkit.getScheduler().runTask(plugin.getPlugin(), () -> {
                        try {
                            generatePremiumOneblock(world, islandLocation);
                            generationFuture.complete(null);
                        } catch (Exception e) {
                            generationFuture.completeExceptionally(e);
                        }
                    });

                    generationFuture.join();

                    playerIslandCache.put(player.getUniqueId(), finalIslandIdentifier);
                    nextIslandId++;

                    LOGGER.info("Created premium island " + island.getIdentifier() + " for player " + player.getName() + 
                               " with region at spiral position " + islandRegion.getSpiralPosition());
                    return null;
                } catch (Exception e) {
                    LOGGER.severe("Failed to create premium island for player " + player.getName() + ": " + e.getMessage());
                    throw new RuntimeException(e);
                }
            });
        });
    }

    @Override
    public @NotNull CompletableFuture<Boolean> deleteIsland(@NotNull UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var player = oneblockPlayerRepository.findByUuid(playerId);
                if (player == null || player.getOwnedIsland() == null) {
                    return false;
                }

                var island = player.getOwnedIsland();

                World world = Bukkit.getWorld(island.getCenterLocation().getWorld().getName());
                if (world != null) {
                    CompletableFuture<Void> clearFuture = new CompletableFuture<>();
                    Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("JExOneblock"), () -> {
                        try {
                            clearIslandArea(world, island);
                            clearFuture.complete(null);
                        } catch (Exception e) {
                            clearFuture.completeExceptionally(e);
                        }
                    });

                    clearFuture.join();
                }

                oneblockIslandRepository.delete(island.getId());

                playerIslandCache.remove(playerId);

                LOGGER.info("Deleted premium island " + island.getIdentifier() + " for player " + playerId);
                return true;
            } catch (Exception e) {
                LOGGER.severe("Failed to delete premium island for player " + playerId + ": " + e.getMessage());
                return false;
            }
        });
    }

    @Override
    public @NotNull CompletableFuture<Boolean> teleportToIsland(@NotNull Player player) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var oneblockPlayer = oneblockPlayerRepository.findByUuid(player.getUniqueId());
                if (oneblockPlayer == null || oneblockPlayer.getOwnedIsland() == null) {
                    return false;
                }

                var island = oneblockPlayer.getOwnedIsland();
                Location spawnLocation = island.getCenterLocation().clone().add(0.5, 1, 0.5);

                ensureSafeSpawn(spawnLocation);

                Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("JExOneblock"), 
                    () -> player.teleport(spawnLocation));
                
                return true;
            } catch (Exception e) {
                LOGGER.severe("Failed to teleport player " + player.getName() + " to premium island: " + e.getMessage());
                return false;
            }
        });
    }

    @Override
    public @NotNull CompletableFuture<Integer> getBlockLevel(@NotNull UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var player = oneblockPlayerRepository.findByUuid(playerId);
                if (player == null || player.getOwnedIsland() == null) {
                    return 0;
                }
                
                var island = player.getOwnedIsland();
                return island.getLevel();
            } catch (Exception e) {
                LOGGER.severe("Failed to get block level for premium player " + playerId + ": " + e.getMessage());
                return 0;
            }
        });
    }

    @Override
    public @Nullable Long getPlayerIslandId(@NotNull Player player) {
        String cachedId = playerIslandCache.get(player.getUniqueId());
        if (cachedId != null) {
            LOGGER.info("Found island in local cache for " + player.getName() + ": " + cachedId);
            return Long.parseLong(cachedId.replace("premium_island_", ""));
        }

        LOGGER.info("Querying database for island owned by " + player.getName() + " (" + player.getUniqueId() + ")");
        var island = oneblockIslandRepository.findByOwner(player.getUniqueId());

        if (island == null) {
            LOGGER.info("Not found in repository cache, trying identifier lookup...");
            island = oneblockIslandRepository.findByOwnerViaIdentifier(player.getUniqueId(), "premium_island_");
        }
        
        if (island != null) {
            LOGGER.info("Found island: " + island.getIdentifier() + " for player " + player.getName());
            playerIslandCache.put(player.getUniqueId(), island.getIdentifier());
            return Long.parseLong(island.getIdentifier().replace("premium_island_", ""));
        }

        LOGGER.info("No island found for player " + player.getName());
        return null;
    }

    @Override
    public @Nullable String getPlayerIslandIdentifier(@NotNull Player player) {
        String cachedId = playerIslandCache.get(player.getUniqueId());
        if (cachedId != null) {
            return cachedId;
        }

        var island = oneblockIslandRepository.findByOwner(player.getUniqueId());

        if (island == null) {
            island = oneblockIslandRepository.findByOwnerViaIdentifier(player.getUniqueId(), "premium_island_");
        }
        
        if (island != null) {
            playerIslandCache.put(player.getUniqueId(), island.getIdentifier());
            return island.getIdentifier();
        }

        return null;
    }

    @Override
    public @Nullable Long getIslandIdAtLocation(@NotNull Location location) {
        if (!worldManagementService.isOneBlockWorld(location.getWorld())) {
            return null;
        }

        var islands = oneblockIslandRepository.findAll();
        for (var island : islands) {
            if (island.getCenterLocation().getWorld().getName().equals(location.getWorld().getName())) {
                double distance = island.getCenterLocation().distance(location);
                if (distance <= island.getCurrentSize() / 2.0) {
                    return Long.parseLong(island.getIdentifier().replace("premium_island_", ""));
                }
            }
        }
        
        return null;
    }

    @Override
    public boolean isOneBlockLocation(@NotNull Location location) {
        var islandId = getIslandIdAtLocation(location);
        if (islandId == null) {
            return false;
        }

        var island = oneblockIslandRepository.findByIdentifier("premium_island_" + islandId);
        if (island == null) {
            return false;
        }

        Location oneblockLoc = island.getOneblock().getOneblockLocation();
        return location.getBlockX() == oneblockLoc.getBlockX() && 
               location.getBlockZ() == oneblockLoc.getBlockZ() &&
               location.getBlockY() == oneblockLoc.getBlockY();
    }

    private void loadNextIslandId() {
        var islands = oneblockIslandRepository.findAll();
        long maxId = 0;
        for (var island : islands) {
            try {
                long id = Long.parseLong(island.getIdentifier().replace("premium_island_", ""));
                if (id > maxId) {
                    maxId = id;
                }
            } catch (NumberFormatException e) {
            }
        }
        nextIslandId = maxId + 1;
    }

    private void generatePremiumOneblock(World world, Location center) {
        int platformSize = 8;
        int y = center.getBlockY();

        for (int x = -platformSize; x <= platformSize; x++) {
            for (int z = -platformSize; z <= platformSize; z++) {
                Location blockLoc = center.clone().add(x, -1, z);
                if (Math.abs(x) <= 1 && Math.abs(z) <= 1) {
                    world.getBlockAt(blockLoc).setType(Material.DIAMOND_BLOCK);
                } else {
                    world.getBlockAt(blockLoc).setType(Material.EMERALD_BLOCK);
                }
            }
        }

        world.getBlockAt(center).setType(Material.DIAMOND_BLOCK);

        world.getBlockAt(center.clone().add(2, 0, 0)).setType(Material.ENDER_CHEST);
        world.getBlockAt(center.clone().add(-2, 0, 0)).setType(Material.ENCHANTING_TABLE);
        world.getBlockAt(center.clone().add(0, 0, 2)).setType(Material.ANVIL);
        world.getBlockAt(center.clone().add(0, 0, -2)).setType(Material.BREWING_STAND);
    }

    private void clearIslandArea(World world, OneblockIsland island) {
        Location center = island.getCenterLocation();
        int clearRadius = island.getCurrentSize() / 2;

        for (int x = -clearRadius; x <= clearRadius; x++) {
            for (int z = -clearRadius; z <= clearRadius; z++) {
                for (int y = 0; y < 256; y++) {
                    Location blockLoc = center.clone().add(x, y - center.getBlockY(), z);
                    world.getBlockAt(blockLoc).setType(Material.AIR);
                }
            }
        }
    }

    private void ensureSafeSpawn(Location location) {
        World world = location.getWorld();
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();

        world.getBlockAt(x, y - 1, z).setType(Material.DIAMOND_BLOCK);
        world.getBlockAt(x, y, z).setType(Material.AIR);
        world.getBlockAt(x, y + 1, z).setType(Material.AIR);
    }
    
    // Island Information Methods
    @Override
    public void showIslandInfo(@NotNull Player player) {
        var oneblockPlayer = oneblockPlayerRepository.findByUuid(player.getUniqueId());
        if (oneblockPlayer == null || oneblockPlayer.getOwnedIsland() == null) {
            player.sendMessage("§cYou don't have an island!");
            return;
        }
        
        var island = oneblockPlayer.getOwnedIsland();
        var memberCount = island.getIslandMembers().size();
        var bannedCount = island.getBannedPlayers().size();
        
        player.sendMessage("§6========== Premium Island Info ==========");
        player.sendMessage("§eName: §f" + island.getIslandName());
        player.sendMessage("§eOwner: §f" + island.getOwner().getPlayerName());
        player.sendMessage("§eLevel: §f" + island.getLevel());
        player.sendMessage("§eExperience: §f" + String.format("%.2f", island.getExperience()));
        player.sendMessage("§eSize: §f" + island.getCurrentSize() + "/" + island.getMaximumSize());
        player.sendMessage("§eCoins: §f" + island.getIslandCoins());
        player.sendMessage("§eMembers: §f" + memberCount);
        player.sendMessage("§eBanned: §f" + bannedCount);
        player.sendMessage("§ePrivacy: §f" + (island.isPrivacy() ? "Private" : "Public"));
        player.sendMessage("§eCreated: §f" + island.getCreatedAt().toLocalDate());
        player.sendMessage("§eType: §6Premium");
        player.sendMessage("§6=========================================");
    }
    
    @Override
    public void showIslandLevel(@NotNull Player player) {
        var oneblockPlayer = oneblockPlayerRepository.findByUuid(player.getUniqueId());
        if (oneblockPlayer == null || oneblockPlayer.getOwnedIsland() == null) {
            player.sendMessage("§cYou don't have an island!");
            return;
        }
        
        var island = oneblockPlayer.getOwnedIsland();
        var currentLevel = island.getLevel();
        var experience = island.getExperience();
        var nextLevelXp = calculateNextLevelXp(currentLevel);
        var progress = (experience / nextLevelXp) * 100;
        
        player.sendMessage("§6========== Island Level ==========");
        player.sendMessage("§eCurrent Level: §f" + currentLevel);
        player.sendMessage("§eExperience: §f" + String.format("%.2f", experience) + " / " + String.format("%.2f", nextLevelXp));
        player.sendMessage("§eProgress: §f" + String.format("%.1f", progress) + "%");
        player.sendMessage("§6==================================");
    }
    
    @Override
    public void showTopIslands(@NotNull Player player) {
        var allIslands = oneblockIslandRepository.findAll();
        var topIslands = allIslands.stream()
            .sorted((a, b) -> Integer.compare(b.getLevel(), a.getLevel()))
            .limit(10)
            .toList();
        
        player.sendMessage("§6========== Top Islands ==========");
        int rank = 1;
        for (var island : topIslands) {
            String medal = rank <= 3 ? (rank == 1 ? "§6🥇" : rank == 2 ? "§7🥈" : "§c🥉") : "§e" + rank + ".";
            player.sendMessage(medal + " §f" + island.getIslandName() + " §7- Level " + island.getLevel() + " §8(Owner: " + island.getOwner().getPlayerName() + ")");
            rank++;
        }
        player.sendMessage("§6==================================");
    }
    
    // Island Management Methods
    @Override
    public void handlePrestige(@NotNull Player player) {
        var oneblockPlayer = oneblockPlayerRepository.findByUuid(player.getUniqueId());
        if (oneblockPlayer == null || oneblockPlayer.getOwnedIsland() == null) {
            player.sendMessage("§cYou don't have an island!");
            return;
        }
        
        var island = oneblockPlayer.getOwnedIsland();
        if (island.getLevel() < 100) {
            player.sendMessage("§cYou need to reach level 100 to prestige!");
            return;
        }
        
        player.sendMessage("§ePrestige system coming soon! You'll be able to reset your island for permanent bonuses.");
    }
    
    @Override
    public void setIslandHome(@NotNull Player player) {
        var oneblockPlayer = oneblockPlayerRepository.findByUuid(player.getUniqueId());
        if (oneblockPlayer == null || oneblockPlayer.getOwnedIsland() == null) {
            player.sendMessage("§cYou don't have an island!");
            return;
        }
        
        var island = oneblockPlayer.getOwnedIsland();
        
        // Check if player is on their island
        if (!island.getCenterLocation().getWorld().equals(player.getWorld())) {
            player.sendMessage("§cYou must be on your island to set the home location!");
            return;
        }
        
        island.setCenterLocation(player.getLocation());
        oneblockIslandRepository.update(island);
        player.sendMessage("§aIsland home set to your current location!");
    }
    
    @Override
    public void deleteIsland(@NotNull Player player) {
        var oneblockPlayer = oneblockPlayerRepository.findByUuid(player.getUniqueId());
        if (oneblockPlayer == null || oneblockPlayer.getOwnedIsland() == null) {
            player.sendMessage("§cYou don't have an island!");
            return;
        }
        
        player.sendMessage("§cAre you sure you want to delete your island? This cannot be undone!");
        player.sendMessage("§cType §e/island delete confirm §cto proceed.");
        // TODO: Add confirmation system
    }

    // Member Management Methods
    @Override
    public void handleInvite(@NotNull Player player, @NotNull String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /island invite <player>");
            return;
        }
        
        var oneblockPlayer = oneblockPlayerRepository.findByUuid(player.getUniqueId());
        if (oneblockPlayer == null || oneblockPlayer.getOwnedIsland() == null) {
            player.sendMessage("§cYou don't have an island!");
            return;
        }
        
        var island = oneblockPlayer.getOwnedIsland();
        if (!island.isOwner(oneblockPlayer)) {
            player.sendMessage("§cOnly the island owner can invite players!");
            return;
        }
        
        var targetPlayer = org.bukkit.Bukkit.getPlayer(args[1]);
        if (targetPlayer == null) {
            player.sendMessage("§cPlayer not found!");
            return;
        }
        
        player.sendMessage("§eInvite system coming soon! You'll be able to invite " + targetPlayer.getName() + " to your island.");
    }
    
    @Override
    public void handleAcceptInvite(@NotNull Player player, @NotNull String[] args) {
        player.sendMessage("§eYou don't have any pending invites.");
    }
    
    @Override
    public void handleDenyInvite(@NotNull Player player, @NotNull String[] args) {
        player.sendMessage("§eYou don't have any pending invites.");
    }
    
    @Override
    public void handleKick(@NotNull Player player, @NotNull String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /island kick <player>");
            return;
        }
        
        var oneblockPlayer = oneblockPlayerRepository.findByUuid(player.getUniqueId());
        if (oneblockPlayer == null || oneblockPlayer.getOwnedIsland() == null) {
            player.sendMessage("§cYou don't have an island!");
            return;
        }
        
        player.sendMessage("§eMember management coming soon!");
    }
    
    @Override
    public void handleBan(@NotNull Player player, @NotNull String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /island ban <player> [reason]");
            return;
        }
        
        var oneblockPlayer = oneblockPlayerRepository.findByUuid(player.getUniqueId());
        if (oneblockPlayer == null || oneblockPlayer.getOwnedIsland() == null) {
            player.sendMessage("§cYou don't have an island!");
            return;
        }
        
        player.sendMessage("§eBan system coming soon!");
    }
    
    @Override
    public void handleUnban(@NotNull Player player, @NotNull String[] args) {
        if (args.length < 2) {
            player.sendMessage("§cUsage: /island unban <player>");
            return;
        }
        
        var oneblockPlayer = oneblockPlayerRepository.findByUuid(player.getUniqueId());
        if (oneblockPlayer == null || oneblockPlayer.getOwnedIsland() == null) {
            player.sendMessage("§cYou don't have an island!");
            return;
        }
        
        player.sendMessage("§eBan system coming soon!");
    }
    
    @Override
    public void handleLeave(@NotNull Player player) {
        var oneblockPlayer = oneblockPlayerRepository.findByUuid(player.getUniqueId());
        if (oneblockPlayer == null) {
            player.sendMessage("§cYou are not a member of any island!");
            return;
        }
        
        if (oneblockPlayer.getOwnedIsland() != null) {
            player.sendMessage("§cYou cannot leave your own island! Use /island delete instead.");
            return;
        }
        
        player.sendMessage("§eMember management coming soon!");
    }
    
    // Storage Methods
    @Override
    public void showStorageInfo(@NotNull Player player) {
        var oneblockPlayer = oneblockPlayerRepository.findByUuid(player.getUniqueId());
        if (oneblockPlayer == null || oneblockPlayer.getOwnedIsland() == null) {
            player.sendMessage("§cYou don't have an island!");
            return;
        }
        
        var island = oneblockPlayer.getOwnedIsland();
        var plugin = Bukkit.getPluginManager().getPlugin("JExOneblock");
        
        if (plugin instanceof JExOneblock jexPlugin) {
            var storageManager = jexPlugin.getIslandStorageManager();
            if (storageManager != null) {
                var storage = storageManager.getStorage(island.getId());
                if (storage != null) {
                    var totalItems = storage.getStoredItems().values().stream().mapToLong(Long::longValue).sum();
                    var uniqueTypes = storage.getStoredItems().size();
                    
                    // Calculate total capacity across all rarities
                    var totalCapacity = storage.getRarityCapacities().values().stream().mapToLong(Long::longValue).sum();
                    
                    player.sendMessage("§6========== Storage Info ==========");
                    player.sendMessage("§eTier: §f" + storage.getCurrentTier().getDisplayName());
                    player.sendMessage("§eCapacity: §f" + totalItems + " / " + totalCapacity);
                    player.sendMessage("§eTotal Items: §f" + totalItems);
                    player.sendMessage("§eUnique Types: §f" + uniqueTypes);
                    player.sendMessage("§ePassive Bonuses:");
                    player.sendMessage("  §7XP: §a+" + (int)((storage.getCurrentTier().getPassiveXpMultiplier() - 1) * 100) + "%");
                    player.sendMessage("  §7Drops: §b+" + (int)((storage.getCurrentTier().getPassiveDropMultiplier() - 1) * 100) + "%");
                    player.sendMessage("§6==================================");
                    return;
                }
            }
        }
        
        player.sendMessage("§6========== Storage Info ==========");
        player.sendMessage("§eStorage system is initializing...");
        player.sendMessage("§6==================================");
    }
    
    private double calculateNextLevelXp(int level) {
        return 100 + (level * 50); // Simple formula: 100 + (level * 50)
    }
}
