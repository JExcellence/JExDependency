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

package com.raindropcentral.rdq.database.entity.perk;

import com.raindropcentral.rplatform.config.icon.IconSection;
import com.raindropcentral.rdq.database.converter.IconSectionConverter;
import com.raindropcentral.rplatform.database.converter.RequirementConverter;
import com.raindropcentral.rplatform.requirement.AbstractRequirement;
import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Entity representing a requirement that must be met to unlock a perk.
 *
 * <p>This entity encapsulates an {@link AbstractRequirement} from RPlatform and its visual icon,
 * providing convenience methods for requirement evaluation and progress calculation.
 *
 * @author JExcellence
 * @version 1.0.0
 */
@Setter
@Getter
@Entity
@Table(name = "rdq_perk_requirement")
public class PerkRequirement extends BaseEntity {

    /**
     * The perk to which this requirement belongs.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "perk_id", nullable = false)
    private Perk perk;

    /**
     * Display order for this requirement within the perk's requirements.
     */
    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    /**
     * The requirement definition stored as JSON.
     */
    @Convert(converter = RequirementConverter.class)
    @Column(name = "requirement_data", nullable = false, columnDefinition = "LONGTEXT")
    private AbstractRequirement requirement;

    /**
     * The icon representing this requirement in the UI.
     */
    @Convert(converter = IconSectionConverter.class)
    @Column(name = "icon", columnDefinition = "LONGTEXT")
    private IconSection icon;

    /**
     * Version for optimistic locking.
     */
    @Version
    @Column(name = "version")
    private int version;

    /**
     * Protected no-argument constructor for JPA.
     */
    protected PerkRequirement() {
    }

    /**
     * Constructs a new {@code PerkRequirement} with the specified perk, requirement, and icon.
     *
     * @param perk        the perk to which this requirement belongs
     * @param requirement the requirement definition
     * @param icon        the icon for UI display
     */
    public PerkRequirement(
            @Nullable final Perk perk,
            @NotNull final AbstractRequirement requirement,
            @Nullable final IconSection icon
    ) {
        this.perk = perk;
        this.requirement = requirement;
        this.icon = icon;

        if (perk != null) {
            perk.addRequirement(this);
        }
    }

    /**
     * Checks if this requirement is met by the specified player.
     *
     * @param player the player to check
     * @return true if the requirement is met, false otherwise
     */
    public boolean isMet(@NotNull final Player player) {
        return requirement.isMet(player);
    }

    /**
     * Calculates the progress towards meeting this requirement for the specified player.
     *
     * @param player the player to check
     * @return progress value between 0.0 and 1.0
     */
    public double calculateProgress(@NotNull final Player player) {
        return requirement.calculateProgress(player);
    }

    /**
     * Consumes resources required by this requirement for the specified player.
     *
     * @param player the player whose resources to consume
     */
    public void consume(@NotNull final Player player) {
        requirement.consume(player);
    }

    /**
     * Gets the type identifier of this requirement.
     *
     * @return the requirement type ID
     */
    public String getTypeId() {
        return requirement.getTypeId();
    }

    /**
     * Gets the description key for this requirement.
     *
     * @return the i18n description key
     */
    public String getDescriptionKey() {
        return requirement.getDescriptionKey();
    }

    /**
     * Executes equals.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PerkRequirement that)) return false;

        if (this.getId() != null && that.getId() != null) {
            return this.getId().equals(that.getId());
        }

        return perk != null && perk.equals(that.perk) &&
                requirement != null && requirement.equals(that.requirement) &&
                displayOrder == that.displayOrder;
    }

    /**
     * Returns whether hCode.
     */
    @Override
    public int hashCode() {
        if (this.getId() != null) {
            return this.getId().hashCode();
        }

        return Objects.hash(perk, requirement, displayOrder);
    }

    /**
     * Executes toString.
     */
    @Override
    public String toString() {
        return "PerkRequirement{" +
                "id=" + getId() +
                ", perk=" + (perk != null ? perk.getIdentifier() : null) +
                ", displayOrder=" + displayOrder +
                ", requirementType=" + (requirement != null ? requirement.getTypeId() : null) +
                '}';
    }
}
