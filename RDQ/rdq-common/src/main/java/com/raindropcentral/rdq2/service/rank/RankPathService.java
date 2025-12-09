/*
package com.raindropcentral.rdq2.service.rank;

import com.raindropcentral.rdq2.RDQ;
import com.raindropcentral.rdq2.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq2.database.entity.rank.RPlayerRank;
import com.raindropcentral.rdq2.database.entity.rank.RPlayerRankPath;
import com.raindropcentral.rdq2.database.entity.rank.RRank;
import com.raindropcentral.rdq2.database.entity.rank.RRankTree;
import com.raindropcentral.rdq2.view.rank.RankProgressionManager;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

public final class RankPathService {

    private static final Logger LOGGER = CentralLogger.getLogger(RankPathService.class.getName());

    private final RDQ rdq;
    private final RankProgressionManager progressionManager;

    public RankPathService(final @NotNull RDQ rdq) {
        this.rdq = Objects.requireNonNull(rdq);
        this.progressionManager = new RankProgressionManager(rdq);
    }

    public void assignDefaultRank(final @NotNull RDQPlayer player, final @NotNull RRank defaultRank) {
        final RPlayerRank defaultPlayerRank = new RPlayerRank(player, defaultRank);
        player.addPlayerRank(defaultPlayerRank);
        rdq.getPlayerRepository().update(player);
        rdq.getPlayerRankRepository().create(defaultPlayerRank);
        LOGGER.info("Assigned default rank %s to player %s".formatted(defaultRank.getIdentifier(), player.getPlayerName()));
    }

    public boolean selectRankPath(
            final @NotNull RDQPlayer player,
            final @NotNull RRankTree selectedRankTree,
            final @NotNull RRank startingRank
    ) {
        if (!selectedRankTree.isEnabled()) {
            LOGGER.warning("Attempted to select disabled rank tree: " + selectedRankTree.getIdentifier());
            return false;
        }

        if (!checkRankTreePrerequisites(player, selectedRankTree)) {
            return false;
        }

        deactivateAllRankPaths(player);

        getRankPathForTree(player, selectedRankTree)
            .ifPresentOrElse(
                existingPath -> activateRankPath(existingPath),
                () -> createNewRankPath(player, selectedRankTree)
            );

        handleRankAssignmentForTree(player, selectedRankTree, startingRank);
        progressionManager.assignInitialRankForPath(player, selectedRankTree);
        progressionManager.processAutoCompletableRanks(player, selectedRankTree);

        LOGGER.info("Selected rank path %s for player %s".formatted(selectedRankTree.getIdentifier(), player.getPlayerName()));
        return true;
    }

    private void activateRankPath(final @NotNull RPlayerRankPath rankPath) {
        final RPlayerRankPath fresh = rdq.getPlayerRankPathRepository().findById(rankPath.getId());
        if (fresh != null && !fresh.isActive()) {
            fresh.setActive(true);
            rdq.getPlayerRankPathRepository().update(fresh);
        }
    }

    private void createNewRankPath(final @NotNull RDQPlayer player, final @NotNull RRankTree rankTree) {
        final RPlayerRankPath newRankPath = new RPlayerRankPath(player, rankTree, true);
        rdq.getPlayerRankPathRepository().create(newRankPath);
    }

    public boolean switchRankPath(
            final @NotNull RDQPlayer player,
            final @NotNull RRankTree newRankTree,
            final @NotNull RRank startingRank
    ) {
        if (!checkRankTreePrerequisites(player, newRankTree)) {
            return false;
        }
        return selectRankPath(player, newRankTree, startingRank);
    }

    public boolean hasSelectedRankPath(final @NotNull RDQPlayer player, final @NotNull RRankTree rankTree) {
        return getRankPathForTree(player, rankTree)
            .map(RPlayerRankPath::isActive)
            .orElse(false);
    }

    public void cleanupLegacyRanks(final @NotNull RDQPlayer player) {
        // TODO: Implement cleanup logic for legacy ranks
    }

    public @NotNull List<RPlayerRank> getPlayerRanksForTree(
            final @NotNull RDQPlayer player,
            final @Nullable RRankTree rankTree
    ) {
        final List<RPlayerRank> allPlayerRanks = rdq.getPlayerRankRepository()
                .findListByAttributes(Map.of("player.uniqueId", player.getUniqueId()));

        return rankTree == null ? allPlayerRanks : allPlayerRanks.stream()
                .filter(rank -> Objects.equals(rank.getRankTree(), rankTree))
                .toList();
    }

    private void deactivateAllRankPaths(final @NotNull RDQPlayer player) {
        rdq.getPlayerRankPathRepository()
            .findListByAttributes(Map.of("player", player))
            .stream()
            .filter(RPlayerRankPath::isActive)
            .forEach(this::deactivateRankPath);
    }

    private void deactivateRankPath(final @NotNull RPlayerRankPath rankPath) {
        final RPlayerRankPath fresh = rdq.getPlayerRankPathRepository().findById(rankPath.getId());
        if (fresh != null && fresh.isActive()) {
            fresh.setActive(false);
            rdq.getPlayerRankPathRepository().update(fresh);
        }
    }

    private @NotNull Optional<RPlayerRankPath> getRankPathForTree(final @NotNull RDQPlayer player, final @NotNull RRankTree rankTree) {
        return Optional.ofNullable(rdq.getPlayerRankPathRepository()
                .findByAttributes(Map.of(
                        "player", player,
                        "rankTree", rankTree,
                        "isActive", true
                )));
    }

    private void handleRankAssignmentForTree(
            final @NotNull RDQPlayer player,
            final @NotNull RRankTree rankTree,
            final @NotNull RRank startingRank
    ) {
        deactivateAllPlayerRanks(player);

        getPlayerRankForTree(player, rankTree)
            .ifPresentOrElse(
                existingRank -> reactivatePlayerRank(existingRank),
                () -> createNewPlayerRank(player, startingRank, rankTree)
            );

        final RDQPlayer freshPlayer = rdq.getPlayerRepository().findById(player.getId());
        if (freshPlayer != null) {
            rdq.getPlayerRepository().update(freshPlayer);
        }
    }

    private void reactivatePlayerRank(final @NotNull RPlayerRank playerRank) {
        final RPlayerRank fresh = rdq.getPlayerRankRepository().findById(playerRank.getId());
        if (fresh != null) {
            fresh.setActive(true);
            rdq.getPlayerRankRepository().update(fresh);
        }
    }

    private void createNewPlayerRank(final @NotNull RDQPlayer player, final @NotNull RRank rank, final @NotNull RRankTree rankTree) {
        final RPlayerRank newPlayerRank = new RPlayerRank(player, rank, rankTree, true);
        player.addPlayerRank(newPlayerRank);
        rdq.getPlayerRankRepository().create(newPlayerRank);
    }

    private void deactivateAllPlayerRanks(final @NotNull RDQPlayer player) {
        rdq.getPlayerRankRepository()
            .findListByAttributes(Map.of("player.uniqueId", player.getUniqueId()))
            .stream()
            .filter(RPlayerRank::isActive)
            .forEach(this::deactivatePlayerRank);
    }

    private void deactivatePlayerRank(final @NotNull RPlayerRank playerRank) {
        final RPlayerRank fresh = rdq.getPlayerRankRepository().findById(playerRank.getId());
        if (fresh != null && fresh.isActive()) {
            fresh.setActive(false);
            rdq.getPlayerRankRepository().update(fresh);
        }
    }

    private @NotNull Optional<RPlayerRank> getPlayerRankForTree(final @NotNull RDQPlayer player, final @NotNull RRankTree rankTree) {
        return rdq.getPlayerRankRepository()
                .findListByAttributes(Map.of("player.uniqueId", player.getUniqueId()))
                .stream()
                .filter(rank -> Objects.equals(rank.getRankTree(), rankTree))
                .findFirst();
    }

    private boolean checkRankTreePrerequisites(final @NotNull RDQPlayer player, final @NotNull RRankTree rankTree) {
        // TODO: Implement prerequisite checking logic
        return true;
    }

    private @NotNull Optional<RPlayerRankPath> getCurrentRankPath(final @NotNull RDQPlayer player) {
        return Optional.ofNullable(rdq.getPlayerRankPathRepository()
            .findByAttributes(Map.of("player", player, "isActive", true)));
    }

    private int getCompletedRankTreesCount(final @NotNull RDQPlayer player) {
        return rdq.getPlayerRankPathRepository()
            .findListByAttributes(Map.of("player", player, "isCompleted", true))
            .size();
    }

    private boolean isRankTreeCompleted(final @NotNull RDQPlayer player, final @NotNull RRankTree rankTree) {
        return Optional.ofNullable(rdq.getPlayerRankPathRepository()
            .findByAttributes(Map.of("player", player, "rankTree", rankTree)))
            .map(RPlayerRankPath::isCompleted)
            .orElse(false);
    }
}*/
