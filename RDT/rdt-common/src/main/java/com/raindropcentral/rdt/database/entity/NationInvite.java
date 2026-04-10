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

package com.raindropcentral.rdt.database.entity;

import com.raindropcentral.rplatform.database.converter.UUIDConverter;
import de.jexcellence.hibernate.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

/**
 * Persistent invite record for one town that may join a pending or active nation.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
@Entity
@Table(name = "rdt_nation_invites")
public class NationInvite extends BaseEntity {

    @Column(name = "nation_uuid", nullable = false)
    @Convert(converter = UUIDConverter.class)
    private UUID nationUuid;

    @Column(name = "target_town_uuid", nullable = false)
    @Convert(converter = UUIDConverter.class)
    private UUID targetTownUuid;

    @Enumerated(EnumType.STRING)
    @Column(name = "invite_type", nullable = false, length = 32)
    private NationInviteType inviteType;

    @Enumerated(EnumType.STRING)
    @Column(name = "invite_status", nullable = false, length = 32)
    private NationInviteStatus status;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Column(name = "expires_at", nullable = false)
    private long expiresAt;

    @Column(name = "responded_at", nullable = false)
    private long respondedAt;

    /**
     * Creates a nation invite.
     *
     * @param nationUuid nation UUID
     * @param targetTownUuid invited town UUID
     * @param inviteType invite type
     * @param expiresAt invite expiry timestamp
     */
    public NationInvite(
        final @NotNull UUID nationUuid,
        final @NotNull UUID targetTownUuid,
        final @NotNull NationInviteType inviteType,
        final long expiresAt
    ) {
        this.nationUuid = Objects.requireNonNull(nationUuid, "nationUuid");
        this.targetTownUuid = Objects.requireNonNull(targetTownUuid, "targetTownUuid");
        this.inviteType = Objects.requireNonNull(inviteType, "inviteType");
        this.status = NationInviteStatus.PENDING;
        this.createdAt = System.currentTimeMillis();
        this.expiresAt = Math.max(0L, expiresAt);
        this.respondedAt = 0L;
    }

    /**
     * Constructor reserved for JPA entity hydration.
     */
    protected NationInvite() {
    }

    /**
     * Returns the linked nation UUID.
     *
     * @return nation UUID
     */
    public @NotNull UUID getNationUuid() {
        return Objects.requireNonNull(this.nationUuid, "nationUuid");
    }

    /**
     * Returns the invited town UUID.
     *
     * @return target town UUID
     */
    public @NotNull UUID getTargetTownUuid() {
        return Objects.requireNonNull(this.targetTownUuid, "targetTownUuid");
    }

    /**
     * Returns the invite type.
     *
     * @return invite type
     */
    public @NotNull NationInviteType getInviteType() {
        return this.inviteType == null ? NationInviteType.FORMATION : this.inviteType;
    }

    /**
     * Returns the invite status.
     *
     * @return invite status
     */
    public @NotNull NationInviteStatus getStatus() {
        return this.status == null ? NationInviteStatus.PENDING : this.status;
    }

    /**
     * Returns the creation timestamp.
     *
     * @return creation timestamp
     */
    public long getCreatedAtMillis() {
        return Math.max(0L, this.createdAt);
    }

    /**
     * Returns the expiry timestamp.
     *
     * @return expiry timestamp
     */
    public long getExpiresAt() {
        return Math.max(0L, this.expiresAt);
    }

    /**
     * Returns the response timestamp.
     *
     * @return response timestamp, or {@code 0} when pending
     */
    public long getRespondedAt() {
        return Math.max(0L, this.respondedAt);
    }

    /**
     * Returns whether the invite is still pending.
     *
     * @return {@code true} when the invite is still pending
     */
    public boolean isPending() {
        return this.getStatus() == NationInviteStatus.PENDING;
    }

    /**
     * Marks the invite as accepted.
     */
    public void accept() {
        this.status = NationInviteStatus.ACCEPTED;
        this.respondedAt = System.currentTimeMillis();
    }

    /**
     * Marks the invite as declined.
     */
    public void decline() {
        this.status = NationInviteStatus.DECLINED;
        this.respondedAt = System.currentTimeMillis();
    }

    /**
     * Marks the invite as timed out.
     */
    public void timeout() {
        this.status = NationInviteStatus.TIMED_OUT;
        this.respondedAt = Math.max(System.currentTimeMillis(), this.expiresAt);
    }
}
