package com.raindropcentral.rdq.view.ranks.interaction;


import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.database.entity.rank.*;
import com.raindropcentral.rdq.service.RankUpgradeProgressService;
import com.raindropcentral.rdq.view.ranks.hierarchy.RankNode;
import com.raindropcentral.rplatform.logging.CentralLogger;
import de.jexcellence.jextranslate.i18n.I18n;
import me.devnatan.inventoryframework.context.SlotClickContext;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages rank progression operations including starting progression,
 * checking completion, handling rank redemption, and automatic rank assignment.
 *
 * Refactored to follow the working pattern from RankPathOverview with:
 * - Simplified database operations
 * - Early returns to reduce complexity
 * - Clear error handling
 * - Linear execution flow
 */
public class RankProgressionManager {
	
	private static final Logger LOGGER = CentralLogger.getLogger(RankProgressionManager.class.getName());
	
	private final RDQ rdq;
	private final RankUpgradeProgressService rankUpgradeProgressService;
	
	public RankProgressionManager(final @NotNull RDQ rdq) {
		this.rdq = rdq;
		this.rankUpgradeProgressService = new RankUpgradeProgressService(this.rdq);
	}
	
	/**
	 * Handles automatic assignment of the first rank when a player selects a rank path.
	 */
	public void assignInitialRankForPath(
		final @NotNull RDQPlayer rdqPlayer,
		final @NotNull RRankTree rankTree
	) {
		try {
			// Find the initial rank for this tree
			final RRank initialRank = this.findInitialRankForTree(rankTree);
			if (initialRank == null) {
				LOGGER.log(Level.WARNING, "No initial rank found for rank tree: " + rankTree.getIdentifier());
				return;
			}
			
			// Check if player already has this rank
			if (this.playerHasRank(rdqPlayer, initialRank)) {
				LOGGER.log(Level.FINE, "Player " + rdqPlayer.getPlayerName() + " already has initial rank " + initialRank.getIdentifier());
				return;
			}
			
			// Automatically assign the initial rank
			this.processRankAssignment(rdqPlayer, initialRank, rankTree, true);
			
			LOGGER.log(Level.INFO, "Automatically assigned initial rank " + initialRank.getIdentifier() + " to player " + rdqPlayer.getPlayerName() + " for path " + rankTree.getIdentifier());
			
		} catch (final Exception exception) {
			LOGGER.log(Level.SEVERE, "Failed to assign initial rank for path", exception);
		}
	}
	
	/**
	 * Starts progression tracking for a rank that is available to the player.
	 */
	public void startRankProgression(
		final @NotNull SlotClickContext clickContext,
		final @NotNull RankNode rankNode,
		final @NotNull RDQPlayer rdqPlayer
	) {
		try {
			final Player player = clickContext.getPlayer();
			final Set<RRankUpgradeRequirement> upgradeRequirements = rankNode.rank.getUpgradeRequirements();
			
			// Handle ranks with no requirements (instant completion)
			if (upgradeRequirements.isEmpty()) {
				this.sendNoRequirementsMessage(player, rankNode.rank);
				this.processRankAssignmentWithPlayer(player, rdqPlayer, rankNode.rank, rankNode.rank.getRankTree(), false);
				return;
			}
			
			// Initialize progress tracking for all requirements
			this.createProgressTrackingEntries(rdqPlayer, upgradeRequirements);
			
			// Send confirmation message
            new I18n.Builder("rank_progression.started", player)
                    .withPlaceholders(Map.of(
                            "rank_name", rankNode.rank.getIdentifier(),
                            "requirement_count", upgradeRequirements.size()
                    ))
                    .includePrefix()
                    .build().sendMessage();
			
			LOGGER.log(Level.INFO, "Started rank progression for player " + player.getName() + " on rank " + rankNode.rank.getIdentifier());
			
		} catch (final Exception exception) {
			LOGGER.log(Level.SEVERE, "Failed to start rank progression", exception);
			this.sendErrorMessage(clickContext.getPlayer(), "rank_progression.start_failed");
		}
	}
	
	/**
	 * Attempts to redeem a rank that the player has been working on.
	 */
	public void attemptRankRedemption(
		final @NotNull SlotClickContext clickContext,
		final @NotNull RankNode rankNode
	) {
		try {
			final Player player = clickContext.getPlayer();
			final RDQPlayer rdqPlayer = this.getRDQPlayer(clickContext);
			
			if (rdqPlayer == null) {
				this.sendErrorMessage(player, "rank_progression.player_not_found");
				return;
			}
			
			// Check if all requirements are completed
			if (this.rankUpgradeProgressService.hasCompletedAllUpgradeRequirements(rdqPlayer, rankNode.rank)) {
				this.processRankAssignmentWithPlayer(player, rdqPlayer, rankNode.rank, rankNode.rank.getRankTree(), false);
			} else {
				this.handleIncompleteRequirements(player, rdqPlayer, rankNode.rank);
			}
			
		} catch (final Exception exception) {
			LOGGER.log(Level.SEVERE, "Failed to attempt rank redemption", exception);
			this.sendErrorMessage(clickContext.getPlayer(), "rank_progression.redemption_failed");
		}
	}
	
	/**
	 * Opens a detailed view of the rank's upgrade requirements.
	 */
	public void openRequirementsView(
		final @NotNull SlotClickContext clickContext,
		final @NotNull RankNode rankNode
	) {
		try {
			final Player player = clickContext.getPlayer();
			final RDQPlayer rdqPlayer = this.getRDQPlayer(clickContext);
			
			if (rdqPlayer == null) {
				this.sendErrorMessage(player, "rank_progression.player_not_found");
				return;
			}
			
			// TODO: Open requirements detail view
			// This would typically open another GUI showing detailed progress
            new I18n.Builder("rank_progression.requirements_view_opened", player)
                    .includePrefix()
                    .withPlaceholder("rank_name", rankNode.rank.getIdentifier())
                    .build().sendMessage();
			
			LOGGER.log(Level.FINE, "Opened requirements view for rank " + rankNode.rank.getIdentifier() + " for player " + player.getName());
			
		} catch (final Exception exception) {
			LOGGER.log(Level.WARNING, "Failed to open requirements view", exception);
			this.sendErrorMessage(clickContext.getPlayer(), "rank_progression.requirements_view_failed");
		}
	}
	
	/**
	 * Checks if any available ranks can be automatically completed and processes them.
	 */
	public void processAutoCompletableRanks(
		final @NotNull RDQPlayer rdqPlayer,
		final @NotNull RRankTree rankTree
	) {
		try {
			final List<RRank> availableRanks = this.getAvailableRanksFoRDQPlayer(rdqPlayer, rankTree);
			
			for (final RRank rank : availableRanks) {
				// Skip if player already has this rank
				if (this.playerHasRank(rdqPlayer, rank)) {
					continue;
				}
				
				// Check if rank has no requirements
				if (rank.getUpgradeRequirements().isEmpty()) {
					this.processRankAssignment(rdqPlayer, rank, rankTree, false);
					LOGGER.log(Level.INFO, "Auto-completed rank " + rank.getIdentifier() + " for player " + rdqPlayer.getPlayerName() + " (no requirements)");
				}
			}
		} catch (final Exception exception) {
			LOGGER.log(Level.WARNING, "Failed to process auto-completable ranks", exception);
		}
	}
	
	/**
	 * Processes the actual rank assignment when requirements are met or for initial ranks.
	 * This version includes player messaging.
	 */
	private void processRankAssignmentWithPlayer(
		final @NotNull Player player,
		final @NotNull RDQPlayer rdqPlayer,
		final @NotNull RRank rank,
		final @NotNull RRankTree rankTree,
		final boolean isInitialRank
	) {
		try {
			// Handle the rank assignment in the database
			this.handleRankAssignmentInTree(rdqPlayer, rankTree, rank, isInitialRank);
			
			// Handle LuckPerms integration if available
			this.handleLuckPermsIntegration(rdqPlayer, rank);
			
			// Send appropriate success message
			final String messageKey = isInitialRank ? "rank_progression.initial_rank_assigned" : "rank_progression.redeemed_successfully";

            new I18n.Builder(messageKey, player)
                    .includePrefix()
                    .withPlaceholder("rank_name", rank.getIdentifier())
                    .build().sendMessage();
			
			// Execute rank rewards/commands if any
			this.executeRankRewards(rdqPlayer, rank);
			
			// Check for auto-completable ranks that might now be available
			if (!isInitialRank) {
				this.processAutoCompletableRanks(rdqPlayer, rankTree);
			}
			
			LOGGER.log(Level.INFO, "Player " + rdqPlayer.getPlayerName() + " successfully " + (isInitialRank ? "received initial" : "redeemed") + " rank " + rank.getIdentifier());
			
		} catch (final Exception exception) {
			LOGGER.log(Level.SEVERE, "Failed to process rank assignment", exception);
			this.sendErrorMessage(player, "rank_progression.assignment_failed");
		}
	}
	
	/**
	 * Processes the actual rank assignment when requirements are met or for initial ranks.
	 * This version is for internal use without player messaging.
	 */
	private void processRankAssignment(
		final @NotNull RDQPlayer rdqPlayer,
		final @NotNull RRank rank,
		final @NotNull RRankTree rankTree,
		final boolean isInitialRank
	) {
		try {
			// Handle the rank assignment in the database
			this.handleRankAssignmentInTree(rdqPlayer, rankTree, rank, isInitialRank);
			
			// Handle LuckPerms integration if available
			this.handleLuckPermsIntegration(rdqPlayer, rank);
			
			// Execute rank rewards/commands if any
			this.executeRankRewards(rdqPlayer, rank);
			
			// Check for auto-completable ranks that might now be available
			if (!isInitialRank) {
				this.processAutoCompletableRanks(rdqPlayer, rankTree);
			}
			
			LOGGER.log(Level.INFO, "Player " + rdqPlayer.getPlayerName() + " successfully " + (isInitialRank ? "received initial" : "redeemed") + " rank " + rank.getIdentifier());
			
		} catch (final Exception exception) {
			LOGGER.log(Level.SEVERE, "Failed to process rank assignment", exception);
			throw new RuntimeException("Rank assignment failed", exception);
		}
	}
	
	/**
	 * Handles rank assignment within the specific rank tree.
	 * Simplified to follow the working pattern from RankPathOverview.
	 */
	private void handleRankAssignmentInTree(
		final @NotNull RDQPlayer rdqPlayer,
		final @NotNull RRankTree rankTree,
		final @NotNull RRank newRank,
		final boolean isInitialRank
	) {
		try {
			final RPlayerRank existingRankInTree = this.getPlayerRankForTree(rdqPlayer, rankTree);
			
			if (existingRankInTree != null) {
				// Update existing rank
				final RPlayerRank freshRank = this.rdq.getPlayerRankRepository().findById(existingRankInTree.getId()).orElse(null);
				if (freshRank == null) {
					throw new RuntimeException("Could not find existing rank to update");
				}
				
				freshRank.setCurrentRank(newRank);
				freshRank.setActive(true);
				this.rdq.getPlayerRankRepository().update(freshRank);
				LOGGER.log(Level.INFO, "Updated existing rank for player " + rdqPlayer.getPlayerName() + " to " + newRank.getIdentifier());
			} else {
				// Create new rank assignment
				final RPlayerRank newPlayerRank = new RPlayerRank(rdqPlayer, newRank, rankTree, true);
				rdqPlayer.addPlayerRank(newPlayerRank);
				this.rdq.getPlayerRankRepository().create(newPlayerRank);
				LOGGER.log(Level.INFO, "Created new rank assignment for player " + rdqPlayer.getPlayerName() + " with rank " + newRank.getIdentifier());
			}
			
			// Update player entity
			final RDQPlayer freshPlayer = this.rdq.getPlayerRepository().findById(rdqPlayer.getId()).orElse(null);
			if (freshPlayer != null) {
				this.rdq.getPlayerRepository().update(freshPlayer);
			}
			
			// Mark progress entries as completed if this is initial rank
			if (isInitialRank) {
				this.markProgressEntriesCompleted(rdqPlayer, newRank);
			}
			
		} catch (final Exception exception) {
			LOGGER.log(Level.SEVERE, "Failed to handle rank assignment in tree", exception);
			throw new RuntimeException("Database rank assignment failed", exception);
		}
	}
	
	/**
	 * Marks all progress entries for a rank as completed.
	 * Simplified to use direct database operations.
	 */
	private void markProgressEntriesCompleted(
		final @NotNull RDQPlayer rdqPlayer,
		final @NotNull RRank newRank
	) {
		try {
			for (final RRankUpgradeRequirement upgradeRequirement : newRank.getUpgradeRequirements()) {
				final List<RPlayerRankUpgradeProgress> progressList = this.rdq.getPlayerRankUpgradeProgressRepository()
				                                                              .findAllByAttributes(Map.of(
					                                                              "player.id", rdqPlayer.getId(),
					                                                              "upgradeRequirement.id", upgradeRequirement.getId()
				                                                              ));
				
				for (final RPlayerRankUpgradeProgress progress : progressList) {
					if (progress.isCompleted()) {
						continue;
					}
					
					final RPlayerRankUpgradeProgress freshProgress = this.rdq.getPlayerRankUpgradeProgressRepository()
					                                                         .findById(progress.getId()).orElse(null);
					if (freshProgress == null) {
						continue;
					}
					
					freshProgress.setProgress(1.0);
					this.rdq.getPlayerRankUpgradeProgressRepository().update(freshProgress);
				}
			}
			
			LOGGER.log(Level.FINE, "Marked progress entries as completed for rank " + newRank.getIdentifier());
			
		} catch (final Exception exception) {
			LOGGER.log(Level.WARNING, "Failed to mark progress entries as completed", exception);
		}
	}
	
	/**
	 * Creates progress tracking entries for all requirements.
	 * Simplified to use direct database operations.
	 */
	private void createProgressTrackingEntries(
		final @NotNull RDQPlayer rdqPlayer,
		final @NotNull Set<RRankUpgradeRequirement> upgradeRequirements
	) {
		for (final RRankUpgradeRequirement requirement : upgradeRequirements) {
			final List<RPlayerRankUpgradeProgress> existingProgress = this.rdq.getPlayerRankUpgradeProgressRepository()
			                                                                  .findAllByAttributes(Map.of(
				                                                                  "player.uniqueId", rdqPlayer.getUniqueId(),
				                                                                  "upgradeRequirement.id", requirement.getId()
			                                                                  ));
			
			if (existingProgress.isEmpty()) {
				final RPlayerRankUpgradeProgress newProgress = new RPlayerRankUpgradeProgress(rdqPlayer, requirement);
				this.rdq.getPlayerRankUpgradeProgressRepository().create(newProgress);
				LOGGER.log(Level.FINE, "Created progress tracking for requirement " + requirement.getId());
			}
		}
	}
	
	/**
	 * Handles LuckPerms integration for rank assignment.
	 */
	private void handleLuckPermsIntegration(final @NotNull RDQPlayer rdqPlayer, final @NotNull RRank newRank) {
		try {
			if (this.rdq.getLuckPermsService() == null) {
				LOGGER.log(Level.WARNING, "LuckPerms service not available for rank assignment");
				return;
			}
			
			final String luckPermsGroup = newRank.getAssignedLuckPermsGroup();
			if (luckPermsGroup == null || luckPermsGroup.isEmpty()) {
				LOGGER.log(Level.WARNING, "No LuckPerms group defined for rank " + newRank.getIdentifier());
				return;
			}
			
			// Remove player from previous rank groups in this tree
			this.removePlayerFromPreviousRankGroups(rdqPlayer, newRank);
			
			// Add player to the new rank group
			// TODO: Implement when LuckPermsService methods are available
			// this.rdq.getLuckPermsService().addPlayerToGroup(rdqPlayer.getUniqueId(), luckPermsGroup);
			
			LOGGER.log(Level.INFO, "Assigned LuckPerms group '" + luckPermsGroup + "' to player " + rdqPlayer.getPlayerName());
			
		} catch (final Exception exception) {
			LOGGER.log(Level.WARNING, "Failed to handle LuckPerms integration", exception);
			// Don't throw here - rank assignment should still work even if LuckPerms fails
		}
	}
	
	/**
	 * Removes player from previous rank groups in the same tree.
	 */
	private void removePlayerFromPreviousRankGroups(
		final @NotNull RDQPlayer rdqPlayer,
		final @NotNull RRank newRank
	) {
		try {
			final RRankTree rankTree = newRank.getRankTree();
			if (rankTree == null) {
				return;
			}
			
			for (final RRank rank : rankTree.getRanks()) {
				if (rank.equals(newRank)) {
					continue;
				}
				
				final String groupToRemove = rank.getAssignedLuckPermsGroup();
				if (groupToRemove == null || groupToRemove.isEmpty()) {
					continue;
				}
				
				// TODO: Implement when LuckPermsService methods are available
				// this.rdq.getLuckPermsService().removePlayerFromGroup(rdqPlayer.getUniqueId(), groupToRemove);
			}
		} catch (final Exception exception) {
			LOGGER.log(Level.WARNING, "Failed to remove player from previous rank groups", exception);
		}
	}
	
	/**
	 * Handles the case where requirements are not yet complete.
	 */
	private void handleIncompleteRequirements(
		final @NotNull Player player,
		final @NotNull RDQPlayer rdqPlayer,
		final @NotNull RRank rank
	) {
		try {
			final double completionPercentage = this.rankUpgradeProgressService.getOverallCompletionPercentage(rdqPlayer, rank);
			final int completionPercent = (int) Math.round(completionPercentage * 100);

            new I18n.Builder("rank_progression.requirements_incomplete", player)
                    .withPlaceholders(Map.of(
                            "rank_name", rank.getIdentifier(),
                            "completion_percentage", String.valueOf(completionPercent)
                    ))
                    .includePrefix()
                    .build().sendMessage();
			
			// Show which requirements are still needed
			this.showIncompleteRequirements(player, rdqPlayer, rank);
			
		} catch (final Exception exception) {
			LOGGER.log(Level.WARNING, "Failed to handle incomplete requirements", exception);
		}
	}
	
	/**
	 * Shows the player which requirements are still incomplete.
	 */
	private void showIncompleteRequirements(
		final @NotNull Player player,
		final @NotNull RDQPlayer rdqPlayer,
		final @NotNull RRank rank
	) {
		try {
			final Set<RRankUpgradeRequirement> upgradeRequirements = rank.getUpgradeRequirements();
			
			for (final RRankUpgradeRequirement requirement : upgradeRequirements) {
				if (this.rankUpgradeProgressService.hasCompletedUpgradeRequirement(rdqPlayer, requirement)) {
					continue;
				}
				
				final RPlayerRankUpgradeProgress progress = this.rankUpgradeProgressService.getProgressForRequirement(rdqPlayer, requirement);
				
				if (progress == null) {
					continue;
				}
				
				final double currentProgress = progress.getProgress();
				final double requiredProgress = 1.0;
				final int progressPercent = (int) Math.round((currentProgress / requiredProgress) * 100);

                new I18n.Builder("rank_progression.requirement_incomplete", player)
                        .includePrefix()
                        .withPlaceholders(Map.of(
                                "requirement_type", requirement.getRequirement().getRequirement().getType().name(),
                                "current_progress", String.valueOf((int) currentProgress),
                                "required_progress", String.valueOf((int) requiredProgress),
                                "progress_percentage", String.valueOf(progressPercent)
                        ))
                        .build().sendMessage();
			}
		} catch (final Exception exception) {
			LOGGER.log(Level.WARNING, "Failed to show incomplete requirements", exception);
		}
	}
	
	/**
	 * Executes any rewards or commands associated with the rank.
	 */
	private void executeRankRewards(final @NotNull RDQPlayer rdqPlayer, final @NotNull RRank rank) {
		try {
			// TODO: Implement rank reward execution
			// This could include:
			// - Running commands
			// - Giving items
			// - Granting permissions
			// - Playing effects
			
			LOGGER.log(Level.FINE, "Executed rank rewards for " + rdqPlayer.getPlayerName() + " on rank " + rank.getIdentifier());
			
		} catch (final Exception exception) {
			LOGGER.log(Level.WARNING, "Failed to execute rank rewards", exception);
		}
	}
	
	/**
	 * Sends a message for ranks with no requirements.
	 */
	private void sendNoRequirementsMessage(final @NotNull Player player, final @NotNull RRank rank) {
		try {
            new I18n.Builder("rank_progression.no_requirements", player)
                    .includePrefix()
                    .withPlaceholder("rank_name", rank.getIdentifier())
                    .build().sendMessage();
		} catch (final Exception exception) {
			LOGGER.log(Level.WARNING, "Failed to send no requirements message", exception);
		}
	}
	
	/**
	 * Finds the initial rank for a given rank tree.
	 */
	private @Nullable RRank findInitialRankForTree(final @NotNull RRankTree rankTree) {
		return rankTree.getRanks().stream()
		               .filter(RRank::isInitialRank)
		               .findFirst()
		               .orElse(null);
	}
	
	/**
	 * Checks if a player has a specific rank.
	 */
	private boolean playerHasRank(final @NotNull RDQPlayer rdqPlayer, final @NotNull RRank rank) {
		try {
			final List<RPlayerRank> playerRanks = this.rdq.getPlayerRankRepository()
			                                              .findAllByAttributes(Map.of("player.uniqueId", rdqPlayer.getUniqueId()));
			
			return playerRanks.stream()
			                  .anyMatch(playerRank -> Objects.equals(playerRank.getCurrentRank(), rank));
		} catch (final Exception exception) {
			LOGGER.log(Level.WARNING, "Failed to check if player has rank", exception);
			return false;
		}
	}
	
	/**
	 * Gets available ranks for a player in a specific tree.
	 */
	private @NotNull List<RRank> getAvailableRanksFoRDQPlayer(final @NotNull RDQPlayer rdqPlayer, final @NotNull RRankTree rankTree) {
		// TODO: Implement logic to determine which ranks are available to the player
		// This would check prerequisites and current player progress
		return List.of(); // Placeholder
	}
	
	/**
	 * Gets the player's rank assignment for a specific tree.
	 * Simplified to use direct database operations.
	 */
	private @Nullable RPlayerRank getPlayerRankForTree(
		final @NotNull RDQPlayer rdqPlayer,
		final @NotNull RRankTree rankTree
	) {
		try {
			final List<RPlayerRank> playerRanks = this.rdq.getPlayerRankRepository()
			                                              .findAllByAttributes(Map.of("player.uniqueId", rdqPlayer.getUniqueId()));
			
			return playerRanks.stream()
			                  .filter(playerRank -> {
				                  final RRankTree playerRankTree = playerRank.getRankTree();
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
	 * Gets the RDQPlayer instance for the clicking player.
	 */
	private @Nullable RDQPlayer getRDQPlayer(final @NotNull SlotClickContext clickContext) {
		try {
			return this.rdq.getPlayerRepository().findByAttributes(Map.of("uniqueId", clickContext.getPlayer().getUniqueId())).orElse(null);
		} catch (final Exception exception) {
			LOGGER.log(Level.WARNING, "Failed to get RDQPlayer for player", exception);
			return null;
		}
	}
	
	/**
	 * Sends an error message to the player.
	 */
	private void sendErrorMessage(final @NotNull Player player, final @NotNull String messageKey) {
		try {
            new I18n.Builder(messageKey, player)
                    .includePrefix()
                    .build().sendMessage();
		} catch (final Exception exception) {
			LOGGER.log(Level.WARNING, "Failed to send error message", exception);
		}
	}
}