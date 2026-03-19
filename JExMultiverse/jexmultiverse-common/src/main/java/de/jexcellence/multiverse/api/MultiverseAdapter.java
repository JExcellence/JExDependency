package de.jexcellence.multiverse.api;

import com.raindropcentral.rplatform.logging.CentralLogger;
import de.jexcellence.jextranslate.i18n.I18n;
import de.jexcellence.multiverse.database.entity.MVWorld;
import de.jexcellence.multiverse.database.repository.MVWorldRepository;
import de.jexcellence.multiverse.factory.WorldFactory;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of {@link IMultiverseAdapter} providing API access for external plugins.
 * <p>
 * This adapter handles:
 * <ul>
 *   <li>World retrieval from repository with caching</li>
 *   <li>Spawn location resolution with fallback logic</li>
 *   <li>Player teleportation with i18n message support</li>
 * </ul>
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 */
public class MultiverseAdapter implements IMultiverseAdapter {

    private static final Logger LOGGER = CentralLogger.getLoggerByName("MultiverseAdapter");

    private final JavaPlugin plugin;
    private final MVWorldRepository worldRepository;
    private final WorldFactory worldFactory;

    /**
     * Constructs a new MultiverseAdapter.
     *
     * @param plugin          the plugin instance
     * @param worldRepository the world repository for database operations
     * @param worldFactory    the world factory for world management
     */
    public MultiverseAdapter(
            @NotNull JavaPlugin plugin,
            @NotNull MVWorldRepository worldRepository,
            @NotNull WorldFactory worldFactory
    ) {
        this.plugin = plugin;
        this.worldRepository = worldRepository;
        this.worldFactory = worldFactory;
    }

    @Override
    public @NotNull CompletableFuture<Optional<MVWorld>> getGlobalMVWorld() {
        return worldRepository.findByGlobalSpawnAsync();
    }

    @Override
    public @NotNull CompletableFuture<Optional<MVWorld>> getMVWorld(@NotNull String worldName) {
        // First check the cache for faster access
        Optional<MVWorld> cached = worldFactory.getCachedWorld(worldName);
        if (cached.isPresent()) {
            return CompletableFuture.completedFuture(cached);
        }

        // Fall back to repository lookup
        return worldRepository.findByIdentifierAsync(worldName);
    }

    @Override
    public @NotNull CompletableFuture<Boolean> hasMultiverseSpawn(@NotNull String worldName) {
        // Check if global spawn exists OR if the specific world is managed
        return getGlobalMVWorld()
                .thenCompose(globalWorld -> {
                    if (globalWorld.isPresent()) {
                        return CompletableFuture.completedFuture(true);
                    }
                    return getMVWorld(worldName)
                            .thenApply(Optional::isPresent);
                });
    }

    @Override
    public @NotNull CompletableFuture<Boolean> spawn(@NotNull Player player, @NotNull String messageKey) {
        return resolveSpawnLocation(player)
                .thenCompose(location -> teleportPlayer(player, location, messageKey));
    }

    /**
     * Resolves the appropriate spawn location for a player.
     * <p>
     * Priority:
     * <ol>
     *   <li>Global spawn world (if configured)</li>
     *   <li>Current world's spawn (if managed by JExMultiverse)</li>
     *   <li>Default world spawn (fallback)</li>
     * </ol>
     * </p>
     *
     * @param player the player to resolve spawn for
     * @return a CompletableFuture containing the spawn location
     */
    private @NotNull CompletableFuture<Location> resolveSpawnLocation(@NotNull Player player) {
        return getGlobalMVWorld()
                .thenCompose(globalWorldOpt -> {
                    // Priority 1: Global spawn
                    if (globalWorldOpt.isPresent()) {
                        MVWorld globalWorld = globalWorldOpt.get();
                        Location spawnLocation = globalWorld.getSpawnLocation();

                        // Ensure the world is loaded
                        if (spawnLocation.getWorld() != null) {
                            return CompletableFuture.completedFuture(spawnLocation);
                        }

                        // Try to get the Bukkit world
                        World bukkitWorld = Bukkit.getWorld(globalWorld.getIdentifier());
                        if (bukkitWorld != null) {
                            Location resolvedLocation = new Location(
                                    bukkitWorld,
                                    spawnLocation.getX(),
                                    spawnLocation.getY(),
                                    spawnLocation.getZ(),
                                    spawnLocation.getYaw(),
                                    spawnLocation.getPitch()
                            );
                            return CompletableFuture.completedFuture(resolvedLocation);
                        }

                        LOGGER.warning("Global spawn world '" + globalWorld.getIdentifier() + "' is not loaded");
                    }

                    // Priority 2: Current world's spawn
                    String currentWorldName = player.getWorld().getName();
                    return getMVWorld(currentWorldName)
                            .thenApply(worldOpt -> {
                                if (worldOpt.isPresent()) {
                                    MVWorld mvWorld = worldOpt.get();
                                    Location spawnLocation = mvWorld.getSpawnLocation();

                                    // Ensure the world reference is set
                                    if (spawnLocation.getWorld() == null) {
                                        World bukkitWorld = Bukkit.getWorld(mvWorld.getIdentifier());
                                        if (bukkitWorld != null) {
                                            return new Location(
                                                    bukkitWorld,
                                                    spawnLocation.getX(),
                                                    spawnLocation.getY(),
                                                    spawnLocation.getZ(),
                                                    spawnLocation.getYaw(),
                                                    spawnLocation.getPitch()
                                            );
                                        }
                                    }
                                    return spawnLocation;
                                }

                                // Priority 3: Default world spawn (fallback)
                                return getDefaultSpawnLocation();
                            });
                });
    }

    /**
     * Gets the default spawn location (main world spawn).
     *
     * @return the default spawn location
     */
    private @NotNull Location getDefaultSpawnLocation() {
        World defaultWorld = Bukkit.getWorlds().get(0);
        return defaultWorld.getSpawnLocation();
    }

    /**
     * Teleports a player to the specified location on the main thread.
     *
     * @param player     the player to teleport
     * @param location   the destination location
     * @param messageKey the i18n message key to send on success
     * @return a CompletableFuture containing true if teleportation was successful
     */
    private @NotNull CompletableFuture<Boolean> teleportPlayer(
            @NotNull Player player,
            @NotNull Location location,
            @NotNull String messageKey
    ) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();

        // Teleportation must happen on the main thread
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                // Validate location
                if (location.getWorld() == null) {
                    LOGGER.warning("Cannot teleport player '" + player.getName() + "': location world is null");
                    future.complete(false);
                    return;
                }

                // Ensure safe landing
                ensureSafeLanding(location);

                // Perform teleportation
                boolean success = player.teleport(location);

                if (success && messageKey != null && !messageKey.isEmpty()) {
                    sendTeleportMessage(player, messageKey, location);
                }

                future.complete(success);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error teleporting player '" + player.getName() + "'", e);
                future.complete(false);
            }
        });

        return future;
    }

    /**
     * Sends a teleport success message to the player.
     *
     * @param player     the player to send the message to
     * @param messageKey the i18n message key
     * @param location   the destination location (for placeholders)
     */
    private void sendTeleportMessage(
            @NotNull Player player,
            @NotNull String messageKey,
            @NotNull Location location
    ) {
        try {
            new I18n.Builder(messageKey, player)
                    .includePrefix()
                    .withPlaceholder("world", location.getWorld() != null ? location.getWorld().getName() : "Unknown")
                    .withPlaceholder("x", String.format("%.0f", location.getX()))
                    .withPlaceholder("y", String.format("%.0f", location.getY()))
                    .withPlaceholder("z", String.format("%.0f", location.getZ()))
                    .build()
                    .sendMessage();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to send teleport message to player '" + player.getName() + "'", e);
        }
    }

    /**
     * Ensures safe landing by placing a glass block under the teleport location if the block below is air.
     * <p>
     * This prevents players from falling into the void when teleporting to locations
     * where there is no solid ground beneath them.
     * </p>
     *
     * @param location the teleport destination location
     */
    private void ensureSafeLanding(@NotNull Location location) {
        if (location.getWorld() == null) {
            return;
        }

        Block blockBelow = location.clone().subtract(0, 1, 0).getBlock();
        if (blockBelow.getType().isAir()) {
            blockBelow.setType(Material.GLASS);
            LOGGER.fine("Placed safety glass block at " + blockBelow.getLocation());
        }
    }
}
