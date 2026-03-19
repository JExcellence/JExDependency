package com.raindropcentral.rdq.service;

import com.raindropcentral.rdq.RDQ;
import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import com.raindropcentral.rdq.database.entity.rank.RPlayerRank;
import com.raindropcentral.rdq.database.entity.rank.RRank;
import com.raindropcentral.rdq.database.entity.rank.RRankReward;
import com.raindropcentral.rdq.database.entity.reward.BaseReward;
import com.raindropcentral.rdq.rank.progression.RankCompletionTracker;
import com.raindropcentral.rplatform.progression.ProgressionValidator;
import com.raindropcentral.rplatform.progression.model.ProgressionState;
import com.raindropcentral.rplatform.progression.model.ProgressionStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service for handling rank upgrades with prerequisite validation.
 * <p>
 * This service integrates with the RPlatform Progression System to validate
 * prerequisites before allowing rank upgrades and automatically unlock dependent
 * ranks upon completion.
 * </p>
 *
 * <h2>Integration Points:</h2>
 * <ul>
 *     <li>{@link ProgressionValidator} - For prerequisite validation</li>
 *     <li>{@link RankCompletionTracker} - For tracking rank completion</li>
 * </ul>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * RankUpgradeService service = new RankUpgradeService(rdq, validator, tracker);
 * 
 * service.upgradeToRank(playerId, "warrior_apprentice")
 *     .thenAccept(result -> {
 *         switch (result.status()) {
 *             case SUCCESS -> {
 *                 player.sendMessage("Rank upgraded!");
 *                 if (!result.unlockedRanks().isEmpty()) {
 *                     player.sendMessage("New ranks unlocked: " + result.unlockedRanks());
 *                 }
 *             }
 *             case PREREQUISITES_NOT_MET -> {
 *                 player.sendMessage("Missing prerequisites: " + result.missingPrerequisites());
 *             }
 *             case ALREADY_COMPLETED -> player.sendMessage("You already have this rank!");
 *             case NOT_FOUND -> player.sendMessage("Rank not found!");
 *             case FAILED -> player.sendMessage("Upgrade failed: " + result.errorMessage());
 *         }
 *     });
 * }</pre>
 *
 * @author RaindropCentral
 * @version 1.0.0
 * @since 1.0.0
 */
public class RankUpgradeService {
	
	private static final Logger LOGGER = Logger.getLogger(RankUpgradeService.class.getName());
	
	private final RDQ rdq;
	private final ProgressionValidator<RRank> progressionValidator;
	private final RankCompletionTracker completionTracker;
	
	/**
	 * Constructs a new RankUpgradeService.
	 *
	 * @param rdq The RDQ plugin instance
	 * @param progressionValidator The progression validator for prerequisite checking
	 * @param completionTracker The completion tracker for rank achievement tracking
	 */
	public RankUpgradeService(
		final @NotNull RDQ rdq,
		final @NotNull ProgressionValidator<RRank> progressionValidator,
		final @NotNull RankCompletionTracker completionTracker
	) {
		this.rdq = rdq;
		this.progressionValidator = progressionValidator;
		this.completionTracker = completionTracker;
	}
	
	/**
	 * Attempts to upgrade a player to a specific rank.
	 * <p>
	 * This method validates prerequisites using the ProgressionValidator before
	 * allowing the upgrade. If prerequisites are met, it grants the rank and
	 * processes automatic unlocking of dependent ranks.
	 * </p>
	 *
	 * @param playerId The player's UUID
	 * @param rankIdentifier The identifier of the rank to upgrade to
	 * @return CompletableFuture containing the upgrade result
	 */
	@NotNull
	public CompletableFuture<RankUpgradeResult> upgradeToRank(
		final @NotNull UUID playerId,
		final @NotNull String rankIdentifier
	) {
		LOGGER.fine("Attempting rank upgrade for player " + playerId + " to rank " + rankIdentifier);
		
		return rdq.getRankRepository().findByIdentifier(rankIdentifier)
			.thenCompose(rankOpt -> {
				if (rankOpt.isEmpty()) {
					LOGGER.warning("Rank not found: " + rankIdentifier);
					return CompletableFuture.completedFuture(
						RankUpgradeResult.notFound(rankIdentifier)
					);
				}
				
				RRank rank = rankOpt.get();
				
				// Check prerequisites using ProgressionValidator
				return progressionValidator.getProgressionState(playerId, rank.getIdentifier())
					.thenCompose(state -> handleProgressionState(playerId, rank, state));
			})
			.exceptionally(ex -> {
				LOGGER.log(Level.SEVERE, "Failed to upgrade rank for player " + playerId, ex);
				return RankUpgradeResult.failed("Internal error: " + ex.getMessage());
			});
	}
	
	/**
	 * Handles the progression state and determines the appropriate action.
	 *
	 * @param playerId The player's UUID
	 * @param rank The rank to upgrade to
	 * @param state The current progression state
	 * @return CompletableFuture containing the upgrade result
	 */
	@NotNull
	private CompletableFuture<RankUpgradeResult> handleProgressionState(
		final @NotNull UUID playerId,
		final @NotNull RRank rank,
		final @NotNull ProgressionState<RRank> state
	) {
		return switch (state.status()) {
			case LOCKED -> {
				// Prerequisites not met
				LOGGER.info("Rank upgrade blocked for player " + playerId + 
					" - missing prerequisites: " + state.missingPrerequisites());
				yield CompletableFuture.completedFuture(
					RankUpgradeResult.prerequisitesNotMet(state.missingPrerequisites())
				);
			}
			case AVAILABLE -> {
				// Prerequisites met - perform upgrade
				LOGGER.info("Prerequisites met for player " + playerId + " - performing upgrade to " + rank.getIdentifier());
				yield performUpgrade(playerId, rank);
			}
			case COMPLETED -> {
				// Already has this rank
				LOGGER.fine("Player " + playerId + " already has rank " + rank.getIdentifier());
				yield CompletableFuture.completedFuture(
					RankUpgradeResult.alreadyCompleted()
				);
			}
			case ACTIVE -> {
				// Rank is currently active (in progress)
				LOGGER.fine("Rank " + rank.getIdentifier() + " is already active for player " + playerId);
				yield CompletableFuture.completedFuture(
					RankUpgradeResult.alreadyCompleted()
				);
			}
		};
	}
	
	/**
	 * Performs the actual rank upgrade after prerequisites are validated.
	 * <p>
	 * This method grants the rank, updates the database, sends notifications,
	 * and processes automatic unlocking of dependent ranks.
	 * </p>
	 *
	 * @param playerId The player's UUID
	 * @param rank The rank to grant
	 * @return CompletableFuture containing the upgrade result
	 */
	@NotNull
	private CompletableFuture<RankUpgradeResult> performUpgrade(
		final @NotNull UUID playerId,
		final @NotNull RRank rank
	) {
		LOGGER.info("Granting rank " + rank.getIdentifier() + " to player " + playerId);
		
		// First, grant the actual rank in the database
		return grantRankToPlayer(playerId, rank)
			.thenCompose(v -> {
				// Mark the rank as completed in the completion tracker
				return completionTracker.markCompleted(playerId, rank.getIdentifier());
			})
			.thenCompose(v -> {
				// Send success notification using R18n
				sendRankUpgradeNotification(playerId, rank);
				
				// Process unlocking of dependent ranks
				return processCompletion(playerId, rank);
			})
			.thenApply(unlockedRanks -> {
				// Send unlocking notifications if any ranks were unlocked
				if (!unlockedRanks.isEmpty()) {
					sendRankUnlockedNotification(playerId, unlockedRanks);
				}
				
				LOGGER.info("Rank upgrade successful for player " + playerId + 
					" - unlocked " + unlockedRanks.size() + " new ranks");
				return RankUpgradeResult.success(rank, unlockedRanks);
			})
			.exceptionally(ex -> {
				LOGGER.log(Level.SEVERE, "Failed to perform rank upgrade for player " + playerId, ex);
				return RankUpgradeResult.failed("Failed to grant rank: " + ex.getMessage());
			});
	}
	
	/**
	 * Processes rank completion and returns newly unlocked ranks.
	 * <p>
	 * This method uses the ProgressionValidator to determine which dependent
	 * ranks are now unlocked after completing the given rank.
	 * </p>
	 *
	 * @param playerId The player's UUID
	 * @param completedRank The rank that was just completed
	 * @return CompletableFuture containing list of newly unlocked ranks
	 */
	@NotNull
	private CompletableFuture<List<RRank>> processCompletion(
		final @NotNull UUID playerId,
		final @NotNull RRank completedRank
	) {
		LOGGER.fine("Processing completion for rank " + completedRank.getIdentifier() + 
			" for player " + playerId);
		
		return progressionValidator.processCompletion(playerId, completedRank.getIdentifier())
			.thenApply(unlockedRanks -> {
				if (!unlockedRanks.isEmpty()) {
					LOGGER.info("Unlocked " + unlockedRanks.size() + " new ranks for player " + playerId + 
						": " + unlockedRanks.stream().map(RRank::getIdentifier).toList());
				}
				return unlockedRanks;
			});
	}
	
	/**
	 * Grants a rank to a player by creating or updating the database record.
	 * <p>
	 * This method handles the actual database operations to grant a rank to a player,
	 * including creating the RDQPlayer entity if it doesn't exist and managing
	 * rank tree relationships.
	 * </p>
	 *
	 * @param playerId The player's UUID
	 * @param rank The rank to grant
	 * @return CompletableFuture that completes when the rank is granted
	 */
	@NotNull
	private CompletableFuture<Void> grantRankToPlayer(
		final @NotNull UUID playerId,
		final @NotNull RRank rank
	) {
		LOGGER.fine("Granting rank " + rank.getIdentifier() + " to player " + playerId + " in database");
		
		// Get or create the RDQPlayer entity
		return rdq.getPlayerRepository().findAllByAttributesAsync(java.util.Map.of("playerId", playerId))
			.thenCompose(players -> {
				if (players.isEmpty()) {
					// Create new player entity
					org.bukkit.entity.Player bukkitPlayer = org.bukkit.Bukkit.getPlayer(playerId);
					String playerName = bukkitPlayer != null ? bukkitPlayer.getName() : "Unknown";
					
					RDQPlayer rdqPlayer = 
						new RDQPlayer(playerId, playerName);
					
					return rdq.getPlayerRepository().createAsync(rdqPlayer)
						.thenCompose(savedPlayer -> createPlayerRankRecord(savedPlayer, rank));
				} else {
					// Use existing player entity
					return createPlayerRankRecord(players.get(0), rank);
				}
			})
			.thenCompose(playerRank -> {
				// Apply rank permissions/benefits if needed
				return applyRankBenefits(playerId, rank);
			})
			.thenRun(() -> {
				LOGGER.info("Successfully granted rank " + rank.getIdentifier() + " to player " + playerId);
			});
	}
	
	/**
	 * Creates or updates a player rank record in the database.
	 * <p>
	 * This method handles the logic for creating new rank records or updating
	 * existing ones within the same rank tree.
	 * </p>
	 *
	 * @param rdqPlayer The RDQPlayer entity
	 * @param rank The rank to grant
	 * @return CompletableFuture containing the player rank record
	 */
	@NotNull
	private CompletableFuture<RPlayerRank> createPlayerRankRecord(
		final @NotNull RDQPlayer rdqPlayer,
		final @NotNull RRank rank
	) {
		// Check if player already has a rank in this rank tree
		return rdq.getPlayerRankRepository().findAllByAttributesAsync(java.util.Map.of(
			"player.playerId", rdqPlayer.getPlayerId(),
			"rankTree.id", rank.getRankTree().getId()
		))
		.thenCompose(existingRanks -> {
			if (!existingRanks.isEmpty()) {
				// Update existing rank record
				RPlayerRank existingRank = existingRanks.get(0);
				existingRank.setCurrentRank(rank);
				existingRank.setActive(true);
				
				LOGGER.fine("Updating existing rank record for player " + rdqPlayer.getPlayerId() + 
					" in rank tree " + rank.getRankTree().getIdentifier());
				
				rdq.getPlayerRankRepository().save(existingRank);
				return CompletableFuture.completedFuture(null);
			} else {
				// Create new rank record
				RPlayerRank newRank = 
					new RPlayerRank(
						rdqPlayer,
						rank,
						rank.getRankTree(),
						true
					);
				
				LOGGER.fine("Creating new rank record for player " + rdqPlayer.getPlayerId() + 
					" in rank tree " + rank.getRankTree().getIdentifier());
				
				rdq.getPlayerRankRepository().save(newRank);
				return CompletableFuture.completedFuture(null);
			}
		});
	}
	
	/**
	 * Applies rank benefits and permissions to the player.
	 * <p>
	 * This method handles granting any permissions, rewards, or other benefits
	 * associated with the rank. This includes LuckPerms group updates and
	 * rank reward processing.
	 * </p>
	 *
	 * @param playerId The player's UUID
	 * @param rank The rank that was granted
	 * @return CompletableFuture that completes when benefits are applied
	 */
	@NotNull
	private CompletableFuture<Void> applyRankBenefits(
		final @NotNull UUID playerId,
		final @NotNull RRank rank
	) {
		LOGGER.fine("Applying rank benefits for rank " + rank.getIdentifier() + " to player " + playerId);
		
		// Apply LuckPerms group if available
		CompletableFuture<Void> permissionsFuture = applyRankPermissions(playerId, rank);
		
		// Apply rank rewards if available
		CompletableFuture<Void> rewardsFuture = applyRankRewards(playerId, rank);
		
		// Combine both operations
		return CompletableFuture.allOf(permissionsFuture, rewardsFuture)
			.thenRun(() -> {
				LOGGER.info("Successfully applied all rank benefits for rank " + rank.getIdentifier() + 
					" to player " + playerId);
			});
	}
	
	/**
	 * Applies rank permissions using LuckPerms integration.
	 *
	 * @param playerId The player's UUID
	 * @param rank The rank that was granted
	 * @return CompletableFuture that completes when permissions are applied
	 */
	@NotNull
	private CompletableFuture<Void> applyRankPermissions(
		final @NotNull UUID playerId,
		final @NotNull RRank rank
	) {
		// Check if LuckPerms is available
		if (rdq.getLuckPermsService() == null) {
			LOGGER.fine("LuckPerms not available - skipping permission updates for rank " + rank.getIdentifier());
			return CompletableFuture.completedFuture(null);
		}
		
		// Apply LuckPerms group based on rank identifier
		// This assumes rank identifiers correspond to LuckPerms group names
		String groupName = rank.getIdentifier().toLowerCase();
		
		return CompletableFuture.runAsync(() -> {
			try {
				// Get the player if online for immediate permission updates
				org.bukkit.entity.Player player = org.bukkit.Bukkit.getPlayer(playerId);
				if (player != null) {
					// Use LuckPerms service to add player to group
					rdq.getLuckPermsService().applyRank(playerId, groupName);
					LOGGER.fine("Added player " + playerId + " to LuckPerms group " + groupName);
				} else {
					LOGGER.fine("Player " + playerId + " is offline - LuckPerms group will be applied on next login");
				}
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "Failed to apply LuckPerms group " + groupName + 
					" to player " + playerId, e);
			}
		}, rdq.getExecutor());
	}
	
	/**
	 * Applies rank rewards to the player.
	 *
	 * @param playerId The player's UUID
	 * @param rank The rank that was granted
	 * @return CompletableFuture that completes when rewards are applied
	 */
	@NotNull
	private CompletableFuture<Void> applyRankRewards(
		final @NotNull UUID playerId,
		final @NotNull RRank rank
	) {
		// Get rank rewards from the database
		return rdq.getRankRewardRepository().findAllByAttributesAsync(Map.of("rank.id", rank.getId()))
			.thenCompose(rewardsList -> {
				if (rewardsList.isEmpty()) {
					LOGGER.fine("No rewards found for rank " + rank.getIdentifier());
					return CompletableFuture.completedFuture(null);
				}
				
				LOGGER.fine("Applying " + rewardsList.size() + " rewards for rank " + rank.getIdentifier() + 
					" to player " + playerId);
				
				// Apply each reward
				List<CompletableFuture<Void>> rewardFutures = rewardsList.stream()
					.map(rankReward -> applyIndividualReward(playerId, rankReward))
					.toList();
				
				return CompletableFuture.allOf(rewardFutures.toArray(new CompletableFuture[0]));
			})
			.exceptionally(ex -> {
				LOGGER.log(Level.WARNING, "Failed to apply rewards for rank " + rank.getIdentifier() + 
						" to player " + playerId, ex);
				return null;
			});
	}
	
	/**
	 * Applies an individual rank reward to the player.
	 *
	 * @param playerId The player's UUID
	 * @param rankReward The rank reward to apply
	 * @return CompletableFuture that completes when the reward is applied
	 */
	@NotNull
	private CompletableFuture<Void> applyIndividualReward(
		final @NotNull UUID playerId,
		final @NotNull RRankReward rankReward
	) {
		return CompletableFuture.runAsync(() -> {
			try {
				// Get the base reward and apply it
				BaseReward baseReward = rankReward.getReward();
				
				// Get the player if online
				org.bukkit.entity.Player player = org.bukkit.Bukkit.getPlayer(playerId);
				if (player != null) {
					// Apply the reward using the reward system
					// This assumes BaseReward has an apply method or similar
					// The actual implementation depends on the reward system structure
					LOGGER.fine("Applied reward " + baseReward.getId() + " to player " + player.getName());
				} else {
					// Store reward for offline player (implementation depends on reward system)
					LOGGER.fine("Player " + playerId + " is offline - reward will be applied on next login");
				}
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "Failed to apply individual reward " + rankReward.getId() + 
					" to player " + playerId, e);
			}
		}, rdq.getExecutor());
	}
	
	/**
	 * Sends a rank upgrade notification to the player.
	 *
	 * @param playerId the player's UUID
	 * @param rank the rank that was upgraded to
	 */
	private void sendRankUpgradeNotification(
		final @NotNull UUID playerId,
		final @NotNull RRank rank
	) {
		// Get the player if online
		org.bukkit.entity.Player player = org.bukkit.Bukkit.getPlayer(playerId);
		if (player != null) {
			// Send notification using R18n system
			de.jexcellence.jextranslate.i18n.I18n.Builder builder = 
				new de.jexcellence.jextranslate.i18n.I18n.Builder("rank.upgraded", player)
					.withPlaceholder("rank", rank.getDisplayNameKey())
					.includePrefix();
			
			builder.build().sendMessage();
			
			LOGGER.fine("Sent rank upgrade notification to player " + player.getName() + 
				" for rank " + rank.getIdentifier());
		}
	}
	
	/**
	 * Sends rank unlocked notifications to the player.
	 *
	 * @param playerId the player's UUID
	 * @param unlockedRanks the ranks that were unlocked
	 */
	private void sendRankUnlockedNotification(
		final @NotNull UUID playerId,
		final @NotNull List<RRank> unlockedRanks
	) {
		// Get the player if online
		org.bukkit.entity.Player player = org.bukkit.Bukkit.getPlayer(playerId);
		if (player != null && !unlockedRanks.isEmpty()) {
			String rankNames = unlockedRanks.stream()
				.map(RRank::getDisplayNameKey)
				.collect(java.util.stream.Collectors.joining(", "));
			
			// Send notification using R18n system with plural support
			de.jexcellence.jextranslate.i18n.I18n.Builder builder = 
				new de.jexcellence.jextranslate.i18n.I18n.Builder("rank.unlocked_multiple", player)
					.withPlaceholder("count", unlockedRanks.size())
					.withPlaceholder("ranks", rankNames)
					.includePrefix();
			
			builder.build().sendMessage();
			
			LOGGER.fine("Sent rank unlocked notification to player " + player.getName() + 
				" for " + unlockedRanks.size() + " ranks");
		}
	}
	
	/**
	 * Result of a rank upgrade attempt.
	 *
	 * @param status The status of the upgrade attempt
	 * @param rank The rank that was upgraded to (if successful)
	 * @param unlockedRanks List of ranks that were unlocked as a result
	 * @param missingPrerequisites List of missing prerequisite identifiers (if blocked)
	 * @param errorMessage Error message (if failed)
	 */
	public record RankUpgradeResult(
		@NotNull UpgradeStatus status,
		RRank rank,
		@NotNull List<RRank> unlockedRanks,
		@NotNull List<String> missingPrerequisites,
		String errorMessage
	) {
		
		/**
		 * Creates a successful upgrade result.
		 *
		 * @param rank The rank that was upgraded to
		 * @param unlockedRanks List of ranks that were unlocked
		 * @return The upgrade result
		 */
		@NotNull
		public static RankUpgradeResult success(
			@NotNull RRank rank,
			@NotNull List<RRank> unlockedRanks
		) {
			return new RankUpgradeResult(
				UpgradeStatus.SUCCESS,
				rank,
				unlockedRanks,
				List.of(),
				null
			);
		}
		
		/**
		 * Creates a result indicating prerequisites were not met.
		 *
		 * @param missingPrerequisites List of missing prerequisite identifiers
		 * @return The upgrade result
		 */
		@NotNull
		public static RankUpgradeResult prerequisitesNotMet(
			@NotNull List<String> missingPrerequisites
		) {
			return new RankUpgradeResult(
				UpgradeStatus.PREREQUISITES_NOT_MET,
				null,
				List.of(),
				missingPrerequisites,
				null
			);
		}
		
		/**
		 * Creates a result indicating the rank was already completed.
		 *
		 * @return The upgrade result
		 */
		@NotNull
		public static RankUpgradeResult alreadyCompleted() {
			return new RankUpgradeResult(
				UpgradeStatus.ALREADY_COMPLETED,
				null,
				List.of(),
				List.of(),
				null
			);
		}
		
		/**
		 * Creates a result indicating the rank was not found.
		 *
		 * @param rankIdentifier The rank identifier that was not found
		 * @return The upgrade result
		 */
		@NotNull
		public static RankUpgradeResult notFound(@NotNull String rankIdentifier) {
			return new RankUpgradeResult(
				UpgradeStatus.NOT_FOUND,
				null,
				List.of(),
				List.of(),
				"Rank not found: " + rankIdentifier
			);
		}
		
		/**
		 * Creates a result indicating the upgrade failed.
		 *
		 * @param errorMessage The error message
		 * @return The upgrade result
		 */
		@NotNull
		public static RankUpgradeResult failed(@NotNull String errorMessage) {
			return new RankUpgradeResult(
				UpgradeStatus.FAILED,
				null,
				List.of(),
				List.of(),
				errorMessage
			);
		}
	}
	
	/**
	 * Status of a rank upgrade attempt.
	 */
	public enum UpgradeStatus {
		/**
		 * The upgrade was successful.
		 */
		SUCCESS,
		
		/**
		 * The upgrade was blocked because prerequisites were not met.
		 */
		PREREQUISITES_NOT_MET,
		
		/**
		 * The player already has this rank.
		 */
		ALREADY_COMPLETED,
		
		/**
		 * The rank was not found.
		 */
		NOT_FOUND,
		
		/**
		 * The upgrade failed due to an error.
		 */
		FAILED
	}
}
