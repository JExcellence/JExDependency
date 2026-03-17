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
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a node in the rank hierarchy tree structure.
 * Contains references to parent and child ranks for navigation.
 */
public class RankNode {
	
	public final @NotNull RRank          rank;
	public final @NotNull List<RankNode> children = new ArrayList<>();
	public final @NotNull List<RankNode> parents  = new ArrayList<>();
	
	/**
	 * Executes RankNode.
	 */
	public RankNode(final @NotNull RRank rank) {
		
		this.rank = rank;
	}
	
	/**
	 * Checks if this node is a root node (has no parents).
	 */
	public boolean isRoot() {
		
		return this.parents.isEmpty() || this.rank.isInitialRank();
	}
	
	/**
	 * Checks if this node is a leaf node (has no children).
	 */
	public boolean isLeaf() {
		
		return this.children.isEmpty();
	}
	
	/**
	 * Gets the number of children this node has.
	 */
	public int getChildCount() {
		
		return this.children.size();
	}
	
	/**
	 * Gets the number of parents this node has.
	 */
	public int getParentCount() {
		
		return this.parents.size();
	}
	
	/**
	 * Executes toString.
	 */
	@Override
	public String toString() {
		
		return "RankNode{rank=" + this.rank.getIdentifier() +
		       ", children=" + this.children.size() +
		       ", parents=" + this.parents.size() + '}';
	}
	
}
