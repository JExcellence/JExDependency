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

public final class PlayerPreLogin implements Listener {

    private static final Logger LOGGER = CentralLogger.getLogger(PlayerPreLogin.class);

    private final RDQ rdq;
    private final String pluginNamespace;

    public PlayerPreLogin(@NotNull RDQ rdq) {
        this.rdq = rdq;
        this.pluginNamespace = Objects.requireNonNullElse(rdq.getEdition(), "RDQ");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAsyncPreLogin(@NotNull AsyncPlayerPreLoginEvent event) {
        if (rdq.isDisabling() || !rdq.isEnabled()) {
            event.disallow(
                    AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                    Component.text("Server is restarting, please try again shortly.")
            );
            return;
        }

        var uniqueId = event.getUniqueId();
        var playerName = event.getName();

        rdq.getPlayerRepository()
                .findByAttributesAsync(Map.of("uniqueId", uniqueId))
                .thenComposeAsync(existing -> existing == null 
                    ? createNewPlayerFlow(uniqueId, playerName)
                    : handleExistingPlayer(existing, playerName), rdq.getExecutor())
                .thenAcceptAsync(rdqPlayer -> 
                    LOGGER.info("Processed pre-login for " + rdqPlayer.getPlayerName() + " (" + rdqPlayer.getUniqueId() + ")"), 
                    rdq.getExecutor())
                .exceptionally(ex -> {
                    LOGGER.log(Level.SEVERE, "Error handling pre-login for " + playerName + " (" + uniqueId + "): " + ex.getMessage(), ex);
                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, Component.text("Player data could not be processed. Please try again."));
                    return null;
                })
                .join();
    }

    private CompletableFuture<RDQPlayer> createNewPlayerFlow(@NotNull UUID uniqueId, @NotNull String playerName) {
        try {
            var rdqPlayer = new RDQPlayer(uniqueId, playerName);
            return rdq.getPlayerRepository().createAsync(rdqPlayer)
                    .thenComposeAsync(this::initializePlayerStatistics, rdq.getExecutor())
                    .thenApply(created -> {
                        LOGGER.log(Level.INFO, "Created new player " + created.getPlayerName() + " (" + created.getUniqueId() + ")");
                        return created;
                    });
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private CompletableFuture<RDQPlayer> handleExistingPlayer(@NotNull RDQPlayer player, @NotNull String incomingName) {
        if (!player.getPlayerName().equals(incomingName)) {
            player.setPlayerName(incomingName);
            return rdq.getPlayerRepository().updateAsync(player).thenApply(updated -> player);
        }
        return CompletableFuture.completedFuture(player);
    }

    private CompletableFuture<RDQPlayer> initializePlayerStatistics(@NotNull RDQPlayer created) {
        try {
            //TODO STATISTICS
            return CompletableFuture.completedFuture(created);
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "Statistic initialization skipped for " + created.getUniqueId() + ": " + t.getMessage());
            return CompletableFuture.completedFuture(created);
        }
    }
}