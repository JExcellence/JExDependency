package com.raindropcentral.rdq.rank.progression;

import com.raindropcentral.rdq.database.entity.rank.RRank;
import com.raindropcentral.rdq.database.repository.RPlayerRankRepository;
import com.raindropcentral.rdq.database.repository.RRankRepository;
import com.raindropcentral.rplatform.progression.ICompletionTracker;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Implementation of {@link ICompletionTracker} for Rank entities.
 * <p>
 * This class provides completion tracking functionality for ranks, integrating
 * with the existing rank system's database entities. In the rank system, "completion"
 * means a player has achieved a rank and it is their current active rank.
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * // Create tracker
 * RankCompletionTracker tracker = new RankCompletionTracker(
 *     playerRankRepository,
 *     rankRepository
 * );
 *
 * // Check if player has achieved a rank
 * UUID playerId = player.getUniqueId();
 * boolean achieved = tracker.hasCompleted(playerId, "bronze_rank").join();
 *
 * // Mark rank as achieved (sets as current rank)
 * tracker.markCompleted(playerId, "bronze_rank").thenRun(() -> {
 *     player.sendMessage("Rank achieved!");
 * });
 *
 * // Get all achieved ranks
 * List<String> achieved = tracker.getCompletedNodes(playerId).join();
 * }</pre>
 *
 * <h2>Integration Points:</h2>
 * <ul>
 *     <li>{@link RPlayerRankRepository} - For rank achievement persistence</li>
 *     <li>{@link RRankRepository} - For rank data access</li>
 * </ul>
 *
 * <h2>Performance Characteristics:</h2>
 * <ul>
 *     <li>Database queries are async using CompletableFuture</li>
 *     <li>Leverages CachedRepository for improved performance</li>
 * </ul>
 *
 * @author RaindropCentral
 * @version 1.0.0
 * @since 1.0.0
 */
public class RankCompletionTracker implements ICompletionTracker<RRank> {
	
	private final RPlayerRankRepository playerRankRepository;
	private final RRankRepository rankRepository;
	
	/**
	 * Constructs a new rank completion tracker.
	 *
	 * @param playerRankRepository Repository for player rank data
	 * @param rankRepository Repository for rank data
	 */
	public RankCompletionTracker(
		@NotNull RPlayerRankRepository playerRankRepository,
		@NotNull RRankRepository rankRepository
	) {
		this.playerRankRepository = playerRankRepository;
		this.rankRepository = rankRepository;
	}
	
	@Override
	@NotNull
	public CompletableFuture<Boolean> hasCompleted(@NotNull UUID playerId, @NotNull String nodeIdentifier) {
		// Find rank by identifier
		return rankRepository.findByIdentifier(nodeIdentifier)
			.thenCompose(rankOpt -> {
				if (rankOpt.isEmpty()) {
					return CompletableFuture.completedFuture(false);
				}
				// Check if player has this rank as their current rank
				return playerRankRepository.existsByPlayerIdAndRankId(playerId, rankOpt.get().getId());
			});
	}
	
	@Override
	@NotNull
	public CompletableFuture<Boolean> isActive(@NotNull UUID playerId, @NotNull String nodeIdentifier) {
		// Find rank by identifier
		return rankRepository.findByIdentifier(nodeIdentifier)
			.thenCompose(rankOpt -> {
				if (rankOpt.isEmpty()) {
					return CompletableFuture.completedFuture(false);
				}
				// Check if this is the player's current active rank
				return playerRankRepository.findByPlayerIdAndRankId(playerId, rankOpt.get().getId())
					.thenApply(prOpt -> prOpt.map(pr -> pr.isActive()).orElse(false));
			});
	}
	
	@Override
	@NotNull
	public CompletableFuture<List<String>> getCompletedNodes(@NotNull UUID playerId) {
		// Get all active ranks for the player
		return playerRankRepository.findByPlayerIdAndIsActive(playerId, true)
			.thenApply(playerRanks -> playerRanks.stream()
				.map(pr -> pr.getCurrentRank().getIdentifier())
				.collect(Collectors.toList()));
	}
	
	@Override
	@NotNull
	public CompletableFuture<Void> markCompleted(@NotNull UUID playerId, @NotNull String nodeIdentifier) {
		// Note: This method is a placeholder for the progression system.
		// Actual rank granting should be done through the RankService
		// which handles LuckPerms integration, rewards, etc.
		return CompletableFuture.completedFuture(null);
	}
	
	/**
	 * Invalidates any cached completion data for a player.
	 * <p>
	 * CachedRepository handles cache invalidation automatically,
	 * so this method is a no-op for this implementation.
	 *
	 * @param playerId Player UUID
	 */
	public void invalidateCache(@NotNull UUID playerId) {
		// CachedRepository handles cache invalidation automatically
		// This method is a no-op for this implementation
	}
	
	/**
	 * Gets all ranks a player has achieved (active and inactive).
	 * <p>
	 * This includes ranks from all rank trees the player has progressed through.
	 *
	 * @param playerId Player UUID
	 * @return CompletableFuture containing list of all rank identifiers
	 */
	@NotNull
	public CompletableFuture<List<String>> getAllAchievedRanks(@NotNull UUID playerId) {
		return playerRankRepository.findByPlayerId(playerId)
			.thenApply(playerRanks -> playerRanks.stream()
				.map(pr -> pr.getCurrentRank().getIdentifier())
				.distinct()
				.collect(Collectors.toList()));
	}
	
	/**
	 * Checks if a player has achieved a specific rank in a specific rank tree.
	 * <p>
	 * This is more specific than hasCompleted() as it checks for a particular
	 * rank tree context.
	 *
	 * @param playerId Player UUID
	 * @param rankIdentifier Rank identifier
	 * @param rankTreeIdentifier Rank tree identifier
	 * @return CompletableFuture containing true if player has the rank in that tree
	 */
	@NotNull
	public CompletableFuture<Boolean> hasCompletedInTree(
		@NotNull UUID playerId,
		@NotNull String rankIdentifier,
		@NotNull String rankTreeIdentifier
	) {
		return rankRepository.findByIdentifier(rankIdentifier)
			.thenCompose(rankOpt -> {
				if (rankOpt.isEmpty()) {
					return CompletableFuture.completedFuture(false);
				}
				return playerRankRepository.findByPlayerIdAndRankId(playerId, rankOpt.get().getId())
					.thenApply(prOpt -> prOpt
						.map(pr -> pr.belongsToRankTree(rankTreeIdentifier))
						.orElse(false));
			});
	}
}
