package com.raindropcentral.rdq.manager;


import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.database.entity.rank.RPlayerRankUpgradeProgress;
import com.raindropcentral.rdq.database.entity.rank.RRank;
import com.raindropcentral.rdq.database.entity.rank.RRankUpgradeRequirement;
import com.raindropcentral.rdq.requirement.AbstractRequirement;
import com.raindropcentral.rplatform.logging.CentralLogger;
import de.jexcellence.jextranslate.i18n.I18n;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages rank requirement progress, including persistence, validation, and state management.
 * <p>
 * This manager handles:
 * - Calculating and caching requirement progress
 * - Persisting completion states to the database
 * - Preventing over-completion of requirements
 * - Validating rank completion eligibility
 * - Coordinating between different rank views
 * </p>
 *
 * @author ItsRainingHP
 * @version 1.0.0
 * @since TBD
 */
public class RankRequirementProgressManager {
	
	private static final Logger LOGGER = CentralLogger.getLogger(RankRequirementProgressManager.class.getName());
	
	private final RDQ rdq;
	
	// Cache for requirement progress to avoid repeated database queries
	private final Map<String, RequirementProgressData> progressCache = new ConcurrentHashMap<>();
	private final Map<String, Long> cacheTimestamps = new ConcurrentHashMap<>();
	
	// Cache expiry time (30 seconds)
	private static final long CACHE_EXPIRY_MS = 30000L;
	
	public RankRequirementProgressManager(@NotNull RDQ rdq) {
		this.rdq = rdq;
	}
	
	/**
	 * Gets the current progress for a specific requirement.
	 */
	public @NotNull RequirementProgressData getRequirementProgress(
		@NotNull Player player,
		@NotNull RDQPlayer rdqPlayer,
		@NotNull RRankUpgradeRequirement requirement
	) {
		final String cacheKey = generateCacheKey(player.getUniqueId().toString(), requirement.getId().toString());
		
		// Check cache first
		final RequirementProgressData cached = progressCache.get(cacheKey);
		final Long timestamp = cacheTimestamps.get(cacheKey);
		
		if (cached != null && timestamp != null && (System.currentTimeMillis() - timestamp) < CACHE_EXPIRY_MS) {
			return cached;
		}
		
		// Calculate fresh progress
		final RequirementProgressData freshProgress = calculateRequirementProgress(player, rdqPlayer, requirement);
		
		// Cache the result
		progressCache.put(cacheKey, freshProgress);
		cacheTimestamps.put(cacheKey, System.currentTimeMillis());
		
		return freshProgress;
	}
	
	/**
	 * Attempts to complete a requirement and persists the result.
	 * This method will consume the required resources if the requirement is successfully completed.
	 */
	public @NotNull RequirementCompletionResult attemptRequirementCompletion(
		@NotNull Player player,
		@NotNull RDQPlayer rdqPlayer,
		@NotNull RRankUpgradeRequirement requirement
	) {
		try {
			// Check if requirement is already completed in database
			final RPlayerRankUpgradeProgress existingProgress = getOrCreateProgressEntry(rdqPlayer, requirement);
			
			if (existingProgress.isCompleted()) {
				LOGGER.log(Level.FINE, "Requirement " + requirement.getId() + " already completed for player " + player.getName());
				return new RequirementCompletionResult(
					false,
					"requirement.already_completed",
					getRequirementProgress(player, rdqPlayer, requirement)
				);
			}
			
			// Check if requirement can be completed now
			final AbstractRequirement abstractRequirement = requirement.getRequirement().getRequirement();
			final boolean isMet = abstractRequirement.isMet(player);
			
			if (!isMet) {
				LOGGER.log(Level.FINE, "Requirement " + requirement.getId() + " not yet met for player " + player.getName());
				return new RequirementCompletionResult(
					false,
					"requirement.not_met",
					getRequirementProgress(player, rdqPlayer, requirement)
				);
			}
			
			// IMPORTANT: Consume the resources BEFORE marking as completed
			// This ensures that if consumption fails, we don't mark it as completed
			try {
				LOGGER.log(Level.INFO, "Consuming resources for requirement " + requirement.getId() + " for player " + player.getName());
				abstractRequirement.consume(player);
				LOGGER.log(Level.INFO, "Successfully consumed resources for requirement " + requirement.getId());
			} catch (Exception consumeException) {
				LOGGER.log(Level.SEVERE, "Failed to consume resources for requirement " + requirement.getId() + " for player " + player.getName(), consumeException);
				return new RequirementCompletionResult(
					false,
					"requirement.consumption_failed",
					getRequirementProgress(player, rdqPlayer, requirement)
				);
			}
			
			// Mark the requirement as completed in the database
			existingProgress.setProgress(1.0);
			rdq.getPlayerRankUpgradeProgressRepository().update(existingProgress);
			
			// Clear cache for this requirement
			final String cacheKey = generateCacheKey(player.getUniqueId().toString(), requirement.getId().toString());
			progressCache.remove(cacheKey);
			cacheTimestamps.remove(cacheKey);
			
			LOGGER.log(Level.INFO, "Successfully completed requirement " + requirement.getId() + " for player " + player.getName() + " with resource consumption");
			
			// Return success with fresh progress
			return new RequirementCompletionResult(
				true,
				"requirement.completed_successfully",
				getRequirementProgress(player, rdqPlayer, requirement)
			);
			
		} catch (Exception exception) {
			LOGGER.log(Level.SEVERE, "Failed to complete requirement " + requirement.getId() + " for player " + player.getName(), exception);
			return new RequirementCompletionResult(
				false,
				"requirement.completion_error",
				getRequirementProgress(player, rdqPlayer, requirement)
			);
		}
	}
	
	/**
	 * Checks if all requirements for a rank are completed.
	 */
	public boolean areAllRequirementsCompleted(@NotNull Player player, @NotNull RDQPlayer rdqPlayer, @NotNull RRank rank) {
		try {
			final Set<RRankUpgradeRequirement> requirements = rank.getUpgradeRequirements();
			
			if (requirements.isEmpty()) {
				return true; // No requirements means automatically completed
			}
			
			for (RRankUpgradeRequirement requirement : requirements) {
				final RequirementProgressData progress = getRequirementProgress(player, rdqPlayer, requirement);
				if (!progress.isCompleted()) {
					return false;
				}
			}
			
			return true;
		} catch (Exception exception) {
			LOGGER.log(Level.WARNING, "Failed to check if all requirements are completed for rank " + rank.getIdentifier(), exception);
			return false;
		}
	}
	
	/**
	 * Gets the overall progress percentage for a rank (0.0 to 1.0).
	 */
	public double getRankOverallProgress(@NotNull Player player, @NotNull RDQPlayer rdqPlayer, @NotNull RRank rank) {
		try {
			final Set<RRankUpgradeRequirement> requirements = rank.getUpgradeRequirements();
			
			if (requirements.isEmpty()) {
				return 1.0; // No requirements means 100% complete
			}
			
			double totalProgress = 0.0;
			for (RRankUpgradeRequirement requirement : requirements) {
				final RequirementProgressData progress = getRequirementProgress(player, rdqPlayer, requirement);
				totalProgress += progress.getProgressPercentage();
			}
			
			return totalProgress / requirements.size();
		} catch (Exception exception) {
			LOGGER.log(Level.WARNING, "Failed to calculate overall progress for rank " + rank.getIdentifier(), exception);
			return 0.0;
		}
	}
	
	/**
	 * Initializes progress tracking for all requirements of a rank.
	 */
	public void initializeRankProgressTracking(@NotNull RDQPlayer rdqPlayer, @NotNull RRank rank) {
		try {
			final Set<RRankUpgradeRequirement> requirements = rank.getUpgradeRequirements();
			
			for (RRankUpgradeRequirement requirement : requirements) {
				getOrCreateProgressEntry(rdqPlayer, requirement);
			}
			
			LOGGER.log(Level.INFO, "Initialized progress tracking for " + requirements.size() + " requirements in rank " + rank.getIdentifier());
		} catch (Exception exception) {
			LOGGER.log(Level.SEVERE, "Failed to initialize progress tracking for rank " + rank.getIdentifier(), exception);
		}
	}
	
	/**
	 * Clears the progress cache for a specific player.
	 */
	public void cleaRDQPlayerCache(@NotNull Player player) {
		final String playerPrefix = player.getUniqueId().toString() + ":";
		progressCache.entrySet().removeIf(entry -> entry.getKey().startsWith(playerPrefix));
		cacheTimestamps.entrySet().removeIf(entry -> entry.getKey().startsWith(playerPrefix));
		
		LOGGER.log(Level.FINE, "Cleared progress cache for player " + player.getName());
	}
	
	/**
	 * Clears the entire progress cache.
	 */
	public void clearAllCache() {
		progressCache.clear();
		cacheTimestamps.clear();
		LOGGER.log(Level.FINE, "Cleared all progress cache");
	}
	
	/**
	 * Forces a refresh of all cached progress for a player and rank.
	 */
	public void refreshRankProgress(@NotNull Player player, @NotNull RDQPlayer rdqPlayer, @NotNull RRank rank) {
		try {
			final Set<RRankUpgradeRequirement> requirements = rank.getUpgradeRequirements();
			
			for (RRankUpgradeRequirement requirement : requirements) {
				final String cacheKey = generateCacheKey(player.getUniqueId().toString(), requirement.getId().toString());
				progressCache.remove(cacheKey);
				cacheTimestamps.remove(cacheKey);
				
				// Recalculate and cache
				getRequirementProgress(player, rdqPlayer, requirement);
			}
			
			LOGGER.log(Level.INFO, "Refreshed progress cache for " + requirements.size() + " requirements in rank " + rank.getIdentifier());
		} catch (Exception exception) {
			LOGGER.log(Level.WARNING, "Failed to refresh rank progress for rank " + rank.getIdentifier(), exception);
		}
	}
	
	/**
	 * Refreshes progress for a single requirement.
	 */
	public void refreshRequirementProgress(@NotNull Player player, @NotNull RDQPlayer rdqPlayer, @NotNull RRankUpgradeRequirement requirement) {
		try {
			final String cacheKey = generateCacheKey(player.getUniqueId().toString(), requirement.getId().toString());
			progressCache.remove(cacheKey);
			cacheTimestamps.remove(cacheKey);
			
			// Recalculate and cache
			getRequirementProgress(player, rdqPlayer, requirement);
			
			LOGGER.log(Level.FINE, "Refreshed progress cache for requirement " + requirement.getId());
		} catch (Exception exception) {
			LOGGER.log(Level.WARNING, "Failed to refresh requirement progress for requirement " + requirement.getId(), exception);
		}
	}
	
	// Private helper methods
	
	private @NotNull RequirementProgressData calculateRequirementProgress(
		@NotNull Player player,
		@NotNull RDQPlayer rdqPlayer,
		@NotNull RRankUpgradeRequirement requirement
	) {
		try {
			// Get database progress entry
			final RPlayerRankUpgradeProgress dbProgress = getOrCreateProgressEntry(rdqPlayer, requirement);
			
			// If already marked as completed in database, return completed state
			if (dbProgress.isCompleted()) {
				return new RequirementProgressData(
					requirement.getId().toString(),
					requirement.getRequirement().getRequirement().getType().name(),
					requirement.getRequirement().getRequirement().getDescriptionKey(),
					true,
					1.0,
					RequirementStatus.COMPLETED,
					"requirement.status.completed",
					requirement.getDisplayOrder()
				);
			}
			
			// Calculate current progress from the requirement logic
			final AbstractRequirement abstractRequirement = requirement.getRequirement().getRequirement();
			
			boolean isMet = false;
			double progressPercentage = 0.0;
			RequirementStatus status = RequirementStatus.NOT_STARTED;
			String statusMessage = "requirement.status.not_started";
			
			try {
				isMet = abstractRequirement.isMet(player);
				progressPercentage = abstractRequirement.calculateProgress(player);
				progressPercentage = Math.max(0.0, Math.min(1.0, progressPercentage));
				
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
				false, // Not completed since dbProgress.isCompleted() was false
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
	
	private @NotNull RPlayerRankUpgradeProgress getOrCreateProgressEntry(
		@NotNull RDQPlayer rdqPlayer,
		@NotNull RRankUpgradeRequirement requirement
	) {
		try {
			// Try to find existing progress entry
			final List<RPlayerRankUpgradeProgress> existingProgress = rdq.getPlayerRankUpgradeProgressRepository()
			                                                                .findAllByAttributes(Map.of(
				                                                                "player.uniqueId", rdqPlayer.getUniqueId(),
				                                                                "upgradeRequirement.id", requirement.getId()
			                                                                ));
			
			if (!existingProgress.isEmpty()) {
				return existingProgress.get(0);
			}
			
			// Create new progress entry
			final RPlayerRankUpgradeProgress newProgress = new RPlayerRankUpgradeProgress(rdqPlayer, requirement);
			rdq.getPlayerRankUpgradeProgressRepository().create(newProgress);
			
			LOGGER.log(Level.FINE, "Created new progress entry for requirement " + requirement.getId() + " and player " + rdqPlayer.getPlayerName());
			return newProgress;
			
		} catch (Exception exception) {
			LOGGER.log(Level.SEVERE, "Failed to get or create progress entry", exception);
			throw new RuntimeException("Database operation failed", exception);
		}
	}
	
	private @NotNull String generateCacheKey(@NotNull String playerId, @NotNull String requirementId) {
		return playerId + ":" + requirementId;
	}
	
	// Data classes and enums
	
	public enum RequirementStatus {
		NOT_STARTED,
		IN_PROGRESS,
		READY_TO_COMPLETE,
		COMPLETED,
		ERROR
	}
	
	public static class RequirementProgressData {
		private final String requirementId;
		private final String requirementType;
		private final String descriptionKey;
		private final boolean isCompleted;
		private final double progressPercentage;
		private final RequirementStatus status;
		private final String statusMessage;
		private final int displayOrder;
		
		public RequirementProgressData(
			@NotNull String requirementId,
			@NotNull String requirementType,
			@NotNull String descriptionKey,
			boolean isCompleted,
			double progressPercentage,
			@NotNull RequirementStatus status,
			@NotNull String statusMessage,
			int displayOrder
		) {
			this.requirementId = requirementId;
			this.requirementType = requirementType;
			this.descriptionKey = descriptionKey;
			this.isCompleted = isCompleted;
			this.progressPercentage = progressPercentage;
			this.status = status;
			this.statusMessage = statusMessage;
			this.displayOrder = displayOrder;
		}
		
		// Getters
		public String getRequirementId() { return requirementId; }
		public String getRequirementType() { return requirementType; }
		public String getDescriptionKey() { return descriptionKey; }
		public boolean isCompleted() { return isCompleted; }
		public double getProgressPercentage() { return progressPercentage; }
		public RequirementStatus getStatus() { return status; }
		public String getStatusMessage() { return statusMessage; }
		public int getDisplayOrder() { return displayOrder; }
		
		public int getProgressAsPercentage() {
			return (int) Math.round(progressPercentage * 100);
		}
		
		public boolean hasProgress() {
			return progressPercentage > 0.0;
		}
		
		public String getFormattedProgress() {
			return getProgressAsPercentage() + "%";
		}
		
		public boolean canBeCompleted() {
			return status == RequirementStatus.READY_TO_COMPLETE && !isCompleted;
		}
		
		@Override
		public String toString() {
			return "RequirementProgressData{" +
			       "id='" + requirementId + '\'' +
			       ", type='" + requirementType + '\'' +
			       ", completed=" + isCompleted +
			       ", progress=" + getProgressAsPercentage() + "%" +
			       ", status=" + status +
			       '}';
		}
	}
	
	public static class RequirementCompletionResult {
		private final boolean success;
		private final String messageKey;
		private final RequirementProgressData updatedProgress;
		
		public RequirementCompletionResult(
			boolean success,
			@NotNull String messageKey,
			@NotNull RequirementProgressData updatedProgress
		) {
			this.success = success;
			this.messageKey = messageKey;
			this.updatedProgress = updatedProgress;
		}
		
		public boolean isSuccess() { return success; }
		public String getMessageKey() { return messageKey; }
		public RequirementProgressData getUpdatedProgress() { return updatedProgress; }
		
		public void sendMessage(@NotNull Player player) {
            new I18n.Builder(messageKey, player).includePrefix().build().sendMessage();
		}
	}
}