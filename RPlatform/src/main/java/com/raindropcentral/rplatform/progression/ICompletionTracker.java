package com.raindropcentral.rplatform.progression;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for tracking completion status of progression nodes.
 * <p>
 * Implementations handle persistence and caching of completion data for players,
 * providing async operations to check completion status, track active nodes,
 * and manage completion state.
 * </p>
 *
 * <p>
 * This interface is designed to work with the progression system to provide:
 * </p>
 * <ul>
 *     <li>Async completion status checks</li>
 *     <li>Active node tracking (in-progress nodes)</li>
 *     <li>Batch completion queries</li>
 *     <li>Cache invalidation for performance</li>
 *     <li>Completion marking and persistence</li>
 * </ul>
 *
 * <h2>Basic Implementation Example:</h2>
 * <pre>{@code
 * public class QuestCompletionTracker implements ICompletionTracker<Quest> {
 *     private final QuestCompletionHistoryRepository completionRepository;
 *     private final QuestCacheManager cacheManager;
 *     
 *     public QuestCompletionTracker(
 *         QuestCompletionHistoryRepository completionRepository,
 *         QuestCacheManager cacheManager
 *     ) {
 *         this.completionRepository = completionRepository;
 *         this.cacheManager = cacheManager;
 *     }
 *     
 *     @Override
 *     public CompletableFuture<Boolean> hasCompleted(UUID playerId, String nodeIdentifier) {
 *         return completionRepository.hasPlayerCompleted(playerId, nodeIdentifier);
 *     }
 *     
 *     @Override
 *     public CompletableFuture<Boolean> isActive(UUID playerId, String nodeIdentifier) {
 *         return cacheManager.getActiveQuest(playerId, nodeIdentifier)
 *             .thenApply(Optional::isPresent);
 *     }
 *     
 *     @Override
 *     public CompletableFuture<List<String>> getCompletedNodes(UUID playerId) {
 *         return completionRepository.findCompletedQuestIds(playerId);
 *     }
 *     
 *     @Override
 *     public CompletableFuture<Void> markCompleted(UUID playerId, String nodeIdentifier) {
 *         return completionRepository.recordCompletion(playerId, nodeIdentifier)
 *             .thenRun(() -> invalidateCache(playerId));
 *     }
 *     
 *     @Override
 *     public void invalidateCache(UUID playerId) {
 *         cacheManager.invalidatePlayer(playerId);
 *     }
 * }
 * }</pre>
 *
 * <h2>Rank System Implementation Example:</h2>
 * <pre>{@code
 * public class RankCompletionTracker implements ICompletionTracker<Rank> {
 *     private final RPlayerRankRepository playerRankRepository;
 *     private final Map<UUID, Set<String>> completionCache;
 *     
 *     public RankCompletionTracker(RPlayerRankRepository playerRankRepository) {
 *         this.playerRankRepository = playerRankRepository;
 *         this.completionCache = new ConcurrentHashMap<>();
 *     }
 *     
 *     @Override
 *     public CompletableFuture<Boolean> hasCompleted(UUID playerId, String rankId) {
 *         // Check cache first
 *         Set<String> cached = completionCache.get(playerId);
 *         if (cached != null) {
 *             return CompletableFuture.completedFuture(cached.contains(rankId));
 *         }
 *         
 *         // Query database
 *         return playerRankRepository.findByPlayerAndRank(playerId, rankId)
 *             .thenApply(Optional::isPresent);
 *     }
 *     
 *     @Override
 *     public CompletableFuture<Boolean> isActive(UUID playerId, String rankId) {
 *         return playerRankRepository.getCurrentRank(playerId)
 *             .thenApply(currentRank -> 
 *                 currentRank.map(rank -> rank.getIdentifier().equals(rankId))
 *                     .orElse(false)
 *             );
 *     }
 *     
 *     @Override
 *     public CompletableFuture<List<String>> getCompletedNodes(UUID playerId) {
 *         return playerRankRepository.findCompletedRanks(playerId)
 *             .thenApply(ranks -> ranks.stream()
 *                 .map(Rank::getIdentifier)
 *                 .toList());
 *     }
 *     
 *     @Override
 *     public CompletableFuture<Void> markCompleted(UUID playerId, String rankId) {
 *         return playerRankRepository.recordRankCompletion(playerId, rankId)
 *             .thenRun(() -> invalidateCache(playerId));
 *     }
 *     
 *     @Override
 *     public void invalidateCache(UUID playerId) {
 *         completionCache.remove(playerId);
 *     }
 * }
 * }</pre>
 *
 * <h2>Usage with ProgressionValidator:</h2>
 * <pre>{@code
 * // Create completion tracker
 * QuestCompletionTracker tracker = new QuestCompletionTracker(
 *     completionRepository,
 *     cacheManager
 * );
 *
 * // Create validator with tracker
 * ProgressionValidator<Quest> validator = new ProgressionValidator<>(
 *     tracker,
 *     questRepository.findAll().join()
 * );
 *
 * // Check if quest is unlocked (uses tracker internally)
 * UUID playerId = player.getUniqueId();
 * validator.isNodeUnlocked(playerId, "quest_b").thenAccept(unlocked -> {
 *     if (unlocked) {
 *         // All prerequisites completed
 *     }
 * });
 *
 * // Mark quest as completed
 * tracker.markCompleted(playerId, "quest_a").thenRun(() -> {
 *     // Process automatic unlocking
 *     validator.processCompletion(playerId, "quest_a").thenAccept(unlocked -> {
 *         for (Quest quest : unlocked) {
 *             player.sendMessage("New quest unlocked: " + quest.getName());
 *         }
 *     });
 * });
 * }</pre>
 *
 * <h2>Caching Strategy Example:</h2>
 * <pre>{@code
 * public class CachedCompletionTracker implements ICompletionTracker<Quest> {
 *     private final QuestCompletionHistoryRepository repository;
 *     private final Cache<UUID, Set<String>> completionCache;
 *     
 *     public CachedCompletionTracker(QuestCompletionHistoryRepository repository) {
 *         this.repository = repository;
 *         this.completionCache = Caffeine.newBuilder()
 *             .maximumSize(1000)
 *             .expireAfterWrite(Duration.ofMinutes(30))
 *             .build();
 *     }
 *     
 *     @Override
 *     public CompletableFuture<Boolean> hasCompleted(UUID playerId, String questId) {
 *         Set<String> cached = completionCache.getIfPresent(playerId);
 *         if (cached != null) {
 *             return CompletableFuture.completedFuture(cached.contains(questId));
 *         }
 *         
 *         return getCompletedNodes(playerId)
 *             .thenApply(completed -> {
 *                 completionCache.put(playerId, new HashSet<>(completed));
 *                 return completed.contains(questId);
 *             });
 *     }
 *     
 *     @Override
 *     public void invalidateCache(UUID playerId) {
 *         completionCache.invalidate(playerId);
 *     }
 * }
 * }</pre>
 *
 * <h2>Batch Operations Example:</h2>
 * <pre>{@code
 * // Check multiple completions efficiently
 * CompletableFuture<List<String>> completedFuture = tracker.getCompletedNodes(playerId);
 * 
 * completedFuture.thenAccept(completed -> {
 *     Set<String> completedSet = new HashSet<>(completed);
 *     
 *     // Check multiple quests at once
 *     boolean hasQuestA = completedSet.contains("quest_a");
 *     boolean hasQuestB = completedSet.contains("quest_b");
 *     boolean hasQuestC = completedSet.contains("quest_c");
 *     
 *     // Process results
 *     if (hasQuestA && hasQuestB) {
 *         // Unlock quest that requires both
 *     }
 * });
 * }</pre>
 *
 * <h2>Best Practices:</h2>
 * <ul>
 *     <li>Always use async operations to avoid blocking the main thread</li>
 *     <li>Implement caching for frequently accessed completion data</li>
 *     <li>Invalidate cache when completion status changes</li>
 *     <li>Use batch operations (getCompletedNodes) when checking multiple nodes</li>
 *     <li>Handle database failures gracefully with proper error handling</li>
 *     <li>Consider using Caffeine or similar cache libraries for automatic expiration</li>
 *     <li>Ensure thread-safety when implementing caching mechanisms</li>
 *     <li>Document cache behavior and invalidation strategy</li>
 * </ul>
 *
 * <h2>Performance Considerations:</h2>
 * <ul>
 *     <li>Cache completion data for online players to reduce database queries</li>
 *     <li>Use batch queries when loading completion data for multiple nodes</li>
 *     <li>Consider lazy loading for large completion histories</li>
 *     <li>Implement cache warming on player join for better performance</li>
 *     <li>Use database indexes on player_id and node_identifier columns</li>
 * </ul>
 *
 * <h2>Thread Safety:</h2>
 * <p>
 * Implementations must be thread-safe as they will be accessed concurrently
 * by multiple players. Use thread-safe collections (ConcurrentHashMap, etc.)
 * and ensure proper synchronization when modifying shared state.
 * </p>
 *
 * @param <T> The type of progression node being tracked
 * @author RaindropCentral
 * @version 1.0.0
 * @since 1.0.0
 */
public interface ICompletionTracker<T extends IProgressionNode<T>> {
    
    /**
     * Checks if a player has completed a specific node.
     * <p>
     * This method should check persistent storage (database) or cache to determine
     * if the player has previously completed this node.
     * </p>
     *
     * <p>
     * Implementations should:
     * </p>
     * <ul>
     *     <li>Check cache first for performance</li>
     *     <li>Fall back to database if not cached</li>
     *     <li>Return false if player or node not found</li>
     *     <li>Handle errors gracefully</li>
     * </ul>
     *
     * @param playerId Player UUID
     * @param nodeIdentifier Node identifier to check
     * @return CompletableFuture containing true if completed, false otherwise
     */
    @NotNull CompletableFuture<Boolean> hasCompleted(@NotNull UUID playerId, @NotNull String nodeIdentifier);
    
    /**
     * Checks if a player has an active (in-progress) node.
     * <p>
     * This method determines if the player is currently working on this node.
     * For quests, this means the quest is started but not completed.
     * For ranks, this means the rank is the player's current rank.
     * </p>
     *
     * <p>
     * Implementations should:
     * </p>
     * <ul>
     *     <li>Check active/in-progress state from cache or database</li>
     *     <li>Return false if node is not active</li>
     *     <li>Return false if node is completed</li>
     *     <li>Handle concurrent modifications safely</li>
     * </ul>
     *
     * @param playerId Player UUID
     * @param nodeIdentifier Node identifier to check
     * @return CompletableFuture containing true if active, false otherwise
     */
    @NotNull CompletableFuture<Boolean> isActive(@NotNull UUID playerId, @NotNull String nodeIdentifier);
    
    /**
     * Gets all completed node identifiers for a player.
     * <p>
     * This method returns a list of all nodes the player has completed,
     * which is useful for batch prerequisite checking and UI rendering.
     * </p>
     *
     * <p>
     * Implementations should:
     * </p>
     * <ul>
     *     <li>Return all completed nodes from persistent storage</li>
     *     <li>Consider caching the result for performance</li>
     *     <li>Return empty list if player has no completions</li>
     *     <li>Ensure list is immutable or defensive copy</li>
     * </ul>
     *
     * @param playerId Player UUID
     * @return CompletableFuture containing list of completed node identifiers (never null, may be empty)
     */
    @NotNull CompletableFuture<List<String>> getCompletedNodes(@NotNull UUID playerId);
    
    /**
     * Marks a node as completed for a player.
     * <p>
     * This method persists the completion to storage and should trigger
     * cache invalidation to ensure consistency.
     * </p>
     *
     * <p>
     * Implementations should:
     * </p>
     * <ul>
     *     <li>Persist completion to database</li>
     *     <li>Update cache if applicable</li>
     *     <li>Trigger cache invalidation via {@link #invalidateCache(UUID)}</li>
     *     <li>Handle duplicate completions gracefully</li>
     *     <li>Record completion timestamp if applicable</li>
     * </ul>
     *
     * @param playerId Player UUID
     * @param nodeIdentifier Node identifier to mark as completed
     * @return CompletableFuture that completes when the operation is done
     */
    @NotNull CompletableFuture<Void> markCompleted(@NotNull UUID playerId, @NotNull String nodeIdentifier);
    
    /**
     * Invalidates cached completion data for a player.
     * <p>
     * This method should be called when completion data changes to ensure
     * the cache reflects the current state. It's typically called after
     * marking a node as completed or when external changes occur.
     * </p>
     *
     * <p>
     * Implementations should:
     * </p>
     * <ul>
     *     <li>Remove player's completion data from cache</li>
     *     <li>Clear any derived caches (progression state, etc.)</li>
     *     <li>Be thread-safe for concurrent invalidation</li>
     *     <li>Handle missing cache entries gracefully</li>
     * </ul>
     *
     * @param playerId Player UUID
     */
    void invalidateCache(@NotNull UUID playerId);
}
