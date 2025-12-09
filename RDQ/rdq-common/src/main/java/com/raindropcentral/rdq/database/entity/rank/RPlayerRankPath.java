package com.raindropcentral.rdq.database.entity.rank;

import com.raindropcentral.rdq.database.entity.player.RDQPlayer;
import de.jexcellence.hibernate.entity.AbstractEntity;
import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;

/**
 * Entity representing the association between a player and their selected rank path (rank tree).
 * <p>
 * This entity tracks which rank tree paths a player has selected and which one is currently active.
 * Players can have multiple rank path records (one per rank tree they've interacted with),
 * but only one can be active at a time. Players start with no rank paths and must choose
 * their first progression path when they access the rank system.
 * </p>
 *
 * @author JExcellence
 * @version 1.1.0
 * @since TBD
 */
@Entity
@Table(
	name = "r_player_rank_path",
	uniqueConstraints = @UniqueConstraint(columnNames = {"player_id", "rank_tree_id"})
)
public class RPlayerRankPath extends AbstractEntity {
	
	/**
	 * The player associated with this rank path record.
	 * Changed from @OneToOne to @ManyToOne to allow multiple rank paths per player.
	 */
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(
		name = "player_id",
		nullable = false
	)
	private RDQPlayer player;
	
	/**
	 * The rank tree (path) that the player has selected.
	 */
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(
		name = "rank_tree_id",
		nullable = false
	)
	private RRankTree selectedRankPath;
	
	/**
	 * Indicates whether this rank path is currently active for the player.
	 * Only one rank path can be active per player at any given time.
	 */
	@Column(
		name = "is_active",
		nullable = false
	)
	private boolean isActive;
	
	/**
	 * Tracks the completion status of this rank path.
	 * True if the player has completed all ranks in this tree.
	 */
	@Column(
		name = "is_completed",
		nullable = false
	)
	private boolean isCompleted = false;
	
	/**
	 * The timestamp when this rank path was completed by the player.
	 */
	@Column(
		name = "completed_at",
		nullable = true
	)
	private LocalDateTime completedAt;
	
	/**
	 * Protected no-argument constructor for JPA.
	 */
	protected RPlayerRankPath() {}
	
	/**
	 * Constructs a new {@code RDQPlayerRankPath} entity for the specified player and rank tree.
	 *
	 * @param player        the player associated with this rank path record
	 * @param selectedRankPath the rank tree that the player has selected
	 * @param isActive         whether this path is currently active
	 */
	public RPlayerRankPath(
		final @NotNull RDQPlayer player,
		final @NotNull RRankTree selectedRankPath,
		final boolean isActive
	) {
		this.player = player;
		this.selectedRankPath = selectedRankPath;
		this.isActive = isActive;
	}
	
	/**
	 * Convenience constructor for creating a new active rank path selection.
	 *
	 * @param rdqPlayer        the player selecting the rank path
	 * @param selectedRankPath the rank tree being selected
	 */
	public RPlayerRankPath(
		final @NotNull RDQPlayer rdqPlayer,
		final @NotNull RRankTree selectedRankPath
	) {
		this(rdqPlayer, selectedRankPath, true);
	}
	
	/**
	 * Gets the player associated with this rank path record.
	 *
	 * @return the {@link RDQPlayer} entity
	 */
	public RDQPlayer getRdqPlayer() {
		return this.player;
	}
	
	/**
	 * Sets the player for this rank path record.
	 *
	 * @param player the player to set
	 */
	public void setRdqPlayer(final @NotNull RDQPlayer player) {
		this.player = player;
	}
	
	/**
	 * Gets the rank tree that the player has selected.
	 *
	 * @return the {@link RRankTree} entity
	 */
	public RRankTree getSelectedRankPath() {
		return this.selectedRankPath;
	}
	
	/**
	 * Sets the selected rank path for the player.
	 *
	 * @param selectedRankPath the new rank tree to set
	 */
	public void setSelectedRankPath(final @NotNull RRankTree selectedRankPath) {
		this.selectedRankPath = selectedRankPath;
	}
	
	/**
	 * Checks whether this rank path is currently active for the player.
	 *
	 * @return {@code true} if the path is active, {@code false} otherwise
	 */
	public boolean isActive() {
		return this.isActive;
	}
	
	/**
	 * Sets the active status of this rank path.
	 * Note: When setting a path as active, ensure other paths for the same player are set to inactive.
	 *
	 * @param active the new active status
	 */
	public void setActive(final boolean active) {
		this.isActive = active;
	}
	
	/**
	 * Checks whether this rank path has been completed by the player.
	 *
	 * @return {@code true} if completed, {@code false} otherwise
	 */
	public boolean isCompleted() {
		return this.isCompleted;
	}
	
	/**
	 * Sets the completion status of this rank path.
	 * Automatically sets the completion timestamp when marked as completed.
	 *
	 * @param completed the new completion status
	 */
	public void setCompleted(final boolean completed) {
		this.isCompleted = completed;
		if (completed && this.completedAt == null) {
			this.completedAt = java.time.LocalDateTime.now();
		} else if (!completed) {
			this.completedAt = null;
		}
	}
	
	/**
	 * Gets the timestamp when this rank path was completed.
	 *
	 * @return the completion timestamp, or null if not completed
	 */
	public java.time.LocalDateTime getCompletedAt() {
		return this.completedAt;
	}
	
	/**
	 * Sets the timestamp when this rank path was completed.
	 *
	 * @param completedAt the completion timestamp
	 */
	public void setCompletedAt(final LocalDateTime completedAt) {
		this.completedAt = completedAt;
	}
	
	/**
	 * Marks this rank path as completed with the current timestamp.
	 */
	public void markAsCompleted() {
		setCompleted(true);
	}
	
	/**
	 * Checks if this rank path was selected recently (within the last hour).
	 *
	 * @return true if selected within the last hour, false otherwise
	 */
	public boolean isRecentlySelected() {
		return this.getCreatedAt() != null &&
		       this.getCreatedAt().isAfter(LocalDateTime.now().minusHours(1));
	}
	
	@Override
	public String toString() {
		return "RDQPlayerRankPath{" +
		       "player=" + (player != null ? player.getPlayerName() : "null") +
		       ", rankTree=" + (selectedRankPath != null ? selectedRankPath.getIdentifier() : "null") +
		       ", isActive=" + isActive +
		       ", isCompleted=" + isCompleted +
		       '}';
	}
}