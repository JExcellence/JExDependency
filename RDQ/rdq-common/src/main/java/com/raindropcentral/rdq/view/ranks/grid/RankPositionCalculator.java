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

package com.raindropcentral.rdq.view.ranks.grid;

import com.raindropcentral.rdq.view.ranks.hierarchy.RankNode;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Calculates world positions for ranks in the grid layout.
 */
public class RankPositionCalculator {
	
	private static final Logger LOGGER = CentralLogger.getLoggerByName("RDQ");
	
	private static final GridPosition INITIAL_RANK_POSITION = new GridPosition(
		3,
		2
	);
	private static final int          RANK_SPACING_DISTANCE = 5;
	
	/**
	 * Calculates world positions for all ranks in the hierarchy.
	 */
	public @NotNull Map<String, GridPosition> calculatePositions(
		final @NotNull Map<String, RankNode> rankNodeHierarchy
	) {
		
		final Map<String, GridPosition> worldPositions  = new HashMap<>();
		final Set<String>               positionedRanks = new HashSet<>();
		
		try {
			final List<RankNode> initialRanks = this.findInitialRanks(rankNodeHierarchy);
			
			if (
				initialRanks.isEmpty()
			) {
				LOGGER.log(
					Level.WARNING,
					"No initial ranks found in hierarchy"
				);
				return worldPositions;
			}
			
			LOGGER.log(
				Level.FINE,
				"Found " + initialRanks.size() + " initial ranks"
			);
			
			if (
				initialRanks.size() == 1
			) {
				this.positionSingleInitialRank(
					initialRanks.get(0),
					worldPositions,
					positionedRanks,
					rankNodeHierarchy
				);
			} else {
				this.positionMultipleInitialRanks(
					initialRanks,
					worldPositions,
					positionedRanks,
					rankNodeHierarchy
				);
			}
		} catch (
			final Exception exception
		) {
			LOGGER.log(
				Level.WARNING,
				"Error calculating rank world positions",
				exception
			);
		}
		
		return worldPositions;
	}
	
	private @NotNull List<RankNode> findInitialRanks(final @NotNull Map<String, RankNode> rankNodeHierarchy) {
		
		return rankNodeHierarchy.values().stream()
		                        .filter(RankNode::isRoot)
		                        .sorted(Comparator.comparingInt(node -> node.rank.getTier()))
		                        .toList();
	}
	
	private void positionSingleInitialRank(
		final @NotNull RankNode initialRank,
		final @NotNull Map<String, GridPosition> worldPositions,
		final @NotNull Set<String> positionedRanks,
		final @NotNull Map<String, RankNode> rankNodeHierarchy
	) {
		
		worldPositions.put(
			initialRank.rank.getIdentifier(),
			INITIAL_RANK_POSITION
		);
		positionedRanks.add(initialRank.rank.getIdentifier());
		
		final Integer slotNumber = GridSlotMapper.getSlotForPosition(INITIAL_RANK_POSITION);
		LOGGER.log(
			Level.FINE,
			"Positioned single initial rank '" + initialRank.rank.getIdentifier() + "' at center: " + INITIAL_RANK_POSITION + " (slot " + slotNumber + ")"
		);
		
		this.positionRankChildren(
			initialRank,
			INITIAL_RANK_POSITION,
			worldPositions,
			positionedRanks,
			rankNodeHierarchy
		);
	}
	
	private void positionMultipleInitialRanks(
		final @NotNull List<RankNode> initialRanks,
		final @NotNull Map<String, GridPosition> worldPositions,
		final @NotNull Set<String> positionedRanks,
		final @NotNull Map<String, RankNode> rankNodeHierarchy
	) {
		
		final int startX = INITIAL_RANK_POSITION.x - ((initialRanks.size() - 1) * RANK_SPACING_DISTANCE / 2);
		
		for (
			int i = 0; i < initialRanks.size(); i++
		) {
			final RankNode initialRank = initialRanks.get(i);
			final GridPosition position = new GridPosition(
				startX + (i * RANK_SPACING_DISTANCE),
				INITIAL_RANK_POSITION.y
			);
			
			worldPositions.put(
				initialRank.rank.getIdentifier(),
				position
			);
			positionedRanks.add(initialRank.rank.getIdentifier());
			
			final Integer slotNumber = GridSlotMapper.getSlotForPosition(position);
			LOGGER.log(
				Level.FINE,
				"Positioned initial rank '" + initialRank.rank.getIdentifier() + "' at: " + position + " (slot " + slotNumber + ")"
			);
			
			this.positionRankChildren(
				initialRank,
				position,
				worldPositions,
				positionedRanks,
				rankNodeHierarchy
			);
		}
	}
	
	private void positionRankChildren(
		final @NotNull RankNode parentNode,
		final @NotNull GridPosition parentPosition,
		final @NotNull Map<String, GridPosition> worldPositions,
		final @NotNull Set<String> positionedRanks,
		final @NotNull Map<String, RankNode> allNodes
	) {
		
		if (
			parentNode.children.isEmpty()
		) {
			return;
		}
		
		final List<RankNode> sortedChildren = new ArrayList<>(parentNode.children);
		sortedChildren.sort(Comparator.comparingInt(node -> node.rank.getTier()));
		
		if (
			sortedChildren.size() == 1
		) {
			this.positionSingleChild(
				sortedChildren.get(0),
				parentPosition,
				worldPositions,
				positionedRanks,
				allNodes
			);
		} else {
			this.positionMultipleChildren(
				sortedChildren,
				parentPosition,
				worldPositions,
				positionedRanks,
				allNodes
			);
		}
	}
	
	private void positionSingleChild(
		final @NotNull RankNode childNode,
		final @NotNull GridPosition parentPosition,
		final @NotNull Map<String, GridPosition> worldPositions,
		final @NotNull Set<String> positionedRanks,
		final @NotNull Map<String, RankNode> allNodes
	) {
		
		if (
			positionedRanks.contains(childNode.rank.getIdentifier())
		) {
			return;
		}
		
		final GridPosition childPosition = parentPosition.offset(
			0,
			RANK_SPACING_DISTANCE
		);
		worldPositions.put(
			childNode.rank.getIdentifier(),
			childPosition
		);
		positionedRanks.add(childNode.rank.getIdentifier());
		
		final Integer slotNumber = GridSlotMapper.getSlotForPosition(childPosition);
		LOGGER.log(
			Level.FINE,
			"Positioned single child '" + childNode.rank.getIdentifier() + "' below parent at: " + childPosition + " (slot " + slotNumber + ")"
		);
		
		this.positionRankChildren(
			childNode,
			childPosition,
			worldPositions,
			positionedRanks,
			allNodes
		);
	}
	
	private void positionMultipleChildren(
		final @NotNull List<RankNode> children,
		final @NotNull GridPosition parentPosition,
		final @NotNull Map<String, GridPosition> worldPositions,
		final @NotNull Set<String> positionedRanks,
		final @NotNull Map<String, RankNode> allNodes
	) {
		
		final int childY     = parentPosition.y + RANK_SPACING_DISTANCE;
		final int totalWidth = (children.size() - 1) * RANK_SPACING_DISTANCE;
		final int startX     = parentPosition.x - (totalWidth / 2);
		
		for (
			int i = 0; i < children.size(); i++
		) {
			final RankNode child = children.get(i);
			if (
				positionedRanks.contains(child.rank.getIdentifier())
			) {
				continue;
			}
			
			final GridPosition childPosition = new GridPosition(
				startX + (i * RANK_SPACING_DISTANCE),
				childY
			);
			worldPositions.put(
				child.rank.getIdentifier(),
				childPosition
			);
			positionedRanks.add(child.rank.getIdentifier());
			
			final Integer slotNumber = GridSlotMapper.getSlotForPosition(childPosition);
			LOGGER.log(
				Level.FINE,
				"Positioned child '" + child.rank.getIdentifier() + "' at balanced position: " + childPosition + " (slot " + slotNumber + ")"
			);
			
			this.positionRankChildren(
				child,
				childPosition,
				worldPositions,
				positionedRanks,
				allNodes
			);
		}
	}
	
}
