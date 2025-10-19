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
 * Listener that orchestrates asynchronous player preparation during {@link AsyncPlayerPreLoginEvent}.
 *
 * <p>Event workflow:</p>
 * <ol>
 *     <li>Resolve the {@link RPlayer} via {@code uniqueId}; branch for creation versus update.</li>
 *     <li>Create a new profile with baseline statistics or refresh an existing profile's name, timestamps,
 *     and counters.</li>
 *     <li>Ensure category defaults are present for gameplay, system, progression, and perk statistics.</li>
 *     <li>Log the outcome and disallow the login with a friendly message on fatal failures.</li>
 * </ol>
 *
 * <p>Threading notes:</p>
 * <ul>
 *     <li>The listener is invoked on the asynchronous pre-login thread provided by Bukkit.</li>
 *     <li>Repository calls and follow-up processing are chained on the plugin's executor to keep blocking work off the
 *     login thread.</li>
 *     <li>Error handlers still run asynchronously before the join, ensuring login decisions are made prior to the main
 *     thread resuming the connection.</li>
 * </ul>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public final class PlayerPreLogin implements Listener {

    /**
     * Central logger used to capture lifecycle details and failures while handling asynchronous login preparation.
     */
    private static final Logger LOGGER = CentralLogger.getLogger(PlayerPreLogin.class);

    /**
     * Owning plugin instance that provides repositories, executors, and overall player bootstrap configuration for the
     * asynchronous pipeline.
     */
    private final RCoreFree rCore;

    /**
     * Namespace identifier recorded in statistics to attribute server-specific metrics during login handling and
     * default statistic population.
     */
    private final String pluginNamespace;

    /**
     * Creates a new listener bound to the supplied plugin implementation.
     *
     * @param rCore core plugin implementation providing repositories and executors
     */
    public PlayerPreLogin(final @NotNull RCoreFree rCore) {
        this.rCore = Objects.requireNonNull(rCore, "rCore cannot be null");
        this.pluginNamespace = this.rCore.getName();
    }

    /**
     * Handles the asynchronous pre-login event by delegating persistence operations to the plugin executor, creating or
     * updating player records, and applying statistic migrations.
     *
     * <p>The future pipeline continues on the plugin executor while the final {@link CompletableFuture#join()} keeps the
     * Bukkit asynchronous login thread blocked until a decision is made. Any repository failure is surfaced through the
     * completion stages and results in a friendly kick message being applied to the event.</p>
     *
     * @param event asynchronous pre-login event supplied by Bukkit
     */
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

    /**
     * Builds a brand new {@link RPlayer} profile and persists it asynchronously on the plugin executor.
     *
     * <p>Statistic defaults include gameplay, system, progression, and perk categories to guarantee every downstream
     * module sees a fully populated statistic aggregate. Exceptions during player instantiation or repository calls are
     * wrapped into the returned {@link CompletableFuture} to be handled by the caller.</p>
     *
     * @param uniqueId   unique identifier of the incoming player
     * @param playerName player name at login time
     * @return future completing with the created {@link RPlayer} on the plugin executor
     */
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

    /**
     * Applies name, statistic, and migration updates for an existing {@link RPlayer}, persisting changes on the plugin
     * executor when required.
     *
     * <p>Validation failures (for example, invalid name updates) are logged and skipped while still allowing the login
     * to proceed. The returned future executes on the plugin executor so downstream consumers remain on the asynchronous
     * thread.</p>
     *
     * @param player       loaded player entity from storage
     * @param incomingName player name currently provided by Bukkit
     * @return future completing on the plugin executor with the persisted {@link RPlayer}
     */
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
     * Generates the comprehensive baseline statistic payload for a new player while executing on the asynchronous
     * repository workflow.
     *
     * <p>Baseline values include timestamp anchors, login counters, and server namespace metadata that downstream
     * modules rely on. The method is intentionally synchronous yet invoked from asynchronous stages to keep the
     * pipeline consistent.</p>
     *
     * @param player player instance to initialize statistics for
     * @return initialized statistic aggregate ready for persistence
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

    /**
     * Populates gameplay-related defaults, executed within the plugin's asynchronous pipeline prior to persistence.
     *
     * @param playerStatistic statistic aggregate being enriched
     */
    private void addGameplayStatistics(final @NotNull RPlayerStatistic playerStatistic) {
        final Map<String, Object> gameplayStats = EStatisticType.getGameplayDefaults();
        RPlayerStatisticService.addStatisticsBulk(playerStatistic, gameplayStats, "Gameplay");
    }

    /**
     * Adds system category defaults for the current server namespace as part of the asynchronous initialization flow.
     *
     * @param playerStatistic statistic aggregate being enriched
     */
    private void addSystemStatistics(final @NotNull RPlayerStatistic playerStatistic) {
        final Map<String, Object> systemStats =
                EStatisticType.getDefaultValuesForCategory(EStatisticType.StatisticCategory.SYSTEM);
        RPlayerStatisticService.addStatisticsBulk(playerStatistic, systemStats, this.pluginNamespace);
    }

    /**
     * Adds progression defaults for tracking advancement metrics while still on the asynchronous executor pipeline.
     *
     * @param playerStatistic statistic aggregate being enriched
     */
    private void addProgressionStatistics(final @NotNull RPlayerStatistic playerStatistic) {
        final Map<String, Object> progressionStats =
                EStatisticType.getDefaultValuesForCategory(EStatisticType.StatisticCategory.PROGRESSION);
        RPlayerStatisticService.addStatisticsBulk(playerStatistic, progressionStats, "Progression");
    }

    /**
     * Updates timestamps, login counters, and performs migration checks for returning players inside the asynchronous
     * processing chain.
     *
     * <p>The caller executes the method on the plugin executor, keeping statistic mutations off the Bukkit main thread
     * while guaranteeing that core, progression, and perk defaults remain synchronized.</p>
     *
     * @param playerStatistic statistic aggregate to refresh
     */
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

    /**
     * Ensures every core statistic entry is present for the player while executing on the plugin executor.
     *
     * <p>Missing entries are populated with the default values defined by {@link EStatisticType#getCoreDefaults()},
     * preventing {@code null} statistics from leaking to downstream services. Each update occurs through the
     * {@link RPlayerStatisticService} to maintain consistent persistence semantics.</p>
     *
     * @param playerStatistic statistic aggregate to validate
     */
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

    /**
     * Ensures the RDQ perk statistic namespace is populated to maintain compatibility with dependent modules.
     *
     * <p>Perk statistics live under the {@link EStatisticType.StatisticCategory#RDQ} namespace to align with RDQ module
     * expectations. Missing entries are inserted asynchronously so follow-up processing remains on the executor thread.</p>
     *
     * @param playerStatistic statistic aggregate to validate
     */
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