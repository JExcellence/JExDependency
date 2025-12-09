package com.raindropcentral.rdq2.view.rank.cache;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class ProgressViewCache<TId, TNode, TStatus> {

    private Map<TId, TNode> nodeGraph;
    private Map<TId, Object> nodeWorldPositions;
    private Map<TId, TStatus> nodeStatuses;
    private Set<TId> ownedNodeIds;
    private Set<TId> pendingNodeIds;
    private long lastRefreshAt;

    public ProgressViewCache() {
        this.nodeGraph = new HashMap<>();
        this.nodeWorldPositions = new HashMap<>();
        this.nodeStatuses = new HashMap<>();
        this.ownedNodeIds = new HashSet<>();
        this.pendingNodeIds = new HashSet<>();
        this.lastRefreshAt = 0L;
    }

    public void initialize(
            final @NotNull Map<TId, TNode> graph,
            final @NotNull Map<TId, Object> positions,
            final @NotNull Map<TId, TStatus> statuses,
            final @NotNull Set<TId> owned,
            final @NotNull Set<TId> pending
    ) {
        this.nodeGraph = new HashMap<>(graph);
        this.nodeWorldPositions = new HashMap<>(positions);
        this.nodeStatuses = new HashMap<>(statuses);
        this.ownedNodeIds = new HashSet<>(owned);
        this.pendingNodeIds = new HashSet<>(pending);
        this.lastRefreshAt = System.currentTimeMillis();
    }

    public void clear() {
        this.nodeGraph.clear();
        this.nodeWorldPositions.clear();
        this.nodeStatuses.clear();
        this.ownedNodeIds.clear();
        this.pendingNodeIds.clear();
        this.lastRefreshAt = 0L;
    }

    public boolean needsRefresh(final long maxAgeMillis) {
        return System.currentTimeMillis() - lastRefreshAt > maxAgeMillis;
    }

    public @NotNull Map<TId, TNode> getNodeGraph() {
        return Collections.unmodifiableMap(nodeGraph);
    }

    public @NotNull Map<TId, Object> getNodeWorldPositions() {
        return Collections.unmodifiableMap(nodeWorldPositions);
    }

    public @NotNull Map<TId, TStatus> getNodeStatuses() {
        return Collections.unmodifiableMap(nodeStatuses);
    }

    public @NotNull Set<TId> getOwnedNodeIds() {
        return Collections.unmodifiableSet(ownedNodeIds);
    }

    public @NotNull Set<TId> getPendingNodeIds() {
        return Collections.unmodifiableSet(pendingNodeIds);
    }

    public long getLastRefreshAt() {
        return lastRefreshAt;
    }
}