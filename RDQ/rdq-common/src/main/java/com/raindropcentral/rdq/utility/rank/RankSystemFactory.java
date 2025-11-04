package com.raindropcentral.rdq.utility.rank;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.config.ranks.system.RankSystemSection;
import com.raindropcentral.rdq.database.entity.rank.RRank;
import com.raindropcentral.rdq.database.entity.rank.RRankTree;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Coordinates loading, validation, and persistence of rank configuration data for RDQ.
 * <p>
 *     Instances orchestrate asynchronous tasks that hydrate the in-memory {@link RankSystemState} and
 *     prepare all rank entities before the gameplay systems access them.
 * </p>
 *
 * @author JExcellence
 * @since 1.0.0
 * @version 1.0.1
 */
public final class RankSystemFactory {

    private static final Logger LOGGER = CentralLogger.getLogger(RankSystemFactory.class.getName());

    private final @NotNull RDQ rdq;
    private final @NotNull Executor executor;
    private final @NotNull RankConfigurationLoader loader;
    private final @NotNull RankValidationService validator;
    private final @NotNull RankEntityService entityService;

    private volatile boolean initializing;
    private volatile @NotNull RankSystemState state = RankSystemState.empty();

    /**
     * Creates a new factory bound to the supplied RDQ plugin instance.
     *
     * @param rdq the RDQ plugin that provides executors and services required for rank initialization
     */
    public RankSystemFactory(final @NotNull RDQ rdq) {
        this.rdq = Objects.requireNonNull(rdq, "rdq");
        this.executor = rdq.getExecutor();
        this.loader = new RankConfigurationLoader(rdq);
        this.validator = new RankValidationService();
        this.entityService = new RankEntityService(rdq);
    }

    /**
     * Initializes the rank system asynchronously by loading configurations, validating them, and
     * creating the corresponding entities.
     *
     * @return a future that completes when the initialization pipeline finishes, successfully or exceptionally
     */
    public @NotNull CompletableFuture<Void> initializeAsync() {
        if (initializing) {
            return CompletableFuture.completedFuture(null);
        }
        initializing = true;

        LOGGER.info("Rank system initialization started");

        return loader.loadAllAsync(executor)
                .thenComposeAsync(loaded -> {
                    this.state = loaded;
                    return validator.validateConfigurationsAsync(loaded, executor);
                }, executor)
                .thenComposeAsync(v -> entityService.createDefaultRankAsync(state, executor), executor)
                .thenComposeAsync(v -> entityService.createRankTreesAsync(state, executor), executor)
                .thenComposeAsync(v -> entityService.createRanksAsync(state, executor), executor)
                .thenComposeAsync(v -> entityService.establishConnectionsAsync(state, executor), executor)
                .thenComposeAsync(v -> validator.validateSystemAsync(state, executor), executor)
                .whenComplete((v, t) -> {
                    initializing = false;
                    if (t != null) {
                        LOGGER.log(Level.SEVERE, "Rank system initialization failed", t);
                        this.state = RankSystemState.empty();
                    } else {
                        LOGGER.info("Rank system initialization completed");
                    }
                });
    }

    /**
     * Indicates whether the rank system has completed its initialization workflow.
     *
     * @return {@code true} when rank data has been loaded and a default rank is available
     */
    public boolean isInitialized() {
        return !state.rankTrees().isEmpty() || state.defaultRank() != null;
    }

    /**
     *
     * Provides a snapshot of the registered rank trees.
     *
     * @return an immutable map containing the known rank trees keyed by their identifiers
     */
    public @NotNull Map<String, RRankTree> getRankTrees() {
        return Map.copyOf(state.rankTrees());
    }

    /**
     * Provides the configured ranks for each tree as a defensive copy.
     *
     * @return a map of rank tree identifiers to immutable maps of rank identifiers and rank definitions
     */
    public @NotNull Map<String, Map<String, RRank>> getRanks() {
        return state.ranks().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> Map.copyOf(e.getValue())));
    }

    /**
     * Retrieves the default rank that should be assigned to new players.
     *
     * @return the default {@link RRank} if one has been configured, otherwise {@code null}
     */
    public @Nullable RRank getDefaultRank() {
        return state.defaultRank();
    }

    public RankSystemSection getRankSystemSection() { return state.rankSystemSection(); }
}