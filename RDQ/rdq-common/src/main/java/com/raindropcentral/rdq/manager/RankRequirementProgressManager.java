package com.raindropcentral.rdq.manager;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.database.entity.rank.RPlayerRankUpgradeProgress;
import com.raindropcentral.rdq.database.entity.rank.RRank;
import com.raindropcentral.rdq.database.entity.rank.RRankUpgradeRequirement;
import com.raindropcentral.rdq.requirement.AbstractRequirement;
import com.raindropcentral.rdq.view.rank.RequirementCompletionResult;
import com.raindropcentral.rdq.view.rank.RequirementProgressData;
import com.raindropcentral.rdq.view.rank.RequirementStatus;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages rank requirement progress, including persistence, validation, and state management.
 * <p>
 * Responsibilities:
 * - Calculating and caching requirement progress
 * - Persisting completion states to the database
 * - Preventing over-completion of requirements
 * - Validating rank completion eligibility
 * - Coordinating between different rank views
 * </p>
 */
public final class RankRequirementProgressManager {

    private static final Logger LOGGER = CentralLogger.getLogger(RankRequirementProgressManager.class.getName());

    private static final long CACHE_EXPIRY_MS = 30_000L;

    private final @NotNull RDQ plugin;

    private final Map<String, RequirementProgressData> progressCache = new ConcurrentHashMap<>();
    private final Map<String, Long> cacheTimestamps = new ConcurrentHashMap<>();

    public RankRequirementProgressManager(final @NotNull RDQ plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    /** Gets the current progress for a specific requirement. */
    public @NotNull RequirementProgressData getRequirementProgress(
        final @NotNull Player player,
        final @NotNull RDQPlayer rdqPlayer,
        final @NotNull RRankUpgradeRequirement requirement
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(rdqPlayer, "rdqPlayer");
        Objects.requireNonNull(requirement, "requirement");

        final String cacheKey = generateCacheKey(player.getUniqueId().toString(), requirement.getId().toString());

        final RequirementProgressData cached = progressCache.get(cacheKey);
        final Long timestamp = cacheTimestamps.get(cacheKey);
        if (cached != null && timestamp != null && (System.currentTimeMillis() - timestamp) < CACHE_EXPIRY_MS) {
            return cached;
        }

        final RequirementProgressData freshProgress = calculateRequirementProgress(player, rdqPlayer, requirement);

        progressCache.put(cacheKey, freshProgress);
        cacheTimestamps.put(cacheKey, System.currentTimeMillis());
        return freshProgress;
    }

    /**
     * Attempts to complete a requirement and persists the result.
     * This method will consume the required resources if the requirement is successfully completed.
     */
    public @NotNull RequirementCompletionResult attemptRequirementCompletion(
        final @NotNull Player player,
        final @NotNull RDQPlayer rdqPlayer,
        final @NotNull RRankUpgradeRequirement requirement
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(rdqPlayer, "rdqPlayer");
        Objects.requireNonNull(requirement, "requirement");

        try {
            final RPlayerRankUpgradeProgress existingProgress = getOrCreateProgressEntry(rdqPlayer, requirement);
            if (existingProgress.isCompleted()) {
                LOGGER.log(Level.FINE, () -> "Requirement " + requirement.getId() + " already completed for player " + player.getName());
                return new RequirementCompletionResult(
                    false,
                    "requirement.already_completed",
                    getRequirementProgress(player, rdqPlayer, requirement)
                );
            }

            final AbstractRequirement abstractRequirement = requirement.getRequirement().getRequirement();
            final boolean isMet = safeIsMet(abstractRequirement, player);
            if (!isMet) {
                LOGGER.log(Level.FINE, () -> "Requirement " + requirement.getId() + " not yet met for player " + player.getName());
                return new RequirementCompletionResult(
                    false,
                    "requirement.not_met",
                    getRequirementProgress(player, rdqPlayer, requirement)
                );
            }

            try {
                LOGGER.log(Level.INFO, () -> "Consuming resources for requirement " + requirement.getId() + " for player " + player.getName());
                abstractRequirement.consume(player);
                LOGGER.log(Level.INFO, () -> "Successfully consumed resources for requirement " + requirement.getId());
            } catch (final Exception consumeException) {
                LOGGER.log(Level.SEVERE, "Failed to consume resources for requirement " + requirement.getId() + " for player " + player.getName(), consumeException);
                return new RequirementCompletionResult(
                    false,
                    "requirement.consumption_failed",
                    getRequirementProgress(player, rdqPlayer, requirement)
                );
            }

            existingProgress.setProgress(1.0);
            plugin.getPlayerRankUpgradeProgressRepository().update(existingProgress);

            invalidateCache(player, requirement);

            LOGGER.log(Level.INFO, () -> "Successfully completed requirement " + requirement.getId() + " for player " + player.getName() + " with resource consumption");

            return new RequirementCompletionResult(
                true,
                "requirement.completed_successfully",
                getRequirementProgress(player, rdqPlayer, requirement)
            );
        } catch (final Exception exception) {
            LOGGER.log(Level.SEVERE, "Failed to complete requirement " + requirement.getId() + " for player " + player.getName(), exception);
            return new RequirementCompletionResult(
                false,
                "requirement.completion_error",
                getRequirementProgress(player, rdqPlayer, requirement)
            );
        }
    }

    /** Checks if all requirements for a rank are completed. */
    public boolean areAllRequirementsCompleted(
        final @NotNull Player player,
        final @NotNull RDQPlayer rdqPlayer,
        final @NotNull RRank rank
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(rdqPlayer, "rdqPlayer");
        Objects.requireNonNull(rank, "rank");

        try {
            final Set<RRankUpgradeRequirement> requirements = rank.getUpgradeRequirements();
            if (requirements.isEmpty()) {
                return true;
            }

            for (final RRankUpgradeRequirement requirement : requirements) {
                final RequirementProgressData progress = getRequirementProgress(player, rdqPlayer, requirement);
                if (!progress.isCompleted()) {
                    return false;
                }
            }
            return true;
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to check if all requirements are completed for rank " + rank.getIdentifier(), exception);
            return false;
        }
    }

    /** Gets the overall progress percentage for a rank (0.0 to 1.0). */
    public double getRankOverallProgress(
        final @NotNull Player player,
        final @NotNull RDQPlayer rdqPlayer,
        final @NotNull RRank rank
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(rdqPlayer, "rdqPlayer");
        Objects.requireNonNull(rank, "rank");

        try {
            final Set<RRankUpgradeRequirement> requirements = rank.getUpgradeRequirements();
            if (requirements.isEmpty()) {
                return 1.0;
            }

            double totalProgress = 0.0;
            for (final RRankUpgradeRequirement requirement : requirements) {
                final RequirementProgressData progress = getRequirementProgress(player, rdqPlayer, requirement);
                totalProgress += progress.getProgressPercentage();
            }
            return totalProgress / requirements.size();
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to calculate overall progress for rank " + rank.getIdentifier(), exception);
            return 0.0;
        }
    }

    /** Initializes progress tracking for all requirements of a rank. */
    public void initializeRankProgressTracking(final @NotNull RDQPlayer rdqPlayer, final @NotNull RRank rank) {
        Objects.requireNonNull(rdqPlayer, "rdqPlayer");
        Objects.requireNonNull(rank, "rank");

        try {
            final Set<RRankUpgradeRequirement> requirements = rank.getUpgradeRequirements();
            for (final RRankUpgradeRequirement requirement : requirements) {
                getOrCreateProgressEntry(rdqPlayer, requirement);
            }
            LOGGER.log(Level.INFO, () -> "Initialized progress tracking for " + requirements.size() + " requirements in rank " + rank.getIdentifier());
        } catch (final Exception exception) {
            LOGGER.log(Level.SEVERE, "Failed to initialize progress tracking for rank " + rank.getIdentifier(), exception);
        }
    }

    /** Clears the progress cache for a specific player. */
    public void cleaRDQPlayerCache(final @NotNull Player player) {
        Objects.requireNonNull(player, "player");

        final String playerPrefix = player.getUniqueId().toString() + ":";
        progressCache.keySet().removeIf(key -> key.startsWith(playerPrefix));
        cacheTimestamps.keySet().removeIf(key -> key.startsWith(playerPrefix));
        LOGGER.log(Level.FINE, () -> "Cleared progress cache for player " + player.getName());
    }

    /** Clears the entire progress cache. */
    public void clearAllCache() {
        progressCache.clear();
        cacheTimestamps.clear();
        LOGGER.log(Level.FINE, "Cleared all progress cache");
    }

    /** Forces a refresh of all cached progress for a player and rank. */
    public void refreshRankProgress(
        final @NotNull Player player,
        final @NotNull RDQPlayer rdqPlayer,
        final @NotNull RRank rank
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(rdqPlayer, "rdqPlayer");
        Objects.requireNonNull(rank, "rank");

        try {
            final Set<RRankUpgradeRequirement> requirements = rank.getUpgradeRequirements();
            for (final RRankUpgradeRequirement requirement : requirements) {
                invalidateCache(player, requirement);
                getRequirementProgress(player, rdqPlayer, requirement);
            }
            LOGGER.log(Level.INFO, () -> "Refreshed progress cache for " + requirements.size() + " requirements in rank " + rank.getIdentifier());
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to refresh rank progress for rank " + rank.getIdentifier(), exception);
        }
    }

    /** Refreshes progress for a single requirement. */
    public void refreshRequirementProgress(
        final @NotNull Player player,
        final @NotNull RDQPlayer rdqPlayer,
        final @NotNull RRankUpgradeRequirement requirement
    ) {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(rdqPlayer, "rdqPlayer");
        Objects.requireNonNull(requirement, "requirement");

        try {
            invalidateCache(player, requirement);
            getRequirementProgress(player, rdqPlayer, requirement);
            LOGGER.log(Level.FINE, () -> "Refreshed progress cache for requirement " + requirement.getId());
        } catch (final Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to refresh requirement progress for requirement " + requirement.getId(), exception);
        }
    }


    private void invalidateCache(final @NotNull Player player, final @NotNull RRankUpgradeRequirement requirement) {
        final String cacheKey = generateCacheKey(player.getUniqueId().toString(), requirement.getId().toString());
        progressCache.remove(cacheKey);
        cacheTimestamps.remove(cacheKey);
    }

    private @NotNull RequirementProgressData calculateRequirementProgress(
        final @NotNull Player player,
        final @NotNull RDQPlayer rdqPlayer,
        final @NotNull RRankUpgradeRequirement requirement
    ) {
        try {
            final RPlayerRankUpgradeProgress dbProgress = getOrCreateProgressEntry(rdqPlayer, requirement);

            if (dbProgress.isCompleted()) {
                final AbstractRequirement req = requirement.getRequirement().getRequirement();
                return new RequirementProgressData(
                    requirement.getId().toString(),
                    req.getType().name(),
                    req.getDescriptionKey(),
                    true,
                    1.0,
                    RequirementStatus.COMPLETED,
                    "requirement.status.completed",
                    requirement.getDisplayOrder()
                );
            }

            final AbstractRequirement abstractRequirement = requirement.getRequirement().getRequirement();
            boolean isMet = false;
            double progressPercentage = 0.0;
            RequirementStatus status = RequirementStatus.NOT_STARTED;
            String statusMessage = "requirement.status.not_started";

            try {
                isMet = safeIsMet(abstractRequirement, player);
                progressPercentage = clamp01(safeCalculateProgress(abstractRequirement, player));

                if (isMet) {
                    status = RequirementStatus.READY_TO_COMPLETE;
                    statusMessage = "requirement.status.ready_to_complete";
                } else if (progressPercentage > 0.0) {
                    status = RequirementStatus.IN_PROGRESS;
                    statusMessage = "requirement.status.in_progress";
                }
            } catch (final Exception progressException) {
                LOGGER.log(Level.WARNING, "Failed to calculate progress for requirement " + requirement.getId(), progressException);
                status = RequirementStatus.ERROR;
                statusMessage = "requirement.status.error";
            }

            return new RequirementProgressData(
                requirement.getId().toString(),
                abstractRequirement.getType().name(),
                abstractRequirement.getDescriptionKey(),
                false,
                progressPercentage,
                status,
                statusMessage,
                requirement.getDisplayOrder()
            );
        } catch (final Exception exception) {
            LOGGER.log(Level.SEVERE, "Critical error calculating requirement progress for " + requirement.getId(), exception);
            return new RequirementProgressData(
                requirement.getId().toString(),
                "UNKNOWN",
                "requirement.error.unknown",
                false,
                0.0,
                RequirementStatus.ERROR,
                "requirement.status.error",
                requirement.getDisplayOrder()
            );
        }
    }

    private @NotNull RPlayerRankUpgradeProgress getOrCreateProgressEntry(
        final @NotNull RDQPlayer rdqPlayer,
        final @NotNull RRankUpgradeRequirement requirement
    ) {
        try {
            final List<RPlayerRankUpgradeProgress> existingProgress = plugin.getPlayerRankUpgradeProgressRepository()
                .findListByAttributes(Map.of(
                    "player.uniqueId", rdqPlayer.getUniqueId(),
                    "upgradeRequirement.id", requirement.getId()
                ));

            if (!existingProgress.isEmpty()) {
                return existingProgress.get(0);
            }

            final RPlayerRankUpgradeProgress newProgress = new RPlayerRankUpgradeProgress(rdqPlayer, requirement);
            plugin.getPlayerRankUpgradeProgressRepository().create(newProgress);
            LOGGER.log(Level.FINE, () -> "Created new progress entry for requirement " + requirement.getId() + " and player " + rdqPlayer.getPlayerName());
            return newProgress;
        } catch (final Exception exception) {
            LOGGER.log(Level.SEVERE, "Failed to get or create progress entry", exception);
            throw new RuntimeException("Database operation failed", exception);
        }
    }

    private static @NotNull String generateCacheKey(final @NotNull String playerId, final @NotNull String requirementId) {
        return playerId + ":" + requirementId;
    }

    private static double clamp01(final double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static boolean safeIsMet(final @NotNull AbstractRequirement requirement, final @NotNull Player player) {
        try {
            return requirement.isMet(player);
        } catch (final Exception e) {
            return false;
        }
    }

    private static double safeCalculateProgress(final @NotNull AbstractRequirement requirement, final @NotNull Player player) {
        try {
            return requirement.calculateProgress(player);
        } catch (final Exception e) {
            return 0.0;
        }
    }
}