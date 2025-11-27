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


public final class RankRequirementProgressManager {

    private static final Logger LOGGER = CentralLogger.getLogger(RankRequirementProgressManager.class.getName());
    private static final long CACHE_EXPIRY_MS = 30_000L;

    private final @NotNull RDQ plugin;
    private final Map<String, RequirementProgressData> progressCache = new ConcurrentHashMap<>();
    private final Map<String, Long> cacheTimestamps = new ConcurrentHashMap<>();

    public RankRequirementProgressManager(@NotNull RDQ plugin) {
        this.plugin = Objects.requireNonNull(plugin);
    }

    public @NotNull RequirementProgressData getRequirementProgress(@NotNull Player player, @NotNull RDQPlayer rdqPlayer, @NotNull RRankUpgradeRequirement requirement) {
        Objects.requireNonNull(player);
        Objects.requireNonNull(rdqPlayer);
        Objects.requireNonNull(requirement);

        var cacheKey = generateCacheKey(player.getUniqueId().toString(), requirement.getId().toString());
        var cached = progressCache.get(cacheKey);
        var timestamp = cacheTimestamps.get(cacheKey);
        
        if (cached != null && timestamp != null && (System.currentTimeMillis() - timestamp) < CACHE_EXPIRY_MS) {
            return cached;
        }

        var freshProgress = calculateRequirementProgress(player, rdqPlayer, requirement);
        progressCache.put(cacheKey, freshProgress);
        cacheTimestamps.put(cacheKey, System.currentTimeMillis());
        return freshProgress;
    }

    public @NotNull RequirementCompletionResult attemptRequirementCompletion(@NotNull Player player, @NotNull RDQPlayer rdqPlayer, @NotNull RRankUpgradeRequirement requirement) {
        Objects.requireNonNull(player);
        Objects.requireNonNull(rdqPlayer);
        Objects.requireNonNull(requirement);

        try {
            var existingProgress = getOrCreateProgressEntry(rdqPlayer, requirement);
            if (existingProgress.isCompleted()) {
                LOGGER.log(Level.FINE, () -> "Requirement " + requirement.getId() + " already completed for player " + player.getName());
                return new RequirementCompletionResult(false, "requirement.already_completed", getRequirementProgress(player, rdqPlayer, requirement));
            }

            var abstractRequirement = requirement.getRequirement().getRequirement();
            var isMet = safeIsMet(abstractRequirement, player);
            if (!isMet) {
                LOGGER.log(Level.FINE, () -> "Requirement " + requirement.getId() + " not yet met for player " + player.getName());
                return new RequirementCompletionResult(false, "requirement.not_met", getRequirementProgress(player, rdqPlayer, requirement));
            }

            try {
                LOGGER.log(Level.INFO, () -> "Consuming resources for requirement " + requirement.getId() + " for player " + player.getName());
                abstractRequirement.consume(player);
                LOGGER.log(Level.INFO, () -> "Successfully consumed resources for requirement " + requirement.getId());
            } catch (Exception consumeException) {
                LOGGER.log(Level.SEVERE, "Failed to consume resources for requirement " + requirement.getId() + " for player " + player.getName(), consumeException);
                return new RequirementCompletionResult(false, "requirement.consumption_failed", getRequirementProgress(player, rdqPlayer, requirement));
            }

            existingProgress.setProgress(1.0);
            plugin.getPlayerRankUpgradeProgressRepository().update(existingProgress);
            invalidateCache(player, requirement);
            LOGGER.log(Level.INFO, () -> "Successfully completed requirement " + requirement.getId() + " for player " + player.getName() + " with resource consumption");
            return new RequirementCompletionResult(true, "requirement.completed_successfully", getRequirementProgress(player, rdqPlayer, requirement));
        } catch (Exception exception) {
            LOGGER.log(Level.SEVERE, "Failed to complete requirement " + requirement.getId() + " for player " + player.getName(), exception);
            return new RequirementCompletionResult(false, "requirement.completion_error", getRequirementProgress(player, rdqPlayer, requirement));
        }
    }

    public boolean areAllRequirementsCompleted(@NotNull Player player, @NotNull RDQPlayer rdqPlayer, @NotNull RRank rank) {
        Objects.requireNonNull(player);
        Objects.requireNonNull(rdqPlayer);
        Objects.requireNonNull(rank);

        try {
            var requirements = rank.getUpgradeRequirements();
            if (requirements.isEmpty()) return true;

            return requirements.stream()
                    .map(requirement -> getRequirementProgress(player, rdqPlayer, requirement))
                    .allMatch(RequirementProgressData::isCompleted);
        } catch (Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to check if all requirements are completed for rank " + rank.getIdentifier(), exception);
            return false;
        }
    }

    public double getRankOverallProgress(@NotNull Player player, @NotNull RDQPlayer rdqPlayer, @NotNull RRank rank) {
        Objects.requireNonNull(player);
        Objects.requireNonNull(rdqPlayer);
        Objects.requireNonNull(rank);

        try {
            var requirements = rank.getUpgradeRequirements();
            if (requirements.isEmpty()) return 1.0;

            return requirements.stream()
                    .mapToDouble(requirement -> getRequirementProgress(player, rdqPlayer, requirement).getProgressPercentage())
                    .average()
                    .orElse(0.0);
        } catch (Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to calculate overall progress for rank " + rank.getIdentifier(), exception);
            return 0.0;
        }
    }

    public void initializeRankProgressTracking(@NotNull RDQPlayer rdqPlayer, @NotNull RRank rank) {
        Objects.requireNonNull(rdqPlayer);
        Objects.requireNonNull(rank);

        try {
            var requirements = rank.getUpgradeRequirements();
            requirements.forEach(requirement -> getOrCreateProgressEntry(rdqPlayer, requirement));
            LOGGER.log(Level.INFO, () -> "Initialized progress tracking for " + requirements.size() + " requirements in rank " + rank.getIdentifier());
        } catch (Exception exception) {
            LOGGER.log(Level.SEVERE, "Failed to initialize progress tracking for rank " + rank.getIdentifier(), exception);
        }
    }

    public void clearPlayerCache(@NotNull Player player) {
        Objects.requireNonNull(player);

        var playerPrefix = player.getUniqueId().toString() + ":";
        progressCache.keySet().removeIf(key -> key.startsWith(playerPrefix));
        cacheTimestamps.keySet().removeIf(key -> key.startsWith(playerPrefix));
        LOGGER.log(Level.FINE, () -> "Cleared progress cache for player " + player.getName());
    }

    public void clearAllCache() {
        progressCache.clear();
        cacheTimestamps.clear();
        LOGGER.log(Level.FINE, "Cleared all progress cache");
    }

    public void refreshRankProgress(@NotNull Player player, @NotNull RDQPlayer rdqPlayer, @NotNull RRank rank) {
        Objects.requireNonNull(player);
        Objects.requireNonNull(rdqPlayer);
        Objects.requireNonNull(rank);

        try {
            var requirements = rank.getUpgradeRequirements();
            requirements.forEach(requirement -> {
                invalidateCache(player, requirement);
                getRequirementProgress(player, rdqPlayer, requirement);
            });
            LOGGER.log(Level.INFO, () -> "Refreshed progress cache for " + requirements.size() + " requirements in rank " + rank.getIdentifier());
        } catch (Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to refresh rank progress for rank " + rank.getIdentifier(), exception);
        }
    }

    public void refreshRequirementProgress(@NotNull Player player, @NotNull RDQPlayer rdqPlayer, @NotNull RRankUpgradeRequirement requirement) {
        Objects.requireNonNull(player);
        Objects.requireNonNull(rdqPlayer);
        Objects.requireNonNull(requirement);

        try {
            invalidateCache(player, requirement);
            getRequirementProgress(player, rdqPlayer, requirement);
            LOGGER.log(Level.FINE, () -> "Refreshed progress cache for requirement " + requirement.getId());
        } catch (Exception exception) {
            LOGGER.log(Level.WARNING, "Failed to refresh requirement progress for requirement " + requirement.getId(), exception);
        }
    }


    private void invalidateCache(@NotNull Player player, @NotNull RRankUpgradeRequirement requirement) {
        var cacheKey = generateCacheKey(player.getUniqueId().toString(), requirement.getId().toString());
        progressCache.remove(cacheKey);
        cacheTimestamps.remove(cacheKey);
    }

    private @NotNull RequirementProgressData calculateRequirementProgress(@NotNull Player player, @NotNull RDQPlayer rdqPlayer, @NotNull RRankUpgradeRequirement requirement) {
        try {
            var dbProgress = getOrCreateProgressEntry(rdqPlayer, requirement);

            if (dbProgress.isCompleted()) {
                var req = requirement.getRequirement().getRequirement();
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

            var abstractRequirement = requirement.getRequirement().getRequirement();
            var isMet = false;
            var progressPercentage = 0.0;
            var status = RequirementStatus.NOT_STARTED;
            var statusMessage = "requirement.status.not_started";

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
            } catch (Exception progressException) {
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
        } catch (Exception exception) {
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

    private @NotNull RPlayerRankUpgradeProgress getOrCreateProgressEntry(@NotNull RDQPlayer rdqPlayer, @NotNull RRankUpgradeRequirement requirement) {
        try {
            var existingProgress = plugin.getPlayerRankUpgradeProgressRepository()
                .findListByAttributes(Map.of(
                    "player.uniqueId", rdqPlayer.getUniqueId(),
                    "upgradeRequirement.id", requirement.getId()
                ));

            if (!existingProgress.isEmpty()) {
                return existingProgress.get(0);
            }

            var newProgress = new RPlayerRankUpgradeProgress(rdqPlayer, requirement);
            plugin.getPlayerRankUpgradeProgressRepository().create(newProgress);
            LOGGER.log(Level.FINE, () -> "Created new progress entry for requirement " + requirement.getId() + " and player " + rdqPlayer.getPlayerName());
            return newProgress;
        } catch (Exception exception) {
            LOGGER.log(Level.SEVERE, "Failed to get or create progress entry", exception);
            throw new RuntimeException("Database operation failed", exception);
        }
    }

    private static @NotNull String generateCacheKey(@NotNull String playerId, @NotNull String requirementId) {
        return playerId + ":" + requirementId;
    }

    private static double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static boolean safeIsMet(@NotNull AbstractRequirement requirement, @NotNull Player player) {
        try {
            return requirement.isMet(player);
        } catch (Exception e) {
            return false;
        }
    }

    private static double safeCalculateProgress(@NotNull AbstractRequirement requirement, @NotNull Player player) {
        try {
            return requirement.calculateProgress(player);
        } catch (Exception e) {
            return 0.0;
        }
    }
}