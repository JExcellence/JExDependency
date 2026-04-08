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
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

/**
 * Persistent invitation from a town to a specific player.
 *
 * @author ItsRainingHP
 * @since 1.0.0
 * @version 1.0.0
 */
@Entity
@Table(name = "rdt_town_invites")
public class TownInvite extends BaseEntity {

    @Column(name = "invite_uuid", nullable = false, unique = true)
    @Convert(converter = UUIDConverter.class)
    private UUID inviteUuid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "town_id", nullable = false)
    private RTown town;

    @Column(name = "invited_player_uuid", nullable = false)
    @Convert(converter = UUIDConverter.class)
    private UUID invitedPlayerUuid;

    @Column(name = "inviter_uuid", nullable = false)
    @Convert(converter = UUIDConverter.class)
    private UUID inviterUuid;

    @Column(name = "created_at", nullable = false)
    private long createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private TownInviteStatus status;

    /**
     * Creates a persisted town invite.
     *
     * @param town owning town
     * @param invitedPlayerUuid invited player UUID
     * @param inviterUuid inviter UUID
     */
    public TownInvite(
        final @NotNull RTown town,
        final @NotNull UUID invitedPlayerUuid,
        final @NotNull UUID inviterUuid
    ) {
        this.inviteUuid = UUID.randomUUID();
        this.town = Objects.requireNonNull(town, "town");
        this.invitedPlayerUuid = Objects.requireNonNull(invitedPlayerUuid, "invitedPlayerUuid");
        this.inviterUuid = Objects.requireNonNull(inviterUuid, "inviterUuid");
        this.createdAt = System.currentTimeMillis();
        this.status = TownInviteStatus.ACTIVE;
    }

    /**
     * Constructor reserved for JPA entity hydration.
     */
    protected TownInvite() {
    }

    /**
     * Returns the stable invite identifier.
     *
     * @return invite UUID
     */
    public @NotNull UUID getInviteUuid() {
        return this.inviteUuid;
    }

    /**
     * Returns the owning town.
     *
     * @return owning town
     */
    public @NotNull RTown getTown() {
        return this.town;
    }

    /**
     * Returns the invited player UUID.
     *
     * @return invited player UUID
     */
    public @NotNull UUID getInvitedPlayerUuid() {
        return this.invitedPlayerUuid;
    }

    /**
     * Returns the inviter UUID.
     *
     * @return inviter UUID
     */
    public @NotNull UUID getInviterUuid() {
        return this.inviterUuid;
    }

    /**
     * Returns the invite creation timestamp in epoch milliseconds.
     *
     * @return creation timestamp
     */
    public long getInviteCreatedAt() {
        return this.createdAt;
    }

    /**
     * Returns the current invite status.
     *
     * @return current invite status
     */
    public @NotNull TownInviteStatus getStatus() {
        return this.status;
    }

    /**
     * Returns whether this invite is still active.
     *
     * @return {@code true} when the invite can still be accepted
     */
    public boolean isActive() {
        return this.status == TownInviteStatus.ACTIVE;
    }

    /**
     * Marks the invite as accepted.
     */
    public void accept() {
        this.status = TownInviteStatus.ACCEPTED;
    }

    /**
     * Marks the invite as declined.
     */
    public void decline() {
        this.status = TownInviteStatus.DECLINED;
    }

    /**
     * Marks the invite as expired.
     */
    public void expire() {
        this.status = TownInviteStatus.EXPIRED;
    }
}
