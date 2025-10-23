package com.raindropcentral.rdq.listener;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rplatform.logging.CentralLogger;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Listener that orchestrates asynchronous player preparation during AsyncPlayerPreLoginEvent.
 *
 * Event workflow:
 *  1) Resolve RDQPlayer via uniqueId; branch for creation vs. update.
 *  2) Create a new profile and initialize baseline statistics or update name/timestamps/counters for existing.
 *  3) Ensure defaults across gameplay/system/progression/perk namespaces.
 *  4) Log result; on fatal repository error, disallow with friendly message.
 *
 * Threading:
 *  - Invoked by Bukkit on an async thread.
 *  - Repository/processing chained on RDQ executor to keep work off the login thread.
 *  - Final join() keeps login thread blocked until decision is made.
 */
public final class PlayerPreLogin implements Listener {

    private static final Logger LOGGER = CentralLogger.getLogger(PlayerPreLogin.class);

    private final RDQ rdq;
    private final String pluginNamespace;

    public PlayerPreLogin(final @NotNull RDQ rdq) {
        this.rdq = Objects.requireNonNull(rdq, "rdq cannot be null");
        this.pluginNamespace = Objects.requireNonNullElse(rdq.getEdition(), "RDQ");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAsyncPreLogin(final @NotNull AsyncPlayerPreLoginEvent event) {
        if (rdq.isDisabling() || !rdq.isEnabled()) {
            event.disallow(
                    AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                    Component.text("Server is restarting, please try again shortly.")
            );
            return;
        }

        final UUID uniqueId = event.getUniqueId();
        final String playerName = event.getName();

        rdq.getPlayerRepository()
                .findByAttributesAsync(Map.of("uniqueId", uniqueId))
                .thenComposeAsync(existing -> {
                    if (existing == null) {
                        return createNewPlayerFlow(uniqueId, playerName);
                    } else {
                        return handleExistingPlayer(existing, playerName);
                    }
                }, rdq.getExecutor())
                .thenAcceptAsync(rdqPlayer -> {
                    LOGGER.info("Processed pre-login for " + rdqPlayer.getPlayerName()
                            + " (" + rdqPlayer.getUniqueId() + ")");
                }, rdq.getExecutor())
                .exceptionally(ex -> {
                    LOGGER.log(Level.SEVERE,
                            "Error handling pre-login for " + playerName + " (" + uniqueId + "): " + ex.getMessage(), ex);
                    event.disallow(
                            AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                            Component.text("Player data could not be processed. Please try again.")
                    );
                    return null;
                })
                .join();
    }

    /**
     * Creates a new RDQ player and initializes baseline statistics.
     */
    private CompletableFuture<RDQPlayer> createNewPlayerFlow(final @NotNull UUID uniqueId, final @NotNull String playerName) {
        try {
            final RDQPlayer rdqPlayer = new RDQPlayer(uniqueId, playerName);

            return rdq.getPlayerRepository().createAsync(rdqPlayer)
                    .thenComposeAsync(created -> initializePlayerStatistics(created), rdq.getExecutor())
                    .thenApply(created -> {
                        LOGGER.log(Level.INFO, "Created new player " + created.getPlayerName() + " (" + created.getUniqueId() + ")");
                        return created;
                    });
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Updates existing RDQ player (e.g., name change) and refreshes statistics.
     */
    private CompletableFuture<RDQPlayer> handleExistingPlayer(final @NotNull RDQPlayer player, final @NotNull String incomingName) {
        boolean needsUpdate = false;

        if (!player.getPlayerName().equals(incomingName)) {
            player.setPlayerName(incomingName);
            needsUpdate = true;
        }

        return needsUpdate
                ? rdq.getPlayerRepository().updateAsync(player).thenApply(updated -> player)
                : CompletableFuture.completedFuture(player);
    }

    /**
     * Initializes statistics for a newly created player.
     * Uses RPlayerStatisticService and EStatisticType defaults, then persists via repository.
     */
    private CompletableFuture<RDQPlayer> initializePlayerStatistics(final @NotNull RDQPlayer created) {
        try {
            //TODO STATISTICS
            return CompletableFuture.completedFuture(created);
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "Statistic initialization skipped for " + created.getUniqueId() + ": " + t.getMessage());
            return CompletableFuture.completedFuture(created);
        }
    }
}