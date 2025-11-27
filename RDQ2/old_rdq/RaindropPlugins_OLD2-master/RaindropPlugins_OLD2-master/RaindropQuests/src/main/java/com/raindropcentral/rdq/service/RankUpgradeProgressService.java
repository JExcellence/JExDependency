package com.raindropcentral.rdq.service;

import com.raindropcentral.rdq.RDQImpl;
import com.raindropcentral.rdq.database.entity.RDQPlayer;
import com.raindropcentral.rdq.database.entity.rank.RPlayerRankUpgradeProgress;
import com.raindropcentral.rdq.database.entity.rank.RRank;
import com.raindropcentral.rdq.database.entity.rank.RRankUpgradeRequirement;
import com.raindropcentral.rplatform.logger.CentralLogger;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Enhanced service for managing player rank upgrade progress with improved
 * async handling and better error recovery.
 *
 * @author JExcellence
 * @version 2.0.0
 * @since TBD
 */
public class RankUpgradeProgressService {
	
	private static final Logger LOGGER = CentralLogger.getLogger(RankUpgradeProgressService.class.getName());
	
	private final RDQImpl rdq;
	
	public RankUpgradeProgressService(final @NotNull RDQImpl rdq) {
		this.rdq = rdq;
	}
	
	/**
	 * Initializes progress tracking for a player working towards a specific rank.
	 * Creates progress entries for all upgrade requirements for this rank.
	 *
	 * @param player the player to initialize progress for
	 * @param targetRank the rank the player is working towards
	 */
	public void initializeProgressForRank(
		final @NotNull RDQPlayer player,
		final @NotNull RRank targetRank
	) {
		try {
			final Set<RRankUpgradeRequirement> upgradeRequirements = targetRank.getUpgradeRequirements();
			
			if (upgradeRequirements.isEmpty()) {
				LOGGER.log(Level.FINE, "No upgrade requirements found for rank " + targetRank.getIdentifier());
				return;
			}
			
			for (final RRankUpgradeRequirement upgradeRequirement : upgradeRequirements) {
				this.initializeProgressForRequirement(player, upgradeRequirement);
			}
			
			LOGGER.log(Level.INFO, "Initialized progress tracking for " + upgradeRequirements.size() +
			                       " requirements for rank " + targetRank.getIdentifier());
			
		} catch (final Exception exception) {
			LOGGER.log(Level.SEVERE, "Failed to initialize progress for rank " + targetRank.getIdentifier(), exception);
		}
	}
	
	/**
	 * Initializes progress for a single requirement if it doesn't already exist.
	 */
	private void initializeProgressForRequirement(
		final @NotNull RDQPlayer player,
		final @NotNull RRankUpgradeRequirement upgradeRequirement
	) {
		try {
			final List<RPlayerRankUpgradeProgress> existingProgress = this.rdq.getPlayerRankUpgradeProgressRepository()
			                                                                  .findListByAttributes(Map.of(
				                                                                  "player.uniqueId", player.getUniqueId(),
				                                                                  "upgradeRequirement.id", upgradeRequirement.getId()
			                                                                  ));
			
			if (existingProgress.isEmpty()) {
				final RPlayerRankUpgradeProgress newProgress = new RPlayerRankUpgradeProgress(player, upgradeRequirement);
				this.rdq.getPlayerRankUpgradeProgressRepository().create(newProgress);
				LOGGER.log(Level.FINE, "Created progress tracking for requirement " + upgradeRequirement.getId());
			} else {
				LOGGER.log(Level.FINE, "Progress tracking already exists for requirement " + upgradeRequirement.getId());
			}
			
		} catch (final Exception exception) {
			LOGGER.log(Level.WARNING, "Failed to initialize progress for requirement " + upgradeRequirement.getId(), exception);
		}
	}
	
	/**
	 * Gets all progress entries for a player working towards a specific rank.
	 *
	 * @param player the player
	 * @param targetRank the target rank
	 * @return list of progress entries
	 */
	public List<RPlayerRankUpgradeProgress> getProgressForRank(
		final @NotNull RDQPlayer player,
		final @NotNull RRank targetRank
	) {
		try {
			final Set<RRankUpgradeRequirement> upgradeRequirements = targetRank.getUpgradeRequirements();
			
			return this.rdq.getPlayerRankUpgradeProgressRepository()
			               .findListByAttributes(Map.of("player.uniqueId", player.getUniqueId()))
			               .stream()
			               .filter(progress -> upgradeRequirements.contains(progress.getUpgradeRequirement()))
			               .toList();
			
		} catch (final Exception exception) {
			LOGGER.log(Level.WARNING, "Failed to get progress for rank " + targetRank.getIdentifier(), exception);
			return List.of();
		}
	}
	
	/**
	 * Checks if a player has completed all upgrade requirements for a rank.
	 * A player completes a rank upgrade if they complete ALL upgrade requirements.
	 *
	 * @param player the player to check
	 * @param targetRank the rank to check completion for
	 * @return true if all upgrade requirements are completed
	 */
	public boolean hasCompletedAllUpgradeRequirements(
		final @NotNull RDQPlayer player,
		final @NotNull RRank targetRank
	) {
		try {
			final Set<RRankUpgradeRequirement> upgradeRequirements = targetRank.getUpgradeRequirements();
			
			if (upgradeRequirements.isEmpty()) {
				return true;
			}
			
			for (final RRankUpgradeRequirement upgradeRequirement : upgradeRequirements) {
				if (!this.hasCompletedUpgradeRequirement(player, upgradeRequirement)) {
					return false;
				}
			}
			
			return true;
			
		} catch (final Exception exception) {
			LOGGER.log(Level.WARNING, "Failed to check completion for rank " + targetRank.getIdentifier(), exception);
			return false;
		}
	}
	
	/**
	 * Checks if a player has completed a specific upgrade requirement.
	 *
	 * @param player the player to check
	 * @param upgradeRequirement the specific upgrade requirement
	 * @return true if this upgrade requirement is completed
	 */
	public boolean hasCompletedUpgradeRequirement(
		final @NotNull RDQPlayer player,
		final @NotNull RRankUpgradeRequirement upgradeRequirement
	) {
		try {
			final List<RPlayerRankUpgradeProgress> progressList = this.rdq.getPlayerRankUpgradeProgressRepository()
			                                                              .findListByAttributes(Map.of(
				                                                              "player.uniqueId", player.getUniqueId(),
				                                                              "upgradeRequirement.id", upgradeRequirement.getId()
			                                                              ));
			
			return !progressList.isEmpty() && progressList.get(0).isCompleted();
			
		} catch (final Exception exception) {
			LOGGER.log(Level.WARNING, "Failed to check completion for requirement " + upgradeRequirement.getId(), exception);
			return false;
		}
	}
	
	/**
	 * Updates progress for a specific upgrade requirement.
	 *
	 * @param player the player
	 * @param upgradeRequirement the upgrade requirement to update
	 * @param newProgress the new progress value
	 */
	public void updateProgress(
		final @NotNull RDQPlayer player,
		final @NotNull RRankUpgradeRequirement upgradeRequirement,
		final double newProgress
	) {
		try {
			RPlayerRankUpgradeProgress progress = this.getOrCreateProgress(player, upgradeRequirement);
			
			final double oldProgress = progress.getProgress();
			progress.setProgress(newProgress);
			
			this.rdq.getPlayerRankUpgradeProgressRepository().update(progress);
			
			LOGGER.log(Level.FINE, "Updated progress for requirement " + upgradeRequirement.getId() +
			                       " from " + oldProgress + " to " + newProgress);
			
		} catch (final Exception exception) {
			LOGGER.log(Level.SEVERE, "Failed to update progress for requirement " + upgradeRequirement.getId(), exception);
		}
	}
	
	/**
	 * Updates progress for a specific upgrade requirement by increment.
	 *
	 * @param player the player
	 * @param upgradeRequirement the upgrade requirement to update
	 * @param incrementAmount the amount to increment progress by
	 */
	public void incrementProgress(
		final @NotNull RDQPlayer player,
		final @NotNull RRankUpgradeRequirement upgradeRequirement,
		final double incrementAmount
	) {
		try {
			RPlayerRankUpgradeProgress progress = this.getOrCreateProgress(player, upgradeRequirement);
			
			final double oldProgress = progress.getProgress();
			final double newProgress = progress.incrementProgress(incrementAmount);
			
			this.rdq.getPlayerRankUpgradeProgressRepository().update(progress);
			
			LOGGER.log(Level.FINE, "Incremented progress for requirement " + upgradeRequirement.getId() +
			                       " from " + oldProgress + " to " + newProgress + " (+" + incrementAmount + ")");
			
		} catch (final Exception exception) {
			LOGGER.log(Level.SEVERE, "Failed to increment progress for requirement " + upgradeRequirement.getId(), exception);
		}
	}
	
	/**
	 * Gets or creates progress entry for a requirement.
	 */
	private RPlayerRankUpgradeProgress getOrCreateProgress(
		final @NotNull RDQPlayer player,
		final @NotNull RRankUpgradeRequirement upgradeRequirement
	) {
		final List<RPlayerRankUpgradeProgress> progressList = this.rdq.getPlayerRankUpgradeProgressRepository()
		                                                              .findListByAttributes(Map.of(
			                                                              "player.uniqueId", player.getUniqueId(),
			                                                              "upgradeRequirement.id", upgradeRequirement.getId()
		                                                              ));
		
		if (!progressList.isEmpty()) {
			return progressList.get(0);
		}
		
		final RPlayerRankUpgradeProgress newProgress = new RPlayerRankUpgradeProgress(player, upgradeRequirement);
		this.rdq.getPlayerRankUpgradeProgressRepository().create(newProgress);
		return newProgress;
	}
	
	/**
	 * Gets the progress for a specific upgrade requirement.
	 *
	 * @param player the player
	 * @param upgradeRequirement the upgrade requirement
	 * @return the progress entry, or null if not found
	 */
	public RPlayerRankUpgradeProgress getProgressForRequirement(
		final @NotNull RDQPlayer player,
		final @NotNull RRankUpgradeRequirement upgradeRequirement
	) {
		try {
			final List<RPlayerRankUpgradeProgress> progressList = this.rdq.getPlayerRankUpgradeProgressRepository()
			                                                              .findListByAttributes(Map.of(
				                                                              "player.uniqueId", player.getUniqueId(),
				                                                              "upgradeRequirement.id", upgradeRequirement.getId()
			                                                              ));
			
			return progressList.isEmpty() ? null : progressList.get(0);
			
		} catch (final Exception exception) {
			LOGGER.log(Level.WARNING, "Failed to get progress for requirement " + upgradeRequirement.getId(), exception);
			return null;
		}
	}
	
	/**
	 * Clears all progress for a player towards a specific rank.
	 *
	 * @param player the player
	 * @param targetRank the rank to clear progress for
	 */
	public void clearProgressForRank(
		final @NotNull RDQPlayer player,
		final @NotNull RRank targetRank
	) {
		try {
			final List<RPlayerRankUpgradeProgress> progressEntries = this.getProgressForRank(player, targetRank);
			
			for (final RPlayerRankUpgradeProgress progress : progressEntries) {
				this.rdq.getPlayerRankUpgradeProgressRepository().delete(progress.getId());
			}
			
			LOGGER.log(Level.INFO, "Cleared " + progressEntries.size() + " progress entries for rank " + targetRank.getIdentifier());
			
		} catch (final Exception exception) {
			LOGGER.log(Level.WARNING, "Failed to clear progress for rank " + targetRank.getIdentifier(), exception);
		}
	}
	
	/**
	 * Resets progress for a specific upgrade requirement.
	 *
	 * @param player the player
	 * @param upgradeRequirement the upgrade requirement to reset
	 */
	public void resetProgressForRequirement(
		final @NotNull RDQPlayer player,
		final @NotNull RRankUpgradeRequirement upgradeRequirement
	) {
		try {
			final RPlayerRankUpgradeProgress progress = this.getProgressForRequirement(player, upgradeRequirement);
			
			if (progress != null) {
				progress.resetProgress();
				this.rdq.getPlayerRankUpgradeProgressRepository().update(progress);
				LOGGER.log(Level.FINE, "Reset progress for requirement " + upgradeRequirement.getId());
			}
			
		} catch (final Exception exception) {
			LOGGER.log(Level.WARNING, "Failed to reset progress for requirement " + upgradeRequirement.getId(), exception);
		}
	}
	
	/**
	 * Gets the overall completion percentage for a rank.
	 *
	 * @param player the player
	 * @param targetRank the target rank
	 * @return completion percentage (0.0 to 1.0)
	 */
	public double getOverallCompletionPercentage(
		final @NotNull RDQPlayer player,
		final @NotNull RRank targetRank
	) {
		try {
			final Set<RRankUpgradeRequirement> upgradeRequirements = targetRank.getUpgradeRequirements();
			
			if (upgradeRequirements.isEmpty()) {
				return 1.0;
			}
			
			double totalProgress = 0.0;
			
			for (final RRankUpgradeRequirement upgradeRequirement : upgradeRequirements) {
				final RPlayerRankUpgradeProgress progress = this.getProgressForRequirement(player, upgradeRequirement);
				
				if (progress != null) {
					totalProgress += progress.getProgress();
				}
			}
			
			return totalProgress / upgradeRequirements.size();
			
		} catch (final Exception exception) {
			LOGGER.log(Level.WARNING, "Failed to calculate completion percentage for rank " + targetRank.getIdentifier(), exception);
			return 0.0;
		}
	}
	
	/**
	 * Async version of initializeProgressForRank for better performance.
	 */
	public CompletableFuture<Void> initializeProgressForRankAsync(
		final @NotNull RDQPlayer player,
		final @NotNull RRank targetRank
	) {
		return CompletableFuture.runAsync(() -> this.initializeProgressForRank(player, targetRank), this.rdq.getExecutor());
	}
	
	/**
	 * Async version of updateProgress for better performance.
	 */
	public CompletableFuture<Void> updateProgressAsync(
		final @NotNull RDQPlayer player,
		final @NotNull RRankUpgradeRequirement upgradeRequirement,
		final double newProgress
	) {
		return CompletableFuture.runAsync(() -> this.updateProgress(player, upgradeRequirement, newProgress), this.rdq.getExecutor());
	}
}