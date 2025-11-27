package com.raindropcentral.rdq.rank.service;

import com.raindropcentral.rdq.api.PremiumRankService;
import com.raindropcentral.rdq.database.entity.rank.PlayerRankPath;
import com.raindropcentral.rdq.rank.*;
import com.raindropcentral.rdq.rank.repository.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public final class DefaultPremiumRankService extends DefaultFreeRankService implements PremiumRankService {

    private static final Logger LOGGER = Logger.getLogger(DefaultPremiumRankService.class.getName());
    private static final int DEFAULT_MAX_ACTIVE_TREES = 3;

    private final int maxActiveRankTrees;

    public DefaultPremiumRankService(
        @NotNull com.raindropcentral.rdq.RDQCore core,
        @NotNull RankTreeRepository treeRepository,
        @NotNull RankRepository rankRepository,
        @NotNull PlayerRankRepository playerRankRepository,
        @NotNull PlayerRankPathRepository pathRepository,
        @NotNull RankRequirementChecker requirementChecker
    ) {
        this(core, treeRepository, rankRepository, playerRankRepository, pathRepository, requirementChecker, DEFAULT_MAX_ACTIVE_TREES);
    }

    public DefaultPremiumRankService(
        @NotNull com.raindropcentral.rdq.RDQCore core,
        @NotNull RankTreeRepository treeRepository,
        @NotNull RankRepository rankRepository,
        @NotNull PlayerRankRepository playerRankRepository,
        @NotNull PlayerRankPathRepository pathRepository,
        @NotNull RankRequirementChecker requirementChecker,
        int maxActiveRankTrees
    ) {
        super(core, treeRepository, rankRepository, playerRankRepository, pathRepository, requirementChecker);
        this.maxActiveRankTrees = maxActiveRankTrees;
    }

    @Override
    protected CompletableFuture<Void> updateOrCreatePath(@NotNull UUID playerId, @NotNull Rank rank) {
        return pathRepository.findByPlayerIdAndTreeIdAsync(playerId, rank.treeId())
            .thenCompose(existingPath -> {
                if (existingPath.isPresent()) {
                    var path = existingPath.get();
                    path.advanceToRank(rank.id());
                    path.setActive(true);
                    return pathRepository.updateAsync(path).thenApply(v -> null);
                } else {
                    var newPath = PlayerRankPath.create(playerId, rank.treeId(), rank.id());
                    return pathRepository.createAsync(newPath).thenApply(saved -> null);
                }
            });
    }

    @Override
    public CompletableFuture<Boolean> switchRankTree(@NotNull UUID playerId, @NotNull String fromTreeId, @NotNull String toTreeId) {
        if (fromTreeId.equals(toTreeId)) {
            return CompletableFuture.completedFuture(false);
        }

        if (!treeRepository.exists(toTreeId)) {
            return CompletableFuture.completedFuture(false);
        }

        return pathRepository.findByPlayerIdAndTreeIdAsync(playerId, fromTreeId)
            .thenCompose(fromPath -> {
                if (fromPath.isEmpty() || !fromPath.get().isActive()) {
                    return CompletableFuture.completedFuture(false);
                }

                var path = fromPath.get();
                path.setActive(false);
                return pathRepository.updateAsync(path)
                    .thenCompose(saved -> pathRepository.findByPlayerIdAndTreeIdAsync(playerId, toTreeId))
                    .thenCompose(toPath -> {
                        if (toPath.isPresent()) {
                            var existingPath = toPath.get();
                            existingPath.setActive(true);
                            return pathRepository.updateAsync(existingPath).thenApply(v -> true);
                        } else {
                            var firstRank = rankRepository.findFirstRankInTree(toTreeId);
                            if (firstRank.isEmpty()) {
                                return CompletableFuture.completedFuture(false);
                            }
                            var newPath = PlayerRankPath.create(playerId, toTreeId, firstRank.get().id());
                            return pathRepository.createAsync(newPath).thenApply(v -> true);
                        }
                    });
            });
    }

    @Override
    public CompletableFuture<Boolean> canSwitchRankTree(@NotNull UUID playerId) {
        return pathRepository.countActiveByPlayerIdAsync(playerId)
            .thenApply(count -> count < maxActiveRankTrees);
    }

    @Override
    public int getMaxActiveRankTrees() {
        return maxActiveRankTrees;
    }
}
