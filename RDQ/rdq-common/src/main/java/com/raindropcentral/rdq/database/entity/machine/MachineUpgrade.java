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

package com.raindropcentral.rdq.database.entity.machine;

import com.raindropcentral.rdq.machine.type.EUpgradeType;
import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entity representing an upgrade applied to a machine.
 *
 * <p>This entity tracks the type and level of upgrades applied to machines,
 * such as speed, efficiency, bonus output, and fuel reduction upgrades.
 *
 * @author JExcellence
 * @version 1.0.0
 */
@Entity
@Table(
    name = "rdq_machine_upgrades",
    uniqueConstraints = @UniqueConstraint(columnNames = {"machine_id", "upgrade_type"})
)
@Getter
@Setter
public class MachineUpgrade extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * The machine this upgrade is applied to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "machine_id", nullable = false)
    private Machine machine;

    /**
     * The type of upgrade (SPEED, EFFICIENCY, BONUS_OUTPUT, FUEL_REDUCTION).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "upgrade_type", nullable = false, length = 30)
    private EUpgradeType upgradeType;

    /**
     * The level of this upgrade.
     */
    @Column(name = "level", nullable = false)
    private int level = 1;

    /**
     * Timestamp when this upgrade was applied.
     */
    @CreationTimestamp
    @Column(name = "applied_at", nullable = false, updatable = false)
    private LocalDateTime appliedAt;

    /**
     * Protected no-argument constructor for JPA.
     */
    protected MachineUpgrade() {
    }

    /**
     * Constructs a new {@code MachineUpgrade}.
     *
     * @param machine     the machine this upgrade is applied to
     * @param upgradeType the type of upgrade
     * @param level       the level of the upgrade
     */
    public MachineUpgrade(
        @NotNull final Machine machine,
        @NotNull final EUpgradeType upgradeType,
        final int level
    ) {
        this.machine = machine;
        this.upgradeType = upgradeType;
        this.level = level;
    }

    /**
     * Increases the level of this upgrade by 1.
     */
    public void incrementLevel() {
        this.level++;
    }

    /**
     * Sets the level of this upgrade.
     *
     * @param level the new level
     */
    public void setLevel(final int level) {
        if (level < 1) {
            throw new IllegalArgumentException("Upgrade level must be at least 1");
        }
        this.level = level;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MachineUpgrade that)) return false;

        if (this.getId() != null && that.getId() != null) {
            return this.getId().equals(that.getId());
        }

        return machine != null && machine.equals(that.machine) &&
            upgradeType == that.upgradeType;
    }

    @Override
    public int hashCode() {
        if (this.getId() != null) {
            return this.getId().hashCode();
        }

        return Objects.hash(machine, upgradeType);
    }

    @Override
    public String toString() {
        return "MachineUpgrade{" +
            "id=" + getId() +
            ", machineId=" + (machine != null ? machine.getId() : null) +
            ", upgradeType=" + upgradeType +
            ", level=" + level +
            '}';
    }
}
