package de.jexcellence.home.factory;

import com.raindropcentral.rplatform.logging.CentralLogger;
import de.jexcellence.home.config.HomeSystemConfig;
import de.jexcellence.home.database.entity.Home;
import de.jexcellence.home.exception.HomeLimitReachedException;
import de.jexcellence.home.exception.HomeNotFoundException;
import de.jexcellence.home.exception.WorldNotLoadedException;
import de.jexcellence.home.service.IHomeService;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Factory class for home business logic following the BountyFactory pattern from RDQ.
 * <p>
 * Provides centralized business logic for home operations including:
 * - Permission and limit validation
 * - Caching for performance
 * - Teleport warmup handling
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 */
public class HomeFactory {

    private static final Logger LOGGER = CentralLogger.getLoggerByName("HomeFactory");

    private static HomeFactory instance;

    private final IHomeService homeService;
    private final HomeSystemConfig config;
    private final JavaPlugin plugin;
    private final Map<UUID, List<Home>> homeCache = new ConcurrentHashMap<>();
    private final Map<UUID, TeleportWarmupTask> activeTeleports = new ConcurrentHashMap<>();

    private HomeFactory(@NotNull IHomeService homeService, @NotNull HomeSystemConfig config, @NotNull JavaPlugin plugin) {
        this.homeService = homeService;
        this.config = config;
        this.plugin = plugin;
    }

    /**
     * Initializes the HomeFactory singleton.
     *
     * @param homeService the home service implementation
     * @param config      the home system configuration
     * @param plugin      the plugin instance
     * @return the initialized factory instance
     */
    public static HomeFactory initialize(@NotNull IHomeService homeService, @NotNull HomeSystemConfig config, @NotNull JavaPlugin plugin) {
        if (instance == null) {
            instance = new HomeFactory(homeService, config, plugin);
            LOGGER.info("HomeFactory initialized");
        }
        return instance;
    }

    /**
     * Gets the initialized instance.
     *
     * @return the factory instance
     * @throws IllegalStateException if not initialized
     */
    public static HomeFactory getInstance() {
        if (instance == null) {
            throw new IllegalStateException("HomeFactory not initialized. Call initialize() first.");
        }
        return instance;
    }

    /**
     * Resets the singleton instance (for testing).
     */
    public static void reset() {
        instance = null;
    }

    // ==================== Core Operations ====================

    /**
     * Creates a new home for a player with validation.
     *
     * @param player the player creating the home
     * @param name   the name of the home
     * @return a CompletableFuture containing the created home
     */
    public @NotNull CompletableFuture<Home> createHome(@NotNull Player player, @NotNull String name) {
        return homeService.getHomeCount(player.getUniqueId())
            .thenCompose(count -> {
                var maxHomes = homeService.getMaxHomesForPlayer(player);
                
                return homeService.findHome(player.getUniqueId(), name)
                    .thenCompose(existing -> {
                        if (existing.isEmpty() && maxHomes > 0 && count >= maxHomes) {
                            return CompletableFuture.failedFuture(
                                new HomeLimitReachedException(count.intValue(), maxHomes)
                            );
                        }
                        return homeService.createHome(player.getUniqueId(), name, player.getLocation());
                    });
            })
            .thenApply(home -> {
                invalidateCache(player.getUniqueId());
                LOGGER.fine("Home '" + name + "' created for player " + player.getName());
                return home;
            });
    }

    /**
     * Deletes a home with cache invalidation.
     *
     * @param player the player deleting the home
     * @param name   the name of the home
     * @return a CompletableFuture containing true if deleted
     */
    public @NotNull CompletableFuture<Boolean> deleteHome(@NotNull Player player, @NotNull String name) {
        return homeService.deleteHome(player.getUniqueId(), name)
            .thenApply(deleted -> {
                if (deleted) {
                    invalidateCache(player.getUniqueId());
                    LOGGER.fine("Home '" + name + "' deleted for player " + player.getName());
                }
                return deleted;
            });
    }

    /**
     * Gets all homes for a player with caching.
     *
     * @param playerId the UUID of the player
     * @return a CompletableFuture containing the list of homes
     */
    public @NotNull CompletableFuture<List<Home>> getPlayerHomes(@NotNull UUID playerId) {
        var cached = homeCache.get(playerId);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }

        return homeService.getPlayerHomes(playerId)
            .thenApply(homes -> {
                homeCache.put(playerId, homes);
                return homes;
            });
    }

    /**
     * Finds a home by name.
     *
     * @param playerId the UUID of the player
     * @param name     the name of the home
     * @return a CompletableFuture containing the home
     */
    public @NotNull CompletableFuture<Home> findHome(@NotNull UUID playerId, @NotNull String name) {
        return homeService.findHome(playerId, name)
            .thenApply(optional -> optional.orElseThrow(() -> new HomeNotFoundException(name)));
    }

    // ==================== Teleportation ====================

    /**
     * Teleports a player to a home with optional warmup.
     *
     * @param player   the player to teleport
     * @param homeName the name of the home
     * @param onComplete callback when teleport completes
     */
    public void teleportToHome(@NotNull Player player, @NotNull String homeName, @Nullable Runnable onComplete) {
        homeService.findHome(player.getUniqueId(), homeName)
            .thenAccept(optionalHome -> {
                if (optionalHome.isEmpty()) {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        new de.jexcellence.jextranslate.i18n.I18n.Builder("home.does_not_exist", player)
                            .includePrefix()
                            .withPlaceholder("home_name", homeName)
                            .build()
                            .sendMessage();
                    });
                    return;
                }

                var home = optionalHome.get();
                var location = home.toLocation();

                if (location == null) {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        new de.jexcellence.jextranslate.i18n.I18n.Builder("home.world_not_loaded", player)
                            .includePrefix()
                            .withPlaceholder("world", home.getWorldName())
                            .build()
                            .sendMessage();
                    });
                    return;
                }

                var delay = config.getTeleportDelay();
                if (delay <= 0) {
                    executeTeleport(player, home, onComplete);
                } else {
                    startWarmupTeleport(player, home, delay, onComplete);
                }
            })
            .exceptionally(throwable -> {
                LOGGER.log(Level.WARNING, "Failed to teleport player " + player.getName(), throwable);
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    new de.jexcellence.jextranslate.i18n.I18n.Builder("home.error.internal", player)
                        .includePrefix()
                        .build()
                        .sendMessage();
                });
                return null;
            });
    }

    /**
     * Teleports a player to a home with warmup delay.
     *
     * @param player     the player to teleport
     * @param home       the home to teleport to
     * @param delay      the warmup delay in seconds
     * @param onComplete callback when teleport completes
     */
    public void teleportWithWarmup(@NotNull Player player, @NotNull Home home, int delay, @Nullable Runnable onComplete) {
        if (delay <= 0) {
            executeTeleport(player, home, onComplete);
        } else {
            startWarmupTeleport(player, home, delay, onComplete);
        }
    }

    private void startWarmupTeleport(@NotNull Player player, @NotNull Home home, int delay, @Nullable Runnable onComplete) {
        cancelActiveTeleport(player.getUniqueId());

        var task = new TeleportWarmupTask(plugin, player, home, config, () -> {
            activeTeleports.remove(player.getUniqueId());
            executeTeleport(player, home, onComplete);
        }, () -> activeTeleports.remove(player.getUniqueId()));

        activeTeleports.put(player.getUniqueId(), task);
        task.start(delay);
    }

    private void executeTeleport(@NotNull Player player, @NotNull Home home, @Nullable Runnable onComplete) {
        var location = home.toLocation();
        if (location == null) {
            throw new WorldNotLoadedException(home.getWorldName());
        }

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            player.teleport(location);
            home.recordVisit();
            homeService.updateHome(home);
            
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }

    /**
     * Cancels an active teleport for a player.
     *
     * @param playerId the UUID of the player
     */
    public void cancelActiveTeleport(@NotNull UUID playerId) {
        var task = activeTeleports.remove(playerId);
        if (task != null) {
            task.cancel();
        }
    }

    // ==================== Cache Management ====================

    /**
     * Invalidates the cache for a player.
     *
     * @param playerId the UUID of the player
     */
    public void invalidateCache(@NotNull UUID playerId) {
        homeCache.remove(playerId);
    }

    /**
     * Clears all cached data.
     */
    public void clearCache() {
        homeCache.clear();
    }

    /**
     * Refreshes the cache for a player.
     *
     * @param playerId the UUID of the player
     */
    public void refreshCache(@NotNull UUID playerId) {
        invalidateCache(playerId);
        getPlayerHomes(playerId);
    }

    // ==================== Getters ====================

    public @NotNull IHomeService getHomeService() {
        return homeService;
    }

    public @NotNull HomeSystemConfig getConfig() {
        return config;
    }

    public boolean isPremium() {
        return homeService.isPremium();
    }
}
