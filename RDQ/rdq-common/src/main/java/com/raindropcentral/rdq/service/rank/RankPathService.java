package com.raindropcentral.rdq.service.rank;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.database.entity.rank.RPlayerRank;
import com.raindropcentral.rdq.database.entity.rank.RPlayerRankPath;
import com.raindropcentral.rdq.database.entity.rank.RRank;
import com.raindropcentral.rdq.database.entity.rank.RRankTree;
import com.raindropcentral.rdq.view.rank.RankProgressionManager;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service for managing rank paths and player rank assignments.
 * Handles rank path selection, switching, and related operations.
 */
public final class RankPathService {

    private static final Logger LOGGER = CentralLogger.getLogger(RankPathService.class.getName());

    private final @NotNull RDQ rdq;
    private final @NotNull RankProgressionManager progressionManager;

    public RankPathService(final @NotNull RDQ rdq) {
        this.rdq = Objects.requireNonNull(rdq, "rdq");
        this.progressionManager = new RankProgressionManager(rdq);
    }

    /**
     * Assigns a default rank to a player (typically used for new players).
     */
    public void assignDefaultRank(final @NotNull RDQPlayer player, final @NotNull RRank defaultRank) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(defaultRank, "defaultRank");

        try {
            final RPlayerRank defaultPlayerRank = new RPlayerRank(player, defaultRank);
            player.addPlayerRank(defaultPlayerRank);
            rdq.getPlayerRepository().update(player);
            rdq.getPlayerRankRepository().create(defaultPlayerRank);
            LOGGER.log(Level.INFO, () -> "Assigned default rank " + defaultRank.getIdentifier() + " to player " + player.getPlayerName());
        } catch (final Exception exception) {
            LOGGER.log(Level.SEVERE, "Failed to assign default rank to player " + player.getPlayerName(), exception);
            throw new RuntimeException("Failed to assign default rank", exception);
        }
    }

    /**
     * Selects a rank path for a player and automatically assigns the initial rank.
     */
    public boolean selectRankPath(
            final @NotNull RDQPlayer player,
            final @NotNull RRankTree selectedRankTree,
            final @NotNull RRank startingRank
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(selectedRankTree, "selectedRankTree");
        Objects.requireNonNull(startingRank, "startingRank");

        try {
            if (!selectedRankTree.isEnabled()) {
                LOGGER.log(Level.WARNING, () -> "Attempted to select disabled rank tree: " + selectedRankTree.getIdentifier());
                return false;
            }

            if (!checkRankTreePrerequisites(player, selectedRankTree)) {
                LOGGER.log(Level.INFO, () -> "Player " + player.getPlayerName() + " does not meet prerequisites for rank tree: " + selectedRankTree.getIdentifier());
                return false;
            }

            deactivateAllRankPaths(player);

            RPlayerRankPath existingRankPath = getRankPathForTree(player, selectedRankTree);
            if (existingRankPath != null) {
                final RPlayerRankPath freshRankPath = rdq.getPlayerRankPathRepository().findById(existingRankPath.getId());
                if (freshRankPath != null && !freshRankPath.isActive()) {
                    freshRankPath.setActive(true);
                    rdq.getPlayerRankPathRepository().update(freshRankPath);
                }
            } else {
                final RPlayerRankPath newRankPath = new RPlayerRankPath(player, selectedRankTree, true);
                rdq.getPlayerRankPathRepository().create(newRankPath);
            }

            handleRankAssignmentForTree(player, selectedRankTree, startingRank);
            progressionManager.assignInitialRankForPath(player, selectedRankTree);
            progressionManager.processAutoCompletableRanks(player, selectedRankTree);

            LOGGER.log(Level.INFO, () -> "Successfully selected rank path " + selectedRankTree.getIdentifier() + " for player " + player.getPlayerName());
            return true;
        } catch (final Exception exception) {
            LOGGER.log(Level.SEVERE, "Failed to select rank path for player " + player.getPlayerName(), exception);
            return false;
        }
    }

    /**
     * Switches a player's active rank path to a different tree.
     */
    public boolean switchRankPath(
            final @NotNull RDQPlayer player,
            final @NotNull RRankTree newRankTree,
            final @NotNull RRank startingRank
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(newRankTree, "newRankTree");
        Objects.requireNonNull(startingRank, "startingRank");

        try {
            if (!checkRankTreePrerequisites(player, newRankTree)) {
                LOGGER.log(Level.INFO, () -> "Player " + player.getPlayerName() + " cannot switch to rank tree " + newRankTree.getIdentifier() + " - prerequisites not met");
                return false;
            }
            return selectRankPath(player, newRankTree, startingRank);
        } catch (final Exception exception) {
            LOGGER.log(Level.SEVERE, "Failed to switch rank path for player " + player.getPlayerName(), exception);
            return false;
        }
    }

    /**
     * Checks if a player has selected a specific rank path.
     */
    public boolean hasSelectedRankPath(final @NotNull RDQPlayer player, final @NotNull RRankTree rankTree) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(rankTree, "rankTree");

        try {
            final RPlayerRankPath rankPath = getRankPathForTree(player, rankTree);
            return rankPath != null && rankPath.isActive();
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to check if player has selected rank path", exception);
            return false;
        }
    }

    /**
     * Cleans up legacy rank assignments that are no longer valid.
     */
    public void cleanupLegacyRanks(final @NotNull RDQPlayer player) {
        Objects.requireNonNull(player, "player");

        try {
            // TODO: Implement cleanup logic for legacy ranks
            LOGGER.log(Level.FINE, () -> "Cleaned up legacy ranks for player " + player.getPlayerName());
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to cleanup legacy ranks", exception);
        }
    }

    /**
     * Gets all player ranks for a specific tree.
     */
    public @NotNull List<RPlayerRank> getPlayerRanksForTree(
            final @NotNull RDQPlayer player,
            final @Nullable RRankTree rankTree
    ) {
        Objects.requireNonNull(player, "player");

        try {
            final List<RPlayerRank> allPlayerRanks = rdq.getPlayerRankRepository()
                    .findListByAttributes(Map.of("player.uniqueId", player.getUniqueId()));

            if (rankTree == null) {
                return allPlayerRanks;
            }

            return allPlayerRanks.stream()
                    .filter(rank -> Objects.equals(rank.getRankTree(), rankTree))
                    .toList();
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to get player ranks for tree", exception);
            return List.of();
        }
    }

    // Private helper methods

    /**
     * Deactivates all rank paths for a player.
     */
    private void deactivateAllRankPaths(final @NotNull RDQPlayer player) {
        try {
            final List<RPlayerRankPath> allRankPaths = rdq.getPlayerRankPathRepository()
                    .findListByAttributes(Map.of("player", player));

            for (final RPlayerRankPath rp : allRankPaths) {
                if (!rp.isActive()) continue;
                final RPlayerRankPath fresh = rdq.getPlayerRankPathRepository().findById(rp.getId());
                if (fresh == null || !fresh.isActive()) continue;
                fresh.setActive(false);
                rdq.getPlayerRankPathRepository().update(fresh);
            }
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to deactivate all rank paths for player " + player.getPlayerName(), exception);
        }
    }

    /**
     * Retrieves the active rank path for a player in a specific rank tree.
     * Uses mapped attributes: player, rankTree, and active flag.
     */
    private @Nullable RPlayerRankPath getRankPathForTree(final @NotNull RDQPlayer player, final @NotNull RRankTree rankTree) {
        try {
            return rdq.getPlayerRankPathRepository()
                    .findByAttributes(Map.of(
                            "player", player,
                            "rankTree", rankTree,
                            "isActive", true
                    ));
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to get rank path for tree", exception);
            return null;
        }
    }

    /**
     * Handles rank assignment or reactivation for a player in a specific rank tree.
     */
    private void handleRankAssignmentForTree(
            final @NotNull RDQPlayer player,
            final @NotNull RRankTree rankTree,
            final @NotNull RRank startingRank
    ) {
        try {
            deactivateAllPlayerRanks(player);

            final RPlayerRank existingRankInTree = getPlayerRankForTree(player, rankTree);
            if (existingRankInTree != null) {
                final RPlayerRank freshRank = rdq.getPlayerRankRepository().findById(existingRankInTree.getId());
                if (freshRank != null) {
                    freshRank.setActive(true);
                    rdq.getPlayerRankRepository().update(freshRank);
                    LOGGER.log(Level.INFO, () -> "Reactivated existing rank for player " + player.getPlayerName() + " in tree " + rankTree.getIdentifier());
                }
            } else {
                final RPlayerRank newPlayerRank = new RPlayerRank(player, startingRank, rankTree, true);
                player.addPlayerRank(newPlayerRank);
                rdq.getPlayerRankRepository().create(newPlayerRank);
                LOGGER.log(Level.INFO, () -> "Created new rank assignment for player " + player.getPlayerName() + " in tree " + rankTree.getIdentifier());
            }

            final RDQPlayer freshPlayer = rdq.getPlayerRepository().findById(player.getId());
            if (freshPlayer != null) {
                rdq.getPlayerRepository().update(freshPlayer);
            }
        } catch (final Exception exception) {
            LOGGER.log(Level.SEVERE, "Failed to handle rank assignment for tree", exception);
            throw new RuntimeException("Rank assignment failed", exception);
        }
    }

    /**
     * Deactivates all player ranks.
     */
    private void deactivateAllPlayerRanks(final @NotNull RDQPlayer player) {
        try {
            final List<RPlayerRank> allPlayerRanks = rdq.getPlayerRankRepository()
                    .findListByAttributes(Map.of("player.uniqueId", player.getUniqueId()));

            for (final RPlayerRank pr : allPlayerRanks) {
                if (!pr.isActive()) continue;
                final RPlayerRank fresh = rdq.getPlayerRankRepository().findById(pr.getId());
                if (fresh == null || !fresh.isActive()) continue;
                fresh.setActive(false);
                rdq.getPlayerRankRepository().update(fresh);
            }
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to deactivate all player ranks for player " + player.getPlayerName(), exception);
        }
    }

    /**
     * Retrieves a player's rank in a specific rank tree.
     */
    private @Nullable RPlayerRank getPlayerRankForTree(final @NotNull RDQPlayer player, final @NotNull RRankTree rankTree) {
        try {
            final List<RPlayerRank> playerRanks = rdq.getPlayerRankRepository()
                    .findListByAttributes(Map.of("player.uniqueId", player.getUniqueId()));

            return playerRanks.stream()
                    .filter(rank -> {
                        final RRankTree playerRankTree = rank.getRankTree();
                        return playerRankTree != null && Objects.equals(playerRankTree, rankTree);
                    })
                    .findFirst()
                    .orElse(null);
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to get player rank for tree", exception);
            return null;
        }
    }

    /**
     * Checks if a player meets the prerequisites for a rank tree.
     */
    private boolean checkRankTreePrerequisites(final @NotNull RDQPlayer player, final @NotNull RRankTree rankTree) {
        try {
            // TODO: Implement prerequisite checking logic
            // This could include:
            // - Minimum level requirements
            // - Completed rank trees
            // - Special permissions
            // - Quest completions
            return true; // Placeholder - always allow for now
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to check rank tree prerequisites", exception);
            return false;
        }
    }

    /**
     * Retrieves the currently active rank path for a player.
     */
    private @Nullable RPlayerRankPath getCurrentRankPath(final @NotNull RDQPlayer player) {
        try {
            return rdq.getPlayerRankPathRepository().findByAttributes(Map.of("player", player, "isActive", true));
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to get current rank path", exception);
            return null;
        }
    }

    /**
     * Gets the count of completed rank trees for a player.
     */
    private int getCompletedRankTreesCount(final @NotNull RDQPlayer player) {
        try {
            return rdq.getPlayerRankPathRepository().findListByAttributes(Map.of("player", player, "isCompleted", true)).size();
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to get completed rank trees count", exception);
            return 0;
        }
    }

    /**
     * Checks if a rank tree is completed by a player.
     */
    private boolean isRankTreeCompleted(final @NotNull RDQPlayer player, final @NotNull RRankTree rankTree) {
        try {
            return rdq.getPlayerRankPathRepository().findByAttributes(Map.of("player", player, "rankTree", rankTree)).isCompleted();
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to check if rank tree is completed", exception);
            return false;
        }
    }
}