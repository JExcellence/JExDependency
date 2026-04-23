package de.jexcellence.core.listener;

import de.jexcellence.core.JExCore;
import de.jexcellence.core.api.CorePlayerSnapshot;
import de.jexcellence.core.api.event.PlayerTrackedEvent;
import de.jexcellence.core.service.CorePlayerService;
import de.jexcellence.jexplatform.logging.JExLogger;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Keeps {@code jexcore_player} rows current with join and quit events.
 * Join: insert-or-update on async pre-login so the record exists before
 * post-login. Quit: refresh the last-seen timestamp.
 *
 * <p>Auto-registered by {@code CommandFactory#registerAllCommandsAndListeners}
 * via the single-arg {@link JExCore} constructor.
 */
public class PlayerLifecycleListener implements Listener {

    private final CorePlayerService players;
    private final JExLogger logger;

    /**
     * Auto-wiring constructor used by JExCommand's reflection-based
     * listener registration. Accepts the orchestrator and pulls the
     * collaborators from it.
     *
     * @param core the orchestrator
     */
    public PlayerLifecycleListener(@NotNull JExCore core) {
        this(core.playerService(), core.logger());
    }

    /**
     * Direct-wiring constructor for tests and manual registration.
     *
     * @param players core player service
     * @param logger platform logger
     */
    public PlayerLifecycleListener(@NotNull CorePlayerService players, @NotNull JExLogger logger) {
        this.players = players;
        this.logger = logger;
    }

    /**
     * Ensures a player record exists before the server accepts the login.
     *
     * @param event async pre-login event
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onAsyncPreLogin(@NotNull AsyncPlayerPreLoginEvent event) {
        this.players.track(event.getUniqueId(), event.getName())
                .thenAccept(player -> {
                    if (player == null) return;
                    Bukkit.getPluginManager().callEvent(new PlayerTrackedEvent(
                            new CorePlayerSnapshot(
                                    player.getUniqueId(),
                                    player.getPlayerName(),
                                    player.getFirstSeen(),
                                    player.getLastSeen()
                            )
                    ));
                })
                .exceptionally(ex -> {
                    this.logger.error("pre-login track failed for {}: {}", event.getName(), ex.getMessage());
                    return null;
                });
    }

    /**
     * Updates the last-seen column on quit.
     *
     * @param event quit event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(@NotNull PlayerQuitEvent event) {
        this.players.markSeen(event.getPlayer().getUniqueId());
    }
}
