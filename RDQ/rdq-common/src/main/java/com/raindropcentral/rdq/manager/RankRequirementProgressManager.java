package com.raindropcentral.rdq.manager;


import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.database.entity.rank.RPlayerRankUpgradeProgress;
import com.raindropcentral.rdq.database.entity.rank.RRank;
import com.raindropcentral.rdq.database.entity.rank.RRankUpgradeRequirement;
import com.raindropcentral.rplatform.requirement.AbstractRequirement;
import com.raindropcentral.rplatform.requirement.RequirementService;
import de.jexcellence.jextranslate.i18n.I18n;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.Logger;

/**
 * Manages rank requirement progress, including persistence, validation, and state management.
 *
 * <p>This manager handles:
 * - Calculating and caching requirement progress (via RequirementService)
 * - Persisting completion states to the database
 * - Preventing over-completion of requirements
 * - Validating rank completion eligibility
 * - Coordinating between different rank views
 *
 * <p><b>IMPORTANT:</b> This manager now uses {@link RequirementService} instead of calling
 * requirement methods directly. This ensures that requirement events are properly fired
 * and the rank progression system integrates with the event-driven architecture.
 *
 * @author ItsRainingHP
 * @version 2.0.0
 * @since TBD
 */
public class RankRequirementProgressManager {
	
	private static final Logger LOGGER = Logger.getLogger(RankRequirementProgressManager.class.getName());
	
	private final RDQ rdq;
	private final RequirementService requirementService;
	
	// Cache for requirement progress to avoid repeated database queries
	private final Map<String, RequirementProgressData> progressCache = new ConcurrentHashMap<>();
	private final Map<String, Long> cacheTimestamps = new ConcurrentHashMap<>();
	
	// Cache expiry time (30 seconds)
	private static final long CACHE_EXPIRY_MS = 30000L;
	
	/**
	 * Executes RankRequirementProgressManager.
	 */
	public RankRequirementProgressManager(@NotNull RDQ rdq) {
		this.rdq = rdq;
		this.requirementService = RequirementService.getInstance();
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
 *
 * <p>This method now uses {@link RequirementService#isMet(Player, AbstractRequirement)}
	 * and {@link RequirementService#consume(Player, AbstractRequirement)} to ensure
	 * that requirement events are properly fired throughout the completion process.
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
			
			// Check if requirement can be completed now using RequirementService (fires events!)
			final AbstractRequirement abstractRequirement = requirement.getRequirement().getRequirement();
			final boolean isMet = requirementService.isMet(player, abstractRequirement);
			
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
			// Use RequirementService to fire consume events!
			try {
				LOGGER.log(Level.INFO, "Consuming resources for requirement " + requirement.getId() + " for player " + player.getName());
				requirementService.consume(player, abstractRequirement);
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
	
	/**
	 * Calculates the current progress for a specific requirement.
 *
 * <p>This method now uses {@link RequirementService#checkRequirement(Player, AbstractRequirement)}
	 * instead of calling requirement methods directly. This ensures that:
	 * - RequirementCheckEvent is fired
	 * - Progress is automatically tracked by RankRequirementListener
	 * - The system integrates with the event-driven architecture
	 */
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
					requirement.getRequirement().getRequirement().getTypeId(),
					requirement.getRequirement().getRequirement().getDescriptionKey(),
					true,
					1.0,
					RequirementStatus.COMPLETED,
					"requirement.status.completed",
					requirement.getDisplayOrder()
				);
			}
			
			// Calculate current progress using RequirementService (fires events!)
			final AbstractRequirement abstractRequirement = requirement.getRequirement().getRequirement();
			
			boolean isMet = false;
			double progressPercentage = 0.0;
			RequirementStatus status = RequirementStatus.NOT_STARTED;
			String statusMessage = "requirement.status.not_started";
			
			try {
				// Use RequirementService instead of direct calls - this fires events!
				isMet = requirementService.isMet(player, abstractRequirement);
				progressPercentage = requirementService.calculateProgress(player, abstractRequirement);
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
				abstractRequirement.getTypeId(),
				abstractRequirement.getDescriptionKey(),
				false, // Not completed since dbProgress.isCompleted() was false
				progressPercentage,
				status,
				statusMessage,
				requirement.getDisplayOrder()
			);
			
		} catch (IllegalStateException e) {
			// Requirement no longer exists - this is expected when requirements are updated
			LOGGER.log(Level.WARNING, "Requirement " + requirement.getId() + " no longer exists in database. " +
				"This is normal after requirement updates. Returning error state.");
			
			return new RequirementProgressData(
				requirement.getId().toString(),
				"DELETED",
				"requirement.error.deleted",
				false,
				0.0,
				RequirementStatus.ERROR,
				"requirement.status.deleted",
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
			// Validate that the requirement exists and has a valid ID
			if (requirement.getId() == null) {
				throw new IllegalArgumentException("Requirement has no ID");
			}
			
			// Try to find existing progress entry
			final List<RPlayerRankUpgradeProgress> existingProgress = rdq.getPlayerRankUpgradeProgressRepository()
			                                                                .findAllByAttributes(Map.of(
				                                                                "player.uniqueId", rdqPlayer.getUniqueId(),
				                                                                "upgradeRequirement.id", requirement.getId()
			                                                                ));
			
			if (!existingProgress.isEmpty()) {
				return existingProgress.get(0);
			}
			
			// Verify the requirement actually exists in the database before creating progress
			// This prevents foreign key violations when requirements have been deleted
			RRank rank = requirement.getRank();
			if (rank != null) {
				RRank freshRank = rdq.getRankRepository().findById(rank.getId()).orElse(null);
				if (freshRank != null) {
					boolean requirementExists = freshRank.getUpgradeRequirements().stream()
						.anyMatch(req -> req.getId() != null && req.getId().equals(requirement.getId()));
					
					if (!requirementExists) {
						LOGGER.warning("Attempted to create progress for non-existent requirement ID: " + requirement.getId() + 
							" in rank: " + rank.getIdentifier() + ". This requirement may have been deleted.");
						throw new IllegalStateException("Requirement no longer exists in database");
					}
				}
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
	
	/**
	 * Represents the RequirementStatus API type.
	 */
	public enum RequirementStatus {
		NOT_STARTED,
		IN_PROGRESS,
		READY_TO_COMPLETE,
		COMPLETED,
		ERROR
	}
	
	/**
	 * Snapshot of requirement progress information for presentation and workflow control.
	 */
	public static class RequirementProgressData {
		private final String requirementId;
		private final String requirementType;
		private final String descriptionKey;
		private final boolean isCompleted;
		private final double progressPercentage;
		private final RequirementStatus status;
		private final String statusMessage;
		private final int displayOrder;
		
		/**
		 * Executes RequirementProgressData.
		 */
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
		/**
		 * Gets requirementId.
		 */
		public String getRequirementId() { return requirementId; }
		/**
		 * Gets requirementType.
		 */
		public String getRequirementType() { return requirementType; }
		/**
		 * Gets descriptionKey.
		 */
		public String getDescriptionKey() { return descriptionKey; }
		/**
		 * Returns whether completed.
		 */
		public boolean isCompleted() { return isCompleted; }
		/**
		 * Gets progressPercentage.
		 */
		public double getProgressPercentage() { return progressPercentage; }
		/**
		 * Gets status.
		 */
		public RequirementStatus getStatus() { return status; }
		/**
		 * Gets statusMessage.
		 */
		public String getStatusMessage() { return statusMessage; }
		/**
		 * Gets displayOrder.
		 */
		public int getDisplayOrder() { return displayOrder; }
		
		/**
		 * Gets progressAsPercentage.
		 */
		public int getProgressAsPercentage() {
			return (int) Math.round(progressPercentage * 100);
		}
		
		/**
		 * Returns whether progress.
		 */
		public boolean hasProgress() {
			return progressPercentage > 0.0;
		}
		
		/**
		 * Gets formattedProgress.
		 */
		public String getFormattedProgress() {
			return getProgressAsPercentage() + "%";
		}
		
		/**
		 * Executes canBeCompleted.
		 */
		public boolean canBeCompleted() {
			return status == RequirementStatus.READY_TO_COMPLETE && !isCompleted;
		}
		
		/**
		 * Executes toString.
		 */
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
	
	/**
	 * Outcome of attempting to complete a requirement.
	 */
	public static class RequirementCompletionResult {
		private final boolean success;
		private final String messageKey;
		private final RequirementProgressData updatedProgress;
		
		/**
		 * Executes RequirementCompletionResult.
		 */
		public RequirementCompletionResult(
			boolean success,
			@NotNull String messageKey,
			@NotNull RequirementProgressData updatedProgress
		) {
			this.success = success;
			this.messageKey = messageKey;
			this.updatedProgress = updatedProgress;
		}
		
		/**
		 * Returns whether success.
		 */
		public boolean isSuccess() { return success; }
		/**
		 * Gets messageKey.
		 */
		public String getMessageKey() { return messageKey; }
		/**
		 * Gets updatedProgress.
		 */
		public RequirementProgressData getUpdatedProgress() { return updatedProgress; }
		
		/**
		 * Executes sendMessage.
		 */
		public void sendMessage(@NotNull Player player) {
            new I18n.Builder(messageKey, player).includePrefix().build().sendMessage();
		}
	}
}
