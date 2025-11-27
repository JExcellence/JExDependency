package com.raindropcentral.rdq.manager.rank;

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
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class RankRequirementProgressManager {

	private static final Logger LOGGER = CentralLogger.getLogger(RankRequirementProgressManager.class.getName());
	private static final long CACHE_EXPIRY_MS = 30000L;
	private static final double PROGRESS_MIN = 0.0;
	private static final double PROGRESS_MAX = 1.0;

	private final RDQ plugin;
	private final Map<String, RequirementProgressData> progressCache = new ConcurrentHashMap<>();
	private final Map<String, Long> cacheTimestamps = new ConcurrentHashMap<>();

	public RankRequirementProgressManager(final @NotNull RDQ plugin) {
		this.plugin = Objects.requireNonNull(plugin, "Plugin cannot be null");
	}

	public @NotNull RequirementProgressData getRequirementProgress(
		final @NotNull Player player,
		final @NotNull RDQPlayer rdqPlayer,
		final @NotNull RRankUpgradeRequirement requirement
	) {
		Objects.requireNonNull(player, "Player cannot be null");
		Objects.requireNonNull(rdqPlayer, "RDQPlayer cannot be null");
		Objects.requireNonNull(requirement, "Requirement cannot be null");

		var cacheKey = generateCacheKey(player.getUniqueId().toString(), requirement.getId().toString());
		var cached = getCachedProgress(cacheKey);
		if (cached != null) return cached;

		var freshProgress = calculateRequirementProgress(player, rdqPlayer, requirement);
		cacheProgress(cacheKey, freshProgress);
		return freshProgress;
	}

	public @NotNull RequirementCompletionResult attemptRequirementCompletion(
		final @NotNull Player player,
		final @NotNull RDQPlayer rdqPlayer,
		final @NotNull RRankUpgradeRequirement requirement
	) {
		Objects.requireNonNull(player, "Player cannot be null");
		Objects.requireNonNull(rdqPlayer, "RDQPlayer cannot be null");
		Objects.requireNonNull(requirement, "Requirement cannot be null");

		try {
			var existingProgress = getOrCreateProgressEntry(rdqPlayer, requirement);

			if (existingProgress.isCompleted()) {
				LOGGER.log(Level.FINE, "Requirement " + requirement.getId() + " already completed for player " + player.getName());
				return new RequirementCompletionResult(false, "requirement.already_completed", getRequirementProgress(player, rdqPlayer, requirement));
			}

			var abstractRequirement = requirement.getRequirement().getRequirement();

			if (!abstractRequirement.isMet(player)) {
				LOGGER.log(Level.FINE, "Requirement " + requirement.getId() + " not yet met for player " + player.getName());
				return new RequirementCompletionResult(false, "requirement.not_met", getRequirementProgress(player, rdqPlayer, requirement));
			}

			try {
				LOGGER.log(Level.INFO, "Consuming resources for requirement " + requirement.getId() + " for player " + player.getName());
				abstractRequirement.consume(player);
				LOGGER.log(Level.INFO, "Successfully consumed resources for requirement " + requirement.getId());
			} catch (final Exception consumeException) {
				LOGGER.log(Level.SEVERE, "Failed to consume resources for requirement " + requirement.getId() + " for player " + player.getName(), consumeException);
				return new RequirementCompletionResult(false, "requirement.consumption_failed", getRequirementProgress(player, rdqPlayer, requirement));
			}

			existingProgress.setProgress(PROGRESS_MAX);
			plugin.getPlayerRankUpgradeProgressRepository().update(existingProgress);
			invalidateProgressCache(player.getUniqueId().toString(), requirement.getId().toString());

			LOGGER.log(Level.INFO, "Successfully completed requirement " + requirement.getId() + " for player " + player.getName() + " with resource consumption");
			return new RequirementCompletionResult(true, "requirement.completed_successfully", getRequirementProgress(player, rdqPlayer, requirement));
		} catch (final Exception exception) {
			LOGGER.log(Level.SEVERE, "Failed to complete requirement " + requirement.getId() + " for player " + player.getName(), exception);
			return new RequirementCompletionResult(false, "requirement.completion_error", getRequirementProgress(player, rdqPlayer, requirement));
		}
	}

	public boolean areAllRequirementsCompleted(final @NotNull Player player, final @NotNull RDQPlayer rdqPlayer, final @NotNull RRank rank) {
		Objects.requireNonNull(player, "Player cannot be null");
		Objects.requireNonNull(rdqPlayer, "RDQPlayer cannot be null");
		Objects.requireNonNull(rank, "Rank cannot be null");

		try {
			var requirements = rank.getUpgradeRequirements();
			if (requirements.isEmpty()) return true;

			return requirements.stream()
				.map(requirement -> getRequirementProgress(player, rdqPlayer, requirement))
				.allMatch(RequirementProgressData::isCompleted);
		} catch (final Exception exception) {
			LOGGER.log(Level.WARNING, "Failed to check if all requirements are completed for rank " + rank.getIdentifier(), exception);
			return false;
		}
	}

	public double getRankOverallProgress(final @NotNull Player player, final @NotNull RDQPlayer rdqPlayer, final @NotNull RRank rank) {
		Objects.requireNonNull(player, "Player cannot be null");
		Objects.requireNonNull(rdqPlayer, "RDQPlayer cannot be null");
		Objects.requireNonNull(rank, "Rank cannot be null");

		try {
			var requirements = rank.getUpgradeRequirements();
			if (requirements.isEmpty()) return PROGRESS_MAX;

			return requirements.stream()
				.mapToDouble(requirement -> getRequirementProgress(player, rdqPlayer, requirement).getProgressPercentage())
				.average()
				.orElse(PROGRESS_MIN);
		} catch (final Exception exception) {
			LOGGER.log(Level.WARNING, "Failed to calculate overall progress for rank " + rank.getIdentifier(), exception);
			return PROGRESS_MIN;
		}
	}

	public boolean hasCompletedUpgradeRequirement(
		final @NotNull Player player,
		final @NotNull RDQPlayer rdqPlayer,
		final @NotNull RRankUpgradeRequirement requirement
	) {
		Objects.requireNonNull(player, "Player cannot be null");
		Objects.requireNonNull(rdqPlayer, "RDQPlayer cannot be null");
		Objects.requireNonNull(requirement, "Requirement cannot be null");

		try {
			final RequirementProgressData progress = getRequirementProgress(player, rdqPlayer, requirement);
			return progress.isCompleted();
		} catch (final Exception exception) {
			LOGGER.log(Level.WARNING, "Failed to check if requirement is completed", exception);
			return false;
		}
	}

	public void initializeRankProgressTracking(
		final @NotNull RDQPlayer rdqPlayer,
		final @NotNull RRank rank
	) {
		Objects.requireNonNull(rdqPlayer, "RDQPlayer cannot be null");
		Objects.requireNonNull(rank, "Rank cannot be null");

		try {
			final Set<RRankUpgradeRequirement> requirements = rank.getUpgradeRequirements();

			for (final RRankUpgradeRequirement requirement : requirements) {
				getOrCreateProgressEntry(rdqPlayer, requirement);
			}

			LOGGER.log(Level.INFO, "Initialized progress tracking for " + requirements.size() + " requirements in rank " + rank.getIdentifier());
		} catch (final Exception exception) {
			LOGGER.log(Level.SEVERE, "Failed to initialize progress tracking for rank " + rank.getIdentifier(), exception);
		}
	}

	public void clearPlayerCache(final @NotNull Player player) {
		Objects.requireNonNull(player, "Player cannot be null");

		var playerPrefix = player.getUniqueId().toString() + ":";
		progressCache.entrySet().removeIf(entry -> entry.getKey().startsWith(playerPrefix));
		cacheTimestamps.entrySet().removeIf(entry -> entry.getKey().startsWith(playerPrefix));

		LOGGER.log(Level.FINE, "Cleared progress cache for player " + player.getName());
	}
	
	// Alias method for typo in calling code
	public void cleaRDQPlayerCache(final @NotNull Player player) {
		clearPlayerCache(player);
	}

	public void clearAllCache() {
		progressCache.clear();
		cacheTimestamps.clear();
		LOGGER.log(Level.FINE, "Cleared all progress cache");
	}

	public void refreshRankProgress(
		final @NotNull Player player,
		final @NotNull RDQPlayer rdqPlayer,
		final @NotNull RRank rank
	) {
		Objects.requireNonNull(player, "Player cannot be null");
		Objects.requireNonNull(rdqPlayer, "RDQPlayer cannot be null");
		Objects.requireNonNull(rank, "Rank cannot be null");

		try {
			final Set<RRankUpgradeRequirement> requirements = rank.getUpgradeRequirements();

			for (final RRankUpgradeRequirement requirement : requirements) {
				invalidateProgressCache(player.getUniqueId().toString(), requirement.getId().toString());
				getRequirementProgress(player, rdqPlayer, requirement);
			}

			LOGGER.log(Level.INFO, "Refreshed progress cache for " + requirements.size() + " requirements in rank " + rank.getIdentifier());
		} catch (final Exception exception) {
			LOGGER.log(Level.WARNING, "Failed to refresh rank progress for rank " + rank.getIdentifier(), exception);
		}
	}

	public void refreshRequirementProgress(
		final @NotNull Player player,
		final @NotNull RDQPlayer rdqPlayer,
		final @NotNull RRankUpgradeRequirement requirement
	) {
		Objects.requireNonNull(player, "Player cannot be null");
		Objects.requireNonNull(rdqPlayer, "RDQPlayer cannot be null");
		Objects.requireNonNull(requirement, "Requirement cannot be null");

		try {
			invalidateProgressCache(player.getUniqueId().toString(), requirement.getId().toString());
			getRequirementProgress(player, rdqPlayer, requirement);

			LOGGER.log(Level.FINE, "Refreshed progress cache for requirement " + requirement.getId());
		} catch (final Exception exception) {
			LOGGER.log(Level.WARNING, "Failed to refresh requirement progress for requirement " + requirement.getId(), exception);
		}
	}

	private @Nullable RequirementProgressData getCachedProgress(final @NotNull String cacheKey) {
		var cached = progressCache.get(cacheKey);
		var timestamp = cacheTimestamps.get(cacheKey);

		if (cached == null || timestamp == null) return null;

		if ((System.currentTimeMillis() - timestamp) >= CACHE_EXPIRY_MS) {
			progressCache.remove(cacheKey);
			cacheTimestamps.remove(cacheKey);
			return null;
		}

		return cached;
	}

	private void cacheProgress(
		final @NotNull String cacheKey,
		final @NotNull RequirementProgressData progress
	) {
		progressCache.put(cacheKey, progress);
		cacheTimestamps.put(cacheKey, System.currentTimeMillis());
	}

	private void invalidateProgressCache(
		final @NotNull String playerId,
		final @NotNull String requirementId
	) {
		final String cacheKey = generateCacheKey(playerId, requirementId);
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
				return createCompletedProgressData(requirement);
			}

			return calculateLiveProgress(player, requirement, dbProgress);
		} catch (final Exception exception) {
			LOGGER.log(Level.SEVERE, "Critical error calculating requirement progress for " + requirement.getId(), exception);
			return createErrorProgressData(requirement);
		}
	}

	private @NotNull RequirementProgressData createCompletedProgressData(
		final @NotNull RRankUpgradeRequirement requirement
	) {
		return new RequirementProgressData(
			requirement.getId().toString(),
			requirement.getRequirement().getRequirement().getType().name(),
			requirement.getRequirement().getRequirement().getDescriptionKey(),
			true,
			PROGRESS_MAX,
			RequirementStatus.COMPLETED,
			"requirement.status.completed",
			requirement.getDisplayOrder()
		);
	}

	private @NotNull RequirementProgressData calculateLiveProgress(
		final @NotNull Player player,
		final @NotNull RRankUpgradeRequirement requirement,
		final @NotNull RPlayerRankUpgradeProgress dbProgress
	) {
		try {
			final AbstractRequirement abstractRequirement = requirement.getRequirement().getRequirement();

			final boolean isMet = abstractRequirement.isMet(player);
			final double progressPercentage = clampProgress(abstractRequirement.calculateProgress(player));

			final RequirementStatus status;
			final String statusMessage;

			if (isMet) {
				status = RequirementStatus.READY_TO_COMPLETE;
				statusMessage = "requirement.status.ready_to_complete";
			} else if (progressPercentage > PROGRESS_MIN) {
				status = RequirementStatus.IN_PROGRESS;
				statusMessage = "requirement.status.in_progress";
			} else {
				status = RequirementStatus.NOT_STARTED;
				statusMessage = "requirement.status.not_started";
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
		} catch (final Exception progressException) {
			LOGGER.log(Level.WARNING, "Failed to calculate progress for requirement " + requirement.getId(), progressException);
			return createErrorProgressData(requirement);
		}
	}

	private @NotNull RequirementProgressData createErrorProgressData(
		final @NotNull RRankUpgradeRequirement requirement
	) {
		return new RequirementProgressData(
			requirement.getId().toString(),
			"UNKNOWN",
			"requirement.error.unknown",
			false,
			PROGRESS_MIN,
			RequirementStatus.ERROR,
			"requirement.status.error",
			requirement.getDisplayOrder()
		);
	}

	private double clampProgress(final double progress) {
		return Math.max(PROGRESS_MIN, Math.min(PROGRESS_MAX, progress));
	}

	private @NotNull RPlayerRankUpgradeProgress getOrCreateProgressEntry(final @NotNull RDQPlayer rdqPlayer, final @NotNull RRankUpgradeRequirement requirement) {
		try {
			var existingProgress = plugin.getPlayerRankUpgradeProgressRepository()
				.findListByAttributes(Map.of(
					"player.uniqueId", rdqPlayer.getUniqueId(),
					"upgradeRequirement.id", requirement.getId()
				));

			if (!existingProgress.isEmpty()) return existingProgress.get(0);

			var newProgress = new RPlayerRankUpgradeProgress(rdqPlayer, requirement);
			plugin.getPlayerRankUpgradeProgressRepository().create(newProgress);

			LOGGER.log(Level.FINE, "Created new progress entry for requirement " + requirement.getId() + " and player " + rdqPlayer.getPlayerName());
			return newProgress;
		} catch (final Exception exception) {
			LOGGER.log(Level.SEVERE, "Failed to get or create progress entry", exception);
			throw new RuntimeException("Database operation failed", exception);
		}
	}

	private @NotNull String generateCacheKey(
		final @NotNull String playerId,
		final @NotNull String requirementId
	) {
		return playerId + ":" + requirementId;
	}
}