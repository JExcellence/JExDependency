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

package com.raindropcentral.rdq.view.ranks.hierarchy;

import com.raindropcentral.rdq.database.entity.rank.RRank;
import com.raindropcentral.rdq.database.entity.rank.RRankTree;
import com.raindropcentral.rplatform.logging.CentralLogger;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Builds rank node hierarchies from rank tree data.
 */
public class RankHierarchyBuilder {
	
	private static final Logger LOGGER = CentralLogger.getLoggerByName("RDQ");
	
	/**
	 * Builds a complete rank node hierarchy from the given rank tree.
	 *
	 * @param rankTree The rank tree to build hierarchy from
	 *
	 * @return A map of rank identifiers to their corresponding nodes
	 */
	public @NotNull Map<String, RankNode> buildHierarchy(
		final @NotNull RRankTree rankTree
	) {
		
		final Map<String, RankNode> nodeHierarchy = new HashMap<>();
		
		try {
			for (final RRank rank : rankTree.getRanks()) {
				nodeHierarchy.put(
					rank.getIdentifier(),
					new RankNode(rank)
				);
			}
			
			for (final RRank rank : rankTree.getRanks()) {
				final RankNode currentNode = nodeHierarchy.get(rank.getIdentifier());
				if (currentNode != null) {
					this.establishNodeRelationships(
						currentNode,
						rank,
						nodeHierarchy
					);
				}
			}
			
			LOGGER.log(
				Level.FINE,
				"Built hierarchy with {} nodes",
				nodeHierarchy.size()
			);
		} catch (final Exception exception) {
			LOGGER.log(
				Level.WARNING,
				"Error building rank node hierarchy",
				exception
			);
		}
		
		return nodeHierarchy;
	}
	
	/**
	 * Establishes parent-child relationships for a rank node.
	 */
	private void establishNodeRelationships(
		final @NotNull RankNode currentNode,
		final @NotNull RRank rank,
		final @NotNull Map<String, RankNode> nodeHierarchy
	) {
		
		for (
			final String nextRankId : rank.getNextRanks()
		) {
			final RankNode childNode = nodeHierarchy.get(nextRankId);
			if (
				childNode != null
			) {
				currentNode.children.add(childNode);
				childNode.parents.add(currentNode);
			} else {
				LOGGER.log(
					Level.WARNING,
					"Child rank not found: {}",
					nextRankId
				);
			}
		}
		
		for (
			final String previousRankId : rank.getPreviousRanks()
		) {
			final RankNode parentNode = nodeHierarchy.get(previousRankId);
			if (
				parentNode != null &&
				! currentNode.parents.contains(parentNode)
			) {
				currentNode.parents.add(parentNode);
				if (
					! parentNode.children.contains(currentNode)
				) {
					parentNode.children.add(currentNode);
				}
			}
		}
	}
	
}