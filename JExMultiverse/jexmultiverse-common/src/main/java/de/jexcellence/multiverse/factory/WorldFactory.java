package de.jexcellence.multiverse.factory;

import com.raindropcentral.rplatform.logging.CentralLogger;
import de.jexcellence.multiverse.database.entity.MVWorld;
import de.jexcellence.multiverse.database.repository.MVWorldRepository;
import de.jexcellence.multiverse.generator.plot.PlotChunkGenerator;
import de.jexcellence.multiverse.generator.void_world.VoidChunkGenerator;
import de.jexcellence.multiverse.type.MVWorldType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Factory class for world creation and management following the HomeFactory pattern.
 * <p>
 * Provides centralized logic for:
 * <ul>
 *   <li>World creation with generator selection based on MVWorldType</li>
 *   <li>World loading and unloading</li>
 *   <li>World cache management</li>
 * </ul>
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 */
public class WorldFactory {

    private static final Logger LOGGER = CentralLogger.getLoggerByName("WorldFactory");

    private static WorldFactory instance;

    private final JavaPlugin plugin;
    private final MVWorldRepository worldRepository;
    private final Map<String, MVWorld> worldCache = new ConcurrentHashMap<>();

    // Generator instances (reusable)
    private final VoidChunkGenerator voidGenerator;
    private final PlotChunkGenerator plotGenerator;

    private WorldFactory(@NotNull JavaPlugin plugin, @NotNull MVWorldRepository worldRepository) {
        this.plugin = plugin;
        this.worldRepository = worldRepository;
        this.voidGenerator = new VoidChunkGenerator();
        this.plotGenerator = new PlotChunkGenerator();
    }

    /**
     * Initializes the WorldFactory singleton.
     *
     * @param plugin          the plugin instance
     * @param worldRepository the world repository
     * @return the initialized factory instance
     */
    public static WorldFactory initialize(@NotNull JavaPlugin plugin, @NotNull MVWorldRepository worldRepository) {
        if (instance == null) {
            instance = new WorldFactory(plugin, worldRepository);
            LOGGER.info("WorldFactory initialized");
        }
        return instance;
    }

    /**
     * Gets the initialized instance.
     *
     * @return the factory instance
     * @throws IllegalStateException if not initialized
     */
    public static WorldFactory getInstance() {
        if (instance == null) {
            throw new IllegalStateException("WorldFactory not initialized. Call initialize() first.");
        }
        return instance;
    }

    /**
     * Resets the singleton instance (for testing/reload).
     */
    public static void reset() {
        instance = null;
    }

    // ==================== World Creation ====================

    /**
     * Creates a Bukkit world with the appropriate generator based on world type.
     *
     * @param identifier  the world identifier (name)
     * @param environment the world environment
     * @param type        the world generation type
     * @return a CompletableFuture containing the created World
     */
    public @NotNull CompletableFuture<World> createBukkitWorld(
            @NotNull String identifier,
            @NotNull World.Environment environment,
            @NotNull MVWorldType type
    ) {
        return CompletableFuture.supplyAsync(() -> {
            // Check if world already exists
            World existingWorld = Bukkit.getWorld(identifier);
            if (existingWorld != null) {
                LOGGER.warning("World '" + identifier + "' already exists in Bukkit");
                return existingWorld;
            }

            // Create world on main thread
            CompletableFuture<World> worldFuture = new CompletableFuture<>();

            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    WorldCreator creator = new WorldCreator(identifier);
                    creator.environment(environment);

                    // Select generator based on type
                    ChunkGenerator generator = getGeneratorForType(type);
                    if (generator != null) {
                        creator.generator(generator);
                    }

                    World world = creator.createWorld();
                    if (world != null) {
                        LOGGER.info("Created world '" + identifier + "' with type " + type);
                        worldFuture.complete(world);
                    } else {
                        worldFuture.completeExceptionally(
                                new RuntimeException("Failed to create world: " + identifier)
                        );
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error creating world: " + identifier, e);
                    worldFuture.completeExceptionally(e);
                }
            });

            return worldFuture.join();
        });
    }

    /**
     * Gets the appropriate chunk generator for the given world type.
     *
     * @param type the world type
     * @return the chunk generator, or null for DEFAULT type
     */
    public @Nullable ChunkGenerator getGeneratorForType(@NotNull MVWorldType type) {
        return switch (type) {
            case VOID -> voidGenerator;
            case PLOT -> plotGenerator;
            case DEFAULT -> null; // Use vanilla generation
        };
    }

    /**
     * Gets the default spawn location for a world type.
     *
     * @param world the Bukkit world
     * @param type  the world type
     * @return the spawn location
     */
    public @NotNull Location getDefaultSpawnForType(@NotNull World world, @NotNull MVWorldType type) {
        return switch (type) {
            case VOID -> new Location(world, 0.5, VoidChunkGenerator.SPAWN_Y, 0.5);
            case PLOT -> new Location(world,
                    PlotChunkGenerator.DEFAULT_PLOT_SIZE / 2.0 + 0.5,
                    PlotChunkGenerator.DEFAULT_SPAWN_Y,
                    PlotChunkGenerator.DEFAULT_PLOT_SIZE / 2.0 + 0.5);
            case DEFAULT -> world.getSpawnLocation();
        };
    }

    // ==================== World Loading ====================

    /**
     * Loads all persisted worlds from the database on plugin startup.
     *
     * @return a CompletableFuture that completes when all worlds are loaded
     */
    public @NotNull CompletableFuture<Void> loadAllWorlds() {
        return worldRepository.findAllAsync()
                .thenCompose(worlds -> {
                    LOGGER.info("Loading " + worlds.size() + " worlds from database");

                    List<CompletableFuture<Void>> loadFutures = worlds.stream()
                            .map(this::loadWorld)
                            .toList();

                    return CompletableFuture.allOf(loadFutures.toArray(new CompletableFuture[0]));
                })
                .thenRun(() -> LOGGER.info("All worlds loaded successfully"));
    }

    /**
     * Loads a single world from the database.
     *
     * @param mvWorld the MVWorld entity to load
     * @return a CompletableFuture that completes when the world is loaded
     */
    public @NotNull CompletableFuture<Void> loadWorld(@NotNull MVWorld mvWorld) {
        String identifier = mvWorld.getIdentifier();

        // Check if already loaded
        World existingWorld = Bukkit.getWorld(identifier);
        if (existingWorld != null) {
            worldCache.put(identifier, mvWorld);
            LOGGER.fine("World '" + identifier + "' already loaded");
            return CompletableFuture.completedFuture(null);
        }

        // Check if world folder exists
        File worldFolder = new File(Bukkit.getWorldContainer(), identifier);
        if (!worldFolder.exists()) {
            LOGGER.warning("World folder not found for '" + identifier + "', skipping load");
            return CompletableFuture.completedFuture(null);
        }

        return createBukkitWorld(identifier, mvWorld.getEnvironment(), mvWorld.getType())
                .thenAccept(world -> {
                    worldCache.put(identifier, mvWorld);
                    LOGGER.info("Loaded world '" + identifier + "'");
                })
                .exceptionally(throwable -> {
                    LOGGER.log(Level.WARNING, "Failed to load world: " + identifier, throwable);
                    return null;
                });
    }

    // ==================== World Unloading ====================

    /**
     * Unloads a world from the server.
     *
     * @param identifier the world identifier
     * @param save       whether to save the world before unloading
     * @return a CompletableFuture containing true if unloaded successfully
     */
    public @NotNull CompletableFuture<Boolean> unloadWorld(@NotNull String identifier, boolean save) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        Bukkit.getScheduler().runTask(plugin, () -> {
            World world = Bukkit.getWorld(identifier);
            if (world == null) {
                worldCache.remove(identifier);
                future.complete(true);
                return;
            }

            // Check for players in the world
            if (!world.getPlayers().isEmpty()) {
                LOGGER.warning("Cannot unload world '" + identifier + "': players are present");
                future.complete(false);
                return;
            }

            boolean unloaded = Bukkit.unloadWorld(world, save);
            if (unloaded) {
                worldCache.remove(identifier);
                LOGGER.info("Unloaded world '" + identifier + "'");
            } else {
                LOGGER.warning("Failed to unload world '" + identifier + "'");
            }
            future.complete(unloaded);
        });

        return future;
    }

    /**
     * Unloads a world and deletes its files.
     *
     * @param identifier the world identifier
     * @return a CompletableFuture containing true if deleted successfully
     */
    public @NotNull CompletableFuture<Boolean> deleteWorldFiles(@NotNull String identifier) {
        return unloadWorld(identifier, false)
                .thenApply(unloaded -> {
                    if (!unloaded) {
                        return false;
                    }

                    File worldFolder = new File(Bukkit.getWorldContainer(), identifier);
                    if (worldFolder.exists()) {
                        boolean deleted = deleteDirectory(worldFolder);
                        if (deleted) {
                            LOGGER.info("Deleted world files for '" + identifier + "'");
                        } else {
                            LOGGER.warning("Failed to delete world files for '" + identifier + "'");
                        }
                        return deleted;
                    }
                    return true;
                });
    }

    /**
     * Recursively deletes a directory.
     */
    private boolean deleteDirectory(@NotNull File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    if (!file.delete()) {
                        LOGGER.warning("Failed to delete file: " + file.getAbsolutePath());
                    }
                }
            }
        }
        return directory.delete();
    }

    // ==================== Cache Management ====================

    /**
     * Gets a cached MVWorld by identifier.
     *
     * @param identifier the world identifier
     * @return an Optional containing the cached world
     */
    public @NotNull Optional<MVWorld> getCachedWorld(@NotNull String identifier) {
        return Optional.ofNullable(worldCache.get(identifier));
    }

    /**
     * Adds or updates a world in the cache.
     *
     * @param world the world to cache
     */
    public void cacheWorld(@NotNull MVWorld world) {
        worldCache.put(world.getIdentifier(), world);
    }

    /**
     * Removes a world from the cache.
     *
     * @param identifier the world identifier
     */
    public void invalidateCache(@NotNull String identifier) {
        worldCache.remove(identifier);
    }

    /**
     * Clears all cached worlds.
     */
    public void clearCache() {
        worldCache.clear();
    }

    /**
     * Refreshes the cache from the database.
     *
     * @return a CompletableFuture that completes when the cache is refreshed
     */
    public @NotNull CompletableFuture<Void> refreshCache() {
        return worldRepository.findAllAsync()
                .thenAccept(worlds -> {
                    worldCache.clear();
                    worlds.forEach(world -> worldCache.put(world.getIdentifier(), world));
                    LOGGER.info("World cache refreshed with " + worlds.size() + " worlds");
                });
    }

    /**
     * Gets all cached worlds.
     *
     * @return a list of all cached MVWorld entities
     */
    public @NotNull List<MVWorld> getAllCachedWorlds() {
        return List.copyOf(worldCache.values());
    }

    // ==================== Utility Methods ====================

    /**
     * Checks if a world is loaded in Bukkit.
     *
     * @param identifier the world identifier
     * @return true if the world is loaded
     */
    public boolean isWorldLoaded(@NotNull String identifier) {
        return Bukkit.getWorld(identifier) != null;
    }

    /**
     * Gets the Bukkit World for an identifier.
     *
     * @param identifier the world identifier
     * @return an Optional containing the Bukkit World
     */
    public @NotNull Optional<World> getBukkitWorld(@NotNull String identifier) {
        return Optional.ofNullable(Bukkit.getWorld(identifier));
    }

    /**
     * Gets the plugin instance.
     *
     * @return the plugin
     */
    public @NotNull JavaPlugin getPlugin() {
        return plugin;
    }

    /**
     * Gets the world repository.
     *
     * @return the repository
     */
    public @NotNull MVWorldRepository getWorldRepository() {
        return worldRepository;
    }
}
