package com.raindropcentral.rdq.listener;

import com.raindropcentral.rcore.database.entity.RPlayer;
import com.raindropcentral.rcore.database.entity.statistic.RPlayerStatistic;
import com.raindropcentral.rcore.service.RPlayerStatisticService;
import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.RDQPlayer;
import com.raindropcentral.rplatform.enumeration.EStatisticType;
import com.raindropcentral.rplatform.logger.CentralLogger;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Listener for handling player join events.
 * <p>
 * This listener manages player data persistence and statistics when a player joins the server.
 * It checks if the player is new or returning, updates or creates their database record,
 * and initializes comprehensive statistics for new players using {@link RPlayerStatisticService}
 * and the {@link EStatisticType} enum system.
 * </p>
 *
 * <ul>
 *     <li>If the player is new, creates a new {@link RPlayer} and {@link RPlayerStatistic} with initial statistics.</li>
 *     <li>If the player is returning, updates their name if it has changed and updates last seen timestamp.</li>
 *     <li>Uses the comprehensive {@link EStatisticType} enum for type-safe statistic management.</li>
 *     <li>Initializes perk tracking statistics for comprehensive perk usage monitoring.</li>
 * </ul>
 *
 * @author JExcellence
 */
public class OnJoin implements Listener {

    /**
     * The core plugin instance used for accessing repositories and logging.
     */
    private final RDQ rdq;

    /**
     * Constructs a new {@code OnJoin} listener with the specified core plugin instance.
     *
     * @param rdq the core plugin instance
     */
    public OnJoin(
            final @NotNull RDQ rdq
    ) {
        this.rdq = rdq;
    }

    /**
     * Handles the asynchronous player pre-login event.
     * <p>
     * Checks if the player exists in the database by unique ID. If not, creates a new player record
     * and initializes their statistics using {@link RPlayerStatisticService} and {@link EStatisticType}.
     * If the player exists, updates their name if it has changed and updates their last seen timestamp.
     * </p>
     *
     * @param event the asynchronous player pre-login event
     */
    @EventHandler
    public void onPlayerLogin(final AsyncPlayerPreLoginEvent event) {

        this.rdq.getImpl().getPlayerRepository()
                .findByAttributesAsync(Map.of("uniqueId", event.getUniqueId()))
                .thenComposeAsync(rdqPlayer -> {
                    if (rdqPlayer == null) {
                        RDQPlayer player = new RDQPlayer(event.getUniqueId(), event.getName());
                        return this.rdq.getImpl().getPlayerRepository().createAsync(player);
                    } else {
                        return handleExistingPlayer(rdqPlayer, event);
                    }
                }, this.rdq.getImpl().getExecutor())
                .thenAcceptAsync(rdqPlayer -> {
                    CentralLogger.getLogger(OnJoin.class.getName()).info(
                            "Successfully processed join for player: " + rdqPlayer.getPlayerName() +
                                    " (" + rdqPlayer.getUniqueId() + ")"
                    );
                }, this.rdq.getImpl().getExecutor())
                .exceptionally(ex -> {
                    CentralLogger.getLogger(OnJoin.class.getName()).severe(
                            "Error handling player join for " + event.getName() + " (" + event.getUniqueId() + "): " + ex.getMessage()
                    );
                    event.disallow(
                            AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                            Component.text("Player data could not be processed. Please try again.")
                    );
                    return null;
                });
    }

    /**
     * Handles updates for existing players.
     *
     * @param rdqPlayer the existing player
     * @param event   the login event
     * @return a CompletableFuture with the updated player
     */
    private CompletableFuture<RDQPlayer> handleExistingPlayer(
            final @NotNull RDQPlayer rdqPlayer,
            final @NotNull AsyncPlayerPreLoginEvent event
    ) {
        boolean needsUpdate = false;

        if (!rdqPlayer.getPlayerName().equals(event.getName())) {
            rdqPlayer.setPlayerName(event.getName());
            needsUpdate = true;
        }

        if (needsUpdate) {
            return this.rdq.getImpl().getPlayerRepository().updateAsync(rdqPlayer).thenApply(updated -> rdqPlayer);
        }

        return CompletableFuture.completedFuture(rdqPlayer);
    }
}