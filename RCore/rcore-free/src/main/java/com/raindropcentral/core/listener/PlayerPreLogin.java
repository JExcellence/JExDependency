package com.raindropcentral.core.listener;

import com.raindropcentral.core.RCoreFree;
import com.raindropcentral.core.database.entity.player.RPlayer;
import com.raindropcentral.core.database.entity.statistic.RPlayerStatistic;
import com.raindropcentral.core.service.RPlayerStatisticService;
import com.raindropcentral.rplatform.logging.CentralLogger;
import com.raindropcentral.rplatform.type.EStatisticType;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Listener for handling player async pre-login.
 *
 * Responsibilities:
 * - Load or create RPlayer by uniqueId
 * - Initialize RPlayerStatistic for new players with comprehensive defaults
 * - Update returning players: name changes, last seen, login count
 * - Ensure newly introduced statistics exist for legacy players
 *
 * Threading:
 * - Executes repository and service operations on the plugin's executor
 * - Async flow guarded; disallows login on fatal failure with a user-friendly message
 */
public final class PlayerPreLogin implements Listener {

    private static final Logger LOGGER = CentralLogger.getLogger(PlayerPreLogin.class);

    private final RCoreFree rCore;
    private final String pluginNamespace;

    public PlayerPreLogin(final @NotNull RCoreFree rCore) {
        this.rCore = Objects.requireNonNull(rCore, "rCore cannot be null");
        this.pluginNamespace = this.rCore.getName();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAsyncPreLogin(final @NotNull AsyncPlayerPreLoginEvent event) {
        final UUID uniqueId = event.getUniqueId();
        final String playerName = event.getName();

        this.rCore.getImpl().getRPlayerRepository()
                .findByAttributesAsync(Map.of("uniqueId", uniqueId))
                .thenComposeAsync(existing -> {
                    if (existing == null) {
                        return createNewPlayerFlow(uniqueId, playerName);
                    } else {
                        return handleExistingPlayer(existing, playerName);
                    }
                }, this.rCore.getImpl().getExecutor())
                .thenAcceptAsync(player -> {
                    final int statCount = player.getPlayerStatistic() != null
                            ? player.getPlayerStatistic().getStatistics().size()
                            : 0;
                    LOGGER.info("Processed pre-login for " + player.getPlayerName()
                            + " (" + player.getUniqueId() + ") with " + statCount + " statistics");
                }, this.rCore.getImpl().getExecutor())
                .exceptionally(ex -> {
                    LOGGER.log(Level.SEVERE, "Error handling pre-login for " + playerName
                            + " (" + uniqueId + "): " + ex.getMessage(), ex);
                    event.disallow(
                            AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                            Component.text("Player data could not be processed. Please try again.")
                    );
                    return null;
                })
                .join();
    }

    private CompletableFuture<RPlayer> createNewPlayerFlow(final @NotNull UUID uniqueId, final @NotNull String playerName) {
        try {
            final RPlayer player = new RPlayer(uniqueId, playerName);

            final RPlayerStatistic statistic = createInitialPlayerStatistics(player);
            player.setPlayerStatistic(statistic);

            return this.rCore.getImpl().getRPlayerRepository().createAsync(player)
                    .thenApply(created -> {
                        final int count = created.getPlayerStatistic() != null
                                ? created.getPlayerStatistic().getStatistics().size() : 0;
                        LOGGER.log(Level.INFO, "Created new player " + created.getPlayerName()
                                + " with " + count + " initial statistics");
                        return created;
                    });
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    private CompletableFuture<RPlayer> handleExistingPlayer(final @NotNull RPlayer player, final @NotNull String incomingName) {
        boolean needsUpdate = false;

        if (!player.getPlayerName().equals(incomingName)) {
            try {
                player.updatePlayerName(incomingName);
                needsUpdate = true;
            } catch (final IllegalArgumentException iae) {
                LOGGER.log(Level.WARNING, "Rejected invalid name update for "
                        + player.getUniqueId() + ": " + iae.getMessage());
            }
        }

        if (player.getPlayerStatistic() == null) {
            player.setPlayerStatistic(createInitialPlayerStatistics(player));
            needsUpdate = true;
        } else {
            final RPlayerStatistic stats = player.getPlayerStatistic();
            final int currentStatCount = stats.getStatistics().size();
            LOGGER.log(Level.FINE, "Loaded player " + player.getPlayerName() 
                    + " with " + currentStatCount + " existing statistics");

            updateReturningPlayerStatistics(stats);
            needsUpdate = true;
        }

        if (needsUpdate) {
            return this.rCore.getImpl().getRPlayerRepository()
                    .updateAsync(player)
                    .thenApply(updated -> {
                        LOGGER.log(Level.FINE, "Updated existing player " + updated.getPlayerName()
                                + " (" + updated.getUniqueId() + ")");
                        return updated;
                    });
        }
        return CompletableFuture.completedFuture(player);
    }

    /**
     * Create a comprehensive baseline of statistics for a new player.
     * Combines core defaults with time/server scoped values, and initializes category defaults.
     */
    private RPlayerStatistic createInitialPlayerStatistics(final @NotNull RPlayer player) {
        final Map<String, Object> coreStats = new HashMap<>(EStatisticType.getCoreDefaults());

        final LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        coreStats.put(EStatisticType.JOIN_DATE.getKey(), now);
        coreStats.put(EStatisticType.LAST_SEEN.getKey(), now);
        coreStats.put(EStatisticType.FIRST_JOIN_SERVER.getKey(), this.pluginNamespace);
        coreStats.put(EStatisticType.LOGIN_COUNT.getKey(), 1.0);

        final RPlayerStatistic playerStatistic =
                RPlayerStatisticService.createPlayerStatisticWithData(player, coreStats, this.pluginNamespace);

        addGameplayStatistics(playerStatistic);
        addSystemStatistics(playerStatistic);
        addProgressionStatistics(playerStatistic);

        return playerStatistic;
    }

    private void addGameplayStatistics(final @NotNull RPlayerStatistic playerStatistic) {
        final Map<String, Object> gameplayStats = EStatisticType.getGameplayDefaults();
        RPlayerStatisticService.addStatisticsBulk(playerStatistic, gameplayStats, "Gameplay");
    }

    private void addSystemStatistics(final @NotNull RPlayerStatistic playerStatistic) {
        final Map<String, Object> systemStats =
                EStatisticType.getDefaultValuesForCategory(EStatisticType.StatisticCategory.SYSTEM);
        RPlayerStatisticService.addStatisticsBulk(playerStatistic, systemStats, this.pluginNamespace);
    }

    private void addProgressionStatistics(final @NotNull RPlayerStatistic playerStatistic) {
        final Map<String, Object> progressionStats =
                EStatisticType.getDefaultValuesForCategory(EStatisticType.StatisticCategory.PROGRESSION);
        RPlayerStatisticService.addStatisticsBulk(playerStatistic, progressionStats, "Progression");
    }

    private void updateReturningPlayerStatistics(final @NotNull RPlayerStatistic playerStatistic) {

        RPlayerStatisticService.addOrUpdateStatistic(
                playerStatistic,
                EStatisticType.LAST_SEEN.getKey(),
                this.pluginNamespace,
                LocalDateTime.now(ZoneOffset.UTC)
        );

        RPlayerStatisticService.incrementNumericStatistic(
                playerStatistic,
                EStatisticType.LOGIN_COUNT.getKey(),
                this.pluginNamespace,
                1.0
        );

        ensureAllCoreStatisticsExist(playerStatistic);
        ensurePerkStatisticsExist(playerStatistic);
    }

    private void ensureAllCoreStatisticsExist(final @NotNull RPlayerStatistic playerStatistic) {
        final Map<String, Object> coreDefaults = EStatisticType.getCoreDefaults();
        for (Map.Entry<String, Object> entry : coreDefaults.entrySet()) {
            final String key = entry.getKey();
            final Object defaultValue = entry.getValue();

            if (!playerStatistic.hasStatisticByIdentifier(key)) {
                RPlayerStatisticService.addOrUpdateStatistic(
                        playerStatistic,
                        key,
                        this.pluginNamespace,
                        defaultValue
                );
            }
        }
    }

    private void ensurePerkStatisticsExist(final @NotNull RPlayerStatistic playerStatistic) {
        final Map<String, Object> perkDefaults = EStatisticType.getPerkDefaults();
        for (Map.Entry<String, Object> entry : perkDefaults.entrySet()) {
            final String key = entry.getKey();
            final Object defaultValue = entry.getValue();

            if (!playerStatistic.hasStatistic(key, EStatisticType.StatisticCategory.RDQ.name())) {
                RPlayerStatisticService.addOrUpdateStatistic(
                        playerStatistic,
                        key,
                        EStatisticType.StatisticCategory.RDQ.name(),
                        defaultValue
                );
            }
        }
    }
}