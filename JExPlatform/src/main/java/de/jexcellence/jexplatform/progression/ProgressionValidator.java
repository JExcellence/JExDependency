package de.jexcellence.jexplatform.progression;

import de.jexcellence.jexplatform.progression.ProgressionState.ProgressionStatus;
import de.jexcellence.jexplatform.progression.exception.CircularDependencyException;
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
 * Core service for validating progression prerequisites and determining node unlock status.
 *
 * <p>Handles prerequisite validation, node unlock determination, automatic unlocking
 * on completion, circular dependency detection, and progression state calculation.
 *
 * <p>Thread-safe: internal caches use {@link ConcurrentHashMap}.
 *
 * @param <T> the type of progression node being validated
 * @author JExcellence
 * @since 1.0.0
 */
public class ProgressionValidator<T extends ProgressionNode<T>> {

    private final CompletionTracker<T> completionTracker;
    private final Map<String, T> nodeCache;
    private final Map<String, Set<String>> prerequisiteCache;

    /**
     * Creates a progression validator.
     *
     * <p>Builds internal caches for fast prerequisite lookups. Nodes should not
     * be mutated after passing them to this constructor.
     *
     * @param completionTracker the completion tracker for querying player progress
     * @param nodes             all nodes in the progression system
     */
    public ProgressionValidator(
            @NotNull CompletionTracker<T> completionTracker,
            @NotNull Collection<T> nodes) {
        this.completionTracker = Objects.requireNonNull(completionTracker,
                "completionTracker cannot be null");
        Objects.requireNonNull(nodes, "nodes cannot be null");

        this.nodeCache = new ConcurrentHashMap<>();
        this.prerequisiteCache = new ConcurrentHashMap<>();

        nodes.forEach(node -> {
            nodeCache.put(node.identifier(), node);
            prerequisiteCache.put(
                    node.identifier(),
                    new HashSet<>(node.previousNodeIdentifiers()));
        });
    }

    /**
     * Checks whether a node is unlocked for a player.
     *
     * <p>A node is unlocked if it is an initial node or all prerequisites
     * have been completed. Does not check whether the node is already
     * completed or active.
     *
     * @param playerId       the player UUID
     * @param nodeIdentifier the node to check
     * @return a future resolving to {@code true} if unlocked
     */
    @NotNull
    public CompletableFuture<Boolean> isNodeUnlocked(
            @NotNull UUID playerId, @NotNull String nodeIdentifier) {
        var node = nodeCache.get(nodeIdentifier);
        if (node == null) {
            return CompletableFuture.completedFuture(false);
        }
        if (node.isInitialNode()) {
            return CompletableFuture.completedFuture(true);
        }
        return checkPrerequisitesCompleted(playerId, node.previousNodeIdentifiers());
    }

    /**
     * Returns the complete progression state for a node.
     *
     * @param playerId       the player UUID
     * @param nodeIdentifier the node to check
     * @return a future resolving to the progression state
     * @throws IllegalArgumentException if the node identifier is not found
     */
    @NotNull
    public CompletableFuture<ProgressionState<T>> getProgressionState(
            @NotNull UUID playerId,
            @NotNull String nodeIdentifier) {
        var node = nodeCache.get(nodeIdentifier);
        if (node == null) {
            throw new IllegalArgumentException("Node not found: " + nodeIdentifier);
        }

        return completionTracker.hasCompleted(playerId, nodeIdentifier)
                .thenCompose(completed -> {
                    if (completed) {
                        return CompletableFuture.completedFuture(
                                new ProgressionState<>(node, ProgressionStatus.COMPLETED,
                                        List.of()));
                    }

                    return completionTracker.isActive(playerId, nodeIdentifier)
                            .thenCompose(active -> {
                                if (active) {
                                    return CompletableFuture.completedFuture(
                                            new ProgressionState<>(node,
                                                    ProgressionStatus.ACTIVE, List.of()));
                                }
                                if (node.isInitialNode()) {
                                    return CompletableFuture.completedFuture(
                                            new ProgressionState<>(node,
                                                    ProgressionStatus.AVAILABLE, List.of()));
                                }
                                return getMissingPrerequisites(playerId,
                                        node.previousNodeIdentifiers())
                                        .thenApply(missing -> {
                                            var status = missing.isEmpty()
                                                    ? ProgressionStatus.AVAILABLE
                                                    : ProgressionStatus.LOCKED;
                                            return new ProgressionState<>(node, status,
                                                    missing);
                                        });
                            });
                });
    }

    /**
     * Returns all unlocked nodes from a collection.
     *
     * @param playerId the player UUID
     * @param nodes    the nodes to check
     * @return a future resolving to the list of unlocked nodes
     */
    @NotNull
    public CompletableFuture<List<T>> getUnlockedNodes(
            @NotNull UUID playerId,
            @NotNull Collection<T> nodes) {
        var futures = nodes.stream()
                .map(node -> getProgressionState(playerId, node.identifier()))
                .toList();

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .filter(ProgressionState::isUnlocked)
                        .map(ProgressionState::node)
                        .toList());
    }

    /**
     * Processes node completion and returns newly unlocked dependents.
     *
     * @param playerId                the player UUID
     * @param completedNodeIdentifier the identifier of the completed node
     * @return a future resolving to newly unlocked nodes (may be empty)
     */
    @NotNull
    public CompletableFuture<List<T>> processCompletion(
            @NotNull UUID playerId,
            @NotNull String completedNodeIdentifier) {
        var completedNode = nodeCache.get(completedNodeIdentifier);
        if (completedNode == null) {
            return CompletableFuture.completedFuture(List.of());
        }

        var dependentNodes = completedNode.nextNodeIdentifiers().stream()
                .map(nodeCache::get)
                .filter(Objects::nonNull)
                .toList();

        var unlockFutures = dependentNodes.stream()
                .map(node -> checkAndReturnIfUnlocked(playerId, node))
                .toList();

        return CompletableFuture.allOf(unlockFutures.toArray(CompletableFuture[]::new))
                .thenApply(v -> unlockFutures.stream()
                        .map(CompletableFuture::join)
                        .filter(Objects::nonNull)
                        .toList());
    }

    /**
     * Validates all prerequisite chains for circular dependencies using DFS.
     *
     * <p>Should be called during plugin initialization to ensure the progression
     * configuration forms a directed acyclic graph.
     *
     * @throws CircularDependencyException if a circular dependency is detected
     */
    public void validatePrerequisiteChains() {
        var visited = new HashSet<String>();
        var recursionStack = new HashSet<String>();

        for (var nodeId : nodeCache.keySet()) {
            if (hasCircularDependency(nodeId, visited, recursionStack)) {
                throw new CircularDependencyException(
                        "Circular dependency detected in progression chain involving: "
                                + nodeId);
            }
        }
    }

    /**
     * Invalidates cached data for a player.
     *
     * @param playerId the player UUID
     */
    public void invalidatePlayerCache(@NotNull UUID playerId) {
        completionTracker.invalidateCache(playerId);
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private CompletableFuture<Boolean> checkPrerequisitesCompleted(
            UUID playerId, List<String> prerequisiteIds) {
        if (prerequisiteIds.isEmpty()) {
            return CompletableFuture.completedFuture(true);
        }

        var checks = prerequisiteIds.stream()
                .map(prereqId -> completionTracker.hasCompleted(playerId, prereqId))
                .toList();

        return CompletableFuture.allOf(checks.toArray(CompletableFuture[]::new))
                .thenApply(v -> checks.stream().allMatch(CompletableFuture::join));
    }

    private CompletableFuture<List<String>> getMissingPrerequisites(
            UUID playerId, List<String> prerequisiteIds) {
        if (prerequisiteIds.isEmpty()) {
            return CompletableFuture.completedFuture(List.of());
        }

        var checks = prerequisiteIds.stream()
                .map(prereqId -> completionTracker.hasCompleted(playerId, prereqId)
                        .thenApply(completed -> Map.entry(prereqId, completed)))
                .toList();

        return CompletableFuture.allOf(checks.toArray(CompletableFuture[]::new))
                .thenApply(v -> checks.stream()
                        .map(CompletableFuture::join)
                        .filter(entry -> !entry.getValue())
                        .map(Map.Entry::getKey)
                        .toList());
    }

    private CompletableFuture<T> checkAndReturnIfUnlocked(UUID playerId, T node) {
        return checkPrerequisitesCompleted(playerId, node.previousNodeIdentifiers())
                .thenApply(allCompleted -> allCompleted ? node : null);
    }

    private boolean hasCircularDependency(
            String nodeId, Set<String> visited, Set<String> recursionStack) {
        if (recursionStack.contains(nodeId)) {
            return true;
        }
        if (visited.contains(nodeId)) {
            return false;
        }

        visited.add(nodeId);
        recursionStack.add(nodeId);

        var prerequisites = prerequisiteCache.get(nodeId);
        if (prerequisites != null) {
            for (var prereqId : prerequisites) {
                if (hasCircularDependency(prereqId, visited, recursionStack)) {
                    return true;
                }
            }
        }

        recursionStack.remove(nodeId);
        return false;
    }
}
