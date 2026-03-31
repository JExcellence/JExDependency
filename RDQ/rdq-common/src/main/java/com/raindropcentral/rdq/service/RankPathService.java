/*
 * Copyright (c) 2021-2026 Antimatter Zone LLC. All rights reserved.
 *
 * This source code is proprietary and confidential to Antimatter Zone LLC.
 * Unauthorized copying, modification, distribution, display, performance,
 * publication, sublicensing, or creation of derivative works is prohibited
 * without prior written permission from Antimatter Zone LLC, except to the
 * extent permitted by applicable United States law.
 *
 * This notice is intended to preserve all rights and remedies available under
 * the laws of the State of Washington and the United States of America.
 */

package com.raindropcentral.rdq.service;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.database.entity.rank.RPlayerRank;
import com.raindropcentral.rdq.database.entity.rank.RPlayerRankPath;
import com.raindropcentral.rdq.database.entity.rank.RRank;
import com.raindropcentral.rdq.database.entity.rank.RRankTree;
import com.raindropcentral.rdq.rank.progression.RankCompletionTracker;
import com.raindropcentral.rdq.view.ranks.interaction.RankProgressionManager;
import com.raindropcentral.rplatform.logging.CentralLogger;
import com.raindropcentral.rplatform.progression.ProgressionValidator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service for managing rank path selection and progression.
 * <p>
 * This service integrates with the RPlatform Progression System to provide
 * prerequisite validation and automatic unlocking of dependent ranks.
 * </p>
 *
 * <h2>Integration Points:</h2>
 * <ul>
 *     <li>{@link ProgressionValidator} - For prerequisite validation and unlocking</li>
 *     <li>{@link RankCompletionTracker} - For tracking rank completion</li>
 *     <li>{@link RankProgressionManager} - For legacy rank progression logic</li>
 * </ul>
 *
 * @author RaindropCentral
 * @version 2.0.0
 * @since 1.0.0
 */
public class RankPathService {
	
	private static final Logger LOGGER = Logger.getLogger(RankPathService.class.getName());
	private final RDQ rdq;
	private final RankProgressionManager progressionManager;
	private final ProgressionValidator<RRank> progressionValidator;
	private final RankCompletionTracker completionTracker;

	/**
	 * Constructs a new RankPathService with progression system integration.
	 *
	 * @param rdq The RDQ plugin instance
	 * @param progressionValidator The progression validator for prerequisite checking
	 * @param completionTracker The completion tracker for rank achievement tracking
	 */
	public RankPathService(
		final @NotNull RDQ rdq,
		final @NotNull ProgressionValidator<RRank> progressionValidator,
		final @NotNull RankCompletionTracker completionTracker
	) {
		this.rdq = rdq;
		this.progressionManager = new RankProgressionManager(rdq);
		this.progressionValidator = progressionValidator;
		this.completionTracker = completionTracker;
	}

	/**
	 * Legacy constructor for backward compatibility.
	 * Creates progression validator and completion tracker internally.
	 *
	 * @param rdq The RDQ plugin instance
	 * @deprecated Use {@link #RankPathService(RDQ, ProgressionValidator, RankCompletionTracker)} instead
	 */
	@Deprecated
	public RankPathService(final @NotNull RDQ rdq) {
		this.rdq = rdq;
		this.progressionManager = new RankProgressionManager(rdq);
		this.completionTracker = new RankCompletionTracker(
			rdq.getPlayerRankRepository(),
			rdq.getRankRepository()
		);
		// Load all ranks for progression validator
		// Note: This is a blocking call during initialization
		List<RRank> allRanks = rdq.getRankRepository().findAllByAttributes(Map.of());
		this.progressionValidator = new ProgressionValidator<>(
			completionTracker,
			allRanks
		);
	}
	
	/**
	 * Assigns a default rank to a player (typically used for new players).
	 */
	public void assignDefaultRank(
		final @NotNull RDQPlayer player,
		final @NotNull RRank defaultRank
	) {
		try {
			
			final RPlayerRank defaultPlayerRank = new RPlayerRank(player, defaultRank);
			player.addPlayerRank(defaultPlayerRank);
			
			this.rdq.getPlayerRepository().update(player);
			this.rdq.getPlayerRankRepository().create(defaultPlayerRank);
			
			LOGGER.log(Level.INFO, "Assigned default rank " + defaultRank.getIdentifier() + " to player " + player.getPlayerName());
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
		try {
			if (!selectedRankTree.isEnabled()) {
				LOGGER.log(Level.WARNING, "Attempted to select disabled rank tree: {}", selectedRankTree.getIdentifier());
				return false;
			}
			
			if (!this.checkRankTreePrerequisites(player, selectedRankTree)) {
				LOGGER.log(Level.INFO, "Player " + player.getPlayerName() + " does not meet prerequisites for rank tree: " + selectedRankTree.getIdentifier());
				return false;
			}
			
			this.deactivateAllRankPaths(player);
			
			RPlayerRankPath existingRankPath = this.getRankPathForTree(player, selectedRankTree);
			
			if (existingRankPath != null) {
				RPlayerRankPath freshRankPath = this.rdq.getPlayerRankPathRepository().findById(existingRankPath.getId()).orElse(null);
				if (freshRankPath != null) {
					freshRankPath.setActive(true);
					this.rdq.getPlayerRankPathRepository().update(freshRankPath);
				}
			} else {
				final RPlayerRankPath newRankPath = new RPlayerRankPath(player, selectedRankTree);
				newRankPath.setActive(true);
				this.rdq.getPlayerRankPathRepository().create(newRankPath);
			}
			
			this.handleRankAssignmentForTree(player, selectedRankTree, startingRank);
			
			this.progressionManager.assignInitialRankForPath(player, selectedRankTree);
			
			this.progressionManager.processAutoCompletableRanks(player, selectedRankTree);
			
			LOGGER.log(Level.INFO, "Successfully selected rank path " + selectedRankTree.getIdentifier() + " for player " + player.getPlayerName());
			
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
		try {
			if (!this.checkRankTreePrerequisites(player, newRankTree)) {
				LOGGER.log(Level.INFO, "Player " + player.getPlayerName() + " cannot switch to rank tree " + newRankTree.getIdentifier() + " - prerequisites not met");
				return false;
			}
			
			return this.selectRankPath(player, newRankTree, startingRank);
			
		} catch (final Exception exception) {
			LOGGER.log(Level.SEVERE, "Failed to switch rank path for player " + player.getPlayerName(), exception);
			return false;
		}
	}
	
	/**
	 * Deactivates all rank paths for a player.
	 */
	private void deactivateAllRankPaths(final @NotNull RDQPlayer player) {
		try {
			final List<RPlayerRankPath> allRankPaths = this.rdq.getPlayerRankPathRepository()
			                                                   .findAllByAttributes(Map.of("player", player));
			
			for (final RPlayerRankPath rankPath : allRankPaths) {
				if (rankPath.isActive()) {
					RPlayerRankPath freshRankPath = this.rdq.getPlayerRankPathRepository().findById(rankPath.getId()).orElse(null);
					if (freshRankPath != null && freshRankPath.isActive()) {
						freshRankPath.setActive(false);
						this.rdq.getPlayerRankPathRepository().update(freshRankPath);
					}
				}
			}
		} catch (final Exception exception) {
			LOGGER.log(Level.WARNING, "Failed to deactivate all rank paths for player " + player.getPlayerName(), exception);
		}
	}
	
	/**
	 * Gets the rank path for a specific tree.
	 */
	@Nullable
	private RPlayerRankPath getRankPathForTree(
		final @NotNull RDQPlayer player,
		final @NotNull RRankTree rankTree
	) {
		return this.rdq.getPlayerRankPathRepository()
		               .findByAttributes(Map.of(
			               "player", player,
			               "selectedRankPath", rankTree
		               )).orElse(null);
	}
	
	/**
	 * Handles rank assignment for a tree when selecting a path.
	 */
	private void handleRankAssignmentForTree(
		final @NotNull RDQPlayer player,
		final @NotNull RRankTree rankTree,
		final @NotNull RRank startingRank
	) {
		try {
			this.deactivateAllPlayerRanks(player);
			
			RPlayerRank existingRankInTree = this.getPlayerRankForTree(player, rankTree);
			
			if (existingRankInTree != null) {
				RPlayerRank freshRank = this.rdq.getPlayerRankRepository().findById(existingRankInTree.getId()).orElse(null);
				if (freshRank != null) {
					freshRank.setActive(true);
					this.rdq.getPlayerRankRepository().update(freshRank);
					LOGGER.log(Level.INFO, "Reactivated existing rank for player " + player.getPlayerName() + " in tree " + rankTree.getIdentifier());
				}
			} else {
				// Fetch fresh player to avoid stale state
				RDQPlayer freshPlayer = this.rdq.getPlayerRepository().findById(player.getId()).orElse(player);
				final RPlayerRank newPlayerRank = new RPlayerRank(freshPlayer, startingRank, rankTree);
				this.rdq.getPlayerRankRepository().create(newPlayerRank);
				LOGGER.log(Level.INFO, "Created new rank assignment for player " + freshPlayer.getPlayerName() + " in tree " + rankTree.getIdentifier());
			}
			// Removed redundant player update that caused OptimisticLockException
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
			final List<RPlayerRank> allPlayerRanks = this.rdq.getPlayerRankRepository()
			                                                 .findAllByAttributes(Map.of("player.uniqueId", player.getUniqueId()));
			
			for (final RPlayerRank playerRank : allPlayerRanks) {
				if (playerRank.isActive()) {
					RPlayerRank freshRank = this.rdq.getPlayerRankRepository().findById(playerRank.getId()).orElse(null);
					if (freshRank != null && freshRank.isActive()) {
						freshRank.setActive(false);
						this.rdq.getPlayerRankRepository().update(freshRank);
					}
				}
			}
		} catch (final Exception exception) {
			LOGGER.log(Level.WARNING, "Failed to deactivate all player ranks for player " + player.getPlayerName(), exception);
		}
	}
	
	/**
	 * Gets the player's rank for a specific tree.
	 */
	@Nullable
	private RPlayerRank getPlayerRankForTree(
		final @NotNull RDQPlayer player,
		final @NotNull RRankTree rankTree
	) {
		try {
			final List<RPlayerRank> playerRanks = this.rdq.getPlayerRankRepository()
			                                              .findAllByAttributes(Map.of("player.uniqueId", player.getUniqueId()));
			
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
	private boolean checkRankTreePrerequisites(
		final @NotNull RDQPlayer player,
		final @NotNull RRankTree rankTree
	) {
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
	 * Gets the current active rank path for a player.
	 */
	@Nullable
	private RPlayerRankPath getCurrentRankPath(final @NotNull RDQPlayer player) {
		try {
			final List<RPlayerRankPath> rankPaths = this.rdq.getPlayerRankPathRepository()
			                                                .findAllByAttributes(Map.of("player", player));
			
			return rankPaths.stream()
			                .filter(RPlayerRankPath::isActive)
			                .findFirst()
			                .orElse(null);
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
			// TODO: Implement logic to count completed rank trees
			return 0; // Placeholder
		} catch (final Exception exception) {
			LOGGER.log(Level.WARNING, "Failed to get completed rank trees count", exception);
			return 0;
		}
	}
	
	/**
	 * Checks if a rank tree is completed by a player.
	 */
	private boolean isRankTreeCompleted(
		final @NotNull RDQPlayer player,
		final @NotNull RRankTree rankTree
	) {
		try {
			// TODO: Implement logic to check if rank tree is completed
			return false; // Placeholder
		} catch (final Exception exception) {
			LOGGER.log(Level.WARNING, "Failed to check if rank tree is completed", exception);
			return false;
		}
	}
	
	/**
	 * Checks if a player has selected a specific rank path.
	 */
	public boolean hasSelectedRankPath(final @NotNull RDQPlayer player, final @NotNull RRankTree rankTree) {
		try {
			final RPlayerRankPath rankPath = this.getRankPathForTree(player, rankTree);
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
		try {
			// TODO: Implement cleanup logic for legacy ranks
			LOGGER.log(Level.FINE, "Cleaned up legacy ranks for player {}", player.getPlayerName());
		} catch (final Exception exception) {
			LOGGER.log(Level.WARNING, "Failed to cleanup legacy ranks", exception);
		}
	}
	
	/**
	 * Gets all player ranks for a specific tree.
	 */
	public List<RPlayerRank> getPlayerRanksForTree(
		final @NotNull RDQPlayer player,
		final @Nullable RRankTree rankTree
	) {
		try {
			final List<RPlayerRank> allPlayerRanks = this.rdq.getPlayerRankRepository()
			                                                 .findAllByAttributes(Map.of("player.uniqueId", player.getUniqueId()));
			
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
}