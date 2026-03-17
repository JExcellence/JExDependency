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

import com.raindropcentral.rdq.config.utility.IconSection;
import com.raindropcentral.rdq.database.converter.IconSectionConverter;
import com.raindropcentral.rdq.database.entity.requirement.BaseRequirement;
import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Entity representing a single upgrade requirement for a {@link RRank} in the RaindropQuests system.
 *
 * <p>This entity encapsulates a single {@link com.raindropcentral.rdq.database.entity.requirement.BaseRequirement} that must be satisfied
 * to fulfill part of the upgrade condition for the associated rank. It also includes an icon for visual representation.
 *
 *
 * <p>Multiple instances of this entity can exist for a single rank, representing different requirements
 * that all need to be completed for the rank upgrade.
 *
 * @author JExcellence
 * @version 2.0.0
 * @since TBD
 */
@Entity
@Table(name = "r_rank_upgrade_requirement")
@Getter
@Setter
public class RRankUpgradeRequirement extends BaseEntity {
	
	/**
	 * The rank to which this upgrade requirement belongs.
	 */
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(
		name = "rank_id",
		nullable = false
	)
	private RRank rank;
	
	/**
	 * The requirement that must be satisfied for this upgrade.
	 */
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(
		name = "requirement_id",
		nullable = false
	)
	private BaseRequirement requirement;
	
	/**
	 * The icon representing this upgrade requirement, stored as a serialized {@link ItemStack}.
	 */
	@Column(
		name = "icon",
		nullable = false,
		columnDefinition = "LONGTEXT"
	)
	@Convert(converter = IconSectionConverter.class)
	private IconSection icon;
	
	/**
	 * Optional display order for this requirement within the rank's requirements.
	 */
	@Column(name = "display_order")
	private int displayOrder = 0;
	
	@Version
	@Column(name = "version")
	private int version;
	
	/**
	 * Protected no-argument constructor for JPA.
	 */
	protected RRankUpgradeRequirement() {}
	
	/**
	 * Constructs a new {@code RRankUpgradeRequirement} with the specified rank, requirement, and icon.
	 *
	 * @param rank        the {@link RRank} to which this upgrade requirement belongs
	 * @param requirement the {@link BaseRequirement} that must be satisfied
	 * @param icon        the {@link ItemStack} used as the icon for this upgrade requirement
	 */
	public RRankUpgradeRequirement(
		@Nullable final RRank rank,
		@NotNull final BaseRequirement requirement,
		@NotNull final IconSection icon
	) {
		this.rank = rank;
		this.requirement = requirement;
		this.icon = icon;
		
		if (rank != null) {
			rank.addUpgradeRequirement(this);
		}
	}
	
	/**
	 * Returns the rank to which this upgrade requirement belongs.
	 *
	 * @return the associated {@link RRank}
	 */
	@NotNull
	public RRank getRank() {
		return this.rank;
	}
	
	/**
	 * Returns the requirement that must be satisfied for this upgrade.
	 *
	 * @return the {@link BaseRequirement} object
	 */
	@NotNull
	public BaseRequirement getRequirement() {
		return this.requirement;
	}
	
	/**
	 * Sets the requirement for this upgrade requirement.
	 *
	 * @param requirement the requirement
	 */
	public void setRequirement(@NotNull final BaseRequirement requirement) {
		this.requirement = requirement;
	}

	/**
	 * Convenience method to check if this requirement is met for a player.
	 *
	 * @param player the player to check against
	 * @return {@code true} if the requirement is met, {@code false} otherwise
	 */
	public boolean isMet(@NotNull final Player player) {
		return this.requirement.isMet(player);
	}
	
	/**
	 * Convenience method to calculate the progress for this requirement.
	 *
	 * @param player the player to calculate progress for
	 * @return the progress value between 0.0 and 1.0
	 */
	public double calculateProgress(@NotNull final Player player) {
		return this.requirement.calculateProgress(player);
	}
	
	/**
	 * Convenience method to consume resources for this requirement.
	 *
	 * @param player the player from whom to consume resources
	 */
	public void consume(@NotNull final Player player) {
		this.requirement.consume(player);
	}
	
	/**
	 * Executes equals.
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof RRankUpgradeRequirement that)) return false;
		
		if (this.getId() != null && that.getId() != null) {
			return this.getId().equals(that.getId());
		}
		
		if (this.requirement != null && that.requirement != null &&
		    this.rank != null && that.rank != null) {
			return this.requirement.equals(that.requirement) &&
			       this.rank.equals(that.rank) &&
			       this.displayOrder == that.displayOrder;
		}
		
		return false;
	}
	
	/**
	 * Returns whether hCode.
	 */
	@Override
	public int hashCode() {
		if (this.getId() != null) {
			return this.getId().hashCode();
		}
		
		if (this.requirement != null && this.rank != null) {
			return Objects.hash(this.requirement, this.rank, this.displayOrder);
		}
		
		return System.identityHashCode(this);
	}
	
	/**
	 * Enhanced setRank method with better relationship management.
	 */
	public void setRank(@Nullable final RRank rank) {
		if (this.rank != null && this.rank != rank) {
			this.rank.getUpgradeRequirements().remove(this);
		}
		
		this.rank = rank;
		
		if (rank != null) {
			rank.getUpgradeRequirements().add(this);
		}
	}
}
