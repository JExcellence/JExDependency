package com.raindropcentral.rdq.rank;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.database.entity.rank.RRank;
import com.raindropcentral.rdq.database.entity.rank.RRankTree;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Premium version of the Rank System Service with no limitations.
 * All features are fully unlocked.
 * 
 * Features:
 * - Unlimited rank trees
 * - Unlimited ranks per tree
 * - Multiple concurrent rank paths
 * - Full access to all rank system features
 * 
 * @author JExcellence
 * @version 1.0.0
 * @since 6.0.0
 */
public class PremiumRankSystemService implements IRankSystemService {

    private static final Logger LOGGER = CentralLogger.getLogger("RDQ-Premium");

    // Premium has no limits - use Integer.MAX_VALUE for "unlimited"
    private static final int UNLIMITED = Integer.MAX_VALUE;

    private static PremiumRankSystemService instance;
    private RDQ rdq;

    /**
     * Private constructor.
     */
    private PremiumRankSystemService() {
    }

    /**
     * Initializes the Premium Rank System Service.
     *
     * @param rdq the RDQ plugin instance
     * @return the initialized service instance
     */
    public static PremiumRankSystemService initialize(@NotNull RDQ rdq) {
        if (instance == null) {
            instance = new PremiumRankSystemService();
        }
        instance.rdq = rdq;
        LOGGER.log(Level.INFO, "PremiumRankSystemService initialized - all features unlocked");
        return instance;
    }

    /**
     * Gets the initialized instance.
     * 
     * @return the service instance
     * @throws IllegalStateException if not initialized
     */
    public static PremiumRankSystemService getInstance() {
        if (instance == null) {
            throw new IllegalStateException("PremiumRankSystemService not initialized. Call initialize() first.");
        }
        return instance;
    }

    @Override
    public boolean isPremium() {
        return true;
    }

    @Override
    public int getMaxAbsolvableRankTrees() {
        return UNLIMITED;
    }

    @Override
    public int getMaxRanksPerTree() {
        return UNLIMITED;
    }

    @Override
    public int getMaxActiveRankPaths() {
        return UNLIMITED;
    }

    @Override
    public boolean canStartNewRankTree(@NotNull RDQPlayer player) {
        // Premium: Always allowed
        return true;
    }

    @Override
    public boolean canProgressToRank(@NotNull RDQPlayer player, @NotNull RRank rank) {
        // Premium: Always allowed (other prerequisites still apply)
        return true;
    }

    @Override
    public boolean isPreviewOnly(@NotNull RDQPlayer player, @NotNull RRankTree rankTree) {
        // Premium: Nothing is preview-only
        return false;
    }

    @Override
    public boolean isRankBeyondLimit(@NotNull RRank rank, @NotNull RRankTree rankTree) {
        // Premium: No rank limits
        return false;
    }

    @Override
    public int getAbsolvedRankTreeCount(@NotNull RDQPlayer player) {
        // Premium doesn't need to track this for limits, but return actual count for stats
        try {
            if (rdq == null) {
                return 0;
            }
            
            return (int) rdq.getPlayerRankPathRepository()
                    .findAllByAttributes(java.util.Map.of("player", player))
                    .stream()
                    .map(path -> path.getSelectedRankPath())
                    .filter(tree -> tree != null)
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
            
            return (int) rdq.getPlayerRankPathRepository()
                    .findAllByAttributes(java.util.Map.of("player", player))
                    .stream()
                    .filter(path -> path.isActive())
                    .count();
        } catch (Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to get active rank path count", exception);
            return 0;
        }
    }

    @Override
    public @NotNull String getRestrictionMessageKey(@NotNull RankRestrictionType restrictionType) {
        // Premium has no restrictions, but return a key just in case
        return "premium.no_restrictions";
    }
}
