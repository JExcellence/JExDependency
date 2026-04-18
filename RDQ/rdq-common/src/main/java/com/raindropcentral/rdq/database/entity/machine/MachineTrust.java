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

import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import java.util.UUID;

/**
 * Entity representing a trust relationship for a machine.
 *
 * <p>This entity tracks players who are trusted to interact with a machine,
 * allowing them to access the machine's GUI and perform operations.
 *
 * @author JExcellence
 * @version 1.0.0
 */
@Entity
@Table(
    name = "rdq_machine_trust",
    uniqueConstraints = @UniqueConstraint(columnNames = {"machine_id", "trusted_uuid"})
)
@Getter
@Setter
public class MachineTrust extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * The machine this trust entry belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "machine_id", nullable = false)
    private Machine machine;

    /**
     * The UUID of the trusted player.
     */
    @Column(name = "trusted_uuid", nullable = false)
    private UUID trustedUuid;

    /**
     * Timestamp when this trust was granted.
     */
    @CreationTimestamp
    @Column(name = "granted_at", nullable = false, updatable = false)
    private LocalDateTime grantedAt;

    /**
     * Protected no-argument constructor for JPA.
     */
    protected MachineTrust() {
    }

    /**
     * Constructs a new {@code MachineTrust} entry.
     *
     * @param machine     the machine this trust entry belongs to
     * @param trustedUuid the UUID of the trusted player
     */
    public MachineTrust(
        @NotNull final Machine machine,
        @NotNull final UUID trustedUuid
    ) {
        this.machine = machine;
        this.trustedUuid = trustedUuid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MachineTrust that)) return false;

        if (this.getId() != null && that.getId() != null) {
            return this.getId().equals(that.getId());
        }

        return machine != null && machine.equals(that.machine) &&
            trustedUuid.equals(that.trustedUuid);
    }

    @Override
    public int hashCode() {
        if (this.getId() != null) {
            return this.getId().hashCode();
        }

        return Objects.hash(machine, trustedUuid);
    }

    @Override
    public String toString() {
        return "MachineTrust{" +
            "id=" + getId() +
            ", machineId=" + (machine != null ? machine.getId() : null) +
            ", trustedUuid=" + trustedUuid +
            ", grantedAt=" + grantedAt +
            '}';
    }
}
