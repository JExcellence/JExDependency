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
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PlayerPreLogin implements Listener {

    private final static Logger LOGGER = CentralLogger.getLogger("RDQ");
    private final RDQ rdq;

    public  PlayerPreLogin(@NotNull RDQ rdq) {
        this.rdq = rdq;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAsyncPreLogin(AsyncPlayerPreLoginEvent event) {
        var uniqueId = event.getUniqueId();
        var playerName = event.getName();

        rdq.getPlayerRepository().findByAttributesAsync(Map.of("uniqueId", uniqueId)).thenCompose(
                existing -> (existing == null ? createPlayer(uniqueId, playerName) : handlePlayer(existing, playerName))
        ).exceptionally(throwable -> {
            LOGGER.log(Level.WARNING, "Error handling pre-login for " + playerName + " (" + uniqueId + ")", throwable);
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, Component.text("Player data could not be processed. Please try again or contact the administrator of the server."));
            return null;
        });
    }

    private CompletableFuture<RDQPlayer> handlePlayer(@NotNull RDQPlayer player, @NotNull String playerName) {
        if (player.getPlayerName().equals(playerName)) {
            return CompletableFuture.completedFuture(player);
        }

        player.setPlayerName(playerName);
        return rdq.getPlayerRepository().updateAsync(player);
    }

    private CompletableFuture<RDQPlayer> createPlayer(@NotNull UUID uniqueId, @NotNull String playerName) {
        var player = new RDQPlayer(uniqueId, playerName);

        return rdq.getPlayerRepository().createAsync(player).thenCompose(created -> {
           return addPlayerStatistic(player);
        });
    }

    private CompletableFuture<RDQPlayer> addPlayerStatistic(@NotNull RDQPlayer player) {
        //TODO STATISTIC - RCore
        return CompletableFuture.completedFuture(player);
    }
}
