package com.raindropcentral.rplatform.progression;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock implementation of {@link ICompletionTracker} for testing purposes.
 * <p>
 * This implementation stores completion data in memory and provides
 * synchronous operations wrapped in CompletableFuture for testing
 * progression validation logic without requiring database setup.
 * </p>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * // Create mock tracker
 * MockCompletionTracker<Quest> tracker = new MockCompletionTracker<>();
 * 
 * // Mark some quests as completed
 * UUID playerId = UUID.randomUUID();
 * tracker.markCompleted(playerId, "quest_a").join();
 * tracker.markCompleted(playerId, "quest_b").join();
 * 
 * // Check completion
 * boolean completed = tracker.hasCompleted(playerId, "quest_a").join();
 * assertTrue(completed);
 * 
 * // Get all completed
 * List<String> completed = tracker.getCompletedNodes(playerId).join();
 * assertEquals(2, completed.size());
 * }</pre>
 *
 * <h2>Testing with ProgressionValidator:</h2>
 * <pre>{@code
 * @Test
 * void testLinearProgression() {
 *     // Setup
 *     Quest questA = createQuest("quest_a", List.of(), List.of("quest_b"));
 *     Quest questB = createQuest("quest_b", List.of("quest_a"), List.of());
 *     
 *     MockCompletionTracker<Quest> tracker = new MockCompletionTracker<>();
 *     ProgressionValidator<Quest> validator = new ProgressionValidator<>(
 *         tracker,
 *         List.of(questA, questB)
 *     );
 *     
 *     UUID playerId = UUID.randomUUID();
 *     
 *     // Quest B should be locked initially
 *     assertFalse(validator.isNodeUnlocked(playerId, "quest_b").join());
 *     
 *     // Complete quest A
 *     tracker.markCompleted(playerId, "quest_a").join();
 *     
 *     // Quest B should now be unlocked
 *     assertTrue(validator.isNodeUnlocked(playerId, "quest_b").join());
 * }
 * }</pre>
 *
 * @param <T> The type of progression node being tracked
 * @author RaindropCentral
 * @version 1.0.0
 * @since 1.0.0
 */
public class MockCompletionTracker<T extends IProgressionNode<T>> implements ICompletionTracker<T> {
    
    /**
     * Map of player ID to set of completed node identifiers.
     */
    private final Map<UUID, Set<String>> completedNodes;
    
    /**
     * Map of player ID to set of active node identifiers.
     */
    private final Map<UUID, Set<String>> activeNodes;
    
    /**
     * Constructs a new mock completion tracker with empty state.
     */
    public MockCompletionTracker() {
        this.completedNodes = new ConcurrentHashMap<>();
        this.activeNodes = new ConcurrentHashMap<>();
    }
    
    @Override
    @NotNull
    public CompletableFuture<Boolean> hasCompleted(@NotNull UUID playerId, @NotNull String nodeIdentifier) {
        Set<String> completed = completedNodes.get(playerId);
        boolean result = completed != null && completed.contains(nodeIdentifier);
        return CompletableFuture.completedFuture(result);
    }
    
    @Override
    @NotNull
    public CompletableFuture<Boolean> isActive(@NotNull UUID playerId, @NotNull String nodeIdentifier) {
        Set<String> active = activeNodes.get(playerId);
        boolean result = active != null && active.contains(nodeIdentifier);
        return CompletableFuture.completedFuture(result);
    }
    
    @Override
    @NotNull
    public CompletableFuture<List<String>> getCompletedNodes(@NotNull UUID playerId) {
        Set<String> completed = completedNodes.get(playerId);
        List<String> result = completed != null ? new ArrayList<>(completed) : List.of();
        return CompletableFuture.completedFuture(result);
    }
    
    @Override
    @NotNull
    public CompletableFuture<Void> markCompleted(@NotNull UUID playerId, @NotNull String nodeIdentifier) {
        completedNodes.computeIfAbsent(playerId, k -> ConcurrentHashMap.newKeySet())
            .add(nodeIdentifier);
        
        // Remove from active if present
        Set<String> active = activeNodes.get(playerId);
        if (active != null) {
            active.remove(nodeIdentifier);
        }
        
        return CompletableFuture.completedFuture(null);
    }
    
    @Override
    public void invalidateCache(@NotNull UUID playerId) {
        // No-op for mock implementation as there's no real cache
        // In a real implementation, this would clear cached data
    }
    
    /**
     * Marks a node as active (in-progress) for a player.
     * <p>
     * This is a test utility method not part of the ICompletionTracker interface.
     * </p>
     *
     * @param playerId Player UUID
     * @param nodeIdentifier Node identifier to mark as active
     */
    public void markActive(@NotNull UUID playerId, @NotNull String nodeIdentifier) {
        activeNodes.computeIfAbsent(playerId, k -> ConcurrentHashMap.newKeySet())
            .add(nodeIdentifier);
    }
    
    /**
     * Removes a node from active status for a player.
     * <p>
     * This is a test utility method not part of the ICompletionTracker interface.
     * </p>
     *
     * @param playerId Player UUID
     * @param nodeIdentifier Node identifier to remove from active
     */
    public void removeActive(@NotNull UUID playerId, @NotNull String nodeIdentifier) {
        Set<String> active = activeNodes.get(playerId);
        if (active != null) {
            active.remove(nodeIdentifier);
        }
    }
    
    /**
     * Clears all completion and active data for a player.
     * <p>
     * This is a test utility method for resetting state between tests.
     * </p>
     *
     * @param playerId Player UUID
     */
    public void clearPlayer(@NotNull UUID playerId) {
        completedNodes.remove(playerId);
        activeNodes.remove(playerId);
    }
    
    /**
     * Clears all data for all players.
     * <p>
     * This is a test utility method for resetting state between tests.
     * </p>
     */
    public void clearAll() {
        completedNodes.clear();
        activeNodes.clear();
    }
    
    /**
     * Gets the number of completed nodes for a player.
     * <p>
     * This is a test utility method for assertions.
     * </p>
     *
     * @param playerId Player UUID
     * @return number of completed nodes
     */
    public int getCompletedCount(@NotNull UUID playerId) {
        Set<String> completed = completedNodes.get(playerId);
        return completed != null ? completed.size() : 0;
    }
    
    /**
     * Gets the number of active nodes for a player.
     * <p>
     * This is a test utility method for assertions.
     * </p>
     *
     * @param playerId Player UUID
     * @return number of active nodes
     */
    public int getActiveCount(@NotNull UUID playerId) {
        Set<String> active = activeNodes.get(playerId);
        return active != null ? active.size() : 0;
    }
}
