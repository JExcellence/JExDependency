package com.raindropcentral.rdq.rank.service;

import com.raindropcentral.rdq.api.FreeRankService;
import com.raindropcentral.rdq.database.entity.rank.PlayerRank;
import com.raindropcentral.rdq.database.entity.rank.PlayerRankPath;
import com.raindropcentral.rdq.rank.*;
import com.raindropcentral.rdq.rank.repository.*;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class DefaultFreeRankService implements FreeRankService {

    private static final Logger LOGGER = Logger.getLogger(DefaultFreeRankService.class.getName());

    protected final com.raindropcentral.rdq.RDQCore core;
    protected final RankTreeRepository treeRepository;
    protected final RankRepository rankRepository;
    protected final PlayerRankRepository playerRankRepository;
    protected final PlayerRankPathRepository pathRepository;
    protected final RankRequirementChecker requirementChecker;

    public DefaultFreeRankService(
        @NotNull com.raindropcentral.rdq.RDQCore core,
        @NotNull RankTreeRepository treeRepository,
        @NotNull RankRepository rankRepository,
        @NotNull PlayerRankRepository playerRankRepository,
        @NotNull PlayerRankPathRepository pathRepository,
        @NotNull RankRequirementChecker requirementChecker
    ) {
        this.core = core;
        this.treeRepository = treeRepository;
        this.rankRepository = rankRepository;
        this.playerRankRepository = playerRankRepository;
        this.pathRepository = pathRepository;
        this.requirementChecker = requirementChecker;
    }

    @Override
    public CompletableFuture<Optional<PlayerRankData>> getPlayerRanks(@NotNull UUID playerId) {
        return pathRepository.findByPlayerIdAsync(playerId)
            .thenCombine(playerRankRepository.findByPlayerIdAsync(playerId), (paths, ranks) -> {
                if (paths.isEmpty() && ranks.isEmpty()) {
                    return Optional.empty();
                }

                var activePaths = paths.stream()
                    .filter(PlayerRankPath::isActive)
                    .map(p -> new PlayerRankData.ActivePath(p.treeId(), p.currentRankId(), p.startedAt()))
                    .toList();

                var unlockedRanks = new HashMap<String, Instant>();
                for (var rank : ranks) {
                    unlockedRanks.put(rank.rankId(), rank.unlockedAt());
                }

                return Optional.of(new PlayerRankData(playerId, activePaths, unlockedRanks));
            });
    }


    @Override
    public CompletableFuture<Boolean> unlockRank(@NotNull UUID playerId, @NotNull String rankId) {
        var rankOpt = rankRepository.findById(rankId);
        if (rankOpt.isEmpty()) {
            LOGGER.warning("Attempted to unlock non-existent rank: " + rankId);
            return CompletableFuture.completedFuture(false);
        }

        var rank = rankOpt.get();

        return checkRequirements(playerId, rankId)
            .thenCompose(requirementsMet -> {
                if (!requirementsMet) {
                    return CompletableFuture.completedFuture(false);
                }

                return playerRankRepository.hasUnlockedRankAsync(playerId, rankId)
                    .thenCompose(alreadyUnlocked -> {
                        if (alreadyUnlocked) {
                            return CompletableFuture.completedFuture(false);
                        }

                        return checkLinearProgression(playerId, rank)
                            .thenCompose(canProgress -> {
                                if (!canProgress) {
                                    return CompletableFuture.completedFuture(false);
                                }

                                var playerRank = PlayerRank.create(playerId, rankId, rank.treeId());
                                return playerRankRepository.createAsync(playerRank)
                                    .thenCompose(saved -> updateOrCreatePath(playerId, rank))
                                    .thenApply(v -> true);
                            });
                    });
            });
    }

    protected CompletableFuture<Boolean> checkLinearProgression(@NotNull UUID playerId, @NotNull Rank rank) {
        var ranksInTree = rankRepository.findEnabledByTreeId(rank.treeId());
        if (ranksInTree.isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }

        var firstRank = ranksInTree.getFirst();
        if (rank.id().equals(firstRank.id())) {
            return CompletableFuture.completedFuture(true);
        }

        var previousRank = ranksInTree.stream()
            .filter(r -> r.tier() < rank.tier())
            .max(Comparator.comparingInt(Rank::tier));

        if (previousRank.isEmpty()) {
            return CompletableFuture.completedFuture(true);
        }

        return playerRankRepository.hasUnlockedRankAsync(playerId, previousRank.get().id());
    }

    protected CompletableFuture<Void> updateOrCreatePath(@NotNull UUID playerId, @NotNull Rank rank) {
        return pathRepository.findByPlayerIdAndTreeIdAsync(playerId, rank.treeId())
            .thenCompose(existingPath -> {
                if (existingPath.isPresent()) {
                    var path = existingPath.get();
                    path.advanceToRank(rank.id());
                    return pathRepository.updateAsync(path).thenApply(v -> null);
                } else {
                    return pathRepository.countActiveByPlayerIdAsync(playerId)
                        .thenCompose(activeCount -> {
                            if (activeCount >= 1) {
                                return pathRepository.deactivateAllForPlayerAsync(playerId);
                            }
                            return CompletableFuture.completedFuture(null);
                        })
                        .thenCompose(v -> {
                            var newPath = PlayerRankPath.create(playerId, rank.treeId(), rank.id());
                            return pathRepository.createAsync(newPath).thenApply(saved -> null);
                        });
                }
            });
    }


    @Override
    public CompletableFuture<Boolean> checkRequirements(@NotNull UUID playerId, @NotNull String rankId) {
        var rankOpt = rankRepository.findById(rankId);
        if (rankOpt.isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }

        var rank = rankOpt.get();
        if (rank.requirements().isEmpty()) {
            return CompletableFuture.completedFuture(true);
        }

        return requirementChecker.checkAll(playerId, rank.requirements());
    }

    @Override
    public CompletableFuture<List<RankTree>> getAvailableRankTrees() {
        return CompletableFuture.completedFuture(treeRepository.findAllEnabled());
    }

    @Override
    public CompletableFuture<Optional<Rank>> getRank(@NotNull String rankId) {
        return CompletableFuture.completedFuture(rankRepository.findById(rankId));
    }

    @Override
    public CompletableFuture<Optional<RankTree>> getRankTree(@NotNull String treeId) {
        return CompletableFuture.completedFuture(treeRepository.findById(treeId));
    }

    @Override
    public CompletableFuture<Void> reload() {
        return core.reloadRankSystem();
    }
}
