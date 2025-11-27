package com.raindropcentral.rdq.view.ranks.cache;

import com.raindropcentral.rdq.RDQImpl;
import com.raindropcentral.rdq.database.entity.RDQPlayer;
import com.raindropcentral.rdq.database.entity.rank.RRankTree;
import com.raindropcentral.rdq.type.ERankStatus;
import com.raindropcentral.rdq.view.ranks.grid.GridPosition;
import com.raindropcentral.rdq.view.ranks.grid.RankPositionCalculator;
import com.raindropcentral.rdq.view.ranks.hierarchy.RankHierarchyBuilder;
import com.raindropcentral.rdq.view.ranks.hierarchy.RankNode;
import com.raindropcentral.rplatform.logger.CentralLogger;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages cached data for rank path overview to avoid repeated database queries.
 */
public class RankDataCache {
	
	private static final Logger LOGGER = CentralLogger.getLogger(RankDataCache.class.getName());
	
	private final RankHierarchyBuilder hierarchyBuilder = new RankHierarchyBuilder();
	private final RankPositionCalculator positionCalculator = new RankPositionCalculator();
	
	private Map<String, RankNode> cachedRankHierarchy = new HashMap<>();
	private Map<String, GridPosition> cachedWorldPositions = new HashMap<>();
	private Map<String, ERankStatus>  cachedRankStatuses   = new HashMap<>();
	private Set<String>               cachedOwnedRanks     = new HashSet<>();
	private Set<String> cachedInProgressRanks = new HashSet<>();
	private long lastRefreshTimestamp = 0L;
	
	/**
	 * Initializes and caches all necessary data.
	 */
	public void initializeCache(
		final @NotNull RRankTree rankTree,
		final @NotNull RDQImpl plugin,
		final @NotNull RDQPlayer rdqPlayer,
		final boolean previewMode
	) {
		try {
			LOGGER.log(Level.FINE, "Initializing rank data cache...");
			
			// Build and cache rank hierarchy
			this.cachedRankHierarchy = this.hierarchyBuilder.buildHierarchy(rankTree);
			
			// Calculate and cache world positions
			this.cachedWorldPositions = this.positionCalculator.calculatePositions(this.cachedRankHierarchy);
			
			if (!previewMode) {
				// Pre-load player progression data
				this.cachedOwnedRanks = this.loadOwnedRanks(plugin, rdqPlayer, rankTree);
				this.cachedInProgressRanks = this.loadInProgressRanks(plugin, rdqPlayer, rankTree);
				
				// Calculate and cache all rank statuses
				this.cachedRankStatuses = this.calculateAllRankStatuses(previewMode);
				
				LOGGER.log(Level.FINE, "Cached data: " + this.cachedOwnedRanks.size() + " owned ranks, " + this.cachedInProgressRanks.size() + " in-progress ranks");
			} else {
				// Preview mode - simple status calculation
				this.cachedRankStatuses = this.calculatePreviewStatuses();
			}
			
			this.lastRefreshTimestamp = System.currentTimeMillis();
			LOGGER.log(Level.FINE, "Data caching completed successfully");
			
		} catch (final Exception exception) {
			LOGGER.log(Level.SEVERE, "Failed to initialize cache", exception);
			this.clearCache();
		}
	}
	
	/**
	 * Clears all cached data.
	 */
	public void clearCache() {
		this.cachedRankHierarchy.clear();
		this.cachedWorldPositions.clear();
		this.cachedRankStatuses.clear();
		this.cachedOwnedRanks.clear();
		this.cachedInProgressRanks.clear();
		this.lastRefreshTimestamp = 0L;
	}
	
	/**
	 * Checks if the cache needs refreshing based on time elapsed.
	 */
	public boolean needsRefresh(final long maxAgeMillis) {
		return System.currentTimeMillis() - this.lastRefreshTimestamp > maxAgeMillis;
	}
	
	// Getters for cached data
	public @NotNull Map<String, RankNode> getRankHierarchy() {
		return new HashMap<>(this.cachedRankHierarchy);
	}
	
	public @NotNull Map<String, GridPosition> getWorldPositions() {
		return new HashMap<>(this.cachedWorldPositions);
	}
	
	public @NotNull Map<String, ERankStatus> getRankStatuses() {
		return new HashMap<>(this.cachedRankStatuses);
	}
	
	public @NotNull Set<String> getOwnedRanks() {
		return new HashSet<>(this.cachedOwnedRanks);
	}
	
	public @NotNull Set<String> getInProgressRanks() {
		return new HashSet<>(this.cachedInProgressRanks);
	}
	
	public long getLastRefreshTimestamp() {
		return this.lastRefreshTimestamp;
	}
	
	private @NotNull Set<String> loadOwnedRanks(
		final @NotNull RDQImpl plugin,
		final @NotNull RDQPlayer rdqPlayer,
		final @NotNull RRankTree rankTree
	) {
		// Implementation moved from main class
		// This would contain the same logic as before
		return new HashSet<>(); // Placeholder
	}
	
	private @NotNull Set<String> loadInProgressRanks(
		final @NotNull RDQImpl plugin,
		final @NotNull RDQPlayer rdqPlayer,
		final @NotNull RRankTree rankTree
	) {
		// Implementation moved from main class
		// This would contain the same logic as before
		return new HashSet<>(); // Placeholder
	}
	
	private @NotNull Map<String, ERankStatus> calculateAllRankStatuses(final boolean previewMode) {
		final Map<String, ERankStatus> statuses = new HashMap<>();
		
		if (previewMode) {
			return this.calculatePreviewStatuses();
		}
		
		for (final Map.Entry<String, RankNode> entry : this.cachedRankHierarchy.entrySet()) {
			final String rankId = entry.getKey();
			final RankNode rankNode = entry.getValue();
			
			if (this.cachedOwnedRanks.contains(rankId)) {
				statuses.put(rankId, ERankStatus.OWNED);
			} else if (this.cachedInProgressRanks.contains(rankId)) {
				statuses.put(rankId, ERankStatus.IN_PROGRESS);
			} else if (this.arePrerequisitesMet(rankNode)) {
				statuses.put(rankId, ERankStatus.AVAILABLE);
			} else {
				statuses.put(rankId, ERankStatus.LOCKED);
			}
		}
		
		return statuses;
	}
	
	private @NotNull Map<String, ERankStatus> calculatePreviewStatuses() {
		final Map<String, ERankStatus> previewStatuses = new HashMap<>();
		for (
			final Map.Entry<String, RankNode> entry : this.cachedRankHierarchy.entrySet()
		) {
			final RankNode rankNode = entry.getValue();
			previewStatuses.put(entry.getKey(), rankNode.isRoot() ? ERankStatus.OWNED : ERankStatus.AVAILABLE);
		}
		return previewStatuses;
	}
	
	private boolean arePrerequisitesMet(
		final @NotNull RankNode rankNode
	) {
		if (rankNode.parents.isEmpty() || rankNode.rank.isInitialRank()) {
			return true;
		}
		
		return rankNode.parents.stream()
		                       .anyMatch(parent -> this.cachedOwnedRanks.contains(parent.rank.getIdentifier()));
	}
}