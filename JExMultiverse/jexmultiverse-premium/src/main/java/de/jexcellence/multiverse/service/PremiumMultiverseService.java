package de.jexcellence.multiverse.service;

import com.raindropcentral.rplatform.logging.CentralLogger;
import de.jexcellence.multiverse.database.entity.MVWorld;
import de.jexcellence.multiverse.database.repository.MVWorldRepository;
import de.jexcellence.multiverse.factory.WorldFactory;
import de.jexcellence.multiverse.type.MVWorldType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Premium version of the Multiverse Service with full functionality.
 * <p>
 * Features:
 * <ul>
 *   <li>Unlimited worlds</li>
 *   <li>All world types available (DEFAULT, VOID, PLOT)</li>
 *   <li>Full feature access</li>
 * </ul>
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 */
public class PremiumMultiverseService implements IMultiverseService {

    private static final Logger LOGGER = CentralLogger.getLoggerByName("PremiumMultiverseService");

    private static final int UNLIMITED_WORLDS = -1;
    private static final List<MVWorldType> AVAILABLE_TYPES = Arrays.asList(MVWorldType.values());

    private static PremiumMultiverseService instance;

    private final MVWorldRepository repository;
    private final WorldFactory worldFactory;

    private PremiumMultiverseService(@NotNull MVWorldRepository repository, @NotNull WorldFactory worldFactory) {
        this.repository = repository;
        this.worldFactory = worldFactory;
    }

    /**
     * Initializes the Premium Multiverse Service.
     *
     * @param repository   the world repository
     * @param worldFactory the world factory
     * @return the initialized service instance
     */
    public static PremiumMultiverseService initialize(
            @NotNull MVWorldRepository repository,
            @NotNull WorldFactory worldFactory
    ) {
        if (instance == null) {
            instance = new PremiumMultiverseService(repository, worldFactory);
            LOGGER.info("PremiumMultiverseService initialized");
        }
        return instance;
    }

    /**
     * Gets the initialized instance.
     *
     * @return the service instance
     * @throws IllegalStateException if not initialized
     */
    public static PremiumMultiverseService getInstance() {
        if (instance == null) {
            throw new IllegalStateException("PremiumMultiverseService not initialized. Call initialize() first.");
        }
        return instance;
    }

    /**
     * Resets the singleton instance (for testing/reload).
     */
    public static void reset() {
        instance = null;
    }

    // ==================== Edition Detection ====================

    @Override
    public boolean isPremium() {
        return true;
    }

    // ==================== World Limits ====================

    @Override
    public int getMaxWorlds() {
        return UNLIMITED_WORLDS;
    }

    @Override
    public int getMaxWorldTypes() {
        return AVAILABLE_TYPES.size();
    }

    @Override
    public @NotNull List<MVWorldType> getAvailableWorldTypes() {
        return AVAILABLE_TYPES;
    }

    @Override
    public boolean isWorldTypeAvailable(@NotNull MVWorldType type) {
        return true; // All types available in premium
    }

    // ==================== World CRUD Operations ====================

    @Override
    public @NotNull CompletableFuture<MVWorld> createWorld(
            @NotNull String identifier,
            @NotNull World.Environment environment,
            @NotNull MVWorldType type,
            @NotNull Player creator
    ) {
        // Check if world already exists
        return worldExists(identifier).thenCompose(exists -> {
            if (exists) {
                return CompletableFuture.failedFuture(
                        new IllegalArgumentException("World '" + identifier + "' already exists")
                );
            }

            // Create the Bukkit world
            return worldFactory.createBukkitWorld(identifier, environment, type)
                    .thenCompose(world -> {
                        // Create and save MVWorld entity
                        Location spawnLocation = worldFactory.getDefaultSpawnForType(world, type);

                        MVWorld mvWorld = MVWorld.builder()
                                .identifier(identifier)
                                .type(type)
                                .environment(environment)
                                .spawnLocation(spawnLocation)
                                .globalizedSpawn(false)
                                .pvpEnabled(true)
                                .build();

                        return repository.createAsync(mvWorld)
                                .thenApply(saved -> {
                                    worldFactory.cacheWorld(saved);
                                    LOGGER.info("Created world '" + identifier + "' by " + creator.getName());
                                    return saved;
                                });
                    });
        });
    }

    @Override
    public @NotNull CompletableFuture<Boolean> deleteWorld(@NotNull String identifier, @NotNull Player deleter) {
        return getWorld(identifier).thenCompose(optionalWorld -> {
            if (optionalWorld.isEmpty()) {
                return CompletableFuture.failedFuture(
                        new IllegalArgumentException("World '" + identifier + "' does not exist")
                );
            }

            MVWorld mvWorld = optionalWorld.get();

            // Check if world has players
            World bukkitWorld = Bukkit.getWorld(identifier);
            if (bukkitWorld != null && !bukkitWorld.getPlayers().isEmpty()) {
                return CompletableFuture.failedFuture(
                        new IllegalStateException("Cannot delete world: players are present")
                );
            }

            // Delete world files and database entry
            return worldFactory.deleteWorldFiles(identifier)
                    .thenCompose(deleted -> repository.deleteAsync(mvWorld.getId()))
                    .thenApply(v -> {
                        worldFactory.invalidateCache(identifier);
                        LOGGER.info("Deleted world '" + identifier + "' by " + deleter.getName());
                        return true;
                    });
        });
    }

    @Override
    public @NotNull CompletableFuture<Optional<MVWorld>> getWorld(@NotNull String identifier) {
        // Check cache first
        Optional<MVWorld> cached = worldFactory.getCachedWorld(identifier);
        if (cached.isPresent()) {
            return CompletableFuture.completedFuture(cached);
        }

        return repository.findByIdentifierAsync(identifier);
    }

    @Override
    public @NotNull CompletableFuture<List<MVWorld>> getAllWorlds() {
        return repository.findAllAsync();
    }

    @Override
    public @NotNull CompletableFuture<MVWorld> updateWorld(@NotNull MVWorld world) {
        return repository.updateAsync(world)
                .thenApply(updated -> {
                    worldFactory.cacheWorld(updated);
                    return updated;
                });
    }

    // ==================== Spawn Management ====================

    @Override
    public @NotNull CompletableFuture<Boolean> setSpawn(@NotNull MVWorld world, @NotNull Location location) {
        world.setSpawnLocation(location);
        return updateWorld(world)
                .thenApply(updated -> {
                    LOGGER.info("Set spawn for world '" + world.getIdentifier() + "'");
                    return true;
                })
                .exceptionally(throwable -> {
                    LOGGER.log(Level.WARNING, "Failed to set spawn for world: " + world.getIdentifier(), throwable);
                    return false;
                });
    }

    @Override
    public @NotNull CompletableFuture<Boolean> setGlobalSpawn(@NotNull MVWorld world, boolean global) {
        if (global) {
            // Clear global spawn from other worlds first
            return getGlobalSpawnWorld()
                    .thenCompose(optionalGlobal -> {
                        if (optionalGlobal.isPresent() && !optionalGlobal.get().getIdentifier().equals(world.getIdentifier())) {
                            MVWorld previousGlobal = optionalGlobal.get();
                            previousGlobal.setGlobalizedSpawn(false);
                            return updateWorld(previousGlobal);
                        }
                        return CompletableFuture.completedFuture(null);
                    })
                    .thenCompose(v -> {
                        world.setGlobalizedSpawn(true);
                        return updateWorld(world);
                    })
                    .thenApply(updated -> {
                        LOGGER.info("Set global spawn to world '" + world.getIdentifier() + "'");
                        return true;
                    });
        } else {
            world.setGlobalizedSpawn(false);
            return updateWorld(world)
                    .thenApply(updated -> {
                        LOGGER.info("Removed global spawn from world '" + world.getIdentifier() + "'");
                        return true;
                    });
        }
    }

    @Override
    public @NotNull CompletableFuture<Optional<MVWorld>> getGlobalSpawnWorld() {
        return repository.findByGlobalSpawnAsync();
    }

    @Override
    public @NotNull CompletableFuture<Location> getSpawnLocation(@NotNull Player player) {
        // Priority: Global spawn -> Current world spawn -> Default world spawn
        return getGlobalSpawnWorld()
                .thenCompose(optionalGlobal -> {
                    if (optionalGlobal.isPresent()) {
                        return CompletableFuture.completedFuture(optionalGlobal.get().getSpawnLocation());
                    }

                    // Try current world
                    World currentWorld = player.getWorld();
                    return getWorld(currentWorld.getName())
                            .thenApply(optionalWorld -> {
                                if (optionalWorld.isPresent()) {
                                    return optionalWorld.get().getSpawnLocation();
                                }
                                // Fall back to default world spawn
                                World defaultWorld = Bukkit.getWorlds().get(0);
                                return defaultWorld.getSpawnLocation();
                            });
                });
    }

    // ==================== Validation ====================

    @Override
    public @NotNull CompletableFuture<Boolean> worldExists(@NotNull String identifier) {
        // Check Bukkit first
        if (Bukkit.getWorld(identifier) != null) {
            return CompletableFuture.completedFuture(true);
        }

        // Check database
        return repository.findByIdentifierAsync(identifier)
                .thenApply(Optional::isPresent);
    }

    @Override
    public @NotNull CompletableFuture<Boolean> isAtWorldLimit() {
        // Premium has no world limit
        return CompletableFuture.completedFuture(false);
    }

    @Override
    public @NotNull CompletableFuture<Long> getWorldCount() {
        return repository.findAllAsync()
                .thenApply(worlds -> (long) worlds.size());
    }
}
