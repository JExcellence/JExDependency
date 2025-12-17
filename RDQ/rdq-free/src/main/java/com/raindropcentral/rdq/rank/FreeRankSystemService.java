package com.raindropcentral.rdq.rank;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.database.entity.rank.RPlayerRankPath;
import com.raindropcentral.rdq.database.entity.rank.RRank;
import com.raindropcentral.rdq.database.entity.rank.RRankTree;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Free version of the Rank System Service with hardcoded limitations.
 * These limits cannot be changed by server owners.
 * 
 * Limitations:
 * - Maximum 1 rank tree absolvable
 * - Maximum 3 ranks per tree (higher ranks are preview-only)
 * - Single active rank path at a time
 * - Additional rank trees shown as preview-only
 * 
 * @author JExcellence
 * @version 1.0.0
 * @since 6.0.0
 */
public class FreeRankSystemService implements IRankSystemService {

    private static final Logger LOGGER = CentralLogger.getLogger("RDQ-Free");

    // Hardcoded limits - cannot be changed by server owners
    private static final int MAX_ABSOLVABLE_RANK_TREES = 1;
    private static final int MAX_RANKS_PER_TREE = 3;
    private static final int MAX_ACTIVE_RANK_PATHS = 1;

    private static FreeRankSystemService instance;
    private RDQ rdq;

    /**
     * Private constructor.
     */
    private FreeRankSystemService() {
    }

    /**
     * Initializes the Free Rank System Service.
     *
     * @param rdq the RDQ plugin instance
     * @return the initialized service instance
     */
    public static FreeRankSystemService initialize(@NotNull RDQ rdq) {
        if (instance == null) {
            instance = new FreeRankSystemService();
        }
        instance.rdq = rdq;
        LOGGER.log(Level.INFO, "FreeRankSystemService initialized with limits: " +
                "maxTrees=" + MAX_ABSOLVABLE_RANK_TREES + 
                ", maxRanks=" + MAX_RANKS_PER_TREE + 
                ", maxPaths=" + MAX_ACTIVE_RANK_PATHS);
        return instance;
    }

    /**
     * Gets the initialized instance.
     * 
     * @return the service instance
     * @throws IllegalStateException if not initialized
     */
    public static FreeRankSystemService getInstance() {
        if (instance == null) {
            throw new IllegalStateException("FreeRankSystemService not initialized. Call initialize() first.");
        }
        return instance;
    }

    @Override
    public boolean isPremium() {
        return false;
    }

    @Override
    public int getMaxAbsolvableRankTrees() {
        return MAX_ABSOLVABLE_RANK_TREES;
    }

    @Override
    public int getMaxRanksPerTree() {
        return MAX_RANKS_PER_TREE;
    }

    @Override
    public int getMaxActiveRankPaths() {
        return MAX_ACTIVE_RANK_PATHS;
    }

    @Override
    public boolean canStartNewRankTree(@NotNull RDQPlayer player) {
        int absolvedCount = getAbsolvedRankTreeCount(player);
        return absolvedCount < MAX_ABSOLVABLE_RANK_TREES;
    }

    @Override
    public boolean canProgressToRank(@NotNull RDQPlayer player, @NotNull RRank rank) {
        RRankTree rankTree = rank.getRankTree();
        if (rankTree == null) {
            return false;
        }
        return !isRankBeyondLimit(rank, rankTree);
    }

    @Override
    public boolean isPreviewOnly(@NotNull RDQPlayer player, @NotNull RRankTree rankTree) {
        // If player already has this tree active or absolved, it's not preview-only
        if (hasActiveOrAbsolvedTree(player, rankTree)) {
            return false;
        }
        
        // If player has reached max absolvable trees, other trees are preview-only
        int absolvedCount = getAbsolvedRankTreeCount(player);
        return absolvedCount >= MAX_ABSOLVABLE_RANK_TREES;
    }

    @Override
    public boolean isRankBeyondLimit(@NotNull RRank rank, @NotNull RRankTree rankTree) {
        // Get all ranks in the tree sorted by tier
        List<RRank> sortedRanks = rankTree.getRanks().stream()
                .sorted(Comparator.comparingInt(RRank::getTier))
                .toList();
        
        // Find the position of this rank
        int rankPosition = 0;
        for (int i = 0; i < sortedRanks.size(); i++) {
            if (sortedRanks.get(i).getIdentifier().equals(rank.getIdentifier())) {
                rankPosition = i + 1; // 1-based position
                break;
            }
        }
        
        // Rank is beyond limit if its position exceeds MAX_RANKS_PER_TREE
        return rankPosition > MAX_RANKS_PER_TREE;
    }

    @Override
    public int getAbsolvedRankTreeCount(@NotNull RDQPlayer player) {
        try {
            if (rdq == null) {
                return 0;
            }
            
            List<RPlayerRankPath> rankPaths = rdq.getPlayerRankPathRepository()
                    .findAllByAttributes(Map.of("player", player));
            
            // Count trees where player has made progress (has a rank path entry)
            return (int) rankPaths.stream()
                    .map(RPlayerRankPath::getSelectedRankPath)
                    .distinct()
                    .count();
        } catch (Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to get absolved rank tree count", exception);
            return 0;
        }
    }

    @Override
    public int getActiveRankPathCount(@NotNull RDQPlayer player) {
        try {
            if (rdq == null) {
                return 0;
            }
            
            List<RPlayerRankPath> rankPaths = rdq.getPlayerRankPathRepository()
                    .findAllByAttributes(Map.of("player", player));
            
            return (int) rankPaths.stream()
                    .filter(RPlayerRankPath::isActive)
                    .count();
        } catch (Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to get active rank path count", exception);
            return 0;
        }
    }

    @Override
    public @NotNull String getRestrictionMessageKey(@NotNull RankRestrictionType restrictionType) {
        return switch (restrictionType) {
            case MAX_RANK_TREES_REACHED -> "free.restriction.max_rank_trees";
            case MAX_RANKS_PER_TREE_REACHED -> "free.restriction.max_ranks_per_tree";
            case MAX_ACTIVE_PATHS_REACHED -> "free.restriction.max_active_paths";
            case RANK_TREE_PREVIEW_ONLY -> "free.restriction.preview_only";
            case RANK_BEYOND_LIMIT -> "free.restriction.rank_beyond_limit";
        };
    }

    /**
     * Checks if player has an active or absolved entry for a specific tree.
     */
    private boolean hasActiveOrAbsolvedTree(@NotNull RDQPlayer player, @NotNull RRankTree rankTree) {
        try {
            if (rdq == null) {
                return false;
            }
            
            List<RPlayerRankPath> rankPaths = rdq.getPlayerRankPathRepository()
                    .findAllByAttributes(Map.of("player", player));
            
            return rankPaths.stream()
                    .anyMatch(path -> path.getSelectedRankPath() != null && 
                            path.getSelectedRankPath().getIdentifier().equals(rankTree.getIdentifier()));
        } catch (Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to check if player has tree", exception);
            return false;
        }
    }
}
