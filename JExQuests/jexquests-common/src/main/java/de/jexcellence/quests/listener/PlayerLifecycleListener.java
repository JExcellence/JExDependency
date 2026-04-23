package de.jexcellence.quests.listener;

import de.jexcellence.jexplatform.logging.JExLogger;
import de.jexcellence.quests.JExQuests;
import de.jexcellence.quests.service.QuestsPlayerService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Ensures every logging-in player has a {@code QuestsPlayer} row. Runs
 * at {@code LOWEST} priority on the async pre-login event so the record
 * exists well before any other JExQuests service touches the UUID.
 *
 * <p>Auto-registered by {@code CommandFactory#registerAllCommandsAndListeners}
 * via the single-arg {@link JExQuests} constructor — the factory
 * injects the orchestrator; we pull services off it.
 */
public class PlayerLifecycleListener implements Listener {

    private final QuestsPlayerService players;
    private final JExLogger logger;

    public PlayerLifecycleListener(@NotNull JExQuests quests) {
        this(quests.questsPlayerService(), quests.logger());
    }

    public PlayerLifecycleListener(@NotNull QuestsPlayerService players, @NotNull JExLogger logger) {
        this.players = players;
        this.logger = logger;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAsyncPreLogin(@NotNull AsyncPlayerPreLoginEvent event) {
        this.players.trackAsync(event.getUniqueId()).exceptionally(ex -> {
            this.logger.error("JExQuests track failed for {}: {}", event.getName(), ex.getMessage());
            return null;
        });
    }
}
