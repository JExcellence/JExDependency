package de.jexcellence.oneblock.service;

import de.jexcellence.oneblock.adapter.MultiverseAdapter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class WorldManagementService {

    private static final Logger LOGGER = Logger.getLogger(WorldManagementService.class.getName());

    private static final int ISLAND_SPACING = 1000;
    private static final int ISLANDS_PER_ROW = 100;
    private static final int START_X = 0;
    private static final int START_Z = 0;
    private static final int ISLAND_HEIGHT = 100;

    private final ExecutorService executorService;
    private final MultiverseAdapter multiverseAdapter;
    private volatile int nextIslandIndex = 0;

    public WorldManagementService(@NotNull ExecutorService executorService, @Nullable MultiverseAdapter multiverseAdapter) {
        this.executorService = executorService;
        this.multiverseAdapter = multiverseAdapter;
    }

    public @NotNull CompletableFuture<List<World>> getAvailableOneBlockWorlds() {
        return CompletableFuture.supplyAsync(() -> {
            if (multiverseAdapter == null || !multiverseAdapter.isAvailable()) {
                LOGGER.warning("JExMultiverse not available, using default worlds");
                return getDefaultWorlds();
            }

            try {
                return Bukkit.getWorlds().stream()
                    .filter(this::isVoidWorld)
                    .filter(multiverseAdapter::isSuitableForIslands)
                    .collect(Collectors.toList());
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error getting available OneBlock worlds", e);
                return getDefaultWorlds();
            }
        }, executorService);
    }

    private @NotNull List<World> getDefaultWorlds() {
        var defaultWorld = Bukkit.getWorlds().get(0);
        return List.of(defaultWorld);
    }

    private boolean isVoidWorld(@NotNull World world) {
        var generator = world.getGenerator() != null ? 
            world.getGenerator().getClass().getSimpleName() : "default";
        
        return generator.toLowerCase().contains("void") || 
               generator.toLowerCase().contains("empty") ||
               world.getName().toLowerCase().contains("flat");
    }

    private boolean isSuitableForOneBlock(@NotNull World world) {
        return world.getEnvironment() == World.Environment.NORMAL &&
               !world.getName().toLowerCase().contains("nether") &&
               !world.getName().toLowerCase().contains("end");
    }

    public @NotNull CompletableFuture<Location> getNextIslandLocation(@NotNull World world) {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (this) {
                var currentIndex = nextIslandIndex++;
                
                var row = currentIndex / ISLANDS_PER_ROW;
                var col = currentIndex % ISLANDS_PER_ROW;
                
                var x = START_X + (col * ISLAND_SPACING);
                var z = START_Z + (row * ISLAND_SPACING);
                
                var location = new Location(world, x, ISLAND_HEIGHT, z);
                LOGGER.info("Generated island location: " + location + " (index: " + currentIndex + ")");
                
                return location;
            }
        }, executorService);
    }

    public @NotNull CompletableFuture<Location> getSafeIslandLocation(@NotNull World world, @Nullable Location preferredLocation) {
        return CompletableFuture.supplyAsync(() -> {
            if (preferredLocation != null && isLocationSafe(preferredLocation)) {
                return preferredLocation;
            }
            
            return getNextIslandLocation(world).join();
        }, executorService);
    }

    public @NotNull CompletableFuture<Boolean> teleportToWorld(@NotNull Player player, @NotNull World world) {
        if (multiverseAdapter == null || !multiverseAdapter.isAvailable()) {
            return CompletableFuture.supplyAsync(() -> {
                var spawnLocation = world.getSpawnLocation();
                return player.teleport(spawnLocation);
            });
        }

        return multiverseAdapter.teleportToWorld(player, world.getName());
    }

    private boolean isLocationSafe(@NotNull Location location) {
        if (!isOneBlockWorld(location.getWorld())) {
            return false;
        }

        if (location.getBlockY() != ISLAND_HEIGHT) {
            return false;
        }
        
        return true;
    }

    public boolean isOneBlockWorld(@Nullable World world) {
        if (world == null) {
            return false;
        }
        
        return isVoidWorld(world) && isSuitableForOneBlock(world);
    }

    public @NotNull CompletableFuture<WorldValidationResult> validateWorlds() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var availableWorlds = getAvailableOneBlockWorlds().join();

                if (availableWorlds.isEmpty()) {
                    return new WorldValidationResult(false, "No suitable OneBlock worlds found");
                }

                LOGGER.info("Found " + availableWorlds.size() + " suitable OneBlock worlds");
                for (var world : availableWorlds) {
                    LOGGER.info("  - " + world.getName());
                }

                return new WorldValidationResult(true, "OneBlock worlds are available and configured");

            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error validating OneBlock worlds", e);
                return new WorldValidationResult(false, "Validation failed: " + e.getMessage());
            }
        }, executorService);
    }

    public @NotNull CompletableFuture<WorldInitializationResult> initializeWorlds() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                LOGGER.info("Initializing OneBlock worlds...");
                
                var validationResult = validateWorlds().join();
                if (!validationResult.isValid()) {
                    return new WorldInitializationResult(false, validationResult.message());
                }
                
                var availableWorlds = getAvailableOneBlockWorlds().join();
                var worldsInitialized = 0;
                
                for (var world : availableWorlds) {
                    if (ensureWorldSpawnLocation(world)) {
                        worldsInitialized++;
                        LOGGER.info("✓ World '" + world.getName() + "' spawn location verified");
                    } else {
                        LOGGER.warning("✗ Failed to set spawn location for world '" + world.getName() + "'");
                    }
                }
                
                if (worldsInitialized == 0) {
                    return new WorldInitializationResult(false, "No worlds could be properly initialized with spawn locations");
                }
                
                var message = "Successfully initialized " + worldsInitialized + " of " + availableWorlds.size() + " OneBlock worlds";
                LOGGER.info(message);
                return new WorldInitializationResult(true, message);
                
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error during world initialization", e);
                return new WorldInitializationResult(false, "World initialization failed: " + e.getMessage());
            }
        }, executorService);
    }
    
    private boolean ensureWorldSpawnLocation(@NotNull World world) {
        try {
            var currentSpawn = world.getSpawnLocation();
            
            if (currentSpawn == null || !isSpawnLocationSafe(currentSpawn)) {
                LOGGER.info("Setting safe spawn location for world: " + world.getName());
                
                var safeSpawn = new Location(world, 0, ISLAND_HEIGHT, 0);
                world.setSpawnLocation(safeSpawn);
                
                var verifySpawn = world.getSpawnLocation();
                if (verifySpawn != null && verifySpawn.getWorld() != null) {
                    LOGGER.info("Spawn location set to: " + verifySpawn);
                    return true;
                } else {
                    LOGGER.warning("Failed to verify spawn location for world: " + world.getName());
                    return false;
                }
            }
            
            return true;
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error ensuring spawn location for world: " + world.getName(), e);
            return false;
        }
    }
    
    private boolean isSpawnLocationSafe(@NotNull Location location) {
        if (location.getWorld() == null) {
            return false;
        }
        
        var y = location.getBlockY();
        if (y < 50 || y > 200) {
            return false;
        }
        
        return true;
    }

    public record WorldInitializationResult(boolean success, String message) {}
    public record WorldValidationResult(boolean isValid, String message) {}
}