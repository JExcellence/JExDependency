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


public final class RankSystemFactory {

    private static final Logger LOGGER = CentralLogger.getLogger(RankSystemFactory.class.getName());

    private final @NotNull RDQ rdq;
    private final @NotNull Executor executor;
    private final @NotNull RankConfigurationLoader loader;
    private final @NotNull RankValidationService validator;
    private final @NotNull RankEntityService entityService;

    private boolean initializing;
    private @NotNull RankSystemState state = RankSystemState.empty();

    public RankSystemFactory(@NotNull RDQ rdq) {
        this.rdq = Objects.requireNonNull(rdq);
        this.executor = rdq.getExecutor();
        this.loader = new RankConfigurationLoader(rdq);
        this.validator = new RankValidationService();
        this.entityService = new RankEntityService(rdq);
    }

    public @NotNull CompletableFuture<Void> initializeAsync() {
        if (initializing) return CompletableFuture.completedFuture(null);
        
        initializing = true;
        LOGGER.info("Rank system initialization started");

        return loader.loadAllAsync(executor)
                .thenComposeAsync(loaded -> {
                    state = loaded;
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
                        state = RankSystemState.empty();
                    } else {
                        LOGGER.info("Rank system initialization completed");
                    }
                });
    }

    public boolean isInitialized() {
        return !state.rankTrees().isEmpty() || state.defaultRank() != null;
    }

    public @NotNull Map<String, RRankTree> getRankTrees() {
        return Map.copyOf(state.rankTrees());
    }

    public @NotNull Map<String, Map<String, RRank>> getRanks() {
        return state.ranks().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> Map.copyOf(e.getValue())));
    }

    public @Nullable RRank getDefaultRank() {
        return state.defaultRank();
    }

    public RankSystemSection getRankSystemSection() {
        return state.rankSystemSection();
    }

    public void loadAndPersistRankSystem() {
        try {
            LOGGER.info("Loading rank system from YAML files...");
            
            RankSystemState loadedState = loader.loadRankSystem();
            
            validator.validate(loadedState);
            
            entityService.persistRankSystem(loadedState);
            
            this.state = loadedState;
            
            int treeCount = loadedState.getTreeSections() != null ? loadedState.getTreeSections().size() : 0;
            int rankCount = loadedState.getRankSections() != null ? 
                loadedState.getRankSections().values().stream()
                    .mapToInt(Map::size)
                    .sum() : 0;
            
            LOGGER.info(String.format("Loaded %d rank trees with %d total ranks", treeCount, rankCount));
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load and persist rank system", e);
            throw new RuntimeException("Rank system loading failed", e);
        }
    }
}