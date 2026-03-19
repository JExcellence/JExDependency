package de.jexcellence.multiverse.listener;

import com.raindropcentral.rplatform.logging.CentralLogger;
import de.jexcellence.multiverse.JExMultiverse;
import de.jexcellence.multiverse.database.entity.MVWorld;
import de.jexcellence.multiverse.database.repository.MVWorldRepository;
import de.jexcellence.multiverse.factory.WorldFactory;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Listener for handling player spawn and respawn events.
 * <p>
 * This listener handles:
 * <ul>
 *   <li>{@link PlayerSpawnLocationEvent} - Sets initial spawn location when player joins</li>
 *   <li>{@link PlayerRespawnEvent} - Sets respawn location after death</li>
 * </ul>
 * </p>
 * <p>
 * Spawn location priority:
 * <ol>
 *   <li>Global spawn world (if configured)</li>
 *   <li>Current world's spawn (if managed by JExMultiverse)</li>
 *   <li>Default world spawn (fallback)</li>
 * </ol>
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 */
public class SpawnListener implements Listener {

    private static final Logger LOGGER = CentralLogger.getLoggerByName("SpawnListener");

    private final JExMultiverse multiverse;
    private final MVWorldRepository worldRepository;
    private final WorldFactory worldFactory;

    /**
     * Constructs a new SpawnListener.
     *
     * @param multiverse          the plugin instance
     */
    public SpawnListener(
            @NotNull JExMultiverse multiverse
    ) {
        this.multiverse = multiverse;
        this.worldRepository = multiverse.getWorldRepository();
        this.worldFactory = multiverse.getWorldFactory();
    }

    /**
     * Handles the PlayerSpawnLocationEvent to set initial spawn location.
     * <p>
     * This event fires when a player joins the server and determines
     * where they will spawn. Uses HIGHEST priority to override other plugins.
     * </p>
     *
     * @param event the spawn location event
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerSpawnLocation(@NotNull PlayerSpawnLocationEvent event) {
        Player player = event.getPlayer();
        
        try {
            Location spawnLocation = resolveSpawnLocation(player, event.getSpawnLocation());
            if (spawnLocation != null) {
                event.setSpawnLocation(spawnLocation);
                LOGGER.fine("Set spawn location for player '" + player.getName() + "' to " + formatLocation(spawnLocation));
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error handling spawn location for player '" + player.getName() + "'", e);
        }
    }

    /**
     * Handles the PlayerRespawnEvent to set respawn location after death.
     * <p>
     * This event fires when a player respawns after dying. Uses HIGHEST
     * priority to override other plugins.
     * </p>
     *
     * @param event the respawn event
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(@NotNull PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        
        // Skip if player is respawning at bed or anchor
        if (event.isBedSpawn() || event.isAnchorSpawn()) {
            LOGGER.fine("Player '" + player.getName() + "' respawning at bed/anchor, skipping multiverse spawn");
            return;
        }
        
        try {
            Location spawnLocation = resolveSpawnLocation(player, event.getRespawnLocation());
            if (spawnLocation != null) {
                event.setRespawnLocation(spawnLocation);
                LOGGER.fine("Set respawn location for player '" + player.getName() + "' to " + formatLocation(spawnLocation));
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error handling respawn location for player '" + player.getName() + "'", e);
        }
    }

    /**
     * Resolves the appropriate spawn location for a player.
     * <p>
     * Priority:
     * <ol>
     *   <li>Global spawn world (if configured)</li>
     *   <li>Current world's spawn (if managed by JExMultiverse)</li>
     *   <li>Fallback location (original location)</li>
     * </ol>
     * </p>
     *
     * @param player           the player to resolve spawn for
     * @param fallbackLocation the fallback location if no spawn is configured
     * @return the resolved spawn location, or null to use default
     */
    private Location resolveSpawnLocation(@NotNull Player player, @NotNull Location fallbackLocation) {
        // Priority 1: Check for global spawn
        Optional<MVWorld> globalSpawnWorld = worldRepository.findByGlobalSpawn();
        if (globalSpawnWorld.isPresent()) {
            Location globalSpawn = resolveWorldSpawn(globalSpawnWorld.get());
            if (globalSpawn != null) {
                return globalSpawn;
            }
        }

        // Priority 2: Check current world's spawn
        String currentWorldName = fallbackLocation.getWorld() != null 
                ? fallbackLocation.getWorld().getName() 
                : player.getWorld().getName();
        
        Optional<MVWorld> currentWorld = worldFactory.getCachedWorld(currentWorldName);
        if (currentWorld.isEmpty()) {
            // Try repository if not in cache
            currentWorld = worldRepository.findByIdentifier(currentWorldName);
        }
        
        if (currentWorld.isPresent()) {
            Location worldSpawn = resolveWorldSpawn(currentWorld.get());
            if (worldSpawn != null) {
                return worldSpawn;
            }
        }

        // Priority 3: Return null to use fallback/default
        return null;
    }

    /**
     * Resolves the spawn location for an MVWorld entity.
     * <p>
     * Ensures the world reference is properly set on the location.
     * </p>
     *
     * @param mvWorld the MVWorld entity
     * @return the resolved location with world reference, or null if world not loaded
     */
    private Location resolveWorldSpawn(@NotNull MVWorld mvWorld) {
        Location spawnLocation = mvWorld.getSpawnLocation();
        
        // Check if world reference is already set
        if (spawnLocation.getWorld() != null) {
            return spawnLocation;
        }
        
        // Try to get the Bukkit world
        World bukkitWorld = Bukkit.getWorld(mvWorld.getIdentifier());
        if (bukkitWorld == null) {
            LOGGER.warning("World '" + mvWorld.getIdentifier() + "' is not loaded, cannot use spawn location");
            return null;
        }
        
        // Create new location with world reference
        return new Location(
                bukkitWorld,
                spawnLocation.getX(),
                spawnLocation.getY(),
                spawnLocation.getZ(),
                spawnLocation.getYaw(),
                spawnLocation.getPitch()
        );
    }

    /**
     * Formats a location for logging purposes.
     *
     * @param location the location to format
     * @return formatted location string
     */
    private @NotNull String formatLocation(@NotNull Location location) {
        String worldName = location.getWorld() != null ? location.getWorld().getName() : "Unknown";
        return String.format("%s (%.0f, %.0f, %.0f)", 
                worldName, 
                location.getX(), 
                location.getY(), 
                location.getZ());
    }

    /**
     * Registers this listener with the plugin.
     *
     * @param plugin the plugin to register with
     */
    public void register(@NotNull JavaPlugin plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        LOGGER.info("SpawnListener registered");
    }
}
