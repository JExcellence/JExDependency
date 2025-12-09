/*
package com.raindropcentral.rdq2.service.rank;

import com.raindropcentral.rdq2.RDQ;
import com.raindropcentral.rdq2.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq2.database.entity.rank.RPlayerRankUpgradeProgress;
import com.raindropcentral.rdq2.database.entity.rank.RRank;
import com.raindropcentral.rdq2.database.entity.rank.RRankUpgradeRequirement;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public final class RankUpgradeProgressService {

    private static final Logger LOGGER = CentralLogger.getLogger(RankUpgradeProgressService.class.getName());

    private final RDQ rdq;

    public RankUpgradeProgressService(final @NotNull RDQ rdq) {
        this.rdq = Objects.requireNonNull(rdq);
    }

    public void initializeProgressForRank(final @NotNull RDQPlayer player, final @NotNull RRank targetRank) {
        final Set<RRankUpgradeRequirement> upgradeRequirements = targetRank.getUpgradeRequirements();
        if (upgradeRequirements.isEmpty()) {
            return;
        }

        upgradeRequirements.forEach(requirement -> initializeProgressForRequirement(player, requirement));
        LOGGER.info("Initialized progress for %d requirements for rank %s".formatted(upgradeRequirements.size(), targetRank.getIdentifier()));
    }

    private void initializeProgressForRequirement(
        final @NotNull RDQPlayer player,
        final @NotNull RRankUpgradeRequirement upgradeRequirement
    ) {
        final List<RPlayerRankUpgradeProgress> existingProgress = rdq.getPlayerRankUpgradeProgressRepository()
            .findListByAttributes(Map.of(
                "player.uniqueId", player.getUniqueId(),
                "upgradeRequirement.id", upgradeRequirement.getId()
            ));

        if (existingProgress.isEmpty()) {
            final RPlayerRankUpgradeProgress newProgress = new RPlayerRankUpgradeProgress(player, upgradeRequirement);
            rdq.getPlayerRankUpgradeProgressRepository().create(newProgress);
        }
    }

    public @NotNull List<RPlayerRankUpgradeProgress> getProgressForRank(
        final @NotNull RDQPlayer player,
        final @NotNull RRank targetRank
    ) {
        final Set<RRankUpgradeRequirement> upgradeRequirements = targetRank.getUpgradeRequirements();
        return rdq.getPlayerRankUpgradeProgressRepository()
            .findListByAttributes(Map.of("player.uniqueId", player.getUniqueId()))
            .stream()
            .filter(progress -> upgradeRequirements.contains(progress.getUpgradeRequirement()))
            .toList();
    }

    public boolean hasCompletedAllUpgradeRequirements(final @NotNull RDQPlayer player, final @NotNull RRank targetRank) {
        final Set<RRankUpgradeRequirement> upgradeRequirements = targetRank.getUpgradeRequirements();
        if (upgradeRequirements.isEmpty()) {
            return true;
        }

        return upgradeRequirements.stream()
            .allMatch(requirement -> hasCompletedUpgradeRequirement(player, requirement));
    }

    public boolean hasCompletedUpgradeRequirement(
        final @NotNull RDQPlayer player,
        final @NotNull RRankUpgradeRequirement upgradeRequirement
    ) {
        return rdq.getPlayerRankUpgradeProgressRepository()
            .findListByAttributes(Map.of(
                "player.uniqueId", player.getUniqueId(),
                "upgradeRequirement.id", upgradeRequirement.getId()
            ))
            .stream()
            .findFirst()
            .map(RPlayerRankUpgradeProgress::isCompleted)
            .orElse(false);
    }

    public void updateProgress(
        final @NotNull RDQPlayer player,
        final @NotNull RRankUpgradeRequirement upgradeRequirement,
        final double newProgress
    ) {
        final RPlayerRankUpgradeProgress progress = getOrCreateProgress(player, upgradeRequirement);
        progress.setProgress(newProgress);
        rdq.getPlayerRankUpgradeProgressRepository().update(progress);
    }

    public void incrementProgress(
        final @NotNull RDQPlayer player,
        final @NotNull RRankUpgradeRequirement upgradeRequirement,
        final double incrementAmount
    ) {
        final RPlayerRankUpgradeProgress progress = getOrCreateProgress(player, upgradeRequirement);
        progress.incrementProgress(incrementAmount);
        rdq.getPlayerRankUpgradeProgressRepository().update(progress);
    }

    private @NotNull RPlayerRankUpgradeProgress getOrCreateProgress(
        final @NotNull RDQPlayer player,
        final @NotNull RRankUpgradeRequirement upgradeRequirement
    ) {
        final List<RPlayerRankUpgradeProgress> progressList = rdq.getPlayerRankUpgradeProgressRepository()
            .findListByAttributes(Map.of(
                "player.uniqueId", player.getUniqueId(),
                "upgradeRequirement.id", upgradeRequirement.getId()
            ));

        if (!progressList.isEmpty()) {
            return progressList.get(0);
        }

        final RPlayerRankUpgradeProgress newProgress = new RPlayerRankUpgradeProgress(player, upgradeRequirement);
        rdq.getPlayerRankUpgradeProgressRepository().create(newProgress);
        return newProgress;
    }

    public @NotNull Optional<RPlayerRankUpgradeProgress> getProgressForRequirement(
        final @NotNull RDQPlayer player,
        final @NotNull RRankUpgradeRequirement upgradeRequirement
    ) {
        return rdq.getPlayerRankUpgradeProgressRepository()
            .findListByAttributes(Map.of(
                "player.uniqueId", player.getUniqueId(),
                "upgradeRequirement.id", upgradeRequirement.getId()
            ))
            .stream()
            .findFirst();
    }

    public void clearProgressForRank(final @NotNull RDQPlayer player, final @NotNull RRank targetRank) {
        final List<RPlayerRankUpgradeProgress> progressEntries = getProgressForRank(player, targetRank);
        progressEntries.forEach(progress -> rdq.getPlayerRankUpgradeProgressRepository().delete(progress.getId()));
        LOGGER.info("Cleared %d progress entries for rank %s".formatted(progressEntries.size(), targetRank.getIdentifier()));
    }

    public void resetProgressForRequirement(
        final @NotNull RDQPlayer player,
        final @NotNull RRankUpgradeRequirement upgradeRequirement
    ) {
        getProgressForRequirement(player, upgradeRequirement).ifPresent(progress -> {
            progress.setProgress(0.0);
            rdq.getPlayerRankUpgradeProgressRepository().update(progress);
        });
    }

    public double getOverallCompletionPercentage(final @NotNull RDQPlayer player, final @NotNull RRank targetRank) {
        final Set<RRankUpgradeRequirement> upgradeRequirements = targetRank.getUpgradeRequirements();
        if (upgradeRequirements.isEmpty()) {
            return 1.0;
        }

        final double totalProgress = upgradeRequirements.stream()
            .map(requirement -> getProgressForRequirement(player, requirement))
            .filter(Optional::isPresent)
            .mapToDouble(opt -> opt.get().getProgress())
            .sum();

        return totalProgress / upgradeRequirements.size();
    }

    public @NotNull CompletableFuture<Void> initializeProgressForRankAsync(
        final @NotNull RDQPlayer player,
        final @NotNull RRank targetRank
    ) {
        return CompletableFuture.runAsync(() -> initializeProgressForRank(player, targetRank), rdq.getExecutor());
    }

    public @NotNull CompletableFuture<Void> updateProgressAsync(
        final @NotNull RDQPlayer player,
        final @NotNull RRankUpgradeRequirement upgradeRequirement,
        final double newProgress
    ) {
        return CompletableFuture.runAsync(() -> updateProgress(player, upgradeRequirement, newProgress), rdq.getExecutor());
    }
}*/
