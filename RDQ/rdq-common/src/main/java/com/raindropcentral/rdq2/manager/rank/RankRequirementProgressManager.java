/*
package com.raindropcentral.rdq2.manager.rank;

import com.raindropcentral.rdq2.RDQ;
import com.raindropcentral.rdq2.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq2.database.entity.rank.RPlayerRankUpgradeProgress;
import com.raindropcentral.rdq2.database.entity.rank.RRank;
import com.raindropcentral.rdq2.database.entity.rank.RRankUpgradeRequirement;
import com.raindropcentral.rdq2.requirement.AbstractRequirement;
import com.raindropcentral.rdq2.view.rank.RequirementCompletionResult;
import com.raindropcentral.rdq2.view.rank.RequirementProgressData;
import com.raindropcentral.rdq2.view.rank.RequirementStatus;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public final class RankRequirementProgressManager {

	private static final Logger LOGGER = CentralLogger.getLogger(RankRequirementProgressManager.class.getName());
	private static final long CACHE_EXPIRY_MS = 30000L;
	private static final double PROGRESS_MIN = 0.0;
	private static final double PROGRESS_MAX = 1.0;

	private final RDQ plugin;
	private final Map<String, CachedProgress> progressCache = new ConcurrentHashMap<>();

	public RankRequirementProgressManager(final @NotNull RDQ plugin) {
		this.plugin = Objects.requireNonNull(plugin);
	}

	private record CachedProgress(RequirementProgressData data, long timestamp) {
		boolean isExpired() {
			return (System.currentTimeMillis() - timestamp) >= CACHE_EXPIRY_MS;
		}
	}

	public @NotNull RequirementProgressData getRequirementProgress(
		final @NotNull Player player,
		final @NotNull RDQPlayer rdqPlayer,
		final @NotNull RRankUpgradeRequirement requirement
	) {
		final String cacheKey = generateCacheKey(player.getUniqueId().toString(), requirement.getId().toString());
		
		return progressCache.computeIfAbsent(cacheKey, k -> 
			new CachedProgress(calculateRequirementProgress(player, rdqPlayer, requirement), System.currentTimeMillis())
		).data();
	}

	public @NotNull RequirementCompletionResult attemptRequirementCompletion(
		final @NotNull Player player,
		final @NotNull RDQPlayer rdqPlayer,
		final @NotNull RRankUpgradeRequirement requirement
	) {
		final RPlayerRankUpgradeProgress existingProgress = getOrCreateProgressEntry(rdqPlayer, requirement);

		if (existingProgress.isCompleted()) {
			return new RequirementCompletionResult(false, "requirement.already_completed", getRequirementProgress(player, rdqPlayer, requirement));
		}

		final AbstractRequirement abstractRequirement = requirement.getRequirement().getRequirement();

		if (!abstractRequirement.isMet(player)) {
			return new RequirementCompletionResult(false, "requirement.not_met", getRequirementProgress(player, rdqPlayer, requirement));
		}

		try {
			abstractRequirement.consume(player);
		} catch (final Exception consumeException) {
			LOGGER.severe("Failed to consume resources for requirement %d: %s".formatted(requirement.getId(), consumeException.getMessage()));
			return new RequirementCompletionResult(false, "requirement.consumption_failed", getRequirementProgress(player, rdqPlayer, requirement));
		}

		existingProgress.setProgress(PROGRESS_MAX);
		plugin.getPlayerRankUpgradeProgressRepository().update(existingProgress);
		invalidateProgressCache(player.getUniqueId().toString(), requirement.getId().toString());

		LOGGER.info("Completed requirement %d for player %s".formatted(requirement.getId(), player.getName()));
		return new RequirementCompletionResult(true, "requirement.completed_successfully", getRequirementProgress(player, rdqPlayer, requirement));
	}

	public boolean areAllRequirementsCompleted(final @NotNull Player player, final @NotNull RDQPlayer rdqPlayer, final @NotNull RRank rank) {
		final Set<RRankUpgradeRequirement> requirements = rank.getUpgradeRequirements();
		if (requirements.isEmpty()) return true;

		return requirements.stream()
			.map(requirement -> getRequirementProgress(player, rdqPlayer, requirement))
			.allMatch(RequirementProgressData::isCompleted);
	}

	public double getRankOverallProgress(final @NotNull Player player, final @NotNull RDQPlayer rdqPlayer, final @NotNull RRank rank) {
		final Set<RRankUpgradeRequirement> requirements = rank.getUpgradeRequirements();
		if (requirements.isEmpty()) return PROGRESS_MAX;

		return requirements.stream()
			.mapToDouble(requirement -> getRequirementProgress(player, rdqPlayer, requirement).getProgressPercentage())
			.average()
			.orElse(PROGRESS_MIN);
	}

	public boolean hasCompletedUpgradeRequirement(
		final @NotNull Player player,
		final @NotNull RDQPlayer rdqPlayer,
		final @NotNull RRankUpgradeRequirement requirement
	) {
		return getRequirementProgress(player, rdqPlayer, requirement).isCompleted();
	}

	public void initializeRankProgressTracking(
		final @NotNull RDQPlayer rdqPlayer,
		final @NotNull RRank rank
	) {
		final Set<RRankUpgradeRequirement> requirements = rank.getUpgradeRequirements();
		requirements.forEach(requirement -> getOrCreateProgressEntry(rdqPlayer, requirement));
		LOGGER.info("Initialized progress tracking for %d requirements in rank %s".formatted(requirements.size(), rank.getIdentifier()));
	}

	public void clearPlayerCache(final @NotNull Player player) {
		final String playerPrefix = player.getUniqueId().toString() + ":";
		progressCache.entrySet().removeIf(entry -> entry.getKey().startsWith(playerPrefix));
	}
	
	// Alias method for typo in calling code
	public void cleaRDQPlayerCache(final @NotNull Player player) {
		clearPlayerCache(player);
	}

	public void clearAllCache() {
		progressCache.clear();
	}

	public void refreshRankProgress(
		final @NotNull Player player,
		final @NotNull RDQPlayer rdqPlayer,
		final @NotNull RRank rank
	) {
		rank.getUpgradeRequirements().forEach(requirement -> {
			invalidateProgressCache(player.getUniqueId().toString(), requirement.getId().toString());
			getRequirementProgress(player, rdqPlayer, requirement);
		});
	}

	public void refreshRequirementProgress(
		final @NotNull Player player,
		final @NotNull RDQPlayer rdqPlayer,
		final @NotNull RRankUpgradeRequirement requirement
	) {
		invalidateProgressCache(player.getUniqueId().toString(), requirement.getId().toString());
		getRequirementProgress(player, rdqPlayer, requirement);
	}

	private void invalidateProgressCache(
		final @NotNull String playerId,
		final @NotNull String requirementId
	) {
		progressCache.remove(generateCacheKey(playerId, requirementId));
	}

	private @NotNull RequirementProgressData calculateRequirementProgress(
		final @NotNull Player player,
		final @NotNull RDQPlayer rdqPlayer,
		final @NotNull RRankUpgradeRequirement requirement
	) {
		final RPlayerRankUpgradeProgress dbProgress = getOrCreateProgressEntry(rdqPlayer, requirement);

		return dbProgress.isCompleted() 
			? createCompletedProgressData(requirement)
			: calculateLiveProgress(player, requirement, dbProgress);
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
		final AbstractRequirement abstractRequirement = requirement.getRequirement().getRequirement();
		final boolean isMet = abstractRequirement.isMet(player);
		final double progressPercentage = clampProgress(abstractRequirement.calculateProgress(player));

		final RequirementStatus status;
		if (isMet) {
			status = RequirementStatus.READY_TO_COMPLETE;
		} else if (progressPercentage > PROGRESS_MIN) {
			status = RequirementStatus.IN_PROGRESS;
		} else {
			status = RequirementStatus.NOT_STARTED;
		}

		final String statusMessage = switch (status) {
			case READY_TO_COMPLETE -> "requirement.status.ready_to_complete";
			case IN_PROGRESS -> "requirement.status.in_progress";
			case NOT_STARTED -> "requirement.status.not_started";
			default -> "requirement.status.unknown";
		};

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
		return newProgress;
	}

	private @NotNull String generateCacheKey(
		final @NotNull String playerId,
		final @NotNull String requirementId
	) {
		return playerId + ":" + requirementId;
	}
}*/
