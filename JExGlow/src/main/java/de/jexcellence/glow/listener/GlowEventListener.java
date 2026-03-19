package de.jexcellence.glow.listener;

import com.raindropcentral.rplatform.logging.CentralLogger;
import de.jexcellence.glow.JExGlow;
import de.jexcellence.glow.factory.GlowFactory;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Event listener for reapplying glow effects on player join and respawn.
 * <p>
 * Ensures that glow effects persist across player deaths and server restarts
 * by querying the database and reapplying effects when needed.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 */
public class GlowEventListener implements Listener {

    private static final Logger LOGGER = CentralLogger.getLoggerByName("GlowEventListener");
    private final JExGlow plugin;

    /**
     * Constructs a new GlowEventListener.
     *
     * @param plugin the plugin instance
     */
    public GlowEventListener(@NotNull JExGlow plugin) {
        this.plugin = plugin;
    }

    /**
     * Handles player join events to reapply glow effects.
     * <p>
     * Queries the database asynchronously and schedules glow application
     * on the main thread with a 300ms delay to ensure the player entity
     * is fully loaded.
     * </p>
     *
     * @param event the player join event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(@NotNull PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        try {
            var glowService = GlowFactory.getService();
            
            glowService.isGlowEnabled(player.getUniqueId())
                .thenAccept(isEnabled -> {
                    if (isEnabled) {
                        // Schedule glow application with delay to ensure player is fully loaded
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            if (player.isOnline() && player.isValid()) {
                                glowService.applyGlowEffect(player)
                                    .exceptionally(throwable -> {
                                        LOGGER.log(Level.WARNING, 
                                            "Failed to apply glow effect on join for " + player.getName(), 
                                            throwable);
                                        return null;
                                    });
                            }
                        }, 15L); // 300ms delay (15 ticks)
                    }
                })
                .exceptionally(throwable -> {
                    LOGGER.log(Level.WARNING, 
                        "Failed to check glow status on join for " + player.getName(), 
                        throwable);
                    return null;
                });
        } catch (IllegalStateException e) {
            LOGGER.log(Level.SEVERE, "GlowFactory not initialized", e);
        }
    }

    /**
     * Handles player respawn events to reapply glow effects.
     * <p>
     * Queries the database asynchronously and schedules glow application
     * on the main thread with a 100ms delay to ensure the player entity
     * is fully loaded after respawn.
     * </p>
     *
     * @param event the player respawn event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerRespawn(@NotNull PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        
        try {
            var glowService = GlowFactory.getService();
            
            glowService.isGlowEnabled(player.getUniqueId())
                .thenAccept(isEnabled -> {
                    if (isEnabled) {
                        // Schedule glow application with delay to ensure player is fully loaded
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            if (player.isOnline() && player.isValid()) {
                                glowService.applyGlowEffect(player)
                                    .exceptionally(throwable -> {
                                        LOGGER.log(Level.WARNING, 
                                            "Failed to apply glow effect on respawn for " + player.getName(), 
                                            throwable);
                                        return null;
                                    });
                            }
                        }, 5L); // 100ms delay (5 ticks)
                    }
                })
                .exceptionally(throwable -> {
                    LOGGER.log(Level.WARNING, 
                        "Failed to check glow status on respawn for " + player.getName(), 
                        throwable);
                    return null;
                });
        } catch (IllegalStateException e) {
            LOGGER.log(Level.SEVERE, "GlowFactory not initialized", e);
        }
    }
}
