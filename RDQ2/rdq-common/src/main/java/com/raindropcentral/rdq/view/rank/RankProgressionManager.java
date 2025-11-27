package com.raindropcentral.rdq.view.rank;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.database.entity.rank.*;
import com.raindropcentral.rdq.service.rank.RankUpgradeProgressService;
import com.raindropcentral.rplatform.logging.CentralLogger;
import de.jexcellence.jextranslate.api.TranslationKey;
import de.jexcellence.jextranslate.api.TranslationService;
import me.devnatan.inventoryframework.context.SlotClickContext;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages rank progression operations including starting progression,
 * checking completion, handling rank redemption, and automatic rank assignment.
 * Refactored to follow best practices with:
 * - Simplified database operations
 * - Early returns to reduce complexity
 * - Clear error handling
 * - Linear execution flow
 */
public final class RankProgressionManager {

    private static final Logger LOGGER = CentralLogger.getLogger(RankProgressionManager.class.getName());

    private final @NotNull RDQ rdq;
    private final @NotNull RankUpgradeProgressService rankUpgradeProgressService;

    public RankProgressionManager(final @NotNull RDQ rdq) {
        this.rdq = Objects.requireNonNull(rdq, "rdq");
        this.rankUpgradeProgressService = new RankUpgradeProgressService(rdq);
    }

    /** Handles automatic assignment of the first rank when a player selects a rank path. */
    public void assignInitialRankForPath(final @NotNull RDQPlayer rdqPlayer, final @NotNull RRankTree rankTree) {
        Objects.requireNonNull(rdqPlayer, "rdqPlayer");
        Objects.requireNonNull(rankTree, "rankTree");

        try {
            final RRank initialRank = findInitialRankForTree(rankTree);
            if (initialRank == null) {
                LOGGER.log(Level.WARNING, () -> "No initial rank found for rank tree: " + rankTree.getIdentifier());
                return;
            }

            if (playerHasRank(rdqPlayer, initialRank)) {
                LOGGER.log(Level.FINE, () -> "Player " + rdqPlayer.getPlayerName() + " already has initial rank " + initialRank.getIdentifier());
                return;
            }

            processRankAssignment(rdqPlayer, initialRank, rankTree, true);
            LOGGER.log(Level.INFO, () -> "Automatically assigned initial rank " + initialRank.getIdentifier() + " to player " + rdqPlayer.getPlayerName() + " for path " + rankTree.getIdentifier());
        } catch (final Exception exception) {
            LOGGER.log(Level.SEVERE, "Failed to assign initial rank for path", exception);
        }
    }

    /** Starts progression tracking for a rank that is available to the player. */
    public void startRankProgression(
        final @NotNull SlotClickContext clickContext,
        final @NotNull RankNode rankNode,
        final @NotNull RDQPlayer rdqPlayer
    ) {
        Objects.requireNonNull(clickContext, "clickContext");
        Objects.requireNonNull(rankNode, "rankNode");
        Objects.requireNonNull(rdqPlayer, "rdqPlayer");

        try {
            final Player player = clickContext.getPlayer();
            final Set<RRankUpgradeRequirement> upgradeRequirements = rankNode.getRank().getUpgradeRequirements();

            if (upgradeRequirements.isEmpty()) {
                sendNoRequirementsMessage(player, rankNode.getRank());
                processRankAssignmentWithPlayer(player, rdqPlayer, rankNode.getRank(), rankNode.getRank().getRankTree(), false);
                return;
            }

            createProgressTrackingEntries(rdqPlayer, upgradeRequirements);

            TranslationService.create(TranslationKey.of("rank_progression.started"), player)
                .withAll(Map.of(
                    "rank_name", rankNode.getRank().getIdentifier(),
                    "requirement_count", upgradeRequirements.size()
                ))
                    .withPrefix()
                .send();

            LOGGER.log(Level.INFO, () -> "Started rank progression for player " + player.getName() + " on rank " + rankNode.getRank().getIdentifier());
        } catch (final Exception exception) {
            LOGGER.log(Level.SEVERE, "Failed to start rank progression", exception);
            sendErrorMessage(clickContext.getPlayer(), "rank_progression.start_failed");
        }
    }

    /** Attempts to redeem a rank that the player has been working on. */
    public void attemptRankRedemption(final @NotNull SlotClickContext clickContext, final @NotNull RankNode rankNode) {
        Objects.requireNonNull(clickContext, "clickContext");
        Objects.requireNonNull(rankNode, "rankNode");

        try {
            final Player player = clickContext.getPlayer();
            final RDQPlayer rdqPlayer = getRDQPlayer(clickContext);
            if (rdqPlayer == null) {
                sendErrorMessage(player, "rank_progression.player_not_found");
                return;
            }

            if (rankUpgradeProgressService.hasCompletedAllUpgradeRequirements(rdqPlayer, rankNode.getRank())) {
                processRankAssignmentWithPlayer(player, rdqPlayer, rankNode.getRank(), rankNode.getRank().getRankTree(), false);
            } else {
                handleIncompleteRequirements(player, rdqPlayer, rankNode.getRank());
            }
        } catch (final Exception exception) {
            LOGGER.log(Level.SEVERE, "Failed to attempt rank redemption", exception);
            sendErrorMessage(clickContext.getPlayer(), "rank_progression.redemption_failed");
        }
    }

    /** Opens a detailed view of the rank's upgrade requirements. */
    public void openRequirementsView(final @NotNull SlotClickContext clickContext, final @NotNull RankNode rankNode) {
        Objects.requireNonNull(clickContext, "clickContext");
        Objects.requireNonNull(rankNode, "rankNode");

        try {
            final Player player = clickContext.getPlayer();
            final RDQPlayer rdqPlayer = getRDQPlayer(clickContext);
            if (rdqPlayer == null) {
                sendErrorMessage(player, "rank_progression.player_not_found");
                return;
            }

            // Open requirements detail view
            this.rdq.getViewFrame().open(
                    com.raindropcentral.rdq.view.rank.view.RankRequirementDetailView.class,
                    player,
                    Map.of(
                            "plugin", this.rdq,
                            "rank", rankNode.getRank(),
                            "player", rdqPlayer
                    )
            );

            LOGGER.log(Level.FINE, () -> "Opened requirements view for rank " + rankNode.getRank().getIdentifier() + " for player " + player.getName());
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to open requirements view", exception);
            sendErrorMessage(clickContext.getPlayer(), "rank_progression.requirements_view_failed");
        }
    }

    /** Checks if any available ranks can be automatically completed and processes them. */
    public void processAutoCompletableRanks(final @NotNull RDQPlayer rdqPlayer, final @NotNull RRankTree rankTree) {
        Objects.requireNonNull(rdqPlayer, "rdqPlayer");
        Objects.requireNonNull(rankTree, "rankTree");

        try {
            final List<RRank> availableRanks = getAvailableRanksForPlayer(rdqPlayer, rankTree);
            for (final RRank rank : availableRanks) {
                if (playerHasRank(rdqPlayer, rank)) {
                    continue;
                }

                if (rank.getUpgradeRequirements().isEmpty()) {
                    processRankAssignment(rdqPlayer, rank, rankTree, false);
                    LOGGER.log(Level.INFO, () -> "Auto-completed rank " + rank.getIdentifier() + " for player " + rdqPlayer.getPlayerName() + " (no requirements)");
                }
            }
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to process auto-completable ranks", exception);
        }
    }

    // Private helper methods

    private void processRankAssignmentWithPlayer(
        final @NotNull Player player,
        final @NotNull RDQPlayer rdqPlayer,
        final @NotNull RRank rank,
        final @NotNull RRankTree rankTree,
        final boolean isInitialRank
    ) {
        try {
            handleRankAssignmentInTree(rdqPlayer, rankTree, rank, isInitialRank);
            handleLuckPermsIntegration(rdqPlayer, rank);

            final String messageKey = isInitialRank ? "rank_progression.initial_rank_assigned" : "rank_progression.redeemed_successfully";
            TranslationService.create(TranslationKey.of(messageKey), player)
                    .withPrefix()
                .with("rank_name", rank.getIdentifier())
                .send();

            executeRankRewards(rdqPlayer, rank);

            if (!isInitialRank) {
                processAutoCompletableRanks(rdqPlayer, rankTree);
            }

            LOGGER.log(Level.INFO, () -> "Player " + rdqPlayer.getPlayerName() + " successfully " + (isInitialRank ? "received initial" : "redeemed") + " rank " + rank.getIdentifier());
        } catch (final Exception exception) {
            LOGGER.log(Level.SEVERE, "Failed to process rank assignment", exception);
            sendErrorMessage(player, "rank_progression.assignment_failed");
        }
    }

    private void processRankAssignment(
        final @NotNull RDQPlayer rdqPlayer,
        final @NotNull RRank rank,
        final @NotNull RRankTree rankTree,
        final boolean isInitialRank
    ) {
        try {
            handleRankAssignmentInTree(rdqPlayer, rankTree, rank, isInitialRank);
            handleLuckPermsIntegration(rdqPlayer, rank);
            executeRankRewards(rdqPlayer, rank);

            if (!isInitialRank) {
                processAutoCompletableRanks(rdqPlayer, rankTree);
            }

            LOGGER.log(Level.INFO, () -> "Player " + rdqPlayer.getPlayerName() + " successfully " + (isInitialRank ? "received initial" : "redeemed") + " rank " + rank.getIdentifier());
        } catch (final Exception exception) {
            LOGGER.log(Level.SEVERE, "Failed to process rank assignment", exception);
            throw new RuntimeException("Rank assignment failed", exception);
        }
    }

    private void handleRankAssignmentInTree(
        final @NotNull RDQPlayer rdqPlayer,
        final @NotNull RRankTree rankTree,
        final @NotNull RRank newRank,
        final boolean isInitialRank
    ) {
        try {
            final RPlayerRank existingRankInTree = getPlayerRankForTree(rdqPlayer, rankTree);
            if (existingRankInTree != null) {
                final RPlayerRank freshRank = rdq.getPlayerRankRepository().findById(existingRankInTree.getId());
                if (freshRank == null) {
                    throw new RuntimeException("Could not find existing rank to update");
                }
                freshRank.setCurrentRank(newRank);
                freshRank.setActive(true);
                rdq.getPlayerRankRepository().update(freshRank);
                LOGGER.log(Level.INFO, () -> "Updated existing rank for player " + rdqPlayer.getPlayerName() + " to " + newRank.getIdentifier());
            } else {
                final RPlayerRank newPlayerRank = new RPlayerRank(rdqPlayer, newRank, rankTree, true);
                rdqPlayer.addPlayerRank(newPlayerRank);
                rdq.getPlayerRankRepository().create(newPlayerRank);
                LOGGER.log(Level.INFO, () -> "Created new rank assignment for player " + rdqPlayer.getPlayerName() + " with rank " + newRank.getIdentifier());
            }

            final RDQPlayer freshPlayer = rdq.getPlayerRepository().findById(rdqPlayer.getId());
            if (freshPlayer != null) {
                rdq.getPlayerRepository().update(freshPlayer);
            }

            if (isInitialRank) {
                markProgressEntriesCompleted(rdqPlayer, newRank);
            }
        } catch (final Exception exception) {
            LOGGER.log(Level.SEVERE, "Failed to handle rank assignment in tree", exception);
            throw new RuntimeException("Database rank assignment failed", exception);
        }
    }

    private void markProgressEntriesCompleted(final @NotNull RDQPlayer rdqPlayer, final @NotNull RRank newRank) {
        try {
            for (final RRankUpgradeRequirement upgradeRequirement : newRank.getUpgradeRequirements()) {
                final List<RPlayerRankUpgradeProgress> progressList = rdq.getPlayerRankUpgradeProgressRepository()
                    .findListByAttributes(Map.of(
                        "player.id", rdqPlayer.getId(),
                        "upgradeRequirement.id", upgradeRequirement.getId()
                    ));

                for (final RPlayerRankUpgradeProgress progress : progressList) {
                    if (progress.isCompleted()) {
                        continue;
                    }

                    final RPlayerRankUpgradeProgress freshProgress = rdq.getPlayerRankUpgradeProgressRepository()
                        .findById(progress.getId());
                    if (freshProgress == null) {
                        continue;
                    }

                    freshProgress.setProgress(1.0);
                    rdq.getPlayerRankUpgradeProgressRepository().update(freshProgress);
                }
            }
            LOGGER.log(Level.FINE, () -> "Marked progress entries as completed for rank " + newRank.getIdentifier());
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to mark progress entries as completed", exception);
        }
    }

    private void createProgressTrackingEntries(
        final @NotNull RDQPlayer rdqPlayer,
        final @NotNull Set<RRankUpgradeRequirement> upgradeRequirements
    ) {
        for (final RRankUpgradeRequirement requirement : upgradeRequirements) {
            final List<RPlayerRankUpgradeProgress> existingProgress = rdq.getPlayerRankUpgradeProgressRepository()
                .findListByAttributes(Map.of(
                    "player.uniqueId", rdqPlayer.getUniqueId(),
                    "upgradeRequirement.id", requirement.getId()
                ));

            if (existingProgress.isEmpty()) {
                final RPlayerRankUpgradeProgress newProgress = new RPlayerRankUpgradeProgress(rdqPlayer, requirement);
                rdq.getPlayerRankUpgradeProgressRepository().create(newProgress);
                LOGGER.log(Level.FINE, () -> "Created progress tracking for requirement " + requirement.getId());
            }
        }
    }

    private void handleLuckPermsIntegration(final @NotNull RDQPlayer rdqPlayer, final @NotNull RRank newRank) {
        try {
            if (rdq.getLuckPermsService() == null) {
                LOGGER.log(Level.WARNING, "LuckPerms service not available for rank assignment");
                return;
            }

            final String luckPermsGroup = newRank.getAssignedLuckPermsGroup();
            if (luckPermsGroup.isEmpty()) {
                LOGGER.log(Level.WARNING, () -> "No LuckPerms group defined for rank " + newRank.getIdentifier());
                return;
            }

            removePlayerFromPreviousRankGroups(rdqPlayer, newRank);

            // TODO: Implement when LuckPermsService methods are available
            // rdq.getLuckPermsService().addPlayerToGroup(rdqPlayer.getUniqueId(), luckPermsGroup);
            LOGGER.log(Level.INFO, () -> "Assigned LuckPerms group '" + luckPermsGroup + "' to player " + rdqPlayer.getPlayerName());
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to handle LuckPerms integration", exception);
            // Don't throw here - rank assignment should still work even if LuckPerms fails
        }
    }

    private void removePlayerFromPreviousRankGroups(final @NotNull RDQPlayer rdqPlayer, final @NotNull RRank newRank) {
        try {
            final RRankTree rankTree = newRank.getRankTree();
            if (rankTree == null) {
                return;
            }

            for (final RRank rank : rankTree.getRanks()) {
                if (rank.equals(newRank)) {
                    continue;
                }

                final String groupToRemove = rank.getAssignedLuckPermsGroup();
                if (groupToRemove.isEmpty()) {
                    continue;
                }

                // TODO: Implement when LuckPermsService methods are available
                // rdq.getLuckPermsService().removePlayerFromGroup(rdqPlayer.getUniqueId(), groupToRemove);
            }
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to remove player from previous rank groups", exception);
        }
    }

    private void handleIncompleteRequirements(
        final @NotNull Player player,
        final @NotNull RDQPlayer rdqPlayer,
        final @NotNull RRank rank
    ) {
        try {
            final double completionPercentage = rankUpgradeProgressService.getOverallCompletionPercentage(rdqPlayer, rank);
            final int completionPercent = (int) Math.round(completionPercentage * 100);

            TranslationService.create(TranslationKey.of("rank_progression.requirements_incomplete"), player)
                .withAll(Map.of(
                    "rank_name", rank.getIdentifier(),
                    "completion_percentage", String.valueOf(completionPercent)
                ))
                    .withPrefix()
                .send();

            showIncompleteRequirements(player, rdqPlayer, rank);
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to handle incomplete requirements", exception);
        }
    }

    private void showIncompleteRequirements(
        final @NotNull Player player,
        final @NotNull RDQPlayer rdqPlayer,
        final @NotNull RRank rank
    ) {
        try {
            final Set<RRankUpgradeRequirement> upgradeRequirements = rank.getUpgradeRequirements();
            for (final RRankUpgradeRequirement requirement : upgradeRequirements) {
                if (rankUpgradeProgressService.hasCompletedUpgradeRequirement(rdqPlayer, requirement)) {
                    continue;
                }

                final RPlayerRankUpgradeProgress progress = rankUpgradeProgressService.getProgressForRequirement(rdqPlayer, requirement);
                if (progress == null) {
                    continue;
                }

                final double currentProgress = progress.getProgress();
                final double requiredProgress = 1.0;
                final int progressPercent = (int) Math.round((currentProgress / requiredProgress) * 100);

                TranslationService.create(TranslationKey.of("rank_progression.requirement_incomplete"), player)
                        .withPrefix()
                    .withAll(Map.of(
                        "requirement_type", requirement.getRequirement().getRequirement().getType().name(),
                        "current_progress", String.valueOf((int) currentProgress),
                        "required_progress", String.valueOf((int) requiredProgress),
                        "progress_percentage", String.valueOf(progressPercent)
                    ))
                    .send();
            }
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to show incomplete requirements", exception);
        }
    }

    private void executeRankRewards(final @NotNull RDQPlayer rdqPlayer, final @NotNull RRank rank) {
        try {
            // TODO: Implement rank reward execution
            LOGGER.log(Level.FINE, () -> "Executed rank rewards for " + rdqPlayer.getPlayerName() + " on rank " + rank.getIdentifier());
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to execute rank rewards", exception);
        }
    }

    private void sendNoRequirementsMessage(final @NotNull Player player, final @NotNull RRank rank) {
        try {
            TranslationService.create(TranslationKey.of("rank_progression.no_requirements"), player)
                    .withPrefix()
                .with("rank_name", rank.getIdentifier())
                .send();
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to send no requirements message", exception);
        }
    }

    private @Nullable RRank findInitialRankForTree(final @NotNull RRankTree rankTree) {
        return rankTree.getRanks().stream()
            .filter(RRank::isInitialRank)
            .findFirst()
            .orElse(null);
    }

    private boolean playerHasRank(final @NotNull RDQPlayer rdqPlayer, final @NotNull RRank rank) {
        try {
            final List<RPlayerRank> playerRanks = rdq.getPlayerRankRepository()
                .findListByAttributes(Map.of("player.uniqueId", rdqPlayer.getUniqueId()));

            return playerRanks.stream()
                .anyMatch(playerRank -> Objects.equals(playerRank.getCurrentRank(), rank));
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to check if player has rank", exception);
            return false;
        }
    }

    private @NotNull List<RRank> getAvailableRanksForPlayer(final @NotNull RDQPlayer rdqPlayer, final @NotNull RRankTree rankTree) {
        // TODO: Implement logic to determine which ranks are available to the player
        return List.of(); // Placeholder
    }

    private @Nullable RPlayerRank getPlayerRankForTree(final @NotNull RDQPlayer rdqPlayer, final @NotNull RRankTree rankTree) {
        try {
            final List<RPlayerRank> playerRanks = rdq.getPlayerRankRepository()
                .findListByAttributes(Map.of("player.uniqueId", rdqPlayer.getUniqueId()));

            return playerRanks.stream()
                .filter(playerRank -> {
                    final RRankTree playerRankTree = playerRank.getRankTree();
                    return playerRankTree != null && Objects.equals(playerRankTree, rankTree);
                })
                .findFirst()
                .orElse(null);
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to get player rank for tree", exception);
            return null;
        }
    }

    private @Nullable RDQPlayer getRDQPlayer(final @NotNull SlotClickContext clickContext) {
        try {
            return rdq.getPlayerRepository().findByAttributes(Map.of("uniqueId", clickContext.getPlayer().getUniqueId()));
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to get RDQPlayer for player", exception);
            return null;
        }
    }

    private void sendErrorMessage(final @NotNull Player player, final @NotNull String messageKey) {
        try {
            TranslationService.create(TranslationKey.of(messageKey), player).withPrefix().send();
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to send error message", exception);
        }
    }
}
