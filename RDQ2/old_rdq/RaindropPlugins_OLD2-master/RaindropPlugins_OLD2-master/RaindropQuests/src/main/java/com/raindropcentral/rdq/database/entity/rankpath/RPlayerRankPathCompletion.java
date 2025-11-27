package com.raindropcentral.rdq.database.entity.rankpath;

import com.raindropcentral.rdq.database.entity.RDQPlayer;
import com.raindropcentral.rdq.database.entity.rank.RRank;
import com.raindropcentral.rdq.database.entity.rank.RRankTree;
import de.jexcellence.hibernate.entity.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;

/**
 * Represents a player's completion (absolution) of a rank tree.
 * <p>
 * This entity tracks when a player has successfully completed all ranks
 * within a rank tree, marking it as "absolved". Once a rank tree is absolved,
 * it may unlock new progression paths and provide completion rewards.
 * </p>
 *
 * <p>
 * This entity is mapped to the {@code r_player_rank_tree_completion} table in the database.
 * </p>
 *
 * @author JExcellence
 * @version 1.0.0
 * @since TBD
 */
@Entity
@Table(name = "r_player_rank_path_completion")
public class RPlayerRankPathCompletion extends AbstractEntity {
	
	/**
	 * The player who completed the rank tree.
	 */
	@ManyToOne(
		fetch = FetchType.LAZY,
		optional = false
	)
	@JoinColumn(
		name = "player_id",
		nullable = false
	)
	private RDQPlayer player;
	
	/**
	 * The rank tree that was completed.
	 */
	@ManyToOne(
		fetch = FetchType.LAZY,
		optional = false
	)
	@JoinColumn(
		name = "rank_tree_id",
		nullable = false
	)
	private RRankTree completedRankTree;
	
	/**
	 * When the rank tree was completed.
	 */
	@Column(
		name = "completed_at",
		nullable = false
	)
	private LocalDateTime completedAt;
	
	/**
	 * Whether the completion rewards have been granted.
	 */
	@Column(
		name = "rewards_granted",
		nullable = false
	)
	private boolean rewardsGranted;
	
	/**
	 * The final rank that was achieved to complete the tree.
	 */
	@ManyToOne(
		fetch = FetchType.LAZY,
		optional = false
	)
	@JoinColumn(
		name = "final_rank_id",
		nullable = false
	)
	private RRank finalRank;
	
	/**
	 * Protected no-args constructor for JPA.
	 */
	protected RPlayerRankPathCompletion() {}
	
	/**
	 * Constructs a new rank tree completion record.
	 *
	 * @param player the player who completed the tree
	 * @param completedRankTree the rank tree that was completed
	 * @param finalRank the final rank achieved
	 */
	public RPlayerRankPathCompletion(
		final @NotNull RDQPlayer player,
		final @NotNull RRankTree completedRankTree,
		final @NotNull RRank finalRank
	) {
		this.player = player;
		this.completedRankTree = completedRankTree;
		this.finalRank = finalRank;
		this.completedAt = LocalDateTime.now();
		this.rewardsGranted = false;
	}
	
	/**
	 * Gets the player who completed the rank tree.
	 *
	 * @return the player
	 */
	public RDQPlayer getPlayer() {
		return player;
	}
	
	/**
	 * Sets the player who completed the rank tree.
	 *
	 * @param player the player
	 */
	public void setPlayer(final RDQPlayer player) {
		this.player = player;
	}
	
	/**
	 * Gets the completed rank tree.
	 *
	 * @return the completed rank tree
	 */
	public RRankTree getCompletedRankTree() {
		return completedRankTree;
	}
	
	/**
	 * Sets the completed rank tree.
	 *
	 * @param completedRankTree the rank tree
	 */
	public void setCompletedRankTree(final RRankTree completedRankTree) {
		this.completedRankTree = completedRankTree;
	}
	
	/**
	 * Gets when the rank tree was completed.
	 *
	 * @return the completion timestamp
	 */
	public LocalDateTime getCompletedAt() {
		return completedAt;
	}
	
	/**
	 * Sets when the rank tree was completed.
	 *
	 * @param completedAt the completion timestamp
	 */
	public void setCompletedAt(final LocalDateTime completedAt) {
		this.completedAt = completedAt;
	}
	
	/**
	 * Checks if completion rewards have been granted.
	 *
	 * @return true if rewards were granted, false otherwise
	 */
	public boolean isRewardsGranted() {
		return rewardsGranted;
	}
	
	/**
	 * Sets whether completion rewards have been granted.
	 *
	 * @param rewardsGranted the rewards status
	 */
	public void setRewardsGranted(final boolean rewardsGranted) {
		this.rewardsGranted = rewardsGranted;
	}
	
	/**
	 * Gets the final rank that was achieved to complete the tree.
	 *
	 * @return the final rank
	 */
	public RRank getFinalRank() {
		return finalRank;
	}
	
	/**
	 * Sets the final rank that was achieved to complete the tree.
	 *
	 * @param finalRank the final rank
	 */
	public void setFinalRank(final RRank finalRank) {
		this.finalRank = finalRank;
	}
}