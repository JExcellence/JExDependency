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

package com.raindropcentral.rdq.database.entity.rank;


import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;

/**
 * Entity representing the association between a player and their current rank within a specific rank tree.
 *
 * <p>This entity maps a player to their current rank and rank tree, and tracks whether this rank is currently active.
 * It is mapped to the {@code r_player_rank} table in the database, with a unique constraint on the combination
 * of player and rank tree to ensure a player can only have one current rank per rank tree.
 *
 * @author JExcellence
 * @version 1.1.0
 * @since TBD
 */
@Entity
@Table(
		name = "r_player_rank",
		uniqueConstraints = @UniqueConstraint(columnNames = {"player_id", "rank_tree_id"})
)
/**
 * Represents the RPlayerRank API type.
 */
public class RPlayerRank extends BaseEntity {

	/**
	 * The player associated with this rank record.
	 * Changed to ManyToOne to allow multiple rank records per player (one per rank tree).
	 */
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(
			name = "player_id",
			nullable = false
	)
	private RDQPlayer player;

	/**
	 * The current rank of the player within the specified rank tree.
	 */
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(
			name = "current_rank_id",
			nullable = false
	)
	private RRank currentRank;

	/**
	 * The rank tree this rank belongs to.
	 * This is needed to enforce the unique constraint and to identify which rank tree this rank record is for.
	 */
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(
			name = "rank_tree_id"
	)
	private RRankTree rankTree;

	/**
	 * Indicates whether this rank is currently active for the player.
	 */
	@Column(
			name = "is_active",
			nullable = false
	)
	private boolean isActive = true;

	/**
	 * Protected no-argument constructor for JPA.
	 */
	protected RPlayerRank() {}

	/**
	 * Constructs a new {@code RPlayerRank} entity for the specified player, rank, and rank tree.
	 *
	 * @param player   the player associated with this rank record
	 * @param currentRank the current rank of the player within the rank tree
	 * @param rankTree    the rank tree this rank belongs to
	 */
	public RPlayerRank(
			final @NotNull RDQPlayer player,
			final @NotNull RRank currentRank,
			final @NotNull RRankTree rankTree
	) {

		this.player = player;
		this.currentRank = currentRank;
		this.rankTree = rankTree;
	}

	/**
	 * Constructs a new {@code RPlayerRank} entity for the specified player, rank, rank tree, and active status.
	 *
	 * @param player   the player associated with this rank record
	 * @param currentRank the current rank of the player within the rank tree
	 * @param rankTree    the rank tree this rank belongs to
	 * @param isActive    whether this rank is currently active
	 */
	public RPlayerRank(
			final @NotNull RDQPlayer player,
			final @NotNull RRank currentRank,
			final @NotNull RRankTree rankTree,
			final boolean isActive
	) {

		this.player = player;
		this.currentRank = currentRank;
		this.rankTree = rankTree;
		this.isActive = isActive;
	}

	/**
	 * Legacy constructor for backward compatibility.
	 * Automatically derives the rank tree from the current rank.
	 *
	 * @param player the player associated with this rank record
	 * @param rank      the current rank of the player within the rank tree
	 */
	public RPlayerRank(
			final @NotNull RDQPlayer player,
			final @NotNull RRank rank
	) {

		this.player = player;
		this.currentRank = rank;
		this.rankTree = rank.getRankTree();
	}

	/**
	 * Gets the player associated with this rank record.
	 *
	 * @return the {@link RDQPlayer} entity
	 */
	public RDQPlayer getRdqPlayer() {

		return this.player;
	}

	/**
	 * Sets the player associated with this rank record.
	 *
	 * @param player the player to set
	 */
	public void setRdqPlayer(final @NotNull RDQPlayer player) {

		this.player = player;
	}

	/**
	 * Gets the current rank of the player within the specified rank tree.
	 *
	 * @return the {@link RRank} entity
	 */
	public RRank getCurrentRank() {

		return this.currentRank;
	}

	/**
	 * Sets the current rank of the player.
	 *
	 * @param currentRank the new current rank
	 */
	public void setCurrentRank(final @NotNull RRank currentRank) {

		this.currentRank = currentRank;
	}

	/**
	 * Gets the rank tree this rank belongs to.
	 *
	 * @return the {@link RRankTree} entity
	 */
	public RRankTree getRankTree() {

		return this.rankTree;
	}

	/**
	 * Sets the rank tree this rank belongs to.
	 *
	 * @param rankTree the rank tree to set
	 */
	public void setRankTree(final @NotNull RRankTree rankTree) {

		this.rankTree = rankTree;
	}

	/**
	 * Checks whether this rank is currently active for the player.
	 *
	 * @return {@code true} if the rank is active, {@code false} otherwise
	 */
	public boolean isActive() {

		return this.isActive;
	}

	/**
	 * Sets the active status of this rank.
	 *
	 * @param active the new active status
	 */
	public void setActive(final boolean active) {

		this.isActive = active;
	}

	/**
	 * Convenience method to activate this rank record.
	 */
	public void activate() {

		this.isActive = true;
	}

	/**
	 * Convenience method to deactivate this rank record.
	 */
	public void deactivate() {

		this.isActive = false;
	}

	/**
	 * Checks if this rank record belongs to the specified rank tree.
	 *
	 * @param rankTree the rank tree to check against
	 *
	 * @return true if this record belongs to the specified rank tree
	 */
	public boolean belongsToRankTree(final @NotNull RRankTree rankTree) {

		return this.rankTree != null && this.rankTree.equals(rankTree);
	}

	/**
	 * Checks if this rank record belongs to a rank tree with the specified identifier.
	 *
	 * @param rankTreeIdentifier the rank tree identifier to check against
	 *
	 * @return true if this record belongs to a rank tree with the specified identifier
	 */
	public boolean belongsToRankTree(final @NotNull String rankTreeIdentifier) {

		return this.rankTree != null &&
				this.rankTree.getIdentifier() != null &&
				this.rankTree.getIdentifier().equals(rankTreeIdentifier);
	}

	/**
	 * Executes toString.
	 */
	@Override
	public String toString() {

		return "RPlayerRank{" +
				"player=" + (
				player != null ?
						player.getPlayerName() :
						"null"
		) +
				", currentRank=" + (
				currentRank != null ?
						currentRank.getIdentifier() :
						"null"
		) +
				", rankTree=" + (
				rankTree != null ?
						rankTree.getIdentifier() :
						"null"
		) +
				", isActive=" + isActive +
				'}';
	}

}
