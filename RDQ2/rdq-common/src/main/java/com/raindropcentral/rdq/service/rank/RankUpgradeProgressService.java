package com.raindropcentral.rdq.service.rank;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.database.entity.rank.RPlayerRankUpgradeProgress;
import com.raindropcentral.rdq.database.entity.rank.RRank;
import com.raindropcentral.rdq.database.entity.rank.RRankUpgradeRequirement;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Enhanced service for managing player rank upgrade progress with improved
 * async handling and better error recovery.
 */
public final class RankUpgradeProgressService {

    private static final Logger LOGGER = CentralLogger.getLogger(RankUpgradeProgressService.class.getName());

    private final @NotNull RDQ rdq;

    public RankUpgradeProgressService(final @NotNull RDQ rdq) {
        this.rdq = Objects.requireNonNull(rdq, "rdq");
    }

    /** Initializes progress tracking for a player working towards a specific rank. */
    public void initializeProgressForRank(final @NotNull RDQPlayer player, final @NotNull RRank targetRank) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(targetRank, "targetRank");

        try {
            final Set<RRankUpgradeRequirement> upgradeRequirements = targetRank.getUpgradeRequirements();
            if (upgradeRequirements.isEmpty()) {
                LOGGER.log(Level.FINE, () -> "No upgrade requirements found for rank " + targetRank.getIdentifier());
                return;
            }

            for (final RRankUpgradeRequirement upgradeRequirement : upgradeRequirements) {
                initializeProgressForRequirement(player, upgradeRequirement);
            }

            LOGGER.log(Level.INFO, () -> "Initialized progress tracking for " + upgradeRequirements.size() + " requirements for rank " + targetRank.getIdentifier());
        } catch (final Exception exception) {
            LOGGER.log(Level.SEVERE, "Failed to initialize progress for rank " + targetRank.getIdentifier(), exception);
        }
    }

    private void initializeProgressForRequirement(
        final @NotNull RDQPlayer player,
        final @NotNull RRankUpgradeRequirement upgradeRequirement
    ) {
        try {
            final List<RPlayerRankUpgradeProgress> existingProgress = rdq.getPlayerRankUpgradeProgressRepository()
                .findListByAttributes(Map.of(
                    "player.uniqueId", player.getUniqueId(),
                    "upgradeRequirement.id", upgradeRequirement.getId()
                ));

            if (existingProgress.isEmpty()) {
                final RPlayerRankUpgradeProgress newProgress = new RPlayerRankUpgradeProgress(player, upgradeRequirement);
                rdq.getPlayerRankUpgradeProgressRepository().create(newProgress);
                LOGGER.log(Level.FINE, () -> "Created progress tracking for requirement " + upgradeRequirement.getId());
            } else {
                LOGGER.log(Level.FINE, () -> "Progress tracking already exists for requirement " + upgradeRequirement.getId());
            }
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to initialize progress for requirement " + upgradeRequirement.getId(), exception);
        }
    }

    /** Gets all progress entries for a player working towards a specific rank. */
    public @NotNull List<RPlayerRankUpgradeProgress> getProgressForRank(
        final @NotNull RDQPlayer player,
        final @NotNull RRank targetRank
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(targetRank, "targetRank");

        try {
            final Set<RRankUpgradeRequirement> upgradeRequirements = targetRank.getUpgradeRequirements();
            return rdq.getPlayerRankUpgradeProgressRepository()
                .findListByAttributes(Map.of("player.uniqueId", player.getUniqueId()))
                .stream()
                .filter(progress -> upgradeRequirements.contains(progress.getUpgradeRequirement()))
                .toList();
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to get progress for rank " + targetRank.getIdentifier(), exception);
            return List.of();
        }
    }

    /** Checks if a player has completed all upgrade requirements for a rank. */
    public boolean hasCompletedAllUpgradeRequirements(final @NotNull RDQPlayer player, final @NotNull RRank targetRank) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(targetRank, "targetRank");

        try {
            final Set<RRankUpgradeRequirement> upgradeRequirements = targetRank.getUpgradeRequirements();
            if (upgradeRequirements.isEmpty()) {
                return true;
            }

            for (final RRankUpgradeRequirement upgradeRequirement : upgradeRequirements) {
                if (!hasCompletedUpgradeRequirement(player, upgradeRequirement)) {
                    return false;
                }
            }
            return true;
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to check completion for rank " + targetRank.getIdentifier(), exception);
            return false;
        }
    }

    /** Checks if a player has completed a specific upgrade requirement. */
    public boolean hasCompletedUpgradeRequirement(
        final @NotNull RDQPlayer player,
        final @NotNull RRankUpgradeRequirement upgradeRequirement
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(upgradeRequirement, "upgradeRequirement");

        try {
            final List<RPlayerRankUpgradeProgress> progressList = rdq.getPlayerRankUpgradeProgressRepository()
                .findListByAttributes(Map.of(
                    "player.uniqueId", player.getUniqueId(),
                    "upgradeRequirement.id", upgradeRequirement.getId()
                ));

            return !progressList.isEmpty() && progressList.get(0).isCompleted();
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to check completion for requirement " + upgradeRequirement.getId(), exception);
            return false;
        }
    }

    /** Updates progress for a specific upgrade requirement. */
    public void updateProgress(
        final @NotNull RDQPlayer player,
        final @NotNull RRankUpgradeRequirement upgradeRequirement,
        final double newProgress
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(upgradeRequirement, "upgradeRequirement");

        try {
            final RPlayerRankUpgradeProgress progress = getOrCreateProgress(player, upgradeRequirement);
            final double oldProgress = progress.getProgress();
            progress.setProgress(newProgress);
            rdq.getPlayerRankUpgradeProgressRepository().update(progress);
            LOGGER.log(Level.FINE, () -> "Updated progress for requirement " + upgradeRequirement.getId() + " from " + oldProgress + " to " + newProgress);
        } catch (final Exception exception) {
            LOGGER.log(Level.SEVERE, "Failed to update progress for requirement " + upgradeRequirement.getId(), exception);
        }
    }

    /** Updates progress for a specific upgrade requirement by increment. */
    public void incrementProgress(
        final @NotNull RDQPlayer player,
        final @NotNull RRankUpgradeRequirement upgradeRequirement,
        final double incrementAmount
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(upgradeRequirement, "upgradeRequirement");

        try {
            final RPlayerRankUpgradeProgress progress = getOrCreateProgress(player, upgradeRequirement);
            final double oldProgress = progress.getProgress();
            final double newProgress = progress.incrementProgress(incrementAmount);
            rdq.getPlayerRankUpgradeProgressRepository().update(progress);
            LOGGER.log(Level.FINE, () -> "Incremented progress for requirement " + upgradeRequirement.getId() + " from " + oldProgress + " to " + newProgress + " (+" + incrementAmount + ")");
        } catch (final Exception exception) {
            LOGGER.log(Level.SEVERE, "Failed to increment progress for requirement " + upgradeRequirement.getId(), exception);
        }
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

    /** Gets the progress for a specific upgrade requirement. */
    public @Nullable RPlayerRankUpgradeProgress getProgressForRequirement(
        final @NotNull RDQPlayer player,
        final @NotNull RRankUpgradeRequirement upgradeRequirement
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(upgradeRequirement, "upgradeRequirement");

        try {
            final List<RPlayerRankUpgradeProgress> progressList = rdq.getPlayerRankUpgradeProgressRepository()
                .findListByAttributes(Map.of(
                    "player.uniqueId", player.getUniqueId(),
                    "upgradeRequirement.id", upgradeRequirement.getId()
                ));

            return progressList.isEmpty() ? null : progressList.get(0);
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to get progress for requirement " + upgradeRequirement.getId(), exception);
            return null;
        }
    }

    /** Clears all progress for a player towards a specific rank. */
    public void clearProgressForRank(final @NotNull RDQPlayer player, final @NotNull RRank targetRank) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(targetRank, "targetRank");

        try {
            final List<RPlayerRankUpgradeProgress> progressEntries = getProgressForRank(player, targetRank);
            for (final RPlayerRankUpgradeProgress progress : progressEntries) {
                rdq.getPlayerRankUpgradeProgressRepository().delete(progress.getId());
            }
            LOGGER.log(Level.INFO, () -> "Cleared " + progressEntries.size() + " progress entries for rank " + targetRank.getIdentifier());
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to clear progress for rank " + targetRank.getIdentifier(), exception);
        }
    }

    /** Resets progress for a specific upgrade requirement. */
    public void resetProgressForRequirement(
        final @NotNull RDQPlayer player,
        final @NotNull RRankUpgradeRequirement upgradeRequirement
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(upgradeRequirement, "upgradeRequirement");

        try {
            final RPlayerRankUpgradeProgress progress = getProgressForRequirement(player, upgradeRequirement);
            if (progress != null) {
                progress.resetProgress();
                rdq.getPlayerRankUpgradeProgressRepository().update(progress);
                LOGGER.log(Level.FINE, () -> "Reset progress for requirement " + upgradeRequirement.getId());
            }
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to reset progress for requirement " + upgradeRequirement.getId(), exception);
        }
    }

    /** Gets the overall completion percentage for a rank. */
    public double getOverallCompletionPercentage(final @NotNull RDQPlayer player, final @NotNull RRank targetRank) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(targetRank, "targetRank");

        try {
            final Set<RRankUpgradeRequirement> upgradeRequirements = targetRank.getUpgradeRequirements();
            if (upgradeRequirements.isEmpty()) {
                return 1.0;
            }

            double totalProgress = 0.0;
            for (final RRankUpgradeRequirement upgradeRequirement : upgradeRequirements) {
                final RPlayerRankUpgradeProgress progress = getProgressForRequirement(player, upgradeRequirement);
                if (progress != null) {
                    totalProgress += progress.getProgress();
                }
            }
            return totalProgress / upgradeRequirements.size();
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to calculate completion percentage for rank " + targetRank.getIdentifier(), exception);
            return 0.0;
        }
    }

    /** Async version of initializeProgressForRank for better performance. */
    public @NotNull CompletableFuture<Void> initializeProgressForRankAsync(
        final @NotNull RDQPlayer player,
        final @NotNull RRank targetRank
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(targetRank, "targetRank");

        return CompletableFuture.runAsync(() -> initializeProgressForRank(player, targetRank), rdq.getExecutor());
    }

    /** Async version of updateProgress for better performance. */
    public @NotNull CompletableFuture<Void> updateProgressAsync(
        final @NotNull RDQPlayer player,
        final @NotNull RRankUpgradeRequirement upgradeRequirement,
        final double newProgress
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(upgradeRequirement, "upgradeRequirement");

        return CompletableFuture.runAsync(() -> updateProgress(player, upgradeRequirement, newProgress), rdq.getExecutor());
    }
}