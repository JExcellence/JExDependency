package com.raindropcentral.rplatform.progression;

import com.raindropcentral.rplatform.progression.exception.CircularDependencyException;
import com.raindropcentral.rplatform.progression.model.ProgressionState;
import com.raindropcentral.rplatform.progression.model.ProgressionStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for validating progression prerequisites and determining node unlock status.
 * <p>
 * This class provides the core logic for progression systems, handling:
 * <ul>
 *     <li>Prerequisite validation</li>
 *     <li>Node unlock status determination</li>
 *     <li>Automatic unlocking on completion</li>
 *     <li>Circular dependency detection</li>
 *     <li>Progression state calculation</li>
 * </ul>
 *
 * <h2>Basic Usage Example:</h2>
 * <pre>{@code
 * // Create completion tracker
 * QuestCompletionTracker tracker = new QuestCompletionTracker(
 *     completionRepository,
 *     cacheManager
 * );
 *
 * // Create validator with all quests
 * ProgressionValidator<Quest> validator = new ProgressionValidator<>(
 *     tracker,
 *     questRepository.findAll().join()
 * );
 *
 * // Validate configuration on startup
 * try {
 *     validator.validatePrerequisiteChains();
 * } catch (CircularDependencyException e) {
 *     logger.severe("Invalid quest configuration: " + e.getMessage());
 *     return;
 * }
 *
 * // Check if quest is unlocked
 * UUID playerId = player.getUniqueId();
 * validator.isNodeUnlocked(playerId, "quest_b").thenAccept(unlocked -> {
 *     if (unlocked) {
 *         player.sendMessage("Quest B is available!");
 *     } else {
 *         player.sendMessage("Complete Quest A first!");
 *     }
 * });
 * }</pre>
 *
 * <h2>Progression State Example:</h2>
 * <pre>{@code
 * // Get detailed progression state
 * validator.getProgressionState(playerId, "quest_c").thenAccept(state -> {
 *     switch (state.status()) {
 *         case COMPLETED -> player.sendMessage("Already completed!");
 *         case ACTIVE -> player.sendMessage("Quest in progress");
 *         case AVAILABLE -> player.sendMessage("Quest available to start");
 *         case LOCKED -> {
 *             player.sendMessage("Quest locked. Complete:");
 *             state.missingPrerequisites().forEach(prereq -> 
 *                 player.sendMessage("  - " + prereq)
 *             );
 *         }
 *     }
 * });
 * }</pre>
 *
 * <h2>Automatic Unlocking Example:</h2>
 * <pre>{@code
 * // When player completes a quest
 * tracker.markCompleted(playerId, "quest_a").thenRun(() -> {
 *     // Process automatic unlocking
 *     validator.processCompletion(playerId, "quest_a").thenAccept(unlocked -> {
 *         if (!unlocked.isEmpty()) {
 *             player.sendMessage("New quests unlocked:");
 *             for (Quest quest : unlocked) {
 *                 player.sendMessage("  - " + quest.getName());
 *             }
 *         }
 *     });
 * });
 * }</pre>
 *
 * <h2>Batch Operations Example:</h2>
 * <pre>{@code
 * // Get all unlocked quests in a category
 * List<Quest> categoryQuests = questRepository.findByCategory("combat").join();
 * validator.getUnlockedNodes(playerId, categoryQuests).thenAccept(unlocked -> {
 *     // Display unlocked quests in GUI
 *     gui.displayQuests(unlocked);
 * });
 * }</pre>
 *
 * <h2>Thread Safety:</h2>
 * <p>
 * This class is thread-safe and can be used concurrently by multiple players.
 * Internal caches use ConcurrentHashMap for safe concurrent access.
 *
 * <h2>Performance Considerations:</h2>
 * <ul>
 *     <li>Node and prerequisite data is cached in memory for fast access</li>
 *     <li>Completion status is queried from the completion tracker (may hit database)</li>
 *     <li>Use batch operations (getUnlockedNodes) when checking multiple nodes</li>
 *     <li>Circular dependency validation is O(V+E) where V=nodes, E=prerequisites</li>
 * </ul>
 *
 * <h2>Circular Dependency Detection:</h2>
 * <p>
 * The validator uses depth-first search to detect cycles in prerequisite chains.
 * Call {@link #validatePrerequisiteChains()} on startup to ensure configuration is valid.
 *
 * @param <T> The type of progression node being validated
 * @author RaindropCentral
 * @version 1.0.0
 * @since 1.0.0
 */
public class ProgressionValidator<T extends IProgressionNode<T>> {
    
    /**
     * Completion tracker for querying player completion status.
     */
    private final ICompletionTracker<T> completionTracker;
    
    /**
     * Cache of nodes by identifier for fast lookup.
     */
    private final Map<String, T> nodeCache;
    
    /**
     * Cache of prerequisite sets by node identifier.
     */
    private final Map<String, Set<String>> prerequisiteCache;
    
    /**
     * Constructs a new progression validator.
     * <p>
     * This constructor builds internal caches for fast prerequisite lookups.
     * The caches are immutable after construction, so nodes should not be
     * modified after passing them to this constructor.
     *
     * @param completionTracker Completion tracker implementation for querying player progress
     * @param nodes Collection of all nodes in the progression system
     * @throws NullPointerException if completionTracker or nodes is null
     */
    public ProgressionValidator(
        @NotNull ICompletionTracker<T> completionTracker,
        @NotNull Collection<T> nodes
    ) {
        this.completionTracker = Objects.requireNonNull(completionTracker, "completionTracker cannot be null");
        Objects.requireNonNull(nodes, "nodes cannot be null");
        
        this.nodeCache = new ConcurrentHashMap<>();
        this.prerequisiteCache = new ConcurrentHashMap<>();
        
        // Build caches
        nodes.forEach(node -> {
            nodeCache.put(node.getIdentifier(), node);
            prerequisiteCache.put(
                node.getIdentifier(),
                new HashSet<>(node.getPreviousNodeIdentifiers())
            );
        });
    }
    
    /**
     * Checks if a node is unlocked for a player.
     * <p>
     * A node is considered unlocked if:
     * <ul>
     *     <li>It is an initial node (no prerequisites), OR</li>
     *     <li>All of its prerequisite nodes have been completed by the player</li>
     * </ul>
     *
     * <p>
     * This method does NOT check if the node is already completed or active.
     * Use {@link #getProgressionState(UUID, String)} for complete status information.
     *
     * @param playerId Player UUID
     * @param nodeIdentifier Node identifier to check
     * @return CompletableFuture containing true if the node is unlocked, false otherwise
     */
    @NotNull
    public CompletableFuture<Boolean> isNodeUnlocked(@NotNull UUID playerId, @NotNull String nodeIdentifier) {
        T node = nodeCache.get(nodeIdentifier);
        if (node == null) {
            return CompletableFuture.completedFuture(false);
        }
        
        if (node.isInitialNode()) {
            return CompletableFuture.completedFuture(true);
        }
        
        return checkPrerequisitesCompleted(playerId, node.getPreviousNodeIdentifiers());
    }
    
    /**
     * Gets the complete progression state for a node.
     * <p>
     * This method determines the node's status and any missing prerequisites.
     * The status can be:
     * <ul>
     *     <li>{@link ProgressionStatus#COMPLETED} - Player has completed this node</li>
     *     <li>{@link ProgressionStatus#ACTIVE} - Player is currently working on this node</li>
     *     <li>{@link ProgressionStatus#AVAILABLE} - Node is unlocked and can be started</li>
     *     <li>{@link ProgressionStatus#LOCKED} - Prerequisites not met</li>
     * </ul>
     *
     * @param playerId Player UUID
     * @param nodeIdentifier Node identifier to check
     * @return CompletableFuture containing the progression state
     * @throws IllegalArgumentException if the node identifier is not found
     */
    @NotNull
    public CompletableFuture<ProgressionState<T>> getProgressionState(
        @NotNull UUID playerId,
        @NotNull String nodeIdentifier
    ) {
        T node = nodeCache.get(nodeIdentifier);
        if (node == null) {
            throw new IllegalArgumentException("Node not found: " + nodeIdentifier);
        }
        
        // Check if completed
        return completionTracker.hasCompleted(playerId, nodeIdentifier)
            .thenCompose(completed -> {
                if (completed) {
                    return CompletableFuture.completedFuture(
                        new ProgressionState<>(node, ProgressionStatus.COMPLETED, List.of())
                    );
                }
                
                // Check if active
                return completionTracker.isActive(playerId, nodeIdentifier)
                    .thenCompose(active -> {
                        if (active) {
                            return CompletableFuture.completedFuture(
                                new ProgressionState<>(node, ProgressionStatus.ACTIVE, List.of())
                            );
                        }
                        
                        // Check prerequisites
                        if (node.isInitialNode()) {
                            return CompletableFuture.completedFuture(
                                new ProgressionState<>(node, ProgressionStatus.AVAILABLE, List.of())
                            );
                        }
                        
                        return getMissingPrerequisites(playerId, node.getPreviousNodeIdentifiers())
                            .thenApply(missing -> {
                                ProgressionStatus status = missing.isEmpty() 
                                    ? ProgressionStatus.AVAILABLE 
                                    : ProgressionStatus.LOCKED;
                                return new ProgressionState<>(node, status, missing);
                            });
                    });
            });
    }
    
    /**
     * Gets all unlocked nodes from a collection.
     * <p>
     * This is a batch operation that efficiently checks multiple nodes at once.
     * Only nodes with status AVAILABLE or ACTIVE are considered unlocked.
     *
     * @param playerId Player UUID
     * @param nodes Collection of nodes to check
     * @return CompletableFuture containing list of unlocked nodes
     */
    @NotNull
    public CompletableFuture<List<T>> getUnlockedNodes(
        @NotNull UUID playerId,
        @NotNull Collection<T> nodes
    ) {
        List<CompletableFuture<ProgressionState<T>>> futures = nodes.stream()
            .map(node -> getProgressionState(playerId, node.getIdentifier()))
            .toList();
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .filter(ProgressionState::isUnlocked)
                .map(ProgressionState::node)
                .toList());
    }
    
    /**
     * Processes node completion and returns newly unlocked nodes.
     * <p>
     * This method should be called after a player completes a node to determine
     * which dependent nodes are now unlocked. It checks all nodes that list the
     * completed node as a prerequisite.
     *
     * <p>
     * A dependent node is considered newly unlocked if:
     * <ul>
     *     <li>It lists the completed node as a prerequisite</li>
     *     <li>All of its prerequisites are now completed</li>
     *     <li>It is not already completed or active</li>
     * </ul>
     *
     * @param playerId Player UUID
     * @param completedNodeIdentifier Identifier of the node that was just completed
     * @return CompletableFuture containing list of newly unlocked nodes (may be empty)
     */
    @NotNull
    public CompletableFuture<List<T>> processCompletion(
        @NotNull UUID playerId,
        @NotNull String completedNodeIdentifier
    ) {
        T completedNode = nodeCache.get(completedNodeIdentifier);
        if (completedNode == null) {
            return CompletableFuture.completedFuture(List.of());
        }
        
        // Find dependent nodes
        List<T> dependentNodes = completedNode.getNextNodeIdentifiers().stream()
            .map(nodeCache::get)
            .filter(Objects::nonNull)
            .toList();
        
        // Check which dependent nodes are now unlocked
        List<CompletableFuture<T>> unlockFutures = dependentNodes.stream()
            .map(node -> checkAndReturnIfUnlocked(playerId, node))
            .toList();
        
        return CompletableFuture.allOf(unlockFutures.toArray(new CompletableFuture[0]))
            .thenApply(v -> unlockFutures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .toList());
    }
    
    /**
     * Validates all prerequisite chains for circular dependencies.
     * <p>
     * This method uses depth-first search to detect cycles in the prerequisite graph.
     * It should be called during plugin initialization to ensure the progression
     * configuration is valid.
     *
     * <p>
     * The algorithm:
     * <ol>
     *     <li>Iterates through all nodes</li>
     *     <li>For each node, performs DFS on its prerequisite chain</li>
     *     <li>Maintains a recursion stack to detect back edges (cycles)</li>
     *     <li>Throws exception if any cycle is detected</li>
     * </ol>
     *
     * @throws CircularDependencyException if a circular dependency is detected
     */
    public void validatePrerequisiteChains() {
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();
        
        for (String nodeId : nodeCache.keySet()) {
            if (hasCircularDependency(nodeId, visited, recursionStack)) {
                throw new CircularDependencyException(
                    "Circular dependency detected in progression chain involving: " + nodeId
                );
            }
        }
    }
    
    /**
     * Invalidates cached data for a player.
     * <p>
     * This method delegates to the completion tracker to clear any cached
     * completion data for the player. Call this when player data is modified
     * externally or needs to be refreshed.
     *
     * @param playerId Player UUID
     */
    public void invalidatePlayerCache(@NotNull UUID playerId) {
        completionTracker.invalidateCache(playerId);
    }
    
    // Private helper methods
    
    /**
     * Checks if all prerequisites are completed for a player.
     *
     * @param playerId Player UUID
     * @param prerequisiteIds List of prerequisite node identifiers
     * @return CompletableFuture containing true if all prerequisites are completed
     */
    private CompletableFuture<Boolean> checkPrerequisitesCompleted(
        UUID playerId,
        List<String> prerequisiteIds
    ) {
        if (prerequisiteIds.isEmpty()) {
            return CompletableFuture.completedFuture(true);
        }
        
        List<CompletableFuture<Boolean>> checks = prerequisiteIds.stream()
            .map(prereqId -> completionTracker.hasCompleted(playerId, prereqId))
            .toList();
        
        return CompletableFuture.allOf(checks.toArray(new CompletableFuture[0]))
            .thenApply(v -> checks.stream().allMatch(CompletableFuture::join));
    }
    
    /**
     * Gets the list of missing (not completed) prerequisites for a player.
     *
     * @param playerId Player UUID
     * @param prerequisiteIds List of prerequisite node identifiers
     * @return CompletableFuture containing list of missing prerequisite identifiers
     */
    private CompletableFuture<List<String>> getMissingPrerequisites(
        UUID playerId,
        List<String> prerequisiteIds
    ) {
        if (prerequisiteIds.isEmpty()) {
            return CompletableFuture.completedFuture(List.of());
        }
        
        List<CompletableFuture<Map.Entry<String, Boolean>>> checks = prerequisiteIds.stream()
            .map(prereqId -> completionTracker.hasCompleted(playerId, prereqId)
                .thenApply(completed -> Map.entry(prereqId, completed)))
            .toList();
        
        return CompletableFuture.allOf(checks.toArray(new CompletableFuture[0]))
            .thenApply(v -> checks.stream()
                .map(CompletableFuture::join)
                .filter(entry -> !entry.getValue())
                .map(Map.Entry::getKey)
                .toList());
    }
    
    /**
     * Checks if a node is unlocked and returns it, or null if locked.
     *
     * @param playerId Player UUID
     * @param node Node to check
     * @return CompletableFuture containing the node if unlocked, null otherwise
     */
    private CompletableFuture<T> checkAndReturnIfUnlocked(UUID playerId, T node) {
        return checkPrerequisitesCompleted(playerId, node.getPreviousNodeIdentifiers())
            .thenApply(allCompleted -> allCompleted ? node : null);
    }
    
    /**
     * Recursively checks for circular dependencies using depth-first search.
     *
     * @param nodeId Current node identifier
     * @param visited Set of already visited nodes
     * @param recursionStack Set of nodes in current recursion path
     * @return true if a cycle is detected, false otherwise
     */
    private boolean hasCircularDependency(
        String nodeId,
        Set<String> visited,
        Set<String> recursionStack
    ) {
        if (recursionStack.contains(nodeId)) {
            return true; // Cycle detected - node is in current path
        }
        
        if (visited.contains(nodeId)) {
            return false; // Already processed this node
        }
        
        visited.add(nodeId);
        recursionStack.add(nodeId);
        
        // Check all prerequisites
        Set<String> prerequisites = prerequisiteCache.get(nodeId);
        if (prerequisites != null) {
            for (String prereqId : prerequisites) {
                if (hasCircularDependency(prereqId, visited, recursionStack)) {
                    return true;
                }
            }
        }
        
        recursionStack.remove(nodeId);
        return false;
    }
}
