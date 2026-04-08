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
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.jetbrains.annotations.NotNull;

/**
 * Entity representing a player's progress towards fulfilling a specific rank upgrade requirement.
 *
 * <p>Each instance of this class tracks the progress of a single player towards a single
 * {@link RRankUpgradeRequirement}. This follows the same pattern as the generator system
 * where progress is tracked against individual requirements.
 *
 *
 * <p>This entity is mapped to the {@code r_player_rank_upgrade_progress} table in the database.
 *
 * @author JExcellence
 * @version 2.0.0
 * @since TBD
 */
@Entity
@Table(
		name = "r_player_rank_upgrade_progress",
		uniqueConstraints = @UniqueConstraint(columnNames = {"player_id", "upgrade_requirement_id"})
)
/**
 * Represents the RPlayerRankUpgradeProgress API type.
 */
public class RPlayerRankUpgradeProgress extends BaseEntity {

	/**
	 * The player whose progress is being tracked.
	 */
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(
			name = "player_id",
			nullable = false
	)
	private RDQPlayer player;

	/**
	 * The rank upgrade requirement for which progress is being tracked.
	 */
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(
			name = "upgrade_requirement_id",
			nullable = false
	)
	private RRankUpgradeRequirement upgradeRequirement;

	/**
	 * The progress value towards completing the requirement.
 *
 * <p>The meaning and scale of this value depend on the requirement's implementation.
	 * Progress is typically between 0.0 and 1.0, where 1.0 means completed.
	 */
	@Column(name = "progress_value", nullable = false)
	private double progress;

	/**
	 * Protected no-argument constructor for JPA.
	 */
	protected RPlayerRankUpgradeProgress() {}

	/**
	 * Constructs a new {@code RPlayerRankUpgradeProgress} entity for the given player and upgrade requirement.
	 *
	 * @param player             the player whose progress is being tracked
	 * @param upgradeRequirement the upgrade requirement being progressed towards
	 */
	public RPlayerRankUpgradeProgress(
			@NotNull final RDQPlayer player,
			@NotNull final RRankUpgradeRequirement upgradeRequirement
	) {
		this.player = player;
		this.upgradeRequirement = upgradeRequirement;
		this.progress = 0.0;
	}

	/**
	 * Gets the player whose progress is being tracked.
	 *
	 * @return the player entity
	 */
	@NotNull
	public RDQPlayer getPlayer() {
		return this.player;
	}

	/**
	 * Sets the player whose progress is being tracked.
	 *
	 * @param player the player entity
	 */
	public void setPlayer(@NotNull final RDQPlayer player) {
		this.player = player;
	}

	/**
	 * Gets the upgrade requirement for which progress is being tracked.
	 *
	 * @return the upgrade requirement entity
	 */
	@NotNull
	public RRankUpgradeRequirement getUpgradeRequirement() {
		return this.upgradeRequirement;
	}

	/**
	 * Sets the upgrade requirement for which progress is being tracked.
	 *
	 * @param upgradeRequirement the upgrade requirement entity
	 */
	public void setUpgradeRequirement(@NotNull final RRankUpgradeRequirement upgradeRequirement) {
		this.upgradeRequirement = upgradeRequirement;
	}

	/**
	 * Gets the current progress value towards the requirement.
	 *
	 * @return the progress value (typically between 0.0 and 1.0)
	 */
	public double getProgress() {
		return this.progress;
	}

	/**
	 * Sets the progress value towards the requirement.
	 * Progress is capped at 1.0 to prevent overflow.
	 *
	 * @param progress the new progress value
	 */
	public void setProgress(final double progress) {
		this.progress = Math.min(progress, 1.0);
	}

	/**
	 * Increments the progress by a given amount, capping at 1.0.
	 *
	 * @param amount the amount to increment by
	 * @return the new progress value
	 */
	public double incrementProgress(final double amount) {
		setProgress(this.progress + amount);
		return this.progress;
	}

	/**
	 * Resets the progress to zero.
	 */
	public void resetProgress() {
		this.progress = 0.0;
	}

	/**
	 * Checks if this requirement has been completed (progress >= 1.0).
	 *
	 * @return true if completed, false otherwise
	 */
	public boolean isCompleted() {
		return this.progress >= 1.0;
	}

	/**
	 * Gets the rank this progress is ultimately for.
	 * This is a convenience method to navigate up the relationship chain.
	 *
	 * @return the target rank, or null if the relationship chain is incomplete
	 */
	@NotNull
	public RRank getTargetRank() {
		return this.upgradeRequirement.getRank();
	}
}
